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
import androidx.annotation.Dimension
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.example.wirelesstransferandroid.R
import java.util.jar.Attributes

class FunctionBtnView: ConstraintLayout {
    private var onClick: () -> Unit = {}

    var fun_icon: Drawable? = null
    var fun_name: String? = ""
    var imgBtn_width: Int = resources.getDimensionPixelSize(R.dimen.default_btn_width)
    var imgBtn_height: Int = resources.getDimensionPixelSize(R.dimen.default_btn_height)

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
        imgBtn_width = typedArray.getDimensionPixelSize(R.styleable.FunctionBtnView_imgBtn_width, resources.getDimensionPixelSize(R.dimen.default_btn_width))
        imgBtn_height = typedArray.getDimensionPixelSize(R.styleable.FunctionBtnView_imgBtn_height, resources.getDimensionPixelSize(R.dimen.default_btn_height))
        typedArray.recycle()
    }

    private fun initView() {
        inflate(context, R.layout.customview_fuctionbtnview, this)

        var function_icon: ImageView = findViewById(R.id.function_icon)
        function_icon.setImageDrawable(fun_icon)

        var function_name: TextView = findViewById(R.id.function_name)
        function_name.text = fun_name
        //function_name.width = imgBtn_width

        var imgBtn: ImageButton = findViewById(R.id.imgBtn)
        imgBtn.contentDescription = fun_name
        //imgBtn.layoutParams = LayoutParams(imgBtn_width, imgBtn_height)
        imgBtn.setOnClickListener {
            onClick.invoke()
        }
    }

    fun setOnClick(block: () -> Unit) {
        onClick = block
    }
}