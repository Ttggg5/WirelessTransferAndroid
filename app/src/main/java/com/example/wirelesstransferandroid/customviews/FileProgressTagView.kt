package com.example.wirelesstransferandroid.customviews

import android.content.Context
import android.content.res.TypedArray
import android.net.Uri
import android.os.Environment
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.wirelesstransferandroid.R
import com.example.wirelesstransferandroid.tools.FileInfoPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.droidsonroids.gif.GifImageView
import java.io.File

enum class FileShareTagState
{
    Waiting,
    Processing,
    Complete,
}

class FileProgressTagView: ConstraintLayout {
    companion object {
        const val MAX_LENGTH = 24
    }

    private var onCompleted: () -> Unit = {}
    fun setOnCompleted(block: () -> Unit) {
        onCompleted = block
    }

    var state = FileShareTagState.Waiting
    var originalFileName = ""
        private set

    var showedFileName = ""
        private set

    var fullFileSize = 0L
        private set

    var curFileSize = 0L
        private set

    var fileSave =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
            + "/" + resources.getString(R.string.save_directory_name))
        private set

    var fileUri: Uri? = null
        private set

    lateinit var stateAnimation: GifImageView
        private set

    lateinit var progressBar: ProgressBar
        private set

    lateinit var fileNameTV: TextView
        private set

    lateinit var fileSizeTV: TextView
        private set

    lateinit var fileIconIV: ImageView
        private set

    constructor(context: Context): super(context) {
        fileSave.mkdir()
        initView()
    }

    constructor(context: Context, attrs: AttributeSet): super(context, attrs) {
        fileSave.mkdir()
        var typedArray = context.obtainStyledAttributes(attrs, R.styleable.FileProgressTagView)
        getValues(typedArray)
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int): super(context, attrs, defStyle) {
        fileSave.mkdir()
        var typedArray = context.obtainStyledAttributes(attrs, R.styleable.FileProgressTagView, defStyle, 0)
        getValues(typedArray)
        initView()
    }

    private fun getValues(typedArray: TypedArray) {
        fileSave = File(fileSave.absolutePath + "/" + typedArray.getString(R.styleable.FileProgressTagView_file_name))
        fileSave = GetNonDuplicateFilePath(fileSave)

        originalFileName = typedArray.getString(R.styleable.FileProgressTagView_file_name) ?: ""
        if (originalFileName.length > MAX_LENGTH)
            showedFileName = originalFileName.substring(0..MAX_LENGTH) + "..."
        else
            showedFileName = originalFileName

        fullFileSize = typedArray.getString(R.styleable.FileProgressTagView_file_size)?.toLong() ?: 0

        typedArray.recycle()
    }

    private fun initView() {
        inflate(context, R.layout.customview_fileprogresstagview, this)

        fileNameTV = findViewById<TextView>(R.id.fileNameTV)
        fileNameTV.text = showedFileName

        fileSizeTV = findViewById<TextView>(R.id.fileSizeTV)
        fileSizeTV.text = FileInfoPresenter.getFileSizePresent(fullFileSize)

        fileIconIV = findViewById<ImageView>(R.id.fileIconIV)
        fileIconIV.setImageDrawable(resources.getDrawable(FileInfoPresenter.getFileIconId(fileSave.extension)))

        stateAnimation = findViewById<GifImageView>(R.id.stateAnimation)
        progressBar = findViewById<ProgressBar>(R.id.progressBar)
    }

    private fun GetNonDuplicateFilePath(file: File): File {
        var count = 0
        var result = File(file.absolutePath)

        while (result.exists()) {
            result =
                File(file.absolutePath.substring(0..file.absolutePath.length - file.extension.length - 2)
                        + "(" + count + ")." + file.extension)
            count++
        }

        return result
    }

    fun setFileName(name: String) {
        fileSave = File(fileSave.absolutePath + "/" + name)
        fileSave = GetNonDuplicateFilePath(fileSave)

        originalFileName = name
        if (originalFileName.length > MAX_LENGTH)
            showedFileName = originalFileName.substring(0..MAX_LENGTH) + "..."
        else
            showedFileName = originalFileName

        fileNameTV.text = showedFileName

        fileIconIV.setImageDrawable(resources.getDrawable(FileInfoPresenter.getFileIconId(fileSave.extension)))
    }

    fun setFileSize(size: Long) {
        fullFileSize = size
        fileSizeTV.text = FileInfoPresenter.getFileSizePresent(fullFileSize)
    }

    fun setFileUri(uri: Uri) {
        fileUri = uri
    }

    fun updateProgress(size: Long) {
        if (state == FileShareTagState.Waiting) {
            state = FileShareTagState.Processing
            stateAnimation.setImageDrawable(pl.droidsonroids.gif.GifDrawable(resources, R.drawable.loading_animation))
        }

        curFileSize += size
        progressBar.setProgress(((curFileSize.toFloat() / fullFileSize.toFloat()) * 100.0).toInt(), true)

        if (curFileSize == fullFileSize) {
            state = FileShareTagState.Complete
            stateAnimation.setImageDrawable(resources.getDrawable(R.drawable.complete_icon))
            onCompleted.invoke()
        }
    }

    fun writeDataToFile(data: ByteArray) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                fileSave.appendBytes(data)
            }

            updateProgress(data.size.toLong())
        }
    }

    fun deleteWroteFile() {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                try {
                    fileSave.delete()
                } catch (ex: Exception) {

                }
            }
        }
    }
}