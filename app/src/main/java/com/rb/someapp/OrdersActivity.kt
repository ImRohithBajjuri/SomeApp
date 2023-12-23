package com.rb.someapp

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.SnapHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.rb.someapp.databinding.ActivityOrdersBinding
import kotlinx.coroutines.*

class OrdersActivity : AppCompatActivity() {
    lateinit var binding: ActivityOrdersBinding

    lateinit var dataList: MutableList<OrderUiData>

    lateinit var currentList: MutableList<OrderUiData>

    lateinit var firestore: FirebaseFirestore

    var firebaseUser: FirebaseUser? = null


    lateinit var adapter: OrdersAdapter

    lateinit var adapter2: CurrentOrdersAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrdersBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController!!.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        }


        dataList = ArrayList()

        currentList = ArrayList()

        firestore = Firebase.firestore

        firebaseUser = FirebaseAuth.getInstance().currentUser
        //Check if user is signed in and stop the activity process
        if (firebaseUser == null) {
            binding.ordersSignInLayout.visibility = View.VISIBLE
            binding.currentOrdsSubHeader.visibility = View.GONE
            binding.cmpltOrdsSubHeader.visibility = View.GONE
            return
        }


        val layoutManager = LinearLayoutManager(this)
        binding.ordersRecy.layoutManager = layoutManager
        adapter = OrdersAdapter(this, dataList)
        binding.ordersRecy.adapter = adapter

        val layoutManager2 = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.currentOrdersRecy.layoutManager = layoutManager2
        adapter2 = CurrentOrdersAdapter(this, currentList)
        binding.currentOrdersRecy.adapter = adapter2

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.currentOrdersRecy)

        binding.currentOrdersIndicator.attachToRecyclerView(binding.currentOrdersRecy)

        if (firebaseUser != null) {

            getAllOrders()

            firestore.collection("Orders").document("doc").collection(firebaseUser!!.uid)
                .addSnapshotListener { value, error ->
                    for (change in value!!.documentChanges) {
                        if (change.type == DocumentChange.Type.MODIFIED) {
                            val orderData = change.document.toObject<OrderData>()
                            CoroutineScope(Dispatchers.IO).launch {
                                shiftOrderUi(orderData)
                            }
                        }
                    }
                }
        }


        //Adjust empty layout visibility
        CoroutineScope(Dispatchers.Main).launch {
            delay(3000)
            adjustEmptyLayoutVisibility()
        }



        binding.ordersToolbar.setNavigationOnClickListener {
            finish()
        }


    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.activity_stable, R.anim.activity_close)

    }

    private fun getAllOrders() {
        firestore.collection("Orders").document("doc").collection(firebaseUser!!.uid).get()
            .addOnSuccessListener {
                for (doc in it.documents) {
                    val data = doc.toObject<OrderData>()
                    firestore.collection("Products").document("doc")
                        .collection(data!!.cartData!!.type).whereEqualTo("id", data.productId)
                        .get()
                        .addOnSuccessListener { it2 ->
                            for (doc2 in it2.documents) {
                                val productData = doc2.toObject<ProductData>()
                                val orderUiData = OrderUiData()
                                orderUiData.orderData = data
                                orderUiData.productData = productData

                                if (data.orderStatus == AppUtils.ORDER_PLACED) {
                                    currentList.add(orderUiData)
                                    adapter2.notifyItemInserted(currentList.size - 1)
                                } else {
                                    dataList.add(orderUiData)
                                    adapter.notifyItemInserted(dataList.size - 1)
                                }

                            }
                        }
                }
            }

    }

    private suspend fun shiftOrderUi(data: OrderData) {

        //Shift from currentList to dataList(completed orders)
        val iterator2 = currentList.iterator()
        while (iterator2.hasNext()) {
            val currentOrderData = iterator2.next()
            //Check if the order status is different and then change the section it belongs to.
            if (currentOrderData.orderData!!.id == data.id && currentOrderData.orderData!!.orderStatus != data.orderStatus) {
                val currentPosition = currentList.indexOf(currentOrderData)

                currentOrderData.orderData = data

                //Add it to current list as it doesn't belong to dataList(completed orders)
                withContext(Dispatchers.Main) {
                    dataList.add(currentOrderData)
                    adapter.notifyDataSetChanged()

                    currentList.remove(currentOrderData)
                    adapter2.notifyItemRemoved(currentPosition)

                    adjustEmptyLayoutVisibility()
                }



                return
            }
        }
    }

    private fun adjustEmptyLayoutVisibility() {
        //For current orders
        if (currentList.isEmpty()) {
            binding.currentOrdersEmptyLayout.visibility = View.VISIBLE
            binding.currentOrdersRecy.visibility = View.GONE
            binding.currentOrdersIndicator.visibility = View.GONE
        } else {
            binding.currentOrdersEmptyLayout.visibility = View.GONE
            binding.currentOrdersRecy.visibility = View.VISIBLE
            binding.currentOrdersIndicator.visibility = View.VISIBLE
        }

        //For completed orders
        if (dataList.isEmpty()) {
            binding.ordersEmptyLayout.visibility = View.VISIBLE
            binding.ordersRecy.visibility = View.GONE
        } else {
            binding.ordersEmptyLayout.visibility = View.GONE
            binding.ordersRecy.visibility = View.VISIBLE

        }
    }



}