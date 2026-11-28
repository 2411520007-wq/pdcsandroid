package com.a2411520007.mahasiswa

import android.app.Activity
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.a2411520007.mahasiswa.databinding.PopupNotifikasiBinding

object UiPopup {

    fun show(
        activity: Activity,
        message: String,
        isError: Boolean = false,
        durationMs: Long = 2400L
    ) {
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val binding = PopupNotifikasiBinding.inflate(LayoutInflater.from(activity), rootView, false)

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = 36
        }

        val accentColor = if (isError) {
            Color.parseColor("#B3261E")
        } else {
            Color.parseColor("#1B6B3A")
        }

        binding.viewIndicator.setBackgroundColor(accentColor)
        binding.cardPopup.strokeColor = accentColor
        binding.tvPopupMessage.text = message

        binding.root.alpha = 0f
        binding.root.translationY = -24f
        rootView.addView(binding.root, lp)

        binding.root.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(200L)
            .start()

        Handler(Looper.getMainLooper()).postDelayed({
            if (binding.root.parent != null) {
                binding.root.animate()
                    .alpha(0f)
                    .translationY(-20f)
                    .setDuration(180L)
                    .withEndAction {
                        rootView.removeView(binding.root)
                    }
                    .start()
            }
        }, durationMs)
    }
}