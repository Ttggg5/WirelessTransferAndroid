package com.example.wirelesstransferandroid.internetsocket.MyTcp

import android.os.Handler
import com.example.wirelesstransferandroid.internetsocket.cmd.ClientInfoCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.Cmd
import com.example.wirelesstransferandroid.internetsocket.cmd.CmdDecoder
import com.example.wirelesstransferandroid.internetsocket.cmd.RequestCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.RequestType
import com.example.wirelesstransferandroid.tools.InternetInfo
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.util.ArrayDeque
import javax.net.SocketFactory


enum class MyTcpClientState {
    Waiting,
    Connected,
    Disconnected,
}

class Indexes(var startIndex: Int = 0, var endIndex: Int = 0)

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

            val thread = Thread(runConnect())
            thread.start()
        }
    }

    fun runConnect() = Runnable {
        try {
            client = Socket(serverIp, port)
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
                client.close()
                onDisconnected.invoke()
            }
        }
    }

    fun sendCmd(cmd: Cmd) {
        if (state != MyTcpClientState.Connected) return

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

    fun disconnect() {
        synchronized(state) {
            if (state != MyTcpClientState.Disconnected) {
                state = MyTcpClientState.Disconnected
                client.close()
                onDisconnected.invoke()
            }
        }
    }
}