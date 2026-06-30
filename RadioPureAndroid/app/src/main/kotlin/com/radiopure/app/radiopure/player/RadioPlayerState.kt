package com.radiopure.app.radiopure.player

import com.radiopure.app.radiopure.model.RadioStation

data class RadioPlayerState(
    val currentStation: RadioStation? = null,
    val isPlaying: Boolean = false,
    val isError: Boolean = false,
    val volume: Float = 0.7f,
)
