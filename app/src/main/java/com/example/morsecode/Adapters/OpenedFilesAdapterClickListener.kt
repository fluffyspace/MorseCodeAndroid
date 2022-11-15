package com.example.morsecode.Adapters

interface OpenedFilesAdapterClickListener {
    fun longHold(id: Int, position: Int)
    fun click(id: Int, uri: String)
}