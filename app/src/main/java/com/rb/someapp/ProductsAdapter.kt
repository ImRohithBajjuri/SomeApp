package com.rb.someapp

import android.app.ActivityOptions
import android.app.AppComponentFactory
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.rb.someapp.databinding.ProductItemBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductsAdapter(): RecyclerView.Adapter<ProductsAdapter.ViewHolder>() {

    lateinit var context: Context
    lateinit var dataList: MutableList<ProductUiData>

    constructor(context: Context, dataList: MutableList<ProductUiData>) : this() {
        this.context = context
        this.dataList = dataList
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ProductItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.productName.text = dataList[position].productData!!.name
        holder.binding.productDes.text = dataList[position].productData!!.description
        holder.binding.productPrice.text = "Cost per each: ${dataList[position].productData!!.price.toString()} ${context.getString(R.string.rs)}"

        //Set the rating of the product
/*
        CoroutineScope(Dispatchers.IO).launch {
            if (dataList[position].reviewList.isEmpty()) {

                withContext(Dispatchers.Main) {
                    holder.binding.rating.rating = 0f
                }
            }
            else {
                var ratingsSum = 0f
                for (review in dataList[position].reviewList) {
                    ratingsSum += review.rating
                }

                val totalRating = ratingsSum / dataList[position].reviewList.size

                withContext(Dispatchers.Main) {
                    holder.binding.rating.rating = totalRating
                }
            }
        }

        holder.binding.ratingCount.text = "${dataList[position].reviewList.size}"
*/

        holder.binding.rating.rating = dataList[position].avgRating
        holder.binding.ratingCount.text = dataList[position].ratingCount.toString()

        CoroutineScope(Dispatchers.Main).launch {
            Glide.with(context).asDrawable().load(dataList[position].productData!!.imgUrl).into(holder.binding.productImg)
        }


    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    inner class ViewHolder(view: ProductItemBinding): RecyclerView.ViewHolder(view.root) {
        val binding = view

        init {
            binding.rating.progressDrawable.setTint(ContextCompat.getColor(context, R.color.blue))
            binding.rating.progressBackgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.grey))

            binding.productCard.setOnClickListener {
                val listener = object : Animation.AnimationListener {
                    override fun onAnimationStart(p0: Animation?) {

                    }

                    override fun onAnimationEnd(p0: Animation?) {
                        val intent = Intent(context, ProductPage::class.java)
                        intent.putExtra("type", dataList[adapterPosition].productData!!.type)
                        intent.putExtra("docname", dataList[adapterPosition].productData!!.name)
                        context.startActivity(intent)
                        (context as AppCompatActivity).overridePendingTransition(R.anim.activity_open, R.anim.activity_stable)
                    }

                    override fun onAnimationRepeat(p0: Animation?) {
                    }

                }
                it.startAnimation(AnimUtils.pressAnim(listener))

            }
        }

    }

    suspend fun calculateRating(reviewList: MutableList<ReviewData>, productUiData: ProductUiData) {
        var ratingsSum = 0f


        for (review in reviewList) {
            ratingsSum += review.rating

            if (reviewList.indexOf(review) == reviewList.size - 1) {
                val avgRating = ratingsSum / reviewList.size
                val position = dataList.indexOf(productUiData)
                dataList[position].avgRating = avgRating
                dataList[position].ratingCount = reviewList.size
                withContext(Dispatchers.Main) {
                    notifyItemChanged(position)
                }
            }
        }

    }

    suspend fun calculateRating2(firestore: FirebaseFirestore) {
        for (product in dataList) {
            val reviewList = ArrayList<ReviewData>()
            var ratingsSum = 0f
            val position = dataList.indexOf(product)
            firestore.collection("Products").document("doc").collection(product.productData!!.type).whereEqualTo("id", product.productData!!.id).get().addOnSuccessListener {
                it.documents[0].reference.collection("Reviews").get().addOnSuccessListener {
                    for (doc in it.documents) {
                        val data = doc.toObject<ReviewData>()
                        reviewList.add(data!!)
                        ratingsSum += data.rating

                        if (reviewList.indexOf(data) == it.documents.toMutableList().size - 1) {
                            val avgRating = ratingsSum / reviewList.size
                            dataList[position].avgRating = avgRating
                            dataList[position].ratingCount = reviewList.size
                            CoroutineScope(Dispatchers.Main).launch {
                                notifyItemChanged(position)
                            }
                        }
                    }
                }
            }
        }
    }

}