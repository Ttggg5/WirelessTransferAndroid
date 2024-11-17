package com.example.wirelesstransferandroid.internetsocket.cmd

enum class ReplyType {
    Accept,
    Refuse,
}

class ReplyCmd: Cmd {
    // Correct message format:
    //---------------------------------------------------------------------------------
    // data = replyType
    //---------------------------------------------------------------------------------

    lateinit var replyType: ReplyType
        private set

    // For sender.
    constructor(replyType: ReplyType)
    {
        this.replyType = replyType
        cmdType = CmdType.Reply
    }

    // For receiver.
    constructor(buffer: ByteArray)
    {
        data = buffer
        cmdType = CmdType.Reply
    }

    override fun Encode(): ByteArray {
        data = replyType.name.toByteArray(Charsets.US_ASCII)
        return AddHeadTail(data)
    }

    override fun Decode()
    {
        val tmp = data.toString(Charsets.US_ASCII)
        try {
            replyType = ReplyType.valueOf(tmp)
        } catch (ex: IllegalArgumentException) {
            replyType = ReplyType.Refuse
        }
    }
}