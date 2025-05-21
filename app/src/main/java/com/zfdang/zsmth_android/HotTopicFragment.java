package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.helpers.RecyclerViewUtil;
import com.zfdang.zsmth_android.listeners.OnTopicFragmentInteractionListener;
import com.zfdang.zsmth_android.listeners.OnVolumeUpDownListener;
import com.zfdang.zsmth_android.models.Board;
import com.zfdang.zsmth_android.models.BoardListContent;
import com.zfdang.zsmth_android.models.Topic;
import com.zfdang.zsmth_android.models.TopicListContent;
import com.zfdang.zsmth_android.newsmth.SMTHHelper;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import okhttp3.ResponseBody;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnTopicFragmentInteractionListener}
 * interface.
 */
public class HotTopicFragment extends Fragment implements OnVolumeUpDownListener {

  private final String TAG = "HotTopicFragment";

  private OnTopicFragmentInteractionListener mListener;

  private RecyclerView mRecyclerView = null;
  private SmartRefreshLayout mRefreshLayout = null;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public HotTopicFragment() {
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_hot_topic, container, false);

    // http://sapandiwakar.in/pull-to-refresh-for-android-recyclerview-or-any-other-vertically-scrolling-view/
    // pull to refresh for android recyclerview
    mRefreshLayout = (SmartRefreshLayout) rootView;
    mRefreshLayout.setEnableLoadMore(false);
    mRefreshLayout.setOnRefreshListener(refreshLayout -> RefreshGuidanceFromWWW());

