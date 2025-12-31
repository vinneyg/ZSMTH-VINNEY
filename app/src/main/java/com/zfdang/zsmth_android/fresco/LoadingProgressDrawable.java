package com.zfdang.zsmth_android.fresco;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;
import androidx.annotation.NonNull;
import com.zfdang.zsmth_android.R;

import java.util.HashMap;
import java.util.Map;

/**
 * ========================================================== <br>
 * <b>版权</b>：　　　别志华 版权所有(c) 2015 <br>
 * <b>作者</b>：　　　别志华 biezhihua@163.com<br>
 * <b>创建日期</b>：　15-9-17 <br>
 * <b>描述</b>：　　　加载进度<br>
 * <b>版本</b>：　    V1.0 <br>
 * <b>修订历史</b>：　<br>
 * ========================================================== <br>
 */
public class LoadingProgressDrawable extends Drawable {

    private static final String TAG = "LoadingProgressDrawable";
    private static final int[] Loadings = {
            R.mipmap.load_progress_1, R.mipmap.load_progress_3, R.mipmap.load_progress_4, R.mipmap.load_progress_6, R.mipmap.load_progress_7,
            R.mipmap.load_progress_8, R.mipmap.load_progress_9, R.mipmap.load_progress_10, R.mipmap.load_progress_11, R.mipmap.load_progress_12
    };
    private final Paint mPaint;
    private int mLevel;
    private final Context context;
    // 缓存解码后的 Bitmap
    private final Map<Integer, Bitmap> mBitmapCache = new HashMap<>();
    private Bitmap mCurrentBitmap;
    private int mCurrentIndex = -1;
    // 圆角半径
    private float mCornerRadius = 0f;
    // 圆角路径
    private Path mClipPath;

    public LoadingProgressDrawable(Context context) {
        this.context = context;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        int index = getIndex();

        // 检查缓存的 Bitmap 是否需要更新
        if (mCurrentIndex != index) {
            mCurrentBitmap = getBitmapForIndex(index);
            mCurrentIndex = index;
        }

        if (mCurrentBitmap != null) {
            int left = getBounds().right / 2 - mCurrentBitmap.getWidth() / 2;
            int top = getBounds().bottom / 2 - mCurrentBitmap.getHeight() / 2;

            // 如果设置了圆角，则使用圆角路径绘制
            if (mCornerRadius > 0) {
                if (mClipPath == null) {
                    mClipPath = new Path();
                }
                RectF rectF = new RectF(left, top, left + mCurrentBitmap.getWidth(), top + mCurrentBitmap.getHeight());
                mClipPath.reset();
                mClipPath.addRoundRect(rectF, mCornerRadius, mCornerRadius, Path.Direction.CW);
                canvas.save();
                canvas.clipPath(mClipPath);
                canvas.drawBitmap(mCurrentBitmap, left, top, mPaint);
                canvas.restore();
            } else {
                canvas.drawBitmap(mCurrentBitmap, left, top, mPaint);
            }
        }
    }

    private Bitmap getBitmapForIndex(int index) {
        if (index < 0 || index >= Loadings.length) {
            return null;
        }

        // 检查缓存
        if (mBitmapCache.containsKey(index)) {
            return mBitmapCache.get(index);
        }

        // 解码并缓存
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeResource(context.getResources(), Loadings[index], options);
            if (bitmap != null) {
                mBitmapCache.put(index, bitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to decode bitmap resource: " + Loadings[index], e);
        }

        return bitmap;
    }

    private int getIndex() {
        int index = mLevel / 1000;
        if (index < 0) {
            index = 0;
        } else if (index >= Loadings.length) {
            index = Loadings.length - 1;
        }
        return index;
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    @SuppressLint("WrongConstant")
    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    @Override
    protected boolean onLevelChange(int level) {
        this.mLevel = level;
        this.invalidateSelf();
        return true;
    }

    @Override
    public void onBoundsChange(@NonNull android.graphics.Rect bounds) {
        super.onBoundsChange(bounds);
        // 重置当前缓存索引以重新计算位置
        mCurrentIndex = -1;
    }

    // 设置圆角半径
    public void setCornerRadius(float radius) {
        this.mCornerRadius = radius;
        invalidateSelf();
    }

    // 获取圆角半径
    public float getCornerRadius() {
        return this.mCornerRadius;
    }

    // 清理缓存资源
    public void release() {
        for (Bitmap bitmap : mBitmapCache.values()) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        mBitmapCache.clear();
        if (mCurrentBitmap != null && !mCurrentBitmap.isRecycled()) {
            mCurrentBitmap.recycle();
            mCurrentBitmap = null;
        }
        if (mClipPath != null) {
            mClipPath.reset();
            mClipPath = null;
        }
    }
}
