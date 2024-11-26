package com.example.wirelesstransferandroid.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.PointF
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.example.wirelesstransferandroid.R
import com.example.wirelesstransferandroid.databinding.FragmentDisplayScreenBinding
import com.example.wirelesstransferandroid.internetsocket.MyTcp.MyTcpClient
import com.example.wirelesstransferandroid.internetsocket.MyTcp.MyTcpClientState
import com.example.wirelesstransferandroid.internetsocket.cmd.CmdType
import com.example.wirelesstransferandroid.internetsocket.cmd.KeyState
import com.example.wirelesstransferandroid.internetsocket.cmd.KeyboardCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.MouseAct
import com.example.wirelesstransferandroid.internetsocket.cmd.MouseCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.MouseMoveCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.RequestCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.RequestType
import com.example.wirelesstransferandroid.internetsocket.cmd.ScreenCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.VirtualKeyCode
import com.example.wirelesstransferandroid.tools.InternetInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
    var isScrolling = false

    var isLeftButtonClicked = false
    var LeftButtonClickedTime = 0L

    lateinit var clickJob: Job
    var disableClick = false

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
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }

        binding.screenIV.setOnTouchListener { _, event ->
            screenIVOnTouch(event)
            true
        }

        binding.keyboardImgBtn.setOnClickListener {
            toggleKeyboard()
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

    fun screenIVOnTouch(event: MotionEvent) {
        if (myTcpClient?.state != MyTcpClientState.Connected) return

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                preX = event.x
                preY = event.y

                preActionX = event.x
                preActionY = event.y
                preAction = event.action
                preActionTime = event.eventTime

                // hold left button
                if (isLeftButtonClicked && event.eventTime <= LeftButtonClickedTime + 150) {
                    disableClick = true
                    /*
                    lifecycleScope.launch {
                        myTcpClient?.sendCmd(MouseCmd(PointF(0f, 0f), MouseAct.LeftButtonDown, 0, false))
                    }
                    */
                }
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

                if (event.pointerCount == 2) {
                    isScrolling = true
                    // scroll
                    lifecycleScope.launch {
                        myTcpClient?.sendCmd(MouseCmd(PointF(0f, 0f), MouseAct.MiddleButtonRolled, movementY.toInt() * 3, false))
                    }
                }
                else if (!isScrolling){
                    // move mouse
                    lifecycleScope.launch {
                        myTcpClient?.sendCmd(MouseMoveCmd(movementX * 1.5f, movementY * 1.5f))
                    }
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
                    if (event.x in preActionX - 20..preActionX + 20 &&
                        event.y in preActionY - 20..preActionY + 20) {
                        if (isTwoFingerAction) {
                            if (event.eventTime <= preActionTime + 150) {
                                // right button click
                                lifecycleScope.launch {
                                    myTcpClient?.sendCmd(MouseCmd(PointF(0f, 0f), MouseAct.RightButtonDown, 0, false))
                                    myTcpClient?.sendCmd(MouseCmd(PointF(0f, 0f), MouseAct.RightButtonUp, 0, false))
                                }
                            }
                        }
                        else {
                            // left button click
                            clickJob = lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    myTcpClient?.sendCmd(MouseCmd(PointF(0f, 0f), MouseAct.LeftButtonDown, 0, false))
                                    delay(100)
                                    if (!disableClick)
                                        myTcpClient?.sendCmd(MouseCmd(PointF(0f, 0f), MouseAct.LeftButtonUp, 0, false))
                                }
                            }

                            isLeftButtonClicked = true
                            LeftButtonClickedTime = event.eventTime
                        }
                    }
                    else if (isLeftButtonClicked) {
                        // release left button
                        lifecycleScope.launch {
                            myTcpClient?.sendCmd(MouseCmd(PointF(0f, 0f), MouseAct.LeftButtonUp, 0, false))
                        }
                        disableClick = false
                        isLeftButtonClicked = false
                    }
                }

                isTwoFingerAction = false
                isScrolling = false
                preAction = event.action
            }
        }
    }

    fun toggleKeyboard() {
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                myTcpClient?.disconnect()
            }

            in KeyEvent.KEYCODE_SHIFT_LEFT..KeyEvent.KEYCODE_SHIFT_RIGHT -> {}

            KeyEvent.KEYCODE_AT -> {
                lifecycleScope.launch {
                    myTcpClient?.sendCmd(KeyboardCmd(VirtualKeyCode.SHIFT, KeyState.Down))
                    myTcpClient?.sendCmd(KeyboardCmd(VirtualKeyCode.VK_2, KeyState.Click))
                    myTcpClient?.sendCmd(KeyboardCmd(VirtualKeyCode.SHIFT, KeyState.Up))
                }
            }

            KeyEvent.KEYCODE_PLUS -> {
                lifecycleScope.launch {
                    myTcpClient?.sendCmd(KeyboardCmd(VirtualKeyCode.SHIFT, KeyState.Down))
                    myTcpClient?.sendCmd(KeyboardCmd(VirtualKeyCode.OEM_PLUS, KeyState.Click))
                    myTcpClient?.sendCmd(KeyboardCmd(VirtualKeyCode.SHIFT, KeyState.Up))
                }
            }

            KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN -> {
                lifecycleScope.launch {
                    myTcpClient?.sendCmd(KeyboardCmd(VirtualKeyCode.SHIFT, KeyState.Down))
                    myTcpClient?.sendCmd(KeyboardCmd(VirtualKeyCode.VK_9, KeyState.Click))
                    myTcpClient?.sendCmd(KeyboardCmd(VirtualKeyCode.SHIFT, KeyState.Up))
                }
            }

            KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN -> {
                lifecycleScope.launch {
                    myTcpClient?.sendCmd(KeyboardCmd(VirtualKeyCode.SHIFT, KeyState.Down))
                    myTcpClient?.sendCmd(KeyboardCmd(VirtualKeyCode.VK_0, KeyState.Click))
                    myTcpClient?.sendCmd(KeyboardCmd(VirtualKeyCode.SHIFT, KeyState.Up))
                }
            }

            KeyEvent.KEYCODE_STAR -> {
                lifecycleScope.launch {
                    myTcpClient?.sendCmd(KeyboardCmd(VirtualKeyCode.SHIFT, KeyState.Down))
                    myTcpClient?.sendCmd(KeyboardCmd(VirtualKeyCode.VK_8, KeyState.Click))
                    myTcpClient?.sendCmd(KeyboardCmd(VirtualKeyCode.SHIFT, KeyState.Up))
                }
            }

            KeyEvent.KEYCODE_POUND -> {
                lifecycleScope.launch {
                    myTcpClient?.sendCmd(KeyboardCmd(VirtualKeyCode.SHIFT, KeyState.Down))
                    myTcpClient?.sendCmd(KeyboardCmd(VirtualKeyCode.VK_3, KeyState.Click))
                    myTcpClient?.sendCmd(KeyboardCmd(VirtualKeyCode.SHIFT, KeyState.Up))
                }
            }

            else -> {
                val virtualKeyCode = VirtualKeyCode.fromInt(keyCode)
                if (virtualKeyCode == null)
                    return true

                lifecycleScope.launch {
                    if (event?.isShiftPressed ?: false)
                        myTcpClient?.sendCmd(KeyboardCmd(VirtualKeyCode.SHIFT, KeyState.Down))
                    myTcpClient?.sendCmd(KeyboardCmd(virtualKeyCode, KeyState.Click))
                    if (event?.isShiftPressed ?: false)
                        myTcpClient?.sendCmd(KeyboardCmd(VirtualKeyCode.SHIFT, KeyState.Up))
                }
            }
        }

        return true
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onDetach() {
        super.onDetach()

        if (requireActivity().requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            toggleFullScreen()
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
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
                    myTcpClient?.disconnect()
                } catch (ex: Exception) {

                }
            }
        }
    }
}