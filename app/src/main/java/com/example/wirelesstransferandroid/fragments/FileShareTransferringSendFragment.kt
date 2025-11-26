package com.example.wirelesstransferandroid.fragments

import android.R.bool
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.Toast
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.transition.TransitionInflater
import com.example.wirelesstransferandroid.R
import com.example.wirelesstransferandroid.customviews.FileProgressTagView
import com.example.wirelesstransferandroid.databinding.FragmentFileShareTransferringSendBinding
import com.example.wirelesstransferandroid.internetsocket.MyTcp.MyTcpClientInfo
import com.example.wirelesstransferandroid.internetsocket.MyTcp.MyTcpServer
import com.example.wirelesstransferandroid.internetsocket.MyTcp.MyTcpServerState
import com.example.wirelesstransferandroid.internetsocket.cmd.Cmd
import com.example.wirelesstransferandroid.internetsocket.cmd.CmdType
import com.example.wirelesstransferandroid.internetsocket.cmd.FileDataCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.FileInfoCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.ReplyCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.ReplyType
import com.example.wirelesstransferandroid.internetsocket.cmd.RequestCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.RequestType
import com.example.wirelesstransferandroid.tools.InternetInfo
import com.example.wirelesstransferandroid.tools.NotificationSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class FileShareTransferringSendFragment : Fragment() {

    lateinit var binding: FragmentFileShareTransferringSendBinding

    lateinit var server: MyTcpServer

    lateinit var fileUriList: ArrayList<Uri>
    val fileProgressTags = ArrayList<FileProgressTagView>()

    var fileLeft = 0
    var nextSeg: Boolean = true

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
            server.stop()
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

                    fileProgressTags.add(fptv)
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
                    try {
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

                                                if (server.state != MyTcpServerState.Listening)
                                                    break
                                                server.sendCmd(FileDataCmd(name, buffer.sliceArray(0 until actualLength)), clientInfo)

                                                requireActivity().runOnUiThread {
                                                    fileProgressTagView?.updateProgress(actualLength.toLong())
                                                }

                                                while (!nextSeg) { }
                                            }
                                        } catch (_: Exception) { }
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) { }
                }

                // send progress notification
                Thread {
                    try {
                        while (fileLeft > 0) {
                            val tag = fileProgressTags[fileProgressTags.size - fileLeft]
                            val percentage = ((tag.curFileSize.toDouble() / tag.fullFileSize.toDouble()) * 100.0).toInt()
                            NotificationSender.sendProgressNotification(requireContext(), "FileShare",
                                String.format("%s(%d/%d)", resources.getString(R.string.sending), fileProgressTags.size - fileLeft + 1, fileProgressTags.size), percentage, false)
                            Thread.sleep(500)
                        }

                        NotificationSender.sendProgressNotification(requireContext(), "FileShare",
                            String.format("%s(%d/%d)", resources.getString(R.string.sending), fileProgressTags.size, fileProgressTags.size), 100, false)
                    } catch (_: Exception) { }
                }.start()
            }
        }
        else if (cmd.cmdType == CmdType.Request) {
            if ((cmd as RequestCmd).requestType == RequestType.FileShare)
                nextSeg = true
            else if ((cmd as RequestCmd).requestType == RequestType.Disconnect)
                server.stop()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        server.stop()
    }
}