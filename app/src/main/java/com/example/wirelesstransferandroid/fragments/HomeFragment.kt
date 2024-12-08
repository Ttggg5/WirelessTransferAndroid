package com.example.wirelesstransferandroid.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.transition.TransitionInflater
import com.example.wirelesstransferandroid.R
import com.example.wirelesstransferandroid.databinding.FragmentHomeBinding
import com.example.wirelesstransferandroid.internetsocket.Indexes
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
import kotlin.text.Regex


class HomeFragment : Fragment() {
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 0
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

            if (!content[1].matches(Regex("(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])"))) {
                Toast.makeText(requireContext(), resources.getString(R.string.incorrect_format), Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            binding.waitingMask.visibility = View.VISIBLE

            // send request and navigate to destination
            val rc = RequestCmd(RequestType.QRConnect, Settings.Global.getString(context?.contentResolver, "device_name"))
            Thread {
                val myUdp = MyUdp(resources.getInteger(R.integer.udp_port))
                myUdp.send(rc.Encode(), content[1])

                var count = 0
                while (true) {
                    if (count++ > 20) break

                    var recvBuffer = myUdp.receive()
                    val indexes = Indexes(0, recvBuffer.size)
                    var cmd = CmdDecoder.DecodeCmd(recvBuffer, indexes)

                    if (cmd != null) {
                        if (cmd.cmdType == CmdType.Request){
                            if ((cmd as RequestCmd).requestType == RequestType.FileShare ||
                                (cmd as RequestCmd).requestType == RequestType.Mirror ||
                                (cmd as RequestCmd).requestType == RequestType.Extend){
                                myUdp.send(ReplyCmd(ReplyType.Accept).Encode(), content[1])
                                myUdp.close()

                                requireActivity().runOnUiThread {
                                    if (content[0] == FUNCTION_MIRROR || content[0] == FUNCTION_EXTEND) {
                                        val bundle = bundleOf("serverIp" to content[1])
                                        requireActivity().findNavController(R.id.fragmentContainerView).navigate(R.id.action_homeFragment_to_displayScreenFragment, bundle)
                                    }
                                    else if (content[0] == FUNCTION_FILE_SHARE) {
                                        val bundle = bundleOf("serverIp" to content[1])
                                        requireActivity().findNavController(R.id.fragmentContainerView).navigate(R.id.action_homeFragment_to_fileShareTransferringReceiveFragment, bundle)
                                    }
                                }
                                break
                            }
                        }
                    }
                    else {
                        myUdp.close()
                        break
                    }
                }

                requireActivity().runOnUiThread {
                    binding.waitingMask.visibility = View.GONE
                }
            }.start()
        }
    }

    private lateinit var binding: FragmentHomeBinding

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
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // request location permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)

        // init device information
        binding.deviceNameTv.text = Settings.Global.getString(context?.contentResolver, "device_name")

        // check is connected to wifi or wifi hotspot opened
        if (InternetInfo.isWifiHotspotOn(requireContext())) {
            binding.deviceIpTv.text = InternetInfo.getPhoneIp(requireContext())
            binding.wifiNameLL.visibility = View.GONE
            binding.hotspotTV.visibility = View.VISIBLE
        }
        else if (InternetInfo.isWifiOn(requireContext())) {
            binding.deviceIpTv.text = InternetInfo.getPhoneIp(requireContext())
            binding.wifiNameTv.text = InternetInfo.getWifiSSID(requireContext())
        }
        else {
            val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
            builder.setCancelable(false)
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

        // get storage permission
        if (!Environment.isExternalStorageManager()) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
            builder.setCancelable(false)
            builder.setTitle(R.string.permission_warring_dialog_title)
            builder.setMessage(R.string.storage_permission_warring_message)
            builder.setPositiveButton(R.string.confirm) { dialogInterface, _ ->
                dialogInterface.dismiss()

                val permissionIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(permissionIntent)
            }
            builder.setNegativeButton(R.string.cancel) { dialogInterface, _ ->
                dialogInterface.dismiss()

                requireActivity().finish()
            }
            builder.create().show()
        }

        askNotificationPermission()
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            )
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty()) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    binding.wifiNameTv.text = InternetInfo.getWifiSSID(requireContext())
                } else {
                    val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
                    builder.setCancelable(false)
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