package com.example.commuterx_java

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat

class CustomSearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.searchViewStyle
) : SearchView(context, attrs, defStyleAttr) {

    init {
        setBackgroundResource(R.drawable.search_background_with_border)

        findViewById<SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)?.apply {
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
            background = null // Remove the EditText's background
        }

        // Add padding to the SearchView
        val horizontalPadding = (16 * context.resources.displayMetrics.density).toInt()
        val verticalPadding = (8 * context.resources.displayMetrics.density).toInt()
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

    }
}