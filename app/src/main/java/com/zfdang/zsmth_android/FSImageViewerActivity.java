package com.zfdang.zsmth_android;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.github.chrisbanes.photoview.OnOutsidePhotoTapListener;
import com.github.chrisbanes.photoview.OnPhotoTapListener;
import com.jude.swipbackhelper.SwipeBackHelper;
import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.fresco.FrescoUtils;
import com.zfdang.zsmth_android.fresco.MyPhotoView;
import com.zfdang.zsmth_android.helpers.FileSizeUtil;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.util.ArrayList;
import me.relex.circleindicator.CircleIndicator;


public class FSImageViewerActivity extends AppCompatActivity implements OnPhotoTapListener, OnOutsidePhotoTapListener {

  private static final String TAG = "FullViewer";

  private static final int MY_PERMISSIONS_REQUEST_STORAGE_CODE = 299;
  private HackyViewPager mViewPager;

  private FSImagePagerAdapter mPagerAdapter;
  private ArrayList<String> mURLs;

  private LinearLayout layoutToolbar;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // 延伸显示区域到刘海
    Window window = this.getWindow();
    WindowManager.LayoutParams lp = this.getWindow().getAttributes();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
    }
    window.setAttributes(lp);

    setContentView(R.layout.activity_fs_image_viewer);

    mViewPager = findViewById(R.id.fullscreen_image_pager);

    // find parameters from parent
    mURLs = getIntent().getStringArrayListExtra(SMTHApplication.ATTACHMENT_URLS);

    if (mURLs == null) {
      Log.e(TAG, "mURLs is null.");
      return;
    }
    int pos = getIntent().getIntExtra(SMTHApplication.ATTACHMENT_CURRENT_POS, 0);
    if (pos < 0 || pos >= mURLs.size()) {
      pos = 0;
    }

    mPagerAdapter = new FSImagePagerAdapter(mURLs, this);
    mViewPager.setAdapter(mPagerAdapter);
    mViewPager.setCurrentItem(pos);

    CircleIndicator mIndicator = findViewById(R.id.fullscreen_image_indicator);
    mIndicator.setViewPager(mViewPager);

    // initialize toolbar and its child buttons
    layoutToolbar = findViewById(R.id.fullscreen_toolbar);

    ImageView btBack = findViewById(R.id.fullscreen_button_back);
    btBack.setOnClickListener(v -> finish());

    ImageView btInfo = findViewById(R.id.fullscreen_button_info);
    btInfo.setOnClickListener(v -> {
      int position = mViewPager.getCurrentItem();
      final String imagePath = mURLs.get(position);

      showExifDialog(imagePath);
    });

    ImageView btSave = findViewById(R.id.fullscreen_button_save);
    btSave.setOnClickListener(v -> {

      if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
        if (ContextCompat.checkSelfPermission(FSImageViewerActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
          ActivityCompat.requestPermissions(FSImageViewerActivity.this,
                  new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                  MY_PERMISSIONS_REQUEST_STORAGE_CODE);
        }
        else
        {
          realSaveImageToFile();
        }
      }
      else{
        if(Environment.isExternalStorageManager())
        {
          realSaveImageToFile();
        }
        else
        {
          Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
          startActivity(intent);
        }
      }
    });

    hideSystemUI();

    SwipeBackHelper.onCreate(this);
    SwipeBackHelper.getCurrentPage(this).setSwipeEdgePercent(0.2f);
  }

  public void realSaveImageToFile()
  {
    int position = mViewPager.getCurrentItem();
    final String imagePath = mURLs.get(position);

    View currentView = mPagerAdapter.mCurrentView;
    boolean isAnimation = false;
    if (currentView instanceof MyPhotoView) {
      MyPhotoView photoView = (MyPhotoView) currentView;
      isAnimation = photoView.isAnimation();
    }

    saveImageToFile(imagePath, isAnimation);
  }
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode,permissions,grantResults);
      if (requestCode == MY_PERMISSIONS_REQUEST_STORAGE_CODE) {// If request is cancelled, the result arrays are empty.
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {

              if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                  // permission was granted, yay! Do the
                  // contacts-related task you need to do.
                  realSaveImageToFile();

              } else {
                  //在版本低于此的时候，做一些处理

                  // permission denied, boo! Disable the
                  // functionality that depends on this permission.
                  Toast.makeText(FSImageViewerActivity.this, getString(com.zfdang.multiple_images_selector.R.string.selector_permission_error), Toast.LENGTH_SHORT).show();
              }
          } else {
              if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                      && grantResults[1] == PackageManager.PERMISSION_GRANTED
                      || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                  // permission was granted, yay! Do the
                  // contacts-related task you need to do.
                  realSaveImageToFile();
              } else {
                  // permission denied, boo! Disable the
                  // functionality that depends on this permission.
                  Toast.makeText(FSImageViewerActivity.this, getString(com.zfdang.multiple_images_selector.R.string.selector_permission_error), Toast.LENGTH_SHORT).show();
              }
          }
      }
  }
  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    if (hasFocus) {
      hideSystemUI();
      layoutToolbar.setVisibility(LinearLayout.GONE);
    }
  }

  private void hideSystemUI() {
    // Enables regular immersive mode.
    // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
    // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    View decorView = getWindow().getDecorView();
    decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE
                    // Hide the nav bar and status bar
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    // Set the content to appear under the system bars so that the
                    // content doesn't resize when the system bars hide and show.
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    );
  }

  // Shows the system bars by removing all the flags
  // except for the ones that make the content appear under the system bars.
  private void showSystemUI() {
    View decorView = getWindow().getDecorView();
    decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
  }


  @Override protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    SwipeBackHelper.onPostCreate(this);
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    SwipeBackHelper.onDestroy(this);
  }

  @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
      mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1, true);
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
      mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1, true);
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  // http://stackoverflow.com/questions/4500354/control-volume-keys
  @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
    // disable the beep sound when volume up/down is pressed
    if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
      return true;
    }
    return super.onKeyUp(keyCode, event);
  }

  private String getURLHashCode(String imagePath) {
    return Integer.toHexString(imagePath.hashCode());
  }

  public void saveImageToFile(String imagePath, boolean isAnimation) {
    File imageFile = FrescoUtils.getCachedImageOnDisk(Uri.parse(imagePath));
    if (imageFile == null) {
      Toast.makeText(FSImageViewerActivity.this, "无法读取缓存文件！", Toast.LENGTH_SHORT).show();
      return;
    }
    // Log.d(TAG, "saveImageToFile: " + imageFile.getAbsolutePath());

    // save image to sdcard
    try {
      if (TextUtils.equals(Environment.getExternalStorageState(), Environment.MEDIA_MOUNTED)) {
        String path = Environment.getExternalStorageDirectory().getPath() + "/zSMTH/";
        File dir = new File(path);
        if (!dir.exists()) {
          dir.mkdirs();
        }

        String IMAGE_FILE_PREFIX = "zSMTH-";
        String IMAGE_FILE_SUFFIX = ".jpg";
        if (isAnimation) {
          IMAGE_FILE_SUFFIX = ".gif";
        }
        File outFile = new File(dir, IMAGE_FILE_PREFIX + getURLHashCode(imagePath) + IMAGE_FILE_SUFFIX);

        BufferedInputStream bufr = new BufferedInputStream(Files.newInputStream(imageFile.toPath()));
        BufferedOutputStream bufw = new BufferedOutputStream(Files.newOutputStream(outFile.toPath()));


        int len;
        byte[] buf = new byte[1024];
        while ((len = bufr.read(buf)) != -1) {
          bufw.write(buf, 0, len);
          bufw.flush();
        }
        bufw.close();
        bufr.close();

        // make sure the new file can be recognized soon
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(outFile)));

        Toast.makeText(FSImageViewerActivity.this, "图片已存为: /zSMTH/" + outFile.getName(), Toast.LENGTH_SHORT).show();
      }
    } catch (Exception e) {
      Log.e(TAG, "saveImageToFile: " + Log.getStackTraceString(e));
      Toast.makeText(FSImageViewerActivity.this, "保存图片失败:\n请授予应用存储权限！\n" + e, Toast.LENGTH_SHORT).show();
    }
  }

  // get image attribute from exif
  private void setImageAttributeFromExif(View layout, int tv_id, ExifInterface exif, String attr) {
    if (layout == null || exif == null) return;
    TextView tv = layout.findViewById(tv_id);
    if (tv == null) {
      Log.d(TAG, "setImageAttributeFromExif: " + "Invalid resource ID: " + tv_id);
      return;
    }

    String attribute = exif.getAttribute(attr);
    if (attribute != null) {
      // there are some special treatment
      // http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/EXIF.html
      switch (attr) {
        case ExifInterface.TAG_F_NUMBER:
          attribute = "F/" + attribute;
          break;
        case ExifInterface.TAG_EXPOSURE_TIME:
          try {
            float f = Float.parseFloat(attribute);
            if (f >= 1.0) {
              attribute = attribute + " s";
            } else if (f >= 0.1) {
              f = 1 / f;
              BigDecimal exposure = new BigDecimal(f).setScale(0, RoundingMode.HALF_UP);
              attribute = "1/" + exposure.toString() + " s";
            } else {
              f = 1 / f / 10;
              BigDecimal exposure = new BigDecimal(f).setScale(0, RoundingMode.HALF_UP);
              exposure = exposure.multiply(new BigDecimal(10));
              attribute = "1/" + exposure.toString() + " s";
            }
          } catch (NumberFormatException e) {
            Log.d("Can't convert exposure:", attribute);
          }
          break;
        case ExifInterface.TAG_FLASH:
          int flash = Integer.parseInt(attribute);
          switch (flash) {
            case 0x0:
              attribute += " (No Flash)";
              break;
            case 0x1:
              attribute += " (Fired)";
              break;
            case 0x5:
              attribute += " (Fired, Return not detected)";
              break;
            case 0x7:
              attribute += " (Fired, Return detected)";
              break;
            case 0x8:
              attribute += " (On, Did not fire)";
              break;
            case 0x9:
              attribute += " (On, Fired)";
              break;
            case 0xd:
              attribute += " (On, Return not detected)";
              break;
            case 0xf:
              attribute += " (On, Return detected)";
              break;
            case 0x10:
              attribute += " (Off, Did not fire)";
              break;
            case 0x14:
              attribute += " (Off, Did not fire, Return not detected)";
              break;
            case 0x18:
              attribute += " (Auto, Did not fire)";
              break;
            case 0x19:
              attribute += " (Auto, Fired)";
              break;
            case 0x1d:
              attribute += " (Auto, Fired, Return not detected)";
              break;
            case 0x1f:
              attribute += " (Auto, Fired, Return detected)";
              break;
            case 0x20:
              attribute += " (No flash function)";
              break;
            case 0x30:
              attribute += " (Off, No flash function)";
              break;
            case 0x41:
              attribute += " (Fired, Red-eye reduction)";
              break;
            case 0x45:
              attribute += " (Fired, Red-eye reduction, Return not detected)";
              break;
            case 0x47:
              attribute += " (Fired, Red-eye reduction, Return detected)";
              break;
            case 0x49:
              attribute += " (On, Red-eye reduction)";
              break;
            case 0x4d:
              attribute += " (On, Red-eye reduction, Return not detected)";
              break;
            case 0x4f:
              attribute += " (On, Red-eye reduction, Return detected)";
              break;
            case 0x50:
              attribute += " (Off, Red-eye reduction)";
              break;
            case 0x58:
              attribute += " (Auto, Did not fire, Red-eye reduction)";
              break;
            case 0x59:
              attribute += " (Auto, Fired, Red-eye reduction)";
              break;
            case 0x5d:
              attribute += " (Auto, Fired, Red-eye reduction, Return not detected)";
              break;
            case 0x5f:
              attribute += " (Auto, Fired, Red-eye reduction, Return detected)";
              break;
            default:
              break;
          }
          break;
        case ExifInterface.TAG_WHITE_BALANCE:
          int wb = Integer.parseInt(attribute);
          switch (wb) {
            case 0:
              attribute += " (Auto)";
              break;
            case 1:
              attribute += " (Manual)";
              break;
          }
          break;
      }
      tv.setText(attribute);
    }
  }

  public void showExifDialog(String imagePath) {
    File imageFile = FrescoUtils.getCachedImageOnDisk(Uri.parse(imagePath));
    if (imageFile == null) {
      Toast.makeText(FSImageViewerActivity.this, "无法读取缓存文件！", Toast.LENGTH_SHORT).show();
      return;
    }

    // show exif information dialog
    LayoutInflater inflater = getLayoutInflater();
    View layout = inflater.inflate(R.layout.image_exif_info, null);
    try {
      String sFileName = imageFile.getAbsolutePath();

      ExifInterface exif = new ExifInterface(sFileName);
      // basic information
      TextView tvFilename = layout.findViewById(R.id.ii_filename);
      tvFilename.setText(imagePath);
      setImageAttributeFromExif(layout, R.id.ii_datetime, exif, ExifInterface.TAG_DATETIME);
      setImageAttributeFromExif(layout, R.id.ii_width, exif, ExifInterface.TAG_IMAGE_WIDTH);
      setImageAttributeFromExif(layout, R.id.ii_height, exif, ExifInterface.TAG_IMAGE_LENGTH);

      // get filesize
      try {
        long filesize = FileSizeUtil.getFileSize(imageFile);
        TextView tv = layout.findViewById(R.id.ii_size);
        if (tv != null) {
          tv.setText(FileSizeUtil.FormatFileSize(filesize));
        }
      } catch (Exception e) {
        Log.e(TAG, "showExifDialog: " + Log.getStackTraceString(e));
      }

      // capture information
      setImageAttributeFromExif(layout, R.id.ii_make, exif, ExifInterface.TAG_MAKE);
      setImageAttributeFromExif(layout, R.id.ii_model, exif, ExifInterface.TAG_MODEL);
      setImageAttributeFromExif(layout, R.id.ii_focal_length, exif, ExifInterface.TAG_FOCAL_LENGTH);
      setImageAttributeFromExif(layout, R.id.ii_aperture, exif, ExifInterface.TAG_F_NUMBER);
      setImageAttributeFromExif(layout, R.id.ii_exposure_time, exif, ExifInterface.TAG_EXPOSURE_TIME);
      setImageAttributeFromExif(layout, R.id.ii_flash, exif, ExifInterface.TAG_FLASH);
      setImageAttributeFromExif(layout, R.id.ii_iso, exif, ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY);
      setImageAttributeFromExif(layout, R.id.ii_white_balance, exif, ExifInterface.TAG_WHITE_BALANCE);
    } catch (IOException e) {
      Log.d("read ExifInfo", "can't read Exif information");
    }

    new AlertDialog.Builder(FSImageViewerActivity.this).setView(layout).setOnDismissListener(dialog -> {
    }).show();
  }

  private void toggleToobarVisibility() {
    int visibility = layoutToolbar.getVisibility();
    if (visibility == LinearLayout.GONE) {
      showSystemUI();
      layoutToolbar.setVisibility(LinearLayout.VISIBLE);
    } else {
      hideSystemUI();
      layoutToolbar.setVisibility(LinearLayout.GONE);
    }
  }

  @Override
  public void onOutsidePhotoTap(ImageView imageView) {
    toggleToobarVisibility();
  }

  @Override
  public void onPhotoTap(ImageView view, float x, float y) {
    toggleToobarVisibility();
  }
}
