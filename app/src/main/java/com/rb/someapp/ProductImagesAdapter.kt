package com.rb.someapp

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rb.someapp.databinding.ProductImageItemBinding

class ProductImagesAdapter(): RecyclerView.Adapter<ProductImagesAdapter.ViewHolder>() {
    lateinit var context: Context
    lateinit var dataList: MutableList<String>

    constructor(context: Context, dataList: MutableList<String>) : this() {
        this.context = context
        this.dataList = dataList
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ProductImageItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(context).asDrawable().load(dataList[position]).override(300).into(holder.binding.productImgItem)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    inner class ViewHolder(binder: ProductImageItemBinding): RecyclerView.ViewHolder(binder.root) {
        val binding = binder
    }
}