package com.example.wirelesstransferandroid.tools

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter
import android.util.Log
import androidx.compose.runtime.currentRecomposeScope
import com.example.wirelesstransferandroid.internetsocket.MyUdp.MyUdp
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections
import com.example.wirelesstransferandroid.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.Inet4Address
import java.net.SocketException
import java.util.Enumeration
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or


class InternetInfo {
    companion object {
        fun getPhoneIp(context: Context): String {
            if (isWifiOn(context)){
                try {
                    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    return Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
                }
                catch (ex: Exception) {
                    return "Not found"
                }
            }
            else {
                try {
                    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val networks = connectivityManager.allNetworks
                    for (network in networks) {
                        val capabilities = connectivityManager.getNetworkCapabilities(network)
                        if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                            try {
                                val networkInterfaces = NetworkInterface.getNetworkInterfaces()
                                for (networkInterface in networkInterfaces) {
                                    if (networkInterface.isUp && !networkInterface.isLoopback) {
                                        val addresses = networkInterface.inetAddresses
                                        while (addresses.hasMoreElements()) {
                                            val inetAddress = addresses.nextElement()
                                            if (inetAddress is Inet4Address)
                                                return inetAddress.hostAddress
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                return "Not found"
            }
        }

        fun getBroadcastIp(context: Context): String {
            if (isWifiOn(context))
                return "255.255.255.255"

            var ip = ""
            try {
                val enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces()
                while (enumNetworkInterfaces.hasMoreElements()) {
                    val networkInterface = enumNetworkInterfaces.nextElement()
                    val enumInetAddress = networkInterface.getInetAddresses()
                    while (enumInetAddress.hasMoreElements()) {
                        val inetAddress = enumInetAddress.nextElement()

                        if (inetAddress.isSiteLocalAddress) {
                            val tmp = inetAddress.getHostAddress().split(".")
                            ip = String.format("%s.%s.%s.255", tmp[0], tmp[1], tmp[2])
                        }
                    }
                }

            } catch (ex: SocketException) {
                ex.printStackTrace()
                ip = "255.255.255.255"
            }

            return ip
        }

        fun isWifiOn(context: Context): Boolean {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                return wifiManager.isWifiEnabled
            }
            catch (ex: Exception) {
                return false
            }
        }

        fun getWifiInfo(context: Context): WifiInfo? {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                return wifiManager.connectionInfo
            }
            catch (ex: Exception) {
                return null
            }
        }

        fun getWifiSSID(context: Context): String? {
            val wifiInfo = getWifiInfo(context)
            if (wifiInfo != null) {
                return wifiInfo.ssid
            }
            return null
        }

        fun isWifiHotspotOn(context: Context): Boolean {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
                return method.invoke(wifiManager) as Boolean
            } catch (ex: Exception) {

            }

            return false
        }
    }
}