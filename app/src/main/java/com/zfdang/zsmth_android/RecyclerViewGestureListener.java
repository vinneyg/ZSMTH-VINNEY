package com.zfdang.zsmth_android;

import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;
import android.view.GestureDetector;
//import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
//import android.widget.Toast;

//import com.zfdang.SMTHApplication;
//import com.zfdang.zsmth_android.helpers.RecyclerViewUtil;

/**
 * This gesturelistener is for PostListActivity
 * Created by zfdang on 2016-5-5.
 */
public class RecyclerViewGestureListener extends GestureDetector.SimpleOnGestureListener {

  public interface OnItemLongClickListener {
    boolean onItemLongClicked(int position, View v);
    boolean onItemLeftClicked(int position, View v);
    boolean onItemRightClicked(int position, View v);
    boolean onItemBottomClicked(int position, View v);
    boolean onItemTopClicked(int position, View v);
  }

  private OnItemLongClickListener mListener;
  private RecyclerView recyclerView;
  private int mScreenHeight;
  private int mScreenWidth;


  public RecyclerViewGestureListener() {
  }

  public RecyclerViewGestureListener(OnItemLongClickListener listener, RecyclerView recyclerView) {
    this.mListener = listener;
    this.recyclerView = recyclerView;

    WindowManager wm = (WindowManager) this.recyclerView.getContext().getSystemService(Context.WINDOW_SERVICE);
    mScreenHeight = wm.getDefaultDisplay().getHeight();
    mScreenWidth = wm.getDefaultDisplay().getWidth();
  }
/*
  @Override public boolean onSingleTapUp(MotionEvent e) {
    int touchY = (int) e.getRawY();
    //        Log.d("Gesture", "onSingleTapUp: " + String.format("%d / %d = %f", touchY, mScreenHeight, touchY * 1.0 / mScreenHeight));
    if (touchY < mScreenHeight * 0.40) {
      RecyclerViewUtil.ScrollRecyclerViewByKey(this.recyclerView, KeyEvent.KEYCODE_VOLUME_UP);
      return true;
    } else if (touchY > mScreenHeight * 0.60) {
      RecyclerViewUtil.ScrollRecyclerViewByKey(this.recyclerView, KeyEvent.KEYCODE_VOLUME_DOWN);
      return true;
    }

    return super.onSingleTapUp(e);
  }

  */
  @Override public void onLongPress(MotionEvent e) {
    float x = e.getRawX();
    float y = e.getRawY();
    int[] location = new int[2];
    recyclerView.getLocationOnScreen(location);

    View targetView = recyclerView.findChildViewUnder(x - location[0], y - location[1]);
    int position = recyclerView.getChildAdapterPosition(targetView);

    //        Log.d("Gesture", "onLongPress: " + String.format("position = %d", position));
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
