package com.example.wirelesstransferandroid.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.wirelesstransferandroid.R
import com.example.wirelesstransferandroid.databinding.DialogFragmentSearchdeviceBinding
import com.example.wirelesstransferandroid.databinding.FragmentFileShareSendBinding
import com.example.wirelesstransferandroid.tools.IntToDp.dp

class SearchDeviceDialogFragment: DialogFragment() {
    companion object {
        const val TAG = "SearchDeviceDialog"
    }

    private var onDeviceChose: (String) -> Unit = {}
    fun setOnDeviceChose(block: (String) -> Unit) {
        onDeviceChose = block
    }

    lateinit var binding: DialogFragmentSearchdeviceBinding

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

        binding.cancelBtn.setOnClickListener {
            dialog?.dismiss()
        }
    }

    override fun onDetach() {
        super.onDetach()


    }
}