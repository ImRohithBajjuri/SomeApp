package com.rb.someapp

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsetsController
import android.view.animation.Animation
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.rb.someapp.databinding.ActivityCheckoutBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.regex.Pattern


class CheckoutActivity : AppCompatActivity() {

    lateinit var binding: ActivityCheckoutBinding

    lateinit var dataList: MutableList<String>

    lateinit var firestore: FirebaseFirestore

    var addressSelected: Boolean = false

    var selectedAddress: AddressData? = null

    var paymentMethod: String = COD

    var paymentStatus: String = AppUtils.PAYMENT_PENDING

    lateinit var data: CartData
    lateinit var productData: ProductData

    var firebaseUser: FirebaseUser? = null


    lateinit var launchGooglePay: ActivityResultLauncher<Intent>

    lateinit var launchUPI: ActivityResultLauncher<Intent>

    var paymentTime: Long = 0

    lateinit var functions: FirebaseFunctions

    companion object {
        val COD = "cash on delivery"

        val UPI = "upi"

        val GOOGLE_PAY = "google pay"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(LayoutInflater.from(this))
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

        val id = intent.getIntExtra("id", 0)

        dataList = ArrayList()

        firestore = Firebase.firestore

        firebaseUser = FirebaseAuth.getInstance().currentUser

        functions = Firebase.functions

        //Set default payment method
        paymentMethod = GOOGLE_PAY

        //Set default payment method to google pay
        binding.cktPaymentGroup.check(R.id.checkoutGooglePay)





        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.cktItemsRecy.layoutManager = layoutManager

        val adapter = CheckoutItemAdapter(this, dataList)
        binding.cktItemsRecy.adapter = adapter



        if (firebaseUser != null) {
            firestore.collection("Cart").document("doc").collection(firebaseUser!!.uid)
                .document(id.toString()).get().addOnSuccessListener {
                    data = it.toObject<CartData>()!!
                    firestore.collection("Products").document("doc").collection(data.type)
                        .whereEqualTo("id", data.productId).get().addOnSuccessListener {
                            for (doc in it.documents) {
                                productData = doc.toObject<ProductData>()!!
                                dataList.add(productData.imgUrl)
                                adapter.notifyItemInserted(dataList.size - 1)

                                binding.cktItemName.text = productData.name
                                binding.cktBillingName.text = productData.name
                                binding.cktBillingQ.text = data.quantity.toString()
                                val tp = data.quantity * productData.price
                                binding.cktBillingTp.text = "$tp ${getString(R.string.rs)}"
                            }
                        }
                }
        }


        //Implement callback for address activity
        val launchAddAddress =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    val addressID = it.data!!.getIntExtra("id", 0)

                    if (firebaseUser != null) {
                        firestore.collection("Address").document("doc")
                            .collection(firebaseUser!!.uid).document(addressID.toString())
                            .get()
                            .addOnSuccessListener {
                                val data = it.toObject<AddressData>()
                                
                                //set the data and update the UI
                                setCurrentAddress(data!!)

                            }
                    }

                }
            }

        //Implement callback for google pay payment method
        launchGooglePay = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Payment failed, please try again and proceed", Toast.LENGTH_LONG).show()
                paymentStatus = AppUtils.PAYMENT_NOT_PAID

            }

            if (it.resultCode == RESULT_OK) {
                if (it.data != null) {
                    val res = it.data!!.getStringExtra("response")

                    if (res!!.contains("SUCCESS") || res.contains("SUBMITTED")) {

                        //Get ref id
                        val refId = res.subSequence(res.length - 12, res.length)


                        paymentStatus = AppUtils.PAYMENT_PAID
                        paymentTime = Calendar.getInstance().timeInMillis

                        binding.cktOrderPlacingSheen.visibility = View.VISIBLE

                        placeOrder(refId.toString())
                    }
                    else {
                        AppUtils.buildSnackbar(this, "Payment failed, please try again and proceed", binding.root, false).show()

                        paymentStatus = AppUtils.PAYMENT_NOT_PAID
                    }
                }
                else {
                    AppUtils.buildSnackbar(this, "Payment failed, please try again and proceed", binding.root, false).show()

                    paymentStatus = AppUtils.PAYMENT_NOT_PAID

                }
            }
        }


        //Implement callback for upi
        launchUPI = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {

            // Example output  txnId=UPI608f070ee644467aa78d1ccf5c9ce39b&responseCode=ZM&ApprovalRefNo=null&Status=FAILURE&txnRef=undefined
            if (it.data != null) {
                val response = it.data!!.getStringExtra("response")

                if (response!!.contains("SUCCESS") || response.contains("SUBMITTED")) {

                    //Get ref id
                    val refId = response.subSequence(response.length - 12, response.length)


                    paymentStatus = AppUtils.PAYMENT_PAID
                    paymentTime = Calendar.getInstance().timeInMillis

                    binding.cktOrderPlacingSheen.visibility = View.VISIBLE

                    placeOrder(refId.toString())
                }
                else {
                    AppUtils.buildSnackbar(this, "Payment failed, please try again and proceed", binding.root, false).show()

                    paymentStatus = AppUtils.PAYMENT_NOT_PAID
                }

            }
            else {
                AppUtils.buildSnackbar(this, "Payment failed, please try again and proceed", binding.root, false).show()

                paymentStatus = AppUtils.PAYMENT_NOT_PAID
            }

        }


        binding.selectAddress.setOnClickListener {
            val anim = AnimUtils.pressAnim(null)
            it.startAnimation(anim)

            val intent = Intent(this, AddressActivity::class.java)
            intent.putExtra("isSelection", true)
            launchAddAddress.launch(intent)
            overridePendingTransition(R.anim.activity_open, R.anim.activity_stable)
        }

        binding.cktPaymentGroup.setOnCheckedChangeListener { _, i ->
            setPaymentMethod(i)
        }

        binding.cktConfirmOrder.setOnClickListener {
            val anim = AnimUtils.pressAnim(object : Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {

                }

                override fun onAnimationEnd(p0: Animation?) {

                    if (productData.availability == AppUtils.PRODUCT_UNAVAILABLE) {
                        AppUtils.buildSnackbar(this@CheckoutActivity, "Sorry, this product is not available as of now", binding.root, false).show()
                        return
                    }

                    if (selectedAddress == null) {
                        Toast.makeText(this@CheckoutActivity, "Please select a address before proceeding", Toast.LENGTH_LONG).show()
                        return
                    }


                    proceedOrder()
                }

                override fun onAnimationRepeat(p0: Animation?) {
                }
            })

            it.startAnimation(anim)
        }

        binding.cktToolbar.setNavigationOnClickListener {
            finish()
        }

    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.activity_stable, R.anim.activity_close)
    }

    fun setCurrentAddress(data: AddressData) {
        //Set the address data
        selectedAddress = data

        binding.cktAddressName.text = data.name
        binding.cktAddressLaneOne.text = data.laneOne
        binding.cktAddressLaneTwo.text = data.laneTwo
        binding.cktAddressDes.text = "${data.city}, ${data.pinCode}"

        //Change select address text
        binding.selectAddressTxt.text = "Change address"

        //Change 'addressSelected' to 'true'
        addressSelected = true

        //Update the visibility
        binding.cktAddressCard.visibility = View.VISIBLE
    }

    fun setPaymentMethod(id: Int) {
        when (id) {
            R.id.checkoutGooglePay -> {
                paymentMethod = GOOGLE_PAY
                paymentStatus = AppUtils.PAYMENT_PENDING
                binding.cktConfirmOrderTxt.text = "Pay and place your order"
            }


            R.id.checkoutUPI -> {
                paymentMethod = UPI
                paymentStatus = AppUtils.PAYMENT_PENDING
                binding.cktConfirmOrderTxt.text = "Pay and place your order"
            }


            R.id.checkoutCOD -> {
                paymentMethod = COD
                paymentStatus = AppUtils.PAYMENT_NOT_PAID
                binding.cktConfirmOrderTxt.text = "place your order"
            }
        }
    }

    fun placeOrder(refId: String) {
        val totalAmount = data.quantity * productData.price
        val orderData = OrderData()
        orderData.id = java.util.Random().nextInt(100000)
        orderData.orderStatus = AppUtils.ORDER_PLACED
        orderData.cartData = data
        orderData.orderedTime = Calendar.getInstance().timeInMillis
        orderData.paymentMethod = paymentMethod
        orderData.paymentStatus = paymentStatus
        orderData.paymentTime = paymentTime
        orderData.orderPrice = totalAmount
        orderData.productId = productData.id
        orderData.transactionRefId = refId
        orderData.selectedAddress = selectedAddress!!.addressID
        orderData.userNote = binding.cktUserNote.text.toString()

        if (firebaseUser != null) {
            orderData.uid = firebaseUser!!.uid

            firestore.collection("Orders").document("doc").collection(firebaseUser!!.uid)
                .document(orderData.id.toString()).set(orderData).addOnSuccessListener {

                    //Find and notify the takers
                    findOrderTaker(orderData)


                    //Remove the added item in the cart
                    firestore.collection("Cart").document("doc").collection(firebaseUser!!.uid)
                        .document(data.id.toString()).delete()


                    CoroutineScope(Dispatchers.Main).launch {
                        delay(3000)
                        binding.cktOrderPlacingSheen.visibility = View.GONE
                        setResult(RESULT_OK)
                        finish()
                    }
                }
        }

    }

    @SuppressLint("MissingPermission")
    fun findOrderTaker(orderData: OrderData){
        val currentLat = selectedAddress!!.latitude
        val currentLong = selectedAddress!!.longitude

        //Get the data of the 2 takers
        firestore.collection("Order Takers").get().addOnSuccessListener {
            val taker1 = it.documents[0].toObject<OrderTakerData>()
            val taker2 = it.documents[1].toObject<OrderTakerData>()

            //Randomize selection if lat and long are not available
            if (currentLat == 0.0) {
                val randomNum = java.util.Random().nextInt(2)
                val ordersPoolData = OrdersPoolData()
                ordersPoolData.orderId = orderData.id
                ordersPoolData.status = "finding taker"
                ordersPoolData.userId = firebaseUser!!.uid
                ordersPoolData.orderReceivedTime = Calendar.getInstance().timeInMillis
                ordersPoolData.customerName = firebaseUser!!.uid

                if (randomNum == 0) {
                    ordersPoolData.orderTakerId = taker1!!.takerId

                }
                else {
                    ordersPoolData.orderTakerId = taker2!!.takerId
                }

                //Add the data to pool to notify the taker
                firestore.collection("Orders Pool").document(ordersPoolData.orderId.toString()).set(ordersPoolData)

                return@addOnSuccessListener

            }

            //Get distance between the added address and takers
            val distance1 = FloatArray(2)
            val distance2 = FloatArray(2)

            CoroutineScope(Dispatchers.IO).launch {

                Location.distanceBetween(currentLat, currentLong, taker1!!.locLat, taker1.locLong, distance1)
                Location.distanceBetween(currentLat, currentLong, taker2!!.locLat, taker2.locLong, distance2)

                delay(5000)

                val ordersPoolData = OrdersPoolData()
                ordersPoolData.orderId = orderData.id
                ordersPoolData.status = "finding taker"
                ordersPoolData.userId = firebaseUser!!.uid
                ordersPoolData.orderReceivedTime = Calendar.getInstance().timeInMillis
                ordersPoolData.customerName = firebaseUser!!.displayName!!

                if (distance1[0] > distance2[0]) {
                    ordersPoolData.orderTakerId = taker2.takerId

                }
                else {
                    ordersPoolData.orderTakerId = taker1.takerId
                }

                //Add the data to pool to notify the taker
                firestore.collection("Orders Pool").document(ordersPoolData.orderId.toString()).set(ordersPoolData)


            }
        }

    }

    fun proceedOrder() {
        when (paymentMethod) {
            GOOGLE_PAY -> {googlePay()}
            UPI -> upi()
            COD -> {
                paymentTime = 0

                binding.cktOrderPlacingSheenText.text = "Placing your order, please wait..."
                binding.cktOrderPlacingSheen.visibility = View.VISIBLE
                placeOrder("NA")
            }
        }
    }

    fun googlePay() {
        val GOOGLE_PAY_PACKAGE_NAME = "com.google.android.apps.nbu.paisa.user"

        val totalAmount = data.quantity * productData.price

        val reqUri = hashMapOf(
            "amount" to totalAmount,
            "transactionId" to UUID.randomUUID().toString().replace("-", "").subSequence(0, 12)
        )

     /*   val uri: Uri = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", "q647498796@ybl")
            .appendQueryParameter("pn", "Some App")
            .appendQueryParameter("tn", "payment for purchase")
            .appendQueryParameter("am", totalAmount.toString())
            .appendQueryParameter("cu", "INR")
            .build()*/

        functions.getHttpsCallable("pUri").call(reqUri).addOnCompleteListener {
            if (it.isSuccessful) {
                val result = it.result.data.toString()
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(result)
                intent.setPackage(GOOGLE_PAY_PACKAGE_NAME)
                launchGooglePay.launch(intent)
            }
            else {
                val snackbar = AppUtils.buildSnackbar(this, "Unable to proceed with payment, try again later", binding.root,false)
                snackbar.setAction("Ok") {
                    snackbar.dismiss()
                }
                snackbar.duration = Snackbar.LENGTH_INDEFINITE
                snackbar.show()
            }
        }

    }

    fun upi() {
        val totalAmount = data.quantity * productData.price

        val reqUri = hashMapOf(
            "amount" to totalAmount,
            "transactionId" to UUID.randomUUID().toString().replace("-", "").subSequence(0, 12)
        )

        //Launch the payment process
        functions.getHttpsCallable("pUri").call(reqUri).addOnCompleteListener {
            if (it.isSuccessful) {
                val result = it.result.data.toString()
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result))
                launchUPI.launch(Intent.createChooser(intent, "Select a UPI app"))
            }
            else {
                val snackbar = AppUtils.buildSnackbar(this, "Unable to proceed with payment, try again later", binding.root,false)
                snackbar.setAction("Ok") {
                    snackbar.dismiss()
                }
                snackbar.duration = Snackbar.LENGTH_INDEFINITE
                snackbar.show()
            }
        }

    }

