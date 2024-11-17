package com.example.wirelesstransferandroid.internetsocket.MyUdp

import android.net.IpSecManager.UdpEncapsulationSocket
import com.example.wirelesstransferandroid.internetsocket.cmd.Cmd
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketAddress

class MyUdp(private var port: Int) {
    private var listenSocket = DatagramSocket(port)

    private lateinit var listenPacket : DatagramPacket
    private lateinit var sendPacket : DatagramPacket
    val buffer = ByteArray(1024)

    fun send(bytes: ByteArray, ip: String){
        sendPacket = DatagramPacket(bytes, bytes.size, InetAddress.getByName(ip), port)
        listenSocket.send(sendPacket)
    }

    fun receive(): ByteArray {
        val receiverPacket = DatagramPacket(buffer, buffer.size)
        listenSocket.receive(receiverPacket)

        listenPacket = receiverPacket
        val result = ByteArray(listenPacket.length)
        listenPacket.data.copyInto(result, 0, 0, listenPacket.length)
        return result
    }

    fun getSenderIP(): String {
        return listenPacket.address.hostName
    }

    fun close() {
        listenSocket.close()
    }
}