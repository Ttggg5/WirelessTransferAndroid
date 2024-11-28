package com.example.wirelesstransferandroid.tools

import com.example.wirelesstransferandroid.R
import android.graphics.drawable.Drawable

object FileInfoPresenter {
    val imageExtensions = arrayOf("jpg", "png", "bmp", "gif", "jpeg", "svg" )
    val videoExtensions = arrayOf( "mp4", "mov", "mkv", "avi", "mpeg", "m4v", "svi" )
    val musicExtensions = arrayOf( "mp3", "flv", "m4a", "dvf", "m4p", "mmf", "movpkg", "wav" )

    fun getFileIconId(fileExtension: String): Int {
        if (imageExtensions.contains(fileExtension))
            return R.drawable.image_icon
        else if (videoExtensions.contains(fileExtension))
            return R.drawable.video_icon
        else if (musicExtensions.contains(fileExtension))
            return R.drawable.music_icon
        else
            return R.drawable.file_icon
    }

    fun getFileSizePresent(fileSize: Long): String {
        if (fileSize > 1099511627776)
            return String.format("%.2f TB", fileSize / 1099511627776.0)
        else if (fileSize > 1073741824)
            return String.format("%.2f GB", fileSize / 1073741824.0)
        else if (fileSize > 1048576)
            return String.format("%.2f MB", fileSize / 1048576.0)
        else if (fileSize > 1024)
            return String.format("%.2f KB", fileSize / 1024.0)
        else
            return fileSize.toString() + "Bytes"
    }
}