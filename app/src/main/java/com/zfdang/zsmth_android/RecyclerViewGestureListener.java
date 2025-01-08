package com.zfdang.zsmth_android;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * This gesturelistener is for PostListActivity
 * Created by zfdang on 2016-5-5.
 */
public class RecyclerViewGestureListener extends GestureDetector.SimpleOnGestureListener {

  public interface OnItemLongClickListener {
    void onItemLongClicked(int position, View v);
    void onItemLeftClicked(int position, View v);
    void onItemRightClicked(int position, View v);
    void onItemBottomClicked(int position, View v);
    void onItemTopClicked(int position, View v);
  }

  private final OnItemLongClickListener mListener;
  private final RecyclerView recyclerView;
  private final int mScreenHeight;
  private final int mScreenWidth;


  public RecyclerViewGestureListener(OnItemLongClickListener listener, RecyclerView recyclerView) {
    this.mListener = listener;
    this.recyclerView = recyclerView;

    WindowManager wm = (WindowManager) this.recyclerView.getContext().getSystemService(Context.WINDOW_SERVICE);

    DisplayMetrics dm = new DisplayMetrics();
    wm.getDefaultDisplay().getMetrics(dm);
    mScreenWidth = dm.widthPixels;
    mScreenHeight = dm.heightPixels;

  }

  @Override public void onLongPress(MotionEvent e) {
    float x = e.getRawX();
    float y = e.getRawY();
    int[] location = new int[2];
    recyclerView.getLocationOnScreen(location);

    View targetView = recyclerView.findChildViewUnder(x - location[0], y - location[1]);
    if (targetView == null) {
      Log.e("RecyclerViewGes", "targetView is null.");
      return;
    }

    int position = recyclerView.getChildAdapterPosition(targetView);
    // Log.d("Gesture", "onLongPress: " + String.format("position = %d", position));
    if (mListener != null) {
      mListener.onItemLongClicked(position, targetView);
    }

    super.onLongPress(e);
  }

  @Override
  public boolean onSingleTapConfirmed(MotionEvent e) {
    float x = e.getRawX();
    float y = e.getRawY();

    int[] location = new int[2];
    recyclerView.getLocationOnScreen(location);

    View targetView = recyclerView.findChildViewUnder(x - location[0], y - location[1]);

    if (targetView == null) {
      Log.e("RecyclerViewGes", "targetView is null.");
      return false;
    }
    int position = recyclerView.getChildAdapterPosition(targetView);

    //        Log.d("Gesture", "onLongPress: " + String.format("position = %d", position));
    if (mListener != null) {

      if (x < 0.15 * mScreenWidth) {
        mListener.onItemLeftClicked(position,targetView);
      } else if (x > 0.85 * mScreenWidth) {
        mListener.onItemRightClicked(position,targetView);
      } else if (y < 0.3 * mScreenHeight){
        mListener.onItemTopClicked(position,targetView);
      } else if (y > 0.7 * mScreenHeight){
        mListener.onItemBottomClicked(position,targetView);
      }

    }
      return super.onSingleTapConfirmed(e);
  }
}
