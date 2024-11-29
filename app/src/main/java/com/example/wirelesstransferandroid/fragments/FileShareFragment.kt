package com.example.wirelesstransferandroid.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.wirelesstransferandroid.R
import com.example.wirelesstransferandroid.databinding.FragmentFileShareBinding

class FileShareFragment : Fragment() {

    private lateinit var binding: FragmentFileShareBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFileShareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.setNavigationOnClickListener {
            requireActivity().findNavController(R.id.fragmentContainerView).popBackStack()
        }

        // function buttons
        binding.fileSendBtn.setOnClick {
            requireActivity().findNavController(R.id.fragmentContainerView).navigate(R.id.action_fileShareFragment_to_fileShareSendFragment)
        }

        binding.fileReceiveBtn.setOnClick {
            requireActivity().findNavController(R.id.fragmentContainerView).navigate(R.id.action_fileShareFragment_to_fileShareReceiveFragment)
        }
    }
}