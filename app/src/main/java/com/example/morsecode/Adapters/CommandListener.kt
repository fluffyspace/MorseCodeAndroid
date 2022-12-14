package com.example.morsecode.Adapters

import com.example.morsecode.MorseCodeServiceCommands

interface CommandListener {
    fun commandChanged(command: MorseCodeServiceCommands)
    fun bottomCommand(command: MorseCodeServiceCommands)
    fun lastCharactersEntered(characters: String)
    fun lastCharactersVibrated(characters: String)
}