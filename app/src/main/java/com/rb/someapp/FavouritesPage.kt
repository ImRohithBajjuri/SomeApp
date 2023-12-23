package com.rb.someapp

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsetsController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.rb.someapp.databinding.ActivityFavouritesPageBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavouritesPage : AppCompatActivity() {
    lateinit var binding: ActivityFavouritesPageBinding

    lateinit var firestore: FirebaseFirestore

    var firebaseUser: FirebaseUser? = null

    lateinit var dataList: MutableList<ProductUiData>

    lateinit var adapter: ProductsAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavouritesPageBinding.inflate(LayoutInflater.from(this))
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



        firebaseUser = FirebaseAuth.getInstance().currentUser

        //Check if user is signed in and stop the activity process
        if (firebaseUser == null) {
            binding.favSignInLayout.visibility = View.VISIBLE
            return
        }

        firestore = Firebase.firestore


        dataList = ArrayList()
        val layoutManager = LinearLayoutManager(this)
        binding.favRecy.layoutManager = layoutManager
        adapter = ProductsAdapter(this, dataList)
        binding.favRecy.adapter = adapter


        //Get the favourite products
        if (firebaseUser != null) {
            firestore.collection("Favourites").document("doc").collection(firebaseUser!!.uid).get().addOnSuccessListener { it ->
                for (doc in it.documents) {
                    val favData = doc.toObject<FavouriteData>()
                    firestore.collection("Products").document("doc").collection(favData!!.productType).whereEqualTo("id", favData.productId).get().addOnSuccessListener {it2 ->
                        val data = it2.documents[0].toObject<ProductData>()!!
                        val productUiData = ProductUiData()
                        productUiData.productData = data

                        dataList.add(productUiData)
                        adapter.notifyItemInserted(dataList.size - 1)

                        val reviewList = ArrayList<ReviewData>()
                        doc.reference.collection("Reviews").get().addOnSuccessListener { it3 ->
                            for (doc2 in it3.documents) {
                                val reviewData = doc2.toObject<ReviewData>()
                                reviewList.add(reviewData!!)

                                if (reviewList.indexOf(reviewData) == (it3.toMutableList().size - 1)) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        adapter.calculateRating(reviewList, productUiData)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        binding.favToolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.activity_stable, R.anim.activity_close)
    }


}