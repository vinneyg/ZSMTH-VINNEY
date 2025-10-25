package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
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
import android.view.ViewTreeObserver;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.helpers.NewToast;
import com.zfdang.zsmth_android.listeners.OnBoardFragmentInteractionListener;
import com.zfdang.zsmth_android.listeners.OnVolumeUpDownListener;
import com.zfdang.zsmth_android.models.Board;
import com.zfdang.zsmth_android.models.BoardListContent;
import com.zfdang.zsmth_android.newsmth.SMTHHelper;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnBoardFragmentInteractionListener}
 * interface.
 */
public class AllBoardFragment extends Fragment implements OnVolumeUpDownListener {

  private SmartRefreshLayout mRefreshLayout = null;
  private RecyclerView mRecyclerView = null;
  private QueryTextListener mQueryListener = null;

  private OnBoardFragmentInteractionListener mListener = null;
  private BoardRecyclerViewAdapter mAdapter = null;
  private Context mContext;
  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public AllBoardFragment() {
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_all_board, container, false);

    mRefreshLayout = view.findViewById(R.id.swipeRefreshLayoutAllBoard);
    mRefreshLayout.setEnableLoadMore(false);
    mRefreshLayout.setOnRefreshListener(refreshLayout ->  LoadAllBoards());


    mRecyclerView = view.findViewById(R.id.all_board_list);
    // Set the adapter
    if (mContext != null) {
      mRecyclerView.addItemDecoration(new DividerItemDecoration(mContext, LinearLayoutManager.VERTICAL, 0));
      mRecyclerView.setLayoutManager(new WrapContentLinearLayoutManager(mContext));
    }

    if (mAdapter == null) {
      // this is very important, we only create adapter on the first time.
      // otherwise, BoardListContent.ALL_BOARDS might be filtered result already
      mAdapter = new BoardRecyclerViewAdapter(BoardListContent.ALL_BOARDS, mListener);
    }
    mRecyclerView.setAdapter(mAdapter);

    mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        // 添加 RecyclerView 从右往左的动画
        mRecyclerView.setTranslationX((float) mRecyclerView.getWidth() /3); // 初始位置在屏幕右侧

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

    SearchView mSearchView = view.findViewById(R.id.all_board_search);
    mSearchView.setIconifiedByDefault(false);

    // http://stackoverflow.com/questions/11321129/is-it-possible-to-change-the-textcolor-on-an-android-searchview
    @SuppressWarnings("ResourceType")
    int id = mSearchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
    TextView textView = mSearchView.findViewById(id);
    textView.setTextColor(getResources().getColor(R.color.status_text_night,null));
    textView.setHintTextColor(getResources().getColor(R.color.status_text_night,null));

    if (mQueryListener == null) {
      mQueryListener = new QueryTextListener((BoardRecyclerViewAdapter) mRecyclerView.getAdapter());
    }
    mSearchView.setOnQueryTextListener(mQueryListener);

    // set focus to recyclerview
    mRecyclerView.requestFocus();

    if (BoardListContent.ALL_BOARDS.isEmpty()) {
      // only load boards on the first timer
      LoadAllBoards();
    }

