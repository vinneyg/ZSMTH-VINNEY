package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import android.widget.Toast;
import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.helpers.RecyclerViewUtil;
import com.zfdang.zsmth_android.listeners.OnBoardFragmentInteractionListener;
import com.zfdang.zsmth_android.listeners.OnVolumeUpDownListener;
import com.zfdang.zsmth_android.models.Board;
import com.zfdang.zsmth_android.models.BoardListContent;
import com.zfdang.zsmth_android.newsmth.SMTHHelper;
import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnBoardFragmentInteractionListener}
 * interface.
 */
public class FavoriteBoardFragment extends Fragment  implements OnVolumeUpDownListener {

  //private final String TAG = "FavoriteBoardFragment";
  private OnBoardFragmentInteractionListener mListener;

  private RecyclerView mRecyclerView = null;

  // list of favorite paths
  private List<Board> mFavoritePaths = null;

  //private MenuItem refreshMenuItem;

  private ActivityResultLauncher<Intent> mActivityLoginResultLauncher;

  public void pushPath(Board board) {
    mFavoritePaths.add(board);
  }

  public void popPath() {
    if (!mFavoritePaths.isEmpty()) {
      this.mFavoritePaths.remove(this.mFavoritePaths.size() - 1);
    }
  }

  public Board getCurrentPath() {
    if (!mFavoritePaths.isEmpty()) {
      return this.mFavoritePaths.get(this.mFavoritePaths.size() - 1);
    } else {
      return null;
    }
  }

  public String getCurrentPathInString() {
    Board board = getCurrentPath();
    if (board == null) {
      return "";
    }
    if(board.isSection()) {
      return board.getSectionID();
    }
    if(board.isFolder()) {
      return board.getFolderID();
    }
    return "";
  }

