package com.example.wirelesstransferandroid.internetsocket.cmd

import java.nio.channels.FileLock

class FileInfoCmd: Cmd {
    // Correct message format:
    //---------------------------------------------------------------------------------
    // data = fileNameLength + fileName + fileSize + "," + md5
    //---------------------------------------------------------------------------------
    // fileNameLength: 3 bytes to present (fill 0 in front if it not full).

    lateinit var fileName: String
        private set

    var fileSize = 0L
        private set

    lateinit var md5: String
        private set

    // For sender.
    constructor(fileName: String, fileSize: Long, md5: String) {
        this.fileName = fileName
        this.fileSize = fileSize
        this.md5 = md5
        cmdType = CmdType.FileInfo
    }

    // For receiver.
    constructor(buffer: ByteArray) {
        data = buffer
        cmdType = CmdType.FileInfo
    }

    override fun Encode(): ByteArray {
        val tmp = fileName.toByteArray(Charsets.UTF_8)
        data = (tmp.size.toString().padStart(3, '0') + fileName + fileSize + "," + md5)
            .toByteArray(Charsets.UTF_8)
        return AddHeadTail(data)
    }

    override fun Decode() {
        val nameLength: Int = data.sliceArray(0..2).toString(Charsets.UTF_8).toInt()
        fileName = data.sliceArray(3..nameLength + 2).toString(Charsets.UTF_8)
        val tmp = data.sliceArray(nameLength + 3..data.size - 1).toString(Charsets.UTF_8).split(",")
        try {
            fileSize = tmp[0].toLong()
        } catch (ex: IllegalArgumentException) {

        }
        md5 = tmp[1]
    }
}