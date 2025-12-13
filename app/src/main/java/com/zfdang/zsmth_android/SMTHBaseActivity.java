package com.zfdang.zsmth_android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by zfdang on 2016 - 5 - 10.
 * Modified by Vinney on 2025 - 11 - 30.
 */
public class SMTHBaseActivity extends AppCompatActivity {
    protected AlertDialog pDialog = null;
    private SettingsContentObserver settingsContentObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configure cutout display mode
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        applyOrientationSetting();
        // Register content observer to monitor rotation setting changes
        settingsContentObserver = new SettingsContentObserver(new Handler());
        getContentResolver().registerContentObserver(
                android.provider.Settings.System.getUriFor(
                        android.provider.Settings.System.ACCELEROMETER_ROTATION),
                false,
                settingsContentObserver
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyOrientationSetting();
    }

    private void applyOrientationSetting() {
        if (isAutoRotationEnabled()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private boolean isAutoRotationEnabled() {
        try {
            int rotationEnabled = android.provider.Settings.System.getInt(
                    getContentResolver(),
                    android.provider.Settings.System.ACCELEROMETER_ROTATION,
                    0
            );
            return rotationEnabled == 1;
        } catch (Exception e) {
            return false;
        }
    }

    public void showProgress(String message) {
        if (pDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);

            LayoutInflater inflater = this.getLayoutInflater();
            View view = inflater.inflate(R.layout.progress_dialog_layout, null);
            TextView messageTextView = view.findViewById(R.id.dialog_message);
            messageTextView.setText(message);
            builder.setView(view);
            pDialog = builder.create();

            if (pDialog.getWindow() != null) {
                pDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

        } else {
            TextView messageTextView = pDialog.findViewById(R.id.dialog_message);
            if (messageTextView != null) {
                messageTextView.setText(message);
            }
        }
        pDialog.show();
    }

    public void dismissProgress() {
        if (pDialog != null && pDialog.isShowing()) {
            pDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissProgress();
        // Unregister content observer
        if (settingsContentObserver != null) {
            getContentResolver().unregisterContentObserver(settingsContentObserver);
        }
    }

    // ContentObserver to monitor system rotation setting changes
    private class SettingsContentObserver extends ContentObserver {
        public SettingsContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            // Apply new orientation setting when system setting changes
            applyOrientationSetting();
        }
    }

    @Override
    public Resources getResources() {
        Resources res = super.getResources();
        Configuration newConfig = new Configuration(res.getConfiguration());
        newConfig.fontScale = Settings.getInstance().getFontSizeFloatValue();

        try {
            Context context = createConfigurationContext(newConfig);
            res = context.getResources();
        } catch (Exception e) {
            Log.e("SMTHBaseActivity", "Error updating configuration: " + e.getMessage());
        }

        return res;
    }

}