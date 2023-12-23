package com.rb.someapp

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.util.TypedValue
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.firebase.ui.auth.AuthUI
import com.google.android.material.snackbar.Snackbar
import java.util.*

class AppUtils {
    companion object {

        //Order statuses
        val ORDER_PLACED = "order placed"
        val ORDER_CANCELLED = "order cancelled"
        val ORDER_COMPLETED = "order completed"
        val ORDER_SHIPPED = "order shipped"
        val ORDER_OUT_FOR_DLV = "out for delivery"


        //Payment statuses
        val PAYMENT_PAID = "paid"
        val PAYMENT_PENDING = "pending"
        val PAYMENT_NOT_PAID = "payment not paid"

        //Product availability
        val PRODUCT_AVAILABLE = "available"
        val PRODUCT_UNAVAILABLE = "unavailable"

        //Delivery status
        val DLV_YET_TO_START = "yet to start"
        val DLV_ON_THE_WAY = "on the way"
        val DLV_COMPLETED = "completed"
        val DLV_CANCELLED = "cancelled"


        fun sptopx(context: Context, toconvertvalue: Int): Int {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                toconvertvalue.toFloat(),
                context.resources.displayMetrics
            ).toInt()
        }

        fun dptopx(context: Context, toconvertvalue: Int): Int {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                toconvertvalue.toFloat(),
                context.resources.getDisplayMetrics()
            ).toInt()
        }

        fun getNormalTime(millis: Long): String{
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = millis

            val currentTime = java.util.Calendar.getInstance()

            val amPm = getAmPm(calendar)

            return if (calendar.get(java.util.Calendar.DAY_OF_MONTH) == currentTime.get(java.util.Calendar.DAY_OF_MONTH) &&
                calendar.get(java.util.Calendar.MONTH) == currentTime.get(java.util.Calendar.MONTH) &&
                calendar.get(java.util.Calendar.YEAR) == currentTime.get(java.util.Calendar.YEAR)) {
                ("${calendar.get(java.util.Calendar.HOUR)}:${calendar.get(java.util.Calendar.MINUTE)} $amPm")
            } else {
                "${calendar.get(java.util.Calendar.DAY_OF_MONTH)}/${calendar.get(java.util.Calendar.MONTH)}/${calendar.get(java.util.Calendar.YEAR)}"
            }
        }

        fun getAmPm(calendar: java.util.Calendar): String {
            return if (calendar.get(java.util.Calendar.AM_PM) == Calendar.AM) {
                "am"
            } else {
                "pm"
            }
        }

        fun buildDialog(context: Context, title: String, msg: String): AlertDialog.Builder {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(title)
            builder.setMessage(msg)
            return builder
        }

        fun buildSnackbar(context: Context, text: String, view: View, isDarkMode: Boolean): Snackbar {
            val snackbar = Snackbar.make(view, text, Snackbar.LENGTH_LONG)
            snackbar.view.background =
                ContextCompat.getDrawable(context, R.drawable.snackbar_background)
            if (isDarkMode) {
                snackbar.setTextColor(ContextCompat.getColor(context, R.color.black))
                snackbar.setActionTextColor(ContextCompat.getColor(context, R.color.black))
                snackbar.view.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
            } else {
                snackbar.setTextColor(ContextCompat.getColor(context, R.color.white))
                snackbar.setActionTextColor(ContextCompat.getColor(context, R.color.white))
                snackbar.view.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.black))
            }
            return snackbar
        }



    }
}