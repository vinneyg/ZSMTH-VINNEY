package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.text.TextUtils;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import com.zfdang.SMTHApplication;

import com.zfdang.zsmth_android.listeners.EndlessRecyclerOnScrollListener;
import com.zfdang.zsmth_android.listeners.OnMailInteractionListener;
import com.zfdang.zsmth_android.listeners.OnVolumeUpDownListener;
import com.zfdang.zsmth_android.models.ComposePostContext;
import com.zfdang.zsmth_android.models.Mail;
import com.zfdang.zsmth_android.models.MailListContent;
import com.zfdang.zsmth_android.newsmth.AjaxResponse;
import com.zfdang.zsmth_android.newsmth.SMTHHelper;
//import com.zfdang.zsmth_android.newsmth.UserInfo;
import com.zfdang.zsmth_android.services.MaintainUserStatusWorker;

import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import okhttp3.ResponseBody;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnMailInteractionListener}
 * interface.
 */
public class MailListFragment extends Fragment implements OnVolumeUpDownListener, View.OnClickListener {

  //private static final String TAG = "MailListFragment";
  public static final String INBOX_LABEL = "inbox";
  private static final String OUTBOX_LABEL = "outbox";
  private static final String DELETED_LABEL = "deleted";
  public static final String AT_LABEL = "at";
  public static final String REPLY_LABEL = "reply";
  public static final String LIKE_LABEL = "like";

  private OnMailInteractionListener mListener;
  private RecyclerView recyclerView;
  private EndlessRecyclerOnScrollListener mScrollListener = null;

  private Button btInbox;
  private Button btOutbox;
  private Button btTrashbox;
  private Button btAt;
  private Button btReply;
  private Button btLike;

  private int colorNormal;
  private int colorBlue;

  private String currentFolder = INBOX_LABEL;
  private int currentPage;

  private ActivityResultLauncher<Intent> mActivityLoginResultLauncher;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public MailListFragment() {
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // http://stackoverflow.com/questions/8308695/android-options-menu-in-fragment
    setHasOptionsMenu(true);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_mail_list, container, false);

