package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupWindow;

/**
 * Created by zfdang on 2016-4-23.
 */
public class PopupSearchWindow extends PopupWindow {
  private static final String TAG = "PopupSearchWindow";

  private SearchInterface mListener = null;
  private EditText etKeyword;
  private EditText etAuthor;
  private CheckBox ckAttachment;
  private CheckBox ckElite;

  // http://stackoverflow.com/questions/23464232/how-would-you-create-a-popover-view-in-android-like-facebook-comments
  public void initPopupWindow(final Activity context) {
    if (context instanceof SearchInterface) {
      mListener = (SearchInterface) context;
    } else {
      Log.d(TAG, "initPopupWindow: " + "context does not implement SearchInterface");
    }

    LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    @SuppressLint("InflateParams") View contentView = layoutInflater.inflate(R.layout.popup_topic_search, null, false);

    Button cancel = contentView.findViewById(R.id.search_cancel);
    cancel.setOnClickListener(v -> dismiss());

    Button confirm = contentView.findViewById(R.id.search_confirm);
    confirm.setOnClickListener(v -> {
      if (mListener != null) {
        mListener.OnSearchAction(etKeyword.getText().toString(), etAuthor.getText().toString(), ckElite.isChecked(),
            ckAttachment.isChecked());
      }
      dismiss();
    });

    etKeyword = contentView.findViewById(R.id.search_keyword);
    etAuthor = contentView.findViewById(R.id.search_author);
    ckAttachment = contentView.findViewById(R.id.search_attachment);
    ckElite = contentView.findViewById(R.id.search_elite);

    // get device size
    Display display = context.getWindowManager().getDefaultDisplay();
    final Point size = new Point();
    display.getSize(size);

    this.setContentView(contentView);
    this.setWidth((int) (size.x * 0.9));
    this.setHeight((int) (size.y * 0.5));
    this.setFocusable(true);

    new Handler().postDelayed(() -> {
      etAuthor.requestFocus();
      InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.showSoftInput(etAuthor, InputMethodManager.SHOW_IMPLICIT);
    }, 300);
  }

  public interface SearchInterface {
    void OnSearchAction(String keyword, String author, boolean elite, boolean attachment);
  }
}
