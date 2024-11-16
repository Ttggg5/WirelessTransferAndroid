package com.example.wirelesstransferandroid.internetsocket.MyUdp

import com.example.wirelesstransferandroid.internetsocket.cmd.Cmd
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class MyUdp(private var port: Int) {
    private var listenSocket = DatagramSocket(port)

    private lateinit var listenPacket : DatagramPacket
    private lateinit var sendPacket : DatagramPacket
    val buffer = ByteArray(1024)

    fun send(cmd: Cmd, ip: String){
        send(cmd.Encode(), ip)
    }
    fun send(bytes: ByteArray, ip: String){
        sendPacket = DatagramPacket(bytes,bytes.size, InetAddress.getByName(ip), port)
        listenSocket.send(sendPacket)
    }

    fun receive(): ByteArray {
        listenPacket = recv(listenSocket, buffer)
        return listenPacket.data
    }

    fun getSenderIP(): String {
        return listenPacket.address.hostName
    }

    private fun recv(socket: DatagramSocket, receiveBuf: ByteArray): DatagramPacket {
        val receiverPacket = DatagramPacket(receiveBuf, receiveBuf.size)
        socket.receive(receiverPacket)
        return receiverPacket
    }

    fun close(){
        listenSocket.close()
        listenSocket.close()
    }
}