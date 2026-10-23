# SPDX-License-Identifier: GPL-3.0-only
"""Adapt pinned Android-only music widgets and backup workers to the Glyphix host."""
from pathlib import Path
import copy
import re
import shutil
import xml.etree.ElementTree as ET


def prepare_android_extras(source: Path, host: Path):
    upstream = source / 'androidApp/src/main'
    output = source / 'glyphix-android-extras'
    java = output / 'src'
    resources = output / 'res'
    java.mkdir(parents=True)
    resources.mkdir(parents=True)
    refs = {('xml', name) for name in ('turntable_widget_info', 'playlists_widget_info', 'insights_widget_info')}
    refs.update({('string', 'widget_' + name + '_label') for name in ('turntable', 'playlists', 'insights')})
    for folder in ('ui/widget', 'service/backup'):
        for original in (upstream / 'java/com/maxrave/simpmusic' / folder).glob('*.kt'):
            text = original.read_text()
            text = text.replace('import com.maxrave.simpmusic.MainActivity', 'import com.glyphix.app.ui.MainActivity')
            text = text.replace('import com.maxrave.simpmusic.R\n', 'import com.glyphix.app.R\n')
            text = text.replace('R.drawable.holder', 'R.mipmap.ic_launcher')
            if 'actionStartActivity<MainActivity>()' in text:
                if 'import android.content.Intent\n' not in text:
                    text = text.replace('import android.content.Context\n', 'import android.content.Context\nimport android.content.Intent\n')
                text = text.replace('actionStartActivity<MainActivity>()',
                    'actionStartActivity(Intent(context, MainActivity::class.java).setAction("androidx.car.app.media.action.SHOW_MEDIA_PLAYBACK"))')
            # Keep Glyphix backup retention separate from another installed SimpMusic app.
            text = text.replace('simpmusic_backup_', 'glyphix_music_backup_')
            text = text.replace('Download/SimpMusic', 'Download/Glyphix/Music')
            text = text.replace('Downloads/SimpMusic', 'Downloads/Glyphix/Music')
            if original.name == 'AutoBackupWorker.kt':
                # Downloads MediaStore APIs only exist on API 29+. Earlier supported
                # phones use the app-owned external downloads directory without permissions.
                text = text.replace(
                    """        return try {
            val contentValues""",
                    """        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val root = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: return false
            val directory = File(root, "Music").apply { mkdirs() }
            return try { backupFile.copyTo(File(directory, fileName), overwrite = false); true }
            catch (error: Exception) { Logger.e(TAG, "Cannot save music backup: ${error.message}"); false }
        }
        return try {
            val contentValues"""
                )
                text = text.replace(
                    """    private fun cleanupOldBackups(maxFiles: Int) {
        try {""",
                    """    private fun cleanupOldBackups(maxFiles: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val root = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: return
            File(root, "Music").listFiles { file -> file.name.startsWith("glyphix_music_backup_") && file.extension == "zip" }
                ?.sortedByDescending { it.lastModified() }?.drop(maxFiles.coerceAtLeast(1))?.forEach { it.delete() }
            return
        }
        try {"""
                )
                text = text.replace('context.contentResolver.openOutputStream(outputUri)?.use', 'requireNotNull(context.contentResolver.openOutputStream(outputUri)).use')
            (java / original.name).write_text('// Adapted from pinned SimpMusic Android source; see upstream.lock.json and GPL notices.\n' + text)
            refs.update(re.findall(r'(?<![\w.])R\.([a-z_]+)\.([A-Za-z0-9_]+)', text))

    upstream_res = upstream / 'res'
    host_res = host / 'app/src/main/res'
    def value_index(root):
        entries = {}
        for file in (root / 'values').glob('*.xml'):
            for node in ET.parse(file).getroot():
                name = node.get('name')
                if name:
                    kind = {'string-array': 'array', 'integer-array': 'array'}.get(node.tag, node.tag)
                    if node.tag == 'item': kind = node.get('type', kind)
                    entries[(kind, name)] = node
        return entries
    values = value_index(upstream_res)
    host_values = value_index(host_res)
    bundled_values = ET.Element('resources')
    done = set()
    pattern = re.compile(r'@([a-z_]+)/([A-Za-z0-9_]+)')
    def files_for(root, kind, name):
        return [file for directory in root.glob(kind + '*') if directory.is_dir()
                for file in directory.glob(name + '.*') if file.is_file()]
    def resolve(kind, name):
        key = (kind, name)
        if key in done or kind in ('android', 'id'): return
        done.add(key)
        if key in host_values or files_for(host_res, kind, name): return
        if key in values:
            node = copy.deepcopy(values[key])
            if kind == 'string' and name.endswith('_label'):
                node.text = 'Glyphix Music · ' + (node.text or '')
            bundled_values.append(node)
            for ref in pattern.findall(ET.tostring(node, encoding='unicode')): resolve(*ref)
            return
        files = files_for(upstream_res, kind, name)
        if not files: raise ValueError('Unresolved music widget resource: ' + kind + '/' + name)
        for file in files:
            target = resources / file.relative_to(upstream_res)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(file, target)
            if file.suffix == '.xml':
                for ref in pattern.findall(file.read_text()): resolve(*ref)
    for key in sorted(refs): resolve(*key)
    target = resources / 'values/glyphix_music_widgets.xml'
    target.parent.mkdir(parents=True, exist_ok=True)
    ET.indent(bundled_values)
    ET.ElementTree(bundled_values).write(target, encoding='unicode', xml_declaration=True)
    return len(list(java.glob('*.kt'))), len(done)
