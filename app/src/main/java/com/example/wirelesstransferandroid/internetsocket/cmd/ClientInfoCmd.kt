package com.example.wirelesstransferandroid.internetsocket.cmd

class ClientInfoCmd: Cmd {
    // Correct message format:
    //---------------------------------------------------------------------------------
    // data = clientNameLength + clientName + IP
    //---------------------------------------------------------------------------------
    // clientNameLength: 3 bytes to present (fill 0 in front if it not full).

    var clientName: String = ""
        private set

    var ip: String = ""
        private set

    /// For sender.
    constructor(clientName: String, ip: String) {
        this.clientName = clientName;
        this.ip = ip;
        cmdType = CmdType.ClientInfo;
    }

    /// For receiver.
    constructor(buffer: ByteArray) {
        data = buffer;
        cmdType = CmdType.ClientInfo;
    }

    override fun Encode(): ByteArray {
        val tmp: ByteArray = clientName.toByteArray(Charsets.UTF_8)
        data = (tmp.size.toString().padStart(3, '0') + clientName + ip).toByteArray(Charsets.UTF_8)
        return AddHeadTail(data)
    }

    override fun Decode() {
        val nameLength: Int = data.sliceArray(0..2).toString(Charsets.UTF_8).toInt()
        clientName = data.sliceArray(3..nameLength + 2).toString(Charsets.UTF_8)
        ip = data.sliceArray(nameLength + 3..<data.size).toString(Charsets.UTF_8)
    }
}