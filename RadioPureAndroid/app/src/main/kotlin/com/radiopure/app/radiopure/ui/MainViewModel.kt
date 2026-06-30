package com.radiopure.app.radiopure.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radiopure.app.radiopure.model.RadioStation
import com.radiopure.app.radiopure.player.RadioPlayer
import com.radiopure.app.radiopure.player.RadioPlayerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _playerState = MutableStateFlow(RadioPlayerState())
    val playerState: StateFlow<RadioPlayerState> = _playerState.asStateFlow()

    private var radioPlayer: RadioPlayer? = null
    private var collectJob: Job? = null

    fun attachRadioPlayer(player: RadioPlayer) {
        if (radioPlayer === player) return
        collectJob?.cancel()
        radioPlayer = player
        collectJob = viewModelScope.launch {
            player.state.collect { _playerState.value = it }
        }
    }

    fun detachRadioPlayer() {
        collectJob?.cancel()
        collectJob = null
        radioPlayer = null
        _playerState.value = RadioPlayerState()
    }

    fun play(station: RadioStation) {
        radioPlayer?.play(station)
    }

    fun togglePlayPause() {
        radioPlayer?.togglePlayPause()
    }

    fun setVolume(volume: Float) {
        radioPlayer?.setVolume(volume)
    }
}
