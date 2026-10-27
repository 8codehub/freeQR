package com.qr.domain

import android.content.Context
import androidx.annotation.StringRes

/** Defers resource resolution until display, so state and effects survive locale changes. */
data class LocalizedText(@StringRes val id: Int, val arguments: List<Any> = emptyList())
fun LocalizedText.resolve(context: Context): String = context.getString(id, *arguments.map {
    if (it is LocalizedText) it.resolve(context) else it
}.toTypedArray())
