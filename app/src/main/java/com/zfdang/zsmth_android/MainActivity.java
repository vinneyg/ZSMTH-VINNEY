package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.BroadcastReceiver;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.customview.widget.ViewDragHelper;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mob.MobSDK;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;
import com.umeng.analytics.MobclickAgent;
import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.fresco.WrapContentDraweeView;
import com.zfdang.zsmth_android.listeners.OnBoardFragmentInteractionListener;
import com.zfdang.zsmth_android.listeners.OnMailInteractionListener;
import com.zfdang.zsmth_android.listeners.OnTopicFragmentInteractionListener;
import com.zfdang.zsmth_android.listeners.OnVolumeUpDownListener;
import com.zfdang.zsmth_android.models.Board;
import com.zfdang.zsmth_android.models.Mail;
import com.zfdang.zsmth_android.models.MailListContent;
import com.zfdang.zsmth_android.models.Topic;
import com.zfdang.zsmth_android.newsmth.AjaxResponse;
import com.zfdang.zsmth_android.newsmth.SMTHHelper;
import com.zfdang.zsmth_android.newsmth.UserInfo;
import com.zfdang.zsmth_android.services.KeepAliveService;
import com.zfdang.zsmth_android.services.MaintainUserStatusWorker;
import com.zfdang.zsmth_android.services.UserStatusReceiver;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import com.zfdang.zsmth_android.helpers.NewToast;


