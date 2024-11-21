package com.example.wirelesstransferandroid.internetsocket.cmd

class MouseMoveCmd: Cmd {
    // Correct message format:
    //---------------------------------------------------------------------------------
    // data = MouseDisplacementX + "," + MouseDisplacementY
    //---------------------------------------------------------------------------------

    var MouseDisplacementX = 0
        private set

    var MouseDisplacementY = 0
        private set

    // For sender.
    constructor(mouseDisplacementX: Int, mouseDisplacementY: Int) {
        MouseDisplacementX = mouseDisplacementX
        MouseDisplacementY = mouseDisplacementY
        cmdType = CmdType.MouseMove
    }

    // For receiver.
    constructor(buffer: ByteArray) {
        data = buffer
        cmdType = CmdType.MouseMove
    }

    override fun Encode(): ByteArray {
        data = (MouseDisplacementX.toString() + "," + MouseDisplacementY.toString()).toByteArray(Charsets.US_ASCII)
        return AddHeadTail(data)
    }

    override fun Decode() {
        val tmp = data.toString(Charsets.US_ASCII).split(",")
        MouseDisplacementX = tmp[0].toInt()
        MouseDisplacementY = tmp[0].toInt()
    }
}