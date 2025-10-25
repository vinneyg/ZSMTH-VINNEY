package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.fresco.WrapContentDraweeView;
import com.zfdang.zsmth_android.helpers.NewToast;
import com.zfdang.zsmth_android.models.Board;
import com.zfdang.zsmth_android.models.ComposePostContext;
import com.zfdang.zsmth_android.models.ContentSegment;
import com.zfdang.zsmth_android.models.Mail;
import com.zfdang.zsmth_android.models.Post;
import com.zfdang.zsmth_android.models.Topic;
import com.zfdang.zsmth_android.newsmth.SMTHHelper;
import java.util.List;
import java.util.Objects;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MailContentActivity extends AppCompatActivity {
  //private static final String TAG = "MailContent";
  private Mail mMail;
  private int mPostGroupId;
  private Post mPost;

  public TextView mMailTitle;
  public TextView mPostAuthor;
  public TextView mPostIndex;
  public TextView mPostPublishDate;
  private LinearLayout mViewGroup;
  public TextView mPostContent;

  private boolean isMenuItemVisible = false;

  private Button mPostReplyButton;
  private Button mPostMoreButton;

  private GestureDetector gestureDetector;
  private static final int SWIPE_THRESHOLD = 100;
  private static final int SWIPE_VELOCITY_THRESHOLD = 100;

  @Override protected void onDestroy() {
    super.onDestroy();
  }

  @Override protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_mail_content);
    gestureDetector = new GestureDetector(this, new SwipeGestureListener());

    mMailTitle = findViewById(R.id.mail_content_title);
    // init post widget
    mPostAuthor = findViewById(R.id.post_author);
    mPostIndex = findViewById(R.id.post_index);
    //mPostIndex.setVisibility(View.GONE);
    mPostPublishDate = findViewById(R.id.post_publish_date);
    mViewGroup = findViewById(R.id.post_content_holder);
    mPostContent = findViewById(R.id.post_content);

    mPostAuthor.setOnClickListener(v -> {
      if (Settings.getInstance().isSetIdCheck()) {
        Intent intent = new Intent(v.getContext(), QueryUserActivity.class);
        intent.putExtra(SMTHApplication.QUERY_USER_INFO, mMail.author);
        v.getContext().startActivity(intent);
      }
    });


    Toolbar toolbar = findViewById(R.id.mail_toolbar);
    setSupportActionBar(toolbar);

    // Show the Up button in the action bar.
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }


    // load mMail content
    Bundle bundle = getIntent().getExtras();
    mMail = Objects.requireNonNull(bundle).getParcelable(SMTHApplication.MAIL_OBJECT);

    assert mMail != null;
    isMenuItemVisible = mMail.isRefferedPost();

    if (mMail.isRefferedPost()) {
      mPostIndex.setText("作者");
    }else{
      mPostIndex.setText("发信人");
    }

    invalidateOptionsMenu();

    loadMailContent();
    mPostReplyButton = findViewById(R.id.btn_post_reply);
    mPostMoreButton = findViewById(R.id.btn_post_more);
    updateButtonVisibility();
    mPostMoreButton.setOnClickListener(v -> handleReplyMenuItem());

    // 使用 ViewTreeObserver 确保在布局完成后执行动画
    mViewGroup.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        // 移除监听器，避免重复触发
        mViewGroup.getViewTreeObserver().removeOnGlobalLayoutListener(this);

        // 设置初始位置在屏幕右侧
        mViewGroup.setTranslationX((float) mViewGroup.getWidth() /3);
        // 执行从右到左的动画

        mViewGroup.setAlpha(0f); // 初始透明度为0
        mViewGroup.animate()
                .translationX(0)
                .alpha(1f) // 最终透明度为1
                .setDuration(300)
                .setStartDelay(50)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
      }
    });
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    if (gestureDetector.onTouchEvent(ev)) {
      return true;
    }
    return super.dispatchTouchEvent(ev);
  }
  private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
    @Override
    public boolean onFling(MotionEvent e1, @androidx.annotation.NonNull MotionEvent e2, float velocityX, float velocityY) {
      try {
        if (e1 != null) {
          float diffX = e2.getX() - e1.getX();
          float diffY = e2.getY() - e1.getY();
          if (Math.abs(diffX) > Math.abs(diffY)
                  && Math.abs(diffX) > SWIPE_THRESHOLD
                  && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
                  && diffX != 0) {
            finish();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            return true;
          }
        }
      } catch (Exception e) {
        Log.e("MailContentActivity", "onFling: ", e);
      }
      return false;
    }
  }
  private void updateButtonVisibility() {
    float newLetterSpacing = 0f;
    mPostReplyButton.setVisibility(View.GONE);
    mPostMoreButton.setText("回复");
    mPostMoreButton.setLetterSpacing(newLetterSpacing);
  }

  public void loadMailContent() {
    SMTHHelper helper = SMTHHelper.getInstance();
    helper.wService.getMailContent(mMail.url).map(ajaxResponse -> {
      String msg = ajaxResponse.getAjax_msg();
      if( !TextUtils.equals(msg, "操作成功")) {
        throw new Exception(msg);
      }
      mPostGroupId = ajaxResponse.getGroup_id();
      return SMTHHelper.ParseMailContentFromWWW(ajaxResponse.getContent());
    }).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Post>() {
      @Override public void onSubscribe(@NonNull Disposable disposable) {

      }

      @Override public void onNext(@NonNull Post post) {
        mPost = post;

        // copy some attr from mail to post
        mPost.setAuthor(mMail.getFrom());
        mPost.setTitle(mMail.title);
        mPost.setPostID(mMail.getMailIDFromURL());

        mPostAuthor.setText(mPost.getRawAuthor());
        mPostPublishDate.setText(mPost.getFormatedDate());
        mMailTitle.setText(mPost.getTitle());
        inflateContentViewGroup(mViewGroup, mPostContent, mPost);
      }

      @SuppressLint("SetTextI18n")
      @Override public void onError(@NonNull Throwable e) {
        mPostContent.setText("读取内容失败: \n" + e.getMessage());
      }

      @Override public void onComplete() {

      }
    });
  }

  //    copied from PostRecyclerViewAdapter.inflateContentViewGroup, almost the same code
  public void inflateContentViewGroup(ViewGroup viewGroup, TextView contentView, final Post post) {
    // remove all child view in viewgroup
    viewGroup.removeAllViews();

    List<ContentSegment> contents = post.getContentSegments();
    if (contents == null) return;

    if (!contents.isEmpty()) {
      // there are multiple segments, add the first contentView first
      // contentView is always available, we don't have to inflate it again
      ContentSegment content = contents.get(0);
      setupTextView(contentView, content.getSpanned());
      Linkify.addLinks(contentView, Linkify.ALL);
      contentView.setTextIsSelectable(true);
      contentView.setMovementMethod(LinkMovementMethod.getInstance());
      //LinkBuilder.on(contentView).addLinks(ActivityUtils.getPostSupportedLinks(MailContentActivity.this)).build();

      viewGroup.addView(contentView);
    }

    // http://stackoverflow.com/questions/13438473/clicking-html-link-in-textview-fires-weird-androidruntimeexception
    final LayoutInflater inflater = getLayoutInflater();
    for (int i = 1; i < contents.size(); i++) {
      ContentSegment content = contents.get(i);

      if (content.getType() == ContentSegment.SEGMENT_IMAGE) {
        // Log.d("CreateView", "Image: " + content.getUrl());

        // Add the text layout to the parent layout
        WrapContentDraweeView image = (WrapContentDraweeView) inflater.inflate(R.layout.post_item_imageview, viewGroup, false);
        image.setImageFromStringURL(content.getUrl());

        // set onclicklistener
        image.setTag(R.id.image_tag, content.getImgIndex());

        // Add the text view to the parent layout
        viewGroup.addView(image);
      } else if (content.getType() == ContentSegment.SEGMENT_TEXT) {
        // Log.d("CreateView", "Text: " + content.getSpanned().toString());

        TextView tv = (TextView) inflater.inflate(R.layout.post_item_content, viewGroup, false);
        setupTextView(tv, content.getSpanned());
        //LinkBuilder.on(tv).addLinks(ActivityUtils.getPostSupportedLinks(MailContentActivity.this)).build();
        Linkify.addLinks(tv, Linkify.ALL);
        tv.setTextIsSelectable(true);
        tv.setMovementMethod(LinkMovementMethod.getInstance());

        /*
        // 获取支持的链接配置
        List<com.klinker.android.link_builder.Link> supportedLinks = ActivityUtils.getPostSupportedLinks(MailContentActivity.this);
        SpannableString spannableString = new SpannableString(tv.getText());

        for (com.klinker.android.link_builder.Link link : supportedLinks) {
          Pattern pattern = link.getPattern();
          Matcher matcher = pattern.matcher(spannableString);
          while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            ClickableSpan clickableSpan = new ClickableSpan() {
              @Override
              public void onClick(@NonNull View widget) {
                link.getClickListener().onClick(spannableString.subSequence(start, end).toString());
              }

              @Override
              public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(link.getTextColor());
                ds.setUnderlineText(true);
              }
            };
            spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
          }
        }
                tv.setText(spannableString);
        tv.setMovementMethod(LinkMovementMethod.getInstance());

         */
        // Add the text view to the parent layout
        viewGroup.addView(tv);
      }
    }
  }

  private CharSequence removeSpans(CharSequence text) {
    if (text instanceof Spannable) {
      Spannable spannable = (Spannable) text;
      ClickableSpan[] clickableSpans = spannable.getSpans(0, spannable.length(), ClickableSpan.class);
      for (ClickableSpan span : clickableSpans) {
        spannable.removeSpan(span);
      }
      return spannable;
    }
    return text;
  }

  // 在设置文本时调用 removeSpans 方法
  private void setupTextView(TextView textView, CharSequence text) {
    CharSequence cleanText = removeSpans(text); // 只移除 ClickableSpan，保留 URLSpan
    textView.setText(cleanText);
    textView.setTextIsSelectable(true);
    textView.setMovementMethod(LinkMovementMethod.getInstance());
    textView.setFocusable(true);
    textView.setFocusableInTouchMode(true);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == android.R.id.home) {
      handleHomeMenuItem();
      return true;
    } else if (id == R.id.mail_content_open_post) {
      handleOpenPostMenuItem();
      return true;
    }else if(id == R.id.mail_content_copy)
    {
      handleCopyMenuItem();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuItem openPostItem = menu.findItem(R.id.mail_content_open_post);
    if (openPostItem != null) {
      openPostItem.setVisible(isMenuItemVisible);
    }
    return super.onPrepareOptionsMenu(menu);
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.mail_content_menu, menu);

    return true;
  }

  public void setMenuItemVisible(boolean visible) {
    this.isMenuItemVisible = visible;
    invalidateOptionsMenu(); // 通知系统重新创建菜单
  }

  private void handleHomeMenuItem() {
    if (mMail.isRefferedPost()) {
      Board board = new Board();
      board.initAsBoard(mMail.fromBoard, mMail.fromBoard, "", "");

      Intent intent = createBoardTopicIntent(board);
      startActivity(intent);
      overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
      finish();
    } else {
      //onBackPressed();
      finish();
      overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
  }

  private void handleReplyMenuItem() {
    if (mPost == null) {
      //Toast.makeText(MailContentActivity.this, "帖子内容错误，无法回复！", Toast.LENGTH_SHORT).show();
      NewToast.makeText(MailContentActivity.this, "帖子内容错误，无法回复！", Toast.LENGTH_SHORT);
    } else {
      ComposePostContext postContext = createComposePostContext();
      Intent intent = new Intent(this, ComposePostActivity.class);
      intent.putExtra(SMTHApplication.COMPOSE_POST_CONTEXT, postContext);
      startActivity(intent);
    }
  }

  private void handleOpenPostMenuItem() {
    if (mPost != null && mMail.isRefferedPost()) {
      Topic topic = createTopic();
      Intent intent = new Intent(this, PostListActivity.class);
      intent.putExtra(SMTHApplication.TOPIC_OBJECT, topic);
      intent.putExtra(SMTHApplication.FROM_BOARD, SMTHApplication.FROM_BOARD_BOARD);
      startActivity(intent);
    } else {
      //Toast.makeText(MailContentActivity.this, "普通邮件，无法打开原贴!", Toast.LENGTH_SHORT).show();
      NewToast.makeText(MailContentActivity.this, "普通邮件，无法打开原贴!", Toast.LENGTH_SHORT);
    }
  }

  private void handleCopyMenuItem() {
    String content;
    if (mPost != null) {
      content = mPost.getRawContent();

      final android.content.ClipboardManager clipboardManager =
              (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
      final android.content.ClipData clipData = android.content.ClipData.newPlainText("PostContent", content);
      clipboardManager.setPrimaryClip(clipData);

      //Toast.makeText(MailContentActivity.this, "帖子内容已复制到剪贴板", Toast.LENGTH_SHORT).show();
      NewToast.makeText(MailContentActivity.this, "帖子内容已复制到剪贴板", Toast.LENGTH_SHORT);
    } else {
      //Toast.makeText(MailContentActivity.this, "复制失败！", Toast.LENGTH_SHORT).show();
      NewToast.makeText(MailContentActivity.this, "复制失败！", Toast.LENGTH_SHORT);
    }
  }

  private Intent createBoardTopicIntent(Board board) {
    Intent intent = new Intent(this, BoardTopicActivity.class);
    intent.putExtra(SMTHApplication.BOARD_OBJECT, (Parcelable) board);
    return intent;
  }

  private ComposePostContext createComposePostContext() {
    ComposePostContext postContext = new ComposePostContext();
    postContext.setPostId(mPost.getPostID());
    postContext.setPostTitle(mPost.getTitle());
    postContext.setPostAuthor(mPost.getRawAuthor());
    postContext.setPostContent(mPost.getRawContent());

    if (mMail.isRefferedPost()) {
      postContext.setBoardEngName(mMail.fromBoard);
      postContext.setComposingMode(ComposePostContext.MODE_REPLY_POST);
    } else {
      postContext.setComposingMode(ComposePostContext.MODE_REPLY_MAIL);
    }
    return postContext;
  }

  private Topic createTopic() {
    Topic topic = new Topic();
    topic.setTopicID(Integer.toString(mPostGroupId));
    topic.setAuthor(mPost.getRawAuthor());
    topic.setTitle(mPost.getTitle());
    topic.setBoardEngName(mMail.fromBoard);
    topic.setBoardChsName(mMail.fromBoard);
    return topic;
  }

}
