package com.ingokodba.morsecode.Adapters

import com.ingokodba.morsecode.MorseCodeServiceCommands

interface CommandListener {
    fun commandChanged(command: MorseCodeServiceCommands)
    fun bottomCommand(command: MorseCodeServiceCommands)
    fun lastCharactersEntered(characters: String)
    fun lastCharactersVibrated(characters: String)
}