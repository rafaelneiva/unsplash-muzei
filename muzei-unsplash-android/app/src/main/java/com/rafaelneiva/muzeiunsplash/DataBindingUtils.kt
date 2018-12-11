package com.rafaelneiva.muzeiunsplash

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.squareup.picasso.Picasso

/**
 * Created by rafaelneiva on 02/10/18.
 */
object DataBindingUtils {

//    @JvmStatic
//    @BindingAdapter("msl_state")
//    fun setState(multiStateLayout: MultiStateLayout, state: MultiStateLayout.State) {
//        multiStateLayout.setState(state)
//    }

    @JvmStatic
    @BindingAdapter("img_url")
    fun setPicassoImage(imageView: ImageView, imgUrl: String) {
        Picasso.get()
            .load(imgUrl)
            .into(imageView)
    }
}
