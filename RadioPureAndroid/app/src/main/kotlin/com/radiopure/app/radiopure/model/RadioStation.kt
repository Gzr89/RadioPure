package com.radiopure.app.radiopure.model

data class RadioStation(
    val name: String,
    val url: String,
    val fallbackURL: String? = null,
    val emoji: String,
) {
    val id: String get() = name
}
