package com.example.wirelesstransferandroid.internetsocket.MyTcp

import com.example.wirelesstransferandroid.internetsocket.Indexes
import com.example.wirelesstransferandroid.internetsocket.cmd.ClientInfoCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.Cmd
import com.example.wirelesstransferandroid.internetsocket.cmd.CmdDecoder
import com.example.wirelesstransferandroid.internetsocket.cmd.CmdType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

enum class MyTcpServerState {
    Listening,
    Closed,
}

class MyTcpServer(val serverIp: String, val serverName: String, val port: Int) {
    private var onClientConnected: (MyTcpClientInfo) -> Unit = {}
    fun setOnClientConnected(block: (MyTcpClientInfo) -> Unit) {
        onClientConnected = block
    }

    private var onClientDisconnected: (MyTcpClientInfo) -> Unit = {}
    fun setOnClientDisconnected(block: (MyTcpClientInfo) -> Unit) {
        onClientDisconnected = block
    }

    private var onReceivedCmd: (Cmd, MyTcpClientInfo) -> Unit = {_, _ -> }
    fun setOnReceivedCmd(block: (Cmd, MyTcpClientInfo) -> Unit) {
        onReceivedCmd = block
    }

    var state: MyTcpServerState = MyTcpServerState.Closed
        private set

    var server: ServerSocket? = null
        private set

    var connectedClientList = ArrayList<MyTcpClientInfo>()
        private set

    private var indexes = Indexes(0, 0)
    private var buffer = ByteArray(12582912) // 12MB

    // Start the server and listening for client connections asynchronously.
    fun start(maxClient: Int) {
        state = MyTcpServerState.Listening
        Thread {
            try {
                server = ServerSocket(port)
                while (state == MyTcpServerState.Listening) {
                    val client = server!!.accept()
                    try {
                        val tmpBuffer = ByteArray(1024) // this is only for ClientInfoCmd
                        val actualLength = client.getInputStream().read(tmpBuffer)
                        if (actualLength > 0) {
                            val cmd = CmdDecoder.DecodeCmd(tmpBuffer, Indexes(0, actualLength - 1))
                            if (cmd != null && cmd.cmdType == CmdType.ClientInfo) {
                                val cic = cmd as ClientInfoCmd
                                val mtci = MyTcpClientInfo(client, cic.clientName, cic.ip)
                                connectedClientList.add(mtci)
                                onClientConnected.invoke(mtci)

                                Thread { clientReader(mtci) }.start()
                            }
                        }
                    } catch (_: Exception) { }

                    while (connectedClientList.count() >= maxClient){
                        Thread.sleep(1000)
                    }
                }
            } catch (ex: Exception) {
                if (state == MyTcpServerState.Listening) {
                    state = MyTcpServerState.Closed
                    server?.close()
                }
            }
        }.start()
    }

    private fun clientReader(clientInfo: MyTcpClientInfo) {
        try {
            while (state == MyTcpServerState.Listening) {
                var actualSize = clientInfo.client.inputStream.read(buffer, indexes.endIndex, buffer.size - indexes.endIndex)
                if (actualSize > 0) {
                    indexes.endIndex += actualSize
                    if (indexes.endIndex >= buffer.size)
                        indexes.endIndex -= buffer.size

                    // prevent it doesn't only read one cmd
                    while (true) {
                        val cmd: Cmd? = CmdDecoder.DecodeCmd(buffer, indexes)
                        if (cmd != null) {
                            onReceivedCmd.invoke(cmd, clientInfo)
                            continue
                        }
                        break
                    }
                }
            }
        } catch (ex: Exception) {
            clientInfo.client.close()
            if (connectedClientList.remove(clientInfo))
                onClientDisconnected.invoke(clientInfo)
        }
    }

    suspend fun sendCmd(cmd: Cmd, clientInfo: MyTcpClientInfo) {
        withContext(Dispatchers.IO) {
            if (state != MyTcpServerState.Listening) return@withContext

            try {
                clientInfo.client.outputStream.write(cmd.Encode())
                clientInfo.client.outputStream.flush()
            } catch (ex: Exception) {
                if (connectedClientList.remove(clientInfo))
                    onClientDisconnected.invoke(clientInfo)
            }
        }
    }

    fun stop() {
        if (state != MyTcpServerState.Listening) return

        state = MyTcpServerState.Closed
        for (clientInfo in connectedClientList) {
            clientInfo.client.close()
            connectedClientList.remove(clientInfo)
            onClientDisconnected.invoke(clientInfo)
        }

        server?.close()
        server = null
    }
}