package com.example.wirelesstransferandroid.internetsocket.cmd

import java.util.Locale

class MouseMoveCmd: Cmd {
    // Correct message format:
    //---------------------------------------------------------------------------------
    // data = MouseDisplacementX + "," + MouseDisplacementY
    //---------------------------------------------------------------------------------

    var MouseDisplacementX = 0f
        private set

    var MouseDisplacementY = 0f
        private set

    // For sender.
    constructor(mouseDisplacementX: Float, mouseDisplacementY: Float) {
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
        data = String.format(Locale.getDefault(), "%f,%f", MouseDisplacementX, MouseDisplacementY).toByteArray(Charsets.US_ASCII)
        return AddHeadTail(data)
    }

    override fun Decode() {
        val tmp = data.toString(Charsets.US_ASCII).split(",")
        MouseDisplacementX = tmp[0].toFloat()
        MouseDisplacementY = tmp[1].toFloat()
    }
}