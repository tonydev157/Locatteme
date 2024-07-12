package com.tonymen.locatteme.utils

import android.content.res.Resources

fun Int.dpToPx(): Int {
    val density = Resources.getSystem().displayMetrics.density
    return (this * density).toInt()
}
