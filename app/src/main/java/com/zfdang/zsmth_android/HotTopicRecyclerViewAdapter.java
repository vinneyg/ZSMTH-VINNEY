package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.listeners.OnTopicFragmentInteractionListener;
import com.zfdang.zsmth_android.models.Topic;
import java.util.List;

/**
 * used by HotTopicFragment & BoardTopicFragment
 */
public class HotTopicRecyclerViewAdapter extends RecyclerView.Adapter<HotTopicRecyclerViewAdapter.ViewHolder> {

  private final List<Topic> mValues;
  private final OnTopicFragmentInteractionListener mListener;

  public HotTopicRecyclerViewAdapter(List<Topic> items, OnTopicFragmentInteractionListener listener) {
    mValues = items;
    mListener = listener;
  }

  @NonNull
  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.hot_topic_item, parent, false);
    return new ViewHolder(view);
  }

  @SuppressLint({"ResourceAsColor", "ResourceType"})
  @Override public void onBindViewHolder(final ViewHolder holder, int position) {
    holder.mItem = mValues.get(position);

    if (holder.mItem.isCategory) {
      holder.mSeperator.setVisibility(View.VISIBLE);
      holder.mTopicTitle.setVisibility(View.GONE);
      holder.mBoardName.setVisibility(View.GONE);
      holder.mReplyCount.setVisibility(View.GONE);
      holder.mSeperator.setText(holder.mItem.getCategory());
    } else {
      holder.mSeperator.setVisibility(View.GONE);
      holder.mTopicTitle.setVisibility(View.VISIBLE);
      holder.mBoardName.setVisibility(View.VISIBLE);
      holder.mReplyCount.setVisibility(View.VISIBLE);

      holder.mTopicTitle.setText(holder.mItem.getTitle());
      holder.mBoardName.setText(holder.mItem.getBoardName());
      holder.mReplyCount.setText(holder.mItem.getTotalPostNoAsStr());

      //Vinney
      if(Settings.getInstance().isDiffReadTopic()) {
        if ((!SMTHApplication.ReadTopicLists.isEmpty()) && SMTHApplication.ReadTopicLists.contains(holder.mItem.getTitle())) {
         //Log.d("Vinney",holder.mItem.getTopicID());
          holder.mTopicTitle.setTextColor(Color.parseColor("#607D8B")); //R.color.colorPrimary
          holder.mBoardName.setTextColor(Color.parseColor("#607D8B")); //R.color.colorPrimary
          holder.mReplyCount.setTextColor(Color.parseColor("#607D8B"));//R.color.colorPrimary

        }
        else
        {
          //First byte 0xFF ...... means transparent mode.
          if (Settings.getInstance().isNightMode()) {
           //holder.mTopicTitle.setTextColor(0xFFABC2DA);
            holder.mTopicTitle.setTextColor(Color.parseColor("#ABC2DA")); //R.color.status_text_night
            holder.mBoardName.setTextColor(Color.parseColor("#607D8B")); //R.color.colorPrimary
            holder.mReplyCount.setTextColor(Color.parseColor("#607D8B"));//R.color.colorPrimary
          }
          else
          {
            holder.mTopicTitle.setTextColor(Color.parseColor("#000000")); //R.color.status_text_night
            holder.mBoardName.setTextColor(Color.parseColor("#607D8B")); //R.color.colorPrimary
            holder.mReplyCount.setTextColor(Color.parseColor("#607D8B"));//R.color.colorPrimary
          }
        }
      }
    }

    holder.mView.setOnClickListener(new View.OnClickListener() {
      @SuppressLint("ResourceAsColor")
      @Override public void onClick(View v) {
        if (null != mListener) {
          // Notify the active callbacks interface (the activity, if the
          // fragment is attached to one) that an item has been selected.
          SMTHApplication.ReadRec=false;
          SMTHApplication.ReadPostFirst=null;
          mListener.onTopicFragmentInteraction(holder.mItem);

          //Vinney
          if (Settings.getInstance().isDiffReadTopic()) {
            SMTHApplication.ReadTopicLists.add(holder.mItem.getTitle());
            holder.mTopicTitle.setTextColor(Color.parseColor("#607D8B")); //R.color.colorPrimary
            holder.mBoardName.setTextColor(Color.parseColor("#607D8B")); //R.color.colorPrimary
            holder.mReplyCount.setTextColor(Color.parseColor("#607D8B"));//R.color.colorPrimary
          }
        }
      }
    });
  }

  @Override public int getItemCount() {
    return mValues.size();
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    public final View mView;
    public final TextView mSeperator;
    public final TextView mBoardName;
    public final TextView mTopicTitle;
    public final TextView mReplyCount;
    public Topic mItem;

    public ViewHolder(View view) {
      super(view);
      mView = view;
      mSeperator = (TextView) view.findViewById(R.id.category_name);
      mBoardName = (TextView) view.findViewById(R.id.board_name);
      mTopicTitle = (TextView) view.findViewById(R.id.topic_title);
      mReplyCount = (TextView) view.findViewById(R.id.topic_reply_count);
    }

    @NonNull
    @Override public String toString() {
      return mTopicTitle.getText().toString();
    }
  }
}
