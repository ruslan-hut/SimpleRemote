package ua.com.programmer.simpleremote.utility;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.signature.ObjectKey;

import java.io.File;
import java.util.ArrayList;

import ua.com.programmer.simpleremote.R;
import ua.com.programmer.simpleremote.SqliteDB;
import ua.com.programmer.simpleremote.settings.AppSettings;
import ua.com.programmer.simpleremote.specialItems.DataBaseItem;

public class ImageLoader {

    private final RequestManager requestManager;
    private String baseImageURL;
    private LazyHeaders authHeaders;

    private final SqliteDB dataBase;
    private final AppSettings appSettings;

    public ImageLoader(Context context){
        requestManager = Glide.with(context);
        appSettings = AppSettings.getInstance(context);
        dataBase = SqliteDB.getInstance(context);
    }

    /**
     * Initialise environment parameters for load requests.
     */
    private void init(){
        baseImageURL = appSettings.getBaseImageUrl();
        authHeaders = appSettings.getAuthHeaders();
    }

    /**
     * Construct an image URL using image GUID with the base URL combined
     *
     * @param imageGUID image GUID
     * @return image URL
     */
    private String imageURL(String imageGUID){
        if (!imageGUID.isEmpty()) return baseImageURL + imageGUID;
        return "";
    }

    /**
     * Load image by GUID into given ImageView.
     *
     * @param imageGUID image GUID
     * @param view image showing view
     */
    public void load(String imageGUID, ImageView view){
        init();

        String url = imageURL(imageGUID);

        if (!url.isEmpty()) {
            view.setVisibility(View.VISIBLE);
            GlideUrl glideUrl = new GlideUrl(url,authHeaders);
            requestManager.load(glideUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.sharp_more_horiz_black_36)
                    .error(R.drawable.sharp_help_outline_black_36)
                    .into(view);
        }else {
            view.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Stop all current and pending requests on activity onDestroy event.
     */
    public void stop(){
        if (requestManager != null) requestManager.pauseAllRequests();
    }

}
