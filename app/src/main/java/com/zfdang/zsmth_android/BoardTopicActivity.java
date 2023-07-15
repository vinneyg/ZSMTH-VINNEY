package com.zfdang.zsmth_android;

import static com.zfdang.zsmth_android.LoginActivity.LOGIN_ACTIVITY_REQUEST_CODE;

import android.annotation.SuppressLint;
import android.content.Intent;

import android.os.Bundle;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;


import com.jude.swipbackhelper.SwipeBackHelper;
import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.fresco.WrapContentDraweeView;
import com.zfdang.zsmth_android.helpers.RecyclerViewUtil;
import com.zfdang.zsmth_android.listeners.EndlessRecyclerOnScrollListener;
import com.zfdang.zsmth_android.listeners.OnTopicFragmentInteractionListener;
import com.zfdang.zsmth_android.models.Board;
import com.zfdang.zsmth_android.models.ComposePostContext;
import com.zfdang.zsmth_android.models.Topic;
import com.zfdang.zsmth_android.models.TopicListContent;
import com.zfdang.zsmth_android.newsmth.AjaxResponse;
import com.zfdang.zsmth_android.newsmth.SMTHHelper;

import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import okhttp3.ResponseBody;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

//import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;

/**
 * An activity representing a list of Topics. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link PostListActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class BoardTopicActivity extends SMTHBaseActivity
    implements OnTopicFragmentInteractionListener, SwipeRefreshLayout.OnRefreshListener, PopupSearchWindow.SearchInterface {

  /**
   * Whether or not the activity is in two-pane mode, i.e. running on a tablet
   * device.
   */

  private final String TAG = "BoardTopicActivity";

  private Board mBoard = null;

  private int mCurrentPageNo = 1;
  private final int LOAD_MORE_THRESHOLD = 1;

  private SwipeRefreshLayout mSwipeRefreshLayout = null;
  private EndlessRecyclerOnScrollListener mScrollListener = null;
  private RecyclerView mRecyclerView = null;

  private Settings mSetting;

  private boolean isSearchMode = false;

  private static final int MAXSIZE = 100;
  private static Hashtable MapHash = new Hashtable(MAXSIZE);

  @Override protected void onDestroy() {
    super.onDestroy();
    SwipeBackHelper.onDestroy(this);
  }

  @Override protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    SwipeBackHelper.onPostCreate(this);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if(requestCode == ComposePostActivity.COMPOSE_ACTIVITY_REQUEST_CODE) {
      // returned from compose activity
      // ideally, we should also check the resultCode
      RefreshBoardTopicFromPageOne();
    }
    /*
    else if (requestCode == MainActivity.LOGIN_ACTIVITY_REQUEST_CODE) {
      if (resultCode == RESULT_OK) {

        SMTHApplication.activeUser.setId(Settings.getInstance().getUsername());
        Settings.getInstance().setUserOnline(true);
        UpdateNavigationViewHeaderNew();
      }
    }
    */
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override public void onBackPressed() {
    if (isSearchMode) {
      onRefresh();
    }
    super.onBackPressed();
    //mRecyclerView.getAdapter().notifyDataSetChanged();
  }

  private void UpdateNavigationViewHeaderNew() {
    getWindow().invalidatePanelMenu(Window.FEATURE_OPTIONS_PANEL);
    LayoutInflater factory = LayoutInflater.from(BoardTopicActivity.this);

    //View layout = factory.inflate(R.layout.activity_main, null);
    View layout = factory.inflate(R.layout.nav_header_main, null);

    TextView mUsername = (TextView) layout.findViewById(R.id.nav_user_name);
    WrapContentDraweeView mAvatar = (WrapContentDraweeView) layout.findViewById(R.id.nav_user_avatar);

    if (SMTHApplication.isValidUser()) {
      // update user to login user
      mUsername.setText(SMTHApplication.activeUser.getId());
      String faceURL = SMTHApplication.activeUser.getFace_url();
      if (faceURL != null) {
        mAvatar.setImageFromStringURL(faceURL);
      }
    } else {
      // only user to guest
      mUsername.setText(getString(R.string.nav_header_click_to_login));
      mAvatar.setImageResource(R.drawable.ic_person_black_48dp);
    }

  }


  private static BoardTopicActivity mActivity1 = null;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    SwipeBackHelper.onCreate(this);
    mActivity1  = this;

    setContentView(R.layout.activity_board_topic);

    Toolbar toolbar = (Toolbar) findViewById(R.id.board_topic_toolbar);
    setSupportActionBar(toolbar);
    assert toolbar != null;
    toolbar.setTitle(getTitle());

    mSetting = Settings.getInstance();

    // enable pull down to refresh
    mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
    if (mSwipeRefreshLayout == null) throw new AssertionError();
    mSwipeRefreshLayout.setOnRefreshListener(this);

    mRecyclerView = (RecyclerView) findViewById(R.id.board_topic_list);
    assert mRecyclerView != null;
    mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    mRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL, 0));
    LinearLayoutManager linearLayoutManager = new WrapContentLinearLayoutManager(this);
    mRecyclerView.setLayoutManager(linearLayoutManager);
    mRecyclerView.setAdapter(new BoardTopicRecyclerViewAdapter(TopicListContent.BOARD_TOPICS, this));

    mRecyclerView.setItemViewCacheSize(40);

    // enable endless loading
    mScrollListener = new EndlessRecyclerOnScrollListener(linearLayoutManager) {
      @Override public void onLoadMore(int current_page) {
        // do something...
        loadMoreItems();
      }


      @Override
      public void onScrollStateChanged(@androidx.annotation.NonNull RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
        /*
       if(newState == RecyclerView.SCROLL_STATE_IDLE)
        mRecyclerView.getAdapter().notifyDataSetChanged();

         */
      }
      /*
      @Override
      public void onScrolled (RecyclerView recyclerView,int dx , int dy){
        super.onScrolled(recyclerView,dx,dy);
      }
       */

    };
    mRecyclerView.addOnScrollListener(mScrollListener);

    // Show the Up button in the action bar.
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    // get Board information from launcher
    Intent intent = getIntent();
    Board board = intent.getParcelableExtra(SMTHApplication.BOARD_OBJECT);
    assert board != null;
    if (mBoard == null || !mBoard.getBoardEngName().equals(board.getBoardEngName())) {
      mBoard = board;
      TopicListContent.clearBoardTopics();
      mCurrentPageNo = 1;

      if (!mBoard.getBoardChsName().equals(SMTHApplication.ReadBoard1) && !mBoard.getBoardChsName().equals(SMTHApplication.ReadBoard2)
              && !mBoard.getBoardChsName().equals(SMTHApplication.ReadBoard3)) {
        switch (SMTHApplication.ReadBoardCount % 3) {
          case 0:
            SMTHApplication.ReadBoard1 = mBoard.getBoardChsName();
            SMTHApplication.ReadBoardEng1 = mBoard.getBoardEngName();
            break;
          case 1:
            SMTHApplication.ReadBoard2 = mBoard.getBoardChsName();
            SMTHApplication.ReadBoardEng2 = mBoard.getBoardEngName();
            break;
          case 2:
            SMTHApplication.ReadBoard3 = mBoard.getBoardChsName();
            SMTHApplication.ReadBoardEng3 = mBoard.getBoardEngName();
            break;
        }
        SMTHApplication.ReadBoardCount++;
      }
    }

    updateTitle();

    if (TopicListContent.BOARD_TOPICS.size() == 0) {
      // only load boards on the first time
      RefreshBoardTopicsWithoutClear();
    }
  }


  public static BoardTopicActivity getInstance () {
    if (mActivity1 != null) {
      return mActivity1;
    }
    return null;
  }

  public void updateTitle() {
    String title = mBoard.getBoardChsName();
    setTitle(title + " - 主题列表");
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == android.R.id.home) {
      onBackPressed();
      return true;
    } else if (id == R.id.board_topic_action_sticky) {
      mSetting.toggleShowSticky();
      this.RefreshBoardTopicFromPageOne();
    } else if (id == R.id.board_topic_action_refresh) {
      this.RefreshBoardTopicFromPageOne();
    } else if (id == R.id.board_topic_action_newpost) {
      ComposePostContext postContext = new ComposePostContext();
      postContext.setBoardEngName(mBoard.getBoardEngName());
      postContext.setComposingMode(ComposePostContext.MODE_NEW_POST);

      Intent intent = new Intent(this, ComposePostActivity.class);
      intent.putExtra(SMTHApplication.COMPOSE_POST_CONTEXT, postContext);
      startActivityForResult(intent, ComposePostActivity.COMPOSE_ACTIVITY_REQUEST_CODE);
    } else if (id == R.id.board_topic_action_search) {
      PopupSearchWindow popup = new PopupSearchWindow();
      popup.initPopupWindow(this);
      popup.showAtLocation(mRecyclerView, Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
    } else if (id == R.id.board_topic_action_favorite) {
      SMTHHelper helper = SMTHHelper.getInstance();
      helper.wService.manageFavoriteBoard("0", "ab", this.mBoard.getBoardEngName())
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(new Observer<AjaxResponse>() {
            @Override public void onSubscribe(@NonNull Disposable disposable) {

            }

            @Override public void onNext(@NonNull AjaxResponse ajaxResponse) {
              Log.d(TAG, "onNext: " + ajaxResponse.toString());
              if (ajaxResponse.getAjax_st() == AjaxResponse.AJAX_RESULT_OK) {
                Toast.makeText(BoardTopicActivity.this, ajaxResponse.getAjax_msg() + "\n请手动刷新收藏夹！", Toast.LENGTH_SHORT).show();
              } else {
                //Toast.makeText(BoardTopicActivity.this, ajaxResponse.toString(), Toast.LENGTH_SHORT).show();
                Toast.makeText(BoardTopicActivity.this, "该版面已经收藏", Toast.LENGTH_SHORT).show();
              }

            }

            @Override public void onError(@NonNull Throwable e) {
              Toast.makeText(BoardTopicActivity.this, "收藏版面失败！\n" + e.toString(), Toast.LENGTH_SHORT).show();

            }

            @Override public void onComplete() {

            }
          });
    }
    return super.onOptionsItemSelected(item);
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.board_topic_menu, menu);
    return true;
  }

  public void clearLoadingHints() {
    dismissProgress();

    if (mSwipeRefreshLayout != null && mSwipeRefreshLayout.isRefreshing()) {
      mSwipeRefreshLayout.setRefreshing(false);  // This hides the spinner
    }

    if (mScrollListener != null) {
      mScrollListener.setLoading(false);
    }
  }

  // load topics from next page, without alert
  public void loadMoreItems() {
    if (isSearchMode || mSwipeRefreshLayout.isRefreshing() || pDialog.isShowing()) {
      return;
    }


    mCurrentPageNo += 1;
    // Log.d(TAG, mCurrentPageNo + " page is loading now...");
    LoadBoardTopics();
  }
  @SuppressLint("NotifyDataSetChanged")
  @Override public void onRefresh() {
    // this method is slightly different with RefreshBoardTopicFromPageOne
    // this method does not alert since it's triggered by SwipeRefreshLayout
    mCurrentPageNo = 1;
    TopicListContent.clearBoardTopics();
    Objects.requireNonNull(mRecyclerView.getAdapter()).notifyDataSetChanged();
    LoadBoardTopics();
  }

  @SuppressLint("NotifyDataSetChanged")
  public void RefreshBoardTopicFromPageOne() {
    showProgress("刷新版面文章...");

    TopicListContent.clearBoardTopics();
    mRecyclerView.getAdapter().notifyDataSetChanged();

    mCurrentPageNo = 1;
    LoadBoardTopics();
  }

  public void RefreshBoardTopicsWithoutClear() {
    showProgress("加载版面文章...");

    LoadBoardTopics();
  }

  public void LoadBoardTopics() {

    isSearchMode = false;
    final SMTHHelper helper = SMTHHelper.getInstance();

    helper
        .wService
        .getBoardTopicsByPage(mBoard.getBoardEngName(), Integer.toString(mCurrentPageNo))
        .flatMap(
            new Function<ResponseBody, ObservableSource<Topic>>() {
              @Override
              public ObservableSource<Topic> apply(@NonNull ResponseBody responseBody)
                  throws Exception {
                try {
                  String response = responseBody.string();
                  List<Topic> topics = SMTHHelper.ParseBoardTopicsFromWWW(response);
                  // Log.d("vinney-999",Integer.toString(topics.size()));
                  if (topics.size() == 0) {
                    return Observable.empty(); // handle error case
                  }
                  return Observable.fromIterable(topics);
                } catch (Exception e) {
                  Log.e(TAG, "call: " + Log.getStackTraceString(e));
                  return null;
                }
              }
            })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new Observer<Topic>() {
              @Override
              public void onSubscribe(@NonNull Disposable disposable) {
                Topic topic = new Topic(String.format(Locale.CHINA, "第%d页:", mCurrentPageNo));
                topic.isCategory = true;
                TopicListContent.addBoardTopic(topic, mBoard.getBoardEngName());
                // mRecyclerView.getAdapter().notifyItemInserted(TopicListContent.BOARD_TOPICS.size() - 1);
                mRecyclerView.post(
                    new Runnable() {
                      @Override
                      public void run() {
                        // Notify adapter with appropriate notify methods
                        Objects.requireNonNull(mRecyclerView.getAdapter())
                            .notifyItemInserted(TopicListContent.BOARD_TOPICS.size() - 1);
                      }
                    });
              }

              @Override
              public void onNext(@NonNull Topic topic) {
                // Log.d(TAG, topic.toString());
                if (!topic.isSticky || mSetting.isShowSticky()) {
                  if (!MapHash.contains(topic.getTitle())) {
                    if (MapHash.size() < MAXSIZE) {
                      // Log.d(TAG, "Vinney1 + " + topic.getTitle());
                      TopicListContent.addBoardTopic(topic, mBoard.getBoardEngName());
                      MapHash.put(topic.getTitle(), topic.getTopicID());
                      Objects.requireNonNull(mRecyclerView.getAdapter())
                          .notifyItemInserted(TopicListContent.BOARD_TOPICS.size() - 1);
                    } else {
                      // Log.d(TAG, "Vinney2 + " + topic.getTitle());
                      MapHash.clear();
                      TopicListContent.addBoardTopic(topic, mBoard.getBoardEngName());
                      MapHash.put(topic.getTitle(), topic.getTopicID());
                      Objects.requireNonNull(mRecyclerView.getAdapter())
                          .notifyItemInserted(TopicListContent.BOARD_TOPICS.size() - 1);
                    }
                  } else {
                    Log.d(TAG, "Vinney3 + " + topic.getTitle());
                  }
                }
              }

              @Override
              public void onError(@NonNull Throwable e) {
                clearLoadingHints();

                Toast.makeText(
                        SMTHApplication.getAppContext(),
                        String.format(Locale.CHINA, "获取第%d页的帖子失败!\n", mCurrentPageNo)
                            + e.toString(),
                        Toast.LENGTH_SHORT)
                    .show();
                mCurrentPageNo -= 1;
              }

              @Override
              public void onComplete() {
                clearLoadingHints();


                // Special User OFFLINE case: [] or [Category 第一页:]
                if ((TopicListContent.BOARD_TOPICS.toString().length() == 2
                    || TopicListContent.BOARD_TOPICS.toString().length() == 15)) {
                  Toast.makeText(SMTHApplication.getAppContext(),"特殊掉线请重新登录！",Toast.LENGTH_SHORT).show();
                  TopicListContent.clearBoardTopics();
                  SMTHApplication.activeUser = null;

                  try {
                    Thread.sleep(500);
                    onBackPressed();
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                  }
                  if (!SMTHApplication.isValidUser()) {
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivityForResult(intent, MainActivity.LOGIN_ACTIVITY_REQUEST_CODE);
                  }
                }

              }
            });
  }


  @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (Settings.getInstance().isVolumeKeyScroll() && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
      RecyclerViewUtil.ScrollRecyclerViewByKey(mRecyclerView, keyCode);
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  // http://stackoverflow.com/questions/4500354/control-volume-keys
  @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
    // disable the beep sound when volume up/down is pressed
    if (Settings.getInstance().isVolumeKeyScroll() && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
      return true;
    }
    return super.onKeyUp(keyCode, event);
  }

  @Override public void onTopicFragmentInteraction(Topic item) {
    if (item.isCategory) return;
    SMTHApplication.ReadRec = false;//Reset to allow 2nd Topic Reading record
    SMTHApplication.ReadPostFirst = null;
    Intent intent = new Intent(this, PostListActivity.class);
    item.setBoardEngName(mBoard.getBoardEngName());
    item.setBoardChsName(mBoard.getBoardChsName());

    if (!mBoard.getBoardChsName().equals(SMTHApplication.ReadBoard1) && !mBoard.getBoardChsName().equals(SMTHApplication.ReadBoard2)
            && !mBoard.getBoardChsName().equals(SMTHApplication.ReadBoard3)) {
      switch (SMTHApplication.ReadBoardCount % 3) {
        case 0:
          SMTHApplication.ReadBoard1 = mBoard.getBoardChsName();
          SMTHApplication.ReadBoardEng1 = item.getBoardEngName();
          break;
        case 1:
          SMTHApplication.ReadBoard2 = mBoard.getBoardChsName();
          SMTHApplication.ReadBoardEng2 = item.getBoardEngName();
          break;
        case 2:
          SMTHApplication.ReadBoard3 = mBoard.getBoardChsName();
          SMTHApplication.ReadBoardEng3 = item.getBoardEngName();
          break;
      }
      SMTHApplication.ReadBoardCount++;
    }

    intent.putExtra(SMTHApplication.TOPIC_OBJECT, item);
    intent.putExtra(SMTHApplication.FROM_BOARD, SMTHApplication.FROM_BOARD_BOARD);
    startActivity(intent);
  }

  @SuppressLint("NotifyDataSetChanged")
  @Override public void OnSearchAction(String keyword, String author, boolean elite, boolean attachment) {
    Log.d(TAG, "OnSearchAction: " + keyword + author + elite + attachment);

    isSearchMode = true;
    showProgress("加载搜索结果...");

    TopicListContent.BOARD_TOPICS.clear();
    Objects.requireNonNull(mRecyclerView.getAdapter()).notifyDataSetChanged();

    String eliteStr = null;
    if (elite) eliteStr = "on";

    String attachmentStr = null;
    if (attachment) attachmentStr = "on";

    SMTHHelper helper = SMTHHelper.getInstance();
    helper.wService.searchTopicInBoard(keyword, author, eliteStr, attachmentStr, this.mBoard.getBoardEngName())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .flatMap(new Function<ResponseBody, ObservableSource<Topic>>() {
          @Override public ObservableSource<Topic> apply(@NonNull ResponseBody responseBody) throws Exception {
            try {
              String response = responseBody.string();
              List<Topic> topics = SMTHHelper.ParseSearchResultFromWWW(response);
              Topic topic = new Topic("搜索模式 - 下拉或按返回键退出搜索模式");
              topics.add(0, topic);
              return Observable.fromIterable(topics);
            } catch (Exception e) {
              Log.d(TAG, Log.getStackTraceString(e));
              return null;
            }
          }
        })
        .subscribe(new Observer<Topic>() {
          @Override public void onSubscribe(@NonNull Disposable disposable) {

          }

          @Override public void onNext(@NonNull Topic topic) {
            TopicListContent.addBoardTopic(topic, mBoard.getBoardEngName());
            mRecyclerView.getAdapter().notifyItemInserted(TopicListContent.BOARD_TOPICS.size() - 1);

          }

          @Override public void onError(@NonNull Throwable e) {
            Toast.makeText(SMTHApplication.getAppContext(), "加载搜索结果失败!\n" + e.toString(), Toast.LENGTH_SHORT).show();

          }

          @Override public void onComplete() {
            dismissProgress();
          }
        });
  }

}