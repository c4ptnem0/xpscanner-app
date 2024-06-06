package com.example.xpscanner.ui.scan

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.xpscanner.databinding.FragmentScanBinding

class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        val scanBtn = binding.scanBtn
        val enterBtn = binding.enterBtn

        scanBtn.setOnClickListener {
            // Navigation to the ProductSearchActivity
            val intent = Intent(requireContext(), ScanActivity::class.java)
            startActivity(intent)
        }

        enterBtn.setOnClickListener {
            // redirect to login activity, after signing up
            val intent = Intent(requireContext(), AddItemActivity::class.java)
            startActivity(intent)
        }


        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
