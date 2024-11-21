package com.example.wirelesstransferandroid.internetsocket.cmd

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import java.io.ByteArrayOutputStream



class ScreenCmd: Cmd {
    // Correct message format:
    //---------------------------------------------------------------------------------
    // data = screenBmp
    //---------------------------------------------------------------------------------
    // screenBmp: bitmap in byte[].

    var screenBmp: Bitmap? = null
        private set

    // For sender.
    constructor(screenBmp: Bitmap) {
        this.screenBmp = screenBmp
        cmdType = CmdType.Screen
    }

    // For receiver.
    constructor(buffer: ByteArray) {
        data = buffer
        cmdType = CmdType.Screen
    }

    override fun Encode(): ByteArray {
        val stream = ByteArrayOutputStream()
        screenBmp?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        data = stream.toByteArray()
        screenBmp?.recycle()
        return AddHeadTail(data)
    }

    override fun Decode()
    {
        try {
            screenBmp = BitmapFactory.decodeByteArray(data, 0, data.size)
        } catch(ex: Exception) {
            screenBmp = null
        }
    }
}