    recyclerView = view.findViewById(R.id.recyclerview_mail_contents);
    Context context = view.getContext();
    LinearLayoutManager linearLayoutManager = new WrapContentLinearLayoutManager(context);
    recyclerView.setLayoutManager(linearLayoutManager);
    recyclerView.setItemAnimator(new DefaultItemAnimator());
    recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager.VERTICAL, 0));
    recyclerView.setAdapter(new MailRecyclerViewAdapter(MailListContent.MAILS, mListener));

    // enable endless loading
    mScrollListener = new EndlessRecyclerOnScrollListener(linearLayoutManager) {
      @Override public void onLoadMore(int current_page) {
        // do something...
        LoadMoreMails();
      }
    };
    recyclerView.addOnScrollListener(mScrollListener);

    // enable swipe to delete mail
    initItemHelper();

    // Initialize the ActivityResultLauncher object.
    mActivityLoginResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if(result.getResultCode() == Activity.RESULT_OK)
              {
                WorkRequest userStatusWorkRequest =
                        new OneTimeWorkRequest.Builder(MaintainUserStatusWorker.class).build();
                WorkManager.getInstance().enqueue(userStatusWorkRequest);
                LoadMailsFromBeginning();
              }
            });

    btInbox = view.findViewById(R.id.mail_button_inbox);
    btInbox.setOnClickListener(this);
    btOutbox = view.findViewById(R.id.mail_button_outbox);
    btOutbox.setOnClickListener(this);
    btTrashbox = view.findViewById(R.id.mail_button_trashbox);
    btTrashbox.setOnClickListener(this);
    btAt = view.findViewById(R.id.mail_button_at);
    btAt.setOnClickListener(this);
    btReply = view.findViewById(R.id.mail_button_reply);
    btReply.setOnClickListener(this);
    btLike = view.findViewById(R.id.mail_button_like);
    btLike.setOnClickListener(this);

    colorNormal = getResources().getColor(R.color.status_text_night,null);
    //colorBlue = getResources().getColor(R.color.blue_text_night);
    colorBlue = getResources().getColor(R.color.colorPrimary,null);

    //if (MailListContent.MAILS.isEmpty()||SMTHApplication.bNewMailSent) {
      LoadMailsFromBeginning();

      if(SMTHApplication.bNewMailSent)
        SMTHApplication.bNewMailSent = false;

      if(SMTHApplication.isValidUser())
        ( (MainActivity) requireActivity()).onRelogin();
    //}

    // highlight the current folder
    highlightCurrentFolder();

    return view;
  }

  public void initItemHelper() {
    //0则不执行拖动或者滑动
    ItemTouchHelper.Callback mCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

      @Override public boolean onMove(@androidx.annotation.NonNull RecyclerView recyclerView, @androidx.annotation.NonNull RecyclerView.ViewHolder viewHolder, @androidx.annotation.NonNull RecyclerView.ViewHolder target) {
        return false;
      }

      @Override public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
        //int position = viewHolder.getAdapterPosition();
        int position = viewHolder.getBindingAdapterPosition();
        Mail mail = MailListContent.MAILS.get(position);
        if (!mail.isCategory) {
          String type = "mail";
          String mailId = mail.getMailIDFromURL();
          if (mail.referIndex != null && !mail.referIndex.isEmpty()) {
            type = "refer";
            mailId = mail.referIndex;
          }

          Map<String, String> mails = new HashMap<>();
          String mailKey = String.format("m_%s", mailId);
          mails.put(mailKey, "on");

          SMTHHelper helper = SMTHHelper.getInstance();
          helper.wService.deleteMailOrReferPost(type, currentFolder, mails)
                  .subscribeOn(Schedulers.io())
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(new Observer<AjaxResponse>() {
                    @Override public void onSubscribe(@NonNull Disposable disposable) {

                    }

                    @Override public void onNext(@NonNull AjaxResponse ajaxResponse) {
                      // Log.d(TAG, "onNext: " + ajaxResponse.toString());
                      if (ajaxResponse.getAjax_st() == AjaxResponse.AJAX_RESULT_OK) {
                        //MailListContent.MAILS.remove(viewHolder.getAdapterPosition());
                        MailListContent.MAILS.remove(viewHolder.getBindingAdapterPosition());
                        //Objects.requireNonNull(recyclerView.getAdapter()).notifyItemRemoved(viewHolder.getAdapterPosition());
                        Objects.requireNonNull(recyclerView.getAdapter()).notifyItemRemoved(viewHolder.getBindingAdapterPosition());
                      }
                      //Toast.makeText(getActivity(), ajaxResponse.getAjax_msg(), Toast.LENGTH_SHORT).show();
                    }

                    @Override public void onError(@NonNull Throwable e) {
                      Toast.makeText(SMTHApplication.getAppContext(), "删除邮件失败!\n" + e.toString(), Toast.LENGTH_SHORT).show();
                    }

                    @Override public void onComplete() {

                    }
                  });
        }
        else{
          Objects.requireNonNull(recyclerView.getAdapter()).notifyItemChanged(position);
          Toast.makeText(SMTHApplication.getAppContext(), "目录无法删除", Toast.LENGTH_SHORT).show();
        }
      }
    };
    ItemTouchHelper itemTouchHelper = new ItemTouchHelper(mCallback);
    itemTouchHelper.attachToRecyclerView(recyclerView);
  }

  public void setCurrentFolder(String folder) {
    if (TextUtils.equals(folder, INBOX_LABEL)) {
      currentFolder = INBOX_LABEL;
    } else if (TextUtils.equals(folder, AT_LABEL)) {
      currentFolder = AT_LABEL;
    } else if (TextUtils.equals(folder, REPLY_LABEL)) {
      currentFolder = REPLY_LABEL;
    } else if (TextUtils.equals(folder, LIKE_LABEL)) {
      currentFolder = LIKE_LABEL;
    }
    //        Log.d(TAG, "setCurrentFolder: " + folder);
  }

  public void highlightCurrentFolder() {
    if (TextUtils.equals(currentFolder, INBOX_LABEL)) {
      btInbox.setTextColor(colorBlue);
      btOutbox.setTextColor(colorNormal);
      btTrashbox.setTextColor(colorNormal);
      btAt.setTextColor(colorNormal);
      btReply.setTextColor(colorNormal);
      btLike.setTextColor(colorNormal);
    } else if (TextUtils.equals(currentFolder, OUTBOX_LABEL)) {
      btInbox.setTextColor(colorNormal);
      btOutbox.setTextColor(colorBlue);
      btTrashbox.setTextColor(colorNormal);
      btAt.setTextColor(colorNormal);
      btReply.setTextColor(colorNormal);
      btLike.setTextColor(colorNormal);
    } else if (TextUtils.equals(currentFolder, DELETED_LABEL)) {
      btInbox.setTextColor(colorNormal);
      btOutbox.setTextColor(colorNormal);
      btTrashbox.setTextColor(colorBlue);
      btAt.setTextColor(colorNormal);
      btReply.setTextColor(colorNormal);
      btLike.setTextColor(colorNormal);
    } else if (TextUtils.equals(currentFolder, AT_LABEL)) {
      btInbox.setTextColor(colorNormal);
      btOutbox.setTextColor(colorNormal);
      btTrashbox.setTextColor(colorNormal);
      btAt.setTextColor(colorBlue);
      btReply.setTextColor(colorNormal);
      btLike.setTextColor(colorNormal);
    } else if (TextUtils.equals(currentFolder, REPLY_LABEL)) {
      btInbox.setTextColor(colorNormal);
      btOutbox.setTextColor(colorNormal);
      btTrashbox.setTextColor(colorNormal);
      btAt.setTextColor(colorNormal);
      btReply.setTextColor(colorBlue);
      btLike.setTextColor(colorNormal);
    } else if (TextUtils.equals(currentFolder, LIKE_LABEL)) {
      btInbox.setTextColor(colorNormal);
      btOutbox.setTextColor(colorNormal);
      btTrashbox.setTextColor(colorNormal);
      btAt.setTextColor(colorNormal);
      btReply.setTextColor(colorNormal);
      btLike.setTextColor(colorBlue);
    }
  }

  public void LoadMoreMails() {
    // LoadMore will be re-enabled in clearLoadingHints.
    // if we return here, loadMore will not be triggered again

    MainActivity activity = (MainActivity) getActivity();
    if (activity != null && activity.pDialog != null && activity.pDialog.isShowing()) {
      // loading in progress, do nothing
      return;
    }

    if (currentPage >= MailListContent.totalPages) {
      // reach the last page, do nothing
      if (!(MailListContent.MAILS.get(MailListContent.MAILS.size()-1).isCategory&&MailListContent.MAILS.get(MailListContent.MAILS.size()-1).category.equals(".END."))) {
        Mail mail = new Mail(".END.");
        MailListContent.addItem(mail);
        // recyclerView.getAdapter().notifyItemChanged(MailListContent.MAILS.size() - 1);
      }
      return;
    }

    currentPage += 1;
    LoadMailsOrReferPosts();
  }

  @SuppressLint("NotifyDataSetChanged")
  public void LoadMailsFromBeginning() {
    currentPage = 1;
    MailListContent.clear();
    Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();

    showLoadingHints();
    LoadMailsOrReferPosts();
  }

  public void LoadMailsOrReferPosts() {
    MainActivity activity = (MainActivity) getActivity();
    if (activity == null) {
      Log.e("MailListFragment", "activity is null.");
      return;
    }
    if (TextUtils.equals(currentFolder, INBOX_LABEL) || TextUtils.equals(currentFolder, OUTBOX_LABEL) || TextUtils.equals(currentFolder,
            DELETED_LABEL)) {
      // Load mails
      if (TextUtils.equals(currentFolder, INBOX_LABEL) ) {
        activity.setTitle(SMTHApplication.App_Title_Prefix+"邮件");
      }
      else if(TextUtils.equals(currentFolder,OUTBOX_LABEL)) {
        activity.setTitle(SMTHApplication.App_Title_Prefix+"发件箱");
      }
      else if(TextUtils.equals(currentFolder,DELETED_LABEL)) {
        activity.setTitle(SMTHApplication.App_Title_Prefix+"回收站");
      }
      LoadMails();
    } else {
      // Load refer posts
      if (TextUtils.equals(currentFolder, REPLY_LABEL) ) {
        activity.setTitle(SMTHApplication.App_Title_Prefix+"回复我");
      }
      else if(TextUtils.equals(currentFolder,AT_LABEL)) {
        activity.setTitle(SMTHApplication.App_Title_Prefix+"@我");
      }
      else if(TextUtils.equals(currentFolder,LIKE_LABEL)) {
        activity.setTitle(SMTHApplication.App_Title_Prefix+"LIKE我");
      }
      LoadReferPosts();
    }
  }

  public void LoadReferPosts() {
    // Log.d(TAG, "LoadReferPosts: " + currentPage);
    SMTHHelper helper = SMTHHelper.getInstance();

    helper.wService.getReferPosts(currentFolder, Integer.toString(currentPage)).flatMap((Function<ResponseBody, ObservableSource<Mail>>) responseBody -> {
      try {
        String response = responseBody.string();
        List<Mail> results = SMTHHelper.ParseMailsFromWWW(response);
        return Observable.fromIterable(results);
      } catch (Exception e) {
        Toast.makeText(SMTHApplication.getAppContext(), "加载文章提醒失败\n" + e, Toast.LENGTH_SHORT).show();
      }
      return null;
    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Mail>() {
      @Override public void onSubscribe(@NonNull Disposable disposable) {

      }

      @Override public void onNext(@NonNull Mail mail) {
        // Log.d(TAG, "onNext: " + mail.toString());
        MailListContent.addItem(mail);
        Objects.requireNonNull(recyclerView.getAdapter()).notifyItemChanged(MailListContent.MAILS.size() - 1);
      }

      @Override public void onError(@NonNull Throwable e) {
        clearLoadingHints();
        Toast.makeText(getActivity(), "加载相关文章失败！\n" + e.toString(), Toast.LENGTH_SHORT).show();

      }

      @Override public void onComplete() {
        clearLoadingHints();
        recyclerView.smoothScrollToPosition(0);
      }
    });
  }

  public void LoadMails() {
    // Log.d(TAG, "LoadMails: " + currentPage);
    SMTHHelper helper = SMTHHelper.getInstance();

    helper.wService.getUserMails(currentFolder, Integer.toString(currentPage)).flatMap((Function<ResponseBody, ObservableSource<Mail>>) responseBody -> {
      try {
        String response = responseBody.string();
        List<Mail> results = SMTHHelper.ParseMailsFromWWW(response);
        return Observable.fromIterable(results);
      } catch (Exception e) {
        Toast.makeText(SMTHApplication.getAppContext(), "加载邮件错误\n" + e, Toast.LENGTH_SHORT).show();
      }
      return null;
    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Mail>() {
      @Override public void onSubscribe(@NonNull Disposable disposable) {

      }

      @Override public void onNext(@NonNull Mail mail) {
        MailListContent.addItem(mail);
        Objects.requireNonNull(recyclerView.getAdapter()).notifyItemChanged(MailListContent.MAILS.size() - 1);

        if(mail.isCategory && mail.category.startsWith("产生错误的可能原因")){
          Intent intent = new Intent(requireActivity(), LoginActivity.class);
          //startActivityForResult(intent, MainActivity.LOGIN_ACTIVITY_REQUEST_CODE);
          mActivityLoginResultLauncher.launch(intent);
        }
      }

      @Override public void onError(@NonNull Throwable e) {
        clearLoadingHints();
        Toast.makeText(SMTHApplication.getAppContext(), "加载邮件列表失败！\n" + e.toString(), Toast.LENGTH_SHORT).show();
      }

      @Override public void onComplete() {
        clearLoadingHints();
        recyclerView.smoothScrollToPosition(0);
      }
    });
  }

  /*
  @Override
  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == MainActivity.LOGIN_ACTIVITY_REQUEST_CODE) {
      WorkRequest userStatusWorkRequest =
              new OneTimeWorkRequest.Builder(MaintainUserStatusWorker.class).build();
      WorkManager.getInstance(requireActivity()).enqueue(userStatusWorkRequest);
      LoadMailsFromBeginning();
    }
  }
  */

  public void showLoadingHints() {
    MainActivity activity = (MainActivity) getActivity();
    if (activity != null) activity.showProgress("加载信件中...");
  }

  public void clearLoadingHints() {
    // disable progress bar
    MainActivity activity = (MainActivity) getActivity();
    if (activity != null) {
      activity.dismissProgress();
    }

    // re-enable endless load
    if (mScrollListener != null) {
      mScrollListener.setLoading(false);
    }
  }

  public void markMailAsRead(final int position) {
    if (position >= 0 && position < MailListContent.MAILS.size()) {
      final Mail mail = MailListContent.MAILS.get(position);
      if (!mail.isNew) return;

      // only referred post need explicit marking. for mails, there is no need to mark
      if (TextUtils.equals(currentFolder, INBOX_LABEL) || TextUtils.equals(currentFolder, OUTBOX_LABEL) || TextUtils.equals(currentFolder,
              DELETED_LABEL)) {
        mail.isNew = false;
        Objects.requireNonNull(recyclerView.getAdapter()).notifyItemChanged(position);
        ((MainActivity)requireActivity()).clearBadgeCount(R.id.menu_message);
        return;
      }

      // mark it as read in remote and local
      SMTHHelper helper = SMTHHelper.getInstance();
      helper.wService.readReferPosts(currentFolder, mail.referIndex)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(new Observer<AjaxResponse>() {
                @Override public void onSubscribe(@NonNull Disposable disposable) {

                }

                @Override public void onNext(@NonNull AjaxResponse ajaxResponse) {
                  // Log.d(TAG, "onNext: " + ajaxResponse.toString());
                  if (ajaxResponse.getAjax_st() == AjaxResponse.AJAX_RESULT_OK) {
                    // succeed to mark the post as read in remote
                    mail.isNew = false;
                    Objects.requireNonNull(recyclerView.getAdapter()).notifyItemChanged(position);
                    ((MainActivity)requireActivity()).clearBadgeCount(R.id.menu_message);
                  } else {
                    // mark remote failed, show the response message
                    Toast.makeText(getActivity(), ajaxResponse.getAjax_msg(), Toast.LENGTH_SHORT).show();
                  }
                }

                @Override public void onError(@NonNull Throwable e) {
                  Toast.makeText(SMTHApplication.getAppContext(), "设置已读标记失败!\n" + e.toString(), Toast.LENGTH_SHORT).show();
                }

                @Override public void onComplete() {

                }
              });
    }
  }

  @Override public void onAttach(@androidx.annotation.NonNull Context context) {
    super.onAttach(context);
    if (context instanceof OnMailInteractionListener) {
      mListener = (OnMailInteractionListener) context;
    } else {
      throw new RuntimeException(context + " must implement OnMailInteractionListener");
    }
  }

  @Override public void onDetach() {
    super.onDetach();
    mListener = null;
  }

  @Override public void onCreateOptionsMenu(Menu menu, @androidx.annotation.NonNull MenuInflater inflater) {
    MenuItem item = menu.findItem(R.id.mail_list_fragment_newmail);
    item.setVisible(true);
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    MenuItem item = menu.findItem(R.id.mail_list_read_all);
    item.setVisible(!currentFolder.equals(INBOX_LABEL) && !currentFolder.equals(OUTBOX_LABEL) && !currentFolder.equals(DELETED_LABEL));
    super.onPrepareOptionsMenu(menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.mail_list_fragment_newmail) {
      // write new mail
      ComposePostContext postContext = new ComposePostContext();
      postContext.setComposingMode(ComposePostContext.MODE_NEW_MAIL);
      Intent intent = new Intent(getActivity(), ComposePostActivity.class);
      intent.putExtra(SMTHApplication.COMPOSE_POST_CONTEXT, postContext);
      startActivity(intent);
      return true;
    } else if (id == R.id.main_action_refresh) {
      LoadMailsFromBeginning();
    } else if (id == R.id.mail_list_read_all) {
      if (TextUtils.equals(currentFolder, INBOX_LABEL)||TextUtils.equals(currentFolder, OUTBOX_LABEL)||TextUtils.equals(currentFolder, DELETED_LABEL))
      {
        Toast.makeText(SMTHApplication.getAppContext(),"站点不支持邮件已读！",Toast.LENGTH_SHORT).show();
      }
      else
      {
        for(int pos =0;pos<MailListContent.MAILS.size();pos++)
          markMailAsRead(pos);
        ((MainActivity) requireActivity()).clearBadgeCount(R.id.menu_message);
      }
    }

    return super.onOptionsItemSelected(item);
  }

  @Override public void onClick(View v) {
    if (v == btInbox) {
      if (TextUtils.equals(currentFolder, INBOX_LABEL)) return;
      currentFolder = INBOX_LABEL;
    } else if (v == btOutbox) {
      if (TextUtils.equals(currentFolder, OUTBOX_LABEL)) return;
      currentFolder = OUTBOX_LABEL;
    } else if (v == btTrashbox) {
      if (TextUtils.equals(currentFolder, DELETED_LABEL)) return;
      currentFolder = DELETED_LABEL;
    } else if (v == btAt) {
      if (TextUtils.equals(currentFolder, AT_LABEL)) return;
      currentFolder = AT_LABEL;
    } else if (v == btReply) {
      if (TextUtils.equals(currentFolder, REPLY_LABEL)) return;
      currentFolder = REPLY_LABEL;
    } else if (v == btLike) {
      requireActivity().invalidateOptionsMenu();
      if (TextUtils.equals(currentFolder, LIKE_LABEL)) return;
      currentFolder = LIKE_LABEL;
    }

    highlightCurrentFolder();
    LoadMailsFromBeginning();
  }
  @Override public boolean onVolumeUpDown(int keyCode) {
    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
      requireActivity().findViewById(R.id.bv_bottomNavigation).setVisibility(View.VISIBLE);
    } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
      requireActivity().findViewById(R.id.bv_bottomNavigation).setVisibility(View.GONE);
    }
    return true;
  }

  @Override
  public void onResume(){
    super.onResume();
    if(SMTHApplication.bNewMailSent){
      LoadMailsFromBeginning();
      SMTHApplication.bNewMailSent = false;
    }
  }

}