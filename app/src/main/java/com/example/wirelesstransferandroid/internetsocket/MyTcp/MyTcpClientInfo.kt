package com.example.wirelesstransferandroid.internetsocket.MyTcp

import java.net.Socket

class MyTcpClientInfo(client: Socket, name: String, ip: String) {
    var client: Socket = client
        private set
    var name: String = name
        private set
    var ip: String = ip
        private set
}