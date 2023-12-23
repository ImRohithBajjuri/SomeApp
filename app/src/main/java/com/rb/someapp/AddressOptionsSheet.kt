package com.rb.someapp

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.rb.someapp.databinding.FragmentAddressOptionsSheetBinding


class AddressOptionsSheet() : BottomSheetDialogFragment() {

    lateinit var binding: FragmentAddressOptionsSheetBinding

    lateinit var addressData: AddressData
    lateinit var listener: AddressItemAdapter.AddressListener

    lateinit var firestore: FirebaseFirestore

    var firebaseUser: FirebaseUser? = null

    constructor(addressData: AddressData, listener: AddressItemAdapter.AddressListener) : this() {
        this.addressData = addressData
        this.listener = listener
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogStyle)
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAddressOptionsSheetBinding.inflate(inflater, container, false)

        firestore = Firebase.firestore

        firebaseUser = FirebaseAuth.getInstance().currentUser

        binding.addressItemSheetHeader.text = addressData.name

        binding.addressItemSheetRemove.setOnClickListener {
            if (firebaseUser != null) {
                firestore.collection("Address").document("doc").collection(firebaseUser!!.uid).document(addressData.addressID.toString()).delete()
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            listener.itemRemoved(addressData)
                            Toast.makeText(requireActivity(), "Address removed", Toast.LENGTH_SHORT).show()
                            dismiss()
                        }
                        else {
                            Toast.makeText(requireActivity(), "Unable to remove this address, please try again later", Toast.LENGTH_SHORT).show()
                        }
                    }
            }

        }

        binding.addressItemSheetEdit.setOnClickListener {
            val addressSheet = NewAddressSheet(listener, NewAddressSheet.EDIT, addressData)
            addressSheet.show(childFragmentManager, "useCaseTwo")
        }

        return binding.root
    }

}