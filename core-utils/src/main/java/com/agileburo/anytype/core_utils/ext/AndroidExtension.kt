package com.agileburo.anytype.core_utils.ext

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

fun Context.dimen(res: Int): Float {
    return resources
        .getDimension(res)
}

fun Uri.parsePath(context: Context): String {

    val result: String?

    val cursor = context.contentResolver.query(
        this,
        null,
        null,
        null, null
    )

    if (cursor == null) {
        result = this.path
    } else {
        cursor.moveToFirst()
        val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
        result = cursor.getString(idx)
        cursor.close()
    }

    return result ?: throw IllegalStateException("Cold not get real path")
}

fun Throwable.timber() = Timber.e("Get error : ${this.message}")

const val DATE_FORMAT_MMMdYYYY = "MMM d, yyyy"

fun Long.formatToDateString(pattern: String, locale: Locale): String {
    val formatter = SimpleDateFormat(pattern, locale)
    return formatter.format(Date(this))
}