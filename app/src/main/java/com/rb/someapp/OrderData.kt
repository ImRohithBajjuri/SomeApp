package com.rb.someapp

class OrderData {
    var id: Int = 0
    var productId: Int = 0
    var orderedTime: Long = 0
    var cartData: CartData? = null
    var orderStatus: String = "order status"
    var paymentMethod: String = "payment method"
    var paymentStatus: String = "payment status"
    var paymentTime: Long = 0
    var transactionRefId: String = ""
    var orderPrice: Float? = 0f
    var uid: String = "1"
    var selectedAddress: Int = 0
    var userNote: String = ""
    var shippingData: ShippingData? = null

}