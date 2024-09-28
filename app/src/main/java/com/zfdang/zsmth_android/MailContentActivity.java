package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.jude.swipbackhelper.SwipeBackHelper;
import com.klinker.android.link_builder.LinkBuilder;
import com.klinker.android.link_builder.LinkConsumableTextView;
import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.fresco.WrapContentDraweeView;
import com.zfdang.zsmth_android.helpers.ActivityUtils;
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
  public LinkConsumableTextView mPostContent;

  @Override protected void onDestroy() {
    super.onDestroy();
    SwipeBackHelper.onDestroy(this);
  }

  @Override protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    SwipeBackHelper.onPostCreate(this);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    SwipeBackHelper.onCreate(this);

    setContentView(R.layout.activity_mail_content);

    mMailTitle = findViewById(R.id.mail_content_title);
    // init post widget
    mPostAuthor = findViewById(R.id.post_author);
    mPostIndex = findViewById(R.id.post_index);
    mPostIndex.setVisibility(View.GONE);
    mPostPublishDate = findViewById(R.id.post_publish_date);
    mViewGroup = findViewById(R.id.post_content_holder);
    mPostContent = findViewById(R.id.post_content);

    mPostAuthor.setOnClickListener(v -> {
      if (Settings.getInstance().isSetIdCheck()) {
        Intent intent = new Intent(v.getContext(), QueryUserActivity.class);
        // intent.putExtra(SMTHApplication.QUERY_USER_INFO, mMail.getFrom());
        intent.putExtra(SMTHApplication.QUERY_USER_INFO, mMail.author);
        v.getContext().startActivity(intent);
      }
    });

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    // Show the Up button in the action bar.
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    // load mMail content
    Bundle bundle = getIntent().getExtras();
    mMail = Objects.requireNonNull(bundle).getParcelable(SMTHApplication.MAIL_OBJECT);
    loadMailContent();
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
      contentView.setText(content.getSpanned());
      LinkBuilder.on(contentView).addLinks(ActivityUtils.getPostSupportedLinks(MailContentActivity.this)).build();

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

        // Add the links and make the links clickable
        LinkConsumableTextView tv = (LinkConsumableTextView) inflater.inflate(R.layout.post_item_content, viewGroup, false);
        tv.setText(content.getSpanned());
        LinkBuilder.on(tv).addLinks(ActivityUtils.getPostSupportedLinks(MailContentActivity.this)).build();

        // Add the text view to the parent layout
        viewGroup.addView(tv);
      }
    }
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == android.R.id.home) {
      if (mMail.isRefferedPost() ) {
          //Toast.makeText(MailContentActivity.this, "回复POST提醒", Toast.LENGTH_SHORT).show();
          Board board = new Board();
          board.initAsBoard(mMail.fromBoard, mMail.fromBoard, "", "");

          Intent intent = new Intent(this, BoardTopicActivity.class);
          intent.putExtra(SMTHApplication.BOARD_OBJECT, (Parcelable)board);
          startActivity(intent);
          finish();
      }
      else{
        onBackPressed();
      }
      return true;
    } else if (id == R.id.mail_content_reply) {
      if (mPost == null) {
        Toast.makeText(MailContentActivity.this, "帖子内容错误，无法回复！", Toast.LENGTH_SHORT).show();
        return true;
      } else {
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

        Intent intent = new Intent(this, ComposePostActivity.class);
        intent.putExtra(SMTHApplication.COMPOSE_POST_CONTEXT, postContext);
        startActivity(intent);
      }
    } else if (id == R.id.mail_content_open_post) {
      if (mPost != null && mMail.isRefferedPost()) {
        Topic topic = new Topic();
        topic.setTopicID(Integer.toString(mPostGroupId));
        topic.setAuthor(mPost.getRawAuthor());
        topic.setTitle(mPost.getTitle());

        Intent intent = new Intent(this, PostListActivity.class);
        topic.setBoardEngName(mMail.fromBoard);
        topic.setBoardChsName(mMail.fromBoard);
        intent.putExtra(SMTHApplication.TOPIC_OBJECT, topic);
        intent.putExtra(SMTHApplication.FROM_BOARD, SMTHApplication.FROM_BOARD_BOARD);
        startActivity(intent);
      } else {
        Toast.makeText(MailContentActivity.this, "普通邮件，无法打开原贴!", Toast.LENGTH_SHORT).show();
      }
    }else if(id == R.id.mail_content_copy)
      {
        String content;

        if (mPost != null) {
            content = mPost.getRawContent();

            final android.content.ClipboardManager clipboardManager =
                    (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            final android.content.ClipData clipData = android.content.ClipData.newPlainText("PostContent", content);
            clipboardManager.setPrimaryClip(clipData);

          Toast.makeText(MailContentActivity.this, "帖子内容已复制到剪贴板", Toast.LENGTH_SHORT).show();
        } else {
          Toast.makeText(MailContentActivity.this, "复制失败！", Toast.LENGTH_SHORT).show();
        }
      }
    return super.onOptionsItemSelected(item);
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.mail_content_menu, menu);
    return true;
  }
}
