package com.zfdang.zsmth_android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by zfdang on 2016 - 5 - 10.
 */
public class SMTHBaseActivity extends AppCompatActivity {
    protected AlertDialog pDialog = null;
    
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