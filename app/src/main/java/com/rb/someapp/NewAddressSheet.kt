package com.rb.someapp

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.rb.someapp.databinding.FragmentNewAddressSheetBinding
import java.util.*
import java.util.jar.Manifest

class NewAddressSheet() : BottomSheetDialogFragment() {

    lateinit var binding: FragmentNewAddressSheetBinding
    lateinit var firestore: FirebaseFirestore

    lateinit var listener: AddressItemAdapter.AddressListener

    lateinit var usingFor: String

    var addressData: AddressData? = null

    var lat: Double = 0.0
    var long: Double = 0.0

    lateinit var locationManager: LocationManager

    var firebaseUser: FirebaseUser? = null

    companion object{
        val EDIT = "edit"
        val CREATE = "create"
    }

    constructor(listener: AddressItemAdapter.AddressListener, usingFor: String, addressData: AddressData?) : this() {
        this.listener = listener
        this.usingFor = usingFor
        this.addressData = addressData
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogStyle)
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentNewAddressSheetBinding.inflate(inflater, container, false)

        firestore = Firebase.firestore

        firebaseUser = FirebaseAuth.getInstance().currentUser

        //Set the header, data and action according to the using For
        if (usingFor == CREATE) {
            binding.newAddressSheetHeader.text = "Add a new address"
            binding.saveAddressTxt.text = "Save address"
        }
        else {
            binding.newAddressSheetHeader.text = addressData!!.name
            binding.saveAddressTxt.text = "Edit and save address"

            setCurrentAddress()
        }

        binding.saveAddressButton.setOnClickListener {
            val anim = AnimUtils.pressAnim(null)
            it.startAnimation(anim)

            if (usingFor == CREATE) {
                addNewAddress()
            }
            else {
                editAndSaveAddress()
            }

        }

        binding.newAddressSheetHeader.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                fillLocations()
            }
            else {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), 21)
            }
        }

        return binding.root
    }

    override fun dismiss() {
        super.dismiss()
    }

    fun addNewAddress() {

        val data = AddressData()
        data.addressID = Random().nextInt(1000000)
        data.landmark = binding.addressLandmark.text.toString()
        data.laneOne = binding.addressLaneOne.text.toString()
        data.laneTwo = binding.addressLaneTwo.text.toString()
        data.city = binding.addressCity.text.toString()
        data.name = binding.addressFullName.text.toString()
        data.number = binding.addressPhoneNumber.text.toString().toLong()
        data.notes = binding.addressNotes.text.toString()
        data.pinCode = binding.addressPincode.text.toString().toLong()
        data.addedTime = Calendar.getInstance(TimeZone.getDefault()).timeInMillis
        data.latitude = lat
        data.longitude = long

        if (firebaseUser != null) {
            firestore.collection("Address").document("doc").collection(firebaseUser!!.uid).document(data.addressID.toString()).set(data).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(requireActivity(), "New address added!", Toast.LENGTH_SHORT).show()
                    listener.itemAdded(data)
                    dismiss()
                }
                else {
                    Toast.makeText(requireActivity(), "Unable to add new address, please try again later", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }


    fun setCurrentAddress(){
        binding.addressLandmark.setText(addressData!!.landmark, TextView.BufferType.EDITABLE)
        binding.addressLaneOne.setText(addressData!!.laneOne, TextView.BufferType.EDITABLE)
        binding.addressLaneTwo.setText(addressData!!.laneTwo, TextView.BufferType.EDITABLE)
        binding.addressCity.setText(addressData!!.city, TextView.BufferType.EDITABLE)
        binding.addressFullName.setText(addressData!!.name, TextView.BufferType.EDITABLE)
        binding.addressPhoneNumber.setText(addressData!!.number.toString(), TextView.BufferType.EDITABLE)
        binding.addressNotes.setText(addressData!!.notes, TextView.BufferType.EDITABLE)
        binding.addressPincode.setText(addressData!!.pinCode.toString(), TextView.BufferType.EDITABLE)
    }

    fun editAndSaveAddress() {
        addressData!!.landmark = binding.addressLandmark.text.toString()
        addressData!!.laneOne = binding.addressLaneOne.text.toString()
        addressData!!.laneTwo = binding.addressLaneTwo.text.toString()
        addressData!!.city = binding.addressCity.text.toString()
        addressData!!.name = binding.addressFullName.text.toString()
        addressData!!.number = binding.addressPhoneNumber.text.toString().toLong()
        addressData!!.notes = binding.addressNotes.text.toString()
        addressData!!.pinCode = binding.addressPincode.text.toString().toLong()
        addressData!!.editedTime = Calendar.getInstance(TimeZone.getDefault()).timeInMillis

        if (firebaseUser != null) {
            firestore.collection("Address").document("doc").collection(firebaseUser!!.uid).document(addressData!!.addressID.toString()).set(addressData!!).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(requireActivity(), "Address edited", Toast.LENGTH_SHORT).show()
                    listener.itemEdited(addressData!!)
                    dismiss()
                }
                else {
                    Toast.makeText(requireActivity(), "Unable to edit the address, please try again later", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    @SuppressLint("MissingPermission")
    fun fillLocations(){
        locationManager = requireActivity().getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 1000, 10f) { p0 ->
            val geoCoder = Geocoder(requireActivity())
            val address = geoCoder.getFromLocation(p0.latitude, p0.longitude, 1)


            //Set the addresses according to the field they belong
            binding.addressPincode.setText(address!![0].postalCode, TextView.BufferType.EDITABLE)
            binding.addressCity.setText(address[0].locality, TextView.BufferType.EDITABLE)

            if (address[0].getAddressLine(0) != null) {
                val lane = address[0].getAddressLine(0)
                val subLocality = address[0].subLocality
                binding.addressLaneOne.setText(lane.substring(0, lane.indexOf(subLocality)))
                binding.addressLaneTwo.setText(
                    lane.substring(
                        lane.indexOf(subLocality),
                        lane.length
                    )
                )

            }

            //Set latitude abd longitude
            lat = p0.latitude
            long = p0.longitude
        }
    }
}