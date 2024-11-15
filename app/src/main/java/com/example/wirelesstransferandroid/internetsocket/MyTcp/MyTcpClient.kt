package com.example.wirelesstransferandroid.internetsocket.MyTcp

import android.os.Handler
import com.example.wirelesstransferandroid.internetsocket.cmd.ClientInfoCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.Cmd
import com.example.wirelesstransferandroid.internetsocket.cmd.CmdDecoder
import com.example.wirelesstransferandroid.tools.InternetInfo
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.net.SocketAddress


enum class MyTcpClientState {
    Waiting,
    Connected,
    Disconnected,
}

class Indexes(var startIndex: Int = 0, var endIndex: Int = 0)

class MyTcpClient(clientIp: String, serverIp: String, port: Int, clientName: String) {
    private var onConnected: () -> Unit = {}
    fun setOnConnected(block: () -> Unit) {
        onConnected = block
    }

    private var onDisconnected: () -> Unit = {}
    fun setOnDisconnected(block: () -> Unit) {
        onDisconnected = block
    }

    private var onReceivedCmd: (Cmd) -> Unit = {}
    fun setOnReceivedCmd(block: (Cmd) -> Unit) {
        onReceivedCmd = block
    }

    var state: MyTcpClientState = MyTcpClientState.Disconnected
        private set

    lateinit var client: Socket
        private set


    val clientIp: String = clientIp
    val serverIp: String = serverIp
    val port: Int = port

    var clientName: String = clientName
        private set

    private var timeoutCounter: Int = 0
    private var indexes: Indexes = Indexes(0, 0)
    private var buffer: ByteArray = ByteArray(12582912) // 12MB
    private var tmpBuffer: ByteArray = ByteArray(6291456) // 6MB

    // Connect to server asynchronously.
    fun Connect() {
        if (state == MyTcpClientState.Disconnected) {
            state = MyTcpClientState.Waiting
            Thread {
                try {
                    client = Socket(serverIp, port)
                    val reader = client.getInputStream()
                    val writer = client.getOutputStream()

                    val clientInfoCmd = ClientInfoCmd(clientName, clientIp)
                    val bytes = clientInfoCmd.Encode()
                    writer.write(bytes)
                    writer.flush()

                    state = MyTcpClientState.Connected
                    onConnected.invoke()

                    while (true) {
                        var actualSize = reader.read(tmpBuffer, 0, tmpBuffer.size)
                        if (actualSize > 0)
                        {
                            var tmpSize = buffer.size - indexes.endIndex
                            if (actualSize <= tmpSize)
                            {
                                tmpBuffer.copyInto(buffer, indexes.endIndex, 0, actualSize)
                                indexes.endIndex += actualSize
                            }
                            else
                            {
                                tmpBuffer.copyInto(buffer, indexes.endIndex, 0, tmpSize - 1)
                                tmpBuffer.copyInto(buffer, indexes.endIndex, 0, actualSize - tmpSize - 1)
                                indexes.endIndex = actualSize - tmpSize
                            }

                            // prevent it doesn't only read one cmd
                            while (true)
                            {
                                val cmd: Cmd? = CmdDecoder.DecodeCmd(buffer, indexes)
                                if (cmd != null)
                                {
                                    onReceivedCmd.invoke(cmd)
                                    continue
                                }
                                break
                            }
                        }
                    }
                } catch (ex: Exception) {
                    if (state != MyTcpClientState.Disconnected) {
                        state = MyTcpClientState.Disconnected
                        onDisconnected.invoke()
                    }
                }
            }.start()
        }
    }

    fun SendCmd(cmd: Cmd) {
        try
        {
            val bytes = cmd.Encode()
            client.getOutputStream().write(bytes)
        }
        catch (ex: Exception)
        {
            if (state != MyTcpClientState.Disconnected)
            {
                state = MyTcpClientState.Disconnected
                onDisconnected.invoke()
            }
        }
    }

    fun Disconnect() {
        if (state != MyTcpClientState.Disconnected)
        {
            state = MyTcpClientState.Disconnected
            client.close()
            onDisconnected.invoke()
        }
    }
}