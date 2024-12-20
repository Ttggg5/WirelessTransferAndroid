package com.example.wirelesstransferandroid.internetsocket.cmd

class FileDataCmd: Cmd {
    // Correct message format:
    //---------------------------------------------------------------------------------
    // data = fileNameLength + fileName + FileData
    //---------------------------------------------------------------------------------
    // fileNameLength: 3 bytes to present (fill 0 in front if it not full).

    lateinit var fileName: String
        private set

    lateinit var fileData: ByteArray
        private set

    // For sender.
    constructor(fileName: String, fileData: ByteArray) {
        this.fileName = fileName
        this.fileData = fileData
        cmdType = CmdType.FileData
    }

    // For receiver.
    constructor(buffer: ByteArray) {
        data = buffer
        cmdType = CmdType.FileData
    }

    override fun Encode(): ByteArray {
        val tmp = fileName.toByteArray(Charsets.UTF_8)
        data = (tmp.size.toString().padStart(3, '0') + fileName).toByteArray(Charsets.UTF_8).plus(fileData)
        return AddHeadTail(data)
    }

    override fun Decode() {
        val nameLength: Int = data.sliceArray(0..2).toString(Charsets.UTF_8).toInt()
        fileName = data.sliceArray(3..nameLength + 2).toString(Charsets.UTF_8)
        fileData = data.sliceArray(nameLength + 3..data.size - 1)
    }
}