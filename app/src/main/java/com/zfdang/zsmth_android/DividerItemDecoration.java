package com.zfdang.zsmth_android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

/**
 * Created by zfdang on 2016-3-16.
 */
public class DividerItemDecoration extends RecyclerView.ItemDecoration {

  public static final int HORIZONTAL_LIST = LinearLayoutManager.HORIZONTAL;
  public static final int VERTICAL_LIST = LinearLayoutManager.VERTICAL;

  // the following two dimension are used for divider with drawable resource
  private static final int DIVIDER_HEIGHT = 4;
  private static final int DIVIDER_WIDTH = 4;

  private int mOrientation;

  public final int[] ATTRS = { android.R.attr.listDivider };
  private Drawable mDivider;
  private int mHeight;
  private int mWidth;

  public DividerItemDecoration(Context context, int orientation, int dividerDrawableRes) {
    if (dividerDrawableRes == 0) {
      dividerDrawableRes = R.drawable.recyclerview_divider;
    }

    mDivider = context.getResources().getDrawable(dividerDrawableRes,null);
    mHeight = DIVIDER_HEIGHT;
    mWidth = DIVIDER_WIDTH;
    setOrientation(orientation);
  }

  public void setOrientation(int orientation) {
    if (orientation != HORIZONTAL_LIST && orientation != VERTICAL_LIST) {
      throw new IllegalArgumentException("invalid orientation");
    }
    mOrientation = orientation;
  }

  @Override public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
    if (mOrientation == VERTICAL_LIST) {
      drawVertical(c, parent);
    } else {
      drawHorizontal(c, parent);
    }
  }

  public void drawVertical(Canvas c, RecyclerView parent) {
    final int left = parent.getPaddingLeft();
    final int right = parent.getWidth() - parent.getPaddingRight();

    final int childCount = parent.getChildCount();
    for (int i = 0; i < childCount; i++) {
      final View child = parent.getChildAt(i);
      final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
      final int top = child.getBottom() + params.bottomMargin;
      final int bottom = top + mHeight;
      mDivider.setBounds(left, top, right, bottom);
      mDivider.draw(c);
    }
  }

  public void drawHorizontal(Canvas c, RecyclerView parent) {
    final int top = parent.getPaddingTop();
    final int bottom = parent.getHeight() - parent.getPaddingBottom();

    final int childCount = parent.getChildCount();
    for (int i = 0; i < childCount; i++) {
      final View child = parent.getChildAt(i);
      final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
      final int left = child.getRight() + params.rightMargin;
      final int right = left + mHeight;
      mDivider.setBounds(left, top, right, bottom);
      mDivider.draw(c);
    }
  }

  @Override public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
    if (mOrientation == VERTICAL_LIST) {
      outRect.set(0, 0, 0, mHeight);
    } else {
      outRect.set(0, 0, mWidth, 0);
    }
  }
}