public class MainActivity extends SMTHBaseActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener, OnTopicFragmentInteractionListener,
        OnBoardFragmentInteractionListener, OnMailInteractionListener {
  // used by startActivityForResult
  static final int LOGIN_ACTIVITY_REQUEST_CODE = 9527;  // The request code
  private static final String TAG = "MainActivity";

  private static final String KEY_CURRENT_FRAGMENT = "current_fragment";
  private int currentFragmentId;

  // keep aive service
  private Intent keepAliveService;
  // guidance fragment: display hot topics
  // this fragment is using RecyclerView to show all hot topics
  HotTopicFragment hotTopicFragment = null;
  FavoriteBoardFragment favoriteBoardFragment = null;
  AllBoardFragment allBoardFragment = null;
  MailListFragment mailListFragment = null;
  MyPreferenceFragment preferenceFragment = null;
  Fragment aboutFragment = null;
  private WrapContentDraweeView mAvatar = null;
  private TextView mUsername = null;

  private DrawerLayout mDrawer = null;
  private ActionBarDrawerToggle mToggle = null;

  // press BACK in 2 seconds, app will quit
  private boolean mDoubleBackToExit = false;
  private Handler mHandler = null;
  private FloatingActionMenu mActionMenu;
  private NavigationView mNavigationView;

  private static final int notificationID = 273;

  private BottomNavigationView mBottomNavigationView;

  private ActivityResultLauncher<Intent> mActivityLoginResultLauncher;
  private Button mailButtonInbox;
  private Drawable default_icon;

  private final BroadcastReceiver userStatusReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (Objects.equals(intent.getAction(), "com.zfdang.zsmth_android.UPDATE_USER_STATUS")) {
        updateUserStatusNow();
      }
    }
  };

  @SuppressLint("UnspecifiedRegisterReceiverFlag")
  @Override protected void onCreate(Bundle savedInstanceState) {
    if (Settings.getInstance().isNightMode()) {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    } else {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar =  findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    mBottomNavigationView = findViewById(R.id.bv_bottomNavigation);

    OnBackPressedDispatcher dispatcher = getOnBackPressedDispatcher();

    dispatcher.addCallback(this, new OnBackPressedCallback(true) {
      @Override
      public void handleOnBackPressed() {
        // Handle the back button press here
        onHandleBackPressed();
      }
    });

    // Initialize the ActivityResultLauncher object.
    mActivityLoginResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if(result.getResultCode() == Activity.RESULT_OK)
              {
                updateUserStatusNow();
              }
            });

    // 注册广播接收器
    IntentFilter filter = new IntentFilter();
    filter.addAction("com.zfdang.zsmth_android.UPDATE_USER_STATUS");
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
      // For API level 33 and above, use RECEIVER_NOT_EXPORTED
      registerReceiver(userStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    } else {
      registerReceiver(userStatusReceiver, filter);
    }

    // how to adjust the height of toolbar
    // http://stackoverflow.com/questions/17439683/how-to-change-action-bar-size
    // zsmth_actionbar_size @ dimen ==> ThemeOverlay.ActionBar @ styles ==> theme @ app_bar_main.xml

    // init floating action button & circular action menu
    FloatingActionButton fab = findViewById(R.id.fab);
    initCircularActionMenu(fab);

    initBottomNavigation();

    // fab.hide()
    if (Settings.getInstance().isLaunchBottomNavi()) {
      fab.hide();
      findViewById(R.id.bv_bottomNavigation).setVisibility(View.VISIBLE);
    } else {
      fab.show();
      findViewById(R.id.bv_bottomNavigation).setVisibility(View.GONE);
    }

    mDrawer = findViewById(R.id.drawer_layout);
    if(Settings.getInstance().isLeftNavSlide())
      setDrawerLeftEdgeSize(this, mDrawer, (float) 1);//To support Mail Deletion
    else
      setDrawerLeftEdgeSize(this, mDrawer, (float) 0.3);

    mDrawer.addDrawerListener(new ActionBarDrawerToggle(this, mDrawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

      @Override
      public void onDrawerSlide(View drawerView, float slideOffset) {
        super.onDrawerSlide(drawerView, slideOffset);
        if (slideOffset > 0) {
          UpdateNavigationViewHeader();
        }
      }
      @Override
      public void onDrawerOpened(View drawerView) {

        Menu menu = ((NavigationView) findViewById(R.id.nav_view)).getMenu();

        menu.findItem(R.id.read_board1).setTitle(SMTHApplication.ReadBoard1);
        menu.findItem(R.id.read_board2).setTitle(SMTHApplication.ReadBoard2);
        menu.findItem(R.id.read_board3).setTitle(SMTHApplication.ReadBoard3);

        super.onDrawerOpened(drawerView);
      }
    });

    mToggle = new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close){
      @Override
      public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null && item.getItemId() == android.R.id.home) {
          UpdateNavigationViewHeader();
          if (mDrawer.isDrawerVisible(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
          } else {
            mDrawer.openDrawer(GravityCompat.START);
          }
          return true;
        }
        return super.onOptionsItemSelected(item);
      }
    };
    //mDrawer.addDrawerListener(mToggle);
    mToggle.syncState();
    // Full Screen Drawer

    int[][] states = new int[][]{
            new int[]{-android.R.attr.state_checked},
            new int[]{android.R.attr.state_checked}
    };

    int[] colors = new int[]{getResources().getColor(R.color.colorPrimary,null),
            getResources().getColor(R.color.status_text_night,null)
    };
    ColorStateList csl = new ColorStateList(states, colors);

    mNavigationView = findViewById(R.id.nav_view);
    mNavigationView.setNavigationItemSelectedListener(this);
    mNavigationView.setItemTextColor(csl);
    mNavigationView.setItemIconTintList(csl);
    mNavigationView.setCheckedItem(R.id.nav_guidance);

    // http://stackoverflow.com/questions/33161345/android-support-v23-1-0-update-breaks-navigationview-get-find-header
    View headerView = mNavigationView.getHeaderView(0);
    mAvatar = headerView.findViewById(R.id.nav_user_avatar);
    mAvatar.setOnClickListener(this);

    mUsername = headerView.findViewById(R.id.nav_user_name);
    mUsername.setOnClickListener(this);

    // http://stackoverflow.com/questions/27097126/marquee-title-in-toolbar-actionbar-in-android-with-lollipop-sdk
    TextView titleTextView;
    try {
      Field f = toolbar.getClass().getDeclaredField("mTitleTextView");
      f.setAccessible(true);
      titleTextView = (TextView) f.get(toolbar);
      if (titleTextView == null) {
        Log.e(TAG, "titleTextView is null.");
        return;
      }
      titleTextView.setEllipsize(TextUtils.TruncateAt.START);
    } catch (NoSuchFieldException | IllegalAccessException ignored) {
    }

    // init all fragments
    initFragments();

    if (savedInstanceState != null) {
      if (getIntent().hasExtra("FRAGMENT") &&
              "PREFERENCE".equals(getIntent().getStringExtra("FRAGMENT"))) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, preferenceFragment)
                .commit();
        getIntent().removeExtra("FRAGMENT");
      }
      else{
        currentFragmentId = savedInstanceState.getInt(KEY_CURRENT_FRAGMENT);
        Fragment fragment = getFragmentById(currentFragmentId);

        if (fragment instanceof MailListFragment) {
          MailListFragment mailListFragment = (MailListFragment) fragment;
          Bundle mailListState = savedInstanceState.getBundle("mail_list_fragment_state");
          mailListFragment.restoreState(mailListState);
        }

        if (fragment != null) {
          FragmentManager fm = getSupportFragmentManager();
          fm.beginTransaction().replace(R.id.content_frame, fragment).commit();
          String title = getTitleByFragmentId(currentFragmentId);
          if (fragment != favoriteBoardFragment) {
            setTitle(SMTHApplication.App_Title_Prefix + title);
          }
        }
      }
    } else {
      FragmentManager fm = getSupportFragmentManager();
      if (Settings.getInstance().isLaunchHotTopic()) {
        fm.beginTransaction().replace(R.id.content_frame, hotTopicFragment).commit();
        currentFragmentId = R.id.nav_guidance;
      } else {
        fm.beginTransaction().replace(R.id.content_frame, favoriteBoardFragment).commit();
        currentFragmentId = R.id.nav_favorite;
      }
    }

    getSupportFragmentManager().addOnBackStackChangedListener(() -> {
      //Enable Up button only  if there are entries in the back stack
      boolean canback = getSupportFragmentManager().getBackStackEntryCount() > 0;
      if (canback) {
        mToggle.setDrawerIndicatorEnabled(false);
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
          bar.setDisplayShowHomeEnabled(true);
          bar.setHomeButtonEnabled(true);
          bar.setDisplayHomeAsUpEnabled(true);
        }
      } else {
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
          bar.setDisplayHomeAsUpEnabled(false);
        }
        mToggle.setDrawerIndicatorEnabled(true);
        mDrawer.addDrawerListener(mToggle);
      }
    });

    WorkManager.getInstance(getApplicationContext()).cancelAllWorkByTag(MaintainUserStatusWorker.class.getName());
    // setup receiver to receive user status update from periodical background service
    setupUserStatusReceiver();

    // schedule the periodical background service
    Data.Builder inputData = new Data.Builder();
    inputData.putBoolean(MaintainUserStatusWorker.REPEAT, true);
    OneTimeWorkRequest userStatusWorkRequest =
            new OneTimeWorkRequest.Builder(MaintainUserStatusWorker.class)
                    .setInitialDelay(SMTHApplication.INTERVAL_TO_CHECK_MESSAGE, TimeUnit.MINUTES)
                    .setInputData(inputData.build())
                    .build();
    WorkManager.getInstance(getApplicationContext())
            .enqueueUniqueWork(MaintainUserStatusWorker.WORKER_ID, ExistingWorkPolicy.REPLACE,userStatusWorkRequest);

    // run the background service now
    updateUserStatusNow();
    UpdateNavigationViewHeader();

    if (Settings.getInstance().isFirstRun()) {
      // show info dialog after 5 seconds for the first run
      final Handler handler = new Handler();
      handler.postDelayed(() -> {
        showInfoDialog();
        MobSDK.submitPolicyGrantResult(true);
      }, 3000);
    }
  }


  public void setApplicationNightMode() {
    boolean bNightMode = Settings.getInstance().isNightMode();
    if (bNightMode) {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    } else {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
    SMTHApplication.bNightModeChange = true;
    if(SMTHApplication.bNewMailInNotification)
      setBadgeCount(R.id.menu_message, "信");
    SMTHApplication.bNightModeChange = false;
  }


  private void initBottomNavigation() {
    mBottomNavigationView.setItemIconTintList(null);
    mBottomNavigationView.setItemTextAppearanceActive(R.style.bottom_selected_text);
    mBottomNavigationView.setItemTextAppearanceInactive(R.style.bottom_normal_text);

    if(!Settings.getInstance().isMenuTextOn())
      mBottomNavigationView.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_UNLABELED);

    int[][] states = new int[][]{
            new int[]{-android.R.attr.state_checked},
            new int[]{android.R.attr.state_checked}
    };

    int[] colors = new int[]{getResources().getColor(R.color.status_text_night,null),
            getResources().getColor(R.color.colorPrimary,null)
    };
    ColorStateList csl = new ColorStateList(states, colors);
    mBottomNavigationView.setItemTextColor(csl);
    mBottomNavigationView.setItemIconTintList(csl);

    mBottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
      @SuppressLint("NonConstantResourceId")
      @Override
      public boolean onNavigationItemSelected(@androidx.annotation.NonNull @NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_discover) {
          onNavigationItemID(R.id.nav_guidance);
          mNavigationView.setCheckedItem(R.id.nav_guidance);
        } else if (itemId == R.id.menu_star) {
          onNavigationItemID(R.id.nav_favorite);
          mNavigationView.setCheckedItem(R.id.nav_favorite);
        } else if (itemId == R.id.menu_list) {
          onNavigationItemID(R.id.nav_all_boards);
          mNavigationView.setCheckedItem(R.id.nav_all_boards);
        } else if (itemId == R.id.menu_message) {
          onNavigationItemID(R.id.nav_mail);
          mNavigationView.setCheckedItem(R.id.nav_mail);
        }
        return true;
      }
    });

  }


  @SuppressLint("UseCompatLoadingForDrawables")
  private void initCircularActionMenu(FloatingActionButton fab) {
    SubActionButton.Builder itemBuilder = new SubActionButton.Builder(this);

    ImageView itemIcon1 = new ImageView(this);
    itemIcon1.setImageDrawable(getResources().getDrawable(R.drawable.ic_whatshot_white_48dp,null));
    SubActionButton button1 = itemBuilder.setContentView(itemIcon1)
            .setBackgroundDrawable(getResources().getDrawable(R.drawable.navigation_button_background,null))
            .build();
    button1.setOnClickListener(v -> {
      mActionMenu.close(true);
      onNavigationItemID(R.id.nav_guidance);
      mNavigationView.setCheckedItem(R.id.nav_guidance);
    });

    ImageView itemIcon2 = new ImageView(this);
    itemIcon2.setImageDrawable(getResources().getDrawable(R.drawable.ic_star_white_48dp,null));
    SubActionButton button2 = itemBuilder.setContentView(itemIcon2)
            .setBackgroundDrawable(getResources().getDrawable(R.drawable.navigation_button_background,null))
            .build();
    button2.setOnClickListener(v -> {
      mActionMenu.close(true);
      onNavigationItemID(R.id.nav_favorite);
      mNavigationView.setCheckedItem(R.id.nav_favorite);
    });

    ImageView itemIcon3 = new ImageView(this);
    itemIcon3.setImageDrawable(getResources().getDrawable(R.drawable.ic_format_list_bulleted_white_48dp,null));
    SubActionButton button3 = itemBuilder.setContentView(itemIcon3)
            .setBackgroundDrawable(getResources().getDrawable(R.drawable.navigation_button_background,null))
            .build();
    button3.setOnClickListener(v -> {
      mActionMenu.close(true);
      onNavigationItemID(R.id.nav_all_boards);
      mNavigationView.setCheckedItem(R.id.nav_all_boards);
    });

    ImageView itemIcon4 = new ImageView(this);
    itemIcon4.setImageDrawable(getResources().getDrawable(R.drawable.ic_email_white_48dp,null));
    SubActionButton button4 = itemBuilder.setContentView(itemIcon4)
            .setBackgroundDrawable(getResources().getDrawable(R.drawable.navigation_button_background,null))
            .build();
    button4.setOnClickListener(v -> {
      mActionMenu.close(true);
      onNavigationItemID(R.id.nav_mail);
      mNavigationView.setCheckedItem(R.id.nav_mail);
    });

    mActionMenu = new FloatingActionMenu.Builder(this).addSubActionView(button1)
            .addSubActionView(button2)
            .addSubActionView(button3)
            .addSubActionView(button4)
            .attachTo(fab)
            .build();
  }

  // triger the background service right now
  private void updateUserStatusNow() {
    // run worker immediately for once
    WorkRequest userStatusWorkRequest =
            new OneTimeWorkRequest.Builder(MaintainUserStatusWorker.class).build();
    WorkManager.getInstance(getApplicationContext()).enqueue(userStatusWorkRequest);
  }

  private void setupUserStatusReceiver() {
    UserStatusReceiver mReceiver = new UserStatusReceiver(new Handler());
    mReceiver.setReceiver(new UserStatusReceiver.Receiver() {
      @Override public void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultCode == RESULT_OK) {
          Log.d(TAG,"onReceiveResult: " + "to update navigationview " + SMTHApplication.activeUser.toString());
          runOnUiThread(() -> UpdateNavigationViewHeader());

          // show notification if necessary
          String message = resultData.getString(SMTHApplication.SERVICE_NOTIFICATION_MESSAGE);
          Log.d(TAG, "OnReceiveResult: " + message);
          if (message != null) {
            showNotification(message);
            if(!message.contains(SMTHApplication.NOTIFICATION_LOGIN_LOST)){
              String msg = getMsg(message);
              runOnUiThread(() -> setBadgeCount(R.id.menu_message, msg));
            }
          }
          else{
            runOnUiThread(() -> clearBadgeCount(R.id.menu_message));
          }
        }
      }

      @Override
      public void onServerFailed() {
        stopService(keepAliveService);
        SMTHApplication.activeUser = null;
        SMTHApplication.displayedUserId = "guest";

        runOnUiThread(() -> UpdateNavigationViewHeader());

        final AlertDialog dlg =
                new AlertDialog.Builder(MainActivity.this).setIcon(R.drawable.ic_launcher).setTitle(R.string.app_name).setMessage("已掉线请登录！").create();

        dlg.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.about_close), (dialog, which) -> {
          // do nothing here
        });

        dlg.show();

        /*
        if( getCurrentFragment() instanceof HotTopicFragment ||
                getCurrentFragment() instanceof FavoriteBoardFragment)
          onLogin();
        */
      }
    });
    SMTHApplication.mUserStatusReceiver = mReceiver;
  }

  @androidx.annotation.NonNull
  private static String getMsg(String message) {
    String msg;
    if(message.contains(SMTHApplication.NOTIFICATION_NEW_MAIL)){
      msg = "信";
    }
    else if(message.contains(SMTHApplication.NOTIFICATION_NEW_AT)){
      msg = "@";
    }
    else if(message.contains(SMTHApplication.NOTIFICATION_NEW_REPLY)){
      msg = "R";
    }
    else if(message.contains(SMTHApplication.NOTIFICATION_NEW_LIKE)){
      msg = "L";
    } else {
      msg = "";
    }
    return msg;
  }

  private void init_keep_alive_service() {
    if (keepAliveService == null)
      keepAliveService = new Intent(this, KeepAliveService.class);
    if (!isKeepAliveServiceRunning()) {
      startForegroundService(keepAliveService);
    }
  }
  public Boolean isKeepAliveServiceRunning() {
    ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
    if (runningAppProcesses != null) {
      for (ActivityManager.RunningAppProcessInfo processInfo : runningAppProcesses) {
        if (processInfo.processName.equals(KeepAliveService.class.getName())) {
          return true;
        }
      }
    }

    return false;
  }

  private void showNotification(String text) {
    // http://stackoverflow.com/questions/13602190/java-lang-securityexception-requires-vibrate-permission-on-jelly-bean-4-2
    try {
      Intent notificationIntent = new Intent(MainActivity.this, MainActivity.class);
      notificationIntent.putExtra(SMTHApplication.SERVICE_NOTIFICATION_MESSAGE, text);
      // http://stackoverflow.com/questions/26608627/how-to-open-fragment-page-when-pressed-a-notification-in-android
      notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
      // notification will be handled by MainActivity.onNewIntent
      PendingIntent resultPendingIntent =
              PendingIntent.getActivity(MainActivity.this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

      NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);


      NotificationChannel notificationChannel = new NotificationChannel("newChan","zSMTH-v通知消息",
              NotificationManager.IMPORTANCE_DEFAULT);
      mNotifyMgr.createNotificationChannel(notificationChannel);

      Notification notification = new NotificationCompat.Builder(this,"newChan")
              .setSmallIcon(R.drawable.ic_launcher)
              .setContentTitle("zSMTH-v提醒")
              .setWhen(System.currentTimeMillis())
              .setAutoCancel(true)
              .setOnlyAlertOnce(true)
              .setDefaults(Notification.DEFAULT_VIBRATE)
              .setContentText(text)
              .setContentIntent(resultPendingIntent)
              .build();
      if(mNotifyMgr.areNotificationsEnabled()){
        mNotifyMgr.notify(notificationID, notification);
      }
      /*
      else{
        Intent intent = new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(intent);
      }
     */

    } catch (Exception se) {
      Log.e(TAG, "showNotification: " + se);
    }
  }

  @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (Settings.getInstance().isVolumeKeyScroll() && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
      Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
      if (fragment instanceof OnVolumeUpDownListener) {
        OnVolumeUpDownListener frag = (OnVolumeUpDownListener) fragment;
        return frag.onVolumeUpDown(keyCode);
      }
      return false;
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

  @Override protected void onPause() {
    super.onPause();
    MobclickAgent.onPause(this);
  }

  @Override protected void onNewIntent(Intent intent) {
    // this method will be triggered by showNotification(message);
    super.onNewIntent(intent);
    FragmentManager fm = getSupportFragmentManager();
    Bundle bundle = intent.getExtras();
    if (bundle != null) {
      // this activity is launched by notification, show mail fragment now
      // http://www.androiddesignpatterns.com/2013/08/fragment-transaction-commit-state-loss.html
      // http://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-wit
      // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
      String message = bundle.getString(SMTHApplication.SERVICE_NOTIFICATION_MESSAGE);
      if (message != null) {
        if (message.contains(SMTHApplication.NOTIFICATION_LOGIN_LOST)) {
          // login status lost, show login menu
          onLogin();
        } else {

          if(message.contains("你有新")){
            // find the actual folder for the new message
            String subTitle = "收件箱";
            if (message.contains(SMTHApplication.NOTIFICATION_NEW_MAIL)) {
              mailListFragment.setCurrentFolder(MailListFragment.INBOX_LABEL);
            } else if (message.contains(SMTHApplication.NOTIFICATION_NEW_LIKE)) {
              subTitle = "LIKE我";
              mailListFragment.setCurrentFolder(MailListFragment.LIKE_LABEL);
            } else if (message.contains(SMTHApplication.NOTIFICATION_NEW_AT)) {
              subTitle = "@我";
              mailListFragment.setCurrentFolder(MailListFragment.AT_LABEL);
            } else if (message.contains(SMTHApplication.NOTIFICATION_NEW_REPLY)) {
              subTitle = "回复我";
              mailListFragment.setCurrentFolder(MailListFragment.REPLY_LABEL);
            }
            // force mail fragment to reload
            MailListContent.clear();

            fm.beginTransaction().replace(R.id.content_frame, mailListFragment).commitAllowingStateLoss();
            // switch title of mainActivity
            // setTitle(SMTHApplication.App_Title_Prefix + "邮件");
            setTitle(SMTHApplication.App_Title_Prefix + subTitle);
          }
        }
      }
    }
  }

  @Override protected void onResume() {
    super.onResume();
    MobclickAgent.onResume(this);
  }

  protected void initFragments() {
    hotTopicFragment = new HotTopicFragment();

    // following initilization can be delayed
    favoriteBoardFragment = new FavoriteBoardFragment();
    allBoardFragment = new AllBoardFragment();
    mailListFragment = new MailListFragment();

    preferenceFragment = new MyPreferenceFragment();
    aboutFragment = new LibsBuilder()
            .withLicenseShown(false)
            .supportFragment();
  }

  @Override public boolean onPrepareOptionsMenu(Menu menu) {
    MenuItem login = menu.findItem(R.id.main_action_login);
    MenuItem logout = menu.findItem(R.id.main_action_logout);
    menu.removeItem(R.id.main_action_logout);
    menu.removeItem(R.id.main_action_logout);

    return super.onPrepareOptionsMenu(menu);
  }

  // update header view in navigation header
  public void UpdateNavigationViewHeader() {
    // update optionMenu
    getWindow().invalidatePanelMenu(Window.FEATURE_OPTIONS_PANEL);
    if (SMTHApplication.isValidUser()) {
      // update user to logined user
      mUsername.setText(SMTHApplication.activeUser.getId());
      String faceURL = SMTHApplication.activeUser.getFace_url();
      if (faceURL != null) {
        mAvatar.setImageFromStringURL(faceURL);
      }
      SMTHApplication.displayedUserId = SMTHApplication.activeUser.getId();
      init_keep_alive_service();
    } else {
      // when user is invalid, set notice to login
      mUsername.setText(getString(R.string.nav_header_click_to_login));
      mAvatar.setImageResource(R.drawable.ic_person_black_48dp);
      SMTHApplication.displayedUserId = "guest";
      if (keepAliveService != null)
        stopService(keepAliveService);
    }
  }

  public void onHandleBackPressed() {
    if (mDrawer.isDrawerOpen(GravityCompat.START)) {
      mDrawer.closeDrawer(GravityCompat.START);
      return;
    }

    // handle back button for all fragment
    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
    if (fragment instanceof FavoriteBoardFragment) {
      if (!favoriteBoardFragment.isAtRoot()) {
        favoriteBoardFragment.popPath();
        favoriteBoardFragment.RefreshFavoriteBoards();
        return;
      }
    }

    if (fragment != hotTopicFragment) {
      // return to hot topic if we are not there yet
      String title = "首页";
      FragmentManager fm = getSupportFragmentManager();
      fm.beginTransaction().replace(R.id.content_frame, hotTopicFragment).commit();
      setTitle(SMTHApplication.App_Title_Prefix + title);
      return;
    }

    // for other cases, double back to exit app
    DoubleBackToExit();
  }

  @SuppressLint("ResourceAsColor")
  private void DoubleBackToExit() {
    if (mDoubleBackToExit) {
      // if mDoubleBackToExit is true, exit now
      quitNow();
    } else {
      // set mDoubleBackToExit = true, and set delayed task to
      // reset it to false
      mDoubleBackToExit = true;
      if (mHandler == null) {
        mHandler = new Handler();
      }
      // reset will be run after 2000 ms
      mHandler.postDelayed(new PendingDoubleBackToExit(), 2000);
      //Toast.makeText(this, "再按一次退出zSMTH", Toast.LENGTH_SHORT).show();
      NewToast.makeText(this, "再按一次退出zSMTH", Toast.LENGTH_SHORT,R.color.colorDivider);

    }

  }

  private void quitNow() {
    // stop keep alive service
    if (keepAliveService != null)
      stopService(keepAliveService);

    finish();
    android.os.Process.killProcess(android.os.Process.myPid());
    System.exit(0);
  }

  // show information dialog, called by first run
  private void showInfoDialog() {
    // read version info from androidmanifest.xml
    String versionName = "unknown";
    int versionCode = 0;
    PackageManager pm = getPackageManager();
    try {
      PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
      versionName = pi.versionName;
      versionCode = pi.versionCode;
    } catch (PackageManager.NameNotFoundException e) {
      Log.e(TAG, "showInfoDialog: " + Log.getStackTraceString(e));
    }

    // generate about_content with version from manifest
    String content_with_version = getString(R.string.about_content, versionName, versionCode);

    // linkify
    final SpannableString msg = new SpannableString(content_with_version);
    Linkify.addLinks(msg, Linkify.WEB_URLS);

    final AlertDialog dlg =
            new AlertDialog.Builder(this).setIcon(R.drawable.ic_launcher).setTitle(R.string.about_title).setMessage(msg).create();

    dlg.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.about_close), (dialog, which) -> {
      // do nothing here
    });

    dlg.show();

    // Make the textview clickable. Must be called after show()
    ((TextView) Objects.requireNonNull(dlg.findViewById(android.R.id.message))).setMovementMethod(LinkMovementMethod.getInstance());
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main_menu, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    if (id == R.id.main_action_login) {
      onLogin();
      return true;
    } else if (id == R.id.main_action_logout) {
      onLogout();
      return true;
    } else if (id == android.R.id.home) {
      //onHandleBackPressed();
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public void onLogin() {
    // still use the previous login method
    Intent intent = new Intent(this, LoginActivity.class);
    mActivityLoginResultLauncher.launch(intent);
  }

  public void onLogout() {

    if (SMTHApplication.activeUser!=null ) {
      SMTHApplication.activeUser.setId("guest");
      UpdateNavigationViewHeader();
    }

    SMTHHelper helper = SMTHHelper.getInstance();
    helper.wService.logout()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(new Observer<AjaxResponse>() {
              @Override
              public void onSubscribe(@NonNull Disposable disposable) {

              }

              @Override
              public void onNext(@NonNull AjaxResponse ajaxResponse) {
                //Toast.makeText(MainActivity.this, ajaxResponse.getAjax_msg(), Toast.LENGTH_SHORT).show();
                if(ajaxResponse.getAjax_st() == AjaxResponse.AJAX_RESULT_OK){
                  Settings.getInstance().setAutoLogin(false);
                  Settings.getInstance().setUserOnline(false);
                  SMTHApplication.ReadTopicLists.clear();
                }
              }

              @Override
              public void onError(@NonNull Throwable e) {
                //Toast.makeText(MainActivity.this, "退出登录失败!" , Toast.LENGTH_SHORT).show();
                NewToast.makeText(MainActivity.this, "退出登录失败!" , Toast.LENGTH_SHORT);
              }

              @Override
              public void onComplete() {
                Settings.getInstance().setAutoLogin(false);
                Settings.getInstance().setUserOnline(false);

                Intent intent = new Intent("com.zfdang.zsmth_android.PREFERENCE_CLICKED");
                intent.putExtra("preference_key", "setting_fresco_cache");
                sendBroadcast(intent);

                intent = new Intent("com.zfdang.zsmth_android.PREFERENCE_CLICKED");
                intent.putExtra("preference_key", "setting_okhttp3_cache");
                sendBroadcast(intent);
              }
            });

  }

  //@SuppressWarnings("StatementWithEmptyBody")
  @Override
  public boolean onNavigationItemSelected(MenuItem item) {
    int id = item.getItemId();
    return onNavigationItemID(id);
  }

  public boolean onNavigationItemID(int menuID) {
    // Handle navigation view item clicks here.
    Fragment fragment = null;
    String title = "";

    if (menuID == R.id.nav_guidance) {
      fragment = hotTopicFragment;
      title = "首页";
    } else if (menuID == R.id.nav_favorite) {
      fragment = favoriteBoardFragment;
      title = "收藏";
    } else if (menuID == R.id.nav_all_boards) {
      fragment = allBoardFragment;
      title = "分区";
    } else if (menuID == R.id.nav_mail) {
      fragment = mailListFragment;
      title = "收件箱";
    } else if (menuID == R.id.nav_setting) {
      //            fragment = settingFragment;
      fragment = preferenceFragment;
      title = "设置";
    } else if (menuID == R.id.nav_about) {
      fragment = aboutFragment;
      title = "关于";
    } else if(menuID == R.id.nav_night_mode) {
      boolean bNightMode = Settings.getInstance().isNightMode();
      Settings.getInstance().setNightMode(!bNightMode);
      currentFragmentId = getCurrentFragmentId();
      setApplicationNightMode();
      mDrawer.closeDrawer(GravityCompat.START);
      return true;
    } else if( menuID == R.id.nav_read)
    {
      return true;
    }
    else if( menuID == R.id.read_board1)
    {
      if(SMTHApplication.ReadBoardEng1 != null) {
        // Board item = new Board(null, SMTHApplication.ReadBoard1, SMTHApplication.ReadBoardEng1);
        Board board = new Board();
        board.initAsBoard(SMTHApplication.ReadBoard1, SMTHApplication.ReadBoardEng1, "", "");
        startBoardTopicActivity(board);
      }
    } else if( menuID == R.id.read_board2)
    {
      if(SMTHApplication.ReadBoardEng2 != null) {
        Board board = new Board();
        board.initAsBoard(SMTHApplication.ReadBoard2, SMTHApplication.ReadBoardEng2, "", "");
        startBoardTopicActivity(board);
      }
    }else if( menuID == R.id.read_board3)
    {
      if(SMTHApplication.ReadBoardEng3 != null) {
        Board board = new Board();
        board.initAsBoard(SMTHApplication.ReadBoard3, SMTHApplication.ReadBoardEng3, "", "");
        startBoardTopicActivity(board);
      }
    }


    // switch fragment
    if (fragment != null) {
      mDrawer.closeDrawer(GravityCompat.START);
      FragmentManager fm = getSupportFragmentManager();
      fm.beginTransaction().replace(R.id.content_frame, fragment).commit();

      // favorite board will manage the title by itself. its title will vary depending on folder path
      if (fragment != favoriteBoardFragment) {
        setTitle(SMTHApplication.App_Title_Prefix + title);
      }
    }

    return true;
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.nav_user_avatar || id == R.id.nav_user_name) {
      mDrawer.closeDrawer(GravityCompat.START);
      if (SMTHApplication.activeUser != null && !SMTHApplication.activeUser.getId().equals("guest")) {
        Intent intent = new Intent(this, QueryUserActivity.class);
        intent.putExtra(SMTHApplication.QUERY_USER_INFO, SMTHApplication.activeUser.getId());
        startActivity(intent);
      } else {
        onLogin();
      }
    }
  }

  @Override public void onTopicFragmentInteraction(Topic item) {
    // will be triggered in HotTopicFragment
    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
    if (fragment == hotTopicFragment) {
      if (item.isCategory) return;

      if (!item.getBoardChsName().equals(SMTHApplication.ReadBoard1) && !item.getBoardChsName().equals(SMTHApplication.ReadBoard2)
              && !item.getBoardChsName().equals(SMTHApplication.ReadBoard3)) {
        switch (SMTHApplication.ReadBoardCount % 3) {
          case 0:
            SMTHApplication.ReadBoard1 = item.getBoardChsName();
            SMTHApplication.ReadBoardEng1 = item.getBoardEngName();
            break;
          case 1:
            SMTHApplication.ReadBoard2 = item.getBoardChsName();
            SMTHApplication.ReadBoardEng2 = item.getBoardEngName();
            break;
          case 2:
            SMTHApplication.ReadBoard3 =item.getBoardChsName();
            SMTHApplication.ReadBoardEng3 = item.getBoardEngName();
            break;
        }
        SMTHApplication.ReadBoardCount++;
      }

      Intent intent = new Intent(this, PostListActivity.class);
      intent.putExtra(SMTHApplication.TOPIC_OBJECT, item);
      intent.putExtra(SMTHApplication.READ_MODE, "1");
      intent.putExtra(SMTHApplication.FROM_BOARD, SMTHApplication.FROM_BOARD_HOT);
      startActivity(intent);
    }
  }

  @Override public void onMailInteraction(Mail item, int position) {
    if (item.isCategory) return;

    // mark item as read
    mailListFragment.markMailAsRead(position);
    // MailListFragment
    Intent intent = new Intent(this, MailContentActivity.class);
    intent.putExtra(SMTHApplication.MAIL_OBJECT, item);
    startActivity(intent);
  }

  @Override public void onBoardFragmentInteraction(Board item) {
    // shared by FavoriteBoard & AllBoard fragment
    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
    Log.d(TAG, item.toString());
    if (fragment == favoriteBoardFragment) {
      // favorite fragment, we might enter a folder or section
      if (item.isFolder() || item.isSection()) {
        favoriteBoardFragment.pushPath(item);
        favoriteBoardFragment.RefreshFavoriteBoards();
        return;
      }
    }

    // if it's a normal board, show postlist in the board
    if(item.isBoard()) {
      startBoardTopicActivity(item);
    }
  }

  @Override public void onBoardLongClick(final Board board) {
    /*
    // shared by FavoriteBoard & AllBoard fragment
    // long click to remove board from favorite
    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
    if (fragment == favoriteBoardFragment) {
      // favorite fragment, remove the board
      if (board.isBoard()) {
        // confirm dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String title = String.format("将版面\"%s\"从收藏中删除么？", board.getBoardName());
        builder.setTitle("收藏操作").setMessage(title);

        // Log.d(TAG, favoriteBoardFragment.getCurrentFavoritePath());

        builder.setPositiveButton("删除", (dialog, which) -> {
          dialog.dismiss();

          SMTHHelper helper = SMTHHelper.getInstance();
          helper.wService.manageFavoriteBoard("0", "db", board.getBoardEngName())
                  .subscribeOn(Schedulers.io())
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(new Observer<AjaxResponse>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable disposable) {

                    }

                    @Override
                    public void onNext(@NonNull AjaxResponse ajaxResponse) {
                      //Log.d(TAG, "onNext: " + ajaxResponse.toString());
                      if (ajaxResponse.getAjax_st() == AjaxResponse.AJAX_RESULT_OK) {
                        //Toast.makeText(MainActivity.this, ajaxResponse.getAjax_msg() + "\n" + "请刷新收藏！", Toast.LENGTH_SHORT).show();
                        refreshCurrentFragment();
                      } else {
                        Toast.makeText(MainActivity.this, ajaxResponse.toString(), Toast.LENGTH_SHORT).show();
                      }

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                      Toast.makeText(MainActivity.this, "删除收藏版面失败！\n" + e.toString(), Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onComplete() {

                    }
                  });
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        AlertDialog noticeDialog = builder.create();
        noticeDialog.show();
      } else if(board.isSection()) {
        //* + confirm Folder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String title = String.format("将版面二级目录\"%s\"从收藏中删除么？", board.getFolderName());
        builder.setTitle("收藏操作").setMessage(title);

        builder.setPositiveButton("删除", (dialog, which) -> {
          dialog.dismiss();

          SMTHHelper helper = SMTHHelper.getInstance();
          //Log.d(TAG, favoriteBoardFragment.getCurrentPathInString());
          helper.wService.manageFavoriteBoard("0", "db", board.getSectionID())
                  .subscribeOn(Schedulers.io())
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(new Observer<AjaxResponse>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable disposable) {

                    }

                    @Override
                    public void onNext(@NonNull AjaxResponse ajaxResponse) {
                      //(TAG, "onNext: " + ajaxResponse.toString());
                      if (ajaxResponse.getAjax_st() == AjaxResponse.AJAX_RESULT_OK) {
                        //Toast.makeText(MainActivity.this, ajaxResponse.getAjax_msg() + "\n" + "请刷新收藏！", Toast.LENGTH_SHORT).show();
                        refreshCurrentFragment();
                      } else {
                        Toast.makeText(MainActivity.this, ajaxResponse.toString(), Toast.LENGTH_SHORT).show();
                      }

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                      Toast.makeText(MainActivity.this, "删除收藏目录失败！\n" + e.toString(), Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onComplete() {

                    }
                  });
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        AlertDialog noticeDialog = builder.create();
        noticeDialog.show();
      }
      else if (board.isFolder())
      {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String title = String.format("将版面二级目录\"%s\"从收藏中删除么？", board.getFolderName());
        builder.setTitle("收藏操作").setMessage(title);

        builder.setPositiveButton("删除", (dialog, which) -> {
          dialog.dismiss();

          SMTHHelper helper = SMTHHelper.getInstance();
          //Log.d(TAG, favoriteBoardFragment.getCurrentPathInString());
          helper.wService.manageFavoriteBoard("0", "db", board.getFolderID())
                  .subscribeOn(Schedulers.io())
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(new Observer<AjaxResponse>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable disposable) {

                    }

                    @Override
                    public void onNext(@NonNull AjaxResponse ajaxResponse) {
                      //Log.d(TAG, "onNext: " + ajaxResponse.toString());
                      if (ajaxResponse.getAjax_st() == AjaxResponse.AJAX_RESULT_OK) {
                        //Toast.makeText(MainActivity.this, ajaxResponse.getAjax_msg() + "\n" + "请刷新收藏！", Toast.LENGTH_SHORT).show();
                        refreshCurrentFragment();
                      } else {
                        Toast.makeText(MainActivity.this, ajaxResponse.toString(), Toast.LENGTH_SHORT).show();
                      }

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                      Toast.makeText(MainActivity.this, "删除收藏目录失败！\n" + e.toString(), Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onComplete() {

                    }
                  });
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        AlertDialog noticeDialog = builder.create();
        noticeDialog.show();
      }

    }

     */
    //- confirm Folder */
  }


  public void startBoardTopicActivity(Board board) {
    Intent intent = new Intent(this, BoardTopicActivity.class);
    intent.putExtra(SMTHApplication.BOARD_OBJECT, (Parcelable) board);
    startActivity(intent);
    // 禁用跳转动画
    overridePendingTransition(0, 0);
  }

  class PendingDoubleBackToExit implements Runnable { public void run() {
    mDoubleBackToExit = false;
  }
  }

  private void setDrawerLeftEdgeSize (Activity activity, DrawerLayout drawerLayout, float displayWidthPercentage) {
    if (activity == null || drawerLayout == null) return;
    try {
      Field leftDraggerField =
              drawerLayout.getClass().getDeclaredField("mLeftDragger");//Right
      leftDraggerField.setAccessible(true);
      ViewDragHelper leftDragger = (ViewDragHelper) leftDraggerField.get(drawerLayout);

      if (leftDragger == null) {
        Log.e(TAG, "leftDragger is null.");
        return;
      }

      Field edgeSizeField = leftDragger.getClass().getDeclaredField("mEdgeSize");
      edgeSizeField.setAccessible(true);
      int edgeSize = edgeSizeField.getInt(leftDragger);

      Point displaySize = new Point();
      activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
      edgeSizeField.setInt(leftDragger, Math.max(edgeSize, (int) (displaySize.x *
              displayWidthPercentage)));
    } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ignored) {
    }
  }

  @Override
  protected void onRestart(){
    super.onRestart();
    if(SMTHApplication.isValidUser()) {
      onRelogin();
    }
  }

  public void onRelogin() {
    SMTHHelper helper = SMTHHelper.getInstance();
    helper.wService.queryUserInformation(SMTHApplication.activeUser.getId())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<UserInfo>() {
              @Override public void onSubscribe(@NonNull Disposable disposable) {

              }

              @Override public void onNext(@NonNull UserInfo user) {
                Log.d(TAG, "onNext: " + user.toString());

                if (!user.is_online()) {
                  //Toast.makeText(getApplicationContext(),"掉线将自动登录！", Toast.LENGTH_SHORT).show();
                  NewToast.makeText(getApplicationContext(),"掉线将自动登录！", Toast.LENGTH_SHORT);
                  onLogin();
                }
              }

              @Override public void onError(@NonNull Throwable e) {
                //Toast.makeText(getApplicationContext(), "用户掉线！" , Toast.LENGTH_SHORT).show();
                NewToast.makeText(getApplicationContext(), "用户掉线！" , Toast.LENGTH_SHORT);
              }

              @Override public void onComplete() {

              }
            });
  }

  public void refreshCurrentFragment() {
    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
    if (currentFragment instanceof FavoriteBoardFragment) {
      FavoriteBoardFragment favoriteBoardFragment = (FavoriteBoardFragment) currentFragment;
      favoriteBoardFragment.RefreshFavoriteBoardsWithCache();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    unregisterReceiver(userStatusReceiver);
  }

  public void setBadgeCount(int itemId, String count) {
    MenuItem menuItem = mBottomNavigationView.getMenu().findItem(itemId);

    if (menuItem != null) {
      Drawable icon = menuItem.getIcon();
      if (icon != null) {
        if(SMTHApplication.bNewMailInNotification && SMTHApplication.bNightModeChange)
          return;
        LayerDrawable layerDrawable = createLayerDrawable(icon, count);
        menuItem.setIcon(layerDrawable);
        SMTHApplication.bNewMailInNotification = true;
      }

    }
  }

  public void clearBadgeCount(int itemId) {
    MenuItem menuItem = mBottomNavigationView.getMenu().findItem(itemId);
    if (menuItem != null) {
      menuItem.setIcon(R.drawable.ic_email_white_48dp); // 恢复原始图标
      SMTHApplication.bNewMailInNotification = false;
    }
  }


  private LayerDrawable createLayerDrawable(Drawable icon, String count) {
    int badgeSize = 72; // 调整角标的大小
    float textSize = getResources().getDimension(R.dimen.badge_text_size);
    int badgeColor = ContextCompat.getColor(this, R.color.badge_background);
    int textColor = ContextCompat.getColor(this, R.color.avatar_border_color);

    // 创建角标 Drawable 的可变副本
    BadgeDrawable badgeDrawable = new BadgeDrawable(textSize, badgeColor, textColor, badgeSize);
    badgeDrawable.setBadgeText(String.valueOf(count));

    // 创建一个 LayerDrawable 来叠加图标和角标
    if (icon != null) {
      Drawable.ConstantState constantState = icon.getConstantState();
      if (constantState != null) {
        Drawable iconCopy = constantState.newDrawable().mutate();
        badgeDrawable.mutate();

        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{iconCopy});
        layerDrawable.addLayer(badgeDrawable);

        // 设置角标的位置
        int width = icon.getIntrinsicWidth();
        int height = icon.getIntrinsicHeight();
        int badgeWidth = badgeDrawable.getIntrinsicWidth();
        int badgeHeight = badgeDrawable.getIntrinsicHeight();

        // 设置角标的位置为右上角
        layerDrawable.setLayerInset(1, width - badgeWidth, 0, 0, height - badgeHeight);

        return layerDrawable;
      } else {
        Log.e(TAG, "ConstantState is null for the icon.");
        return null;
      }
    } else {
      Log.e(TAG, "Icon is null.");
      return null;
    }

  }

  public Fragment getCurrentFragment() {
    return getSupportFragmentManager().findFragmentById(R.id.content_frame);
  }

  @Override
  protected void onSaveInstanceState(@androidx.annotation.NonNull @NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(KEY_CURRENT_FRAGMENT, currentFragmentId);

    // 保存 MailListFragment 的状态
    if (mailListFragment != null && mailListFragment.isAdded()) {
      Bundle mailListState = new Bundle();
      mailListFragment.onSaveInstanceState(mailListState);
      outState.putBundle("mail_list_fragment_state", mailListState);
    }
  }
  private Fragment getFragmentById(int fragmentId) {
    Fragment fragment = null;
    if (fragmentId == R.id.nav_guidance) {
      fragment = hotTopicFragment;
    } else if (fragmentId == R.id.nav_favorite) {
      fragment = favoriteBoardFragment;
    } else if (fragmentId == R.id.nav_all_boards) {
      fragment = allBoardFragment;
    } else if (fragmentId == R.id.nav_mail) {
      fragment = mailListFragment;
    } else if (fragmentId == R.id.nav_setting) {
      fragment = preferenceFragment;
    }
    return fragment;
  }

  private String getTitleByFragmentId(int fragmentId) {
    if (fragmentId == R.id.nav_guidance) {
      return "首页";
    } else if (fragmentId == R.id.nav_favorite) {
      return "收藏";
    } else if (fragmentId == R.id.nav_all_boards) {
      return "分区";
    } else if (fragmentId == R.id.nav_mail) {
      return "收件箱";
    } else if (fragmentId == R.id.nav_setting) {
      return "设置";
    } else if (fragmentId == R.id.nav_about) {
      return "关于";
    } else {
      // Default case
      return "";
    }
  }

  private int getCurrentFragmentId() {
    Fragment currentFragment = getCurrentFragment();
    if (currentFragment == hotTopicFragment) {
      return R.id.nav_guidance;
    } else if (currentFragment == favoriteBoardFragment) {
      return R.id.nav_favorite;
    } else if (currentFragment == allBoardFragment) {
      return R.id.nav_all_boards;
    } else if (currentFragment == mailListFragment) {
      return R.id.nav_mail;
    } else if (currentFragment == preferenceFragment) {
      return R.id.nav_setting;
    } else if (currentFragment == aboutFragment) {
      return R.id.nav_about;
    }
    return -1;
  }

  public void handleRightSwipe() {
    // 处理右滑逻辑，例如打开 DrawerLayout
    if (mDrawer != null) {
      mDrawer.openDrawer(GravityCompat.START);
    }
  }
}
