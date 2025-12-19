package com.zfdang.zsmth_android.fresco;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import com.facebook.common.references.CloseableReference;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.BasePostprocessor;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.request.Postprocessor;
import com.zfdang.SMTHApplication;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

/*
 * Created by zfdang on 2016-4-8.
 *
 * Modified by Vinney on 2025-12-19
 */

public class WrapContentDraweeView extends SimpleDraweeView {
    private static final String TAG = "DraweeView";

    private int WindowWidth;
    private Rect src;
    private Rect dst;
    private Paint paint;
    private ArrayList<Bitmap> bmps;

    public WrapContentDraweeView(Context context) {
        super(context);
        initDraweeView();
    }

    public WrapContentDraweeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initDraweeView();
    }

    public WrapContentDraweeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initDraweeView();
    }

    public void initDraweeView() {
        getHierarchy().setProgressBarImage(new LoadingProgressDrawable(SMTHApplication.getAppContext()));
        WindowWidth = getResources().getDisplayMetrics().widthPixels;
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        src = new Rect();
        dst = new Rect();
    }

    int getTimes(int actualNumber, int allowedMaxNumber) {
        if (actualNumber < allowedMaxNumber) return 0;
        int result = actualNumber / allowedMaxNumber;
        if (result * allowedMaxNumber < actualNumber) {
            result += 1;
        }
        return result;
    }

    @Override
    public void setImageURI(Uri uri, Object callerContext) {
        if (uri == null) {
            Log.w(TAG, "setImageURI called with null URI");
            return;
        }

        // ✅ Capture the real URL string for this request
        final String currentUrl = uri.toString();
        Log.d(TAG, "Start loading image: " + currentUrl);

        Postprocessor postProcessor = new BasePostprocessor() {
            @Override
            public String getName() {
                return "SplitLongImagePostProcessor";
            }

            @Override
            public CloseableReference<Bitmap> process(Bitmap sourceBitmap, PlatformBitmapFactory bitmapFactory) {
                CloseableReference<Bitmap> bitmapRef = null;
                try {
                    double ratio = 1.0;
                    if (sourceBitmap.getWidth() >= WindowWidth * 1.5) {
                        ratio = (double) WindowWidth / sourceBitmap.getWidth();
                    }
                    bitmapRef = bitmapFactory.createBitmap(
                            (int) (sourceBitmap.getWidth() * ratio),
                            (int) (sourceBitmap.getHeight() * ratio)
                    );

                    Bitmap destBitmap = bitmapRef.get();
                    Canvas canvas = new Canvas(destBitmap);
                    Rect destRect = new Rect(0, 0, destBitmap.getWidth(), destBitmap.getHeight());
                    canvas.drawBitmap(sourceBitmap, null, destRect, paint);

                    try {
                        int imageTotalHeight = destBitmap.getHeight();
                        double imageAspectRatio = destBitmap.getWidth() / (double) WindowWidth;
                        int imageMaxAllowedHeight = (imageAspectRatio < 1)
                                ? (int) (ImageUtils.getMaxHeight() * imageAspectRatio) - 5
                                : ImageUtils.getMaxHeight();

                        int imageCount = getTimes(imageTotalHeight, imageMaxAllowedHeight);
                        if (imageCount > 1) {
                            bmps = new ArrayList<>();
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            destBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                            InputStream isBm = new ByteArrayInputStream(baos.toByteArray());
                            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(isBm, true);
                            for (int i = 0; i < imageCount; i++) {
                                Rect bsrc = new Rect();
                                bsrc.left = 0;
                                bsrc.top = i * imageMaxAllowedHeight;
                                bsrc.right = destBitmap.getWidth();
                                bsrc.bottom = Math.min(bsrc.top + imageMaxAllowedHeight, imageTotalHeight);
                                Bitmap bmp = null;
                                if (decoder != null) {
                                    bmp = decoder.decodeRegion(bsrc, null);
                                }
                                bmps.add(bmp);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error splitting image", e);
                    }
                    return CloseableReference.cloneOrNull(bitmapRef);
                } finally {
                    CloseableReference.closeSafely(bitmapRef);
                }
            }
        };

        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
                .setRotationOptions(RotationOptions.autoRotate())
                .setResizeOptions(new ResizeOptions(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE))
                .setPostprocessor(postProcessor)
                .build();

        // ✅ Create a new listener per request, capturing currentUrl
        ControllerListener<ImageInfo> listener = new BaseControllerListener<ImageInfo>() {
            @Override
            public void onIntermediateImageSet(String id, @Nullable ImageInfo imageInfo) {
                updateViewSize(imageInfo);
            }

            @Override
            public void onFinalImageSet(String id, @Nullable ImageInfo imageInfo, @Nullable Animatable animatable) {
                if (imageInfo != null) {
                    updateViewSize(imageInfo);
                }
            }

            @Override
            public void onFailure(String id, Throwable throwable) {
                String msg = (throwable.getMessage() != null) ? throwable.getMessage().toLowerCase() : "";
                if (msg.contains("unknown image format") || msg.contains("decode")) {
                    Log.e(TAG, "【图片格式错误】URL: " + currentUrl + " | id=" + id);
                } else {
                    Log.e(TAG, "【网络或下载失败】URL: " + currentUrl + " | id=" + id);
                }
            }
        };

        DraweeController controller = ((PipelineDraweeControllerBuilder) getControllerBuilder())
                .setImageRequest(request)
                .setControllerListener(listener)
                .setCallerContext(callerContext)
                .setAutoPlayAnimations(true)
                .setOldController(getController())
                .build();
        setController(controller);
    }

    void updateViewSize(@Nullable ImageInfo imageInfo) {
        if (imageInfo == null) return;
        getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        setAspectRatio((float) imageInfo.getWidth() / imageInfo.getHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bmps == null || bmps.size() <= 1) {
            super.onDraw(canvas);
        } else {
            int accumulatedHeight = 0;
            for (Bitmap bmp : bmps) {
                src.set(0, 0, bmp.getWidth(), bmp.getHeight());
                dst.set(0, accumulatedHeight,
                        getWidth(),
                        accumulatedHeight + (int) ((float) src.height() / src.width() * getWidth()));
                canvas.drawBitmap(bmp, src, dst, paint);
                accumulatedHeight = dst.bottom;
            }
        }
    }

    public void setImageFromStringURL(final String url) {
        if (url == null || url.isEmpty()) {
            Log.w(TAG, "setImageFromStringURL: url is null or empty");
            return;
        }
        setImageURI(Uri.parse(url));
    }

}