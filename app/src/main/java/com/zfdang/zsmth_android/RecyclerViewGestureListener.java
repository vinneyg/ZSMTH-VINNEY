package com.zfdang.zsmth_android;

import android.content.Context;

import androidx.annotation.NonNull;
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
  private static final int SWIPE_THRESHOLD = 100;
  private static final int SWIPE_VELOCITY_THRESHOLD = 100;
  private final PostListActivity activity;
  private boolean isFinishing = false;


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


  @Override
  public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
    try {
      if (e1 != null &&!isFinishing ) {
        float diffY = e2.getY() - e1.getY();
        float diffX = e2.getX() - e1.getX();

        if (Math.abs(diffX) > (Math.abs(diffY) + (float) SWIPE_THRESHOLD /2) &&
                Math.abs(diffX) > SWIPE_THRESHOLD &&
                Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD ) {
          isFinishing = true;
          activity.finish();
          //activity.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
          return true;
        }
      }
    } catch (Exception e) {
      Log.e("RecyclerViewGes", "onFling: " + e);
    }
    return super.onFling(e1, e2, velocityX, velocityY);
  }

  public RecyclerViewGestureListener(OnItemLongClickListener listener, RecyclerView recyclerView, PostListActivity activity) {
    this.mListener = listener;
    this.recyclerView = recyclerView;
    this.activity = activity;

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

      if (y < 0.3 * mScreenHeight){
        mListener.onItemTopClicked(position,targetView);
      } else if (y > 0.7 * mScreenHeight){
        mListener.onItemBottomClicked(position,targetView);
      } else {
        if (x < 0.15 * mScreenWidth) {
          mListener.onItemLeftClicked(position,targetView);
        } else if (x > 0.85 * mScreenWidth) {
          mListener.onItemRightClicked(position,targetView);
        }
      }

    }
    return super.onSingleTapConfirmed(e);
  }

}
