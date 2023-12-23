package com.rb.someapp

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.rb.someapp.databinding.ActivityCartBinding

class CartActivity : AppCompatActivity() {
    lateinit var binding: ActivityCartBinding

    lateinit var dataList: MutableList<CartUiData>
    lateinit var firestore: FirebaseFirestore

    var firebaseUser: FirebaseUser? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(LayoutInflater.from(this))
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



        dataList = ArrayList()

        firestore = Firebase.firestore

        firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            binding.cartEmptyLayout.visibility = View.VISIBLE
            return
        }




        //Implement callback for checkout activity
        val launchCheckOut = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                setResult(RESULT_OK)
                finish()
            }
        }

        val adapter = CartItemAdapter(this, dataList, launchCheckOut)
        val layoutManager = LinearLayoutManager(this)
        binding.cartRecy.layoutManager = layoutManager
        binding.cartRecy.adapter = adapter

        binding.cartToolBar.setNavigationOnClickListener {
            finish()
        }



        if (firebaseUser != null) {
            firestore.collection("Cart").document("doc").collection(firebaseUser!!.uid).get().addOnSuccessListener {
                for (document in it.documents) {
                    val data = document.toObject<CartData>()
                    firestore.collection("Products").document("doc").collection(data!!.type).whereEqualTo("id", data.productId).get().addOnSuccessListener {
                        for (doc in it.documents) {
                            val productData = doc.toObject<ProductData>()
                            val cartUiData = CartUiData()
                            cartUiData.cartData = data
                            cartUiData.productData = productData
                            dataList.add(cartUiData)
                            adapter.notifyItemInserted(dataList.size - 1)
                        }
                    }
                }
            }
        }

        //Check if cart is empty
        if (firebaseUser != null) {
            firestore.collection("Cart").document("doc").collection(firebaseUser!!.uid).addSnapshotListener(object : EventListener<QuerySnapshot>{
                override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                    if (value!!.isEmpty) {
                        binding.cartEmptyLayout.visibility = View.VISIBLE
                    }
                    else {
                        binding.cartEmptyLayout.visibility = View.GONE
                    }
                }

            })
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.activity_stable, R.anim.activity_close)

    }
}