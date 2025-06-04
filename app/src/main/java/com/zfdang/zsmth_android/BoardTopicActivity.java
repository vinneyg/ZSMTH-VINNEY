package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.FragmentManager;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.jude.swipbackhelper.SwipeBackHelper;
import com.zfdang.SMTHApplication;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import okhttp3.ResponseBody;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * An activity representing a list of Topics. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link PostListActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class BoardTopicActivity extends SMTHBaseActivity
        //implements OnTopicFragmentInteractionListener, SwipeRefreshLayout.OnRefreshListener, PopupSearchWindow.SearchInterface {
        implements OnTopicFragmentInteractionListener,  PopupSearchWindow.SearchInterface {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */

    private final String TAG = "BoardTopicActivity";

    private Board mBoard = null;

    private int mCurrentPageNo = 1;
    private int mCurrentPageModeNo = 1;
    //private final int LOAD_MORE_THRESHOLD = 1;

    private SmartRefreshLayout mSwipeRefreshLayout = null;
    private EndlessRecyclerOnScrollListener mScrollListener = null;
    private RecyclerView mRecyclerView = null;

    private Settings mSetting;

    private boolean isSearchMode = false;

    private static final int MAXSIZE = 100;
    private static final Hashtable<String,String> MapHash = new Hashtable<>(MAXSIZE);

    private ActivityResultLauncher<Intent> mActivityLoginResultLauncher;
    private ActivityResultLauncher<Intent> mActivityPostResultLauncher;

    private boolean showReplierInfo = true;

    private Menu mMenu;
    private String currentMode = SMTHApplication.ReadMode2;

    private BoardTopicRecyclerViewAdapter adapter;

    @Override protected void onDestroy() {
        super.onDestroy();
        SwipeBackHelper.onDestroy(this);

    }

    @Override protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        SwipeBackHelper.onPostCreate(this);
    }

    private static BoardTopicActivity mActivity1 = null;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SwipeBackHelper.onCreate(this);

        OnBackPressedDispatcher dispatcher = getOnBackPressedDispatcher();

        dispatcher.addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Handle the back button press here
                if (isSearchMode) {
                    onRefresh();
                    return;
                }
                FragmentManager fragmentManager = getSupportFragmentManager();
                if (fragmentManager.getBackStackEntryCount() > 0) {
                    fragmentManager.popBackStack();
                } else {
                    // 移除滑动状态检查直接执行关闭
                    SwipeBackHelper.finish(BoardTopicActivity.this);
                }

            }
        });

        mActivity1  = this;

        setContentView(R.layout.activity_board_topic);

        Toolbar toolbar = findViewById(R.id.board_topic_toolbar);
        setSupportActionBar(toolbar);
        if (toolbar == null) {
            Log.e(TAG, "toolbar is null");
            return;
        }
        //toolbar.setTitle(getTitle());

        mSetting = Settings.getInstance();

        // Initialize the ActivityResultLauncher object.
        mActivityLoginResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK)
                    {
                        Intent intent = new Intent("com.zfdang.zsmth_android.UPDATE_USER_STATUS");
                        sendBroadcast(intent);
                        finish();
                    }
                });

        // Initialize the ActivityResultLauncher object.
        mActivityPostResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        RefreshBoardTopicFromPageOne();
                    }
                });

        // enable pull down to refresh
        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayoutBoard);
        if (mSwipeRefreshLayout == null) {
            Log.e(TAG, "mSwipeRefreshLayout is null");
            return;
        }

        //mSwipeRefreshLayout.setOnRefreshListener(refreshLayout -> RefreshBoardTopicsWithoutClear());

        mSwipeRefreshLayout.setOnRefreshListener(refreshLayout -> {
            if (isSearchMode) {
                onRefresh();
            }
            else {
                //RefreshBoardTopicsWithoutClear();
                RefreshBoardTopicFromPageOne();
            }
        });

        mRecyclerView = findViewById(R.id.board_topic_list);
        if (mRecyclerView == null) {
            Log.e(TAG, "mRecyclerView is null");
            return;
        }
        //mRecyclerView.setItemAnimator(null);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL, 0));
        LinearLayoutManager linearLayoutManager = new WrapContentLinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);

        adapter = new BoardTopicRecyclerViewAdapter(TopicListContent.BOARD_TOPICS, this,showReplierInfo);

        mRecyclerView.setAdapter(adapter);

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
            }

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
        if (board == null) {
            Log.e(TAG, "board is null.");
            return;
        }

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

        if (TopicListContent.BOARD_TOPICS.isEmpty()) {
            RefreshBoardTopicsWithoutClear();
            //new Handler(Looper.getMainLooper()).postDelayed(this::RefreshBoardTopicsWithoutClear, 100);
        }

    }


    public static BoardTopicActivity getInstance () {
        if (mActivity1 != null) {
            return mActivity1;
        }
        return null;
    }

    public void updateTitle() {
        String title = mBoard != null ? mBoard.getBoardChsName() : "";
        //setTitle(title + " - 主题列表");
        setTitle(title);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (isSearchMode) {
                onRefresh();
                return true;
            }
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragmentManager.getBackStackEntryCount() > 0) {
                fragmentManager.popBackStack();
            } else {
                // 移除滑动状态检查直接执行关闭
                SwipeBackHelper.finish(BoardTopicActivity.this);
            }
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
            intent.putExtra(SMTHApplication.READ_MODE,SMTHApplication.ReadMode0.equals(currentMode)?"0":"1");
            mActivityPostResultLauncher.launch(intent);
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
                                //Toast.makeText(BoardTopicActivity.this, ajaxResponse.getAjax_msg() + "\n请手动刷新收藏夹！", Toast.LENGTH_SHORT).show();
                                SMTHApplication.bNewFavoriteBoard = true;
                            } else {
                                //Toast.makeText(BoardTopicActivity.this, ajaxResponse.toString(), Toast.LENGTH_SHORT).show();
                                Toast.makeText(BoardTopicActivity.this, "该版面已经收藏！", Toast.LENGTH_SHORT).show();
                            }

                        }

                        @Override public void onError(@NonNull Throwable e) {
                            Toast.makeText(BoardTopicActivity.this, "收藏版面失败！\n" + e.toString(), Toast.LENGTH_SHORT).show();

                        }

                        @Override public void onComplete() {

                        }
                    });
        } else if (id == R.id.mode_2 || id == R.id.mode_1 || id == R.id.mode_0) {
            resetModeMenuCheckedState();
            item.setChecked(true);


            if (id == R.id.mode_0) {
                // Handle the logic when mode_0 is selected
                currentMode = SMTHApplication.ReadMode0;
            } else if (id == R.id.mode_1) {
                currentMode = SMTHApplication.ReadMode1;
                // Handle the logic when mode_1 is selected
            } else if (id == R.id.mode_2) {
                currentMode = SMTHApplication.ReadMode2;
                // Handle the logic when mode_2 is selected
            }
            toggleReplierInfoVisibility(currentMode);
            RefreshBoardTopicFromPageOne();
        }
        return super.onOptionsItemSelected(item);
    }

    private void resetModeMenuCheckedState() {
        if (mMenu != null) {
            MenuItem mode0 = mMenu.findItem(R.id.mode_0);
            MenuItem mode1 = mMenu.findItem(R.id.mode_1);
            MenuItem mode2 = mMenu.findItem(R.id.mode_2);

            if (mode0 != null) {
                mode0.setChecked(false);
            }
            if (mode1 != null) {
                mode1.setChecked(false);
            }
            if (mode2 != null) {
                mode2.setChecked(false);
            }
        }
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.board_topic_menu, menu);
        mMenu = menu;
        return true;
    }

    public void clearLoadingHints() {
        dismissProgress();

        if (mScrollListener != null) {
            mScrollListener.setLoading(false);
        }
    }

    // load topics from next page, without alert
    public void loadMoreItems() {
        if (isSearchMode || mSwipeRefreshLayout.isRefreshing() || pDialog.isShowing()) {
            return;
        }

        if(SMTHApplication.ReadMode0.equals(currentMode)){
            //Mode 0
            mCurrentPageModeNo +=1;
            LoadBoardTopicsMobile();
        }else{
            mCurrentPageNo += 1;
            // Log.d(TAG, mCurrentPageNo + " page is loading now...");
            LoadBoardTopics(currentMode);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void onRefresh() {
        RefreshBoardTopicFromPageOne();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void RefreshBoardTopicFromPageOne() {
        showProgress("刷新版面文章...");
        int oldItemCount = TopicListContent.BOARD_TOPICS.size();
        TopicListContent.clearBoardTopics();
        //Objects.requireNonNull(mRecyclerView.getAdapter()).notifyDataSetChanged();
        MapHash.clear();
        if (oldItemCount > 0) {
            Objects.requireNonNull(mRecyclerView.getAdapter()).notifyItemRangeRemoved(0, oldItemCount);
        }

        if(currentMode.equals("2")||currentMode.equals("1")){
            mCurrentPageNo = 1;
            LoadBoardTopics(currentMode);
        } else {   //mode 0 & 1
            mCurrentPageModeNo=1;
            LoadBoardTopicsMobile();
        }

        Objects.requireNonNull(mRecyclerView.getAdapter()).notifyDataSetChanged();
    }

    public void RefreshBoardTopicsWithoutClear() {
        showProgress("加载版面文章...");
        if(SMTHApplication.ReadMode0.equals(currentMode)){
            LoadBoardTopicsMobile();
        }else{
            LoadBoardTopics(currentMode);
        }
        Objects.requireNonNull(mRecyclerView.getAdapter()).notifyDataSetChanged();
    }

    public void LoadBoardTopicsMobile() {

        isSearchMode = false;
        final SMTHHelper helper = SMTHHelper.getInstance();

        // 用于临时存储新的 Topic
        ArrayList<Topic> newTopics = new ArrayList<>();

        // 添加页码提示 Topic
        Topic pageTopic = new Topic(String.format(java.util.Locale.CHINA, "第%d页:", mCurrentPageModeNo));
        pageTopic.isCategory = true;
        newTopics.add(pageTopic);

        helper
                .mService
                .getBoardTopicsByPage(mBoard.getBoardEngName(), Integer.toString(mCurrentPageModeNo))
                .flatMap(
                        (Function<ResponseBody, ObservableSource<Topic>>) responseBody -> {
                            try {
                                String response = responseBody.string();
                                List<Topic> topics = SMTHHelper.ParseBoardTopicsFromWWWMobile(response);
                                if (topics.isEmpty()){
                                    return null;
                                }
                                return Observable.fromIterable(topics);
                            } catch (Exception e) {
                                Log.e(TAG, "call: " + Log.getStackTraceString(e));
                                return null;
                            }
                        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Observer<Topic>() {
                            @Override
                            public void onSubscribe(@NonNull Disposable disposable) {
                            }

                            @Override
                            public void onNext(@NonNull Topic topic) {
                                // Log.d(TAG, topic.toString());
                                if (!topic.isSticky || mSetting.isShowSticky()) {
                                    if (!MapHash.contains(topic.getTitle())) {
                                        if (MapHash.size() >= MAXSIZE) {
                                            MapHash.clear();
                                        }
                                        newTopics.add(topic);
                                        MapHash.put(topic.getTitle(), topic.getTopicID());
                                    } else {
                                        Log.d(TAG, "sticky " + topic.getTitle());
                                    }
                                }
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                clearLoadingHints();
                                if (mSwipeRefreshLayout != null) {
                                    mSwipeRefreshLayout.finishRefresh(false);
                                }

                                if(mCurrentPageModeNo != 1)
                                    Toast.makeText(
                                                    SMTHApplication.getAppContext(),
                                                    String.format(Locale.CHINA, "错误:获取第%d页的帖子失败!\n"+e.toString(), mCurrentPageNo),
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                else{
                                    mCurrentPageModeNo -= 1;

                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        try {
                                            if (!SMTHApplication.isValidUser()) {
                                                Intent intent = new Intent(BoardTopicActivity.this, LoginActivity.class);
                                                mActivityLoginResultLauncher.launch(intent);
                                            } else {
                                                Toast.makeText(BoardTopicActivity.this, "站点问题，请稍等。\n或者重新登录进入！\n", Toast.LENGTH_SHORT).show();
                                                new Handler(Looper.getMainLooper()).postDelayed(() -> finish(), Toast.LENGTH_SHORT);
                                            }
                                        } catch (Exception ie) {
                                            Log.e(TAG, "Error occurred during delayed operation: ", e);
                                        }
                                    }, 500);

                                }

                            }

                            @Override
                            public void onComplete() {
                                clearLoadingHints();
                                if (mSwipeRefreshLayout != null) {
                                    mSwipeRefreshLayout.finishRefresh(true);
                                }

                                final int startPosition = TopicListContent.BOARD_TOPICS.size();
                                TopicListContent.BOARD_TOPICS.addAll(newTopics);
                                // 一次性通知适配器数据更新
                                if (!newTopics.isEmpty()) {
                                    Objects.requireNonNull(mRecyclerView.getAdapter()).notifyItemRangeInserted(startPosition, newTopics.size());
                                }
                            }
                        });
    }
    public void LoadBoardTopics(String mode) {

        isSearchMode = false;
        final SMTHHelper helper = SMTHHelper.getInstance();

        // 用于临时存储新的 Topic
        ArrayList<Topic> newTopics = new ArrayList<>();

        // 添加页码提示 Topic
        Topic pageTopic = new Topic(String.format(java.util.Locale.CHINA, "第%d页:", mCurrentPageNo));
        pageTopic.isCategory = true;
        newTopics.add(pageTopic);

        helper
                .wService
                .getBoardTopicsByPage(mBoard.getBoardEngName(), Integer.toString(mCurrentPageNo))
                .flatMap(
                        (Function<ResponseBody, ObservableSource<Topic>>) responseBody -> {
                            try {
                                String response = responseBody.string().replace("&emsp;","");
                                List<Topic> topics = SMTHHelper.ParseBoardTopicsFromWWW(response);
                                if (topics.isEmpty()){
                                    return null;
                                }
                                return Observable.fromIterable(topics);
                            } catch (Exception e) {
                                Log.e(TAG, "call: " + Log.getStackTraceString(e));
                                return null;
                            }
                        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Observer<Topic>() {
                            @Override
                            public void onSubscribe(@NonNull Disposable disposable) {
                            }

                            @Override
                            public void onNext(@NonNull Topic topic) {
                                // Log.d(TAG, topic.toString());
                                if (!topic.isSticky || mSetting.isShowSticky()) {
                                    if (!MapHash.contains(topic.getTitle())) {
                                        if (MapHash.size() >= MAXSIZE) {
                                            MapHash.clear();
                                        }
                                        newTopics.add(topic);
                                        MapHash.put(topic.getTitle(), topic.getTopicID());
                                    } else {
                                        Log.d(TAG, "sticky " + topic.getTitle());
                                    }
                                }
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                clearLoadingHints();
                                if (mSwipeRefreshLayout != null) {
                                    mSwipeRefreshLayout.finishRefresh(false);
                                }

                                if(mCurrentPageNo != 1)
                                    Toast.makeText(
                                                    SMTHApplication.getAppContext(),
                                                    String.format(Locale.CHINA, "错误:获取第%d页的帖子失败!\n"+e.toString(), mCurrentPageNo),
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                else{
                                    mCurrentPageNo -= 1;

                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        try {
                                            if (!SMTHApplication.isValidUser()) {
                                                Intent intent = new Intent(BoardTopicActivity.this, LoginActivity.class);
                                                mActivityLoginResultLauncher.launch(intent);
                                            } else {
                                                Toast.makeText(BoardTopicActivity.this, "站点问题，请稍等。\n或者重新登录进入！\n", Toast.LENGTH_SHORT).show();
                                                new Handler(Looper.getMainLooper()).postDelayed(() -> finish(), Toast.LENGTH_SHORT);
                                            }
                                        } catch (Exception ie) {
                                            Log.e(TAG, "Error occurred during delayed operation: ", e);
                                        }
                                    }, 500);

                                }

                            }

                            @Override
                            public void onComplete() {
                                clearLoadingHints();
                                if (mSwipeRefreshLayout != null) {
                                    mSwipeRefreshLayout.finishRefresh(true);
                                }

                                // 如果 mode 为 1，按 publishDate 排序
                                if ("1".equals(mode)) {
                                    Topic tempPageTopic = newTopics.remove(0);

                                    newTopics.sort(new Comparator<Topic>() {
                                        private final SimpleDateFormat timeFormat1 = new SimpleDateFormat("HH:mm", Locale.CHINA);
                                        private final SimpleDateFormat timeFormat2 = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
                                        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
                                        private final Calendar today = Calendar.getInstance();
                                        @Override
                                        public int compare(Topic t1, Topic t2) {
                                            try {
                                                Date date1 = parsePublishDate(t1.getPublishDate());
                                                Date date2 = parsePublishDate(t2.getPublishDate());
                                                return date2.compareTo(date1); // 从新到旧排序
                                            } catch (ParseException e) {
                                                Log.e(TAG, "Error parsing publish date", e);
                                                return 0;
                                            }
                                        }

                                        private Date parsePublishDate(String publishDate) throws ParseException {
                                            if (publishDate == null) {
                                                // Handle null publishDate, for example, return the earliest possible date
                                                return new Date(0);
                                            }
                                            if (publishDate.length() == 5) { // hh:mm 格式
                                                Calendar cal = (Calendar) today.clone();
                                                cal.setTime(Objects.requireNonNull(timeFormat1.parse(publishDate)));
                                                cal.set(Calendar.YEAR, today.get(Calendar.YEAR));
                                                cal.set(Calendar.MONTH, today.get(Calendar.MONTH));
                                                cal.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH));
                                                return cal.getTime();
                                            } else if (publishDate.length() == 8) { // hh:mm:ss 格式
                                                Calendar cal = (Calendar) today.clone();
                                                cal.setTime(Objects.requireNonNull(timeFormat2.parse(publishDate)));
                                                cal.set(Calendar.YEAR, today.get(Calendar.YEAR));
                                                cal.set(Calendar.MONTH, today.get(Calendar.MONTH));
                                                cal.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH));
                                                return cal.getTime();
                                            } else { // YYYY/MM/DD 格式
                                                return dateFormat.parse(publishDate);
                                            }
                                        }
                                    });

                                    newTopics.add(0, tempPageTopic);

                                }
// 获取插入前的位置
                                final int startPosition = TopicListContent.BOARD_TOPICS.size();
                                TopicListContent.BOARD_TOPICS.addAll(newTopics);

                                // 一次性通知适配器数据更新
                                if (!newTopics.isEmpty()) {
                                    Objects.requireNonNull(mRecyclerView.getAdapter()).notifyItemRangeInserted(startPosition, newTopics.size());
                                }
                            }
                        });
    }


    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (Settings.getInstance().isVolumeKeyScroll() && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN)) {
            RecyclerViewUtil.ScrollRecyclerViewByKey(mRecyclerView, keyCode);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // http://stackoverflow.com/questions/4500354/control-volume-keys
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        // disable the beep sound when volume up/down is pressed
        if (Settings.getInstance().isVolumeKeyScroll() && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN)) {
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

        if(SMTHApplication.ReadMode0.equals(currentMode))
            intent.putExtra(SMTHApplication.READ_MODE, SMTHApplication.ReadMode0);
        else
            intent.putExtra(SMTHApplication.READ_MODE, SMTHApplication.ReadMode1);

        intent.putExtra(SMTHApplication.FROM_BOARD, SMTHApplication.FROM_BOARD_BOARD);
        startActivity(intent);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override public void OnSearchAction(String keyword, String author, boolean elite, boolean attachment) {
        Log.d(TAG, "OnSearchAction: " + keyword + author + elite + attachment);

        isSearchMode = true;
        showProgress("加载搜索结果...");

        TopicListContent.BOARD_TOPICS.clear();
        //Objects.requireNonNull(mRecyclerView.getAdapter()).notifyDataSetChanged();

        String eliteStr = null;
        if (elite) eliteStr = "on";

        String attachmentStr = null;
        if (attachment) attachmentStr = "on";

        SMTHHelper helper = SMTHHelper.getInstance();
        helper.wService.searchTopicInBoard(keyword, author, eliteStr, attachmentStr, this.mBoard.getBoardEngName())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap((Function<ResponseBody, ObservableSource<Topic>>) responseBody -> {
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
                })
                .subscribe(new Observer<Topic>() {
                    @Override public void onSubscribe(@NonNull Disposable disposable) {

                    }

                    @Override public void onNext(@NonNull Topic topic) {
                        TopicListContent.addBoardTopic(topic);
                        Objects.requireNonNull(mRecyclerView.getAdapter()).notifyItemInserted(TopicListContent.BOARD_TOPICS.size() - 1);

                    }

                    @Override public void onError(@NonNull Throwable e) {
                        Toast.makeText(SMTHApplication.getAppContext(), "加载搜索结果失败!\n" + e.toString(), Toast.LENGTH_SHORT).show();

                    }

                    @Override public void onComplete() {
                        dismissProgress();
                    }
                });
    }

    @Override
    public void onResume(){
        super.onResume();
        if(SMTHApplication.bNewPost|| SMTHApplication.bNewMailSent){
            SMTHApplication.bNewPost = false;
            SMTHApplication.bNewMailSent = false;
            RefreshBoardTopicFromPageOne();
        }
    }

    public void toggleReplierInfoVisibility(String mode) {
        showReplierInfo = SMTHApplication.ReadMode2.equals(mode);
        boolean showTopicStatus = !SMTHApplication.ReadMode0.equals(mode);
        if (adapter != null) {
            adapter.setShowReplierInfo(showReplierInfo);
            adapter.setShowTopicStatus(showTopicStatus);
        }
    }
}