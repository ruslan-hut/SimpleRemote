package ua.com.programmer.simpleremote.ui.shared

import android.content.Context
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import ua.com.programmer.simpleremote.R
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageLoader @Inject constructor(context: Context) {

    private val requestManager: RequestManager? = Glide.with(context)
    private val fileDir: File? = context.filesDir
    private var loadImages: Boolean = false
    private var baseImageURL: String? = null
    private var authHeaders: LazyHeaders? = null

    fun setToken(token: String) {
        authHeaders = LazyHeaders.Builder()
            .addHeader("Authorization", token)
            .build()
    }

    fun setLoadImages(loadImages: Boolean) {
        this.loadImages = loadImages
    }

    fun setBaseImageURL(baseImageURL: String) {
        this.baseImageURL = baseImageURL
    }

    /**
     * Construct an image URL using image GUID with the base URL combined
     *
     * @param imageGUID image GUID
     * @return image URL
     */
    private fun imageURL(imageGUID: String): String {
        if (!imageGUID.isEmpty()) return baseImageURL + imageGUID
        return ""
    }

    /**
     * Load image by GUID into given ImageView.
     *
     * @param imageGUID image GUID
     * @param view image showing view
     */
    fun load(imageGUID: String, view: ImageView) {
        if (!loadImages) {
            view.visibility = View.GONE
            return
        }

        val url = imageURL(imageGUID)

        if (!url.isEmpty()) {
            view.visibility = View.VISIBLE
            val glideUrl = GlideUrl(url, authHeaders)
            requestManager
                ?.load(glideUrl)
                ?.override(view.width.takeIf { it > 0 } ?: 240, view.height.takeIf { it > 0 } ?: 240)
                ?.centerCrop()
                ?.diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                ?.format(DecodeFormat.PREFER_ARGB_8888)
                ?.dontAnimate()
                ?.placeholder(R.drawable.baseline_downloading_24)
                ?.error(R.drawable.baseline_block_24)?.into(view)
        } else {
            view.visibility = View.INVISIBLE
        }
    }

    fun loadFile(file: String, view: ImageView) {
        if (file.isEmpty()) return
        val imageFile = File(fileDir, file)
        requestManager?.let {
            if (imageFile.exists()) {
                it.load(imageFile)
                    .override(view.width.takeIf { w -> w > 0 } ?: 240, view.height.takeIf { h -> h > 0 } ?: 240)
                    .centerCrop()
                    .into(view)
            }
        }
    }

    /**
     * Stop all current and pending requests on activity onDestroy event.
     */
//    fun stop() {
//        requestManager?.pauseAllRequests()
//    }
}
