package com.example.wirelesstransferandroid.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.wirelesstransferandroid.R
import com.example.wirelesstransferandroid.databinding.FragmentScreenShareBinding
import com.example.wirelesstransferandroid.internetsocket.Indexes
import com.example.wirelesstransferandroid.internetsocket.MyUdp.MyUdp
import com.example.wirelesstransferandroid.internetsocket.cmd.ClientInfoCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.CmdDecoder
import com.example.wirelesstransferandroid.internetsocket.cmd.CmdType
import com.example.wirelesstransferandroid.internetsocket.cmd.ReplyCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.ReplyType
import com.example.wirelesstransferandroid.internetsocket.cmd.RequestCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.RequestType
import com.example.wirelesstransferandroid.tools.InternetInfo
import java.util.concurrent.CountDownLatch


class ScreenShareFragment : Fragment() {

    private lateinit var binding: FragmentScreenShareBinding
    private lateinit var myUdpListener: MyUdp

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentScreenShareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.setNavigationOnClickListener {
            requireActivity().findNavController(R.id.fragmentContainerView).popBackStack()
        }

        Thread {
            myUdpListener = MyUdp(resources.getInteger(R.integer.udp_port))
            while (true) {
                try {
                    var recvBuffer = myUdpListener.receive()
                    val indexes = Indexes(0, recvBuffer.size - 1)
                    var cmd = CmdDecoder.DecodeCmd(recvBuffer, indexes)
                    if (cmd != null && cmd.cmdType == CmdType.Request) {
                        val requestCmd = cmd as RequestCmd
                        when (requestCmd.requestType) {
                            in RequestType.Mirror..RequestType.Extend -> {
                                val latch = CountDownLatch(1)

                                var dialogResult: Boolean = false
                                requireActivity().runOnUiThread {
                                    val builder = AlertDialog.Builder(requireContext())
                                    builder.setCancelable(false)
                                    builder.setTitle(R.string.screen_share_request_dialog_title)
                                    builder.setMessage(requestCmd.deviceName + " " + resources.getString(R.string.screen_share_request_message))
                                    builder.setPositiveButton(R.string.confirm) { dialogInterface, _ ->
                                        dialogInterface.dismiss()

                                        dialogResult = true
                                        latch.countDown()
                                    }
                                    builder.setNegativeButton(R.string.cancel) { dialogInterface, _ ->
                                        dialogInterface.dismiss()

                                        dialogResult = false
                                        latch.countDown()
                                    }
                                    builder.create().show()
                                }

                                latch.await()

                                if (dialogResult) {
                                    myUdpListener.send(ReplyCmd(ReplyType.Accept).Encode(), myUdpListener.getSenderIP())

                                    val bundle = bundleOf("serverIp" to myUdpListener.getSenderIP())
                                    requireActivity().runOnUiThread {
                                        requireActivity().findNavController(R.id.fragmentContainerView).navigate(R.id.action_screenShareFragment_to_displayScreenFragment, bundle)
                                    }
                                } else {
                                    myUdpListener.send(ReplyCmd(ReplyType.Refuse).Encode(), myUdpListener.getSenderIP())
                                }
                            }

                            RequestType.PhoneClientInfoShareScreen -> {
                                myUdpListener.send(
                                    ClientInfoCmd(
                                        Settings.Global.getString(context?.contentResolver, "device_name"),
                                        InternetInfo.getPhoneIp(requireContext())
                                    ).Encode(),
                                    myUdpListener.getSenderIP()
                                )
                            }

                            else -> {}
                        }
                    }
                } catch (ex: Exception) {
                    break
                }
            }
            myUdpListener.close()
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        myUdpListener.close()
    }
}