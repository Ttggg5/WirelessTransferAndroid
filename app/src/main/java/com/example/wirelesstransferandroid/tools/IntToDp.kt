package com.example.wirelesstransferandroid.tools

import android.content.res.Resources

object IntToDp {
    val Int.dp: Int
        get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()

    val Float.dp: Int
        get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()
}