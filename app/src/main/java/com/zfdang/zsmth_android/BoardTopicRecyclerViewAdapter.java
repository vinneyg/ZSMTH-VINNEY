package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.graphics.Color;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
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


      if (Settings.getInstance().isDiffReadTopic()) {
        if ((!SMTHApplication.ReadTopicLists.isEmpty()) && SMTHApplication.ReadTopicLists.contains(holder.mTopic.getTitle())) {
          //Log.d("VinneyY","read topics");

          holder.mTitle.setTextColor(Color.parseColor("#607D8B"));//R.color.colorPrimary
          holder.mAuthor.setTextColor(Color.parseColor("#607D8B"));//R.color.colorPrimary
          holder.mReplier.setTextColor(Color.parseColor("#607D8B"));
          holder.mReplyDate.setTextColor(Color.parseColor("#607D8B"));
          holder.mPublishDate.setTextColor(Color.parseColor("#607D8B"));
          holder.mStatusSummary.setTextColor(Color.parseColor("#607D8B"));
          /*
          if (Settings.getInstance().isNightMode()) {
            holder.mTitle.setTextColor(Color.GRAY);
            holder.mAuthor.setTextColor(Color.GRAY);
            holder.mReplier.setTextColor(Color.GRAY);
            holder.mReplyDate.setTextColor(Color.GRAY);
            holder.mPublishDate.setTextColor(Color.GRAY);
            holder.mStatusSummary.setTextColor(Color.GRAY);
            holder.mTitle.setTextColor(Color.parseColor("#607D8B"));//R.color.status_text_night
            holder.mAuthor.setTextColor(Color.parseColor("#607D8B"));//R.color.colorPrimary
            holder.mReplier.setTextColor(Color.parseColor("#607D8B"));
            holder.mReplyDate.setTextColor(Color.parseColor("#607D8B"));
            holder.mPublishDate.setTextColor(Color.parseColor("#607D8B"));
            holder.mStatusSummary.setTextColor(Color.parseColor("#607D8B"));
          } else {

            holder.mTitle.setTextColor(R.color.colorSecondaryText);
            holder.mAuthor.setTextColor(R.color.colorSecondaryText);
            holder.mReplier.setTextColor(R.color.colorSecondaryText);
            holder.mReplyDate.setTextColor(R.color.colorSecondaryText);
            holder.mPublishDate.setTextColor(R.color.colorSecondaryText);
            holder.mStatusSummary.setTextColor(R.color.colorSecondaryText);
            holder.mTitle.setTextColor(Color.parseColor("#000000")); //R.color.status_text_night
            holder.mAuthor.setTextColor(Color.parseColor("#000000"));//R.color.colorPrimary
            holder.mReplier.setTextColor(Color.parseColor("#000000"));
            holder.mReplyDate.setTextColor(Color.parseColor("#000000"));
            holder.mPublishDate.setTextColor(Color.parseColor("#000000"));
            holder.mStatusSummary.setTextColor(Color.parseColor("#000000"));
         }
         */
        }
        else
        {
          //First byte 0xFF ...... means transparent mode.
          if (Settings.getInstance().isNightMode()) {
            holder.mTitle.setTextColor(Color.parseColor("#ABC2DA"));//R.color.status_text_night
            holder.mAuthor.setTextColor(Color.parseColor("#607D8B"));//R.color.colorPrimary
            holder.mReplier.setTextColor(Color.parseColor("#607D8B"));
            holder.mReplyDate.setTextColor(Color.parseColor("#607D8B"));
            holder.mPublishDate.setTextColor(Color.parseColor("#607D8B"));
            holder.mStatusSummary.setTextColor(Color.parseColor("#607D8B"));
          }
          else{
            holder.mTitle.setTextColor(Color.parseColor("#000000")); //R.color.status_text_night
            holder.mAuthor.setTextColor(Color.parseColor("#607D8B"));//R.color.colorPrimary
            holder.mReplier.setTextColor(Color.parseColor("#607D8B"));
            holder.mReplyDate.setTextColor(Color.parseColor("#607D8B"));
            holder.mPublishDate.setTextColor(Color.parseColor("#607D8B"));
            holder.mStatusSummary.setTextColor(Color.parseColor("#607D8B"));
          }
        }
      }
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
            //Vinney
            if (Settings.getInstance().isDiffReadTopic()) {
              //Log.d("VinneyX+", Integer.toString(SMTHApplication.ReadTopicLists.size()));
              SMTHApplication.ReadTopicLists.add(holder.mTopic.getTitle());
              holder.mTitle.setTextColor(Color.parseColor("#607D8B"));//R.color.status_text_night
              holder.mAuthor.setTextColor(Color.parseColor("#607D8B"));//R.color.colorPrimary
              holder.mReplier.setTextColor(Color.parseColor("#607D8B"));
              holder.mReplyDate.setTextColor(Color.parseColor("#607D8B"));
              holder.mPublishDate.setTextColor(Color.parseColor("#607D8B"));
              holder.mStatusSummary.setTextColor(Color.parseColor("#607D8B"));

              /*
              if (Settings.getInstance().isNightMode()) {
                holder.mTitle.setTextColor(Color.GRAY);
                holder.mAuthor.setTextColor(Color.GRAY);
                holder.mReplier.setTextColor(Color.GRAY);
                holder.mReplyDate.setTextColor(Color.GRAY);
                holder.mPublishDate.setTextColor(Color.GRAY);
                holder.mStatusSummary.setTextColor(Color.GRAY);

                holder.mTitle.setTextColor(Color.parseColor("#607D8B"));//R.color.status_text_night
                holder.mAuthor.setTextColor(Color.parseColor("#607D8B"));//R.color.colorPrimary
                holder.mReplier.setTextColor(Color.parseColor("#607D8B"));
                holder.mReplyDate.setTextColor(Color.parseColor("#607D8B"));
                holder.mPublishDate.setTextColor(Color.parseColor("#607D8B"));
                holder.mStatusSummary.setTextColor(Color.parseColor("#607D8B"));

              } else {
                holder.mTitle.setTextColor(R.color.colorSecondaryText);
                holder.mAuthor.setTextColor(R.color.colorSecondaryText);
                holder.mReplier.setTextColor(R.color.colorSecondaryText);
                holder.mReplyDate.setTextColor(R.color.colorSecondaryText);
                holder.mPublishDate.setTextColor(R.color.colorSecondaryText);
                holder.mStatusSummary.setTextColor(R.color.colorSecondaryText);
                holder.mTitle.setTextColor(Color.parseColor("#607D8B"));//R.color.status_text_night
                holder.mAuthor.setTextColor(Color.parseColor("#607D8B"));//R.color.colorPrimary
                holder.mReplier.setTextColor(Color.parseColor("#607D8B"));
                holder.mReplyDate.setTextColor(Color.parseColor("#607D8B"));
                holder.mPublishDate.setTextColor(Color.parseColor("#607D8B"));
                holder.mStatusSummary.setTextColor(Color.parseColor("#607D8B"));
                holder.mTitle.setTextColor(Color.parseColor("#000000")); //R.color.status_text_night
                holder.mAuthor.setTextColor(Color.parseColor("#000000"));//R.color.colorPrimary
                holder.mReplier.setTextColor(Color.parseColor("#000000"));
                holder.mReplyDate.setTextColor(Color.parseColor("#000000"));
                holder.mPublishDate.setTextColor(Color.parseColor("#000000"));

              }

               */

            }
          }
        }
      });
    }

  @Override public int getItemCount() {
    return mTopics.size();
  }

  public class ViewHolder extends RecyclerView.ViewHolder {
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
      mPageIndicator = (TextView) view.findViewById(R.id.topic_page_indicator);
      mTitle = (TextView) view.findViewById(R.id.topic_title);
      mAuthor = (TextView) view.findViewById(R.id.topic_author);
      mReplier = (TextView) view.findViewById(R.id.topic_replier);
      mPublishDate = (TextView) view.findViewById(R.id.topic_public_date);
      mReplyDate = (TextView) view.findViewById(R.id.topic_reply_date);
      mStatusSummary = (TextView) view.findViewById(R.id.topic_status_summary);
      mAttach = (ImageView) view.findViewById(R.id.topic_status_attach);
      mPinned = (ImageView) view.findViewById(R.id.topic_status_pinned);
      mAuthorReplierRow = (RelativeLayout) view.findViewById(R.id.topic_author_replier_row);
      mStatusRow = (RelativeLayout) view.findViewById(R.id.topic_status_row);
    }

    @Override public String toString() {
      return mTopic.toString();
    }
  }
}
