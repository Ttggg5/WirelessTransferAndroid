package com.example.wirelesstransferandroid.fragments

import android.app.Notification
import android.app.NotificationManager
import android.content.Context.NOTIFICATION_SERVICE
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import com.example.wirelesstransferandroid.databinding.FragmentFileShareTransferringSendBinding
import com.example.wirelesstransferandroid.internetsocket.MyTcp.MyTcpClientInfo
import com.example.wirelesstransferandroid.internetsocket.MyTcp.MyTcpServer
import com.example.wirelesstransferandroid.internetsocket.cmd.Cmd
import com.example.wirelesstransferandroid.internetsocket.cmd.CmdType
import com.example.wirelesstransferandroid.internetsocket.cmd.FileDataCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.FileInfoCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.ReplyCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.ReplyType
import com.example.wirelesstransferandroid.internetsocket.cmd.RequestCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.RequestType
import com.example.wirelesstransferandroid.tools.NotificationSender
import com.example.wirelesstransferandroid.tools.InternetInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FileShareTransferringSendFragment : Fragment() {

    lateinit var binding: FragmentFileShareTransferringSendBinding

    lateinit var server: MyTcpServer

    lateinit var fileUriList: ArrayList<Uri>

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
        binding = FragmentFileShareTransferringSendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fileUriList = arguments?.getParcelableArrayList<Uri>("fileUriList")!!
        if (fileUriList != null) {
            for (uri in fileUriList) {
                addFileProgressTag(uri)
            }
        }

        binding.cancelBtn.setOnClickListener {
            requireActivity().findNavController(R.id.fragmentContainerView).popBackStack()
        }

        binding.returnHomeBtn.setOnClickListener {
            requireActivity().findNavController(R.id.fragmentContainerView).navigate(R.id.action_fileShareTransferringSendFragment_to_homeFragment)
        }

        server = MyTcpServer(
            InternetInfo.getPhoneIp(requireContext()),
            Settings.Global.getString(requireContext().contentResolver, "device_name"),
            resources.getInteger(R.integer.tcp_port)
        )
        server.setOnClientConnected { sendAllFileInfo(it) }
        server.setOnClientDisconnected { clientDisConnect(it) }
        server.setOnReceivedCmd { cmd, clientInfo -> cmdReceive(cmd, clientInfo) }
        server.start(1)
    }

    fun addFileProgressTag(uri: Uri) {
        val cursor = requireActivity().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0 && sizeIndex >= 0) {
                    var name = it.getString(nameIndex)
                    val size = it.getLong(sizeIndex)

                    val fptv = FileProgressTagView(binding.mainLL.context)
                    fptv.setFileName(name)
                    fptv.setFileSize(size)
                    fptv.setFileUri(uri)

                    val layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 400)
                    layoutParams.setMargins(50, 80, 50, 80)
                    fptv.layoutParams = layoutParams

                    fptv.setOnCompleted {
                        requireActivity().runOnUiThread {
                            fileLeft--
                            if (fileLeft == 0) {
                                binding.returnHomeBtn.visibility = View.VISIBLE

                                NotificationSender.sendNotification(requireContext(), "FileShare", resources.getString(R.string.send_complete), NotificationSender.fileShareChannel)
                            }
                            binding.fileLeftTV.text = fileLeft.toString()
                        }
                    }

                    binding.mainLL.addView(fptv)

                    binding.fileLeftTV.text = (++fileLeft).toString()
                }
            }
        }
    }

    fun sendAllFileInfo(clientInfo: MyTcpClientInfo) {
        requireActivity().runOnUiThread {
            for (v in binding.mainLL.children) {
                val fptv = v as FileProgressTagView
                lifecycleScope.launch {
                    server.sendCmd(FileInfoCmd(fptv.originalFileName, fptv.fullFileSize, ""), clientInfo)
                }
            }
            lifecycleScope.launch {
                server.sendCmd(RequestCmd(RequestType.FileShare, server.serverName), clientInfo)
            }
        }
    }

    fun clientDisConnect(clientInfo: MyTcpClientInfo) {
        if (fileLeft > 0) {
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), resources.getString(R.string.disconnected_from_pc), Toast.LENGTH_SHORT).show()
                requireActivity().findNavController(R.id.fragmentContainerView).popBackStack()
            }
        }
    }

    fun cmdReceive(cmd: Cmd, clientInfo: MyTcpClientInfo) {
        if (cmd.cmdType == CmdType.Reply) {
            if ((cmd as ReplyCmd).replyType == ReplyType.Accept) {
                lifecycleScope.launch(Dispatchers.IO) {
                    for (uri in fileUriList) {
                        val buffer = ByteArray(4194304) // 4MB

                        val cursor = requireActivity().contentResolver.query(uri, null, null, null, null)
                        cursor?.use {
                            if (it.moveToFirst()) {
                                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                if (nameIndex >= 0) {
                                    var name = it.getString(nameIndex)

                                    var fileProgressTagView: FileProgressTagView? = null
                                    requireActivity().runOnUiThread {
                                        for (v in binding.mainLL.children) {
                                            if ((v as FileProgressTagView).fileUri?.path == uri.path) {
                                                fileProgressTagView = v
                                                break
                                            }
                                        }
                                    }

                                    try {
                                        val fileStream = requireContext().contentResolver.openInputStream(uri)
                                        while (true) {
                                            val actualLength = fileStream?.read(buffer) ?: 0
                                            if (actualLength < 0) break

                                            server.sendCmd(FileDataCmd(name, buffer.sliceArray(0 until actualLength)), clientInfo)

                                            requireActivity().runOnUiThread {
                                                fileProgressTagView?.updateProgress(actualLength.toLong())
                                            }
                                        }
                                    } catch (ex: Exception) {
                                        ex.stackTrace
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        server.stop()
    }
}