package com.rb.someapp

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.rb.someapp.databinding.ActivityAddressBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AddressActivity: AppCompatActivity(), AddressItemAdapter.AddressListener {

    lateinit var binding: ActivityAddressBinding

    lateinit var firestore: FirebaseFirestore

    lateinit var dataList: MutableList<AddressData>
    lateinit var adapter: AddressItemAdapter

    var firebaseUser: FirebaseUser? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddressBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController!!.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }
        else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        }

        firestore = Firebase.firestore

        firebaseUser = FirebaseAuth.getInstance().currentUser

        //Check if user is signed in and stop the activity process
        if (firebaseUser == null) {
            binding.addressSignInLayout.visibility = View.VISIBLE
            binding.addAddress.visibility = View.GONE
            return
        }



        val isSelection = intent.getBooleanExtra("isSelection", false)

        dataList = ArrayList()

        adapter = AddressItemAdapter(this, dataList, this, isSelection)
        val layoutManager = LinearLayoutManager(this)
        binding.addressRecy.layoutManager = layoutManager
        binding.addressRecy.adapter = adapter

        binding.addAddress.setOnClickListener {
            val anim = AnimUtils.pressAnim(null)
            it.startAnimation(anim)

            if (firebaseUser != null) {
                val newAddressSheet = NewAddressSheet(this, NewAddressSheet.CREATE, null)
                newAddressSheet.show(supportFragmentManager, "useCaseOne")
            }
        }

        if (firebaseUser != null) {
            firestore.collection("Address").document("doc").collection(firebaseUser!!.uid).orderBy("addedTime", Query.Direction.ASCENDING).get().addOnSuccessListener {
                for (doc in it) {
                    val data = doc.toObject<AddressData>()
                    dataList.add(data)
                    adapter.notifyItemInserted(dataList.size - 1)
                }
            }

            firestore.collection("Address").document("doc").collection(firebaseUser!!.uid).addSnapshotListener(this) { snapshot, error ->
                CoroutineScope(Dispatchers.Main).launch {
                    delay(500)
                    if (snapshot!!.isEmpty) {
                        binding.noAddressLayout.visibility = View.VISIBLE
                    }
                    else {
                        binding.noAddressLayout.visibility = View.GONE
                    }
                }
            }
        }


    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.activity_stable, R.anim.activity_close)
    }

    override fun itemAdded(addressData: AddressData) {
        //Update the adapter with newly added address
        dataList.add(addressData)
        adapter.notifyItemInserted(dataList.size - 1)
    }

    override fun itemRemoved(addressData: AddressData) {
        //Update the adapter to adjust item removal
        val position = dataList.indexOf(addressData)
        dataList.removeAt(position)
        adapter.notifyItemRemoved(position)
    }

    override fun itemEdited(addressData: AddressData) {
        //Update the adapter to reflect the changes made
        adapter.notifyItemChanged(dataList.indexOf(addressData))
    }
}