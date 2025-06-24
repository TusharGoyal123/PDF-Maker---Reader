package com.TusharGoyal.imgtopdf

import android.text.format.DateFormat
import java.util.Calendar


import java.util.Locale


object constants {

    fun formatTimeStamp(timeStamp : Long) : String{
        val calendar = Calendar.getInstance(Locale.ENGLISH)
        calendar.timeInMillis = timeStamp //giving the timestamp to calendar

        return DateFormat.format("dd/MM/yyyy",calendar).toString()



    }
}