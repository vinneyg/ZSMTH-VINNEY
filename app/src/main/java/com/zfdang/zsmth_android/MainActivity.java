package com.zfdang.zsmth_android;


import android.annotation.SuppressLint;
//import android.app.ActionBar;
import android.app.Activity;

import android.app.AlertDialog;
//import android.app.Fragment;
//import android.app.FragmentManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
//import android.content.res.ColorStateList;
import android.content.res.ColorStateList;
//import android.graphics.Color;
import android.graphics.Point;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;

import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.ActionBar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.customview.widget.ViewDragHelper;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
//import androidx.preference.CheckBoxPreference;
//import androidx.appcompat.app.ActionBar;
//import androidx.preference.PreferenceFragment;

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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
//import android.widget.Toolbar;

import com.mikepenz.aboutlibraries.LibsBuilder;
//import com.mikepenz.aboutlibraries.ui.LibsSupportFragment;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;
import com.umeng.analytics.MobclickAgent;
import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.fresco.WrapContentDraweeView;
import com.zfdang.zsmth_android.listeners.OnBoardFragmentInteractionListener;
import com.zfdang.zsmth_android.listeners.OnMailInteractionListener;
import com.zfdang.zsmth_android.listeners.OnTopicFragmentInteractionListener;
import com.zfdang.zsmth_android.listeners.OnVolumeUpDownListener;
import com.zfdang.zsmth_android.listeners.ShakeListener;
import com.zfdang.zsmth_android.models.Board;
import com.zfdang.zsmth_android.models.Mail;
import com.zfdang.zsmth_android.models.MailListContent;
//import com.zfdang.zsmth_android.models.PostListContent;
import com.zfdang.zsmth_android.models.Topic;
import com.zfdang.zsmth_android.newsmth.AjaxResponse;
import com.zfdang.zsmth_android.newsmth.SMTHHelper;
import com.zfdang.zsmth_android.services.AlarmBroadcastReceiver;

import com.zfdang.zsmth_android.services.UserStatusReceiver;

import java.lang.reflect.Field;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.google.android.material.bottomnavigation.LabelVisibilityMode.LABEL_VISIBILITY_UNLABELED;


