package com.example.wirelesstransferandroid

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Resources.Theme
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.wirelesstransferandroid.databinding.FragmentDisplayScreenBinding
import com.example.wirelesstransferandroid.internetsocket.MyTcp.MyTcpClient
import com.example.wirelesstransferandroid.internetsocket.MyTcp.MyTcpClientState
import com.example.wirelesstransferandroid.internetsocket.cmd.CmdType
import com.example.wirelesstransferandroid.internetsocket.cmd.RequestCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.RequestType
import com.example.wirelesstransferandroid.internetsocket.cmd.ScreenCmd
import com.example.wirelesstransferandroid.tools.InternetInfo


class DisplayScreenFragment : Fragment() {

    private lateinit var binding: FragmentDisplayScreenBinding

    var myTcpClient: MyTcpClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDisplayScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        toggleFullScreen()

        Thread {
            myTcpClient = arguments?.getString("serverIp")?.let {
                MyTcpClient(
                    InternetInfo.getPhoneIp(requireContext()),
                    it,
                    resources.getInteger(R.integer.tcp_port),
                    Settings.Global.getString(requireContext().contentResolver, "device_name")
                )
            }
            myTcpClient?.setOnConnected {
                /*
                requireActivity().runOnUiThread {
                    val toast = Toast(requireContext())
                    toast.setText(R.string.connected_to_pc)
                    toast.show()
                }
                */
            }
            myTcpClient?.setOnDisconnected {
                requireActivity().runOnUiThread {
                    requireActivity().findNavController(R.id.fragmentContainerView).popBackStack()
                }
            }
            myTcpClient?.setOnReceivedCmd { cmd ->
                when(cmd.cmdType) {
                    CmdType.Screen -> {
                        requireActivity().runOnUiThread {
                            val sc = cmd as ScreenCmd
                            binding.screenIV.setImageBitmap(sc.screenBmp)
                        }
                    }
                    CmdType.Request -> {
                        val rc = cmd as RequestCmd
                        if (rc.requestType == RequestType.Disconnect) {
                            myTcpClient?.disconnect()
                        }
                    }
                    else -> {}
                }
            }
            myTcpClient?.connect()
        }.start()
    }

    override fun onDetach() {
        super.onDetach()

        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        toggleFullScreen()
    }

    fun toggleFullScreen() {
        var uiOptions = requireActivity().window.decorView.systemUiVisibility
        uiOptions = uiOptions xor View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        uiOptions = uiOptions xor View.SYSTEM_UI_FLAG_FULLSCREEN
        uiOptions = uiOptions xor View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        requireActivity().window.decorView.systemUiVisibility = uiOptions
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (myTcpClient?.state == MyTcpClientState.Connected) {
            myTcpClient?.sendCmd(RequestCmd(RequestType.Disconnect, myTcpClient!!.clientName))
            myTcpClient?.disconnect()
        }
    }
}