    return view;
  }

  public void showLoadingHints() {
    MainActivity activity = (MainActivity) getActivity();
    if (activity != null) activity.showProgress("从缓存或网络加载所有版面，请耐心等待...");
  }

  public void clearLoadingHints() {
    // disable progress bar
    MainActivity activity = (MainActivity) getActivity();
    if (activity != null) activity.dismissProgress();
  }

  public void LoadAllBoardsWithoutCache() {
    SMTHHelper.ClearBoardListCache(SMTHHelper.BOARD_TYPE_ALL, null);
    LoadAllBoards();
  }

  public void LoadAllBoards() {
    showLoadingHints();

    // all boards loaded in cached file
    final Observable<List<Board>> cache = Observable.create(observableEmitter -> {
      List<Board> boards = SMTHHelper.LoadBoardListFromCache(SMTHHelper.BOARD_TYPE_ALL, null);
      if (boards != null && !boards.isEmpty()) {
        observableEmitter.onNext(boards);
      } else {
        observableEmitter.onComplete();
      }
    });

    // all boards loaded from network
    final Observable<List<Board>> network = Observable.create(observableEmitter -> {
      List<Board> boards = SMTHHelper.LoadAllBoardsFromWWW();
      if (!boards.isEmpty()) {
        observableEmitter.onNext(boards);
      } else {
        observableEmitter.onComplete();
      }
    });

    // use the first available source to load all boards
    List<Board> boards = new ArrayList<>();
    Observable.concat(cache, network).first(boards).toObservable().flatMap((Function<List<Board>, Observable<Board>>) Observable::fromIterable).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Board>() {
      @SuppressLint("NotifyDataSetChanged")
      @Override public void onSubscribe(@NonNull Disposable disposable) {
        BoardListContent.clearAllBoards();
        Objects.requireNonNull(mRecyclerView.getAdapter()).notifyDataSetChanged();
      }

      @Override public void onNext(@NonNull Board board) {
        // Log.d(TAG, board.toString());
        BoardListContent.addAllBoardItem(board);
        int size = BoardListContent.ALL_BOARDS.size();
        Objects.requireNonNull(mRecyclerView.getAdapter()).notifyItemInserted(size - 1);
        if (size == 50) {
          // if 50 items have been shown already, stop the loading hints
          clearLoadingHints();
        }
      }

      @Override public void onError(@NonNull Throwable e) {
        clearLoadingHints();
        if (mRefreshLayout != null) {
          mRefreshLayout.finishRefresh(false);
        }
        //Toast.makeText(SMTHApplication.getAppContext(), "加载所有版面失败!\n" + e.toString(), Toast.LENGTH_SHORT).show();
        NewToast.makeText(SMTHApplication.getAppContext(), "加载所有版面失败!\n", Toast.LENGTH_SHORT);
      }

      @Override public void onComplete() {
        clearLoadingHints();
        if (mRefreshLayout != null) {
          mRefreshLayout.finishRefresh(true);
        }

      }
    });
  }

  @Override public void onAttach(@androidx.annotation.NonNull Context context) {
    super.onAttach(context);
    mContext = context;
    if (context instanceof OnBoardFragmentInteractionListener) {
      mListener = (OnBoardFragmentInteractionListener) context;
    } else {
      throw new RuntimeException(context + " must implement OnBoardFragmentInteractionListener");
    }
  }

  @Override public void onDetach() {
    super.onDetach();
    mListener = null;
  }

  public static class QueryTextListener implements SearchView.OnQueryTextListener {
    private final BoardRecyclerViewAdapter mAdapter ;

    public QueryTextListener(BoardRecyclerViewAdapter mAdapter) {
      this.mAdapter = mAdapter;
    }

    @Override public boolean onQueryTextSubmit(String query) {
      return false;
    }

    @Override public boolean onQueryTextChange(String newText) {
      String TAG = "AllBoardFragment";
      Log.d(TAG, newText);
      mAdapter.getFilter().filter(newText);
      return true;
    }
  }

  @Override
  public void onViewCreated(@androidx.annotation.NonNull @NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    requireActivity().addMenuProvider(new MenuProvider() {
      @Override
      public void onCreateMenu(@androidx.annotation.NonNull @NonNull Menu menu, @androidx.annotation.NonNull @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.all_board_menu, menu);
      }

      @Override
      public boolean onMenuItemSelected(@androidx.annotation.NonNull @NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.main_action_refresh) {
          LoadAllBoardsWithoutCache();
          return true;
        }
        return false;
      }
    }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
  }

  @Override public boolean onVolumeUpDown(int keyCode) {
    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
      requireActivity().findViewById(R.id.bv_bottomNavigation).setVisibility(View.VISIBLE);
    } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
      requireActivity().findViewById(R.id.bv_bottomNavigation).setVisibility(View.GONE);
    }
    return true;
  }
}
