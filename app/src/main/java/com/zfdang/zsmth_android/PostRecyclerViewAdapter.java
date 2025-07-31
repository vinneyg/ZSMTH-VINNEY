package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.fresco.WrapContentDraweeView;
import com.zfdang.zsmth_android.models.Attachment;
import com.zfdang.zsmth_android.models.ContentSegment;
import com.zfdang.zsmth_android.models.Post;
import java.util.ArrayList;
import java.util.List;

/**
 * used by HotPostFragment & BoardPostFragment
 */
public class PostRecyclerViewAdapter extends RecyclerView.Adapter<PostRecyclerViewAdapter.ViewHolder> {

  private final List<Post> mPosts;
  private final Activity mListener;

  public interface OnBtnMoreClickListener {
    void onItemBtnMoreClicked(int position, View view);
  }

  public interface OnBtnReplyClickListener {
    void onItemBtnReplyClicked(int position, View view);
  }

  private final OnBtnMoreClickListener mBtnMoreClickListener;

  private final OnBtnReplyClickListener mBtnReplyClickListener;


  public PostRecyclerViewAdapter(List<Post> posts, Activity listener,OnBtnMoreClickListener replyListener,OnBtnReplyClickListener moreListener) {
    mPosts = posts;
    mListener = listener;
    mBtnMoreClickListener =replyListener;
    mBtnReplyClickListener = moreListener;
  }


