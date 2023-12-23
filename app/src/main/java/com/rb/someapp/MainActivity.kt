package com.rb.someapp

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsetsController
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.rb.someapp.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {
    lateinit var firestore : FirebaseFirestore

    private lateinit var binding: ActivityMainBinding

    var isCartVisible = false

    lateinit var cartEventListener: EventListener<QuerySnapshot>

    var firebaseUser: FirebaseUser? = null

     lateinit var cartLauncher: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
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


        if (firebaseUser != null) {
            Glide.with(this).asDrawable().load(firebaseUser!!.photoUrl).override(AppUtils.dptopx(this, 30)).centerCrop().circleCrop().into(binding.mainProfileIcon)

        }


        val pagerAdapter = PagerAdapter(this, 2)
        binding.mainPager.adapter = pagerAdapter
        binding.mainPager.offscreenPageLimit = 2


        binding.mainTabBar.setTabTextColors(ContextCompat.getColor(this, R.color.blue), ContextCompat.getColor(this, R.color.blue))

        TabLayoutMediator(binding.mainTabBar, binding.mainPager){tab, position ->
            if (position == 0) {
                tab.text = "Milk"
            }
            if (position == 1) {
                tab.text = "Soy"
            }

        }.attach()



        cartEventListener = object : EventListener<QuerySnapshot> {
            override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                if (error != null) {
                    return
                }

                //Handle visibility based on if the user has any items in cart
                if (value!!.isEmpty) {
                    isCartVisible = false
                    binding.mainCartFAB.visibility = View.GONE
                }
                else {
                    isCartVisible = true
                    binding.mainCartFAB.visibility = View.VISIBLE
                }
            }
        }
        if (firebaseUser != null) {
            firestore.collection("Cart").document("doc").collection(firebaseUser!!.uid).addSnapshotListener(this, cartEventListener)
        }


        //Listen for callbacks from cart activity
        cartLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            //RESULT_OK is returned only when user orders a cart item
            if (it.resultCode == RESULT_OK) {
               val snackbar = AppUtils.buildSnackbar(this, "Order placed successfully!", binding.root,false)
                snackbar.duration = Snackbar.LENGTH_INDEFINITE
                snackbar.setAction("View") {
                    val intent = Intent(this, OrdersActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.activity_open, R.anim.activity_stable)
                }
                snackbar.show()
            }
        }


        binding.mainCartFAB.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            cartLauncher.launch(intent)
            overridePendingTransition(R.anim.activity_open, R.anim.activity_stable)
        }

        binding.mainProfileIcon.setOnClickListener {
            val intent = Intent(this, ProfilePage::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.activity_open, R.anim.activity_stable)
        }




    }

    override fun onResume() {
        if (firebaseUser != null) {
            firestore.collection("Cart").document("doc").collection(firebaseUser!!.uid).addSnapshotListener(this, cartEventListener)
        }
        super.onResume()
    }


    fun showCart(show: Boolean) {
        if (isCartVisible) {
            if (show) {
                binding.mainCartFAB.show()
            }
            else {
                binding.mainCartFAB.hide()
            }
        }
    }

    fun solveN(x: Int): Int {
        var n = x - (2*x) + (6*x)

        if (n == 10) {
            Toast.makeText(this, x.toString(), Toast.LENGTH_LONG).show()

            return x
        }
        else {
            solveN(x -1)
        }

        return 0;
    }

    fun uploadData() {
     /*   val paneer = ProductData()
        paneer.name = "Soy milK"
        paneer.type = "soy"

        firestore.collection("Products").document("doc").collection("Soy").document(paneer.name).set(paneer)


        val butter = ProductData()
        butter.name = "Soy sauce"
        butter.type = "soy"
        firestore.collection("Products").document("doc").collection("Soy").document(butter.name).set(butter)


        val ghee = ProductData()
        ghee.name = "Soybean oil"
        ghee.type = "soy"
        firestore.collection("Products").document("doc").collection("Soy").document(ghee.name).set(ghee)


        val yogurt = ProductData()
        yogurt.name = "Tofu"
        yogurt.type = "soy"
        firestore.collection("Products").document("doc").collection("Soy").document(yogurt.name).set(yogurt)


        val kowa = ProductData()
        kowa.name = "Soy cheese"
        kowa.type = "soy"
        firestore.collection("Products").document("doc").collection("Soy").document(kowa.name).set(kowa)


        val cheese = ProductData()
        cheese.name = "Soy yogurt"
        cheese.type = "soy"
        firestore.collection("Products").document("doc").collection("Soy").document(cheese.name).set(cheese)


        val milkPowder = ProductData()
        milkPowder.name = "Soy butter"
        milkPowder.type = "soy"
        firestore.collection("Products").document("doc").collection("Soy").document(milkPowder.name).set(milkPowder)
*/

       /* val lat1 = 17.373807080624303
        val long1 = 78.55223067261674

        val lat2 = 17.363649360309307
        val long2 = 78.47481138904107



        val distance = FloatArray(5)

        CoroutineScope(Dispatchers.IO).launch {
            Location.distanceBetween(lat1, long1, lat2, long2, distance)

            Location.distanceBetween(lat1, long1, lat2, long2, distance)

            delay(5000)

        }*/

  /*      val shaker = ProductData()
        shaker.name = "Bottle shaker"
        shaker.type = "Accessories"
        shaker.id = Random().nextInt(1000000)
        shaker.availability = AppUtils.PRODUCT_AVAILABLE

        firestore.collection("Products").document("doc").collection(shaker.type).document(shaker.name).set(shaker)

*/
    }


/*
    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val theta = lon1 - lon2
        var dist = (Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + (Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta))))
        dist = Math.acos(dist)
        dist = rad2deg(dist)
        dist = dist * 60 * 1.1515
        return dist
    }

    private fun deg2rad(deg: Double): Double {
        return deg * Math.PI / 180.0
    }

    private fun rad2deg(rad: Double): Double {
        return rad * 180.0 / Math.PI
    }*/
}