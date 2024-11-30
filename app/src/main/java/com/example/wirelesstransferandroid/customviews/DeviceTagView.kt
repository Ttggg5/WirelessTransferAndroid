package com.example.wirelesstransferandroid.customviews

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.wirelesstransferandroid.R
import com.example.wirelesstransferandroid.customviews.FileTagView.Companion.MAX_LENGTH
import com.example.wirelesstransferandroid.tools.FileInfoPresenter
import java.sql.Time
import java.time.LocalDateTime
import java.util.Date

class DeviceTagView: ConstraintLayout {
    private var onClick: (DeviceTagView) -> Unit = {}
    fun setOnClick(block: (DeviceTagView) -> Unit) {
        onClick = block
    }

    var name = ""
        private set

    var ip = ""
        private set

    var foundTime = Date().time

    private lateinit var deviceNameTV: TextView
    private lateinit var deviceIpTV: TextView
    private lateinit var deviceTagLayout: ConstraintLayout

    constructor(context: Context): super(context) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet): super(context, attrs) {
        var typedArray = context.obtainStyledAttributes(attrs, R.styleable.DeviceTagView)
        getValues(typedArray)
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int): super(context, attrs, defStyle) {
        var typedArray = context.obtainStyledAttributes(attrs, R.styleable.DeviceTagView, defStyle, 0)
        getValues(typedArray)
        initView()
    }

    private fun getValues(typedArray: TypedArray) {
        name = typedArray.getString(R.styleable.DeviceTagView_device_name) ?: ""
        ip = typedArray.getString(R.styleable.DeviceTagView_device_ip)?: ""

        typedArray.recycle()
    }

    private fun initView() {
        inflate(context, R.layout.customview_devicetagview, this)

        deviceNameTV = findViewById<TextView>(R.id.deviceNameTV)
        deviceNameTV.text = name

        deviceIpTV = findViewById<TextView>(R.id.deviceIpTV)
        deviceIpTV.text = ip

        deviceTagLayout = findViewById<ConstraintLayout>(R.id.deviceTagLayout)
        deviceTagLayout.setOnClickListener {
            onClick.invoke(this)
        }
    }

    fun setDeviceName(deviceName: String) {
        name = deviceName
        deviceNameTV.text = deviceName
    }

    fun setDeviceIp(deviceIp: String) {
        ip = deviceIp
        deviceIpTV.text = deviceIp
    }
}