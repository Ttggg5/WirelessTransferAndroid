package com.example.wirelesstransferandroid.internetsocket.cmd

class ScreenInfoCmd: Cmd {
    // Correct message format:
    //---------------------------------------------------------------------------------
    // data = width + "," + height
    //---------------------------------------------------------------------------------

    var width = 0
        private set

    var height = 0
        private set

    // For sender.
    constructor(width: Int, height: Int)
    {
        this.width = width
        this.height = height
        cmdType = CmdType.ScreenInfo
    }

    // For receiver.
    constructor(buffer: ByteArray)
    {
        data = buffer
        cmdType = CmdType.ScreenInfo
    }

    override fun Encode(): ByteArray
    {
        data = (width.toString() + "," + height.toString()).toByteArray(Charsets.US_ASCII)
        return AddHeadTail(data)
    }

    override fun Decode()
    {
        val tmp = data.toString(Charsets.US_ASCII).split(",")
        width = tmp[0].toInt()
        height = tmp[1].toInt()
    }
}