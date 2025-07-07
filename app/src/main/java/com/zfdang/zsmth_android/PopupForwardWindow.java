package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.models.Post;

/**
 * Created by zfdang on 2016-4-26.
 */
public class PopupForwardWindow extends PopupWindow {
  private static final String TAG = "PopupForwardWindow";

  Activity mContext;
  private OnForwardInterface mListener;
  //private EditText etMessage;
  static public Post post;
  private RadioButton mTargetSelf;
  private RadioButton mTargetOther;
  private EditText mTargetOtherContent;
  private CheckBox mThread;
  private CheckBox mNoRef;
  private CheckBox mNoAtt;

  private AutoCompleteTextView mTargetBoard;
  //    private ArrayAdapter<String> mBoarddapter;

  // http://stackoverflow.com/questions/23464232/how-would-you-create-a-popover-view-in-android-like-facebook-comments
  public void initPopupWindow(final Activity context, Post post) {
    mContext = context;
    PopupForwardWindow.post = post;
    if (context instanceof OnForwardInterface) {
      mListener = (OnForwardInterface) context;
    } else {
      Log.d(TAG, "initPopupWindow: " + "context does not implement SearchInterface");
    }

    LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    @SuppressLint("InflateParams") View contentView = layoutInflater.inflate(R.layout.popup_forward_layout, null, false);

    mTargetSelf = contentView.findViewById(R.id.popup_forward_target_self);
    mTargetSelf.setOnClickListener(v -> {
      if (mTargetSelf.isChecked()) {
        mTargetOther.setChecked(false);
        mTargetOtherContent.setEnabled(false);
      }
    });
    mTargetOther = contentView.findViewById(R.id.popup_forward_target_other);
    mTargetOther.setOnClickListener(v -> {
      if (mTargetOther.isChecked()) {
        mTargetSelf.setChecked(false);
        mTargetOtherContent.setEnabled(true);
      }
    });
    mTargetOtherContent = contentView.findViewById(R.id.popup_forward_target_other_content);

    mThread = contentView.findViewById(R.id.popup_forward_thread);
    mNoRef = contentView.findViewById(R.id.popup_forward_noref);
    mNoAtt = contentView.findViewById(R.id.popup_forward_noatt);

    mThread.setOnClickListener(v -> {
      mNoRef.setEnabled(mThread.isChecked());
      Settings.getInstance().setThread(!Settings.getInstance().isThread());
    });

    mNoRef.setOnClickListener(v -> Settings.getInstance().setRef(!Settings.getInstance().isRef()));
    mNoAtt.setOnClickListener(v -> Settings.getInstance().setAtt(!Settings.getInstance().isAtt()));

    // init status
    if(Settings.getInstance().isTopicFwdSelf()) {
      mTargetSelf.setChecked(true);
      mTargetOther.setChecked(false);
      mTargetOtherContent.setEnabled(false);
    }
    else{
      mTargetSelf.setChecked(false);
      mTargetOther.setChecked(true);
      mTargetOtherContent.setEnabled(true);
      mTargetOtherContent.setText(Settings.getInstance().getTarget());
    }

    mThread.setChecked(Settings.getInstance().isThread());
    mNoRef.setChecked(Settings.getInstance().isRef());
    mNoAtt.setChecked(Settings.getInstance().isAtt());

    Button cancel = contentView.findViewById(R.id.popup_forward_cancel);
    cancel.setOnClickListener(v -> dismiss());

    Button confirm = contentView.findViewById(R.id.popup_forward_confirm);
    confirm.setOnClickListener(v -> {
      if (mListener != null) {
        String target = null;
        if (SMTHApplication.activeUser != null) {
          target = SMTHApplication.activeUser.getId();
        }
        if (mTargetOther.isChecked()) {
          target = mTargetOtherContent.getText().toString().trim();
          Settings.getInstance().setTarget(target);
        }
        mListener.OnForwardAction(PopupForwardWindow.post, target, mThread.isChecked(), mNoRef.isChecked(), mNoAtt.isChecked());
      }
      dismiss();
    });

    // implement post repost part
    mTargetBoard = contentView.findViewById(R.id.popup_post_target);

    Button cancel2 = contentView.findViewById(R.id.popup_post_cancel);
    cancel2.setOnClickListener(v -> dismiss());

    Button confirm2 = contentView.findViewById(R.id.popup_post_confirm);
    confirm2.setOnClickListener(v -> {
      if (mListener != null) {
        String target = mTargetBoard.getText().toString().trim();
        String [] newTarget = target.split(",");
        //ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        for (String s : newTarget) {
          mListener.OnRePostAction(PopupForwardWindow.post, s, "on");

          /*
           final int index = i;
            singleThreadExecutor.execute(new Runnable() {
              @Override
              public void run() {
                try {
                  System.out.println(newTarget[index]);
                  mListener.OnRePostAction(PopupForwardWindow.post, newTarget[index], "on");
                  Thread.sleep(1000);

                } catch (InterruptedException e) {
                  e.printStackTrace();
                }

              }
            });
          */
        }
        }
      dismiss();
    });

    // get device size
    Display display = context.getWindowManager().getDefaultDisplay();
    final Point size = new Point();
    display.getSize(size);

    this.setContentView(contentView);
    this.setWidth((int) (size.x * 0.95));
    this.setHeight((int) (size.y * 0.65));
    this.setFocusable(true);
  }

  //    public void loadBoardsForAutoCompletion() {
  //        final List<String> allboards = new ArrayList<>();
  //        if(mTargetBoard != null) {
  //            // all boards loaded in cached file
  //            Observable.from(SMTHHelper.LoadBoardListFromCache(SMTHHelper.BOARD_TYPE_ALL, null))
  //                    .subscribeOn(Schedulers.io())
  //                    .observeOn(AndroidSchedulers.mainThread())
  //                    .subscribe(new Subscriber<Board>() {
  //                        @Override
  //                        public void onCompleted() {
  //                            mBoarddapter = new ArrayAdapter<String>(mTargetBoard.getContext(), android.R.layout.simple_dropdown_item_1line, allboards);
  //                            mTargetBoard.setAdapter(mBoarddapter);
  //                            Log.d(TAG, "onCompleted: " + allboards.size());
  //                        }
  //
  //                        @Override
  //                        public void onError(Throwable e) {
  //                            Toast.makeText(SMTHApplication.getAppContext(), e.toString(), Toast.LENGTH_SHORT).show();
  //                        }
  //
  //                        @Override
  //                        public void onNext(Board board) {
  //                            allboards.add(board.getBoardEngName());
  //                        }
  //                    });
  //        }
  //    }

  public interface OnForwardInterface {
    void OnForwardAction(Post post, String target, boolean threads, boolean noref, boolean noatt);

    void OnRePostAction(Post post, String target, String outgo);
  }
}
