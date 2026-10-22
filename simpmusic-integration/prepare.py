#!/usr/bin/env python3
"""Prepare a pinned, patched native SimpMusic build without changing either upstream checkout."""
import argparse
import io
import json
import re
from pathlib import Path
import shutil
import subprocess
import tarfile
import tempfile

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent

def run(*args, cwd=None, capture=False):
    return subprocess.run(args, cwd=cwd, check=True, stdout=subprocess.PIPE if capture else None).stdout

def archive(repo, commit, destination):
    run('git', 'cat-file', '-e', commit + '^{commit}', cwd=repo)
    data = run('git', 'archive', commit, cwd=repo, capture=True)
    destination.mkdir(parents=True, exist_ok=True)
    with tarfile.open(fileobj=io.BytesIO(data)) as source:
        source.extractall(destination, filter='data')

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--simpmusic-source', type=Path, help='Optional existing Git checkout, used read-only')
    parser.add_argument('--core-source', type=Path, help='Optional existing Git checkout, used read-only')
    args = parser.parse_args()
    lock = json.loads((HERE / 'upstream.lock.json').read_text())
    output = ROOT / '.simpmusic-build'
    marker = output / '.glyphix-generated'
    if output.exists() and not marker.exists():
        raise SystemExit('Refusing to replace an unrecognised .simpmusic-build directory.')
    with tempfile.TemporaryDirectory(prefix='.simpmusic-prepare-', dir=ROOT) as scratch:
        scratch = Path(scratch)
        source = scratch / 'prepared'
        for name, checkout, target in [('simpmusic', args.simpmusic_source, source), ('core', args.core_source, source / 'core')]:
            if checkout is None:
                checkout = scratch / (name + '-git')
                run('git', 'init', str(checkout))
                run('git', '-C', str(checkout), 'fetch', '--depth=1', lock[name]['url'], lock[name]['commit'])
            archive(checkout.resolve(), lock[name]['commit'], target)
        for name, target in [('simpmusic', source), ('core', source / 'core')]:
            # Isolate git apply from the enclosing Glyphix repository. Without this,
            # Git can silently skip paths outside the current repository prefix.
            run('git', 'init', '--quiet', str(target))
            patch = HERE / 'patches' / (name + '.patch')
            run('git', 'apply', '--check', str(patch), cwd=target)
            run('git', 'apply', str(patch), cwd=target)
        # API 37 is published as android-37.0. Use the public AGP CompileSdkSpec
        # for both normal Android libraries and KMP libraries (which have no compileSdkMinor property).
        for script in source.rglob('build.gradle.kts'):
            text = script.read_text()
            text = re.sub(r'(?m)^([ \t]*)compileSdk = 37[ \t]*$',
                          r'\1compileSdk { version = release(37) { minorApiLevel = 0 } }', text)
            script.write_text(text)
        # Reuse all library modules and generated project accessors, replacing only app shells.
        settings = (source / 'settings.gradle.kts').read_text()
        for module in ['androidApp', 'vivoAndroidApp', 'desktopApp']:
            settings = settings.replace('    ":' + module + '",\n', '')
        settings += '\ninclude(":glyphix")\nproject(":glyphix").projectDir = file("../../app")\nproject(":glyphix").buildFileName = "build-simpmusic.gradle"\n'
        # Prepared build lives at ROOT/.simpmusic-build, so ../app is the host module.
        settings = settings.replace('file("../../app")', 'file("../app")')
        (source / 'settings.gradle.kts').write_text(settings)
        build = (source / 'build.gradle.kts').read_text().replace('plugins {', 'plugins {\n    id("com.google.gms.google-services") version "4.4.4" apply false', 1)
        (source / 'build.gradle.kts').write_text(build)
        properties = (source / 'gradle.properties').read_text()
        properties = properties.replace('isFullBuild=true', 'isFullBuild=false').replace('org.gradle.unsafe.configuration-cache=true', 'org.gradle.configuration-cache=false')
        (source / 'gradle.properties').write_text(properties)
        # Use the caller's JDK 21 instead of forcing upstream's Microsoft vendor.
        (source / 'gradle/gradle-daemon-jvm.properties').unlink(missing_ok=True)
        # Android SDK path only. Do not copy credentials or unrelated local configuration.
        local = ROOT / 'local.properties'
        if local.exists():
            sdk = [line for line in local.read_text().splitlines() if line.startswith('sdk.dir=')]
            (source / 'local.properties').write_text('\n'.join(sdk) + '\n')
        (source / '.glyphix-generated').write_text(json.dumps(lock, indent=2) + '\n')
        if output.exists():
            shutil.rmtree(output)
        shutil.move(str(source), str(output))
    print('Prepared pinned native music sources. Build with:')
    print('  bash .simpmusic-build/gradlew -p .simpmusic-build :glyphix:assembleDebug')

if __name__ == '__main__':
    main()
