package com.example.wirelesstransferandroid.fragments

import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.Toast
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.transition.TransitionInflater
import com.example.wirelesstransferandroid.R
import com.example.wirelesstransferandroid.customviews.FileProgressTagView
import com.example.wirelesstransferandroid.customviews.FileShareTagState
import com.example.wirelesstransferandroid.databinding.FragmentFileShareTransferringReceiveBinding
import com.example.wirelesstransferandroid.internetsocket.MyTcp.MyTcpClient
import com.example.wirelesstransferandroid.internetsocket.MyTcp.MyTcpClientState
import com.example.wirelesstransferandroid.internetsocket.cmd.CmdType
import com.example.wirelesstransferandroid.internetsocket.cmd.FileDataCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.FileInfoCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.ReplyCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.ReplyType
import com.example.wirelesstransferandroid.internetsocket.cmd.RequestCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.RequestType
import com.example.wirelesstransferandroid.tools.InternetInfo
import com.example.wirelesstransferandroid.tools.NotificationSender
import kotlinx.coroutines.launch

class FileShareTransferringReceiveFragment : Fragment() {

    private lateinit var binding: FragmentFileShareTransferringReceiveBinding

    lateinit var myTcpClient: MyTcpClient
    val fileProgressTags = ArrayList<FileProgressTagView>()

    var totalFile = 0
    var fileLeft = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_in)
        exitTransition = inflater.inflateTransition(R.transition.slide_out)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFileShareTransferringReceiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cancelBtn.setOnClickListener {
            if (myTcpClient?.state == MyTcpClientState.Connected) {
                try{
                    myTcpClient?.disconnect()
                } catch (ex: Exception) {

                }
            }

            if (fileLeft == 0)
                requireActivity().findNavController(R.id.fragmentContainerView).navigate(R.id.action_fileShareTransferringReceiveFragment_to_homeFragment)

            NotificationSender.dismissNotification(requireContext(), NotificationSender.fileShareChannel)
        }

        binding.returnHomeBtn.setOnClickListener {
            requireActivity().findNavController(R.id.fragmentContainerView).navigate(R.id.action_fileShareTransferringReceiveFragment_to_homeFragment)
        }

        myTcpClient = MyTcpClient(
            InternetInfo.getPhoneIp(requireContext()),
            arguments?.getString("serverIp").toString(),
            resources.getInteger(R.integer.tcp_port),
            Settings.Global.getString(requireContext().contentResolver, "device_name")
        )
        myTcpClient?.setOnConnected {
            requireActivity().runOnUiThread {
                val toast = Toast(requireContext())
                toast.setText(R.string.transferring)
                toast.show()
            }
        }
        myTcpClient?.setOnDisconnected {
            if (fileLeft == 0) return@setOnDisconnected

            NotificationSender.dismissNotification(requireContext(), NotificationSender.fileShareChannel)
            requireActivity().runOnUiThread {
                requireActivity().findNavController(R.id.fragmentContainerView).navigate(R.id.action_fileShareTransferringReceiveFragment_to_homeFragment)

                val toast = Toast(requireContext())
                toast.setText(R.string.disconnected_from_pc)
                toast.show()
            }
        }
        myTcpClient?.setOnReceivedCmd { cmd ->
            when(cmd.cmdType) {
                CmdType.FileInfo -> {
                    val fic = cmd as FileInfoCmd
                    requireActivity().runOnUiThread {
                        val fptv = FileProgressTagView(binding.mainLL.context)
                        fptv.setFileName(fic.fileName)
                        fptv.setFileSize(fic.fileSize)

                        val layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 400)
                        layoutParams.setMargins(50, 80, 50, 80)
                        fptv.layoutParams = layoutParams

                        fptv.setOnCompleted {
                            fileLeft--
                            if (fileLeft == 0) {
                                NotificationSender.sendNotification(requireContext(), "FileShare", resources.getString(R.string.download_complete), NotificationSender.fileShareChannel)
                                requireActivity().runOnUiThread {
                                    binding.returnHomeBtn.visibility = View.VISIBLE
                                }
                            }
                            requireActivity().runOnUiThread {
                                binding.fileLeftTV.text = fileLeft.toString()
                            }
                        }

                        fileProgressTags.add(fptv)
                        binding.mainLL.addView(fptv)

                        totalFile++
                        binding.fileLeftTV.text = (++fileLeft).toString()
                    }
                }
                CmdType.FileData -> {
                    val fdc = cmd as FileDataCmd
                    for (tag in fileProgressTags) {
                        if (tag.originalFileName == fdc.fileName) {
                            tag.writeDataToFile(fdc.fileData)
                            break
                        }
                    }
                }
                CmdType.Request -> {
                    val rc = cmd as RequestCmd
                    when(rc.requestType) {
                        RequestType.FileShare -> {
                            lifecycleScope.launch {
                                myTcpClient.sendCmd(ReplyCmd(ReplyType.Accept))
                            }

                            // send progress notification
                            Thread {
                                while (fileLeft == 0) {}

                                while (fileLeft > 0) {
                                    val tag = fileProgressTags[totalFile - fileLeft]
                                    val percentage = ((tag.curFileSize.toDouble() / tag.fullFileSize.toDouble()) * 100.0).toInt()
                                    NotificationSender.sendProgressNotification(requireContext(), "FileShare",
                                        String.format("%s(%d/%d)", resources.getString(R.string.downloading), totalFile - fileLeft + 1, totalFile),
                                        percentage, false, NotificationSender.fileShareChannel)
                                    Thread.sleep(500)
                                }
                            }.start()
                        }
                        RequestType.Disconnect -> {
                            myTcpClient?.disconnect()
                        }
                        else -> {}
                    }
                }
                else -> {}
            }
        }
        myTcpClient?.connect()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (myTcpClient?.state == MyTcpClientState.Connected) {
            try{
                myTcpClient?.disconnect()
            } catch (ex: Exception) {

            }
        }

        for (view in binding.mainLL.children) {
            val fptv = view as FileProgressTagView
            if (fptv.state == FileShareTagState.Processing)
                fptv.deleteWroteFile()
        }
    }
}