package com.rb.someapp

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.Rating
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import android.view.animation.Animation
import android.widget.RatingBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.rb.someapp.databinding.ActivityProductPageBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

class ProductPage : AppCompatActivity() {
    lateinit var firestore: FirebaseFirestore

    lateinit var binding: ActivityProductPageBinding

    var quantity = 1

    var productData: ProductData? = null

    var isCartVisible = false

    var firebaseUser: FirebaseUser? = null

    var isFaved: Boolean = false

    lateinit var cartEventListener: EventListener<QuerySnapshot>

    lateinit var favEventListener: EventListener<DocumentSnapshot>

    lateinit var ratingBarChangeListener: RatingBar.OnRatingBarChangeListener

    lateinit var cartLauncher: ActivityResultLauncher<Intent>

    lateinit var infoImgList: MutableList<String>

    lateinit var infoImgSubList: MutableList<String>

    lateinit var infoImageAdapter: ProductImagesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductPageBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)

        } else {
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }

        window.statusBarColor = Color.TRANSPARENT

        val type = intent.getStringExtra("type")
        val docName = intent.getStringExtra("docname")

        firestore = Firebase.firestore
        firebaseUser = FirebaseAuth.getInstance().currentUser

        infoImgList = ArrayList()
        infoImgSubList = ArrayList()





        favEventListener = object : EventListener<DocumentSnapshot> {
            override fun onEvent(value: DocumentSnapshot?, error: FirebaseFirestoreException?) {
                isFaved = value!!.exists()

                //Set the favourite button colour
                if (value.exists()) {
                    binding.pFav.setImageDrawable(
                        ContextCompat.getDrawable(
                            this@ProductPage,
                            R.drawable.ic_round_favorite_30
                        )
                    )
                } else {
                    binding.pFav.setImageDrawable(
                        ContextCompat.getDrawable(
                            this@ProductPage,
                            R.drawable.ic_round_favorite_border_30
                        )
                    )

                }
            }
        }



        firestore.collection("Products").document("doc").collection(type!!).document(docName!!)
            .get().addOnSuccessListener {
                productData = it.toObject<ProductData>()



                binding.pTitle.text = productData!!.title
                binding.pInfo.text = productData!!.info
                binding.pToolbar.title = productData!!.name

                Glide.with(this).asDrawable().load(productData!!.imgUrl).into(binding.pImg)


                binding.pAvailability.text = "${productData!!.availability}"

                binding.pPrice.text = "${productData!!.price} ${getString(R.string.rs)}"

                setAvailability(productData!!.availability)


                if (firebaseUser != null) {
                    firestore.collection("Favourites").document("doc")
                        .collection(firebaseUser!!.uid)
                        .document(productData!!.id.toString()).addSnapshotListener(favEventListener)
                }


                //Get avg rating
                val reviewList = ArrayList<ReviewData>()
                it.reference.collection("Reviews").get().addOnSuccessListener { it3 ->
                    for (doc in it3.documents) {
                        val reviewData = doc.toObject<ReviewData>()
                        reviewList.add(reviewData!!)

                        //Calculate rating after adding the last item
                        if (reviewList.indexOf(reviewData) == (it3.documents.toMutableList().size - 1)) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val avgRating = calculateRating(reviewList)
                                withContext(Dispatchers.Main) {
                                    binding.pRating.rating = avgRating
                                }
                            }
                        }
                    }

                    //Set total number of reviews
                    binding.pRatingCount.text = it3.size().toString()
                }


                //Set the current rating if already given.
                if (firebaseUser != null) {
                    it.reference.collection("Reviews").document(firebaseUser!!.uid).get()
                        .addOnSuccessListener { it2 ->
                            if (it2.exists()) {
                                val reviewData = it2.toObject<ReviewData>()
                                binding.pAddRating.rating = reviewData!!.rating
                            }

                            binding.pAddRating.onRatingBarChangeListener = ratingBarChangeListener
                        }
                }

            }


        //Get the product info images
        var loadCount = 0
        firestore.collection("Products").document("doc").collection(type!!).document(docName!!).collection("Info Images").get().addOnSuccessListener {
            for (doc in it.documents) {
                val url = doc.get("imgUrl").toString()
                infoImgList.add(url)
                loadCount++

                if (loadCount == it.documents.size) {
                    val roundTransformation = RoundedCorners(AppUtils.dptopx(this, 10))



                    handleProductOptionSelection(0)
                }
            }
        }



        cartEventListener = object :
            EventListener<QuerySnapshot> {
            override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                if (error != null) {
                    return
                }

                //Handle visibility based on if the user has any items in cart
                if (value!!.isEmpty) {
                    isCartVisible = false
                    binding.pCartFAB.visibility = View.GONE
                } else {
                    isCartVisible = true
                    binding.pCartFAB.visibility = View.VISIBLE
                }
            }


        }
        if (firebaseUser != null) {
            firestore.collection("Cart").document("doc").collection(firebaseUser!!.uid)
                .addSnapshotListener(this, cartEventListener)


        }


        binding.pRating.progressDrawable.setTint(ContextCompat.getColor(this, R.color.blue))
        binding.pRating.progressBackgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.grey))

        binding.pAddRating.progressDrawable.setTint(ContextCompat.getColor(this, R.color.blue))
        binding.pAddRating.progressBackgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.grey3))


        //Listen for callbacks from cart activity
        cartLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            //RESULT_OK is returned only when user orders a cart item
            if (it.resultCode == RESULT_OK) {
                val snackbar =
                    AppUtils.buildSnackbar(this, "Order placed successfully!", binding.root, false)
                snackbar.duration = Snackbar.LENGTH_INDEFINITE
                snackbar.setAction("View") {
                    val intent = Intent(this, OrdersActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.activity_open, R.anim.activity_stable)
                }
                snackbar.show()
            }
        }



        binding.pScrollParent.viewTreeObserver.addOnScrollChangedListener {
            if (isCartVisible) {
                if (binding.pScrollParent.scrollY > 0) {
                    binding.pCartFAB.hide()
                } else {
                    binding.pCartFAB.show()
                }
            }

        }

        binding.pToolbar.setNavigationOnClickListener {
            finish()
        }

        binding.pAdd.setOnClickListener {
            quantity += 1
            binding.pQuantity.text = "$quantity"
        }

        binding.pRemove.setOnClickListener {
            if (quantity > 0) {
                quantity -= 1
                binding.pQuantity.text = "$quantity"
            }
        }

        binding.pAddToCartButton.setOnClickListener {
            val listener = object : Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {
                }

                override fun onAnimationEnd(p0: Animation?) {
                    if (productData != null) {
                        addToCart()
                    }
                }

                override fun onAnimationRepeat(p0: Animation?) {
                }

            }
            it.startAnimation(AnimUtils.pressAnim(listener))
        }

        binding.pCartFAB.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            cartLauncher.launch(intent)
            overridePendingTransition(R.anim.activity_open, R.anim.activity_stable)

        }

        binding.pFav.setOnClickListener {
            if (firebaseUser == null) {
                askSignIn("Please sign in to add this item to your favourites")

                return@setOnClickListener
            }

            if (!isFaved) {
                val favouriteData = FavouriteData()
                favouriteData.productId = productData!!.id
                favouriteData.productType = productData!!.type
                favouriteData.addedTime = Calendar.getInstance().timeInMillis

                firestore.collection("Favourites").document("doc").collection(firebaseUser!!.uid)
                    .document(favouriteData.productId.toString()).set(favouriteData)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Added this item to your favourites",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            } else {
                firestore.collection("Favourites").document("doc").collection(firebaseUser!!.uid)
                    .document(productData!!.id.toString()).delete().addOnSuccessListener {
                        val snackbar = AppUtils.buildSnackbar(
                            this,
                            "Removed this item from your favourites",
                            binding.root,
                            false
                        )
                        snackbar.duration = Snackbar.LENGTH_LONG
                        snackbar.show()
                    }

            }


        }


        ratingBarChangeListener =
            RatingBar.OnRatingBarChangeListener { ratingBar, fl, b ->
                addUserReview(fl)
            }

    }

    override fun onResume() {
        if (firebaseUser != null) {
            firestore.collection("Cart").document("doc").collection(firebaseUser!!.uid)
                .addSnapshotListener(this, cartEventListener)
        }
        super.onResume()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.activity_stable, R.anim.activity_close)
    }

    fun addToCart() {
        val cartData = CartData()
        cartData.id = java.util.Random().nextInt(1000000)
        cartData.productId = productData!!.id
        cartData.quantity = quantity
        cartData.addedTime = Calendar.getInstance().timeInMillis
        cartData.type = productData!!.type

        if (firebaseUser != null) {
            firestore.collection("Cart").document("doc").collection(firebaseUser!!.uid)
                .document(cartData.id.toString()).set(cartData).addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(
                            this,
                            "${productData!!.name} has been added to your cart",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Sorry, unable to add to your cart. Try again later",
                            Toast.LENGTH_LONG
                        ).show()

                    }
                }
        } else {
            val snackbar = AppUtils.buildSnackbar(
                this,
                "Please sign in to add this item to your cart",
                binding.root,
                false
            )
            snackbar.duration = Snackbar.LENGTH_INDEFINITE
            snackbar.setAction("Sign in") {
                val intent = Intent(this, ProfilePage::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.activity_open, R.anim.activity_stable)
            }
            snackbar.show()

        }
    }

    private fun setAvailability(availability: String) {
        binding.pAvailability.text = availability

        if (availability == AppUtils.PRODUCT_AVAILABLE) {
            binding.pAvailability.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue))
        } else {
            binding.pAvailability.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.black))

        }
    }

    private suspend fun calculateRating(reviewList: MutableList<ReviewData>): Float {
        var ratingsSum = 0f


        for (review in reviewList) {
            ratingsSum += review.rating

            if (reviewList.indexOf(review) == reviewList.size - 1) {
                val avgRating = ratingsSum / reviewList.size
                return avgRating
            }
        }

        return 0f
    }

    private fun addUserReview(rating: Float) {

        if (firebaseUser == null) {
            val snackbar = AppUtils.buildSnackbar(
                this,
                "Please sign in to place your review for this product",
                binding.root,
                false
            )
            snackbar.duration = Snackbar.LENGTH_INDEFINITE
            snackbar.setAction("Sign in") {
                val intent = Intent(this, ProfilePage::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.activity_open, R.anim.activity_stable)
            }
            snackbar.show()
            return
        }

        val review = ReviewData()
        review.review = ""
        review.rating = rating
        review.uid = firebaseUser!!.uid

        //Set, update the review
        firestore.collection("Products").document("doc").collection(productData!!.type)
            .whereEqualTo("id", productData!!.id).get().addOnSuccessListener {
                for (doc in it.documents) {
                    doc.reference.collection("Reviews").document(review.uid).set(review)

                }
            }

        AppUtils.buildSnackbar(this, "Your rating has been submitted", binding.root, false).show()

    }

    private fun preRegisterUser() {

        val data = PreRegisterData()
        data.userId = firebaseUser!!.uid
        data.productId = productData!!.id
        if (firebaseUser!!.email != null) {
            data.contactEmail = firebaseUser!!.email!!
        }
        if (firebaseUser!!.phoneNumber != null) {
            data.contactNum = firebaseUser!!.phoneNumber!!
        }



        firestore.collection("Pre-registered").document("doc").collection(firebaseUser!!.uid)
            .document(productData!!.id.toString()).addSnapshotListener { value, error ->
                if (value!!.exists()) {
                    AppUtils.buildSnackbar(this, "You have already registered.", binding.root, false).show()
                }
                else {
                    firestore.collection("Pre-registered").document("doc").collection(firebaseUser!!.uid)
                        .document(productData!!.id.toString()).set(data).addOnCompleteListener {
                            if (it.isSuccessful) {
                                AppUtils.buildSnackbar(this, "Pre-registered successfully! Thank you for your support.", binding.root, false).show()
                            }
                            else {
                                AppUtils.buildSnackbar(this, "Unable to pre-register as of now, try again later.", binding.root, false).show()
                            }
                        }
                }
        }

    }

    fun askSignIn(msg: String){
        val snackbar = AppUtils.buildSnackbar(
            this,
            msg,
            binding.root,
            false
        )
        snackbar.duration = Snackbar.LENGTH_INDEFINITE
        snackbar.setAction("Sign in") {
            val intent = Intent(this, ProfilePage::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.activity_open, R.anim.activity_stable)
        }
        snackbar.show()
    }

    fun handleProductOptionSelection(position: Int) {
        if (position == 0) {
            val list = infoImgList.subList(0, 2)


            infoImgSubList.clear()
            infoImgSubList.addAll(list)
            infoImageAdapter.notifyDataSetChanged()
        }

        if (position == 1) {
            val list = infoImgList.subList(2, 4)
            list.reverse()


            infoImgSubList.clear()
            infoImgSubList.addAll(list)
            infoImageAdapter.notifyDataSetChanged()
        }

        if (position == 2) {
            val list = infoImgList.subList(4, 6)
            list.reverse()

            infoImgSubList.clear()
            infoImgSubList.addAll(list)
            infoImageAdapter.notifyDataSetChanged()
        }
    }

}