  @NonNull
  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.post_item, parent, false);
    return new ViewHolder(view);
  }

  //    MailContentActivity.inflateContentViewGroup is using the same logic, please make sure they are consistent
  @SuppressLint("SetTextI18n")
  public void inflateContentViewGroup(ViewGroup viewGroup, final Post post) {
    // remove all child view in viewgroup
    viewGroup.removeAllViews();

    List<ContentSegment> contents = post.getContentSegments();
    if (contents == null) return;
    final LayoutInflater inflater = mListener.getLayoutInflater();
    if (!contents.isEmpty()) {
      // there are multiple segments, add the first contentView first
      // contentView is always available, we don't have to inflate it again
      ContentSegment content = contents.get(0);

      if (content.getSpanned().toString().contains("mp4") && content.getSpanned().toString().contains("附件")
              && !(content.getSpanned().toString().contains(": 附件"))){ //Video
        List<Attachment> attaches = post.getAttachVideoFiles();
        for (int i = 0; i < attaches.size(); i++) {
          Attachment attach = attaches.get(i);

          TextView tv = (TextView) inflater.inflate(R.layout.post_item_content, viewGroup, false);
          tv.setTextIsSelectable(true);

          String[] sUrl = new String[6];
          String tempStr = content.getSpanned().toString().split(".mp4")[i];
          sUrl[i] = "附件" + tempStr.split("附件")[1] + ".mp4";
          if (i == 0) {
            tv.setText(tempStr + ".mp4");
          } else {
            tv.setText(sUrl[i]);
          }

          SpannableString spannableString = new SpannableString(tv.getText());
          ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
              Uri uri = Uri.parse(attach.getOriginalVideoSource());
              Intent intent = new Intent(Intent.ACTION_VIEW);
              intent.setDataAndType(uri, "video/*");
              intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              mListener.startActivity(Intent.createChooser(intent, "选择视频播放器"));
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
              super.updateDrawState(ds);
              ds.setUnderlineText(false); // 去掉下划线
            }
          };
          spannableString.setSpan(clickableSpan, 0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
          tv.setText(spannableString);
          tv.setMovementMethod(LinkMovementMethod.getInstance());
          // Add the text view to the parent layout
          viewGroup.addView(tv);
        }
      } else {
        TextView tv = (TextView) inflater.inflate(R.layout.post_item_content, viewGroup, false);
        tv.setTextIsSelectable(true);
        tv.setText(content.getSpanned());
        Linkify.addLinks(tv, Linkify.ALL);

        // Add the text view to the parent layout
        viewGroup.addView(tv);
      }
    }

    // http://stackoverflow.com/questions/13438473/clicking-html-link-in-textview-fires-weird-androidruntimeexception

    for (int i = 1; i < contents.size(); i++) {
      ContentSegment content = contents.get(i);

      if (content.getType() == ContentSegment.SEGMENT_IMAGE) {
        // Log.d("CreateView", "Image: " + content.getUrl());

        // Add the text layout to the parent layout
        WrapContentDraweeView image = (WrapContentDraweeView) inflater.inflate(R.layout.post_item_imageview, viewGroup, false);
        image.setImageFromStringURL(content.getUrl());

        // set onclicklistener
        image.setTag(R.id.image_tag, content.getImgIndex());
        image.setOnClickListener(v -> {
          int position = (int) v.getTag(R.id.image_tag);

          Intent intent = new Intent(mListener, FSImageViewerActivity.class);

          ArrayList<String> urls = new ArrayList<>();
          List<Attachment> attaches = post.getAttachFiles();
          for (Attachment attach : attaches) {
            // load original image in FS image viewer
            urls.add(attach.getOriginalImageSource());
          }

          intent.putStringArrayListExtra(SMTHApplication.ATTACHMENT_URLS, urls);
          intent.putExtra(SMTHApplication.ATTACHMENT_CURRENT_POS, position);
          mListener.startActivity(intent);
        });

        // Add the text view to the parent layout
        viewGroup.addView(image);
      } else if (content.getType() == ContentSegment.SEGMENT_TEXT) {
        if (content.getSpanned().toString().contains("mp4") && content.getSpanned().toString().contains("附件")) { //Video
          List<Attachment> attaches = post.getAttachVideoFiles();
          for (int j = 0; j < attaches.size(); j++) {
            Attachment attach = attaches.get(j);

            TextView tv = (TextView) inflater.inflate(R.layout.post_item_content, viewGroup, false);
            tv.setTextIsSelectable(true);

            String[] sUrl = new String[6];
            String tempStr = content.getSpanned().toString().split(".mp4")[j];
            sUrl[j] = "附件" + tempStr.split("附件")[1] + ".mp4";
            if (j == 0) {
              tv.setText(tempStr + ".mp4");
            } else {
              tv.setText(sUrl[j]);
            }

            SpannableString spannableString = new SpannableString(tv.getText());
            ClickableSpan clickableSpan = new ClickableSpan() {
              @Override
              public void onClick(@NonNull View widget) {
                Uri uri = Uri.parse(attach.getOriginalVideoSource());
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "video/*");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mListener.startActivity(Intent.createChooser(intent, "选择视频播放器"));
              }

              @Override
              public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false); // 去掉下划线
              }
            };
            spannableString.setSpan(clickableSpan, 0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            tv.setText(spannableString);
            tv.setMovementMethod(LinkMovementMethod.getInstance());

            // Add the text view to the parent layout
            viewGroup.addView(tv);
          }
        } else {
          TextView tv = (TextView) inflater.inflate(R.layout.post_item_content, viewGroup, false);
          tv.setTextIsSelectable(true);
          tv.setText(content.getSpanned());
          Linkify.addLinks(tv, Linkify.ALL);

          // Add the text view to the parent layout
          viewGroup.addView(tv);
        }
      }
    }
  }

  @Override public void onBindViewHolder(final ViewHolder holder, final int position) {
    holder.mPost = mPosts.get(position);
    Post post = holder.mPost;

    holder.mPostAuthor.setText(post.getAuthor());
    holder.mPostPublishDate.setText(post.getFormatedDate());
    holder.mPostIndex.setText(post.getPosition());

    holder.mPostAuthor.setOnClickListener(v -> {
      if(Settings.getInstance().isSetIdCheck()) {
        Intent intent = new Intent(v.getContext(), QueryUserActivity.class);
        intent.putExtra(SMTHApplication.QUERY_USER_INFO, post.getRawAuthor());
        v.getContext().startActivity(intent);
        if (v.getContext() instanceof Activity) {
          ((Activity) v.getContext()).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
      }
    });

    holder.mBtnReply.setOnClickListener(v -> {
      if (mBtnReplyClickListener!= null) {
        mBtnReplyClickListener.onItemBtnReplyClicked(position, v);
      }
    });

    holder.mBtnMore.setOnClickListener(v -> {
      if (mBtnMoreClickListener!= null) {
        mBtnMoreClickListener.onItemBtnMoreClicked(position, v);
      }
    });

    inflateContentViewGroup(holder.mViewGroup, post);
    // http://stackoverflow.com/questions/4415528/how-to-pass-the-onclick-event-to-its-parent-on-android
    // http://stackoverflow.com/questions/24885223/why-doesnt-recyclerview-have-onitemclicklistener-and-how-recyclerview-is-dif

    holder.mView.setOnClickListener(v -> {
    });
    holder.mView.setOnTouchListener(new View.OnTouchListener() {
      @SuppressLint("ClickableViewAccessibility")
      @Override public boolean onTouch(View v, MotionEvent event) {
        if (mListener != null && mListener instanceof View.OnTouchListener) {
          return ((View.OnTouchListener) mListener).onTouch(v, event);
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
          v.performClick(); // 必须调用 performClick()
        }
        return false;
      }
    });

  }

  @Override public int getItemCount() {
    return mPosts.size();
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    public final View mView;
    public final TextView mPostAuthor;
    public final TextView mPostIndex;
    public final TextView mPostPublishDate;
    private final LinearLayout mViewGroup;
    public final TextView mPostContent;
    public Post mPost;

    public final Button mBtnReply;
    public final Button mBtnMore;

    public ViewHolder(View view) {
      super(view);
      mView = view;
      mPostAuthor = view.findViewById(R.id.post_author);
      mPostIndex = view.findViewById(R.id.post_index);
      mPostPublishDate = view.findViewById(R.id.post_publish_date);
      mViewGroup = view.findViewById(R.id.post_content_holder);
      mPostContent = view.findViewById(R.id.post_content);
      mBtnReply = view.findViewById(R.id.btn_post_reply);
      mBtnMore= view.findViewById(R.id.btn_post_more);
    }

    @NonNull
    @Override public String toString() {
      return mPost.toString();
    }
  }
}
