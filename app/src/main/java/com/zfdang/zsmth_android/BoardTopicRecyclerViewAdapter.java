package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.listeners.OnTopicFragmentInteractionListener;
import com.zfdang.zsmth_android.models.Topic;
import java.util.List;

/**
 * used by HotTopicFragment & BoardTopicFragment
 */
public class BoardTopicRecyclerViewAdapter extends RecyclerView.Adapter<BoardTopicRecyclerViewAdapter.ViewHolder> {

  private final List<Topic> mTopics;
  private final OnTopicFragmentInteractionListener mListener;

  public BoardTopicRecyclerViewAdapter(List<Topic> items, OnTopicFragmentInteractionListener listener) {
    mTopics = items;
    mListener = listener;
  }

  @NonNull
  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.board_topic_item, parent, false);
    return new ViewHolder(view);
  }

  @SuppressLint("ResourceAsColor")
  @Override public void onBindViewHolder(final ViewHolder holder, int position) {
    holder.mTopic = mTopics.get(position);
    Topic topic = holder.mTopic;

    if (topic.isCategory) {
      holder.mPageIndicator.setVisibility(View.VISIBLE);
      holder.mPageIndicator.setText(topic.getCategory());

      holder.mTitle.setVisibility(View.GONE);
      holder.mAuthorReplierRow.setVisibility(View.GONE);
      holder.mStatusRow.setVisibility(View.GONE);
    } else {
      holder.mPageIndicator.setVisibility(View.GONE);
      holder.mTitle.setVisibility(View.VISIBLE);

      holder.mAuthorReplierRow.setVisibility(View.VISIBLE);
      holder.mStatusRow.setVisibility(View.VISIBLE);

      /*
      if (Settings.getInstance().isDiffReadTopic()) {
        //Common Parts
        holder.mAuthor.setTextColor(Color.parseColor("#607D8B"));//R.color.colorPrimary
        holder.mReplier.setTextColor(Color.parseColor("#607D8B"));
        holder.mReplyDate.setTextColor(Color.parseColor("#607D8B"));
        holder.mPublishDate.setTextColor(Color.parseColor("#607D8B"));
        holder.mStatusSummary.setTextColor(Color.parseColor("#607D8B"));

        if ((!SMTHApplication.ReadTopicLists.isEmpty()) && SMTHApplication.ReadTopicLists.contains(holder.mTopic.getTitle())) {
          holder.mTitle.setTextColor(Color.parseColor("#607D8B"));//R.color.colorPrimary
        }

        else
        {

          //First byte 0xFF ...... means transparent mode.
          if (Settings.getInstance().isNightMode()) {
            holder.mTitle.setTextColor(Color.parseColor("#ABC2DA"));//R.color.status_text_night
          }
          else{
            holder.mTitle.setTextColor(Color.parseColor("#000000")); //R.color.status_text_night
          }
        }

      }
      */
      //
      holder.mTitle.setText(topic.getTitle());
      holder.mAuthor.setText(topic.getAuthor());
      holder.mReplier.setText(topic.getReplier());
      holder.mPublishDate.setText(topic.getPublishDate());
      holder.mReplyDate.setText(topic.getReplyDate());
      holder.mStatusSummary.setText(topic.getStatusSummary());

      if (topic.hasAttach()) {
        holder.mAttach.setVisibility(View.VISIBLE);
      } else {
        holder.mAttach.setVisibility(View.INVISIBLE);
      }

      if (topic.isSticky) {
        holder.mPinned.setVisibility(View.VISIBLE);
      } else {
        holder.mPinned.setVisibility(View.INVISIBLE);
      }
    }

    holder.mView.setOnClickListener(new View.OnClickListener() {
      @SuppressLint("ResourceAsColor")
      @Override
      public void onClick(View v) {
        if (null != mListener) {
          // Notify the active callbacks interface (the activity, if the
          // fragment is attached to one) that an item has been selected.
          mListener.onTopicFragmentInteraction(holder.mTopic);
          if (Settings.getInstance().isDiffReadTopic()) {
            SMTHApplication.ReadTopicLists.add(holder.mTopic.getTitle());
            holder.mTitle.setTextColor(Color.parseColor("#607D8B"));//R.color.status_text_night
            holder.mAuthor.setTextColor(Color.parseColor("#607D8B"));//R.color.colorPrimary
            holder.mReplier.setTextColor(Color.parseColor("#607D8B"));
            holder.mReplyDate.setTextColor(Color.parseColor("#607D8B"));
            holder.mPublishDate.setTextColor(Color.parseColor("#607D8B"));
            holder.mStatusSummary.setTextColor(Color.parseColor("#607D8B"));
          }
        }
      }
    });
  }

  @Override public int getItemCount() {
    return mTopics.size();
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    public final View mView;
    public final TextView mPageIndicator;
    public final TextView mTitle;
    public final TextView mAuthor;
    public final TextView mReplier;
    public final TextView mPublishDate;
    public final TextView mReplyDate;
    public final TextView mStatusSummary;
    public final ImageView mAttach;
    public final ImageView mPinned;

    public final RelativeLayout mAuthorReplierRow;
    public final RelativeLayout mStatusRow;

    public Topic mTopic;

    public ViewHolder(View view) {
      super(view);
      mView = view;
      mPageIndicator = view.findViewById(R.id.topic_page_indicator);
      mTitle = view.findViewById(R.id.topic_title);
      mAuthor = view.findViewById(R.id.topic_author);
      mReplier =  view.findViewById(R.id.topic_replier);
      mPublishDate =  view.findViewById(R.id.topic_public_date);
      mReplyDate =  view.findViewById(R.id.topic_reply_date);
      mStatusSummary =  view.findViewById(R.id.topic_status_summary);
      mAttach =  view.findViewById(R.id.topic_status_attach);
      mPinned =  view.findViewById(R.id.topic_status_pinned);
      mAuthorReplierRow = view.findViewById(R.id.topic_author_replier_row);
      mStatusRow = view.findViewById(R.id.topic_status_row);
    }

    @NonNull
    @Override public String toString() {
      return mTopic.toString();
    }
  }
}
