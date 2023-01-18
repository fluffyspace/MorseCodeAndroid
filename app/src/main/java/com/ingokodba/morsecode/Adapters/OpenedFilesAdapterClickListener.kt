package com.ingokodba.morsecode.Adapters

interface OpenedFilesAdapterClickListener {
    fun longHold(id: Int, position: Int)
    fun click(id: Int, uri: String)
}