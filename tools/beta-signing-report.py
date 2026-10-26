#!/usr/bin/env python3
"""Export public Android certificate fingerprints for backend registration."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--keystore', type=Path, default=Path('keystore/debug.keystore'))
    parser.add_argument('--config', type=Path, default=Path('app/google-services.json'))
    parser.add_argument('--output', type=Path, default=Path('build/beta-signing'))
    parser.add_argument('--package', default='com.glyphix.app')
    args = parser.parse_args()
    certificate = subprocess.run([
        'keytool', '-exportcert', '-keystore', str(args.keystore), '-storepass', 'android',
        '-alias', 'androiddebugkey',
    ], check=True, capture_output=True).stdout
    sha1 = hashlib.sha1(certificate).hexdigest().upper()
    sha256 = hashlib.sha256(certificate).hexdigest().upper()
    config = json.loads(args.config.read_text())
    clients = [client for client in config.get('client', [])
               if client.get('client_info', {}).get('android_client_info', {}).get('package_name') == args.package]
    if not clients:
        raise SystemExit('No Firebase Android client matches ' + args.package)
    configured = {oauth.get('android_info', {}).get('certificate_hash', '').replace(':', '').upper()
                  for client in clients for oauth in client.get('oauth_client', [])
                  if oauth.get('client_type') == 1}
    listed = sha1 in configured
    colon = lambda digest: ':'.join(digest[index:index + 2] for index in range(0, len(digest), 2))
    report = {
        'applicationId': args.package,
        'sha1': colon(sha1),
        'sha256': colon(sha256),
        'listedInCheckedInAndroidOAuthClients': listed,
        'backendVerificationRequired': True,
    }
    instructions = f'''# Beta signing registration

Package: `{args.package}`

SHA-1: `{report['sha1']}`

SHA-256: `{report['sha256']}`

Certificate listed in the checked-in Android OAuth clients: **{'yes' if listed else 'no'}**.
This is a configuration comparison, not proof that live Google sign-in or Firebase rules pass.

A Firebase project administrator must register these public fingerprints for the Android app,
check Google-provider and any Android API-key restrictions, then verify sign-in, profile saving,
community upload/download and the leaderboard with this exact APK. Download the updated
`google-services.json` if the Firebase Android client configuration changes.

For signing continuity across machines and cache eviction, configure the repository secret
`ANDROID_DEBUG_KEYSTORE_BASE64` with the team's approved debug-password keystore.
The private keystore is not included in this report and must not be committed or posted to chat.
Server certificate restrictions and database rules remain enforced.
'''
    args.output.mkdir(parents=True, exist_ok=True)
    (args.output / 'certificate-registration.json').write_text(json.dumps(report, indent=2) + '\n')
    (args.output / 'REGISTRATION.md').write_text(instructions)
    summary = os.environ.get('GITHUB_STEP_SUMMARY')
    if summary:
        with open(summary, 'a') as output:
            output.write(instructions + '\n')
    print(json.dumps(report, indent=2))
    if not listed:
        message = 'Beta certificate is absent from checked-in Android OAuth clients; Firebase administrator registration is required.'
        print(('::warning::' if os.environ.get('GITHUB_ACTIONS') else 'WARNING: ') + message)


if __name__ == '__main__':
    main()
