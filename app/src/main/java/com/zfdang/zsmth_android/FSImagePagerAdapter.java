package com.zfdang.zsmth_android;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.github.chrisbanes.photoview.OnOutsidePhotoTapListener;
import com.github.chrisbanes.photoview.OnPhotoTapListener;
import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.fresco.MyPhotoView;
import java.util.List;


/**
 * Created by zfdang on 2016-3-31.
 */
public class FSImagePagerAdapter extends PagerAdapter {

    private final List<String> mURLs;
    private final Activity mListener;
    //private Map<Integer, PhotoViewAttacher> mAttachers;

    // http://stackoverflow.com/questions/6807262/get-focused-view-from-viewpager
    public View mCurrentView;

    public FSImagePagerAdapter(List<String> URLs, Activity listener) {
        mURLs = URLs;
        mListener = listener;
    }

    @Override public int getCount() {
        return mURLs.size();
    }

    @NonNull
    @Override public Object instantiateItem(@NonNull ViewGroup container, int position) {
        final LayoutInflater inflater = (LayoutInflater) SMTHApplication.getAppContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // Add the text layout to the parent layout
        MyPhotoView image = (MyPhotoView) inflater.inflate(R.layout.image_viewer_pager, container, false);

        if (image == null) {
            Log.e("FSImagePageAdapter", "image is null, please check initialization.");
            return new MyPhotoView(SMTHApplication.getAppContext());
        }


        image.setTag(R.id.fsview_image_index, position);
        image.setMaximumScale(12.0f);
        // Load image with error handling
        String imageUrl = mURLs.get(position);
        try {
            // use only this method to set image
            image.setImageUri(imageUrl);
        } catch (Exception e) {
            Log.e("FSImagePageAdapter", "Failed to load image: " + imageUrl, e);
        }

        image.setOnPhotoTapListener((view, x, y) -> {
            if (mListener != null && mListener instanceof OnPhotoTapListener) {
                ((OnPhotoTapListener) mListener).onPhotoTap(view, x, y);
            }
        });

        image.setOnOutsidePhotoTapListener(imageView -> {
            if (mListener != null && mListener instanceof OnOutsidePhotoTapListener) {
                ((OnOutsidePhotoTapListener) mListener).onOutsidePhotoTap(imageView);
            }
        });

        container.addView(image, 0);
        return image;
    }

    @Override public void destroyItem(ViewGroup container, int position, @NonNull Object object) {

        ImageView iv = (ImageView) object;
        container.removeView(iv);
        //object = null;
    }

    @Override public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        mCurrentView = (View) object;
        super.setPrimaryItem(container, position, object);
    }

    @Override public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return (object == view);
    }

    public View getViewAtPosition(int position) {
        // This would require maintaining a map of position to view
        // For now, rely on mCurrentView for the primary item
        if (mCurrentView != null) {
            Object tag = mCurrentView.getTag(R.id.fsview_image_index);
            if (tag instanceof Integer && (Integer) tag == position) {
                return mCurrentView;
            }
        }
        return null;
    }

}
