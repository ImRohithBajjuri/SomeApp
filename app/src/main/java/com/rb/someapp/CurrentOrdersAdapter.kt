package com.rb.someapp

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.rb.someapp.databinding.CurrentOrderItemBinding
import com.rb.someapp.databinding.OrderItemBinding

class CurrentOrdersAdapter(): RecyclerView.Adapter<CurrentOrdersAdapter.ViewHolder>() {
    lateinit var context: Context
    lateinit var dataList: MutableList<OrderUiData>

    constructor(context: Context, dataList: MutableList<OrderUiData>) : this() {
        this.context = context
        this.dataList = dataList
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = CurrentOrderItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.currentOrderItemName.text = "${dataList[position].productData!!.name} x ${dataList[position].orderData!!.cartData!!.quantity}"

        holder.binding.currentOrderItemTotalPrice.text = "${dataList[position].orderData!!.orderPrice} ${context.getString(R.string.rs)}"

        val normalTime = AppUtils.getNormalTime(dataList[position].orderData!!.orderedTime)

        holder.binding.currentOrderItemTime.text = normalTime

        holder.binding.currentOrderItemStatus.text = dataList[position].orderData!!.orderStatus

        Glide.with(context).asDrawable().load(dataList[position].productData!!.imgUrl).into(holder.binding.currentOrderItemImg)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }



    inner class ViewHolder(itemView: CurrentOrderItemBinding): RecyclerView.ViewHolder(itemView.root) {
        var binding = itemView

        init {

            binding.currentOrderItemCard.setOnClickListener {
                val anim = AnimUtils.pressAnim(object : Animation.AnimationListener {
                    override fun onAnimationStart(p0: Animation?) {

                    }

                    override fun onAnimationEnd(p0: Animation?) {
                        val intent = Intent(context, OrderInfoActivity::class.java)
                        intent.putExtra("id", dataList[adapterPosition].orderData!!.id)
                        context.startActivity(intent)
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