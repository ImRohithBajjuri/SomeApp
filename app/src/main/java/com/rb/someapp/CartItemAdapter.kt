package com.rb.someapp

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rb.someapp.databinding.CartItemBinding

class CartItemAdapter(): RecyclerView.Adapter<CartItemAdapter.ViewHolder>(){
    lateinit var context: Context
    lateinit var dataList: MutableList<CartUiData>
    lateinit var launcher: ActivityResultLauncher<Intent>

    constructor(context: Context, dataList: MutableList<CartUiData>, launcher: ActivityResultLauncher<Intent>) : this() {
        this.context = context
        this.dataList = dataList
        this.launcher = launcher
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = CartItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.cartItemName.text = dataList[position].productData!!.name
        val normalTime = AppUtils.getNormalTime(dataList[position].cartData!!.addedTime)
        holder.binding.cartItemDate.text = normalTime
        holder.binding.cartItemQuantity.text = "${dataList[position].cartData!!.quantity}"

        val price = dataList[position].productData!!.price * dataList[position].cartData!!.quantity
        holder.binding.cartItemPrice.text = "$price ${context.getString(R.string.rs)}"

        Glide.with(context).asDrawable().load(dataList[position].productData!!.imgUrl).into(holder.binding.cartImg)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    inner class ViewHolder(itemView: CartItemBinding): RecyclerView.ViewHolder(itemView.root) {
        var binding = itemView
        init {
            binding.cartItemCard.setOnClickListener {
                val anim = AnimUtils.pressAnim(object : Animation.AnimationListener {
                    override fun onAnimationStart(p0: Animation?) {

                    }

                    override fun onAnimationEnd(p0: Animation?) {
                        val intent = Intent(context, CheckoutActivity::class.java)
                        intent.putExtra("id", dataList[adapterPosition].cartData!!.id)
                        launcher.launch(intent)
                        (context as AppCompatActivity).overridePendingTransition(R.anim.activity_open, R.anim.activity_stable)
                    }

                    override fun onAnimationRepeat(p0: Animation?) {
                    }

                })
                it.startAnimation(anim)
            }
        }
    }
}