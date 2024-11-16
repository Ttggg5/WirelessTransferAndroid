package com.example.wirelesstransferandroid.internetsocket.cmd

enum class CmdType {
    None,
    Alive,
    ClientInfo,
    FileData,
    FileInfo,
    Reply,
    Screen,
    Keyboard,
    Mouse,
    Webcam,
    Request,
    ScreenInfo,
}

abstract class Cmd
{
    // Correct message format:
    //---------------------------------------------------------------------------------
    // <CmdType> + DataLength + Data + <!CmdType>
    //---------------------------------------------------------------------------------
    // CmdType: no limit.
    // DataLength: 7 byte(max: 5242880 bytes = 5MB), fill 0 in front if it not full.
    // Data: max length is 5242880 bytes.

    var cmdType: CmdType = CmdType.None
        protected set

    var data: ByteArray = byteArrayOf() // not included head and tail
        protected set

    abstract fun Encode(): ByteArray

    abstract fun Decode()

    protected fun AddHeadTail(data: ByteArray): ByteArray {
        val cmdString: String = cmdType.name
        val headBytes: ByteArray = ("<$cmdString>").toByteArray(Charsets.US_ASCII)
        val tailBytes: ByteArray = ("<!$cmdString>").toByteArray(Charsets.US_ASCII)
        val dataLengthBytes: ByteArray = (data.size.toString().padStart(7, '0')).toByteArray(Charsets.US_ASCII)

        val fullBytes: ByteArray = headBytes.plus(dataLengthBytes).plus(data).plus(tailBytes)
        return fullBytes
    }
}