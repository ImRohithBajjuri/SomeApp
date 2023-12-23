package com.rb.someapp

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rb.someapp.databinding.FragmentOrderInfoSupportSheetBinding


class OrderInfoSupportSheet : BottomSheetDialogFragment() {
    lateinit var binding: FragmentOrderInfoSupportSheetBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogStyle)
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentOrderInfoSupportSheetBinding.inflate(inflater, container, false)

        binding.infoSupportContact.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:9573951943")
            startActivity(intent)
        }

        return binding.root
    }

}