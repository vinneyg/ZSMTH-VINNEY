package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.zfdang.SMTHApplication;
import com.zfdang.multiple_images_selector.ImagesSelectorActivity;
import com.zfdang.multiple_images_selector.SelectorSettings;
import com.zfdang.zsmth_android.helpers.KeyboardLess;
import com.zfdang.zsmth_android.helpers.NewToast;
import com.zfdang.zsmth_android.helpers.StringUtils;
import com.zfdang.zsmth_android.models.ComposePostContext;
import com.zfdang.zsmth_android.newsmth.AjaxResponse;
import com.zfdang.zsmth_android.newsmth.SMTHHelper;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Locale;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class ComposePostActivity extends SMTHBaseActivity {

  public static final int COMPOSE_ACTIVITY_REQUEST_CODE = 107;
  private static final int REQUEST_CODE = 653;
  private static final String TAG = "ComposePostActivity";

  private LinearLayout mUserRow;
  private EditText mUserID;
  private EditText mTitle;
  private LinearLayout mAttachRow;
  private EditText mAttachments;
  @SuppressLint("UseSwitchCompatOrMaterialCode")
  private Switch mCompress;
  private EditText mContent;
  private ArrayList<String> mPhotos;
  private TextView mContentCount;

  private ComposePostContext mPostContext;

  // used to show progress while publishing
  private static int totalSteps = 1;
  private static int currentStep = 1;

  private int postPublishResult = 0;
  private String postPublishMessage = null;
  private AjaxResponse lastResponse = null;

  private ActivityResultLauncher<Intent> mActivityLoginResultLauncher;
  private ActivityResultLauncher<Intent> mActivityComposeResultLauncher;


  public void startImageSelector() {
    // start multiple photos selector
    Intent intent = new Intent(ComposePostActivity.this, ImagesSelectorActivity.class);
    // max number of images to be selected
    intent.putExtra(SelectorSettings.SELECTOR_MAX_IMAGE_NUMBER, 5);
    // min size of image which will be shown; to filter tiny images (mainly icons)
    intent.putExtra(SelectorSettings.SELECTOR_MIN_IMAGE_SIZE, 10000); // file size > 10k
    // show camera or not
    intent.putExtra(SelectorSettings.SELECTOR_SHOW_CAMERA, true);
    // pass current selected images as the initial value
    intent.putStringArrayListExtra(SelectorSettings.SELECTOR_INITIAL_SELECTED_LIST, mPhotos);
    // start the selector
    //startActivityForResult(intent, REQUEST_CODE);
    mActivityComposeResultLauncher.launch(intent);
  }


  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_compose_post);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    ActionBar bar = getSupportActionBar();
    if (bar != null) {
      bar.setDisplayHomeAsUpEnabled(true);
    }

    OnBackPressedDispatcher dispatcher = getOnBackPressedDispatcher();

    dispatcher.addCallback(this, new OnBackPressedCallback(true) {
      @Override
      public void handleOnBackPressed() {
        // Handle the back button press here
        onBackAction();
      }
    });

    // Initialize the ActivityResultLauncher object.
    mActivityLoginResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if(result.getResultCode() == Activity.RESULT_OK)
              {
                publishPost();
              }
            });

    // Initialize the ActivityResultLauncher object.
    mActivityComposeResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if(result.getResultCode() == Activity.RESULT_OK)
              {
                if (result.getData() != null) {
                  mPhotos = result.getData().getStringArrayListExtra(SelectorSettings.SELECTOR_RESULTS);
                  if (mPhotos == null) {
                    Log.e(TAG, "mPhotos is null.");
                    return;
                  }
                  mAttachments.setText(String.format(Locale.CHINA,"共有%d个附件", mPhotos.size()));
                  StringBuilder attachments = new StringBuilder();
                  for (int i = 0; i < mPhotos.size(); i++) {
                    String UPLOAD_TEMPLATE = " [upload=%d][/upload] ";
                    attachments.append(String.format(Locale.CHINA, UPLOAD_TEMPLATE, i + 1));
                  }

                  // https://stackoverflow.com/questions/3609174/android-insert-text-into-edittext-at-current-position
                  mContent.getText().insert(mContent.getSelectionStart(), attachments.toString());

                }
              }
            });

    mPhotos = new ArrayList<>();

    mUserRow = findViewById(R.id.compose_post_userid_row);
    mUserID = findViewById(R.id.compose_post_userid);
    mTitle = findViewById(R.id.compose_post_title);
    mAttachRow = findViewById(R.id.compose_post_attach_row);
    mAttachments = findViewById(R.id.compose_post_attach);
    mCompress = findViewById(R.id.compose_post_attach_switch);
    mContent = findViewById(R.id.compose_post_content);
    mContentCount = findViewById(R.id.compose_post_content_label);

    String originalHint = mContent.getHint().toString();

    mContent.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus) {
        // 获取焦点时清除 hint
        mContent.setHint("");
      } else {
        if (TextUtils.isEmpty(mContent.getText())) {
          // 失去焦点且无输入内容时恢复 hint
          mContent.setHint(originalHint);
        }
      }
    });

    Button mButton = findViewById(R.id.compose_post_attach_button);
    mButton.setOnClickListener(v -> {
      KeyboardLess.$hide(v.getContext(), mContent);
      startImageSelector();
    });

    mContent.addTextChangedListener(new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override public void afterTextChanged(Editable s) {
        mContentCount.setText(String.format(Locale.CHINA,"文章字数:%d", s.length()));
      }
    });

    // init controls from Intent
    initFromIntent();

    restorePostContentFromCache();

    // open keypads
    KeyboardLess.$show(this, mContent);
  }

  // three methods to manage content cache
  private void restorePostContentFromCache() {
    final String content = Settings.getInstance().getPostCache();
    if (content != null && !content.isEmpty() && content.length() != mContent.getText().toString().length()) {
      AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ComposePostActivity.this,  R.style.MyDialogStyle);
      alertDialogBuilder.setTitle("恢复缓存的内容?")
              .setMessage(StringUtils.getEllipsizedMidString(content, 240))
              .setCancelable(false)
              .setPositiveButton("恢复内容", (dialog, id) -> {
                // if this button is clicked, close current activity
                mContent.setText(content);
              })
              .setNegativeButton("放弃内容", (dialog, id) -> {
                // if this button is clicked, just close the dialog box and do nothing
                dialog.cancel();
              })
              .create()
              .show();
    }
  }

  private void savePostContentToCache() {
    String content = mContent.getText().toString();
    Settings.getInstance().setPostCache(content);
  }

  private void clearPostContentCache() {
    Settings.getInstance().setPostCache("");
  }

  @Override protected void onPause() {
    // this method will also be called after activity.finish(), so we will have to set mContent.SetText("") before .finish()
    super.onPause();
    savePostContentToCache();
  }

  public void initFromIntent() {
    // get ComposePostContext from caller
    Intent intent = getIntent();
    mPostContext = intent.getParcelableExtra(SMTHApplication.COMPOSE_POST_CONTEXT);
    String mReadMode = intent.getStringExtra(SMTHApplication.READ_MODE);
    if (mPostContext == null) {
      Log.e(TAG, "mPostContext is null.");
      return;
    }
    //        Log.d(TAG, "initFromIntent: " + mPostContext.toString());

    // there are totally 5 different cases: ( 3 & 4 can be handled in the same way)
    // 1. new post:   invalid post && !isThroughMail
    // 2. reply post: valid post && !isThroughMail
    // 3. reply post through mail: valid post && isThroughMail
    // 4. reply mail: valid post && isThroughMail
    // 5. write new mail: invalid post && isThroughMail

    // set title, hide userRow / attachRow if necessary
    if (mPostContext.getComposingMode() == ComposePostContext.MODE_NEW_POST) {
      mUserRow.setVisibility(View.GONE);
      setTitle(String.format(Locale.CHINA,"发表文章@%s", mPostContext.getBoardEngName()));
      // set focus to title
      mTitle.requestFocus();
    } else if (mPostContext.getComposingMode() == ComposePostContext.MODE_REPLY_POST) {
      mUserRow.setVisibility(View.GONE);
      setTitle(String.format(Locale.CHINA,"回复文章@%s", mPostContext.getBoardEngName()));
      // set focus to content
      mContent.requestFocus();
    } else if (mPostContext.getComposingMode() == ComposePostContext.MODE_EDIT_POST) {
      mUserRow.setVisibility(View.GONE);
      mAttachRow.setVisibility(View.GONE);
      setTitle(String.format(Locale.CHINA,"修改文章@%s", mPostContext.getBoardEngName()));
      // set focus to content
      mContent.requestFocus();
    } else if (mPostContext.getComposingMode() == ComposePostContext.MODE_NEW_MAIL) {
      mAttachRow.setVisibility(View.GONE);
      setTitle("写新信件");
      // set focus to recipient
      mUserID.requestFocus();
    } else if (mPostContext.getComposingMode() == ComposePostContext.MODE_NEW_MAIL_TO_USER) {
      mAttachRow.setVisibility(View.GONE);
      setTitle("写新信件");
      mUserID.setText(mPostContext.getPostAuthor());
      mUserID.setEnabled(false);
      //Set focus to Title
      mTitle.requestFocus();
      // set focus to content
      //mContent.requestFocus();
    } else if (mPostContext.getComposingMode() == ComposePostContext.MODE_REPLY_MAIL) {
      mAttachRow.setVisibility(View.GONE);
      setTitle("回复信件");
      mUserID.setText(mPostContext.getPostAuthor());
      mUserID.setEnabled(false);
      // set focus to content
      mContent.requestFocus();
    }

    // set post title & content
    if (mPostContext.isValidPost()) {
      // have valid post information
      if (mPostContext.getComposingMode() == ComposePostContext.MODE_EDIT_POST) {
        mTitle.setText(mPostContext.getPostTitle());
        mContent.setText(mPostContext.getPostContent());
      } else {
        String title = mPostContext.getPostTitle();
        if (title != null && !title.startsWith("Re:")) {
          title = String.format(Locale.CHINA,"Re: %s", title);
        }
        mTitle.setText(title);

        String[] lines = mPostContext.getPostContent().split("\n");
        StringBuilder wordList = new StringBuilder();
        wordList.append("\n\n");
        wordList.append(String.format(Locale.CHINA,"【 在 %s 的大作中提到: 】", mPostContext.getPostAuthor())).append("\n");
        //for (int i = 0; i < lines.length && i < 5; i++) {
        for (String line : lines) {
          if (line.startsWith("--")) {
            // this might be the start of signature, ignore following lines in quote
            break;
          }
          wordList.append(String.format(Locale.CHINA, ": %s", line)).append("\n");
        }
        mContent.setText(new String(wordList));
      }

      // if the content is not empty, set cursor to the beginning
      mContent.setSelection(0);
    }
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.compose_post_menu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    int code = item.getItemId();
    if (code == android.R.id.home) {
      onBackAction();
    } else if (code == R.id.compose_post_publish) {
      publishPost();
    }
    return super.onOptionsItemSelected(item);
  }

  public static class BytesContainer {
    public String filename;
    public byte[] bytes;

    public BytesContainer(String filename, byte[] bytes) {
      this.bytes = bytes;
      this.filename = filename;
    }
  }

  public void publishPost() {

    postPublishResult = AjaxResponse.AJAX_RESULT_OK;
    postPublishMessage = "";
    lastResponse = null;

    String progressHint = "发表中(%d/%d)...";
    ComposePostActivity.totalSteps = 1;
    ComposePostActivity.currentStep = 1;

    if (mPhotos != null) {
      ComposePostActivity.totalSteps += mPhotos.size();
    }

    showProgress(String.format(Locale.CHINA,progressHint, ComposePostActivity.currentStep, ComposePostActivity.totalSteps));

    final SMTHHelper helper = SMTHHelper.getInstance();

    // update attachments
    final boolean bCompress = this.mCompress.isChecked();
    Observable<AjaxResponse> resp1 = Observable.fromIterable(mPhotos).map(filename -> {
      byte[] bytes = SMTHHelper.getBitmapBytesWithResize(filename, bCompress);
      return new BytesContainer(filename, bytes);
    }).map(container -> {
      //RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), container.bytes);
      MediaType mediaType = MediaType.get("image/jpeg");
      RequestBody requestBody = RequestBody.create(container.bytes, mediaType);
      AjaxResponse resp =
              helper.wService.uploadAttachment(mPostContext.getBoardEngName(), StringUtils.getLastStringSegment(container.filename),
                      requestBody).blockingFirst();
      if (resp != null) {
        return resp;
      } else {
        Log.d(TAG, "call: " + "failed to upload attachment " + container.filename);
        return null;
      }
    });

    // publish post
    String postContent = mContent.getText().toString();
    if (Settings.getInstance().bUseSignature()) {
      postContent += "\n\n" + String.format(Locale.CHINA,"#发自zSMTH-v-@%s", Settings.getInstance().getSignature());
      //postContent += "\n" + String.format(Locale.CHINA,"#发自zSMTH-v-@%s", Build.BRAND+"-"+Settings.getInstance().getSignature());
    }
    Observable<AjaxResponse> resp2 = null;
    if (mPostContext.getComposingMode() == ComposePostContext.MODE_NEW_MAIL
            || mPostContext.getComposingMode() == ComposePostContext.MODE_NEW_MAIL_TO_USER
            || mPostContext.getComposingMode() == ComposePostContext.MODE_REPLY_MAIL) {
      String userid = mUserID.getText().toString().trim();
      resp2 = SMTHHelper.sendMail(userid, mTitle.getText().toString(), postContent);
      SMTHApplication.bNewMailSent = true;
    } else if (mPostContext.getComposingMode() == ComposePostContext.MODE_NEW_POST
            || mPostContext.getComposingMode() == ComposePostContext.MODE_REPLY_POST) {
      resp2 =
              SMTHHelper.publishPost(mPostContext.getBoardEngName(), mTitle.getText().toString(), postContent, "0", mPostContext.getPostId());
      SMTHApplication.bNewMailSent = true;
    } else if (mPostContext.getComposingMode() == ComposePostContext.MODE_EDIT_POST) {
      postContent = mContent.getText().toString();
      if (Settings.getInstance().bUseSignature()) {
        postContent += "\n" + String.format(Locale.CHINA,"#修改自zSMTH-v-@%s", Settings.getInstance().getSignature());
      }
      SMTHApplication.bNewMailSent = true;
      resp2 = SMTHHelper.editPost(mPostContext.getBoardEngName(), mPostContext.getPostId(), mTitle.getText().toString(), postContent);
    }

    // process all these tasks one by one
    if (resp2 == null) {
      Log.e(TAG, "resp2 is null.");
      return;
    }
    Observable.concat(resp1, resp2)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                    new Observer<AjaxResponse>() {
                      @Override
                      public void onSubscribe(@NonNull Disposable disposable) {}

                      @Override
                      public void onNext(@NonNull AjaxResponse ajaxResponse) {
                        Log.d(TAG, "onNext: " + ajaxResponse.toString());
                        if (ajaxResponse.getAjax_st() != AjaxResponse.AJAX_RESULT_OK) {
                          postPublishResult = AjaxResponse.AJAX_RESULT_FAILED;
                          postPublishMessage += ajaxResponse.getAjax_msg() + "\n";
                        }
                        lastResponse = ajaxResponse;
                        ComposePostActivity.currentStep++;
                        showProgress(
                                String.format(
                                        Locale.CHINA,
                                        progressHint,
                                        ComposePostActivity.currentStep,
                                        ComposePostActivity.totalSteps));
                      }

                      @Override
                      public void onError(@NonNull Throwable e) {
                        dismissProgress();
                        /*
                        Toast.makeText(
                                        SMTHApplication.getAppContext(),
                                        "发生错误:\n" + e.toString(),
                                        Toast.LENGTH_SHORT)
                                .show();
                        */
                        NewToast.makeText(
                                SMTHApplication.getAppContext(),
                                "发生错误:\n" + e.toString(),
                                Toast.LENGTH_SHORT);
                      }

                      @Override
                      public void onComplete() {
                        dismissProgress();

                        String message;
                        if (postPublishResult != AjaxResponse.AJAX_RESULT_OK) {
                          message = "操作失败! \n错误信息:\n" + postPublishMessage;
                          //Toast.makeText(ComposePostActivity.this, message, Toast.LENGTH_SHORT).show();
                          NewToast.makeText(ComposePostActivity.this, message, Toast.LENGTH_SHORT);

                          if(SMTHApplication.bNewMailSent)
                            SMTHApplication.bNewMailSent = false;
                          if(!SMTHApplication.isValidUser()){
                            Intent intent = new Intent(ComposePostActivity.this, LoginActivity.class);
                            //startActivityForResult(intent, MainActivity.LOGIN_ACTIVITY_REQUEST_CODE);
                            mActivityLoginResultLauncher.launch(intent);
                          }
                        } else {
                          if (lastResponse != null) {
                            // if we have valid last response, use the message.
                            message = lastResponse.getAjax_msg();
                          } else {
                            // otherwise, compose the message by ourself
                            message = "成功!";
                          }

                          KeyboardLess.$hide(ComposePostActivity.this, mContent);

                          if (message != null && !message.contains("本文可能含有不当内容")) {
                            if(!message.contains("成功"))
                              //Toast.makeText(SMTHApplication.getAppContext(), message, Toast.LENGTH_SHORT).show();
                              NewToast.makeText(SMTHApplication.getAppContext(), message, Toast.LENGTH_SHORT);
                            mContent.setText("");
                            clearPostContentCache();
                            ComposePostActivity.this.finish();
                          } else {
                            //Toast.makeText(SMTHApplication.getAppContext(), message, Toast.LENGTH_SHORT).show();
                            NewToast.makeText(SMTHApplication.getAppContext(), message, Toast.LENGTH_SHORT);
                            if(SMTHApplication.bNewMailSent)
                              SMTHApplication.bNewMailSent = false;

                          }
                        }
                      }
                    });
  }

  public void onBackAction() {
    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ComposePostActivity.this,R.style.MyDialogStyle);
    alertDialogBuilder.setTitle("放弃编辑")
            .setMessage("将删除已录入的内容")
            .setCancelable(false)
            .setNegativeButton("放弃编辑", (dialog, id) -> {
              // if this button is clicked, close current activity
              KeyboardLess.$hide(ComposePostActivity.this, mContent);
              mContent.setText("");
              clearPostContentCache();
              ComposePostActivity.this.finish();
            })
            .setPositiveButton("继续编辑", (dialog, id) -> {
              // if this button is clicked, just close the dialog box and do nothing
              dialog.cancel();
            })
            .create()
            .show();
  }
}
