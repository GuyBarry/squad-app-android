package com.example.squadapp.utils

import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.TextView

/**
 * Utility for creating clickable spannable text links.
 */
object SpannableUtils {

    /**
     * Sets [fullText] on [textView] and makes [clickableSubstring] a clickable,
     * underlined link that invokes [onClick] when tapped.
     */
    fun setClickableLink(
        textView: TextView,
        fullText: String,
        clickableSubstring: String,
        onClick: () -> Unit
    ) {
        val spannableString = SpannableString(fullText)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) = onClick()
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }

        val startIndex = fullText.indexOf(clickableSubstring)
        val endIndex = startIndex + clickableSubstring.length
        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(UnderlineSpan(), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.text = spannableString
        textView.movementMethod = LinkMovementMethod.getInstance()
    }
}

