package com.zfdang.multiple_images_selector.utilities;

import android.net.Uri;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
//import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

/**
 * Created by zfdang on 2016-4-17.
 */
public class DraweeUtils {
    //private static final String TAG = "DraweeUtils";

    // http://www.jianshu.com/p/5364957dcf49
    public static void showThumb(Uri uri, SimpleDraweeView draweeView){
        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
                //.setAutoRotateEnabled(true)
                .setRotationOptions(RotationOptions.autoRotate())
                .setResizeOptions(new ResizeOptions(200, 200))
                .build();

        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(request)
                .setAutoPlayAnimations(true)
                .setOldController(draweeView.getController())
                .setControllerListener(new BaseControllerListener<>())
                .build();
        draweeView.setController(controller);
    }

}
