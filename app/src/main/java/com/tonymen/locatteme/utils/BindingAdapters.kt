package com.tonymen.locatteme.utils

import android.view.View
import android.widget.ProgressBar
import androidx.databinding.BindingAdapter

@BindingAdapter("app:visible")
fun setProgressBarVisibility(view: ProgressBar, visibility: Boolean) {
    view.visibility = if (visibility) View.VISIBLE else View.GONE
}
