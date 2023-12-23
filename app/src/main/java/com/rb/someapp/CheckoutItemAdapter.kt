package com.rb.someapp

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rb.someapp.databinding.CheckoutItemBinding

class CheckoutItemAdapter(): RecyclerView.Adapter<CheckoutItemAdapter.ViewHolder>() {

    lateinit var context: Context
    lateinit var dataList: MutableList<String>

    constructor(context: Context, dataList: MutableList<String>) : this() {
        this.context = context
        this.dataList = dataList
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = CheckoutItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(context).asDrawable().load(dataList[position]).into(holder.binding.cktItemImg)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    inner class ViewHolder(itemView: CheckoutItemBinding): RecyclerView.ViewHolder(itemView.root) {
        val binding = itemView

    }
}