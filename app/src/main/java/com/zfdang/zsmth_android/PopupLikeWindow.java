package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import com.github.gzuliyujiang.wheelview.widget.WheelView;
import java.util.Arrays;

/**
 * Created by zfdang on 2016-4-26.
 */
public class PopupLikeWindow extends PopupWindow {
  private static final String TAG = "PopupLikeWindow";

  Activity mContext;
  private OnLikeInterface mListener;
  private WheelView wheelView;
  private EditText etMessage;

  // http://stackoverflow.com/questions/23464232/how-would-you-create-a-popover-view-in-android-like-facebook-comments
  @SuppressLint("ResourceType")
  public void initPopupWindow(final Activity context) {
    mContext = context;
    if (context instanceof OnLikeInterface) {
      mListener = (OnLikeInterface) context;
    } else {
      Log.d(TAG, "initPopupWindow: " + "context does not implement SearchInterface");
    }

    LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    @SuppressLint("InflateParams") View contentView = layoutInflater.inflate(R.layout.popup_like_layout, null, false);

    String[] scores = { "-5", "-4", "-3", "-2", "-1", "0", "+1", "+2", "+3", "+4", "+5" };
    wheelView = contentView.findViewById(R.id.like_score);

    wheelView.setData(Arrays.asList(scores));
    wheelView.setStyle( 2 );
    wheelView.setTextColor(R.color.status_text_night);
    wheelView.setDefaultPosition(5);

    etMessage = contentView.findViewById(R.id.like_message);

    Button cancel = contentView.findViewById(R.id.like_cancel);
    cancel.setOnClickListener(v -> dismiss());

    Button confirm = contentView.findViewById(R.id.like_add);
    confirm.setOnClickListener(v -> {
      if (mListener != null) {
        //mListener.OnLikeAction(wheelView.getSelectionItem().toString(), etMessage.getText().toString());
        mListener.OnLikeAction(wheelView.getCurrentItem().toString(), etMessage.getText().toString());

      }
      dismiss();
    });

    // get device size
    Display display = context.getWindowManager().getDefaultDisplay();
    final Point size = new Point();
    display.getSize(size);

    this.setContentView(contentView);
    this.setWidth((int) (size.x * 0.75));
    this.setHeight((int) (size.y * 0.4));
    this.setFocusable(true);
  }

  public interface OnLikeInterface {
    void OnLikeAction(String score, String msg);
  }
}
