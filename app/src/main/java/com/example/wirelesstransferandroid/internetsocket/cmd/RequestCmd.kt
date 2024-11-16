package com.example.wirelesstransferandroid.internetsocket.cmd

enum class RequestType {
    None,
    Disconnect,
    PcClientInfo,
    Mirror,
    Extend,
    FileShare,
    PhoneClientInfoShareScreen,
    PhoneClientInfoFileShare,
}

class RequestCmd: Cmd {
    // Correct message format:
    //---------------------------------------------------------------------------------
    // data = requestType + "," + deviceName
    //---------------------------------------------------------------------------------
    // requestType: RequestType's string value.

    lateinit var requestType: RequestType
        private set

    lateinit var deviceName: String
        private set

    /// For sender.
    constructor(requestType: RequestType, deviceName: String) {
        this.requestType = requestType
        this.deviceName = deviceName
        cmdType = CmdType.Request
    }

    // For receiver.
    constructor(buffer: ByteArray) {
        data = buffer;
        cmdType = CmdType.Request;
    }

    override fun Encode(): ByteArray {
        data = (requestType.name + "," + deviceName).toByteArray(Charsets.UTF_8)
        return AddHeadTail(data)
    }

    override fun Decode() {
        val tmp = data.toString(Charsets.UTF_8).split(",")
        try {
            requestType = RequestType.valueOf(tmp[0])
        } catch (ex: IllegalArgumentException) {
            requestType = RequestType.None
        }
        deviceName = tmp[1]
    }
}