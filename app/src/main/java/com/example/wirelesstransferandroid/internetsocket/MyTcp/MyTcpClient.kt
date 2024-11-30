package com.example.wirelesstransferandroid.internetsocket.MyTcp

import com.example.wirelesstransferandroid.internetsocket.Indexes
import com.example.wirelesstransferandroid.internetsocket.cmd.ClientInfoCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.Cmd
import com.example.wirelesstransferandroid.internetsocket.cmd.CmdDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException


enum class MyTcpClientState {
    Waiting,
    Connected,
    Disconnected,
}

class MyTcpClient(val clientIp: String, val serverIp: String, val port: Int, clientName: String) {
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

    lateinit var reader: InputStream
        private set

    lateinit var writer: OutputStream
        private set

    var clientName: String = clientName
        private set

    private var indexes = Indexes(0, 0)
    private var buffer = ByteArray(12582912) // 12MB

    // Connect to server asynchronously.
    fun connect() {
        if (state == MyTcpClientState.Disconnected) {
            state = MyTcpClientState.Waiting

            Thread(runConnect).start()
        }
    }

    private val runConnect = Runnable {
        while (state == MyTcpClientState.Waiting) {
            try {
                var count = 0
                while (true){
                    try {
                        client = Socket()
                        client.connect(InetSocketAddress(serverIp, port))
                        break
                    } catch (ex: Exception) {
                        if (count++ == 50)
                            throw Exception("no server found")
                        Thread.sleep(100)
                    }
                }
                reader = client.getInputStream()
                writer = client.getOutputStream()

                // send ClientInfo
                val clientInfoCmd = ClientInfoCmd(clientName, clientIp)
                writer.write(clientInfoCmd.Encode())
                writer.flush()

                state = MyTcpClientState.Connected
                onConnected.invoke()

                // start reader
                while (true) {
                    var actualSize = reader.read(buffer, indexes.endIndex, buffer.size - indexes.endIndex)
                    if (actualSize > 0) {
                        indexes.endIndex += actualSize
                        if (indexes.endIndex >= buffer.size)
                            indexes.endIndex -= buffer.size

                        // prevent it doesn't only read one cmd
                        while (true) {
                            val cmd: Cmd? = CmdDecoder.DecodeCmd(buffer, indexes)
                            if (cmd != null) {
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
                    client?.close()
                    onDisconnected.invoke()
                }
            }
        }
    }

    suspend fun sendCmd(cmd: Cmd) {
        withContext(Dispatchers.IO) {
            if (state != MyTcpClientState.Connected) return@withContext

            try {
                writer.write(cmd.Encode())
                writer.flush()
            } catch (ex: Exception) {
                if (state != MyTcpClientState.Disconnected) {
                    state = MyTcpClientState.Disconnected
                    onDisconnected.invoke()
                }
            }
        }
    }

    fun disconnect() {
        synchronized(state) {
            if (state != MyTcpClientState.Disconnected) {
                state = MyTcpClientState.Disconnected
                client?.close()
                onDisconnected.invoke()
            }
        }
    }
}