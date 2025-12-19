package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.helpers.ActivityUtils;
import com.zfdang.zsmth_android.helpers.FileLess;
import com.zfdang.zsmth_android.helpers.FileSizeUtil;
import com.zfdang.zsmth_android.helpers.NewToast;
import com.zfdang.zsmth_android.helpers.FragmentStatusBarUtil;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.util.Locale;
import java.util.Objects;

/**
 * Created by zfdang on 2016-5-2.
 */
public class MyPreferenceFragment extends PreferenceFragmentCompat {
    private static final String TAG = "PreferenceFragment";
    Preference fresco_cache;
    Preference okhttp3_cache;

    CheckBoxPreference signature_control;
    Preference signature_content;
    Preference web_content;
    CheckBoxPreference launch_bottom_navi;
    CheckBoxPreference launch_hottopic_as_entry;
    CheckBoxPreference open_topic_add;
    CheckBoxPreference diff_read_topic;
    CheckBoxPreference daynight_control;
    CheckBoxPreference setting_post_navigation_control;
    CheckBoxPreference auto_load_more;
    CheckBoxPreference quick_reply;
    CheckBoxPreference show_signature;
    CheckBoxPreference menu_text;
    CheckBoxPreference setting_volume_key_scroll;
    ListPreference setting_fontsize_control;
    CheckBoxPreference image_quality_control;
    CheckBoxPreference login_with_verification;
    CheckBoxPreference ssl_verification;
    CheckBoxPreference image_source_cdn;
    CheckBoxPreference notification_control_mail;
    CheckBoxPreference notification_control_like;
    CheckBoxPreference notification_control_reply;
    CheckBoxPreference notification_control_at;
    CheckBoxPreference topic_fwd_self; //
    CheckBoxPreference set_id_check; //
    CheckBoxPreference set_left_nav_slide;
    Preference app_version;

    private BroadcastReceiver receiver;

