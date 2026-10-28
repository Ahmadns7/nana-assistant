package com.nana.ui.main

/** Twenty one-tap phone utilities shown on the home dashboard. */
enum class QuickFeature(
    val title: String,
    val detail: String,
    val symbol: String
) {
    PREVIOUS_TRACK("Previous track", "Media", "⏮"),
    VOLUME_UP("Volume up", "Media", "＋"),
    VOLUME_DOWN("Volume down", "Media", "−"),
    NOW_PLAYING("Now playing", "Media", "♫"),
    FLASHLIGHT("Flashlight", "Tools", "◉"),
    BATTERY("Battery status", "Device", "⚡"),
    STORAGE("Storage", "Device", "▤"),
    PHONE("Phone", "Open app", "☎"),
    MESSAGES("Messages", "Open app", "✉"),
    CAMERA("Camera", "Open app", "▣"),
    MAPS("Maps", "Open app", "⌖"),
    CLOCK("Clock", "Open app", "◷"),
    CALENDAR("Calendar", "Open app", "▦"),
    CONTACTS("Contacts", "Open app", "♙"),
    CALCULATOR("Calculator", "Open app", "％"),
    BROWSER("Browser", "Open app", "◎"),
    WIFI("Wi-Fi settings", "Connectivity", "⌁"),
    BLUETOOTH("Bluetooth", "Connectivity", "ᛒ"),
    DND("Do Not Disturb", "Focus", "☾"),
    DISPLAY("Display & brightness", "Device", "☼")
}
