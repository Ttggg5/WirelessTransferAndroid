package com.example.wirelesstransferandroid.toolmodules

import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.text.format.Formatter


class InternetInfo {
    companion object {
        fun getPhoneIp(context: Context): String {
            try {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                return Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
            }
            catch (ex: Exception) {
                return "Not found"
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

        fun getSSID(context: Context): String? {
            val wifiInfo = getWifiInfo(context)
            if (wifiInfo != null) {
                return wifiInfo.ssid
            }
            return null
        }
    }
}