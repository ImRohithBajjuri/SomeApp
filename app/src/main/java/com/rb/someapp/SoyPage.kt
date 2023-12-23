package com.rb.someapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.rb.someapp.databinding.FragmentSoyPageBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SoyPage : Fragment() {
    lateinit var binding: FragmentSoyPageBinding

    lateinit var firestore: FirebaseFirestore

    lateinit var adapter: ProductsAdapter
    lateinit var dataList: MutableList<ProductUiData>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSoyPageBinding.inflate(inflater, container, false)


        firestore = Firebase.firestore

        dataList = ArrayList()

        val layoutManager = LinearLayoutManager(requireActivity())
        binding.soyRecy.layoutManager = layoutManager

        adapter = ProductsAdapter(requireActivity(), dataList)
        binding.soyRecy.adapter = adapter


        binding.soyRecy.addOnScrollListener(object : RecyclerView.OnScrollListener(){
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    (requireActivity() as MainActivity).showCart(true)
                }
                else {
                    (requireActivity() as MainActivity).showCart(false)
                }
                super.onScrolled(recyclerView, dx, dy)
            }
        })


        firestore.collection("Products").document("doc").collection("Soy").get().addOnSuccessListener {
            for (doc in it.documents) {
                val data = doc.toObject<ProductData>()
                val productUiData = ProductUiData()
                productUiData.productData = data

                CoroutineScope(Dispatchers.Main).launch {
                    dataList.add(productUiData)
                    adapter.notifyItemInserted(dataList.size-1)
                }

                val reviewList = ArrayList<ReviewData>()
                doc.reference.collection("Reviews").get().addOnSuccessListener { it2 ->
                    for (doc2 in it2.documents) {
                        val reviewData = doc2.toObject<ReviewData>()
                        reviewList.add(reviewData!!)

                        if (reviewList.indexOf(reviewData) == (it2.toMutableList().size - 1)) {
                            CoroutineScope(Dispatchers.IO).launch {
                                adapter.calculateRating(reviewList, productUiData)
                            }
                        }
                    }
                }
            }
        }

        return binding.root

    }


}