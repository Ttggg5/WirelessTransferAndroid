package com.example.wirelesstransferandroid.customviews

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.example.wirelesstransferandroid.R
import java.util.jar.Attributes

class FunctionBtnView: ConstraintLayout {
    private var onClick: () -> Unit = {}

    var fun_icon: Drawable? = null
    var fun_name: String? = ""

    constructor(context: Context): super(context) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet): super(context, attrs) {
        var typedArray = context.obtainStyledAttributes(attrs, R.styleable.FunctionBtnView)
        getValues(typedArray)
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int): super(context, attrs, defStyle) {
        var typedArray = context.obtainStyledAttributes(attrs, R.styleable.FunctionBtnView, defStyle, 0)
        getValues(typedArray)
        initView()
    }

    private fun getValues(typedArray: TypedArray) {
        fun_icon = typedArray.getDrawable(R.styleable.FunctionBtnView_function_icon)
        fun_name = typedArray.getString(R.styleable.FunctionBtnView_function_name) ?: ""
        typedArray.recycle()
    }

    private fun initView() {
        inflate(context, R.layout.customview_fuctionbtnview, this)

        var function_icon: ImageView = findViewById<ImageView>(R.id.function_icon)
        function_icon.setImageDrawable(fun_icon)

        var function_name: TextView = findViewById<TextView>(R.id.function_name)
        function_name.setText(fun_name)

        var imgBtn: ImageButton = findViewById<ImageButton>(R.id.imgBtn)
        imgBtn.setOnClickListener {
            onClick.invoke()
        }
    }

    fun setOnClick(block: () -> Unit) {
        onClick = block
    }
}