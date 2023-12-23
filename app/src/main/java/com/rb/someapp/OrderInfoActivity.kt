package com.rb.someapp

import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsetsController
import android.view.animation.Animation
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.rb.someapp.databinding.ActivityOrderInfoBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OrderInfoActivity : AppCompatActivity() {
    lateinit var binding: ActivityOrderInfoBinding

    lateinit var dataList: MutableList<ProductUiData>

    lateinit var firestore: FirebaseFirestore

    var orderId = 0

    var firebaseUser: FirebaseUser? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderInfoBinding.inflate(LayoutInflater.from(this))
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

        orderId = intent.getIntExtra("id", 0)

        dataList = ArrayList()

        val layoutManager = LinearLayoutManager(this)
        binding.orderedItemsRecy.layoutManager = layoutManager

        val adapter = ProductsAdapter(this, dataList)
        binding.orderedItemsRecy.adapter = adapter

        if (firebaseUser != null) {
            firestore.collection("Orders").document("doc").collection(firebaseUser!!.uid).document(orderId.toString()).get().addOnSuccessListener {
                val orderData = it.toObject<OrderData>()
                firestore.collection("Products").document("doc").collection(orderData!!.cartData!!.type).whereEqualTo("id", orderData.productId).get().addOnSuccessListener {
                    for (doc2 in it.documents) {
                        val productData = doc2.toObject<ProductData>()!!
                        val productUiData = ProductUiData()
                        productUiData.productData = productData

                        dataList.add(productUiData)
                        adapter.notifyItemInserted(dataList.size - 1)


                        val reviewList = ArrayList<ReviewData>()
                        doc2.reference.collection("Reviews").get().addOnSuccessListener { it2 ->
                            for (doc3 in it2.documents) {
                                val reviewData = doc3.toObject<ReviewData>()
                                reviewList.add(reviewData!!)

                                if (reviewList.indexOf(reviewData) == (it2.toMutableList().size - 1)) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        adapter.calculateRating(reviewList, productUiData)
                                    }
                                }
                            }
                        }
                    }
                }

                //Set the attributes
                binding.orderInfoId.text = "#${orderData.id}"
                val orderTime = AppUtils.getNormalTime(orderData.orderedTime)
                binding.orderInfoTime.text = orderTime
                binding.orderInfoStatus.text = orderData.orderStatus

                binding.orderInfoTtlAmt.text = "${orderData.orderPrice} ${getString(R.string.rs)}"
                binding.orderInfoPmtMode.text = orderData.paymentMethod
                binding.orderInfoPmtStatus.text = orderData.paymentStatus
                if (orderData.paymentStatus == AppUtils.PAYMENT_NOT_PAID) {
                    binding.orderInfopmtTime.text = "NA"
                }
                else {
                    val paymentTime = AppUtils.getNormalTime(orderData.paymentTime)
                    binding.orderInfopmtTime.text = paymentTime
                }


                //Adjust options
                handleOptionsVisibility(orderData.orderStatus)
            }

            firestore.collection("Orders").document("doc").collection(firebaseUser!!.uid).document(orderId.toString()).addSnapshotListener {snapshot, error ->
                val data = snapshot!!.toObject<OrderData>()
                setTrackingInfo(data!!)
                updateOrderDetails(data)

                //Adjust options
                handleOptionsVisibility(data.orderStatus)
            }

        }


        binding.orderInfoSupportButton.setOnClickListener {
            val anim = AnimUtils.pressAnim(object : Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {
                }

                override fun onAnimationEnd(p0: Animation?) {
                    val supportSheet = OrderInfoSupportSheet()
                    supportSheet.show(supportFragmentManager, "useCaseOne")
                }

                override fun onAnimationRepeat(p0: Animation?) {
                }

            })
            it.startAnimation(anim)
        }

        binding.orderInfoCancelButton.setOnClickListener {
            val anim = AnimUtils.pressAnim(object : Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {

                }

                override fun onAnimationEnd(p0: Animation?) {
                    val dialog = AppUtils.buildDialog(this@OrderInfoActivity, "Cancel order?", "Are you sure? This process can't be undone")
                    dialog.setPositiveButton("Yes") { p0, p1 ->
                        //Cancel the order
                        cancelOrder()

                    }
                    dialog.setNegativeButton("No") { p0, p1 ->
                        //Do nothing
                    }

                    dialog.show()
                }

                override fun onAnimationRepeat(p0: Animation?) {
                }

            })

            it.startAnimation(anim)
        }

        binding.orderInfoToolbar.setNavigationOnClickListener {
            finish()
        }

        binding.trackingSeekBar.setOnTouchListener { p0, p1 -> true }


    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.activity_stable, R.anim.activity_close)
    }

    fun setTrackingInfo(orderData: OrderData) {
        when (orderData.orderStatus) {
            AppUtils.ORDER_PLACED -> {
                binding.orderInfoTrackingInfo.text = "Your order has been placed successfully. Shipping will start soon."
                binding.orderInfoTrackingInfo.setTextColor(ContextCompat.getColor(this, R.color.blue))
                binding.trackingImg.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_round_move_to_inbox_30))
                binding.orderInfoTrackingCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.lightBlue))

                //Adjust the seekbar
                binding.trackingSeekBar.progress = 0
                binding.trackingSeekBar.thumb = ContextCompat.getDrawable(this, R.drawable.ic_round_move_to_inbox_30)
            }

            AppUtils.ORDER_SHIPPED -> {
                binding.orderInfoTrackingInfo.text = "Your ordered item has been successfully shipped, please expand for more details."
                binding.orderInfoTrackingInfo.setTextColor(ContextCompat.getColor(this, R.color.blue))
                binding.trackingImg.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_round_local_shipping_30))
                binding.orderInfoTrackingCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.lightBlue))

                //Adjust the seekbar
                binding.trackingSeekBar.progress = 1
                binding.trackingSeekBar.thumb = ContextCompat.getDrawable(this, R.drawable.ic_round_local_shipping_30)

            }

            AppUtils.ORDER_OUT_FOR_DLV -> {
                binding.orderInfoTrackingInfo.text = "Your ordered item is out for delivery, expect it to reach your hands soon! Expand for more details."
                binding.orderInfoTrackingInfo.setTextColor(ContextCompat.getColor(this, R.color.blue))
                binding.trackingImg.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_round_delivery_dining_30))
                binding.orderInfoTrackingCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.lightBlue))

                //Adjust the seekbar
                binding.trackingSeekBar.progress = 2
                binding.trackingSeekBar.thumb = ContextCompat.getDrawable(this, R.drawable.ic_round_delivery_dining_30)

            }

            AppUtils.ORDER_COMPLETED -> {
                binding.orderInfoTrackingInfo.text = "Your order has been delivered successfully. Thank you for shopping with us."
                binding.orderInfoTrackingInfo.setTextColor(ContextCompat.getColor(this, R.color.white))
                binding.trackingImg.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_round_download_done_30))
                binding.orderInfoTrackingCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.lightGreen))
                binding.trackingSeekBar.thumb = ContextCompat.getDrawable(this, R.drawable.ic_round_download_done_30)

                //Adjust the seekbar
                binding.trackingSeekBar.progress = 3
            }

            AppUtils.ORDER_CANCELLED -> {
                binding.orderInfoTrackingInfo.text = "Your order has been cancelled successfully."
                binding.orderInfoTrackingInfo.setTextColor(ContextCompat.getColor(this, R.color.peachyRed))
                binding.trackingImg.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_round_do_disturb_30))
                binding.orderInfoTrackingCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.peachyRedLight))
            }
        }

        //Set the shipping status if any.
        if (orderData.shippingData != null) {
            binding.trackingShippingStatus.text = orderData.shippingData!!.shippingCurrentStatus
        }
    }

    fun handleOptionsVisibility(orderStatus: String) {
        if (orderStatus == AppUtils.ORDER_PLACED) {
            binding.orderInfoCancelButton.visibility = View.VISIBLE
        }
        else {
            binding.orderInfoCancelButton.visibility = View.GONE
        }
    }

    fun cancelOrder() {
        if (firebaseUser != null) {
            firestore.collection("Orders").document("doc").collection(firebaseUser!!.uid).document(orderId.toString()).get().addOnSuccessListener { it2 ->
                it2.reference.update("orderStatus", AppUtils.ORDER_CANCELLED)
                it2.reference.update("deliveryStatus", AppUtils.DLV_CANCELLED)


                val snackbar = AppUtils.buildSnackbar(this, "Successfully cancelled your order", binding.root, false)
/*
                snackbar.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(2000)
                            finish()
                        }
                        super.onDismissed(transientBottomBar, event)
                    }
                })
*/
                snackbar.show()
                setResult(RESULT_CANCELED)
            }

            firestore.collection("Orders Pool").document(orderId.toString()).delete()
        }

    }

    fun updateOrderDetails(orderData: OrderData) {
        binding.orderInfoStatus.text = orderData.orderStatus

        binding.orderInfoPmtStatus.text = orderData.paymentStatus
        if (orderData.paymentStatus == AppUtils.PAYMENT_NOT_PAID) {
            binding.orderInfopmtTime.text = "NA"
        }
        else {
            val paymentTime = AppUtils.getNormalTime(orderData.paymentTime)
            binding.orderInfopmtTime.text = paymentTime
        }
    }
}