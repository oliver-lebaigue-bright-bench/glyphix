package com.glyphix.app.music

import com.glyphix.app.ui.Tab

fun primaryGlyphixTabs(): List<Tab> = buildList {
    add(Tab.Audio)
    if (MusicIntegration.available) add(Tab.Music)
    add(Tab.Leaderboard)
    add(Tab.Settings)
    add(Tab.Info)
}