public class MainActivity extends SMTHBaseActivity
    implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener, OnTopicFragmentInteractionListener,
    OnBoardFragmentInteractionListener, OnMailInteractionListener {
  // used by startActivityForResult
  static final int LOGIN_ACTIVITY_REQUEST_CODE = 9527;  // The request code
  private static final String TAG = "MainActivity";
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

  private UserStatusReceiver mReceiver;
  // press BACK in 2 seconds, app will quit
  private boolean mDoubleBackToExit = false;
  private Handler mHandler = null;
  private FloatingActionMenu mActionMenu;
  private NavigationView mNavigationView;

  private static final int notificationID = 273;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    // how to adjust the height of toolbar
    // http://stackoverflow.com/questions/17439683/how-to-change-action-bar-size
    // zsmth_actionbar_size @ dimen ==> ThemeOverlay.ActionBar @ styles ==> theme @ app_bar_main.xml

    // init floating action button & circular action menu
    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    initCircularActionMenu(fab);

    initBottomNavigation();
    // Add Vinney Switch
    // fab.hide()
    //BottomNavigationView mBottomNavigationView = findViewById(R.id.bv_bottomNavigation);
    if (Settings.getInstance().isLaunchBottomNavi()) {
      fab.hide();
      findViewById(R.id.bv_bottomNavigation).setVisibility(View.VISIBLE);
    } else {
      fab.show();
      findViewById(R.id.bv_bottomNavigation).setVisibility(View.GONE);
    }

    mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    if(Settings.getInstance().isLeftNavSlide())
      setDrawerLeftEdgeSize(this, mDrawer, (float) 1);//To support Mail Deletion
    else
      setDrawerLeftEdgeSize(this, mDrawer, (float) 0.15);

    mDrawer.addDrawerListener(new ActionBarDrawerToggle(this, mDrawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
      @Override
      public void onDrawerOpened(View drawerView) {
        //mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        Menu menu = ((NavigationView) findViewById(R.id.nav_view)).getMenu();

        menu.findItem(R.id.read_board1).setTitle(SMTHApplication.ReadBoard1);
        menu.findItem(R.id.read_board2).setTitle(SMTHApplication.ReadBoard2);
        menu.findItem(R.id.read_board3).setTitle(SMTHApplication.ReadBoard3);

        super.onDrawerOpened(drawerView);
      }
    });

    mToggle = new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    //mDrawer.addDrawerListener(mToggle);
    mToggle.syncState();
    //Vinney Full Screen Drawer

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

    mUsername =  headerView.findViewById(R.id.nav_user_name);
    mUsername.setOnClickListener(this);

    // http://stackoverflow.com/questions/27097126/marquee-title-in-toolbar-actionbar-in-android-with-lollipop-sdk
    TextView titleTextView ;
    try {
      Field f = toolbar.getClass().getDeclaredField("mTitleTextView");
      f.setAccessible(true);
      titleTextView = (TextView) f.get(toolbar);
      titleTextView.setEllipsize(TextUtils.TruncateAt.START);
    } catch (NoSuchFieldException e) {
    } catch (IllegalAccessException e) {
    }

    // init all fragments
    initFragments();

   FragmentManager fm = getSupportFragmentManager();
    if (Settings.getInstance().isLaunchHotTopic()) {
      fm.beginTransaction().replace(R.id.content_frame, hotTopicFragment).commit();
    } else {
      fm.beginTransaction().replace(R.id.content_frame, favoriteBoardFragment).commit();
    }

    getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
      public void onBackStackChanged() {
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
      }
    });

    // setup receiver to receive user status update from periodical background service
    setupUserStatusReceiver();

    // schedule the periodical background service
    AlarmBroadcastReceiver.schedule(getApplicationContext(), mReceiver);
    // run the background service now
    updateUserStatusNow();
    UpdateNavigationViewHeader();

    if (Settings.getInstance().isFirstRun()) {
      // show info dialog after 5 seconds for the first run
      final Handler handler = new Handler();
      handler.postDelayed(new Runnable() {
        @Override
        public void run() {
          showInfoDialog();
        }
      }, 1000);
    }


    ShakeListener shakeListener = new ShakeListener(this);
    shakeListener.setOnShakeListener(new ShakeListener.OnShakeListener() {
      @Override
      public void onShake() {
        // onVibrator(getApplicationContext());
        quitNow();
      }
    });
  }

  /*
  //Vinney for test
  private void onVibrator(Context context) {
    Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    if (vibrator == null) {
      Vibrator localVibrator = (Vibrator) context.getApplicationContext()
              .getSystemService(Context.VIBRATOR_SERVICE);
      vibrator = localVibrator;
    }
    vibrator.vibrate(100L);
  }
   */



    @SuppressLint("RestrictedApi")
    public  void disableShiftMode(BottomNavigationView view) {
      //获取子View BottomNavigationMenuView的对象
      BottomNavigationMenuView menuView = (BottomNavigationMenuView) view.getChildAt(0);
      try {
        //设置私有成员变量mShiftingMode可以修改
        Field shiftingMode = menuView.getClass().getDeclaredField("mShiftingMode");
        shiftingMode.setAccessible(true);
        shiftingMode.setBoolean(menuView, false);
        shiftingMode.setAccessible(false);
        for (int i = 0; i < menuView.getChildCount(); i++) {
          BottomNavigationItemView item = (BottomNavigationItemView) menuView.getChildAt(i);
          //去除shift效果
          item.setShifting(false);
          item.setChecked(item.getItemData().isChecked());
        }
      } catch (NoSuchFieldException e) {
        Log.e("BNVHelper", "没有mShiftingMode这个成员变量", e);
      } catch (IllegalAccessException e) {
        Log.e("BNVHelper", "无法修改mShiftingMode的值", e);
      }
    }


  //Vinney
  private void initBottomNavigation() {
    BottomNavigationView mBottomNavigationView = findViewById(R.id.bv_bottomNavigation);

    mBottomNavigationView.setItemIconTintList(null);
    mBottomNavigationView.setItemTextAppearanceActive(R.style.bottom_selected_text);
    mBottomNavigationView.setItemTextAppearanceInactive(R.style.bottom_normal_text);
    //Vinney to do
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

    //disableShiftMode(mBottomNavigationView);

    mBottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
      @Override
      public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
          case R.id.menu_discover:
            onNavigationItemID(R.id.nav_guidance);
            mNavigationView.setCheckedItem(R.id.nav_guidance);
            break;
          case R.id.menu_star:
            onNavigationItemID(R.id.nav_favorite);
            mNavigationView.setCheckedItem(R.id.nav_favorite);
            break;
          case R.id.menu_list:
            onNavigationItemID(R.id.nav_all_boards);
            mNavigationView.setCheckedItem(R.id.nav_all_boards);
            break;
          case R.id.menu_message:
            onNavigationItemID(R.id.nav_mail);
            mNavigationView.setCheckedItem(R.id.nav_mail);
          default:
            break;
        }
        return true;
      }
    });

  }


  private void initCircularActionMenu(FloatingActionButton fab) {
    SubActionButton.Builder itemBuilder = new SubActionButton.Builder(this);

    ImageView itemIcon1 = new ImageView(this);
    itemIcon1.setImageDrawable(getResources().getDrawable(R.drawable.ic_whatshot_white_48dp,null));
    SubActionButton button1 = itemBuilder.setContentView(itemIcon1)
        .setBackgroundDrawable(getResources().getDrawable(R.drawable.navigation_button_background,null))
        .build();
    button1.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        mActionMenu.close(true);
        onNavigationItemID(R.id.nav_guidance);
        mNavigationView.setCheckedItem(R.id.nav_guidance);
      }
    });

    ImageView itemIcon2 = new ImageView(this);
    itemIcon2.setImageDrawable(getResources().getDrawable(R.drawable.ic_star_white_48dp,null));
    SubActionButton button2 = itemBuilder.setContentView(itemIcon2)
        .setBackgroundDrawable(getResources().getDrawable(R.drawable.navigation_button_background,null))
        .build();
    button2.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        mActionMenu.close(true);
        onNavigationItemID(R.id.nav_favorite);
        mNavigationView.setCheckedItem(R.id.nav_favorite);
      }
    });

    ImageView itemIcon3 = new ImageView(this);
    itemIcon3.setImageDrawable(getResources().getDrawable(R.drawable.ic_format_list_bulleted_white_48dp,null));
    SubActionButton button3 = itemBuilder.setContentView(itemIcon3)
        .setBackgroundDrawable(getResources().getDrawable(R.drawable.navigation_button_background,null))
        .build();
    button3.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        mActionMenu.close(true);
        onNavigationItemID(R.id.nav_all_boards);
        mNavigationView.setCheckedItem(R.id.nav_all_boards);
      }
    });

    ImageView itemIcon4 = new ImageView(this);
    itemIcon4.setImageDrawable(getResources().getDrawable(R.drawable.ic_email_white_48dp,null));
    SubActionButton button4 = itemBuilder.setContentView(itemIcon4)
        .setBackgroundDrawable(getResources().getDrawable(R.drawable.navigation_button_background,null))
        .build();
    button4.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        mActionMenu.close(true);
        onNavigationItemID(R.id.nav_mail);
        mNavigationView.setCheckedItem(R.id.nav_mail);
      }
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
    AlarmBroadcastReceiver.runJobNow(getApplicationContext(), mReceiver);
  }

  private void setupUserStatusReceiver() {
    mReceiver = new UserStatusReceiver(new Handler());
    mReceiver.setReceiver(new UserStatusReceiver.Receiver() {
      @Override
      public void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultCode == RESULT_OK) {
          //Log.d(TAG, "onReceiveResult: " + "to update navigationview" + SMTHApplication.activeUser.toString());
          UpdateNavigationViewHeader();

          // show notification if necessary
          String message = resultData.getString(SMTHApplication.SERVICE_NOTIFICATION_MESSAGE);
          //Log.d(TAG, "OnReceiveResult" + message);
          if (message != null) {
            showNotification(message);
          }
        }
      }
    });
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

      Notification.Builder mBuilder = new Notification.Builder(this).setSmallIcon(R.drawable.ic_launcher)
              .setContentTitle("zSMTH-v提醒")
              .setWhen(System.currentTimeMillis())
              .setAutoCancel(true)
              .setOnlyAlertOnce(true)
              .setDefaults(Notification.DEFAULT_VIBRATE)
              .setContentText(text)
              .setContentIntent(resultPendingIntent);

      NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        mBuilder.setChannelId(getPackageName()); //必须添加（Android 8.0） 【唯一标识】
        NotificationChannel channel = new NotificationChannel(
                getPackageName(),
                "zSMTH-v通知消息",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        mNotifyMgr.createNotificationChannel(channel);
      }

      Notification notification = mBuilder.build();
      mNotifyMgr.notify(notificationID, notification);
    } catch (Exception se) {
      Log.e(TAG, "showNotification: " + se.toString());
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
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
  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    // disable the beep sound when volume up/down is pressed
    if (Settings.getInstance().isVolumeKeyScroll() && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
      return true;
    }
    return super.onKeyUp(keyCode, event);
  }

  @Override
  protected void onPause() {
    super.onPause();
    MobclickAgent.onPause(this);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    // this method will be triggered by showNotification(message);
    FragmentManager fm = getSupportFragmentManager();
    Bundle bundle = intent.getExtras();
    if (bundle != null) {
      // this activity is launched by notification, show mail fragment now
      // http://www.androiddesignpatterns.com/2013/08/fragment-transaction-commit-state-loss.html
      // http://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-wit
      // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
      String message = bundle.getString(SMTHApplication.SERVICE_NOTIFICATION_MESSAGE);
      if (message != null) {
        // find the actual folder for the new message
        String subTitle = "收件箱";
        if (message.contains(SMTHApplication.NOTIFICATION_NEW_MAIL)) {
          mailListFragment.setCurrentFolder(MailListFragment.INBOX_LABEL);
        } else if (message.contains(SMTHApplication.NOTIFICATION_NEW_LIKE)) {
          subTitle = "Like我";
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
        //setTitle(SMTHApplication.App_Title_Prefix + "邮件");
        setTitle(SMTHApplication.App_Title_Prefix + subTitle);
      }
    }
    super.onNewIntent(intent);
  }

  @Override
  protected void onResume() {
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

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == LOGIN_ACTIVITY_REQUEST_CODE) {
      if (resultCode == RESULT_OK) {
        updateUserStatusNow();
      }
    }
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuItem login = menu.findItem(R.id.main_action_login);
    MenuItem logout = menu.findItem(R.id.main_action_logout);
    if (SMTHApplication.isValidUser()) {
      login.setVisible(false);
      logout.setVisible(true);
    } else {
      login.setVisible(true);
      logout.setVisible(false);
    }
    return super.onPrepareOptionsMenu(menu);
  }

  // update header view in navigation header
  private void UpdateNavigationViewHeader() {
    // update optionMenu
    getWindow().invalidatePanelMenu(Window.FEATURE_OPTIONS_PANEL);

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

  @Override
  public void onBackPressed() {
    if (mDrawer.isDrawerOpen(GravityCompat.START)) {
      mDrawer.closeDrawer(GravityCompat.START);
      return;
    }

    // handle back button for all fragment
    androidx.fragment.app.Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
    if (fragment instanceof FavoriteBoardFragment) {
      if (!favoriteBoardFragment.isAtRoot()) {
        favoriteBoardFragment.popPath();
        favoriteBoardFragment.RefreshFavoriteBoards();
        return;
      }
    }

    if (fragment != hotTopicFragment) {
      // return to hottopic if we are not there yet
      String title = "首页";
      FragmentManager fm = getSupportFragmentManager();
      fm.beginTransaction().replace(R.id.content_frame, hotTopicFragment).commit();
      setTitle(SMTHApplication.App_Title_Prefix + title);
      return;
    }

    // for other cases, double back to exit app
    DoubleBackToExit();
  }

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
      Toast.makeText(this, "再按一次退出zSMTH-v", Toast.LENGTH_SHORT).show();
    }
  }

  private void quitNow() {
    // stop background service
    AlarmBroadcastReceiver.unschedule();

    // quit
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

    final android.app.AlertDialog dlg =
            new android.app.AlertDialog.Builder(this).setIcon(R.drawable.ic_launcher).setTitle(R.string.about_title).setMessage(msg).create();

    dlg.setButton(android.app.AlertDialog.BUTTON_POSITIVE, getString(R.string.about_close), new DialogInterface.OnClickListener() {

      @Override
      public void onClick(DialogInterface dialog, int which) {
        // do nothing here
      }
    });

    dlg.show();

    // Make the textview clickable. Must be called after show()
    ((TextView) dlg.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
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
      this.onBackPressed();
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  public void onLogin() {
    Intent intent = new Intent(this, LoginActivity.class);
    startActivityForResult(intent, LOGIN_ACTIVITY_REQUEST_CODE);
  }

  public void onLogout() {
    Settings.getInstance().setUserOnline(false);
    if (SMTHApplication.activeUser != null) {
      SMTHApplication.activeUser.setId("guest");
    }
    UpdateNavigationViewHeader();

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
                Toast.makeText(MainActivity.this, ajaxResponse.getAjax_msg(), Toast.LENGTH_SHORT).show();
              }

              @Override
              public void onError(@NonNull Throwable e) {
                Toast.makeText(MainActivity.this, "退出登录失败!\n" + e.toString(), Toast.LENGTH_SHORT).show();
              }

              @Override
              public void onComplete() {

              }
            });
    Settings.getInstance().setAutoLogin(false);
  }

  //@SuppressWarnings("StatementWithEmptyBody")
  @Override
  public boolean onNavigationItemSelected(MenuItem item) {
    int id = item.getItemId();
    return onNavigationItemID(id);
  }

  public boolean onNavigationItemID(int menuID) {
    // Handle navigation view item clicks here.
    int id = menuID;

    Fragment fragment = null;
    String title = "";

    if (id == R.id.nav_guidance) {
      fragment = hotTopicFragment;
      title = "首页";
    } else if (id == R.id.nav_favorite) {
      fragment = favoriteBoardFragment;
      title = "收藏";
    } else if (id == R.id.nav_all_boards) {
      fragment = allBoardFragment;
      title = "版面";
    } else if (id == R.id.nav_mail) {
      fragment = mailListFragment;
      title = "消息";
    } else if (id == R.id.nav_setting) {
      //            fragment = settingFragment;
      fragment = preferenceFragment;
      title = "设置";
    } else if (id == R.id.nav_about) {
      fragment = aboutFragment;
      title = "关于";
    } else if (id == R.id.nav_exit)
    {
      quitNow();
    } else if( id == R.id.nav_read)
    {
     //Toast.makeText(this, "Click boards below",Toast.LENGTH_SHORT);
      return true;
    }
    else if( id == R.id.read_board1)
    {
      if(SMTHApplication.ReadBoardEng1 != null) {
       // Board item = new Board(null, SMTHApplication.ReadBoard1, SMTHApplication.ReadBoardEng1);
        Board board = new Board();
        board.initAsBoard(SMTHApplication.ReadBoard1, SMTHApplication.ReadBoardEng1, "", "");
        startBoardTopicActivity(board);
      }
    } else if( id == R.id.read_board2)
    {
      if(SMTHApplication.ReadBoardEng2 != null) {
        Board board = new Board();
        board.initAsBoard(SMTHApplication.ReadBoard2, SMTHApplication.ReadBoardEng2, "", "");
        startBoardTopicActivity(board);
      }
    }else if( id == R.id.read_board3)
    {
      if(SMTHApplication.ReadBoardEng3 != null) {
        Board board = new Board();
        board.initAsBoard(SMTHApplication.ReadBoard3, SMTHApplication.ReadBoardEng3, "", "");
        startBoardTopicActivity(board);
      }
    }

    // switch fragment
    if (fragment != null) {
      FragmentManager fm = getSupportFragmentManager();
      fm.beginTransaction().replace(R.id.content_frame, fragment).commit();

      // favorite board will manage the title by itself. its title will vary depending on folder path
      if (fragment != favoriteBoardFragment) {
        setTitle(SMTHApplication.App_Title_Prefix + title);
      }
    }

    mDrawer.closeDrawer(GravityCompat.START);
    return true;
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.nav_user_avatar || id == R.id.nav_user_name) {
      // 点击图标或者文字，都弹出登录对话框或者profile对话框
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

  @Override
  public void onTopicFragmentInteraction(Topic item) {
    // will be triggered in HotTopicFragment
    androidx.fragment.app.Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
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
      intent.putExtra(SMTHApplication.FROM_BOARD, SMTHApplication.FROM_BOARD_HOT);
      startActivity(intent);
    }
  }

  @Override
  public void onMailInteraction(Mail item, int position) {
    if (item.isCategory) return;

    // mark item as read
    mailListFragment.markMailAsReaded(position);
    // MailListFragment
    Intent intent = new Intent(this, MailContentActivity.class);
    intent.putExtra(SMTHApplication.MAIL_OBJECT, item);
    startActivity(intent);
  }

  @Override
  public void onBoardFragmentInteraction(Board item) {
    // shared by FavoriteBoard & AllBoard fragment
    androidx.fragment.app.Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
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

  @Override
  public void onBoardLongClick(final Board board) {
    // shared by FavoriteBoard & AllBoard fragment
    // long click to remove board from favorite
    androidx.fragment.app.Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
    if (fragment == favoriteBoardFragment) {
      // favorite fragment, remove the board
      if (board.isBoard()) {
        // confirm dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        String title = String.format("将版面\"%s\"从收藏中删除么？", board.getBoardName());
        builder.setTitle("收藏操作").setMessage(title);

       // Log.d(TAG, favoriteBoardFragment.getCurrentFavoritePath());

        builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
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
                        Log.d(TAG, "onNext: " + ajaxResponse.toString());
                        if (ajaxResponse.getAjax_st() == AjaxResponse.AJAX_RESULT_OK) {
                          Toast.makeText(MainActivity.this, ajaxResponse.getAjax_msg() + "\n" + "请刷新收藏！", Toast.LENGTH_SHORT).show();
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
          }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
          }
        });
        android.app.AlertDialog noticeDialog = builder.create();
        noticeDialog.show();
      } else if(board.isSection()) {
        //* +Vinney confirm Folder
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        String title = String.format("将版面二级目录\"%s\"从收藏中删除么？", board.getFolderName());
        builder.setTitle("收藏操作").setMessage(title);

        builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();

            SMTHHelper helper = SMTHHelper.getInstance();
            //Log.d(TAG, "Vinney: " + board.getFolderID() + "&&" + board.getFolderName() + "&&" + String.valueOf(Integer.parseInt(board.getFolderID()) - 1));
            Log.d(TAG, favoriteBoardFragment.getCurrentPathInString());
            helper.wService.manageFavoriteBoard("0", "db", board.getSectionID())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<AjaxResponse>() {
                      @Override
                      public void onSubscribe(@NonNull Disposable disposable) {

                      }

                      @Override
                      public void onNext(@NonNull AjaxResponse ajaxResponse) {
                        Log.d(TAG, "onNext: " + ajaxResponse.toString());
                        if (ajaxResponse.getAjax_st() == AjaxResponse.AJAX_RESULT_OK) {
                          Toast.makeText(MainActivity.this, ajaxResponse.getAjax_msg() + "\n" + "请刷新收藏！", Toast.LENGTH_SHORT).show();
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
          }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
          }
        });
        android.app.AlertDialog noticeDialog = builder.create();
        noticeDialog.show();
      }
      else if (board.isFolder())
      {
          android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
          String title = String.format("将版面二级目录\"%s\"从收藏中删除么？", board.getFolderName());
          builder.setTitle("收藏操作").setMessage(title);

          builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              dialog.dismiss();

              SMTHHelper helper = SMTHHelper.getInstance();
              //Log.d(TAG, "Vinney: " + board.getFolderID() + "&&" + board.getFolderName() + "&&" + String.valueOf(Integer.parseInt(board.getFolderID()) - 1));
              Log.d(TAG, favoriteBoardFragment.getCurrentPathInString());
              helper.wService.manageFavoriteBoard("0", "db", board.getFolderID())
                      .subscribeOn(Schedulers.io())
                      .observeOn(AndroidSchedulers.mainThread())
                      .subscribe(new Observer<AjaxResponse>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable disposable) {

                        }

                        @Override
                        public void onNext(@NonNull AjaxResponse ajaxResponse) {
                          Log.d(TAG, "onNext: " + ajaxResponse.toString());
                          if (ajaxResponse.getAjax_st() == AjaxResponse.AJAX_RESULT_OK) {
                            Toast.makeText(MainActivity.this, ajaxResponse.getAjax_msg() + "\n" + "请刷新收藏！", Toast.LENGTH_SHORT).show();
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
            }
          });
          builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              dialog.dismiss();
            }
          });
          AlertDialog noticeDialog = builder.create();
          noticeDialog.show();
        }
      }
      //-Vinney confirm Folder */
  }


  public void startBoardTopicActivity(Board board) {
    Intent intent = new Intent(this, BoardTopicActivity.class);
    intent.putExtra(SMTHApplication.BOARD_OBJECT, (Parcelable) board);
    startActivity(intent);
  }

  class PendingDoubleBackToExit implements Runnable {
    public void run() {
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

      Field edgeSizeField = leftDragger.getClass().getDeclaredField("mEdgeSize");
      edgeSizeField.setAccessible(true);
      int edgeSize = edgeSizeField.getInt(leftDragger);

      Point displaySize = new Point();
      activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
      edgeSizeField.setInt(leftDragger, Math.max(edgeSize, (int) (displaySize.x *
              displayWidthPercentage)));
    } catch (NoSuchFieldException e) {
    } catch (IllegalArgumentException e) {
    } catch (IllegalAccessException e) {
    }
  }


}