  public boolean isAtRoot() {
    return mFavoritePaths.isEmpty();
  }

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public FavoriteBoardFragment() {
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Initialize the ActivityResultLauncher object.
    mActivityLoginResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if(result.getResultCode() == Activity.RESULT_OK)
              {
                Intent intent = new Intent("com.zfdang.zsmth_android.UPDATE_USER_STATUS");
                requireContext().sendBroadcast(intent);
                RefreshFavoriteBoardsWithCache();
              }
            });

    if (mFavoritePaths == null) {
      mFavoritePaths = new ArrayList<>();
    }
    updateFavoriteTitle();
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mRecyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_favorite_board, container, false);

    // Set the adapter
    if (mRecyclerView != null) {
      //            http://stackoverflow.com/questions/28713231/recyclerview-item-separator
      mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager.VERTICAL, 0));
      Context context = mRecyclerView.getContext();
      mRecyclerView.setLayoutManager(new WrapContentLinearLayoutManager(context));
      mRecyclerView.setAdapter(new BoardRecyclerViewAdapter(BoardListContent.FAVORITE_BOARDS, mListener));
    }

    if (BoardListContent.FAVORITE_BOARDS.isEmpty() || SMTHApplication.bNewFavoriteBoard) {
      // only load boards on the first time
      //RefreshFavoriteBoards();
      RefreshFavoriteBoardsWithCache();
      if(SMTHApplication.bNewFavoriteBoard)
        SMTHApplication.bNewFavoriteBoard = false;
    }

    return mRecyclerView;
  }

  public void showLoadingHints() {
    MainActivity activity = (MainActivity) getActivity();
    if (activity != null) activity.showProgress("加载收藏版面，请稍候...");
  }

  public void clearLoadingHints() {
    // disable progress bar
    MainActivity activity = (MainActivity) getActivity();
    if (activity != null) {
      activity.dismissProgress();
    }
  }

  public void RefreshFavoriteBoardsWithCache() {
    SMTHHelper.ClearBoardListCache(SMTHHelper.BOARD_TYPE_FAVORITE, getCurrentPathInString());
    RefreshFavoriteBoards();
  }

  public void RefreshFavoriteBoards() {
    showLoadingHints();
    LoadFavoriteBoardsByPath();

  }

  protected void LoadFavoriteBoardsByPath() {
    // SMTHHelper helper = SMTHHelper.getInstance();
    Board board = getCurrentPath();
    final String finalCurrentPath = getCurrentPathInString();

    // all boards loaded in cached file
    final Observable<List<Board>> cache = Observable.create(observableEmitter -> {
      List<Board> boards = SMTHHelper.LoadBoardListFromCache(SMTHHelper.BOARD_TYPE_FAVORITE, finalCurrentPath);
      if (boards != null && !boards.isEmpty()) {
        observableEmitter.onNext(boards);
      } else {
        observableEmitter.onComplete();
      }
    });
    // all boards loaded from network
    Observable<List<Board>> network = null;
    if (board == null || board.isFolder()) {
      // 用户在收藏夹里创建的目录
      network = Observable.create(observableEmitter -> {
        List<Board> boards = SMTHHelper.LoadFavoriteBoardsInFolder(finalCurrentPath);
        if (!boards.isEmpty()) {
          observableEmitter.onNext(boards);
        } else {
          observableEmitter.onComplete();
        }
      });
    } else if(board.isSection()){
      // 用户在收藏夹里收藏的系统的二级目录
      network = Observable.create(observableEmitter -> {
        List<Board> boards = SMTHHelper.LoadFavoriteBoardsInSection(finalCurrentPath);
        if (!boards.isEmpty()) {
          observableEmitter.onNext(boards);
        } else {
          observableEmitter.onComplete();
        }
      });
    }

    List<Board> boards = new ArrayList<>();
    if (network == null) {
      Log.e("FavoriteBoard", "network is null.");
      return;
    }
    Observable.concat(cache, network).first(boards).toObservable().flatMap((Function<List<Board>, ObservableSource<Board>>) Observable::fromIterable).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Board>() {
      @SuppressLint("NotifyDataSetChanged")
      @Override public void onSubscribe(@NonNull Disposable disposable) {
        BoardListContent.clearFavorites();
        Objects.requireNonNull(mRecyclerView.getAdapter()).notifyDataSetChanged();
      }

      @Override public void onNext(@NonNull Board board) {
        BoardListContent.addFavoriteItem(board);
        Objects.requireNonNull(mRecyclerView.getAdapter()).notifyItemInserted(BoardListContent.FAVORITE_BOARDS.size());
        // Log.d(TAG, board.toString());
        if(BoardListContent.FAVORITE_BOARDS.get(0).isInvalid()) {
          Intent intent = new Intent(requireActivity(), LoginActivity.class);
          mActivityLoginResultLauncher.launch(intent);
        }
      }

      @Override public void onError(@NonNull Throwable e) {
        clearLoadingHints();
        Toast.makeText(SMTHApplication.getAppContext(), "加载收藏夹失败!\n" + e.toString(), Toast.LENGTH_SHORT).show();
      }

      @Override public void onComplete() {
        clearLoadingHints();
        updateFavoriteTitle();
      }
    });
  }

  private void updateFavoriteTitle() {
    Activity activity = getActivity();

    if (activity == null) {
      return;
    }
    StringBuilder title ;
    String mDefaultTitle = "收藏";
    if(mFavoritePaths.isEmpty()) {
      title = new StringBuilder(SMTHApplication.App_Title_Prefix + mDefaultTitle);
    } else {
      title = new StringBuilder(mDefaultTitle);
      for (int i = 0; i < mFavoritePaths.size(); i++) {
        Board board = mFavoritePaths.get(i);
        if(board.isFolder()) {
          title.append(" | ").append(mFavoritePaths.get(i).getFolderName());
        } else if(board.isSection()) {
          title.append(" | ").append(mFavoritePaths.get(i).getSectionName());
        }
      }
    }
    activity.setTitle(title.toString());
  }

  @Override public void onAttach(@androidx.annotation.NonNull Context context) {
    super.onAttach(context);
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

  @Override
  public void onViewCreated(@androidx.annotation.NonNull @NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    requireActivity().addMenuProvider(new MenuProvider() {
      @Override
      public void onCreateMenu(@androidx.annotation.NonNull @NonNull Menu menu, @androidx.annotation.NonNull @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.favorite_board_menu, menu);
      }

      @Override
      public boolean onMenuItemSelected(@androidx.annotation.NonNull @NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.main_action_refresh) {
          RefreshFavoriteBoardsWithCache();
          return true;
        }
        return false;
      }
    }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
  }


  @Override
  public boolean onVolumeUpDown(int keyCode) {
    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
      RecyclerViewUtil.ScrollRecyclerViewByKey(mRecyclerView, keyCode);

      requireActivity().findViewById(R.id.bv_bottomNavigation).setVisibility(View.VISIBLE);
    } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
      requireActivity().findViewById(R.id.bv_bottomNavigation).setVisibility(View.GONE);
    }
    return true;
  }
  @Override
  public void onResume(){
    super.onResume();
    /*
    if(refreshMenuItem != null)
      onOptionsItemSelected(refreshMenuItem);
    */
  }

}