    private  int preferenceScrollPosition = 0;
    private static final String PREF_SCROLL_POSITION = "preference_scroll_position";
    private static boolean hasPerformedInitialRecreate = false;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra("preference_key")) {
                    String key = intent.getStringExtra("preference_key");
                    if (key == null) {
                        Log.e(TAG, "Preference key is null");
                        return;
                    }
                    Preference preference = findPreference(key);
                    if (preference != null) {
                        CleanCache(preference);
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter("com.zfdang.zsmth_android.PREFERENCE_CLICKED");
        if (Build.VERSION.SDK_INT< Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(receiver, filter);
        } else {
            requireContext().registerReceiver(receiver, filter,Context.RECEIVER_NOT_EXPORTED);
        }

        fresco_cache = findPreference("setting_fresco_cache");
        if (fresco_cache == null) {
            Log.e(TAG, "fresco_cache key is null");
            return;
        }
        fresco_cache.setOnPreferenceClickListener(preference -> {
            // clear cache, then update cache size
            ImagePipeline imagePipeline = Fresco.getImagePipeline();
            imagePipeline.clearDiskCaches();
            imagePipeline.clearMemoryCaches();

            updateFrescoCache();
            return true;
        });

        okhttp3_cache = findPreference("setting_okhttp3_cache");
        if (okhttp3_cache == null) {
            Log.e(TAG, "okhttp3_cache key is null");
            return;
        }

        okhttp3_cache.setOnPreferenceClickListener(preference -> {
            // clear cache, then update cache size
            File cache = new File(SMTHApplication.getAppContext().getCacheDir(), "Responses");
            FileLess.$del(cache);
            if (!cache.exists()) {
                cache.mkdir();
            }
            updateOkHttp3Cache();
            return true;
        });


        launch_bottom_navi = (CheckBoxPreference) findPreference("launch_bottom_navi");
        if (launch_bottom_navi == null) {
            Log.e(TAG, "launch_bottom_navi key is null");
            return;
        }

        launch_bottom_navi.setChecked(Settings.getInstance().isLaunchBottomNavi());
        launch_bottom_navi.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean bValue = Settings.getInstance().isLaunchBottomNavi();
            if (newValue instanceof Boolean) {
                bValue = (Boolean) newValue;
            }
            Settings.getInstance().setLaunchBottomNavi(bValue);
            findPreference("menu_text").setEnabled(bValue);

            Activity activity = getActivity();
            if (activity != null) {
                Intent intent = activity.getIntent();

                if (!intent.hasExtra("FRAGMENT")) {
                    intent.putExtra("FRAGMENT", "PREFERENCE");
                }
                activity.recreate();
            }
            return true;
        });

        launch_hottopic_as_entry = (CheckBoxPreference) findPreference("launch_hottopic_as_entry");
        if (launch_hottopic_as_entry == null) {
            Log.e(TAG, "launch_hottopic_as_entry key is null");
            return;
        }
        launch_hottopic_as_entry.setChecked(Settings.getInstance().isLaunchHotTopic());
        launch_hottopic_as_entry.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean bValue = Settings.getInstance().isLaunchHotTopic();
            if (newValue instanceof Boolean) {
                bValue = (Boolean) newValue;
            }
            Settings.getInstance().setLaunchHotTopic(bValue);
            return true;
        });

        open_topic_add = (CheckBoxPreference) findPreference("open_topic_add");
        if (open_topic_add == null) {
            Log.e(TAG, "open_topic_add key is null");
            return;
        }
        open_topic_add.setChecked(Settings.getInstance().isOpenTopicAdd());
        open_topic_add.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean bValue = Settings.getInstance().isOpenTopicAdd();
            if (newValue instanceof Boolean) {
                bValue = (Boolean) newValue;
            }
            Settings.getInstance().setOpenTopicAdd(bValue);
            return true;
        });

        diff_read_topic = (CheckBoxPreference) findPreference("diff_read_topic");
        if (diff_read_topic == null) {
            Log.e(TAG, "diff_read_topic key is null");
            return;
        }
        diff_read_topic.setChecked(Settings.getInstance().isDiffReadTopic());
        diff_read_topic.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean bDiffRead = Settings.getInstance().isDiffReadTopic();
            if (newValue instanceof Boolean) {
                bDiffRead = (Boolean) newValue;
            }
            Settings.getInstance().setDiffReadTopic(bDiffRead);
            SMTHApplication.ReadTopicLists.clear();// 当选择on或者off的时候清空已读列表。
            return true;
        });

        setting_post_navigation_control =(CheckBoxPreference) findPreference("setting_post_navigation_control");
        if (setting_post_navigation_control == null) {
            Log.e(TAG, "setting_post_navigation_control key is null");
            return;
        }
        setting_post_navigation_control.setChecked(Settings.getInstance().hasPostNavBar());
        setting_post_navigation_control.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean bValue = Settings.getInstance().hasPostNavBar();
            if (newValue instanceof Boolean) {
                bValue = (Boolean) newValue;
            }
            Settings.getInstance().setPostNavBar(bValue);
            return true;
        });

        login_with_verification =(CheckBoxPreference) findPreference("setting_login_with_verification");
        if (login_with_verification == null) {
            Log.e(TAG, "login_with_verification key is null");
            return;
        }
        login_with_verification.setChecked(Settings.getInstance().isLoginWithVerification());
        login_with_verification.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean value = Settings.getInstance().isLoginWithVerification();
            if (newValue instanceof Boolean) {
                value = (Boolean) newValue;

            }
            Settings.getInstance().setLoginWithVerification(value);
            return true;
        });

        ssl_verification =(CheckBoxPreference) findPreference("ssl_verification");
        if (ssl_verification == null) {
            Log.e(TAG, "ssl_verification key is null");
            return;
        }
        ssl_verification.setChecked(Settings.getInstance().isSslVerification());
        ssl_verification.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean value = Settings.getInstance().isSslVerification();
            if (newValue instanceof Boolean) {
                value = (Boolean) newValue;

            }
            Settings.getInstance().setSslVerification(value);
            return true;
        });


        auto_load_more =(CheckBoxPreference) findPreference("auto_load_more");
        if (auto_load_more == null) {
            Log.e(TAG, "auto_load_more key is null");
            return;
        }
        auto_load_more.setChecked(Settings.getInstance().isautoloadmore());
        auto_load_more.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean bValue = Settings.getInstance().isautoloadmore();
            if (newValue instanceof Boolean) {
                bValue = (Boolean) newValue;
            }
            Settings.getInstance().Setautoloadmore(bValue);
            return true;
        });

        quick_reply =(CheckBoxPreference) findPreference("quick_reply");
        if (quick_reply == null) {
            Log.e(TAG, "quick_reply key is null");
            return;
        }
        quick_reply.setChecked(Settings.getInstance().isQuickReply());
        quick_reply.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean bValue = Settings.getInstance().isQuickReply();
            if (newValue instanceof Boolean) {
                bValue = (Boolean) newValue;
            }
            Settings.getInstance().SetQuickReply(bValue);
            return true;
        });

        show_signature =(CheckBoxPreference) findPreference("show_signature");
        if (show_signature == null) {
            Log.e(TAG, "show_signature key is null");
            return;
        }
        show_signature.setChecked(Settings.getInstance().isShowSignature());
        show_signature.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean bValue = Settings.getInstance().isShowSignature();
            if (newValue instanceof Boolean) {
                bValue = (Boolean) newValue;
            }
            Settings.getInstance().SetShowSignature(bValue);
            return true;
        });

        menu_text =(CheckBoxPreference) findPreference("menu_text");
        if (menu_text == null) {
            Log.e(TAG, "menu_text key is null");
            return;
        }
        menu_text.setChecked(Settings.getInstance().isMenuTextOn());
        menu_text.setOnPreferenceChangeListener((preference, newValue) -> {

            if (!Settings.getInstance().isLaunchBottomNavi()) {
                return false;
            }
            else {
                boolean bValue = Settings.getInstance().isMenuTextOn();
                if (newValue instanceof Boolean) {
                    bValue = (Boolean) newValue;
                }
                Settings.getInstance().SetMenuText(bValue);

                Activity activity = getActivity();
                if (activity != null) {
                    Intent intent = activity.getIntent();

                    if (!intent.hasExtra("FRAGMENT")) {
                        intent.putExtra("FRAGMENT", "PREFERENCE");
                    }
                    activity.recreate();
                }
                return true;
            }
        });


        setting_volume_key_scroll =(CheckBoxPreference) findPreference("setting_volume_key_scroll");
        if (setting_volume_key_scroll == null) {
            Log.e(TAG, "setting_volume_key_scroll key is null");
            return;
        }
        setting_volume_key_scroll.setChecked(Settings.getInstance().isVolumeKeyScroll());
        setting_volume_key_scroll.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean bValue = Settings.getInstance().isVolumeKeyScroll();
            if (newValue instanceof Boolean) {
                bValue = (Boolean) newValue;
            }
            Settings.getInstance().setVolumeKeyScroll(bValue);
            return true;
        });

        setting_fontsize_control =(ListPreference) findPreference("setting_fontsize_control");
        if (setting_fontsize_control == null) {
            Log.e(TAG, "setting_fontsize_control key is null");
            return;
        }
        setting_fontsize_control.setValueIndex(Settings.getInstance().getFontIndex());
        setting_fontsize_control.setOnPreferenceChangeListener((preference, newValue) -> {
            int fontIndex = Settings.getInstance().getFontIndex();
            if (newValue instanceof String) {
                fontIndex = Integer.parseInt((String) newValue);
            }
            Settings.getInstance().setFontIndex(fontIndex);

            // recreate activity for font size to take effect
            Activity activity = getActivity();
            if (activity != null) {
                Intent intent = activity.getIntent();

                if (!intent.hasExtra("FRAGMENT")) {
                    intent.putExtra("FRAGMENT", "PREFERENCE");
                }
                activity.recreate();
            }
            return true;
        });

        image_quality_control =(CheckBoxPreference) findPreference("setting_image_quality_control");
        if (image_quality_control == null) {
            Log.e(TAG, "image_quality_control key is null");
            return;
        }
        image_quality_control.setChecked(Settings.getInstance().isLoadOriginalImage());
        image_quality_control.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean bLoadOriginalImage = Settings.getInstance().isLoadOriginalImage();
            if (newValue instanceof Boolean) {
                bLoadOriginalImage = (Boolean) newValue;
            }
            Settings.getInstance().setLoadOriginalImage(bLoadOriginalImage);
            return true;
        });

        image_source_cdn =(CheckBoxPreference) findPreference("setting_image_source_cdn");
        if (image_source_cdn == null) {
            Log.e(TAG, "image_source_cdn key is null");
            return;
        }
        image_source_cdn.setChecked(Settings.getInstance().isImageSourceCDN());
        image_source_cdn.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean bImageSourceCDN = Settings.getInstance().isImageSourceCDN();
            if (newValue instanceof Boolean) {
                bImageSourceCDN = (Boolean) newValue;
            }
            Settings.getInstance().setImageSourceCDN(bImageSourceCDN);
            return true;
        });

        daynight_control =(CheckBoxPreference) findPreference("setting_daynight_control");
        if (daynight_control == null) {
            Log.e(TAG, "daynight_control key is null");
            return;
        }
        daynight_control.setChecked(Settings.getInstance().isNightMode());
        daynight_control.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean bNightMode = Settings.getInstance().isNightMode();
            if (newValue instanceof Boolean) {
                bNightMode = (Boolean) newValue;
            }
            Settings.getInstance().setNightMode(bNightMode);

            setApplicationNightMode();
            return true;
        });

        notification_control_mail = (CheckBoxPreference) findPreference("setting_notification_control_mail");
        if (notification_control_mail == null) {
            Log.e(TAG, "notification_control_mail key is null");
            return;
        }
        notification_control_mail.setChecked(Settings.getInstance().isNotificationMail());
        notification_control_mail.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean bValue = Settings.getInstance().isNotificationMail();
            if (newValue instanceof Boolean) {
                bValue = (Boolean) newValue;
            }
            Settings.getInstance().setNotificationMail(bValue);
            return true;
        });

        notification_control_at = (CheckBoxPreference) findPreference("setting_notification_control_at");
        if (notification_control_at == null) {
            Log.e(TAG, "notification_control_at key is null");
            return;
        }
        notification_control_at.setChecked(Settings.getInstance().isNotificationAt());
        notification_control_at.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean bValue = Settings.getInstance().isNotificationAt();
            if (newValue instanceof Boolean) {
                bValue = (Boolean) newValue;
            }
            Settings.getInstance().setNotificationAt(bValue);
            return true;
        });

        notification_control_like = (CheckBoxPreference) findPreference("setting_notification_control_like");
        if (notification_control_like == null) {
            Log.e(TAG, "notification_control_like key is null");
            return;
        }
        notification_control_like.setChecked(Settings.getInstance().isNotificationLike());
        notification_control_like.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean bValue = Settings.getInstance().isNotificationLike();
            if (newValue instanceof Boolean) {
                bValue = (Boolean) newValue;
            }
            Settings.getInstance().setNotificationLike(bValue);
            return true;
        });

        notification_control_reply = (CheckBoxPreference) findPreference("setting_notification_control_reply");
        if (notification_control_reply == null) {
            Log.e(TAG, "notification_control_reply key is null");
            return;
        }
        notification_control_reply.setChecked(Settings.getInstance().isNotificationReply());
        notification_control_reply.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean bValue = Settings.getInstance().isNotificationReply();
            if (newValue instanceof Boolean) {
                bValue = (Boolean) newValue;
            }
            Settings.getInstance().setNotificationReply(bValue);
            return true;
        });

        signature_control = (CheckBoxPreference) findPreference("setting_signature_control");
        if (signature_control == null) {
            Log.e(TAG, "signature_control key is null");
            return;
        }
        signature_control.setChecked(Settings.getInstance().bUseSignature());
        signature_control.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean bValue = Settings.getInstance().bUseSignature();
            if (newValue instanceof Boolean) {
                bValue = (Boolean) newValue;
            }
            Settings.getInstance().setUseSignature(bValue);
            return true;
        });

        signature_content = findPreference("setting_signature_content");
        if (signature_content == null) {
            Log.e(TAG, "signature_content key is null");
            return;
        }
        signature_content.setSummary(Settings.getInstance().getSignature());
        if (signature_content instanceof EditTextPreference) {
            EditTextPreference et = (EditTextPreference) signature_content;
            et.setText(Settings.getInstance().getSignature());
        }
        signature_content.setOnPreferenceChangeListener((preference, newValue) -> {
            String signature = newValue.toString();
            Settings.getInstance().setSignature(signature);
            signature_content.setSummary(signature);
            return true;
        });

        web_content = findPreference("WebAddr");
        if (web_content == null) {
            Log.e(TAG, "web_content key is null");
            return;
        }
        web_content.setSummary(Settings.getInstance().getWebAddr());
        if (web_content instanceof EditTextPreference) {
            EditTextPreference et = (EditTextPreference) web_content;
            et.setText(Settings.getInstance().getWebAddr());
        }
        web_content.setOnPreferenceChangeListener((preference, newValue) -> {
            String webContent = newValue.toString();
            Settings.getInstance().setWebAddr(webContent);
            web_content.setSummary(webContent);

            Activity activity = getActivity();
            Intent intent = new Intent(Objects.requireNonNull(activity).getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            activity.finish();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
            return true;
        });

        topic_fwd_self = (CheckBoxPreference) findPreference("setting_topic_fwd");
        if (topic_fwd_self == null) {
            Log.e(TAG, "topic_fwd_self key is null");
            return;
        }
        topic_fwd_self.setChecked(Settings.getInstance().isTopicFwdSelf());
        topic_fwd_self.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean bValue = Settings.getInstance().isTopicFwdSelf();
            if (newValue instanceof Boolean) {
                bValue = (Boolean) newValue;
            }
            Settings.getInstance().SetTopicFwdSelf(bValue);
            return true;
        });

        set_id_check = (CheckBoxPreference) findPreference("set_id_check");
        if (set_id_check == null) {
            Log.e(TAG, "set_id_check key is null");
            return;
        }
        set_id_check.setChecked(Settings.getInstance().isSetIdCheck());
        set_id_check.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean bValue = Settings.getInstance().isSetIdCheck();
            if (newValue instanceof Boolean) {
                bValue = (Boolean) newValue;
            }
            Settings.getInstance().SetIdCheck(bValue);
            return true;
        });

        set_left_nav_slide = (CheckBoxPreference) findPreference("left_nav_slide");
        set_left_nav_slide.setChecked(Settings.getInstance().isLeftNavSlide());
        set_left_nav_slide.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean bValue = Settings.getInstance().isLeftNavSlide();
            if (newValue instanceof Boolean) {
                bValue = (Boolean) newValue;
            }
            Settings.getInstance().setLeftNavSlide(bValue);
            Activity activity = getActivity();
            if (activity != null) {
                Intent intent = activity.getIntent();

                if (!intent.hasExtra("FRAGMENT")) {
                    intent.putExtra("FRAGMENT", "PREFERENCE");
                }
                activity.recreate();
            }
            return true;
        });

        app_version = findPreference("setting_app_version");
        if (app_version == null) {
            Log.e(TAG, "app_version key is null");
            return;
        }
        app_version.setOnPreferenceClickListener(preference -> {
            //Toast.makeText(SMTHApplication.getAppContext(),"zSMTH-v版本，请用电脑或者电脑模式手机浏览器下载!",Toast.LENGTH_SHORT).show();
            NewToast.makeText(SMTHApplication.getAppContext(),"zSMTH-v版本，请用电脑或者电脑模式手机浏览器下载!",Toast.LENGTH_SHORT);
            ActivityUtils.openLink("https://lanzoui.com/b01noyh6b", getActivity());
            return true;
        });

        updateOkHttp3Cache();
        updateFrescoCache();
        updateVersionInfo();

    }

    @Override public void onCreatePreferences(Bundle bundle, String s) {
        setPreferencesFromResource(R.xml.preferences, s);
    }


    public void setApplicationNightMode() {
        boolean bNightMode = Settings.getInstance().isNightMode();

        if (bNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        Activity activity = getActivity();
        if (activity != null) {
            Intent intent = activity.getIntent();

            if (!intent.hasExtra("FRAGMENT")) {
                intent.putExtra("FRAGMENT", "PREFERENCE");
            }
            activity.recreate();
        }

    }

    public void updateVersionInfo() {
        Context context = SMTHApplication.getAppContext();
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = pInfo.versionName;
            int verCode = pInfo.versionCode;
            String summary = String.format(Locale.CHINA,"版本号: %s(%d)", version, verCode);
            app_version.setSummary(summary);
        } catch (Exception e) {
            Log.e(TAG, "updateVersionInfo: " + Log.getStackTraceString(e));
        }
    }

    public void updateOkHttp3Cache() {
        File httpCacheDirectory = new File(SMTHApplication.getAppContext().getCacheDir(), "Responses");
        updateCacheSize(httpCacheDirectory.getAbsolutePath(), okhttp3_cache);
    }

    public void updateFrescoCache() {
        File frescoCacheDirectory = new File(SMTHApplication.getAppContext().getCacheDir(), "fresco_cache");
        // Log.d(TAG, "updateFrescoCache: " + frescoCacheDirectory.getAbsolutePath());
        updateCacheSize(frescoCacheDirectory.getAbsolutePath(), fresco_cache);
    }

    public void updateCacheSize(final String folder, final Preference pref) {
        Observable.just(folder).map(FileSizeUtil::getAutoFileOrFolderSize).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<String>() {
            @Override public void onSubscribe(@NonNull Disposable disposable) {

            }

            @Override public void onNext(@NonNull String s) {
                Log.d(TAG, "onNext: Folder size = " + s);
                pref.setSummary("缓存大小:" + s);
            }

            @Override public void onError(@NonNull Throwable e) {
                //Toast.makeText(SMTHApplication.getAppContext(), "获取缓存大小失败!\n" + e.toString(), Toast.LENGTH_SHORT).show();
                NewToast.makeText(SMTHApplication.getAppContext(), "获取缓存大小失败!\n" + e.toString(), Toast.LENGTH_SHORT);
            }

            @Override public void onComplete() {
            }
        });
    }

    private void CleanCache(Preference preference) {

        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        imagePipeline.clearDiskCaches();
        updateFrescoCache();

        File cache = new File(SMTHApplication.getAppContext().getCacheDir(), "Responses");
        FileLess.$del(cache);
        if (!cache.exists()) {
            cache.mkdir();
            updateOkHttp3Cache();
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            requireContext().unregisterReceiver(receiver);
        }
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
    }

    @Override
    public void onViewCreated(@androidx.annotation.NonNull @NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentStatusBarUtil.adaptDarkMode(this, false);

        view.setVisibility(View.GONE);

        if(SMTHApplication.isValidUser()) {
            if (!hasPerformedInitialRecreate) {
                hasPerformedInitialRecreate = true;

                // 延迟执行 recreate，避免卡顿
                view.postDelayed(() -> {
                    if (getActivity() != null && getView() != null) {
                        Activity activity = getActivity();
                        Intent intent = activity.getIntent();
                        if (!intent.hasExtra("FRAGMENT")) {
                            intent.putExtra("FRAGMENT", "PREFERENCE");
                            activity.recreate();
                        }
                    }
                }, 200);
            } else {
                view.setVisibility(View.VISIBLE);
            }
        }else{
            view.setVisibility(View.VISIBLE);
        }

        view.post(() -> {
            if (getListView() != null) {
                getListView().invalidate();
            }
        });

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@androidx.annotation.NonNull @NonNull Menu menu, @androidx.annotation.NonNull @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.preference_menu, menu); // Replace with your actual menu resource
                // Dynamically modify menu items
                MenuItem newItem = menu.findItem(R.id.main_action_refresh);
                newItem.setVisible(false);
            }


            @Override
            public boolean onMenuItemSelected(@androidx.annotation.NonNull @NonNull MenuItem menuItem) {
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onPause() {
        super.onPause();

        View rootView = getView();
        if (rootView != null) {
            // 使用通用方法查找RecyclerView
            RecyclerView recyclerView = findRecyclerViewInHierarchy(rootView);
            if (recyclerView != null && recyclerView.getLayoutManager() != null) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int position = layoutManager.findFirstVisibleItemPosition();
                //int position = findClosestToCenter(recyclerView);
                preferenceScrollPosition = position;
                saveScrollPosition(position);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // 保留原有的滚动位置恢复逻辑
        preferenceScrollPosition = loadScrollPosition();
        View rootView = getView();
        if (rootView != null) {
            // 使用通用方法查找RecyclerView
            RecyclerView recyclerView = findRecyclerViewInHierarchy(rootView);
            if (recyclerView != null && recyclerView.getLayoutManager() != null && preferenceScrollPosition >= 0) {
                final LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

                // Post to message queue to ensure RecyclerView is fully laid out
                recyclerView.post(() -> {
                    if (preferenceScrollPosition != RecyclerView.NO_POSITION) {
                        layoutManager.scrollToPositionWithOffset(preferenceScrollPosition, 0);
                    }
                });
            }
        }
    }

    // 添加辅助方法查找RecyclerView
    private RecyclerView findRecyclerViewInHierarchy(View view) {
        // 首先检查view本身是否就是RecyclerView
        if (view instanceof RecyclerView) {
            return (RecyclerView) view;
        }

        // 如果是ViewGroup，递归查找子视图
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                RecyclerView result = findRecyclerViewInHierarchy(viewGroup.getChildAt(i));
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }



    private int findClosestToCenter(RecyclerView recyclerView) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null) return 0;

        int firstPos = layoutManager.findFirstVisibleItemPosition();
        int lastPos = layoutManager.findLastVisibleItemPosition();

        if (firstPos == RecyclerView.NO_POSITION || firstPos == lastPos) {
            return firstPos;
        }

        int closestPos = firstPos;
        int centerY = recyclerView.getHeight() / 2;

        for (int i = firstPos; i <= lastPos; i++) {
            View child = layoutManager.findViewByPosition(i);
            if (child == null) continue;

            int childMidY = (child.getTop() + child.getBottom()) / 2;
            int currentDelta = Math.abs(childMidY - centerY);
            int bestDelta = (layoutManager.findViewByPosition(closestPos) == null) ? Integer.MAX_VALUE :
                    Math.abs(((Objects.requireNonNull(layoutManager.findViewByPosition(closestPos)).getTop() +
                            Objects.requireNonNull(layoutManager.findViewByPosition(closestPos)).getBottom()) / 2 - centerY));

            if (currentDelta < bestDelta) {
                closestPos = i;
            }
        }

        return closestPos;
    }

    private void saveScrollPosition(int position) {
        requireContext().getSharedPreferences("PreferenceState", Context.MODE_PRIVATE)
                .edit()
                .putInt(PREF_SCROLL_POSITION, position)
                .apply();
    }

    private int loadScrollPosition() {
        return requireContext().getSharedPreferences("PreferenceState", Context.MODE_PRIVATE)
                .getInt(PREF_SCROLL_POSITION, 0);
    }

}