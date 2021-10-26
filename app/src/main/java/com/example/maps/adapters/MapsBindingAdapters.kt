package com.example.maps.adapters

import android.R
import android.util.Log
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

@BindingAdapter("imageFromPath")
fun ImageView.imageFromPath(url: String){
    Log.e("BindingAdapters", ""+url)
    val options: RequestOptions = RequestOptions()
        .centerCrop()
        .placeholder(R.drawable.stat_notify_error)
        .error(R.drawable.stat_notify_error)

    Glide.with(this.context).load(url).apply(options).into(this)
}