    // http://blog.csdn.net/lmj623565791/article/details/45059587
    // 你想要控制Item间的间隔（可绘制），请通过ItemDecoration
    // 你想要控制Item增删的动画，请通过ItemAnimator
    // 你想要控制点击、长按事件，请自己写
    // item被按下的时候的highlight,这个是通过guidance item的backgroun属性来实现的 (android:background="@drawable/recyclerview_item_bg")
    mRecyclerView = rootView.findViewById(R.id.guidance_recycler_view);
    // Set the adapter
    if (mRecyclerView != null) {
      mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager.VERTICAL, 0));
      Context context = rootView.getContext();
      mRecyclerView.setLayoutManager(new WrapContentLinearLayoutManager(context));
      mRecyclerView.setItemAnimator(new DefaultItemAnimator());
      mRecyclerView.setAdapter(new HotTopicRecyclerViewAdapter(TopicListContent.HOT_TOPICS, mListener));
      mRecyclerView.setItemViewCacheSize(40);


      mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(@androidx.annotation.NonNull RecyclerView recyclerView, int newState) {
          super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled (@androidx.annotation.NonNull RecyclerView recyclerView, int dx , int dy){
          super.onScrolled(recyclerView,dx,dy);
          // mRecyclerView.getAdapter().notifyDataSetChanged();
        }

      });

    }


    requireActivity().setTitle(SMTHApplication.App_Title_Prefix + "首页");

    if (TopicListContent.HOT_TOPICS.isEmpty()) {
      RefreshGuidance();
    }

    initItemHelper();

    return rootView;
  }

  private void initItemHelper() {
    //0则不执行拖动或者滑动
    ItemTouchHelper.Callback mCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT ) {

      @Override
      public boolean onMove(@androidx.annotation.NonNull RecyclerView recyclerView, @androidx.annotation.NonNull RecyclerView.ViewHolder viewHolder, @androidx.annotation.NonNull RecyclerView.ViewHolder target) {
        return false;
      }
      @Override
      public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
        //int position = viewHolder.getAdapterPosition();
        int position = viewHolder.getBindingAdapterPosition();

        Topic topic = TopicListContent.HOT_TOPICS.get(position);
        TopicListContent.HOT_TOPICS.remove(position);
        TopicListContent.HOT_TOPICS.add(position,topic);
        Objects.requireNonNull(mRecyclerView.getAdapter()).notifyItemRemoved(position);
        Objects.requireNonNull(mRecyclerView.getAdapter()).notifyItemInserted(position);
        MainActivity mainActivity = (MainActivity) getActivity();

        if (direction == ItemTouchHelper.RIGHT) {
          //RefreshGuidance();
          if (mainActivity != null) {
            mainActivity.onHandleBackPressed();
          }

        }else  if (direction == ItemTouchHelper.LEFT) {
          //RefreshGuidance();
          if (mainActivity != null) {
            mainActivity.handleRightSwipe();
          }
        }
      }
    };
    ItemTouchHelper itemTouchHelper = new ItemTouchHelper(mCallback);
    itemTouchHelper.attachToRecyclerView(mRecyclerView);
  }
  @Override
  public void onViewCreated(@androidx.annotation.NonNull @NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    requireActivity().addMenuProvider(new MenuProvider() {
      @Override
      public void onCreateMenu(@androidx.annotation.NonNull @NonNull Menu menu, @androidx.annotation.NonNull @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.hot_topic_menu, menu);
      }

      @Override
      public boolean onMenuItemSelected(@androidx.annotation.NonNull @NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.main_action_refresh) {
          RefreshGuidance();
          return true;
        }
        return false;
      }
    }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
  }

  public void showLoadingHints() {
    MainActivity activity = (MainActivity) getActivity();
    if (activity != null) activity.showProgress("获取首页信息...");
  }

  public void clearLoadingHints() {
    // disable progress bar
    MainActivity activity = (MainActivity) getActivity();
    if (activity != null) {
      activity.dismissProgress();
    }

  }

  public void RefreshGuidance() {
    // called by onCreate & refresh menu item
    showLoadingHints();
    RefreshGuidanceFromWWW();
  }

  public void RefreshGuidanceFromWWW() {
    final SMTHHelper helper = SMTHHelper.getInstance();
    final List<Topic> newTopics = new ArrayList<>();

    helper.wService.getAllHotTopics().flatMap((Function<ResponseBody, ObservableSource<Topic>>) responseBody -> {
      try {
        String response = responseBody.string();
        List<Topic> results = SMTHHelper.ParseHotTopicsFromWWW(response);
        return Observable.fromIterable(results);
      } catch (Exception e) {
        Log.d(TAG, Log.getStackTraceString(e));
      }
      return null;
    }).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Topic>() {
      @SuppressLint("NotifyDataSetChanged")
      @Override public void onSubscribe(@NonNull Disposable disposable) {
        // clearHotTopics current hot topics
        TopicListContent.clearHotTopics();
        Objects.requireNonNull(mRecyclerView.getAdapter()).notifyDataSetChanged();
      }

      @Override public void onNext(@NonNull Topic topic) {
        //                        Log.d(TAG, topic.toString());
        //TopicListContent.addHotTopic(topic);
        //Objects.requireNonNull(mRecyclerView.getAdapter()).notifyItemInserted(TopicListContent.HOT_TOPICS.size() - 1);
        newTopics.add(topic);
      }

      @Override public void onError(@NonNull Throwable e) {
        clearLoadingHints();
        if (mRefreshLayout != null) {
          mRefreshLayout.finishRefresh(false);
        }
        Toast.makeText(SMTHApplication.getAppContext(), "获取首页热帖失败!\n" + e.toString(), Toast.LENGTH_SHORT).show();
      }

      @Override public void onComplete() {
        Topic topic = new Topic("结束");
        //TopicListContent.addHotTopic(topic);

        newTopics.add(topic);

        if (mRefreshLayout != null) {
          mRefreshLayout.finishRefresh(true);
        }

        for (Topic topicIn : newTopics) {
          TopicListContent.addHotTopic(topicIn);
        }
        Objects.requireNonNull(mRecyclerView.getAdapter()).notifyItemInserted(TopicListContent.HOT_TOPICS.size() - 1);
        clearLoadingHints();
      }
    });
  }

  // http://stackoverflow.com/questions/32604552/onattach-not-called-in-fragment
  // If you run your application on a device with API 23 (marshmallow) then onAttach(Context) will be called.
  // On all previous Android Versions onAttach(Activity) will be called.
  @Override public void onAttach(@androidx.annotation.NonNull Context context) {
    super.onAttach(context);
    if (context instanceof OnTopicFragmentInteractionListener) {
      mListener = (OnTopicFragmentInteractionListener) context;
    } else {
      throw new RuntimeException(context + " must implement OnTopicFragmentInteractionListener");
    }
  }

  @Override public void onDetach() {
    super.onDetach();
    mListener = null;
  }

  @Override public boolean onVolumeUpDown(int keyCode) {
    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
      RecyclerViewUtil.ScrollRecyclerViewByKey(mRecyclerView, keyCode);
      requireActivity().findViewById(R.id.bv_bottomNavigation).setVisibility(View.VISIBLE);
    } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
      RecyclerViewUtil.ScrollRecyclerViewByKey(mRecyclerView, keyCode);
      requireActivity().findViewById(R.id.bv_bottomNavigation).setVisibility(View.GONE);
    }
    return true;
  }
}
