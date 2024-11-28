package com.example.wirelesstransferandroid.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.example.wirelesstransferandroid.R
import com.example.wirelesstransferandroid.databinding.FragmentHomeBinding
import com.example.wirelesstransferandroid.internetsocket.MyTcp.Indexes
import com.example.wirelesstransferandroid.internetsocket.MyUdp.MyUdp
import com.example.wirelesstransferandroid.internetsocket.cmd.CmdDecoder
import com.example.wirelesstransferandroid.internetsocket.cmd.CmdType
import com.example.wirelesstransferandroid.internetsocket.cmd.ReplyCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.ReplyType
import com.example.wirelesstransferandroid.internetsocket.cmd.RequestCmd
import com.example.wirelesstransferandroid.internetsocket.cmd.RequestType
import com.example.wirelesstransferandroid.tools.InternetInfo
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.delay


class HomeFragment : Fragment() {
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

        private const val FUNCTION_MIRROR = "Mirror"
        private const val FUNCTION_EXTEND = "Extend"
        private const val FUNCTION_FILE_SHARE = "FileShare"
    }

    private val barcodeLauncher = registerForActivityResult<ScanOptions, ScanIntentResult>(ScanContract()) { result: ScanIntentResult ->
        if (result.contents != null) {
            //Toast.makeText(requireContext(), "Scanned: " + result.contents, Toast.LENGTH_LONG).show()

            val content = result.contents.split(" ")
            if (content.size != 2){
                Toast.makeText(requireContext(), resources.getString(R.string.incorrect_format), Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            if (!(content[0] == FUNCTION_MIRROR || content[0] == FUNCTION_EXTEND || content[0] == FUNCTION_FILE_SHARE)) {
                Toast.makeText(requireContext(), resources.getString(R.string.incorrect_format), Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            if (content[1].substring(0..7) != "192.168.") {
                Toast.makeText(requireContext(), resources.getString(R.string.incorrect_format), Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            // send request and navigate to destination
            val rc = RequestCmd(RequestType.QRConnect, Settings.Global.getString(context?.contentResolver, "device_name"))
            Thread {
                val myUdp = MyUdp(resources.getInteger(R.integer.udp_port))
                myUdp.send(rc.Encode(), content[1])

                var recvBuffer = myUdp.receive()
                val indexes = Indexes(0, recvBuffer.size - 1)
                var cmd = CmdDecoder.DecodeCmd(recvBuffer, indexes)

                if (cmd != null && cmd.cmdType == CmdType.Request) {
                    myUdp.send(ReplyCmd(ReplyType.Accept).Encode(), content[1])
                    myUdp.close()

                    requireActivity().runOnUiThread {
                        if (content[0] == FUNCTION_MIRROR || content[0] == FUNCTION_EXTEND) {
                            val bundle = bundleOf("serverIp" to content[1])
                            requireActivity().findNavController(R.id.fragmentContainerView).navigate(R.id.action_homeFragment_to_displayScreenFragment, bundle)
                        }
                        else if (content[0] == FUNCTION_FILE_SHARE) {
                            val bundle = bundleOf("serverIp" to content[1])
                            requireActivity().findNavController(R.id.fragmentContainerView).navigate(R.id.action_homeFragment_to_fileShareTransferingFragment, bundle)
                        }
                    }
                }
            }.start()
        }
    }

    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // request location permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }

        // init device information
        binding.deviceNameTv.text = Settings.Global.getString(context?.contentResolver, "device_name")
        binding.deviceIpTv.text = InternetInfo.getPhoneIp(requireContext())
        binding.wifiNameTv.text = InternetInfo.getSSID(requireContext())

        // check is connected to wifi
        if (binding.deviceIpTv.text.equals("0.0.0.0")) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
            builder.setTitle(R.string.wifi_not_connected_dialog_title)
            builder.setMessage(R.string.wifi_not_connected_message)
            builder.setPositiveButton(R.string.confirm) { dialogInterface, _ ->
                dialogInterface.dismiss()
                requireActivity().finish()
            }
            builder.create().show()
        }

        // function buttons
        binding.screenShareBtn.setOnClick {
            requireActivity().findNavController(R.id.fragmentContainerView).navigate(R.id.action_homeFragment_to_screenShareFragment)
        }

        binding.fileShareBtn.setOnClick {
            requireActivity().findNavController(R.id.fragmentContainerView).navigate(R.id.action_homeFragment_to_fileShareFragment)
        }

        // tool bar button click
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.qrScanner -> {
                    val options = ScanOptions()
                    options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    options.setPrompt("請掃描PC端的QR Code")
                    options.setBeepEnabled(false)
                    options.setOrientationLocked(false)
                    barcodeLauncher.launch(options)
                    true
                }
                else -> false
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty()) {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    binding.wifiNameTv.text = InternetInfo.getSSID(requireContext())
                } else {
                    val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
                    builder.setTitle(R.string.permission_warring_dialog_title)
                    builder.setMessage(R.string.location_permission_warring_message)
                    builder.setPositiveButton(R.string.confirm) { dialogInterface, _ ->
                        dialogInterface.dismiss()
                        requireActivity().finish()
                    }
                    builder.create().show()
                }
            }
        }
    }
}