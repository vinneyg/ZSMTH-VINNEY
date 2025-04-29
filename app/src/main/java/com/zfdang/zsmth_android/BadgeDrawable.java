package com.zfdang.zsmth_android;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

public class BadgeDrawable extends Drawable {

    private final Paint mBadgePaint;
    private final TextPaint mTextPaint;
    private final Rect mTextRect = new Rect();
    private String mBadgeText = "";
    private final int mBadgeSize; // 角标的大小

    public BadgeDrawable(float textSize, int badgeColor, int textColor, int badgeSize) {
        mBadgeSize = badgeSize;

        mBadgePaint = new Paint();
        mBadgePaint.setColor(badgeColor);
        mBadgePaint.setStyle(Paint.Style.FILL);
        mBadgePaint.setAntiAlias(true);

        mTextPaint = new TextPaint();
        mTextPaint.setColor(textColor);
        mTextPaint.setTextSize(textSize);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD); // 设置为粗体
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER); // Center align text

    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        float width = mTextPaint.measureText(mBadgeText, 0, mBadgeText.length());
        float radius = mBadgeSize / 2.0f;

        // Draw the badge background
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), radius, mBadgePaint);

        // Get font metrics to calculate baseline
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        float baseline = bounds.centerY() - (fontMetrics.top + fontMetrics.bottom) / 2.0f;

        // Draw the badge text
        mTextPaint.getTextBounds(mBadgeText, 0, mBadgeText.length(), mTextRect);
        float textX = bounds.centerX();
        canvas.drawText(mBadgeText, textX, baseline, mTextPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        mBadgePaint.setAlpha(alpha);
        mTextPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mBadgePaint.setColorFilter(colorFilter);
        mTextPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public void setBadgeText(String badgeText) {
        mBadgeText = badgeText;
        invalidateSelf();
    }

    @Override
    public int getIntrinsicWidth() {
        return mBadgeSize;
    }

    @Override
    public int getIntrinsicHeight() {
        return mBadgeSize;
    }
}
