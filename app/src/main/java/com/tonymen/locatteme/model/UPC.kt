package com.tonymen.locatteme.model
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tonymen.locatteme.R
import java.io.InputStreamReader

data class UPC(val name: String, val latitude: Double, val longitude: Double, val address: String)

class JSONHelper {
    companion object {
        fun loadUPCs(context: Context): Map<String, List<UPC>> {
            val inputStream = context.resources.openRawResource(R.raw.upcs)
            val reader = InputStreamReader(inputStream)
            val upcType = object : TypeToken<Map<String, List<UPC>>>() {}.type
            return Gson().fromJson(reader, upcType)
        }
    }
}
