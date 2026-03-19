package ua.com.programmer.simpleremote.ui.document

import android.app.Dialog
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import ua.com.programmer.simpleremote.R

fun showImageDialog(imageView: ImageView) {
    val context = imageView.context
    val dialog = Dialog(context, R.style.FullscreenImageDialog)
    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_fullscreen_image, null)
    val fullscreenImage = dialogView.findViewById<ImageView>(R.id.fullscreen_image)
    val container = dialogView.findViewById<FrameLayout>(R.id.fullscreen_container)

    val srcDrawable = imageView.drawable
    if (srcDrawable is BitmapDrawable) {
        fullscreenImage.setImageBitmap(srcDrawable.bitmap)
    } else {
        fullscreenImage.setImageDrawable(srcDrawable)
    }

    val metrics = context.resources.displayMetrics
    val horizontalPadding = (metrics.widthPixels * 0.05).toInt()
    val verticalPadding = (metrics.heightPixels * 0.05).toInt()
    container.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

    dialog.setContentView(dialogView)
    dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

    dialog.setOnDismissListener {
        fullscreenImage.setImageDrawable(null)
    }

    dialog.show()

    container.setOnClickListener { dialog.dismiss() }
    fullscreenImage.setOnClickListener { dialog.dismiss() }
}
