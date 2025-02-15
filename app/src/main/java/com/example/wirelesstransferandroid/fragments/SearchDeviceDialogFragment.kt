package com.example.wirelesstransferandroid.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.Toast
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import com.example.wirelesstransferandroid.R
import com.example.wirelesstransferandroid.customviews.DeviceTagView
import com.example.wirelesstransferandroid.databinding.DialogFragmentSearchdeviceBinding
import com.example.wirelesstransferandroid.internetsocket.Indexes
import com.example.wirelesstransferandroid.internetsocket.MyUdp.MyUdp
import com.example.wirelesstransferandroid.internetsocket.cmd.ClientInfoCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.CmdDecoder
import com.example.wirelesstransferandroid.internetsocket.cmd.CmdType
import com.example.wirelesstransferandroid.internetsocket.cmd.ReplyCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.ReplyType
import com.example.wirelesstransferandroid.internetsocket.cmd.RequestCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.RequestType
import com.example.wirelesstransferandroid.tools.IntToDp.dp
import com.example.wirelesstransferandroid.tools.InternetInfo
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Runnable
import pl.droidsonroids.gif.GifDrawable
import java.net.SocketTimeoutException
import java.util.Date

enum class DeviceFinderState {
    Searching,
    Stopped,
}

class SearchDeviceDialogFragment: DialogFragment() {
    companion object {
        const val TAG = "SearchDeviceDialog"
        const val SEARCH_CYCLE = 1000 // unit is "millisecond"
    }

    private var onDeviceChose: (String) -> Unit = {}
    fun setOnDeviceChose(block: (String) -> Unit) {
        onDeviceChose = block
    }

    lateinit var binding: DialogFragmentSearchdeviceBinding

    var state = DeviceFinderState.Stopped
        private set

    var searchClient: MyUdp? = null
    lateinit var broadcastIp: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogFragmentSearchdeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setLayout(350.dp, 500.dp)
        dialog?.window?.setBackgroundDrawable(resources.getDrawable(R.drawable.round_block))

        broadcastIp = InternetInfo.getBroadcastIp(requireContext())

        binding.cancelBtn.setOnClickListener {
            dialog?.dismiss()
        }

        binding.qrShowBtn.setOnClickListener {
            binding.qrMask.startAnimation(AnimationUtils.loadAnimation(binding.qrMask.context, R.anim.qr_fade_in))
            binding.qrMask.visibility = View.VISIBLE
        }

        binding.qrCloseBtn.setOnClickListener {
            binding.qrMask.startAnimation(AnimationUtils.loadAnimation(binding.qrMask.context, R.anim.qr_fade_out))
            binding.qrMask.visibility = View.GONE
        }

        binding.qrIV.setImageBitmap(generateQRCode("FileShare " + InternetInfo.getPhoneIp(requireContext())))

