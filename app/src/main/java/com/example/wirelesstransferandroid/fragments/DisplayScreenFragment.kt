package com.example.wirelesstransferandroid.fragments

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.PointF
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.wirelesstransferandroid.R
import com.example.wirelesstransferandroid.databinding.FragmentDisplayScreenBinding
import com.example.wirelesstransferandroid.internetsocket.MyTcp.MyTcpClient
import com.example.wirelesstransferandroid.internetsocket.MyTcp.MyTcpClientState
import com.example.wirelesstransferandroid.internetsocket.cmd.CmdType
import com.example.wirelesstransferandroid.internetsocket.cmd.MouseAct
import com.example.wirelesstransferandroid.internetsocket.cmd.MouseCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.MouseMoveCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.RequestCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.RequestType
import com.example.wirelesstransferandroid.internetsocket.cmd.ScreenCmd
import com.example.wirelesstransferandroid.tools.InternetInfo


class DisplayScreenFragment : Fragment() {

    private lateinit var binding: FragmentDisplayScreenBinding

    var myTcpClient: MyTcpClient? = null

    var preX = 0f
    var preY = 0f

    var preAction = MotionEvent.ACTION_UP
    var preActionX = 0f
    var preActionY = 0f
    var preActionTime = 0L

    var isTwoFingerAction = false

    var movementX = 0f
    var movementY = 0f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDisplayScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (requireActivity().requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            toggleFullScreen()
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        binding.screenIV.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    preX = event.x
                    preY = event.y

                    preActionX = event.x
                    preActionY = event.y
                    preAction = event.action
                    preActionTime = event.eventTime
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount > 2)
                        isTwoFingerAction = false
                    else if (event.eventTime <= preActionTime + 150)
                        isTwoFingerAction = true
                }

                MotionEvent.ACTION_MOVE -> {
                    movementX = event.x - preX
                    movementY = event.y - preY
                    preX = event.x
                    preY = event.y

                    if (event.pointerCount > 1) {
                        Thread {
                            myTcpClient?.sendCmd(MouseCmd(PointF(0f, 0f), MouseAct.MiddleButtonRolled, movementY.toInt() * 3, false))
                        }.start()
                    }
                    else {
                        Thread {
                            myTcpClient?.sendCmd(MouseMoveCmd(movementX, movementY))
                        }.start()
                    }
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    if (isTwoFingerAction)
                        preActionTime = event.eventTime
                }

                MotionEvent.ACTION_UP -> {
                    movementX = 0f
                    movementY = 0f

                    if (preAction == MotionEvent.ACTION_DOWN) {
                        if (event.x in preActionX - 10..preActionX + 10 &&
                            event.y in preActionY - 10..preActionY + 10) {
                            if (isTwoFingerAction) {
                                if (event.eventTime <= preActionTime + 150) {
                                    // right button click
                                    Thread {
                                        myTcpClient?.sendCmd(MouseCmd(PointF(0f, 0f), MouseAct.RightButtonDown, 0, false))
                                        myTcpClient?.sendCmd(MouseCmd(PointF(0f, 0f), MouseAct.RightButtonUp, 0, false))
                                    }.start()
                                }
                            }
                            else {
                                // left button click
                                Thread {
                                    myTcpClient?.sendCmd(MouseCmd(PointF(0f, 0f), MouseAct.LeftButtonDown, 0, false))
                                    myTcpClient?.sendCmd(MouseCmd(PointF(0f, 0f), MouseAct.LeftButtonUp, 0, false))
                                }.start()
                            }
                        }
                    }

                    isTwoFingerAction = false
                    preAction = event.action
                }
            }
            true
        }

        myTcpClient = arguments?.getString("serverIp")?.let {
            MyTcpClient(
                InternetInfo.getPhoneIp(requireContext()),
                it,
                resources.getInteger(R.integer.tcp_port),
                Settings.Global.getString(requireContext().contentResolver, "device_name")
            )
        }
        myTcpClient?.setOnConnected {
            requireActivity().runOnUiThread {
                val toast = Toast(requireContext())
                toast.setText(R.string.connected_to_pc)
                toast.show()
            }
        }
        myTcpClient?.setOnDisconnected {
            requireActivity().runOnUiThread {
                requireActivity().findNavController(R.id.fragmentContainerView).popBackStack()

                val toast = Toast(requireContext())
                toast.setText(R.string.disconnected_from_pc)
                toast.show()
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
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onDetach() {
        super.onDetach()

        if (requireActivity().requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            toggleFullScreen()
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun toggleFullScreen() {
        var uiOptions = requireActivity().window.decorView.systemUiVisibility
        uiOptions = uiOptions xor View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        uiOptions = uiOptions xor View.SYSTEM_UI_FLAG_FULLSCREEN
        uiOptions = uiOptions xor View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        requireActivity().window.decorView.systemUiVisibility = uiOptions
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (requireActivity().requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            if (myTcpClient?.state == MyTcpClientState.Connected) {
                try{
                    myTcpClient?.sendCmd(RequestCmd(RequestType.Disconnect, myTcpClient!!.clientName))
                    myTcpClient?.disconnect()
                } catch (ex: Exception) {

                }
            }
        }
    }
}