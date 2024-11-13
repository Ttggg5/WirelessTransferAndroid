package com.example.wirelesstransferandroid.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.wirelesstransferandroid.R
import com.example.wirelesstransferandroid.databinding.FragmentHomeBinding
import com.example.wirelesstransferandroid.toolmodules.InternetInfo
import java.net.NetworkInterface
import java.util.Collections
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback

class HomeFragment : Fragment() {
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
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
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }

        // init device information
        binding.deviceNameTv.text = Settings.Global.getString(context?.contentResolver, "device_name")
        binding.deviceIpTv.text = InternetInfo.getPhoneIp(requireContext())
        binding.wifiNameTv.text = InternetInfo.getSSID(requireContext())

        binding.screenShareBtn.setOnClick {
            findNavController().navigate(R.id.action_homeFragment_to_screenShareFragment)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                binding.wifiNameTv.text = InternetInfo.getSSID(requireContext())
            } else {
                Toast.makeText(context, "必須許可，才能使用此app", Toast.LENGTH_SHORT).show()
                requireActivity().finish()
            }
        }
    }
}