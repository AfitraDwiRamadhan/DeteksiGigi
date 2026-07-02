package com.cekgigi.app.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        return dateFormat.format(Date())
    }

    fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
        return dateFormat.format(Date())
    }
}
