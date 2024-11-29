package com.example.wirelesstransferandroid.fragments

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.fragment.app.setFragmentResult
import androidx.navigation.findNavController
import com.example.wirelesstransferandroid.R
import com.example.wirelesstransferandroid.customviews.FileTagView
import com.example.wirelesstransferandroid.databinding.FragmentFileShareSendBinding
import com.example.wirelesstransferandroid.tools.FileInfoPresenter
import com.example.wirelesstransferandroid.tools.IntToDp.dp
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.util.ArrayList

class FileShareSendFragment : Fragment() {

    lateinit var binding: FragmentFileShareSendBinding

    private val requestFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (result.data?.data != null) {
                    result.data?.data?.let { uri ->
                        handleSelectedFile(uri)
                    }
                }
                else {
                    for (i in 0 until (result.data?.clipData?.itemCount ?: 0)) {
                        val item = result.data?.clipData?.getItemAt(i)
                        if (item != null)
                            handleSelectedFile(item.uri)
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFileShareSendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addFileBtn.setOnClickListener { openSpecificFolder("") }
        binding.confirmBtn.setOnClickListener { checkFile() }
        binding.topAppBar.setNavigationOnClickListener {
            requireActivity().findNavController(R.id.fragmentContainerView).popBackStack()
        }

    }

    private fun openSpecificFolder(folderName: String) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"  // Set the MIME type to filter files
            val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:$folderName")
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        requestFileLauncher.launch(intent)
    }

    private fun handleSelectedFile(uri: Uri) {
        val cursor = requireActivity().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0 && sizeIndex >= 0) {
                    var name = it.getString(nameIndex)
                    val size = it.getLong(sizeIndex)

                    requireActivity().runOnUiThread {
                        for (view in binding.mainLL.children) {
                            val tmp = view as FileTagView
                            if (tmp.uri?.path == uri.path)
                                return@runOnUiThread
                            else if (tmp.originalFileName == name) {
                                name = GetNonDuplicateFileName(name)
                            }
                        }

                        val ftv = FileTagView(binding.mainLL.context)
                        ftv.setFileName(name)
                        ftv.setFileSize(size)
                        ftv.setUri(uri)

                        val layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 120.dp)
                        layoutParams.setMargins(20.dp, 0.dp, 20.dp, 30.dp)
                        ftv.layoutParams = layoutParams

                        ftv.setOnDelete {
                            binding.mainLL.removeView(it)
                        }

                        binding.mainLL.addView(ftv)
                    }
                }
            }

            /*
            val pfd = requireContext().contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null){
                val fd = pfd.fileDescriptor
                val fileStream = FileInputStream(fd)
            */
        }
    }

    private fun GetNonDuplicateFileName(name: String): String {
        var count = 1
        var result = name
        val extension = result.split(".").last()
        while (true) {
            result = name.substring(0..name.length - extension.length - 2) + "(" + count + ")." + extension
            count++

            for (view in binding.mainLL.children) {
                val tmp = view as FileTagView

                if (tmp.originalFileName == result)
                    continue
            }
            break
        }

        return result
    }

    private fun checkFile() {
        if (binding.mainLL.children.count() == 0) {
            Toast.makeText(requireContext(), resources.getString(R.string.no_file_selected), Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = SearchDeviceDialogFragment()
        dialog.isCancelable = false
        dialog.setOnDeviceChose {

        }
        dialog.show(childFragmentManager, SearchDeviceDialogFragment.TAG)
    }
}