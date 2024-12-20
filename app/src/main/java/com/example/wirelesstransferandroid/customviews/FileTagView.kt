package com.example.wirelesstransferandroid.customviews

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Context
import android.content.res.TypedArray
import android.net.Uri
import android.util.AttributeSet
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.wirelesstransferandroid.R
import com.example.wirelesstransferandroid.tools.FileInfoPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class FileTagView: ConstraintLayout {
    companion object {
        const val MAX_LENGTH = 20
    }

    private var onDelete: (FileTagView) -> Unit = {}
    fun setOnDelete(block: (FileTagView) -> Unit) {
        onDelete = block
    }

    var uri: Uri? = null
        private set

    var originalFileName = ""
        private set

    var showedFileName = ""
        private set

    var fullFileSize = 0L
        private set

    lateinit var fileNameTV: TextView
        private set

    lateinit var fileSizeTV: TextView
        private set

    lateinit var fileIconIV: ImageView
        private set

    lateinit var deleteBtn: Button
        private set

    constructor(context: Context): super(context) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet): super(context, attrs) {
        var typedArray = context.obtainStyledAttributes(attrs, R.styleable.FileProgressTagView)
        getValues(typedArray)
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int): super(context, attrs, defStyle) {
        var typedArray = context.obtainStyledAttributes(attrs, R.styleable.FileProgressTagView, defStyle, 0)
        getValues(typedArray)
        initView()
    }

    private fun getValues(typedArray: TypedArray) {
        originalFileName = typedArray.getString(R.styleable.FileProgressTagView_file_name) ?: ""
        if (originalFileName.length > MAX_LENGTH)
            showedFileName = originalFileName.substring(0..MAX_LENGTH) + "..."
        else
            showedFileName = originalFileName

        fullFileSize = typedArray.getString(R.styleable.FileProgressTagView_file_size)?.toLong() ?: 0

        typedArray.recycle()
    }

    private fun initView() {
        inflate(context, R.layout.customview_filetagview, this)

        fileNameTV = findViewById<TextView>(R.id.fileNameTV)
        fileNameTV.text = showedFileName

        fileSizeTV = findViewById<TextView>(R.id.fileSizeTV)
        fileSizeTV.text = FileInfoPresenter.getFileSizePresent(fullFileSize)

        fileIconIV = findViewById<ImageView>(R.id.fileIconIV)
        fileIconIV.setImageDrawable(resources.getDrawable(FileInfoPresenter.getFileIconId(originalFileName.split(".").last())))

        deleteBtn = findViewById<Button>(R.id.deleteBtn)
        deleteBtn.setOnClickListener {
            deleteBtn.isEnabled = false
            onDelete.invoke(this)
        }
    }

    fun setFileName(name: String) {
        originalFileName = name
        if (originalFileName.length > FileProgressTagView.Companion.MAX_LENGTH)
            showedFileName = originalFileName.substring(0..FileProgressTagView.Companion.MAX_LENGTH) + "..."
        else
            showedFileName = originalFileName

        fileNameTV.text = showedFileName

        fileIconIV.setImageDrawable(resources.getDrawable(FileInfoPresenter.getFileIconId(originalFileName.split(".").last())))
    }

    fun setFileSize(size: Long) {
        fullFileSize = size
        fileSizeTV.text = FileInfoPresenter.getFileSizePresent(fullFileSize)
    }

    fun setUri(uri: Uri?) {
        this.uri = uri
    }
}