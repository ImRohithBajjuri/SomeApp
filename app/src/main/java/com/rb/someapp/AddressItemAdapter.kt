package com.rb.someapp

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView

import com.rb.someapp.databinding.AddressItemBinding

class AddressItemAdapter(): RecyclerView.Adapter<AddressItemAdapter.ViewHolder>() {
    lateinit var context: Context
    lateinit var dataList: MutableList<AddressData>
    lateinit var listener: AddressListener
    var isSelection = false

    interface AddressListener {
        fun itemAdded(addressData: AddressData)

        fun itemEdited(addressData: AddressData)

        fun itemRemoved(addressData: AddressData)
    }

    constructor(context: Context, dataList: MutableList<AddressData>, listener: AddressListener, isSelection: Boolean) : this() {
        this.context = context
        this.dataList = dataList
        this.listener = listener
        this.isSelection = isSelection
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = AddressItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.addressItemName.text = dataList[position].name
        holder.binding.addressItemLaneOne.text = dataList[position].laneOne
        holder.binding.addressItemLaneTwo.text = dataList[position].laneTwo
        holder.binding.addressItemDes.text = "${dataList[position].city}, ${dataList[position].pinCode}"

        holder.binding.addressItemLaneTwo.visibility = View.GONE

        if (dataList[position].laneTwo.trim().isEmpty()) {
            holder.binding.addressItemLaneTwo.visibility = View.GONE
        }
        else {
            holder.binding.addressItemLaneTwo.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    inner class ViewHolder(itemView: AddressItemBinding): RecyclerView.ViewHolder(itemView.root){
        var binding: AddressItemBinding = itemView

        init {
            binding.addressItemCard.setOnClickListener {
                val anim = AnimUtils.pressAnim(null)
                it.startAnimation(anim)
                if (isSelection) {
                    val intent = Intent()
                    intent.putExtra("id", dataList[adapterPosition].addressID)
                    (context as AddressActivity).setResult(AppCompatActivity.RESULT_OK, intent)
                    (context as AddressActivity).finish()
                    (context as AddressActivity).overridePendingTransition(R.anim.activity_stable, R.anim.activity_close)
                }
                else {
                    val addressItemSheet = AddressOptionsSheet(dataList[adapterPosition], listener)
                    addressItemSheet.show((context as AppCompatActivity).supportFragmentManager, "UseCaseOne")
                }

            }

            binding.addressItemOptions.setOnClickListener {
                val addressItemSheet = AddressOptionsSheet(dataList[adapterPosition], listener)
                addressItemSheet.show((context as AppCompatActivity).supportFragmentManager, "UseCaseOne")
            }

            binding.addressItemCard.setOnLongClickListener {
                val addressItemSheet = AddressOptionsSheet(dataList[adapterPosition], listener)
                addressItemSheet.show((context as AppCompatActivity).supportFragmentManager, "UseCaseOne")
                true
            }
        }
    }


}