        startSearching()
    }

    fun startSearching() {
        if (state == DeviceFinderState.Searching) return

        try {
            searchClient?.close()
        } catch (ex: Exception) {

        } finally {
            searchClient = MyUdp(resources.getInteger(R.integer.udp_port))
            searchClient?.broadcast = true
        }

        state = DeviceFinderState.Searching
        Thread(runReceive).start()
        Thread(runSearching).start()
    }

    private val runReceive = Runnable {
        try {
            while (state == DeviceFinderState.Searching) {
                var receiveBuffer = searchClient?.receive()
                receiveBuffer?.size?.let {
                    if (it > 0) {
                        val indexes = Indexes(0, it)
                        while (true){
                            var cmd = CmdDecoder.DecodeCmd(receiveBuffer, indexes)
                            if (cmd != null) {
                                when(cmd.cmdType) {
                                    CmdType.ClientInfo -> {
                                        val cic = cmd as ClientInfoCmd
                                        var found = false
                                        requireActivity().runOnUiThread {
                                            for (v in binding.mainLL.children) {
                                                val dtv = v as DeviceTagView
                                                if (dtv.ip == cic.ip) {
                                                    found = true
                                                    dtv.foundTime = Date().time
                                                    break
                                                }
                                            }
                                            if (!found) {
                                                val deviceTagView = DeviceTagView(binding.mainLL.context)
                                                deviceTagView.setDeviceName(cic.clientName)
                                                deviceTagView.setDeviceIp(cic.ip)

                                                val layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 80.dp)
                                                layoutParams.setMargins(10.dp, 0.dp, 10.dp, 10.dp)
                                                deviceTagView.layoutParams = layoutParams

                                                deviceTagView.setOnClick { onDeviceTagViewClick(it) }

                                                binding.mainLL.addView(deviceTagView)
                                            }
                                        }
                                    }
                                    CmdType.Request -> {
                                        val rc = cmd as RequestCmd
                                        if (rc.requestType == RequestType.QRConnect) {
                                            val senderIp = searchClient?.getSenderIP()
                                            requireActivity().runOnUiThread {
                                                val tmp = DeviceTagView(requireContext())
                                                tmp.setDeviceIp(senderIp!!)
                                                onDeviceTagViewClick(tmp)
                                            }
                                        }
                                    }
                                    else -> {}
                                }
                            }
                            else break
                        }
                    }
                }
            }
        } catch (ex: Exception) {

        }
    }

    private val runSearching = Runnable {
        try {
            // Send broadcast message
            while (state == DeviceFinderState.Searching) {
                // delete found device when no respond
                requireActivity().runOnUiThread {
                    for (v in binding.mainLL.children) {
                        if (Date().time - (v as DeviceTagView).foundTime > SEARCH_CYCLE * 2)
                            binding.mainLL.removeView(v)
                    }
                }

                // Pc request
                var rc = RequestCmd(RequestType.PcClientInfo, Settings.Global.getString(context?.contentResolver, "device_name"))
                searchClient?.send(rc.Encode(), broadcastIp)

                // Phone request
                rc = RequestCmd(RequestType.PhoneClientInfoFileShare, Settings.Global.getString(context?.contentResolver, "device_name"))
                searchClient?.send(rc.Encode(), broadcastIp)

                // Waiting
                for (i in 0 until SEARCH_CYCLE / 100) {
                    Thread.sleep(100)
                    if (searchClient == null)
                        throw Exception("searchClient is null")
                }
            }
        } catch (ex: Exception) {

        }
    }

    private fun onDeviceTagViewClick(it: DeviceTagView) {
        (binding.searchingGIV.drawable as GifDrawable).stop()
        binding.waitingMask.visibility = View.VISIBLE

        stopSearching()
        Thread {
            // send request
            val udpClient = MyUdp(resources.getInteger(R.integer.udp_port))
            udpClient.timeOut = 30000
            udpClient.send(RequestCmd(RequestType.FileShare, Settings.Global.getString(context?.contentResolver, "device_name")).Encode(), it.ip)

            // waiting for accept
            var isAccept = false
            try {
                while (true) {
                    val receiveBuffer = udpClient.receive()
                    val cmd = CmdDecoder.DecodeCmd(receiveBuffer, Indexes(0, receiveBuffer.size - 1))
                    if (cmd != null && cmd.cmdType == CmdType.Reply) {
                        val rc = cmd as ReplyCmd
                        if (rc.replyType == ReplyType.Refuse) {
                            requireActivity().runOnUiThread {
                                Toast.makeText(requireContext(), resources.getString(R.string.refuse_connect), Toast.LENGTH_SHORT).show()
                            }
                            break
                        }
                        else {
                            isAccept = true
                            break
                        }
                    }
                }
            } catch (ex: SocketTimeoutException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), resources.getString(R.string.request_timeout), Toast.LENGTH_SHORT).show()
                }
            }

            udpClient.close()
            requireActivity().runOnUiThread {
                binding.waitingMask.visibility = View.GONE
                (binding.searchingGIV.drawable as GifDrawable).start()
                if (isAccept)
                    onDeviceChose.invoke(it.ip)
            }
            startSearching()
        }.start()
    }

    fun stopSearching() {
        if (searchClient != null) {
            searchClient?.close()
            state = DeviceFinderState.Stopped
        }
    }

    private fun generateQRCode(text: String): Bitmap? {
        val barcodeEncoder = BarcodeEncoder()
        try {
            // This method returns a Bitmap image of the
            // encoded text with a height and width of 400
            // pixels.
            val bitmap = barcodeEncoder.encodeBitmap(text, BarcodeFormat.QR_CODE, 400, 400)
            return bitmap
        }
        catch (ex: WriterException) {
            ex.printStackTrace()
            return null
        }
    }

    override fun onDetach() {
        super.onDetach()

        stopSearching()
    }
}