/*
    private fun upiPaymentDataOperation(data: ArrayList<String>) {
        if (isConnectionAvailable(this@upi)) {
            var str = data[0]
            //Log.d("UPIPAY", "upiPaymentDataOperation: "+str);
            var paymentCancel = ""
            if (str == null) str = "discard"
            var status = ""
            var approvalRefNo = ""
            val response = str.split("&").toTypedArray()
            for (i in response.indices) {
                val equalStr = response[i].split("=").toTypedArray()
                if (equalStr.size >= 2) {
                    if (equalStr[0].lowercase(Locale.getDefault()) == "Status".lowercase(Locale.getDefault())) {
                        status = equalStr[1].lowercase(Locale.getDefault())
                    } else if (equalStr[0].lowercase(Locale.getDefault()) == "ApprovalRefNo".lowercase(
                            Locale.getDefault()
                        ) || equalStr[0].lowercase(Locale.getDefault()) == "txnRef".lowercase(
                            Locale.getDefault()
                        )
                    ) {
                        approvalRefNo = equalStr[1]
                    }
                } else {
                    paymentCancel = "Payment cancelled by user."
                }
            }
            if (status == "success") {
                //Code to handle successful transaction here.
                Toast.makeText(this@upi, "Transaction successful.", Toast.LENGTH_SHORT).show()
                // Log.d("UPI", "responseStr: "+approvalRefNo);
                Toast.makeText(
                    this,
                    "YOUR ORDER HAS BEEN PLACED\n THANK YOU AND ORDER AGAIN",
                    Toast.LENGTH_LONG
                ).show()
            } else if ("Payment cancelled by user." == paymentCancel) {
                Toast.makeText(this@upi, "Payment cancelled by user.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@upi, "Transaction failed.Please try again", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            Toast.makeText(
                this@upi,
                "Internet connection is not available. Please check and try again",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
*/

    private fun getUPIString(
        payeeAddress: String,
        payeeName: String,
        payeeMCC: String,
        trxnID: String,
        trxnRefId: String,
        trxnNote: String,
        payeeAmount: String,
        currencyCode: String,
        refUrl: String
    ): String? {
        val UPI = ("upi://pay?pa=" + payeeAddress + "&pn=" + payeeName
                + "&mc=" + payeeMCC + "&tid=" + trxnID + "&tr=" + trxnRefId
                + "&tn=" + trxnNote + "&am=" + payeeAmount + "&cu=" + currencyCode
                + "&refUrl=" + refUrl)
        return UPI.replace(" ", "+")
    }



}