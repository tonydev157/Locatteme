package com.tonymen.locatteme.model

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tonymen.locatteme.R
import java.io.InputStream

data class EcuadorLocations(
    val provinces: List<Province>
)

data class Province(
    val name: String,
    val cities: List<String>
)

fun loadEcuadorLocations(context: Context): EcuadorLocations? {
    return try {
        val inputStream: InputStream = context.resources.openRawResource(R.raw.ecuador_locations)
        val json = inputStream.bufferedReader().use { it.readText() }
        val gson = Gson()
        val type = object : TypeToken<EcuadorLocations>() {}.type
        gson.fromJson(json, type)
    } catch (ex: Exception) {
        ex.printStackTrace()
        null
    }
}
