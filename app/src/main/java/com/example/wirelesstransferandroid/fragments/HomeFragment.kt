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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.wirelesstransferandroid.R
import com.example.wirelesstransferandroid.databinding.FragmentHomeBinding
import com.example.wirelesstransferandroid.tools.InternetInfo
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions


class HomeFragment : Fragment() {
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    private val barcodeLauncher = registerForActivityResult<ScanOptions, ScanIntentResult>(ScanContract()) { result: ScanIntentResult ->
        if (result.contents != null) {
            Toast.makeText(requireContext(), "Scanned: " + result.contents, Toast.LENGTH_LONG).show()

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

        binding.screenShareBtn.setOnClick {
            findNavController().navigate(R.id.action_homeFragment_to_screenShareFragment)
        }

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
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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