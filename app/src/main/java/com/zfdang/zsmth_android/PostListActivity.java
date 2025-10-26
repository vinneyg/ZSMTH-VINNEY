package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.helpers.NewToast;
import com.zfdang.zsmth_android.helpers.RecyclerViewUtil;
import com.zfdang.zsmth_android.models.Attachment;
import com.zfdang.zsmth_android.models.Board;
import com.zfdang.zsmth_android.models.ComposePostContext;
import com.zfdang.zsmth_android.models.Post;
import com.zfdang.zsmth_android.models.PostActionAlertDialogItem;
import com.zfdang.zsmth_android.models.PostListContent;
import com.zfdang.zsmth_android.models.Topic;
import com.zfdang.zsmth_android.newsmth.AjaxResponse;
import com.zfdang.zsmth_android.newsmth.SMTHHelper;
import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.onekeyshare.OnekeyShare;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Callback;

/**
 * An activity representing a single Topic detail screen. This
 * activity is only used narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link BoardTopicActivity}.
 */
public class PostListActivity extends SMTHBaseActivity
        implements View.OnClickListener, OnTouchListener, RecyclerViewGestureListener.OnItemLongClickListener, PopupLikeWindow.OnLikeInterface,
        PopupForwardWindow.OnForwardInterface,PostRecyclerViewAdapter.OnBtnReplyClickListener,PostRecyclerViewAdapter.OnBtnMoreClickListener{

    private static final String TAG = "PostListActivity";
    public RecyclerView mRecyclerView = null;
    private TextView mTitle = null;
    private EditText mPageNo = null;

    public int mCurrentPageNo = 1;
    public static int mCurrentReadPageNo = 1;
    public static int mTotalPageNo =0;
    private String mFilterUser = null;

    private static Topic mTopic = null;

    private static final HashMap<String, Integer> lastPositions = new HashMap<>();
    private static final HashMap<String, Integer> lastOffsets = new HashMap<>();

    static private final int POST_PER_PAGE = 10;

    static {
        ClassicsHeader.REFRESH_HEADER_PULLING = "下拉可以刷新";
        ClassicsHeader.REFRESH_HEADER_REFRESHING = "正在刷新...";
        ClassicsHeader.REFRESH_HEADER_LOADING = "正在加载...";
        ClassicsHeader.REFRESH_HEADER_RELEASE = "释放立即刷新";
        ClassicsHeader.REFRESH_HEADER_FINISH = "刷新完成";
        ClassicsHeader.REFRESH_HEADER_FAILED = "刷新失败";
        ClassicsHeader.REFRESH_HEADER_UPDATE = "上次更新 M-d HH:mm";

        ClassicsFooter.REFRESH_FOOTER_PULLING = "上拉可以翻页";
        ClassicsFooter.REFRESH_FOOTER_RELEASE = "释放立即翻页";
        ClassicsFooter.REFRESH_FOOTER_REFRESHING = "正在刷新...";
        ClassicsFooter.REFRESH_FOOTER_LOADING = "正在加载下一页...";
        ClassicsFooter.REFRESH_FOOTER_FINISH = "翻页完成";
        ClassicsFooter.REFRESH_FOOTER_FAILED = "翻页失败";
        ClassicsFooter.REFRESH_FOOTER_NOTHING = "全部加载完成";

    }

    private SmartRefreshLayout mRefreshLayout;
    private String mFrom;
    private String mReadMode;
    private boolean isLoading = false;

    private GestureDetector mGestureDetector;
    private LinearLayoutManager linearLayoutManager;

    private ActivityResultLauncher<Intent> mActivityLoginResultLauncher;
    private ActivityResultLauncher<Intent> mActivityPostResultLauncher;

    @Override protected void onDestroy() {
        super.onDestroy();
    }

    @Override protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    /**
     * Record current View
     */

    private void getPositionAndOffset() {
        if (mRecyclerView.getLayoutManager() != null && mTopic != null) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
            View topView = layoutManager.getChildAt(0);
            if (topView != null) {
                int offset = topView.getTop();
                int position = layoutManager.getPosition(topView);

                lastPositions.put(mTopic.getTopicID(), position);
                lastOffsets.put(mTopic.getTopicID(), offset);
            }
        }
    }

    private void scrollToPosition() {
        if(mRecyclerView.getLayoutManager() != null && mTopic != null) {
            String topicId = mTopic.getTopicID();
            if (lastPositions.containsKey(topicId) && lastOffsets.containsKey(topicId)) {
                Integer position = lastPositions.get(topicId);
                Integer offset = lastOffsets.get(topicId);
                if (position != null && offset != null) {
                    ((LinearLayoutManager) mRecyclerView.getLayoutManager()).scrollToPositionWithOffset(position, offset);
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);

        Toolbar toolbar = findViewById(R.id.post_list_toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        // get Board information from launcher
        Intent intent = getIntent();
        mFrom = intent.getStringExtra(SMTHApplication.FROM_BOARD);
        mReadMode = intent.getStringExtra(SMTHApplication.READ_MODE);
        Topic topic = intent.getParcelableExtra(SMTHApplication.TOPIC_OBJECT);
        if (topic == null) {
            return;
        }
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(topic.getBoardChsName() + " - 阅读文章");
        }

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fragmentManager = getSupportFragmentManager();
                if (fragmentManager.getBackStackEntryCount() > 0) {
                    fragmentManager.popBackStack();
                } else {
                    PostListActivity.this.finish();
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        // Initialize the ActivityResultLauncher object.
        mActivityLoginResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK)
                    {
                        Intent intentTemp = new Intent("com.zfdang.zsmth_android.UPDATE_USER_STATUS");
                        sendBroadcast(intentTemp);
                        finish();
                    }
                });

        // Initialize the ActivityResultLauncher object.
        mActivityPostResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        reloadPostList();
                    }
                });

        mTitle = findViewById(R.id.post_list_title);
        if (mTitle == null) {
            return;
        }

        mPageNo = findViewById(R.id.post_list_page_no);
        if (mPageNo == null) {
            return;
        }

        // define swipe refresh function
        mRefreshLayout = findViewById(R.id.post_list_swipe_refresh_layout);
        mRefreshLayout.setEnableAutoLoadMore(false);
        mRefreshLayout.setEnableScrollContentWhenLoaded(false);
        mRefreshLayout.setEnableOverScrollBounce(false);

        mRefreshLayout.setOnRefreshListener(refreshLayout -> {
            // reload current page
            if(Settings.getInstance().isautoloadmore()) {
                reloadPostListWithoutAlert();
            }
            else { //Waterfall mode  insert item
                InsertPostListWithoutAlert();
            }
        });
        mRefreshLayout.setOnLoadMoreListener(refreshLayout -> {
            if (SMTHApplication.ReadMode0.equals(mReadMode)) {
                refreshLayout.finishLoadMoreWithNoMoreData();
                return;
            }
            // load next page if available
            if(Settings.getInstance().isautoloadmore()) {
                goToNextPage();
            }
            else {
                //goToNextPage();
                reloadPostListWithoutAlertNew();
            }
        });

        mRecyclerView = findViewById(R.id.post_list);
        if (mRecyclerView == null) {
            return;
        }

        mRecyclerView.addItemDecoration(
                new DividerItemDecoration(this, LinearLayoutManager.VERTICAL, R.drawable.recyclerview_divider));
        linearLayoutManager = new WrapContentLinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        PostRecyclerViewAdapter adapter = new PostRecyclerViewAdapter(PostListContent.POSTS, this, this, this);
        mRecyclerView.setAdapter(adapter);

        // 为 RecyclerView 设置触摸监听器
        mRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@androidx.annotation.NonNull  RecyclerView rv, @androidx.annotation.NonNull  MotionEvent e) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) rv.getLayoutManager();
                if (layoutManager == null) return false;

                // 获取第一个可见的 item position（可能不完全可见）
                int firstVisiblePos = layoutManager.findFirstVisibleItemPosition();
                if (firstVisiblePos == RecyclerView.NO_POSITION) return false;

                View firstVisibleView = layoutManager.findViewByPosition(firstVisiblePos);
                if (firstVisibleView == null) return false;

                View targetView = firstVisibleView.findViewById(R.id.post_author);
                if (targetView == null) return false;

                // 计算 targetView 在屏幕上的坐标范围
                int[] location = new int[2];
                targetView.getLocationOnScreen(location);
                int left = location[0];
                int top = location[1];
                int right = left + targetView.getWidth();
                int bottom = top + targetView.getHeight();

                // 判断是否点击了 targetView 区域
                int x = (int) e.getRawX();
                int y = (int) e.getRawY();
                if (x >= left && x <= right && y >= top && y <= bottom) {
                    // 点击的是第一个可见 item 的 post_author，不交给 GestureDetector
                    return false;
                }

                mGestureDetector.onTouchEvent(e);
                return false;
            }

            @Override
            public void onTouchEvent(@androidx.annotation.NonNull  RecyclerView rv, @androidx.annotation.NonNull MotionEvent e) {
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            }
        });

        //  holder.mView.setOnTouchListener(this); so the event will be sent from holder.mView
        mGestureDetector = new GestureDetector(this, new RecyclerViewGestureListener(this, mRecyclerView,PostListActivity.this));
        //mGestureDetector = new GestureDetector(this, new SwipeBackGestureListener(this, mRecyclerView));
        if(SMTHApplication.ReadMode1.equals(mReadMode)){
            // set onClick Lisetner for page navigator buttons

            findViewById(R.id.post_list_first_page).setOnClickListener(this);
            findViewById(R.id.post_list_pre_page).setOnClickListener(this);
            findViewById(R.id.post_list_next_page).setOnClickListener(this);
            findViewById(R.id.post_list_last_page).setOnClickListener(this);
            findViewById(R.id.post_list_go_page).setOnClickListener(this);


            LinearLayout navLayout = findViewById(R.id.post_list_action_layout);
            if (Settings.getInstance().hasPostNavBar()) {
                navLayout.setVisibility(View.VISIBLE);
            } else {
                navLayout.setVisibility(View.GONE);
            }
            initPostNavigationButtons();

            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                boolean isSlidingToLast = false;
                //int mIndex = 0;

                @Override
                public void onScrollStateChanged(@androidx.annotation.NonNull RecyclerView recyclerView, int newState) {
                    LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();

                    super.onScrollStateChanged(recyclerView, newState);
                    if (recyclerView.getLayoutManager() != null) {
                        getPositionAndOffset();
                    }
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        if (mRefreshLayout.isLoading()) {
                            mRefreshLayout.finishLoadMore();
                        }
                    }
                    if(!Settings.getInstance().isautoloadmore()) {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            loadMoreIfNeeded(recyclerView, manager);
                            updatePageInfo(recyclerView, Objects.requireNonNull(manager));
                        }
                    }
                }

                @Override
                public void onScrolled(@androidx.annotation.NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if(dy > 0){
                        isSlidingToLast = true;
                        LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
                        loadMoreIfNeeded(recyclerView, manager);
                    }
                }

                private void loadMoreIfNeeded(RecyclerView recyclerView, LinearLayoutManager manager) {
                    if (isLoading) {
                        return;
                    }

                    if (manager == null) {
                        return;
                    }
                    int lastVisiblePos = manager.findLastVisibleItemPosition();
                    int totalItemCount = manager.getItemCount();
                    int threshold = 3; // 预加载阈值，可根据实际情况调整

                    // 当接近列表底部时，预加载下一页数据
                    if (lastVisiblePos >= totalItemCount - threshold && mCurrentPageNo < mTopic.getTotalPageNo()) {
                        isLoading = true;
                        mCurrentPageNo++;
                        loadPostListByPages();
                    }
                }

                private void updatePageInfo(RecyclerView recyclerView, LinearLayoutManager manager) {
                    if (manager == null) {
                        return;
                    }
                    int lastVisiblePos = manager.findLastVisibleItemPosition();
                    int totalItemCount = manager.getItemCount();
                    // reach bottom
                    if (lastVisiblePos == (totalItemCount - 1) && isSlidingToLast && (mCurrentPageNo < mTopic.getTotalPageNo())) {
                        LoadMoreItems();
                    } else
                    if (lastVisiblePos == (totalItemCount - 1) && isSlidingToLast && (mCurrentPageNo == mTopic.getTotalPageNo())) {
                        clearLoadingHints();
                    } else if ((!isSlidingToLast) || (lastVisiblePos < (totalItemCount - 1))) {
                        //TextView mIndexView = (Objects.requireNonNull(manager.findViewByPosition(lastVisiblePos))).findViewById(R.id.post_index);
                        View lastVisibleView = manager.findViewByPosition(lastVisiblePos);
                        if (lastVisibleView != null) {
                            TextView mIndexView = lastVisibleView.findViewById(R.id.post_index);
                            if (mIndexView != null) {
                                String temp = mIndexView.getText().toString();
                                int mIndex;
                                if (temp.equals("楼主")) {
                                    mIndex = 0;
                                } else {
                                    String newTemp = temp.replaceAll("第", "");
                                    temp = newTemp.replaceAll("楼", "");
                                    mIndex = Integer.parseInt(temp);
                                }
                                mCurrentPageNo = mIndex / POST_PER_PAGE + 1;
                                String title = String.format(Locale.CHINA, "[%d/%d] %s", mCurrentPageNo, mTotalPageNo, mTopic.getTitle());
                                mTitle.setText(title);
                                mPageNo.setText(String.format(Locale.CHINA, "%d", mCurrentPageNo));
                                mCurrentReadPageNo = mCurrentPageNo;
                            }
                        }
                    }
                    else{
                        Log.d(TAG, mCurrentPageNo +"-"+ mTotalPageNo);
                    }
                    isLoading = false;
                }
            });
        } else{
            LinearLayout navLayout = findViewById(R.id.post_list_action_layout);
            navLayout.setVisibility(View.GONE);

            findViewById(R.id.post_list_first_page).setVisibility(View.GONE);
            findViewById(R.id.post_list_pre_page).setVisibility(View.GONE);
            findViewById(R.id.post_list_next_page).setVisibility(View.GONE);
            findViewById(R.id.post_list_last_page).setVisibility(View.GONE);
            findViewById(R.id.post_list_go_page).setVisibility(View.GONE);
            findViewById(R.id.post_list_page_no).setVisibility(View.GONE);


        }

        if (mTopic == null || !mTopic.getTopicID().equals(topic.getTopicID()) || PostListContent.POSTS.isEmpty()) {
            // new topic, different topic, or no post loaded
            mTopic = topic;
            mFilterUser = null;

            if(SMTHApplication.ReadMode1.equals(mReadMode)){
                reloadPostList();
            }
            else{
                mTitle.setText(mTopic.getTitle());
                reloadPostListMobile();
            }
            //setTitle(mTopic.getBoardChsName() + " - 阅读文章");
        }
        else
        {
            mTopic = topic;
            mFilterUser = null;
            //setTitle(mTopic.getBoardChsName() + " - 阅读文章");
            if(!Settings.getInstance().isOpenTopicAdd()) {
                reloadPostList();
            }
            else {
                String title = String.format(Locale.CHINA,"[%d/%d] %s", mCurrentReadPageNo, mTotalPageNo, mTopic.getTitle());
                mTitle.setText(title);
                scrollToPosition();
                mCurrentPageNo =mCurrentReadPageNo;
                clearLoadingHints();
            }
        }
        mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // 添加 RecyclerView 从右往左的动画
                mRecyclerView.setTranslationX(mRecyclerView.getWidth()); // 初始位置在屏幕右侧

                mRecyclerView.setAlpha(0f); // 初始透明度为0
                mRecyclerView.animate()
                        .translationX(0)
                        .alpha(1f) // 最终透明度为1
                        .setDuration(300)
                        .setStartDelay(50)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start();

                mRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

    }

    public void initPostNavigationButtons() {
        int alphaValue = 50;

        ImageButton imageButton;
        imageButton = findViewById(R.id.post_list_action_top);
        imageButton.setImageAlpha(alphaValue);
        imageButton.setOnClickListener(this);

        imageButton = findViewById(R.id.post_list_action_up);
        imageButton.setImageAlpha(alphaValue);
        imageButton.setOnClickListener(this);

        imageButton = findViewById(R.id.post_list_action_down);
        imageButton.setImageAlpha(alphaValue);
        imageButton.setOnClickListener(this);

        imageButton = findViewById(R.id.post_list_action_bottom);
        imageButton.setImageAlpha(alphaValue);
        imageButton.setOnClickListener(this);
    }

    public void clearLoadingHints() {
        dismissProgress();

        if (mRefreshLayout.isRefreshing()) {
            mRefreshLayout.finishRefresh(100);
        }
        if (mRefreshLayout.isLoading()) {
            mRefreshLayout.finishLoadMore(100);
        }
    }

    public void InsertPostListWithoutAlert() {

        if(mCurrentPageNo != 1) {
            mCurrentPageNo = mCurrentPageNo-1;
            loadPostListByPagesNew();
        }
        else {
            //  loadPostListByPages();
            clearLoadingHints();
            //Toast.makeText(PostListActivity.this, "已在首页", Toast.LENGTH_SHORT).show();
            NewToast.makeText(PostListActivity.this, "已在首页", Toast.LENGTH_SHORT);
        }
    }

    public void reloadPostListWithoutAlertNew() {
        if (isLoading) {
            return;
        }
        //PostListContent.clear();
        //mRecyclerView.getAdapter().notifyDataSetChanged();
        //Two scenarios here:
        //Case 1: if already on last item then check next page
        //case 2: if not yet on the last item of this page. then check this page.
        String temp = PostListContent.POSTS.get(PostListContent.POSTS.size()-1).getPosition();

        int Index ;
        if (temp.equals("楼主")) {
            Index = 0;
        } else {
            String newTemp = temp.replaceAll("第", "");
            temp = newTemp.replaceAll("楼", "");
            Index = Integer.parseInt(temp);
        }
        int tmpIndex = Index % POST_PER_PAGE;
        if (mCurrentPageNo == mTopic.getTotalPageNo() && tmpIndex<7 ) {
            loadnextpost();
        } else if( mCurrentPageNo == mTopic.getTotalPageNo() && tmpIndex==7)  {
            mCurrentPageNo += 1;
            loadnextpost();
        }
        else
        {
            clearLoadingHints();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void reloadPostListWithoutAlert() {
        PostListContent.clear();
        Objects.requireNonNull(mRecyclerView.getAdapter()).notifyDataSetChanged();
        loadPostListByPages();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void reloadPostListWithoutAlertMobile() {
        PostListContent.clear();
        Objects.requireNonNull(mRecyclerView.getAdapter()).notifyDataSetChanged();
        loadPostListByPagesMobile();
    }

    public void reloadPostListMobile() {
        //showProgress("加载文章中, 请稍候...");

        reloadPostListWithoutAlertMobile();
    }
    public void reloadPostList() {
        //showProgress("加载文章中, 请稍候...");

        reloadPostListWithoutAlert();
    }

    public void loadnextpost() {
        final SMTHHelper helper = SMTHHelper.getInstance();

        helper.wService.getPostListByPage(mTopic.getTopicURL(), mTopic.getTopicID(), mCurrentPageNo, mFilterUser)
                .flatMap((Function<ResponseBody, Observable<Post>>) responseBody -> {
                    try {
                        String response = responseBody.string();

                        List<Post> posts = SMTHHelper.ParsePostListFromWWW(response, mTopic);
                        if(posts.isEmpty()) {
                            return Observable.empty(); //handle error case
                        }
                        if(!SMTHApplication.ReadRec) {
                            SMTHApplication.ReadPostFirst = posts.get(0);
                            SMTHApplication.ReadRec=true;
                        }
                        return Observable.fromIterable(posts);
                    } catch (Exception e) {
                        SMTHApplication.ReadRec=false;
                        SMTHApplication.ReadPostFirst=null;
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                    return null;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Post>() {
                    private final List<Post> newPosts = new ArrayList<>();

                    @Override public void onSubscribe(@NonNull Disposable disposable) {

                    }

                    @Override public void onNext(@NonNull Post post) {

                        String temp = PostListContent.POSTS.get(PostListContent.POSTS.size()-1).getPosition();
                        int Index ;
                        if (temp.equals("楼主")) {
                            Index = 0;
                        } else {
                            String newTemp = temp.replaceAll("第", "");
                            temp = newTemp.replaceAll("楼", "");
                            Index = Integer.parseInt(temp);
                        }

                        // Log.d(TAG, post.toString());
                        temp = post.getPosition();
                        int mIndex ;
                        if (temp.equals("楼主")) {
                            mIndex = 0;
                        } else {
                            String newTemp = temp.replaceAll("第", "");
                            temp = newTemp.replaceAll("楼", "");
                            mIndex = Integer.parseInt(temp);
                        }
                        if(mIndex > Index)
                        {
                            PostListContent.addItem(post);
                            // mRecyclerView.getAdapter().notifyItemInserted(PostListContent.POSTS.size()-1);
                            Objects.requireNonNull(mRecyclerView.getAdapter()).notifyItemInserted(mIndex);
                        }

                    }

                    @Override public void onError(@NonNull Throwable e) {
                        clearLoadingHints();
                        //Toast.makeText(SMTHApplication.getAppContext(), "加载失败！\n" + e.toString(), Toast.LENGTH_SHORT).show();
                        NewToast.makeText(SMTHApplication.getAppContext(), "加载失败！\n" + e.toString(), Toast.LENGTH_SHORT);
                    }

                    @Override public void onComplete() {
                        String temp = PostListContent.POSTS.get(PostListContent.POSTS.size()-1).getPosition();
                        int Index ;
                        if (temp.equals("楼主")) {
                            Index = 0;
                        } else {
                            String newTemp = temp.replaceAll("第", "");
                            temp = newTemp.replaceAll("楼", "");
                            Index = Integer.parseInt(temp);
                        }
                        if(Index == mTotalPageNo*POST_PER_PAGE-1) {
                            mCurrentPageNo -= 1;
                            //Toast.makeText(SMTHApplication.getAppContext(),"没有新数据",Toast.LENGTH_SHORT).show();
                            NewToast.makeText(SMTHApplication.getAppContext(),"没有新数据",Toast.LENGTH_SHORT);
                        }
                        else if(Index >= mTotalPageNo*POST_PER_PAGE) {
                            mTotalPageNo += 1;
                            mTopic.setTotalPageNo(mTotalPageNo);
                            //Toast.makeText(SMTHApplication.getAppContext(),"没有新数据",Toast.LENGTH_SHORT).show();
                            NewToast.makeText(SMTHApplication.getAppContext(),"没有新数据",Toast.LENGTH_SHORT);
                        }
                        String title = String.format(Locale.CHINA,"[%d/%d] %s", mCurrentPageNo, mTotalPageNo, mTopic.getTitle());
                        mTitle.setText(title);
                        mPageNo.setText(String.format(Locale.CHINA,"%d", mCurrentPageNo));
                        mCurrentReadPageNo = mCurrentPageNo;
                        //mRecyclerView.getAdapter().notifyItemInserted(PostListContent.POSTS.size()-1);
                        clearLoadingHints();

                        //Special User OFFLINE case: [] or [Category 第一页:]
                        if (PostListContent.POSTS.isEmpty()) {
                            //Toast.makeText(SMTHApplication.getAppContext(),"请重新登录-"+ PostListContent.POSTS.size()+"-!",Toast.LENGTH_SHORT).show();
                            PostListContent.clear();

                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                try {
                                    //Toast.makeText(PostListActivity.this,"若登录请稍等，然后重新进入帖子页面",Toast.LENGTH_SHORT).show();
                                    NewToast.makeText(PostListActivity.this,"若登录请稍等，然后重新进入帖子页面",Toast.LENGTH_SHORT);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error occurred during delayed operation: ", e);
                                }
                                finish();
                            }, 500);
                        }
                        isLoading = false;
                    }
                });
    }
    public void loadPostListByPagesMobile() {
        final SMTHHelper helper = SMTHHelper.getInstance();

        helper
                .mService
                .getPostListByPage(mTopic.getTopicURL(), mTopic.getTopicID())
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@androidx.annotation.NonNull Call<ResponseBody> call, @androidx.annotation.NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String htmlContent;
                            try {
                                htmlContent = response.body().string();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            Post post = SMTHHelper.ParsePostListFromWWWMobile(htmlContent, mTopic);
                            PostListContent.clear();
                            PostListContent.addItem(post);
                            clearLoadingHints();
                            Objects.requireNonNull(mRecyclerView.getAdapter()).notifyItemInserted(0);

                        } else {
                            // NULL
                            clearLoadingHints();
                            /*
                            Toast.makeText(
                                            SMTHApplication.getAppContext(),
                                            "加载失败！\n" ,
                                            Toast.LENGTH_SHORT)
                                    .show();
                            */
                            NewToast.makeText(
                                    SMTHApplication.getAppContext(),
                                    "加载失败！\n" ,
                                    Toast.LENGTH_SHORT);
                            isLoading = false;
                        }
                    }

                    @Override
                    public void onFailure(@androidx.annotation.NonNull Call<ResponseBody> call, @androidx.annotation.NonNull Throwable t) {
                        Log.e(TAG, Objects.requireNonNull(t.getMessage()));
                    }
                });
    }
    public void loadPostListByPages() {
        final SMTHHelper helper = SMTHHelper.getInstance();

        helper
                .wService
                .getPostListByPage(mTopic.getTopicURL(), mTopic.getTopicID(), mCurrentPageNo, mFilterUser)
                .flatMap(
                        (Function<ResponseBody, Observable<Post>>) responseBody -> {
                            try {
                                String response = responseBody.string();
                                List<Post> posts = SMTHHelper.ParsePostListFromWWW(response, mTopic);
                                if (posts.isEmpty()) {
                                    return Observable.empty(); // handle error case
                                }
                                if (!SMTHApplication.ReadRec) {
                                    SMTHApplication.ReadPostFirst = posts.get(0);
                                    SMTHApplication.ReadRec = true;
                                }
                                return Observable.fromIterable(posts);
                            } catch (Exception e) {
                                SMTHApplication.ReadRec = false;
                                SMTHApplication.ReadPostFirst = null;
                                Log.e(TAG, Log.getStackTraceString(e));
                            }
                            return null;
                        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Observer<Post>() {
                            @Override
                            public void onSubscribe(@NonNull Disposable disposable) {}

                            @Override
                            public void onNext(@NonNull Post post) {

                                String temp = post.getPosition();
                                int Index;
                                if (temp.equals("楼主")) {
                                    Index = 0;
                                } else {
                                    String newTemp = temp.replaceAll("第", "");
                                    temp = newTemp.replaceAll("楼", "");
                                    Index = Integer.parseInt(temp);
                                }
                                Index = Index % POST_PER_PAGE;
                                PostListContent.addItem(post);
                                Objects.requireNonNull(mRecyclerView.getAdapter()).notifyItemInserted(Index);

                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                clearLoadingHints();
                                /*
                                Toast.makeText(
                                                SMTHApplication.getAppContext(),
                                                "加载失败！\n" + e.toString(),
                                                Toast.LENGTH_SHORT)
                                        .show();
                                */
                                NewToast.makeText(
                                        SMTHApplication.getAppContext(),
                                        "加载失败！\n" + e.toString(),
                                        Toast.LENGTH_SHORT);
                                isLoading = false;
                            }

                            @Override
                            public void onComplete() {
                                mTotalPageNo = mTopic.getTotalPageNo();
                                String title =
                                        String.format(
                                                Locale.CHINA,
                                                "[%d/%d] %s",
                                                mCurrentPageNo,
                                                mTopic.getTotalPageNo(),
                                                mTopic.getTitle());
                                mTitle.setText(title);
                                mPageNo.setText(String.format(Locale.CHINA, "%d", mCurrentPageNo));
                                mCurrentReadPageNo = mCurrentPageNo;
                                clearLoadingHints();
                                SMTHApplication.deletionCount++;
                                isLoading = false;

                                // 数据加载完成后恢复位置
                                if (!PostListContent.POSTS.isEmpty()) {
                                    scrollToPosition();
                                }


                                // 确保 RecyclerView 刷新
                                Objects.requireNonNull(mRecyclerView.getAdapter()).notifyDataSetChanged();

                                // Special User OFFLINE case: [] or [Category 第一页:]
                                if (PostListContent.POSTS.isEmpty()) {
                                    // Toast.makeText(SMTHApplication.getAppContext(),"请重新登录-"+
                                    // PostListContent.POSTS.size()+"-!",Toast.LENGTH_SHORT).show();

                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        Bundle req = getIntent().getExtras();
                                        String str = null;
                                        if(req != null) {
                                            str = req.getString(SMTHApplication.FROM_BOARD);
                                        }

                                        if (!SMTHApplication.isValidUser() && !Objects.equals(str, SMTHApplication.FROM_BOARD_HOT)) {
                                            Intent intent = new Intent(PostListActivity.this, LoginActivity.class);
                                            mActivityLoginResultLauncher.launch(intent);
                                        }
                                        else{
                                            if(Objects.equals(str, SMTHApplication.FROM_BOARD_HOT))
                                                //Toast.makeText(SMTHApplication.getAppContext(),"链接错误，请登录！",Toast.LENGTH_SHORT).show();
                                                NewToast.makeText(SMTHApplication.getAppContext(),"链接错误，请登录！",Toast.LENGTH_SHORT);
                                            else
                                                //Toast.makeText(SMTHApplication.getAppContext(),"链接错误，请刷新页面！",Toast.LENGTH_SHORT).show();
                                                NewToast.makeText(SMTHApplication.getAppContext(),"链接错误，请刷新页面！",Toast.LENGTH_SHORT);
                                        }
                                        finish();
                                    }, 500);

                                }
                            }
                        });
    }

    public void loadPostListByPagesNew() {
        final SMTHHelper helper = SMTHHelper.getInstance();

        helper
                .wService
                .getPostListByPage(mTopic.getTopicURL(), mTopic.getTopicID(), mCurrentPageNo, mFilterUser)
                .flatMap(
                        (Function<ResponseBody, Observable<Post>>) responseBody -> {
                            try {
                                String response = responseBody.string();
                                List<Post> posts = SMTHHelper.ParsePostListFromWWW(response, mTopic);
                                if (posts.isEmpty()) {
                                    return Observable.empty(); // handle error case
                                }
                                if (!SMTHApplication.ReadRec) {
                                    SMTHApplication.ReadPostFirst = posts.get(0);
                                    SMTHApplication.ReadRec = true;
                                }
                                return Observable.fromIterable(posts);
                            } catch (Exception e) {
                                SMTHApplication.ReadRec = false;
                                SMTHApplication.ReadPostFirst = null;
                                Log.e(TAG, Log.getStackTraceString(e));
                            }
                            return null;
                        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Observer<Post>() {
                            @Override
                            public void onSubscribe(@NonNull Disposable disposable) {}

                            @Override
                            public void onNext(@NonNull Post post) {
                                String temp = post.getPosition();
                                int Index;
                                if (temp.equals("楼主")) {
                                    Index = 0;
                                } else {
                                    String newTemp = temp.replaceAll("第", "");
                                    temp = newTemp.replaceAll("楼", "");
                                    Index = Integer.parseInt(temp);
                                }

                                Index = Index % POST_PER_PAGE;

                                PostListContent.InsertItem(Index, post);
                                Objects.requireNonNull(mRecyclerView.getAdapter()).notifyItemInserted(Index);


                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                clearLoadingHints();
                                /*
                                Toast.makeText(
                                                SMTHApplication.getAppContext(),
                                                "加载失败！\n" + e.toString(),
                                                Toast.LENGTH_SHORT)
                                        .show();
                                */
                                NewToast.makeText(
                                        SMTHApplication.getAppContext(),
                                        "加载失败！\n" + e.toString(),
                                        Toast.LENGTH_SHORT);
                                isLoading = false;

                            }

                            @Override
                            public void onComplete() {
                                mTotalPageNo = mTopic.getTotalPageNo();
                                String title =
                                        String.format(
                                                Locale.CHINA,
                                                "[%d/%d] %s",
                                                mCurrentPageNo,
                                                mTopic.getTotalPageNo(),
                                                mTopic.getTitle());
                                mTitle.setText(title);
                                mPageNo.setText(String.format(Locale.CHINA, "%d", mCurrentPageNo));
                                mCurrentReadPageNo = mCurrentPageNo;
                                clearLoadingHints();
                                isLoading = false;
                                // 确保 RecyclerView 刷新
                                Objects.requireNonNull(mRecyclerView.getAdapter()).notifyDataSetChanged();

                                // Special User OFFLINE case: [] or [Category 第一页:]

                                if (PostListContent.POSTS.isEmpty()) {
                                    // Toast.makeText(SMTHApplication.getAppContext(),"请重新登录-"+
                                    // PostListContent.POSTS.size()+"-!",Toast.LENGTH_SHORT).show();
                                    PostListContent.clear();

                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        try {
                                            if (!SMTHApplication.isValidUser()) {
                                                Intent intent = new Intent(PostListActivity.this, LoginActivity.class);
                                                mActivityLoginResultLauncher.launch(intent);
                                            } else {
                                                //Toast.makeText(PostListActivity.this, "链接错误，请刷新页面！", Toast.LENGTH_SHORT).show();
                                                NewToast.makeText(PostListActivity.this, "链接错误，请刷新页面！", Toast.LENGTH_SHORT);
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error occurred during delayed operation: ", e);
                                        }
                                        finish();
                                    }, 500);

                                }
                            }
                        });
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        // http://stackoverflow.com/questions/10692755/how-do-i-hide-a-menu-item-in-the-actionbar
        getMenuInflater().inflate(R.menu.post_list_menu, menu);

        MenuItem item = menu.findItem(R.id.post_list_action_enter_board);
        if (SMTHApplication.FROM_BOARD_BOARD.equals(mFrom)) {
            // from BoardTopicActivity
            item.setVisible(false);
        } else if (SMTHApplication.FROM_BOARD_HOT.equals(mFrom)) {
            // from HotTopicFragment
            item.setVisible(true);
        }
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            PostListActivity.this.finish();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            return true;
        } else if (id == R.id.post_list_action_refresh) {
            reloadPostList();
        } else if (id == R.id.post_list_action_enter_board) {
            Board board = new Board();
            board.initAsBoard(mTopic.getBoardChsName(),mTopic.getBoardEngName(), "", "");
            Intent intent = new Intent(this, BoardTopicActivity.class);
            intent.putExtra(SMTHApplication.BOARD_OBJECT, (Parcelable) board);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }


    @SuppressLint("NonConstantResourceId")
    @Override public void onClick(View v) {
        // page navigation buttons
        int id = v.getId();
        if (id == R.id.post_list_first_page) {
            mCurrentPageNo = 1;
            reloadPostList();
            /*
            if (!Settings.getInstance().isautoloadmore()) {
                if (mCurrentPageNo == 1) {
                    Toast.makeText(PostListActivity.this, "已在首页！", Toast.LENGTH_SHORT).show();
                } else {
                    mCurrentPageNo = 1;
                    reloadPostList();
                }
            } else {
                mCurrentPageNo = 1;
                reloadPostList();
            }
            */
        } else if (id == R.id.post_list_pre_page) {
            if (!Settings.getInstance().isautoloadmore()) {
                if (mCurrentPageNo == 1) {
                    //Toast.makeText(PostListActivity.this, "已在首页！", Toast.LENGTH_SHORT).show();
                    NewToast.makeText(PostListActivity.this, "已在首页！", Toast.LENGTH_SHORT);
                } else {
                    mCurrentPageNo -= 1;
                    reloadPostList();
                }
            } else {
                if (mCurrentPageNo == 1) {
                    reloadPostList();
                } else {
                    mCurrentPageNo -= 1;
                    reloadPostList();
                }
            }
        } else if (id == R.id.post_list_next_page) {
            goToNextPage();
        } else if (id == R.id.post_list_last_page) {
            mCurrentPageNo = mTotalPageNo;
            reloadPostList();
            /*
            if (!Settings.getInstance().isautoloadmore()) {
                if (mCurrentPageNo == mTopic.getTotalPageNo() || mCurrentReadPageNo == mTotalPageNo) {
                    Toast.makeText(PostListActivity.this, "已在末页！", Toast.LENGTH_SHORT).show();
                } else {
                    //mCurrentPageNo = mTopic.getTotalPageNo();
                    mCurrentPageNo = mTotalPageNo;
                    reloadPostList();
                }
            } else {
                mCurrentPageNo = mTotalPageNo;
                reloadPostList();
            }
            */
        } else if (id == R.id.post_list_go_page) {
            int pageNo;
            pageNo = Integer.parseInt(mPageNo.getText().toString());
            if (!Settings.getInstance().isautoloadmore()) {
                try {
                    if (mCurrentPageNo == pageNo) {
                        //Toast.makeText(PostListActivity.this, String.format(Locale.CHINA, "已在第%d页！", pageNo), Toast.LENGTH_SHORT).show();
                        NewToast.makeText(PostListActivity.this, String.format(Locale.CHINA, "已在第%d页！", pageNo), Toast.LENGTH_SHORT);
                    } else if (pageNo >= 1 && pageNo <= mTopic.getTotalPageNo()) {
                        mCurrentPageNo = pageNo;
                        // turn off keyboard
                        mPageNo.clearFocus();
                        InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        im.hideSoftInputFromWindow(mPageNo.getWindowToken(), 0);
                        // jump now
                        reloadPostList();
                    } else {
                        //Toast.makeText(PostListActivity.this, "非法页码！", Toast.LENGTH_SHORT).show();
                        NewToast.makeText(PostListActivity.this, "非法页码！", Toast.LENGTH_SHORT);
                    }
                } catch (Exception e) {
                    //Toast.makeText(PostListActivity.this, "非法输入！", Toast.LENGTH_SHORT).show();
                    NewToast.makeText(PostListActivity.this, "非法输入！", Toast.LENGTH_SHORT);
                }
            } else {
                try {
                    if (mCurrentPageNo == pageNo) {
                        reloadPostList();
                    } else if (pageNo >= 1 && pageNo <= mTopic.getTotalPageNo()) {
                        mCurrentPageNo = pageNo;
                        // turn off keyboard
                        mPageNo.clearFocus();
                        InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        im.hideSoftInputFromWindow(mPageNo.getWindowToken(), 0);
                        // jump now
                        reloadPostList();
                    } else {
                        //Toast.makeText(PostListActivity.this, "非法页码！", Toast.LENGTH_SHORT).show();
                        NewToast.makeText(PostListActivity.this, "非法页码！", Toast.LENGTH_SHORT);
                    }
                } catch (Exception e) {
                    //Toast.makeText(PostListActivity.this, "非法输入！", Toast.LENGTH_SHORT).show();
                    NewToast.makeText(PostListActivity.this, "非法输入！", Toast.LENGTH_SHORT);
                }
            }
        } else if (id == R.id.post_list_action_top) {
            mRecyclerView.scrollToPosition(0);
        } else if (id == R.id.post_list_action_up) {
            int prevPos = linearLayoutManager.findFirstVisibleItemPosition() - 1;
            if (prevPos >= 0) {
                mRecyclerView.smoothScrollToPosition(prevPos);
            }
        } else if (id == R.id.post_list_action_down) {
            int nextPos = linearLayoutManager.findLastVisibleItemPosition() + 1;
            if (nextPos < Objects.requireNonNull(mRecyclerView.getAdapter()).getItemCount()) {
                mRecyclerView.smoothScrollToPosition(nextPos);
            }
        } else if (id == R.id.post_list_action_bottom) {
            mRecyclerView.scrollToPosition(Objects.requireNonNull(mRecyclerView.getAdapter()).getItemCount() - 1);
        }
    }

    public void goToNextPage() {
        if(!Settings.getInstance().isautoloadmore()) {
            if (mCurrentPageNo == mTopic.getTotalPageNo()) {
                //Toast.makeText(PostListActivity.this, "已在末页！", Toast.LENGTH_SHORT).show();
                NewToast.makeText(PostListActivity.this, "已在末页！", Toast.LENGTH_SHORT);
                clearLoadingHints();
            } else {
                mCurrentPageNo += 1;
                reloadPostList();
            }
        }
        else{
            if (mCurrentPageNo == mTopic.getTotalPageNo()) {
                reloadPostList();
            } else if(mCurrentPageNo < mTopic.getTotalPageNo()) {
                mCurrentPageNo += 1;
                reloadPostList();
            }
        }
    }


    public void LoadMoreItems() {

        if (mCurrentPageNo == mTopic.getTotalPageNo()) {
            //Toast.makeText(PostListActivity.this, "已在末页！", Toast.LENGTH_SHORT).show();
            NewToast.makeText(PostListActivity.this, "已在末页！", Toast.LENGTH_SHORT);
            clearLoadingHints();
        } else {
            synchronized (this) {
                mCurrentPageNo += 1;
                //showProgress("加载文章中, 请稍候...");
                loadPostListByPages();
            }
        }
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



    @Override public void onItemLongClicked(final int position, View v) {
    /*
    if (position == RecyclerView.NO_POSITION || position >= PostListContent.POSTS.size()) return;
      //showPostActionMenu(position);
    //Log.d(TAG, String.format(Locale.CHINA,"Post by %s is long clicked", PostListContent.POSTS.get(position).getAuthor()));
    */
    }
    public void onItemLeftClicked(final int position, View v) {
    }
    public void onItemRightClicked(final int position, View v) {
    }

    public void onItemBottomClicked(final int position, View v) {
        LinearLayoutManager manager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        if (manager == null) return;

        int lastVisiblePos = manager.findLastVisibleItemPosition();
        int itemCount = manager.getItemCount();

        if (lastVisiblePos < itemCount - 1) {
            int targetPos = Math.min(itemCount - 1, lastVisiblePos + 1);
            mRecyclerView.scrollToPosition(targetPos);
        } else {
            // 如果已经到达最后一个 item，提示“已到达底部”
            //Toast.makeText(v.getContext(), "已到达底部", Toast.LENGTH_SHORT).show();
            NewToast.makeText(v.getContext(), "已到达底部", Toast.LENGTH_SHORT);
            mRecyclerView.scrollToPosition(lastVisiblePos);
        }
    }



    public void onItemTopClicked(final int position, View v) {
        LinearLayoutManager manager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        if (manager == null) {
            return;
        }


        int firstVisiblePos = manager.findFirstVisibleItemPosition();
        int lastVisiblePos = manager.findLastVisibleItemPosition();

        if (firstVisiblePos > 0) {
            int screenItemCount = lastVisiblePos - firstVisiblePos + 1;
            int targetPos = Math.max(0, firstVisiblePos - screenItemCount);
            mRecyclerView.scrollToPosition(targetPos);
        }

    }

    private void onPostPopupMenuItem(int position, int which) {
        //        Log.d(TAG, String.format(Locale.CHINA,"MenuItem %d was clicked", which));
        if (position >= PostListContent.POSTS.size()) {
            Log.e(TAG, "onPostPopupMenuItem: " + "Invalid Post index" + position);
            return;
        }

        Post post = PostListContent.POSTS.get(position);

        if(SMTHApplication.ReadMode0.equals(mReadMode)){
            if (which == 0) {
                // post_reply_post
                ComposePostContext postContext = new ComposePostContext();
                postContext.setBoardEngName(mTopic.getBoardEngName());
                postContext.setPostId(post.getPostID());
                postContext.setPostTitle(mTopic.getTitle());
                postContext.setPostAuthor(post.getRawAuthor());
                //postContext.setPostContent(post.getRawContent());
                postContext.setPostContent("");
                postContext.setComposingMode(ComposePostContext.MODE_REPLY_POST);

                Intent intent = new Intent(this, ComposePostActivity.class);
                intent.putExtra(SMTHApplication.COMPOSE_POST_CONTEXT, postContext);
                intent.putExtra(SMTHApplication.READ_MODE, mReadMode);
                mActivityPostResultLauncher.launch(intent);
            } else if (which == 1){
                deletePostMobile(post);
            }

        } else {

            if (which == 0) {
                // post_reply_post
                ComposePostContext postContext = new ComposePostContext();
                postContext.setBoardEngName(mTopic.getBoardEngName());
                postContext.setPostId(post.getPostID());
                postContext.setPostTitle(mTopic.getTitle());
                postContext.setPostAuthor(post.getRawAuthor());
                //postContext.setPostContent(post.getRawContent());
                postContext.setPostContent("");
                postContext.setComposingMode(ComposePostContext.MODE_REPLY_POST);

                Intent intent = new Intent(this, ComposePostActivity.class);
                intent.putExtra(SMTHApplication.READ_MODE, mReadMode);
                intent.putExtra(SMTHApplication.COMPOSE_POST_CONTEXT, postContext);
                mActivityPostResultLauncher.launch(intent);
            } else if (which == 1) {
                // like
                // Toast.makeText(PostListActivity.this, "Like:TBD", Toast.LENGTH_SHORT).show();
                PopupLikeWindow popup = new PopupLikeWindow();
                popup.initPopupWindow(this);
                popup.showAtLocation(mRecyclerView, Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 100);
            } else if (which == 2) {
                // post_reply_mail
                // Toast.makeText(PostListActivity.this, "回复到作者信箱:TBD", Toast.LENGTH_SHORT).show();
                ComposePostContext postContext = new ComposePostContext();
                postContext.setBoardEngName(mTopic.getBoardEngName());
                postContext.setPostId(post.getPostID());
                postContext.setPostTitle(mTopic.getTitle());
                postContext.setPostAuthor(post.getRawAuthor());
                //postContext.setPostContent(post.getRawContent());
                postContext.setPostContent("");
                postContext.setComposingMode(ComposePostContext.MODE_REPLY_MAIL);

                Intent intent = new Intent(this, ComposePostActivity.class);
                intent.putExtra(SMTHApplication.READ_MODE, mReadMode);
                intent.putExtra(SMTHApplication.COMPOSE_POST_CONTEXT, postContext);
                startActivity(intent);
            } else if (which == 3) {
                // post_query_author
                Intent intent = new Intent(this, QueryUserActivity.class);
                intent.putExtra(SMTHApplication.QUERY_USER_INFO, post.getRawAuthor());
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } else if (which == 4) {
                // read posts from current users only
                if (mFilterUser == null) {
                    //Toast.makeText(PostListActivity.this, "只看此ID! 再次选择将查看所有文章.", Toast.LENGTH_SHORT).show();
                    NewToast.makeText(PostListActivity.this, "只看此ID! 再次选择将查看所有文章.", Toast.LENGTH_SHORT);
                    mFilterUser = post.getRawAuthor();
                } else {
                    //Toast.makeText(PostListActivity.this, "查看所有文章!", Toast.LENGTH_SHORT).show();
                    NewToast.makeText(PostListActivity.this, "查看所有文章!", Toast.LENGTH_SHORT);
                    mFilterUser = null;
                }
                mCurrentPageNo = 1;
                reloadPostList();
            } else if (which == 5) {
                // copy post content
                // http://stackoverflow.com/questions/8056838/dealing-with-deprecated-android-text-clipboardmanager
                String content;
                if (post != null) {
                    content = post.getRawContent();
                    final ClipboardManager clipboardManager =
                            (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                    final ClipData clipData = ClipData.newPlainText("PostContent", content);
                    clipboardManager.setPrimaryClip(clipData);
                    //Toast.makeText(PostListActivity.this, "帖子内容已复制到剪贴板", Toast.LENGTH_SHORT).show();
                    NewToast.makeText(PostListActivity.this, "帖子内容已复制到剪贴板", Toast.LENGTH_SHORT);
                } else {
                    //Toast.makeText(PostListActivity.this, "复制失败！", Toast.LENGTH_SHORT).show();
                    NewToast.makeText(PostListActivity.this, "复制失败！", Toast.LENGTH_SHORT);
                }
            } else if (which == 6) {
                // post_foward_self
                // Toast.makeText(PostListActivity.this, "转寄信箱:TBD", Toast.LENGTH_SHORT).show();
                PopupForwardWindow popup = new PopupForwardWindow();
                popup.initPopupWindow(this, post);
                popup.showAtLocation(mRecyclerView, Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 100);
            } else if (which == 7) {
                // open post in browser
                String url = String.format(Locale.CHINA,SMTHHelper.SMTH_MOBILE_URL + "/article/%s/%s?p=%d", mTopic.getBoardEngName(), mTopic.getTopicID(), mCurrentPageNo);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } else if (which == 8) {
                // post_share
                // Toast.makeText(PostListActivity.this, "分享:TBD", Toast.LENGTH_SHORT).show();
                sharePost(post);
            } else if (which == 9) {
                // delete post
                deletePost(post);

            } else if (which == 10) {
                // edit post
                ComposePostContext postContext = new ComposePostContext();
                postContext.setBoardEngName(mTopic.getBoardEngName());
                postContext.setPostId(post.getPostID());
                postContext.setPostTitle(mTopic.getTitle());
                postContext.setPostAuthor(post.getRawAuthor());
                postContext.setPostContent(post.getRawContent());
                postContext.setComposingMode(ComposePostContext.MODE_EDIT_POST);

                Intent intent = new Intent(this, ComposePostActivity.class);
                intent.putExtra(SMTHApplication.COMPOSE_POST_CONTEXT, postContext);
                intent.putExtra(SMTHApplication.READ_MODE, mReadMode);
                startActivity(intent);
            } else if (which == 11) {
                // generate screenshot of current post
                View v = Objects.requireNonNull(mRecyclerView.getLayoutManager()).findViewByPosition(position);

                // convert title + post to image
                if (v == null) {
                    return;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if(Environment.isExternalStorageManager())
                    {
                        captureView(mTitle, v, post.getPostID());
                    }
                    else
                    {
                        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        startActivity(intent);
                    }
                } else{
                    captureView(mTitle, v, post.getPostID());
                }


            }
            else if (which == 12) {
                // post_reply_post
                ComposePostContext postContext = new ComposePostContext();
                postContext.setBoardEngName(mTopic.getBoardEngName());
                postContext.setPostId(SMTHApplication.ReadPostFirst.getPostID());
                postContext.setPostTitle(mTopic.getTitle());

                postContext.setPostAuthor(SMTHApplication.ReadPostFirst.getRawAuthor());
                postContext.setPostContent(SMTHApplication.ReadPostFirst.getRawContent());

                postContext.setComposingMode(ComposePostContext.MODE_REPLY_POST);

                Intent intent = new Intent(this, ComposePostActivity.class);
                intent.putExtra(SMTHApplication.READ_MODE, mReadMode);
                intent.putExtra(SMTHApplication.COMPOSE_POST_CONTEXT, postContext);
                //startActivityForResult(intent, ComposePostActivity.COMPOSE_ACTIVITY_REQUEST_CODE);
                mActivityPostResultLauncher.launch(intent);
            }
        }

    }

    public void captureView(View v1, View v2, String postID){
        //Create a Bitmap with the same dimensions
        Bitmap image = Bitmap.createBitmap(v1.getWidth(), v1.getHeight() + v2.getHeight(), Bitmap.Config.RGB_565);
        //Draw the view inside the Bitmap
        Canvas canvas = new Canvas(image);

        if(Settings.getInstance().isNightMode()) {
            canvas.drawColor(Color.BLACK);
        } else {
            canvas.drawColor(Color.WHITE);
        }
        v1.draw(canvas);
        canvas.translate(0, v1.getHeight());
        v2.draw(canvas);
        canvas.save();

        // save image to sdcard
        try {
            if (TextUtils.equals(Environment.getExternalStorageState(), Environment.MEDIA_MOUNTED)) {
                String path = Environment.getExternalStorageDirectory().getPath() + "/zSMTH-v/";
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                String IMAGE_FILE_PREFIX = "post-";
                String IMAGE_FILE_SUFFIX = ".jpg";
                File outFile = new File(dir, IMAGE_FILE_PREFIX + postID + IMAGE_FILE_SUFFIX);
                FileOutputStream out = new FileOutputStream(outFile);

                image.compress(Bitmap.CompressFormat.JPEG, 90, out); //Output
                //Toast.makeText(PostListActivity.this, "截图已存为: /zSMTH-v/" + outFile.getName(), Toast.LENGTH_SHORT).show();
                NewToast.makeText(PostListActivity.this, "截图已存为: /zSMTH-v/" + outFile.getName(), Toast.LENGTH_SHORT);

                // make sure the new file can be recognized soon
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(outFile)));
            }
        } catch (Exception e) {
            Log.e(TAG, "saveImageToFile: " + Log.getStackTraceString(e));
            //Toast.makeText(PostListActivity.this, "保存截图失败:\n请授予应用存储权限！\n" + e, Toast.LENGTH_LONG).show();
            NewToast.makeText(PostListActivity.this, "保存截图失败:\n请授予应用存储权限！\n" + e, Toast.LENGTH_LONG);
        }
    }

    public void deletePostMobile(Post post) {
        SMTHHelper helper = SMTHHelper.getInstance();

        if(post.getPostID() == null) return;

        Observable<Response<ResponseBody>> observable = helper.mService.deletePostMobile(mTopic.getBoardEngName(), post.getPostID(),1);

        observable
                .subscribeOn(Schedulers.io()) // 在 IO 线程发起网络请求
                .observeOn(AndroidSchedulers.mainThread()) // 回到主线程更新 UI
                .subscribe(new Observer<Response<ResponseBody>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        // 可选：显示加载进度条
                    }

                    @Override
                    public void onNext(@NonNull Response<ResponseBody> response) {
                        try {
                            if (response.isSuccessful() && response.body() != null) {
                                String html = SMTHHelper.DecodeResponseFromWWW(response.body().bytes());
                                String result = SMTHHelper.parseDeleteResponseMobile(html);

                                if ("删除成功".equals(result)) {
                                    SMTHApplication.bNewPost = true;
                                    //Toast.makeText(PostListActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                                    PostListActivity.this.finish();
                                } else {
                                    //Toast.makeText(PostListActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                                    NewToast.makeText(PostListActivity.this, "删除失败", Toast.LENGTH_SHORT);
                                    new Handler(Looper.getMainLooper()).postDelayed(PostListActivity.this::finish, 500); // 可以调整延迟时间
                                }
                            }
                        } catch (IOException e) {
                            Log.d(TAG, Objects.requireNonNull(e.getMessage()));
                            //Toast.makeText(PostListActivity.this, "解析失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            NewToast.makeText(PostListActivity.this, "解析失败: " + e.getMessage(), Toast.LENGTH_SHORT);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        //Toast.makeText(PostListActivity.this, "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        NewToast.makeText(PostListActivity.this, "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT);
                    }

                    @Override
                    public void onComplete() {
                        // 可选：隐藏加载进度条
                    }
                });
    }


    public void deletePost(Post post) {
        SMTHHelper helper = SMTHHelper.getInstance();

        helper.wService.deletePost(mTopic.getBoardEngName(), post.getPostID()).map(responseBody -> {
            try {
                String html = SMTHHelper.DecodeResponseFromWWW(responseBody.bytes());
                return SMTHHelper.parseDeleteResponseMobile(html);
            } catch (Exception e) {
                Log.e(TAG, "call: " + Log.getStackTraceString(e));
            }
            return null;
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<String>() {
            @Override public void onSubscribe(@NonNull Disposable disposable) {

            }

            @Override public void onNext(@NonNull String s) {
                //    Toast.makeText(PostListActivity.this, s, Toast.LENGTH_SHORT).show();
                if ("删除成功".equals(s)) {
                    SMTHApplication.bNewPost = true;
                    mCurrentPageNo = 1;
                    mCurrentReadPageNo = 1;
                    //Toast.makeText(PostListActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                    PostListActivity.this.finish();
                } else {
                    //Toast.makeText(PostListActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                    NewToast.makeText(PostListActivity.this, "删除失败", Toast.LENGTH_SHORT);
                    new Handler(Looper.getMainLooper()).postDelayed(PostListActivity.this::finish, 500); // 可以调整延迟时间
                }
            }

            @Override public void onError(@NonNull Throwable e) {
                //Toast.makeText(PostListActivity.this, "删除帖子失败！\n" + e.toString(), Toast.LENGTH_SHORT).show();
                NewToast.makeText(PostListActivity.this, "删除帖子失败！\n" + e.toString(), Toast.LENGTH_SHORT);

            }

            @SuppressLint("NotifyDataSetChanged")
            @Override public void onComplete() {

            }
        });
    }

    public void sharePost(Post post) {
        OnekeyShare oks = new OnekeyShare();
        //关闭sso授权
        oks.disableSSOWhenAuthorize();

        // prepare information from the post
        String title = String.format(Locale.CHINA,"[%s] %s @ 水木社区", mTopic.getBoardChsName(), mTopic.getTitle());
        String postURL =
                String.format(Locale.CHINA,SMTHHelper.SMTH_MOBILE_URL + "/article/%s/%s?p=%d", mTopic.getBoardEngName(), mTopic.getTopicID(), mCurrentPageNo);
        String content = String.format(Locale.CHINA,"[%s]在大作中写到: %s", post.getAuthor(), post.getRawContent());
        // the max length of webo is 140
        if (content.length() > 110) {
            content = content.substring(0, 110);
        }
        content += String.format(Locale.CHINA,"...\nLink:%s", postURL);

        // default: use zSMTH logo
        String imageURL = "http://zsmth-android.zfdang.com/zsmth.png";
        List<Attachment> attaches = post.getAttachFiles();
        if (attaches != null && !attaches.isEmpty()) {
            // use the first attached image
            imageURL = attaches.get(0).getResizedImageSource();
        }

        // more information about OnekeyShare
        // http://wiki.mob.com/docs/sharesdk/android/cn/sharesdk/onekeyshare/OnekeyShare.html

        // title标题，印象笔记、邮箱、信息、微信、人人网、QQ和QQ空间使用
        oks.setTitle(title);

        // titleUrl是标题的网络链接，仅在Linked-in,QQ和QQ空间使用
        // oks.setTitleUrl("http://sharesdk.cn");

        // text是分享文本，所有平台都需要这个字段
        oks.setText(content);

        // 分享网络图片，新浪微博分享网络图片需要通过审核后申请高级写入接口，否则请注释掉测试新浪微博
        // imageUrl是图片的网络路径，新浪微博、人人网、QQ空间和Linked-In支持此字段
        oks.setImageUrl(imageURL);

        // imagePath是图片的本地路径，Linked-In以外的平台都支持此参数
        //oks.setImagePath("/sdcard/test.jpg");//确保SDcard下面存在此张图片

        // url仅在微信（包括好友和朋友圈）中使用
        oks.setUrl(postURL);

        // comment是我对这条分享的评论，仅在人人网和QQ空间使用
        //        oks.setComment("我是测试评论文本");
        // site是分享此内容的网站名称，仅在QQ空间使用
        //        oks.setSite("ShareSDK");
        // siteUrl是分享此内容的网站地址，仅在QQ空间使用
        //        oks.setSiteUrl("http://sharesdk.cn");

        // set callback functions
        oks.setCallback(new PlatformActionListener() {
            @Override public void onComplete(Platform platform, int i, HashMap<String, Object> hashMap) {
                //Toast.makeText(PostListActivity.this, "分享成功!", Toast.LENGTH_SHORT).show();
                NewToast.makeText(PostListActivity.this, "分享成功!", Toast.LENGTH_SHORT);
            }

            @Override public void onError(Platform platform, int i, Throwable throwable) {
                //Toast.makeText(PostListActivity.this, "分享失败:\n" + throwable.toString(), Toast.LENGTH_SHORT).show();
                NewToast.makeText(PostListActivity.this, "分享失败:\n" + throwable.toString(), Toast.LENGTH_SHORT);
            }

            @Override public void onCancel(Platform platform, int i) {
            }
        });

        // 启动分享GUI
        oks.show(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override public boolean onTouch(View v, MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return false;
    }

    @Override public void OnLikeAction(String score, String msg) {
        //        Log.d(TAG, "OnLikeAction: " + score + msg);

        SMTHHelper helper = SMTHHelper.getInstance();
        helper.wService.addLike(mTopic.getBoardEngName(), mTopic.getTopicID(), score, msg, "")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<AjaxResponse>() {
                    @Override public void onSubscribe(@NonNull Disposable disposable) {

                    }

                    @Override public void onNext(@NonNull AjaxResponse ajaxResponse) {
                        // Log.d(TAG, "onNext: " + ajaxResponse.toString());
                        if (ajaxResponse.getAjax_st() == AjaxResponse.AJAX_RESULT_OK) {
                            //Toast.makeText(PostListActivity.this, ajaxResponse.getAjax_msg(), Toast.LENGTH_SHORT).show();
                            reloadPostList();
                        } else {
                            //Toast.makeText(PostListActivity.this, ajaxResponse.toString(), Toast.LENGTH_SHORT).show();
                            NewToast.makeText(PostListActivity.this, ajaxResponse.toString(), Toast.LENGTH_SHORT);
                        }
                    }

                    @Override public void onError(@NonNull Throwable e) {
                        //Toast.makeText(PostListActivity.this, "增加Like失败!\n" + e.toString(), Toast.LENGTH_SHORT).show();
                        NewToast.makeText(PostListActivity.this, "增加Like失败!\n" + e.toString(), Toast.LENGTH_SHORT);
                    }

                    @Override public void onComplete() {

                    }
                });
    }

    @Override public void OnForwardAction(Post post, String target, boolean threads, boolean noref, boolean noatt) {
        //        Log.d(TAG, "OnForwardAction: ");

        String strThreads = null;
        if (threads) strThreads = "on";
        String strNoref = null;
        if (noref) strNoref = "on";
        String strNoatt = null;
        if (noatt) strNoatt = "on";
        String strNoansi = null;
        if (target != null && target.contains("@")) strNoansi = "on";

        SMTHHelper helper = SMTHHelper.getInstance();
        helper.wService.forwardPost(mTopic.getBoardEngName(), post.getPostID(), target, strThreads, strNoref, strNoatt, strNoansi)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<AjaxResponse>() {
                    @Override public void onSubscribe(@NonNull Disposable disposable) {

                    }

                    @Override public void onNext(@NonNull AjaxResponse ajaxResponse) {
                        // Log.d(TAG, "onNext: " + ajaxResponse.toString());
                        if (ajaxResponse.getAjax_st() == AjaxResponse.AJAX_RESULT_OK) {
                            //Toast.makeText(PostListActivity.this, ajaxResponse.getAjax_msg(), Toast.LENGTH_SHORT).show();
                            NewToast.makeText(PostListActivity.this, ajaxResponse.getAjax_msg(), Toast.LENGTH_SHORT);
                        } else {
                            //Toast.makeText(PostListActivity.this, ajaxResponse.toString(), Toast.LENGTH_SHORT).show();
                            NewToast.makeText(PostListActivity.this, ajaxResponse.toString(), Toast.LENGTH_SHORT);
                        }
                    }

                    @Override public void onError(@NonNull Throwable e) {
                        //Toast.makeText(PostListActivity.this, "转寄失败！\n" + e.toString(), Toast.LENGTH_SHORT).show();
                        NewToast.makeText(PostListActivity.this, "转寄失败！\n" + e.toString(), Toast.LENGTH_SHORT);
                    }

                    @Override public void onComplete() {

                    }
                });
    }

    @Override public void OnRePostAction(Post post, String target, String outgo) {
        SMTHHelper helper = SMTHHelper.getInstance();
        helper.wService.repostPost(mTopic.getBoardEngName(), post.getPostID(), target, outgo).map(responseBody -> {
            try {
                String response = SMTHHelper.DecodeResponseFromWWW(responseBody.bytes());
                return SMTHHelper.parseRepostResponse(response);
            } catch (Exception e) {
                Log.e(TAG, "call: " + Log.getStackTraceString(e));
            }
            return null;
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<String>() {
            @Override public void onSubscribe(@NonNull Disposable disposable) {

            }

            @Override public void onNext(@NonNull String s) {
                //Toast.makeText(SMTHApplication.getAppContext(), s, Toast.LENGTH_SHORT).show();
                NewToast.makeText(SMTHApplication.getAppContext(), s, Toast.LENGTH_SHORT);
            }

            @Override public void onError(@NonNull Throwable e) {
                //Toast.makeText(SMTHApplication.getAppContext(), "发生错误:\n" + e.toString(), Toast.LENGTH_SHORT).show();
                NewToast.makeText(SMTHApplication.getAppContext(), "发生错误:\n" + e.toString(), Toast.LENGTH_SHORT);
            }

            @Override public void onComplete() {

            }
        });
    }

    @Override
    public void onItemBtnMoreClicked(int position, View view) {
        if (position == RecyclerView.NO_POSITION || position >= PostListContent.POSTS.size()) return;
        showPostActionMenu(position);
    }

    private void showPostActionMenu(int position) {
        //final Post post = PostListContent.POSTS.get(position);

        List<PostActionAlertDialogItem> menuItemList = new ArrayList<>();

        if (SMTHApplication.ReadMode0.equals(mReadMode)) {
            menuItemList.add(new PostActionAlertDialogItem(getString(R.string.post_reply_post), R.drawable.ic_reply_black_48dp));
            menuItemList.add(new PostActionAlertDialogItem(getString(R.string.post_delete_post), R.drawable.ic_delete_black_48dp));
        } else{
            menuItemList.add(new PostActionAlertDialogItem(getString(R.string.post_reply_post), R.drawable.ic_reply_black_48dp));
            menuItemList.add(new PostActionAlertDialogItem(getString(R.string.post_like_post), R.drawable.like_black));
            menuItemList.add(new PostActionAlertDialogItem(getString(R.string.post_reply_mail), R.drawable.ic_email_black_48dp));
            menuItemList.add(new PostActionAlertDialogItem(getString(R.string.post_query_author), R.drawable.ic_person_black_48dp));
            menuItemList.add(new PostActionAlertDialogItem(getString(R.string.post_filter_author), R.drawable.ic_find_in_page_black_48dp));
            menuItemList.add(new PostActionAlertDialogItem(getString(R.string.post_copy_content), R.drawable.ic_content_copy_black_48dp));
            menuItemList.add(new PostActionAlertDialogItem(getString(R.string.post_foward), R.drawable.ic_send_black_48dp));
            menuItemList.add(new PostActionAlertDialogItem(getString(R.string.post_view_in_browser), R.drawable.ic_open_in_browser_black_48dp));
            menuItemList.add(new PostActionAlertDialogItem(getString(R.string.post_share), R.drawable.ic_share_black_48dp));
            menuItemList.add(new PostActionAlertDialogItem(getString(R.string.post_delete_post), R.drawable.ic_delete_black_48dp));
            menuItemList.add(new PostActionAlertDialogItem(getString(R.string.post_edit_post), R.drawable.ic_edit_black_48dp));
            menuItemList.add(new PostActionAlertDialogItem(getString(R.string.post_convert_image), R.drawable.ic_photo_black_48dp));
            menuItemList.add(new PostActionAlertDialogItem(getString(R.string.post_reply_author), R.drawable.ic_reply_black_48dp));

        }

        final PostActionAlertDialogItem[] menuItems = menuItemList.toArray(new PostActionAlertDialogItem[0]);

        ListAdapter adapter = new ArrayAdapter<PostActionAlertDialogItem>(this, R.layout.post_popup_menu_item, menuItems) {
            ViewHolder holder;

            @androidx.annotation.NonNull
            @SuppressLint("InflateParams")
            public View getView(int position, View convertView, @androidx.annotation.NonNull ViewGroup parent) {
                final LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.post_popup_menu_item, null);

                    holder = new ViewHolder();
                    holder.mIcon = convertView.findViewById(R.id.post_popupmenu_icon);
                    holder.mTitle = convertView.findViewById(R.id.post_popupmenu_title);
                    convertView.setTag(holder);
                } else {
                    // view already defined, retrieve view holder
                    holder = (ViewHolder) convertView.getTag();
                }

                holder.mTitle.setText(menuItems[position].text);
                holder.mIcon.setImageResource(menuItems[position].icon);
                return convertView;
            }

            class ViewHolder {
                ImageView mIcon;
                TextView mTitle;
            }
        };

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.MyDialogStyle)
                .setTitle(getString(R.string.post_alert_title))
                .setAdapter(adapter, (dialog1, which) -> onPostPopupMenuItem(position, which))
                .create();

        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);
        dialog.setOnShowListener(dialogInterface -> {
            Window window = dialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams params = window.getAttributes();
                //params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.7);
                //params.width = WindowManager.LayoutParams.MATCH_PARENT; // 宽度铺满
                params.height = WindowManager.LayoutParams.WRAP_CONTENT; // 高度自适应
                window.setAttributes(params);
            }
        });

        dialog.show();
    }

    public void onItemBtnReplyClicked(final int position, View v) {
        // post_reply_post
        if (position >= PostListContent.POSTS.size()) {
            Log.e(TAG, "onItemRightClicked: " + "Invalid Post index" + position);
            return;
        }

        Post post = PostListContent.POSTS.get(position);
        ComposePostContext postContext = new ComposePostContext();
        postContext.setBoardEngName(mTopic.getBoardEngName());
        postContext.setPostId(post.getPostID());
        postContext.setPostTitle(mTopic.getTitle());
        postContext.setPostAuthor(post.getRawAuthor());
        if (Settings.getInstance().isQuickReply())
            postContext.setPostContent("");
        else
            postContext.setPostContent(post.getRawContent());
        postContext.setComposingMode(ComposePostContext.MODE_REPLY_POST);

        Intent intent = new Intent(this, ComposePostActivity.class);
        intent.putExtra(SMTHApplication.COMPOSE_POST_CONTEXT, postContext);
        intent.putExtra(SMTHApplication.READ_MODE,mReadMode);
        mActivityPostResultLauncher.launch(intent);
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPositionAndOffset();
    }
}