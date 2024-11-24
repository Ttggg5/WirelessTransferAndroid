package com.example.wirelesstransferandroid.internetsocket.cmd

import android.graphics.Point
import android.graphics.PointF

enum class MouseAct {
    RightButtonDown,
    RightButtonUp,
    LeftButtonDown,
    LeftButtonUp,
    MiddleButtonDown,
    MiddleButtonUp,
    MiddleButtonRolled,
    None,
}

class MouseCmd: Cmd {
    // Correct message format:
    //---------------------------------------------------------------------------------
    // data = mousePos.X + "," + mousePos.Y + "," + mouseAct + "," + middleButtonMomentum + "," + moveMouse
    //---------------------------------------------------------------------------------

    lateinit var mousePos: PointF
        private set

    lateinit var mouseAct: MouseAct
        private set

    var middleButtonMomentum = 0
        private set

    var moveMouse = true
        private set

    // For sender.
    constructor(mousePos: PointF, mouseAct: MouseAct, middleButtonMomentum: Int, moveMouse: Boolean) {
        this.mousePos = mousePos
        this.mouseAct = mouseAct
        this.middleButtonMomentum = middleButtonMomentum
        this.moveMouse = moveMouse
        cmdType = CmdType.Mouse
    }

    // For receiver.
    constructor(buffer: ByteArray) {
        data = buffer
        cmdType = CmdType.Mouse
    }

    override fun Encode(): ByteArray {
        data = "${mousePos.x},${mousePos.y},${mouseAct.name},${middleButtonMomentum},${moveMouse}".toByteArray(Charsets.US_ASCII)
        return AddHeadTail(data)
    }

    override fun Decode() {
        val tmp = data.toString(Charsets.US_ASCII).split(",")
        mousePos = PointF(tmp[0].toFloat(), tmp[1].toFloat())
        try {
            mouseAct = MouseAct.valueOf(tmp[2])
        } catch (ex: IllegalArgumentException) {
            mouseAct = MouseAct.None
        }
        middleButtonMomentum = tmp[3].toInt()
        moveMouse = tmp[4].toBoolean()
    }
}