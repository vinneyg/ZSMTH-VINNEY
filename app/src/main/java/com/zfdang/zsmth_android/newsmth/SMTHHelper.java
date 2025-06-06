package com.zfdang.zsmth_android.newsmth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.Settings;
import com.zfdang.zsmth_android.WebviewCookieHandler;
import com.zfdang.zsmth_android.helpers.MakeList;
import com.zfdang.zsmth_android.helpers.StringUtils;
import com.zfdang.zsmth_android.utils.TimeUtils;
import com.zfdang.zsmth_android.models.Board;
import com.zfdang.zsmth_android.models.BoardListContent;
import com.zfdang.zsmth_android.models.BoardSection;
import com.zfdang.zsmth_android.models.Mail;
import com.zfdang.zsmth_android.models.MailListContent;
import com.zfdang.zsmth_android.models.Post;
import com.zfdang.zsmth_android.models.Topic;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
//import java.security.cert.CertificateException;
//import java.security.cert.CertificateExpiredException;
//import java.security.cert.CertificateNotYetValidException;
import java.util.ArrayList;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * Created by zfdang on 2016-3-16.
 */
public class SMTHHelper {

  static final private String TAG = "SMTHHelper";

  public static final String USER_AGENT =
          "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.101 Safari/537.36";

  public OkHttpClient mHttpClient;

  //https://att.mysmth.net OR https://static.mysmth.net"
  static private final String SMTH_IMAGE_PREFIX_CDN = "https://att.newsmth.net";
  //static private final String SMTH_IMAGE_PREFIX_CDN = "https://static.newsmth.net";
  static private final String SMTH_IMAGE_PREFIX_DIRECT = "https://www.newsmth.net";
  // Mobile service of SMTH; this is used for webchat sharing & open in browser
  static public final String SMTH_MOBILE_URL = "https://m.newsmth.net";

  public SMTHWWWService wService;
  public SMTHMService mService;
  public static SMTHMService pService;
  static private final String SMTH_WWW_ENCODING = "GB2312";

  // All boards cache file
  public static int BOARD_TYPE_FAVORITE = 1;
  public static int BOARD_TYPE_ALL = 2;
  static private final String ALL_BOARD_CACHE_FILE = "SMTH_ALLBD_CACHE_KRYO";
  static private final String FAVORITE_BOARD_CACHE_PREFIX = "SMTH_FAVBD_CACHE_KYRO";

  // singleton
  private static SMTHHelper instance = null;

  public static synchronized SMTHHelper getInstance() {
    if (instance == null) {
      try {
        instance = new SMTHHelper();
      } catch (NoSuchAlgorithmException | KeyManagementException e) {
        throw new RuntimeException(e);
      }
    }
    return instance;
  }

  public static synchronized void resetInstance() {
    instance = null;
    try {
      instance = new SMTHHelper();
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      throw new RuntimeException(e);
    }
  }


  // response from WWW is GB2312, we need to conver it to UTF-8
  // http://www.newsmth.net/mainpage.html
  public static String DecodeResponseFromWWW(byte[] bytes) {
    String result = null;
    try {
      //result = new String(bytes, SMTH_WWW_ENCODING);
      String temp = new String(bytes, SMTH_WWW_ENCODING); // GB2312
      if (temp.contains("utf-8") || temp.contains("UTF-8")) {
        return new String(bytes, StandardCharsets.UTF_8);
      }
      return temp;
    } catch (UnsupportedEncodingException e) {
      Log.d("DecodeResponseFromWWW", e.toString());
      return "";
    }
  }

  // protected constructor, can only be called by getInstance
  protected SMTHHelper() throws NoSuchAlgorithmException, KeyManagementException {

    // set your desired log level
    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    logging.setLevel(HttpLoggingInterceptor.Level.NONE);

    //设置缓存路径
    File httpCacheDirectory = new File(SMTHApplication.getAppContext().getCacheDir(), "Responses");
    int cacheSize = 250 * 1024 * 1024; // 250 MiB
    Cache cache = new Cache(httpCacheDirectory, cacheSize);

    OkHttpClient.Builder clientBuilder  = new OkHttpClient().newBuilder().addInterceptor(logging).addInterceptor(new Interceptor() {
              @NonNull
              @Override public Response intercept(@NonNull Chain chain) throws IOException {
                Request request = chain.request().newBuilder().header("User-Agent", USER_AGENT).build();
                return chain.proceed(request);
              }
            })
            /*
            .addInterceptor(new RequestInterceptor())
            .addInterceptor(new ResponseInterceptor())
            */
            .addNetworkInterceptor(new Interceptor() {
              // for error response, do not cache its content
              @NonNull
              @Override public Response intercept(@NonNull Chain chain) throws IOException {
                Response originalResponse = chain.proceed(chain.request());
//        if (originalResponse.isSuccessful() && originalResponse.body().toString().contains("您未登录,请登录后继续操作")) {
                if (originalResponse.isSuccessful() && Objects.requireNonNull(originalResponse.body()).contentLength() > 4096) {
                  // only cache large response
                  // the size of response with "您未登录,请登录后继续操作" is 2033
                  return originalResponse;
                } else {
                  return originalResponse.newBuilder().header("Cache-Control", "no-cache").build();
                }
              }
            }).cookieJar(new WebviewCookieHandler())  // https://gist.github.com/scitbiz/8cb6d8484bb20e47d241cc8e117fa705
            //.sslSocketFactory(OkHttpUtil.getIgnoreInitedSslContext().getSocketFactory(), OkHttpUtil.IGNORE_SSL_TRUST_MANAGER_X509)
            //.hostnameVerifier(OkHttpUtil.getIgnoreSslHostnameVerifier())
            .cache(cache).readTimeout(15, TimeUnit.SECONDS).connectTimeout(10, TimeUnit.SECONDS).build().newBuilder();

    if (!Settings.getInstance().isSslVerification()) {
      SSLContext sslContext = OkHttpUtil.getIgnoreInitedSslContext();
      X509TrustManager trustManager = OkHttpUtil.IGNORE_SSL_TRUST_MANAGER_X509;
      HostnameVerifier hostnameVerifier = OkHttpUtil.getIgnoreSslHostnameVerifier();
      clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), trustManager)
              .hostnameVerifier(hostnameVerifier);
    }

    mHttpClient = clientBuilder.build();

    //        mRetrofit = new Retrofit.Builder()
    //                .baseUrl(SMTH_MOBILE_URL)
    //                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
    //                .addConverterFactory(ScalarsConverterFactory.create())
    //                .client(mHttpClient)
    //                .build();

    // WWW service of SMTH, but actually most of services are actually from nForum
    //private final String SMTH_WWW_URL = "https://www.newsmth.net";
    //private final String SMTH_WWW_URL = "https://www.mysmth.net";
    String SMTH_WWW_URL = SMTHApplication.getWebAddress();
    Retrofit wRetrofit = new Retrofit.Builder().baseUrl(SMTH_WWW_URL)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .client(mHttpClient)
            .build();
    wService = wRetrofit.create(SMTHWWWService.class);
    // Mobile service of SMTH, baseUrl is https://m.newsmth.net
    Retrofit mRetrofit = new Retrofit.Builder().baseUrl(SMTH_MOBILE_URL)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .client(mHttpClient)
            .build();
    mService = mRetrofit.create(SMTHMService.class);
  }

  // query active user status
  // since wService.queryActiveUserStatus does not return correct faceurl, try to query user information again
  public static Observable<UserStatus> queryActiveUserStatus() {
    final SMTHHelper helper = SMTHHelper.getInstance();
    return helper.wService.queryActiveUserStatus()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .map(userStatus -> {
              String userid = userStatus.getId();
              if (userid != null && !userid.equals("guest")) {
                // get correct faceURL
                UserInfo user = helper.wService.queryUserInformation(userid).blockingFirst();
                if (user != null) {
                  userStatus.setFace_url(user.getFace_url());
                }
              }
              return userStatus;
            });
  }

  private static Bitmap loadResizedBitmapFromFile(final String filename, final int targetWidth, final int targetHeight, boolean bCompress) {
    try {
      Bitmap bitmap;

      // o.inPurgeable = true;
      bitmap = BitmapFactory.decodeFile(filename, null);
      Log.d(TAG, "loadResizedBitmapFromFile: " + String.format(Locale.CHINA,"Pre-sized bitmap size: (%dx%d).", bitmap.getWidth(), bitmap.getHeight()));

      if (bCompress) {
        // create bitmap which matches exactly within the target size
        // calc exact destination size
        // http://developer.android.com/reference/android/graphics/Matrix.ScaleToFit.html
        Matrix m = new Matrix();
        RectF inRect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF outRect = new RectF(0, 0, targetWidth, targetHeight);
        m.setRectToRect(inRect, outRect, Matrix.ScaleToFit.CENTER);
        float[] values = new float[9];
        m.getValues(values);

        Log.d(TAG, "loadResizedBitmapFromFile: " + String.format(Locale.CHINA,"Zoom: (%fx%f).", values[0], values[4]));
        if (values[0] < 1.0 || values[4] < 1.0) {
          bitmap = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * values[0]), (int) (bitmap.getHeight() * values[4]), true);
          Log.d(TAG, "loadResizedBitmapFromFile: " + "reduce size");
        }
      }

      Log.d(TAG, "loadResizedBitmapFromFile: " + String.format(Locale.CHINA,"Final bitmap size: (%dx%d).", bitmap.getWidth(), bitmap.getHeight()));
      return bitmap;
    } catch (final OutOfMemoryError e) {
      return null;
    }
  }

  /**
   * Returns the contents of the file in a byte array
   *
   * @param file File this method should read
   * @return byte[] Returns a byte[] array of the contents of the file
   */
  private static byte[] getBytesFromFile(File file) {
    byte[] bytes = null;
    try {
      InputStream is = Files.newInputStream(file.toPath());

      // Get the size of the file
      long length = file.length();
      if (length > Integer.MAX_VALUE) {
        Log.e(TAG, "getBytesFromFile: " + "File is too large to process");
        return null;
      }

      // Create the byte array to hold the data
      bytes = new byte[(int) length];

      // Read in the bytes
      int offset = 0;
      int numRead;
      while ((offset < bytes.length) && ((numRead = is.read(bytes, offset, bytes.length - offset)) >= 0)) {
        offset += numRead;
      }

      // Ensure all the bytes have been read in
      if (offset < bytes.length) {
        throw new IOException("Could not completely read file " + file.getName());
      }
      is.close();
    } catch (IOException e) {
      Log.e(TAG, "getBytesFromFile: " + Log.getStackTraceString(e));
    }

    return bytes;
  }

  public static String saveBitmapToFile(final Bitmap bitmap, final String filename) {
    try {
      if (TextUtils.equals(Environment.getExternalStorageState(), Environment.MEDIA_MOUNTED)) {
        String path = Environment.getExternalStorageDirectory().getPath() + "/zSMTH/uploaded";
        File dir = new File(path);
        if (!dir.exists()) {
          dir.mkdirs();
        }

        File outFile = new File(dir, new File(filename).getName());

        FileOutputStream fstream = new FileOutputStream(outFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fstream);
        fstream.flush();
        fstream.close();

        Log.d(TAG, "saveBitmapToFile: " + outFile.getAbsolutePath());
        return outFile.getAbsolutePath();
      }
    } catch (Exception e) {
      Log.e(TAG, "saveBitmapToFile: ", e);
    }
    return null;
  }

  public static void copyExif(String oldPath, String newPath) {
    try {
      ExifInterface oldExif = new ExifInterface(oldPath);
      String[] attributes = new String[] {
              ExifInterface.TAG_F_NUMBER, ExifInterface.TAG_DATETIME, ExifInterface.TAG_DATETIME_DIGITIZED, ExifInterface.TAG_EXPOSURE_TIME,
              ExifInterface.TAG_FLASH, ExifInterface.TAG_FOCAL_LENGTH, ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_GPS_ALTITUDE_REF,
              ExifInterface.TAG_GPS_DATESTAMP, ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE_REF,
              ExifInterface.TAG_GPS_LONGITUDE, ExifInterface.TAG_GPS_LONGITUDE_REF, ExifInterface.TAG_GPS_PROCESSING_METHOD,
              ExifInterface.TAG_GPS_TIMESTAMP, ExifInterface.TAG_IMAGE_LENGTH, ExifInterface.TAG_IMAGE_WIDTH, ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
              ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL, ExifInterface.TAG_ORIENTATION, ExifInterface.TAG_SUBSEC_TIME,
              ExifInterface.TAG_SUBSEC_TIME_DIGITIZED, ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, ExifInterface.TAG_WHITE_BALANCE
      };

      ExifInterface newExif = new ExifInterface(newPath);
      for (String attribute : attributes) {
        String value = oldExif.getAttribute(attribute);
        if (value != null) newExif.setAttribute(attribute, value);
      }
      newExif.saveAttributes();
    } catch (IOException e) {
      Log.e(TAG, "copyExif: ", e);
    }
  }

  public static byte[] getBitmapBytesWithResize(final String filename, boolean bCompress) {
//    final SMTHHelper helper = SMTHHelper.getInstance();
    Log.d(TAG, "getBitmapBytesWithResize: " + filename);

    if (filename.toLowerCase().endsWith(".gif")) {
      // gif, don't resize
      File infile = new File(filename);
      return getBytesFromFile(infile);
    } else {
      // static image, resize it
      Bitmap theBitmap = loadResizedBitmapFromFile(filename, 1200, 1200, bCompress);

      // save bitmap to temp file
      String newfilename = saveBitmapToFile(theBitmap, filename);

      // copy exif information from old file to new file
      copyExif(filename, newfilename);

      // read data
      assert newfilename != null;
      File infile = new File(newfilename);
      return getBytesFromFile(infile);
    }
  }

  public static Observable<AjaxResponse> publishPost(String boardEngName, String subject, String content, String signature,
                                                     String replyPostID) {
    SMTHHelper helper = SMTHHelper.getInstance();
    return helper.wService.publishPost(boardEngName, subject, content, signature, replyPostID)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io());
  }

  public static Observable<AjaxResponse> editPost(String boardEngName, String postID, String subject, String content) {
    SMTHHelper helper = SMTHHelper.getInstance();
    return helper.wService.editPost(boardEngName, postID, subject, content).subscribeOn(Schedulers.io()).observeOn(Schedulers.io());
  }

  public static Observable<AjaxResponse> sendMail(String userid, String title, String content) {
    SMTHHelper helper = SMTHHelper.getInstance();
    return helper.wService.sendMail("NULL", userid, title, content, "0", "on", "").subscribeOn(Schedulers.io()).observeOn(Schedulers.io());
  }

  public static Post ParseMailContentFromWWW(String content) {
    Document doc = Jsoup.parse(content);

    Post post = new Post();
    post.parsePostContent(doc, false);
    return post;
  }

  public static Post ParsePostListFromWWWMobile(String content, Topic topic) {

    Post result = new Post();
    if (content == null) {
      return result;
    }

    //指定的文章不存在或链接错误
    if(content.contains("指定的文章不存在或链接错误")){
      topic.setTotalPostNoFromString("1");
      result.setAuthor("发生错误");
      result.setRawContent("指定的文章不存在或链接错误");
      return result;
    }

    Document doc = Jsoup.parse(content);

    // 提取标题：第一个 li.f 的文本
    Element titleLi = doc.selectFirst("ul.list.sec > li.f");
    String title = titleLi != null ? titleLi.text().replaceAll("[\\u00B7\\u2022\\u00A0]", "") : "";
    title = title.replace("主题:", "");
    // 提取用户名：第一个 a.nav 中的文本
    Element userLink = doc.selectFirst("ul.list.sec > li .nav a[href]");
    String username = userLink != null ? userLink.text() : "";
    // 提取时间：class为 plant 的 a 标签
    Element timeElement = doc.selectFirst("ul.list.sec > li .nav a.plant");
    String postTimeStr = timeElement != null ? timeElement.text() : "";

    // 提取回复链接：href 包含 /post/
    Element replyLink = doc.selectFirst("ul.list.sec > li .nav a[href*=/post/]");
    String replyHref = replyLink != null ? replyLink.attr("href") : "";
    // 使用正则表达式提取第一个数字字符串
    Pattern pattern = Pattern.compile("\\d+");
    Matcher matcher = pattern.matcher(replyHref);

    String firstNumber = "";
    if (matcher.find()) {
      firstNumber = matcher.group();
      Log.d(TAG, "第一个数字字符串: " + firstNumber);
    } else {
      Log.d(TAG, "未找到数字字符串");
    }

    // 提取正文内容：div.sp 的内容
    Element contentDiv = doc.selectFirst("ul.list.sec > li .sp");
    String contentSP = contentDiv != null ? contentDiv.html() : "";

    result.setTitle(title);
    result.setAuthor(username);
    result.setPostID(firstNumber);

    Date postTime = TimeUtils.convertStringToDate(postTimeStr);
    result.setDate(postTime);
    result.setHtmlContent(contentSP);

    return result;
  }

  public static boolean hasValidContent(Element contentTd) {
    if (contentTd == null) return false;

    String text = contentTd.text().trim();  // 获取纯文本并去除前后空格
    String html = contentTd.html().trim(); // 获取HTML内容并去除前后空格

    // 如果整个HTML为空或只有 <p></p> 这类标签，则视为无效
    return !TextUtils.isEmpty(text) ||
            (!html.contains("<p></p>") && !html.contains("<div></div>") && !html.isEmpty());
  }

  public static List<Post> ParsePostListFromWWW(String content, Topic topic) {
    //final String TAG = "ParsePostListFromWWW";
    List<Post> results = new ArrayList<>();

    Document doc = Jsoup.parse(content);

    // find total posts for this topic, and total pages
    Elements lis = doc.select("li.page-pre");
    if (!lis.isEmpty()) {
      Element li = lis.first();
      // 贴数:152 分页:
      //            Log.d(TAG, li.text());

      Pattern pattern = Pattern.compile("(\\d+)", Pattern.DOTALL);
      assert li != null;
      Matcher matcher = pattern.matcher(li.text());
      if (matcher.find()) {
        String totalPostString = matcher.group(0);
        topic.setTotalPostNoFromString(totalPostString);
      }
    }  else  {
      return results;
    }

    // find all posts
    Elements tables = doc.select("table.article");

    for (Element table : tables) {
      Post post = new Post();

      // find author for this post
      // <span class="a-u-name"><a href="/nForum/user/query/CZB">CZB</a></span>
      Elements authors = table.select("span.a-u-name");
      if (!authors.isEmpty()) {
        Element author = authors.get(0);
        String authorName = author.text();
        post.setAuthor(authorName);
      }

      // find post id for this post
      // <samp class="ico-pos-reply"></samp><a href="/nForum/article/WorkLife/post/1113865" class="a-post">回复</a></li>
      Elements links = table.select("li a.a-post");
      if (!links.isEmpty()) {
        Element link = links.first();
        assert link != null;
        String postID = StringUtils.getLastStringSegment(link.attr("href"));
        post.setPostID(postID);
      }

      // find post position
      // <span class="a-pos">第1楼</span>
      Elements positions = table.select("span.a-pos");
      if (!positions.isEmpty()) {
        Element position = positions.first();
        assert position != null;
        post.setPosition(position.text());
      }

      // find & parse post content
      Elements contents = table.select("td.a-content");
      if (!contents.isEmpty()) {
        for (Element contentTd : contents) {
          // 检查是否包含有效内容
          if (hasValidContent(contentTd)) {
            post.parsePostContent(contentTd, true);
            results.add(post);
          }
        }
      }
    }

    if (results.isEmpty()) {

      // there might be some problems with the response
      //            <div class="error">
      //            <h5>产生错误的可能原因：</h5>
      //            <ul>
      //            <li>
      //            <samp class="ico-pos-dot"></samp>指定的文章不存在或链接错误</li>
      //            </ul>
      //            </div>
      Elements divs = doc.select("div.error");
      if (!divs.isEmpty()) {
        Element div = divs.first();

        topic.setTotalPostNoFromString("1");

        Post post = new Post();
        post.setAuthor("发生错误");
        assert div != null;
        post.setRawContent(div.toString());
        results.add(post);
      }
    }

    return results;
  }


  public static Topic ParseTopicFromElement(Element ele, String type) {
    if ("top10".equals(type) || "sectionhot".equals(type)) {
      // two <A herf> nodes

      // normal hot topic
      // <li><a href="/nForum/article/OurEstate/1685281" title="lj让我走垫资(114)">lj让我走垫资&nbsp;(114)</a></li>

      // special hot topic -- 近期热帖: 1. board信息，没有reply_count
      // <li>
      // <div><a href="/nForum/board/Picture"><span class="board">[贴图]</span></a><a href="/nForum/article/ShiDa/59833" title=" 南都副总编及编辑被处分开除"><span class="title"> 南都副总编及编辑被处分开除</span></a></div>
      // </li>

      Elements as = ele.select("a[href]");
      if (as.size() == 2) {
        Element a1 = as.get(0);
        Element a2 = as.get(1);

        String boardChsName = a1.text().replace("]", "").replace("[", "");
        String boardEngName = StringUtils.getLastStringSegment(a1.attr("href"));

        String title = a2.attr("title");
        String topicID = StringUtils.getLastStringSegment(a2.attr("href"));

        Topic topic = new Topic();
        String reply_count = StringUtils.getReplyCountInParentheses(title);
        if (!reply_count.isEmpty()) {
          title = title.substring(0, title.length() - reply_count.length() - 2);
          topic.setTotalPostNoFromString(reply_count);
        }

        topic.setBoardEngName(boardEngName);
        topic.setBoardChsName(boardChsName);
        topic.setTopicID(topicID);
        topic.setTitle(title);

        //                Log.d(TAG, topic.toString());
        return topic;
      }
    } else if ("pictures".equals(type)) {
      // three <A herf> nodes

      // <li>
      // <a href="/nForum/article/SchoolEstate/430675"><img src="http://images.newsmth.net/nForum/img/hotpic/SchoolEstate_430675.jpg" title="点击查看原帖" /></a>
      // <br /><a class="board" href="/nForum/board/SchoolEstate">[学区房]</a>
      // <br /><a class="title" href="/nForum/article/SchoolEstate/430675" title="这个小学排名还算靠谱吧， AO爸爸排的。。。">这个小学排名还算靠谱吧， AO爸爸排的。。。</a>
      // </li>
      Elements as = ele.select("a[href]");
      if (as.size() == 3) {
        Element a1 = as.get(1);
        Element a2 = as.get(2);

        String boardChsName = a1.text().replace("]", "").replace("[", "");
        String boardEngName = StringUtils.getLastStringSegment(a1.attr("href"));

        String title = a2.attr("title");
        String topicID = StringUtils.getLastStringSegment(a2.attr("href"));

        Topic topic = new Topic();
        topic.setBoardEngName(boardEngName);
        topic.setBoardChsName(boardChsName);
        topic.setTopicID(topicID);
        topic.setTitle(title);

        //                Log.d(TAG, topic.toString());
        return topic;
      }
    }
    return null;
  }

  // parse guidance page, to find all hot topics
  public static List<Topic> ParseHotTopicsFromWWW(String content) {
    List<Topic> results = new ArrayList<>();
    if (content == null || content.isEmpty()) {
      return results;
    }

    Topic topic;
    Document doc = Jsoup.parse(content);

    // find top10
    // <div id="top10">
    Elements top10s = doc.select("div#top10");
    if (top10s.size() == 1) {
      // add separator
      topic = new Topic("本日十大热门话题");
      results.add(topic);

      // parse hot hopic
      Element top10 = top10s.first();
      assert top10 != null;
      Elements lis = top10.getElementsByTag("li");

      for (Element li : lis) {
        topic = ParseTopicFromElement(li, "top10");
        if (topic != null) {
          //                    Log.d(TAG, topic.toString());
          results.add(topic);
        }
      }
    }

    // find hotspot
    // <div id="hotspot" class="block">
    // skip this part, it's tedious
    //        Elements hotspots = doc.select("div#hotspot div.topics");
    //        if(hotspots.size() == 1) {
    //            // add separator
    //            topic = new Topic("近期热帖");
    //            results.add(topic);
    //
    //            // parse hot hopic
    //            Element hotspot = hotspots.first();
    //            Elements lis = hotspot.getElementsByTag("li");
    //
    //            for(Element li: lis) {
    //                topic = ParseTopicFromElement(li, "hotspot");
    //                if(topic != null) {
    ////                    Log.d(TAG, topic.toString());
    //                    results.add(topic);
    //                }
    //            }
    //        }

    // find hot picture
    // <div id="pictures" class="block">
    Elements pictures = doc.select("div#pictures");
    for (Element section : pictures) {
      // add separator
      Elements sectionNames = section.getElementsByTag("h3");
      if (sectionNames.size() == 1) {
        Element sectionName = sectionNames.first();
        assert sectionName != null;
        topic = new Topic(sectionName.text());
        results.add(topic);
      }

      Elements lis = section.select("div li");
      for (Element li : lis) {
        //                Log.d(TAG, li.toString());
        topic = ParseTopicFromElement(li, "pictures");
        if (topic != null) {
          //                    Log.d(TAG, topic.toString());
          results.add(topic);
        }
      }
    }

    // find hot topics from each section
    // <div id="hotspot" class="block">
    Elements sections = doc.select("div.b_section");
    for (Element section : sections) {
      // add separator
      Elements sectionNames = section.getElementsByTag("h3");
      if (sectionNames.size() == 1) {
        Element sectionName = sectionNames.first();
        assert sectionName != null;
        String name = sectionName.text();
        if (name.equals("系统与祝福")) {
          continue;
        }
        topic = new Topic(name);
        results.add(topic);
      }

      Elements lis = section.select("div.topics li");
      for (Element li : lis) {
        //                Log.d(TAG, li.toString());
        topic = ParseTopicFromElement(li, "sectionhot");
        if (topic != null) {
          //                    Log.d(TAG, topic.toString());
          results.add(topic);
        }
      }
    }

    return results;
  }

  public static List<Topic> ParseBoardTopicsFromWWWMobile(String content) {
    List<Topic> results = new ArrayList<>();
    if (content == null) {
      return results;
    }


    Document doc = Jsoup.parse(content);

    // <li class="page-select"><a title="当前页">2</a></li>
    String currentPage = null;
    Elements lis = doc.select("ul.list.sec li");

    if (!lis.isEmpty()) {

      for (Element li : lis) {
        Topic topic = new Topic();
        // 提取第一个 div 中的标题和数量信息
        Element firstDiv = li.selectFirst("div:nth-of-type(1)");
        String titleWithCount = Objects.requireNonNull(firstDiv).text();
        Element firstDivLink = li.selectFirst("div:nth-of-type(1) a");
        //略过文摘
        if ("top".equals(Objects.requireNonNull(firstDivLink).attr("class"))) {
          continue;
        }
        //Topic Href: "/article/Apple/single/1551240/0";
        String href = firstDivLink.attr("href");
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(href);
        String id="";
        if (matcher.find()) {
          id = matcher.group();
        }

        // 提取第二个 div 中的日期和作者信息
        Element secondDiv = li.selectFirst("div:nth-of-type(2)");
        assert secondDiv != null;
        Element userLink = secondDiv.selectFirst("a");
        assert userLink != null;
        String firstAuthor = "  "+ userLink.text();
        String[] parts = secondDiv.text().split(" ");
        String firstDate= parts[1].replace("&nbsp;","");
        topic.setTitle(titleWithCount);
        topic.setTopicID(id);
        topic.setAuthor(firstAuthor);
        topic.setPublishDate(firstDate);
        topic.setReplier(firstAuthor);
        topic.setReplyDate(firstDate);
        if (topic.getTitle() != null && !topic.getTitle().isEmpty()) {
          results.add(topic);
        }

      }

    }
    // To handle Error case :  <title>水木社区-错误信息</title>
    else
    {
      return results;
    }

    return results;
  }


  public static String getSubString(String str, int maxLength) {
    if (str == null || str.isEmpty()) {
      return "";
    }
    return str.length() <= maxLength ? str : str.substring(0, maxLength);
  }
  // parse board topics from WWW
  public static List<Topic> ParseBoardTopicsFromWWW(String content) {
    List<Topic> results = new ArrayList<>();
    if (content == null) {
      return results;
    }

    Document doc = Jsoup.parse(content);

    // <li class="page-select"><a title="当前页">2</a></li>
    String currentPage = null;
    Elements lis = doc.select("li.page-select");

    if (!lis.isEmpty()) {
      Element li = lis.first();
      assert li != null;
      // currentPage = li.text();
      //            Log.d(TAG, "ParseBoardTopicsFromWWW: " + currentPage);
    }
    // To handle Error case :  <title>水木社区-错误信息</title>
    else
    {
      return results;
    }

    //        <tr class="top">
    //        <td class="title_8">
    //        <a target="_blank" href="/nForum/article/FamilyLife/1757972219" title="在新窗口打开此主题">
    //        <samp class="tag ico-pos-article-light"></samp>
    //        </a>
    //        </td>
    //        <td class="title_9"><a href="/nForum/article/FamilyLife/1757972219">2岁女孩找妈妈 其父母终于被找到</a>
    //        <samp class="tag-att ico-pos-article-attach"></samp><span class="threads-tab">[<a href="/nForum/article/FamilyLife/1757972219?p=2">2</a>]</span></td>
    //        <td class="title_10">2016-05-05</td>
    //        <td class="title_12">|&ensp;<a href="/nForum/user/query/Muscle" class="c63f">Muscle</a></td>
    //        <td class="title_11 middle"></td>
    //        <td class="title_11 middle"></td>
    //        <td class="title_11 middle">14</td>
    //        <td class="title_10"><a href="/nForum/article/FamilyLife/1757972219?p=2#a14" title="跳转至最后回复">21:38:58&emsp;</a></td>
    //        <td class="title_12">|&ensp;<a href="/nForum/user/query/kxxx" class="c09f">kxxx</a></td>
    //        </tr>

    // get all trs
    Elements trs = doc.select("table.board-list tbody tr");
    for (Element tr : trs) {
      //            Log.d(TAG, "ParseBoardTopicsFromWWW: " + tr.toString());
      Topic topic = new Topic();

      String trClass = tr.attr("class");
      if (TextUtils.equals(trClass, "top")) {
        // is sticky
        topic.isSticky = true;
      }

      Elements tds = tr.getElementsByTag("td");
      for (Element td : tds) {
        String tdClass = td.attr("class");
        //                Log.d(TAG, "ParseBoardTopicsFromWWW: td.class = " + tdClass);

        if (TextUtils.equals(tdClass, "title_9")) {
          // <td class="title_9"><a href="/nForum/article/FamilyLife/1757972219">2岁女孩找妈妈 其父母终于被找到</a>
          Elements as = td.getElementsByTag("a");
          if (!as.isEmpty()) {
            Element a = as.first();
            assert a != null;
            topic.setTitle(a.text());

            String topicURL = a.attr("href");
            topic.setTopicID(StringUtils.getLastStringSegment(topicURL));
          }
          // <samp class="tag-att ico-pos-article-attach"></samp>
          // find attachment flag
          Elements samps = td.getElementsByTag("samp");
          if (!samps.isEmpty()) {
            topic.setHasAttach(true);
          }
        } else if (TextUtils.equals(tdClass, "title_10")) {
          // <td class="title_10">2016-05-05</td>
          // <td class="title_10"><a href="/nForum/article/FamilyLife/1757972219?p=2#a14" title="跳转至最后回复">21:38:58&emsp;</a></td>
          String publishDate = topic.getPublishDate();
          if (publishDate == null || publishDate.isEmpty()) {
            topic.setPublishDate(td.text());
          } else {
            topic.setReplyDate(td.text());
          }
        } else if (TextUtils.equals(tdClass, "title_12")) {
          // <td class="title_12">|&ensp;<a href="/nForum/user/query/Muscle" class="c63f">Muscle</a></td>
          // <td class="title_12">|&ensp;<a href="/nForum/user/query/kxxx" class="c09f">kxxx</a></td>
          String author = topic.getAuthor();
          String value = td.text().replace("|", "").trim();
          if (author == null || author.isEmpty()) {
            topic.setAuthor(value);
          } else {
            topic.setReplier(value);
          }
        } else if (TextUtils.equals(tdClass, "title_11 middle")) {
          // <td class="title_11 middle">评分</td>
          // <td class="title_11 middle">like</td>
          // <td class="title_11 middle">回复: 14</td>
          String score = topic.getScore();
          String likes = topic.getLikes();
          String value = td.text();
          if (score == null) {
            topic.setScore(value);
          } else if (likes == null) {
            topic.setLikes(value);
          } else {
            topic.setReplyCounts(value);
          }
        }
      }

      // Log.d("ParseBoardTopics", topic.toString());
      if (topic.getTitle() != null && !topic.getTitle().isEmpty()) {
        results.add(topic);
      }
    }

    return results;
  }

  // parse topics from nForum search results
  //    <tr>
  //    <td class="title_8">1.</td>
  //    <td class="title_14">
  //    <a target="_blank" href="/nForum/article/PocketLife/2217534" title="在新窗口打开此主题">
  //    <samp class="tag ico-pos-article-normal"></samp>
  //    </a>
  //    </td>
  //    <td class="title_9"><a href="/nForum/article/PocketLife/2217534">更改了一下zSMTH&#45;Android的颜色搭配</a></td>
  //    <td class="title_10">08:47:21&emsp;</td>
  //    <td class="title_12">|&ensp;<a href="/nForum/user/query/mozilla" class="c63f">mozilla</a></td>
  //    <td class="title_11 middle">9</td>
  //    <td class="title_10"><a href="/nForum/article/PocketLife/2217534?p=1#a9" title="跳转至最后回复">11:04:03&emsp;</a></td>
  //    <td class="title_12">|&ensp;<a href="/nForum/user/query/mozilla" class="c09f">rasper</a></td>
  //    </tr>
  public static List<Topic> ParseSearchResultFromWWW(String content) {
    final String TAG = "ParseSearchResult";

    List<Topic> results = new ArrayList<>();
    if (content == null) {
      return results;
    }
    // Log.d(TAG, content);

    // parse topics using Jsoup
    Document doc = Jsoup.parse(content);

    // get all lis
    Elements divs = doc.select("div.b-content");
    if (divs.isEmpty()) {
      Log.d(TAG, "ParseSearchResultFromWWW: " + "Did not find div.b-content");
      return results;
    }
    Element div = divs.first();

    assert div != null;
    Elements trs = div.getElementsByTag("tr");
    for (Element tr : trs) {
      // Log.d(TAG, "ParseSearchResultFromWWW: " + tr.toString());
      Elements tds = tr.getElementsByTag("td");
      if (tds.isEmpty()) {
        continue;
      }

      Topic topic = new Topic();
      String title = "";
      String author = "";
      String replier = "";
      String publishDate = "";
      String replyDate = "";
      String topicID = "";

      for (Element td : tds) {
        if (TextUtils.equals(td.attr("class"), "title_9")) {
          title = td.text();
          Elements As = td.getElementsByTag("A");
          if (!As.isEmpty()) {
            Element A = As.first();
            assert A != null;
            topicID = StringUtils.getLastStringSegment(A.attr("href"));
          }
        } else if (TextUtils.equals(td.attr("class"), "title_10")) {
          if (publishDate.isEmpty()) {
            publishDate = td.text();
          } else {
            replyDate = td.text();
          }
        } else if (TextUtils.equals(td.attr("class"), "title_12")) {
          String person = td.text().replace("|", "").trim();
          if (author.isEmpty()) {
            author = person;
          } else {
            replier = person;
          }
        }
      }
      topic.setAuthor(author);
      topic.setTopicID(topicID);
      topic.setTitle(title);
      topic.setReplier(replier);
      topic.setPublishDate(publishDate);
      topic.setReplyDate(replyDate);

      // Log.d(TAG, "ParseSearchResultFromWWW: " + topic.toString());
      if (topic.getTitle() != null && !topic.getTitle().isEmpty()) {
        results.add(topic);
      }
    }

    return results;
  }

  public static List<Board> ParseFavoriteBoardsFromWWW(String content) {
    List<Board> boards = new ArrayList<>();

//    o.f(1,'次常用版面 ',5,'');
//    o.o(false,1,134,84878,'[生活]','AutoWorld','汽车世界','solorist freshcool hairstal alba',13450,133,2);
//    o.o(false,1,1250,435,'[学科]','ChildEducation','儿童教育','cococi',391,1249,0);
//    o.o(true,1,678,59144,'[供求]','SecondHand','二手货交易','[目录]',981,677,0);
//    o.o(false,1,1108,87992,'[游戏]','TVGame','视频游戏','ibriano',68756,1107,0);

    final String error_msg = "您还没有登录，或者长时间没有动作，请您重新登录";
    if (content.contains(error_msg)) {
      Board board = new Board();
      board.initAsInvalid(error_msg + "！\n" + "登录后，请点击右上角'刷新'收藏夹内容。");
      boards.add(board);

      return boards;
    }

    // 先提取用户创建的目录
    // o.f(1,'次常用版面 ',5,'');
    Pattern pattern = Pattern.compile("o\\.f\\((\\d+),'([^']+)',\\d+,''\\);");
    Matcher matcher = pattern.matcher(content);
    while (matcher.find()) {
      Board board = new Board();
      board.initAsFolder(matcher.group(1), matcher.group(2));

//      Log.d(TAG, board.toString());
      boards.add(board);
    }

    // 再提取收藏的版面, 或者系统的二级目录
    // o.o(false,1,998,22156,'[站务]','Ask','新用户疑难解答','haning BJH',733,997,0);
    // o.o(true,1,678,59144,'[供求]','SecondHand','二手货交易','[目录]',981,677,0);
    pattern = Pattern.compile("o\\.o\\((\\w+),\\d+,(\\d+),\\d+,'\\[([^']+)\\]','([^']+)','([^']+)','([^']*)',\\d+,\\d+,\\d+\\)");
    matcher = pattern.matcher(content);
    while (matcher.find()) {
      String boardType = matcher.group(1);
      //String boardID = matcher.group(2);
      String category = matcher.group(3);
      String engName = matcher.group(4);
      String chsName = matcher.group(5);
      String moderator = matcher.group(6);

      Board board = new Board();
      if(TextUtils.equals(boardType, "true") || TextUtils.equals(moderator, "[目录]")) {
        // o.o(true,1,1368,0,'[数码]','SmartLife','智能生活','[目录]',0,1367,0);
        board.initAsSection(engName,chsName);
      } else {
        assert moderator != null;
        board.initAsBoard(chsName,engName, category, moderator);
      }

//      Log.d(TAG, board.toString());
      boards.add(board);
    }

//    Log.d(TAG, boards.toString());
    return boards;
  }

  // sample input: mailbox_response.html, refer_at_posts.html
  public static List<Mail> ParseMailsFromWWW(String content) {
    List<Mail> mails = new ArrayList<>();

    Document doc = Jsoup.parse(content);

    // <div class="error"><h5>产生错误的可能原因：</h5><ul><li><samp class="ico-pos-dot"></samp>请勿频繁登录</li></ul></div>
    Elements errors = doc.select("div.error");
    if (!errors.isEmpty()) {
      Element error = errors.first();
      assert error != null;
      Mail mail = new Mail(error.text());
      mails.add(mail);
      return mails;
    }

    // <li class="page-pre">邮件总数:<i>177</i>&emsp;分页:</li>
    // <li class="page-pre">文章总数:<i>17</i>&emsp;分页:</li>
    Elements is = doc.select("div.page li.page-pre i");
    if (!is.isEmpty()) {
      Element i = is.first();
      assert i != null;
      String totalMails = i.text();
      // Log.d(TAG, "ParseMailsFromWWW: " + totalMails);
      MailListContent.setTotalMails(Integer.parseInt(totalMails));
    }

    // <li class="page-select"><a title="当前页">1</a></li>
    Elements lis = doc.select("div.page li.page-select");
    // Log.d(TAG, "ParseMailsFromWWW: " + lis.toString());
    if (!lis.isEmpty()) {
      // find
      Element li = lis.first();
      assert li != null;
      String page = li.text();
      Mail mail = new Mail(String.format(Locale.CHINA,"第%s页", page));
      mails.add(mail);
    }

    Elements trs = doc.select("table.m-table tr");
    //        <tr class="no-read">
    //        <td class="title_1">
    //        <input type="checkbox" name="m_175" class="mail-item" />
    //        </td>
    //        <td class="title_2"><a href="/nForum/user/query/mozilla">mozilla</a></td>
    //        <td class="title_3"><a href="/nForum/mail/inbox/175.json" class="mail-detail">Re: 求助，dish的机顶盒到货了，锅怎么办？？？&#40;转寄&#41;</a></td>
    //        <td class="title_4">2016-04-27 16:38:54</td>
    //        </tr>
    for (Element tr : trs) {
      Mail mail = new Mail();

      if (TextUtils.equals(tr.attr("class"), "no-read")) {
        mail.isNew = true;
      }

      Elements tds = tr.getElementsByTag("td");
      for (Element td : tds) {
        if (TextUtils.equals(td.attr("class"), "title_2")) {
          // <td class="title_2"><a href="/nForum/user/query/Wunderman">Wunderman</a></td>
          // <td class="title_2"><a href="/nForum/board/PocketLife">PocketLife</a></td>
          if (mail.author == null || mail.author.isEmpty()) {
            mail.author = td.text();
          } else {
            mail.fromBoard = td.text();
          }
        } else if (TextUtils.equals(td.attr("class"), "title_3")) {
          // <td class="title_3"><a href="/nForum/article/PocketLife/ajax_single/2228708.json" class="m-single" _index="16">Re: zSMTH 1.0.0版发布</a></td>
          mail.title = td.text();
          Elements as = td.getElementsByTag("a");
          if (!as.isEmpty()) {
            Element a = as.first();
            assert a != null;
            mail.url = a.attr("href");
            mail.referIndex = a.attr("_index");
          }
        } else if (TextUtils.equals(td.attr("class"), "title_4")) {
          // <td class="title_4">2016-05-06 04:13:55</td>
          mail.date = td.text();
        }
      }

      if (mail.author != null && !mail.author.isEmpty() && !TextUtils.equals(mail.author, "作者")) {
        // only valid mail will be added
        // referred post have table head, so we make sure author != "作者"
        mails.add(mail);
      }
    }

    if (mails.isEmpty()) {
      Mail mail = new Mail(".无信件.");
      mails.add(mail);
    }

    return mails;
  }


  public static String parseDeleteResponseMobile(String response) {
    if (response == null || response.isEmpty()) {
      return "删除失败";
    }

    if (response.contains("删除成功")|| response.contains("删除文章成功")) {
      return "删除成功";
    }
    return "删除失败";
  }


  // sample response: assets/deletion_response.html
  public static String parseDeleteResponse(String response) {
    if (response == null || response.isEmpty()) {
      return "删除失败";
    }
    if (response.contains("删除成功")|| response.contains("删除文章成功")) {
      return "删除成功";
    }
    return "删除失败";
    /*
    String result = "";
    Document doc = Jsoup.parse(response);
    Elements bodies = doc.getElementsByTag("body");

    if (!bodies.isEmpty()) {
      Element body = bodies.first();

      assert body != null;
      Elements divs = body.select("div.nav");
      for (Element div : divs) {
        div.remove();
      }

      Elements as = body.getElementsByTag("a");
      for (Element a : as) {
        a.remove();
      }

      result = body.text();
    }

    result = result.replaceAll("用户名：", "");
    result = result.replaceAll("密　码：", "");
    return result;
    */

  }

  // sample response: assets/deletion_response.html
  public static String parseRepostResponse(String response) {
    if (response == null) {
      return "错误的返回结果";
    }

    if (response.contains("操作成功: 转贴成功！")) {
      return "操作成功: 转贴成功！";
    }

    Document doc = Jsoup.parse(response);
    Elements errors = doc.select("table.error");
    if (!errors.isEmpty()) {
      Element error = errors.first();
      assert error != null;
      return error.text();
    }

    return "未识别的返回";
  }

  /*
   * All Boards related methods
   * Starts here
   */
  public static String getCacheFile(int type, String folder) {
    if (type == BOARD_TYPE_ALL) {
      return ALL_BOARD_CACHE_FILE;
    } else if (type == BOARD_TYPE_FAVORITE) {
      if (folder == null || folder.isEmpty()) {
        folder = "ROOT";
      }
      return String.format(Locale.CHINA,"%s-%s", FAVORITE_BOARD_CACHE_PREFIX, folder);
    }
    return null;
  }

  public static List<Board> LoadBoardListFromCache(int type, String folder) {
    String filename = getCacheFile(type, folder);
    List<Board> boards = new ArrayList<>();
    try {
      Kryo kryo = new Kryo();
      Input input = new Input(SMTHApplication.getAppContext().openFileInput(filename));
      boards = kryo.readObject(input, new ArrayList<Board>(){}.getClass());
      input.close();
      Log.d("LoadBoardListFromCache", String.format(Locale.CHINA,"%d boards loaded from cache file %s", boards.size(), filename));
    } catch (Exception e) {
      Log.d("LoadBoardListFromCache", e.toString());
      Log.d("LoadBoardListFromCache", "failed to load boards from cache file " + filename);
    }
    return boards;
  }

  public static void SaveBoardListToCache(List<Board> boards, int type, String folder) {
    String filename = getCacheFile(type, folder);
    try {
      Kryo kryo = new Kryo();
      Output output = new Output(SMTHApplication.getAppContext().openFileOutput(filename, Context.MODE_PRIVATE));
      kryo.writeObject(output, boards);
      output.close();
      Log.d("SaveBoardListToCache", String.format(Locale.CHINA,"%d boards saved to cache file %s", boards.size(), filename));
    } catch (Exception e) {
      Log.d("SaveBoardListToCache", e.toString());
      Log.d("SaveBoardListToCache", "failed to save boards to cache file " + filename);
    }
  }

  public static void ClearBoardListCache(int type, String folder) {
    String filename = getCacheFile(type, folder);
    try {
      if (SMTHApplication.getAppContext().deleteFile(filename)) {
        Log.d("ClearBoardListCache", String.format(Locale.CHINA,"delete cache file %s successfully", filename));
        //return;
      }
    } catch (Exception e) {
      Log.d("ClearBoardListCache", e.toString());
      Log.d("ClearBoardListCache", "Failed to delete cache file " + filename);
    }
  }

  public static List<Board> LoadFavoriteBoardsInFolder(final String path) {
    Iterable<Board> its = SMTHHelper.getInstance().wService.getFavoriteBoardsInFolder(path).flatMap((Function<ResponseBody, Observable<Board>>) responseBody -> {
      try {
        String response = SMTHHelper.DecodeResponseFromWWW(responseBody.bytes());
        // Log.d(TAG, response);
        List<Board> boards = SMTHHelper.ParseFavoriteBoardsFromWWW(response);
        return Observable.fromIterable(boards);
      } catch (Exception e) {
        Log.e(TAG, "Failed to load favorite {" + path + "}");
        Log.e(TAG, Log.getStackTraceString(e));
        return null;
      }
    }).blockingIterable();

    List<Board> results = MakeList.makeList(its);

    SaveBoardListToCache(results, BOARD_TYPE_FAVORITE, path);

    return results;
  }

  public static List<Board> LoadFavoriteBoardsInSection(final String path) {
    Iterable<Board> its = SMTHHelper.getInstance().wService.getFavoriteBoardsInSection(path).flatMap((Function<ResponseBody, Observable<Board>>) responseBody -> {
      try {
        String response = SMTHHelper.DecodeResponseFromWWW(responseBody.bytes());
        // Log.d(TAG, response);
        List<Board> boards = SMTHHelper.ParseFavoriteBoardsFromWWW(response);
        return Observable.fromIterable(boards);
      } catch (Exception e) {
        Log.e(TAG, "Failed to load favorite {" + path + "}");
        Log.e(TAG, Log.getStackTraceString(e));
        return null;
      }
    }).blockingIterable();

    List<Board> results = MakeList.makeList(its);

    SaveBoardListToCache(results, BOARD_TYPE_FAVORITE, path);

    return results;
  }

  // load all boards from WWW, recursively
  // http://stackoverflow.com/questions/31246088/how-to-do-recursive-observable-call-in-rxjava
  public static List<Board> LoadAllBoardsFromWWW() {
    final String[] SectionNames = { "社区管理", "国内院校", "休闲娱乐", "五湖四海", "游戏运动", "社会信息", "知性感性", "文化人文", "学术科学", "电脑技术", "终止版面" };
    final String[] SectionURLs = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A" };
    final List<BoardSection> sections = new ArrayList<>();
    for (int index = 0; index < SectionNames.length; index++) {
      BoardSection section = new BoardSection();
      section.sectionURL = SectionURLs[index];
      section.sectionName = SectionNames[index];
      sections.add(section);
    }

    Iterable<Board> its =
            Observable.fromIterable(sections).flatMap((Function<BoardSection, Observable<Board>>) SMTHHelper::loadBoardsInSectionFromWWW).flatMap((Function<Board, Observable<Board>>) SMTHHelper::loadChildBoardsRecursivelyFromWWW).filter(Board::isBoard).blockingIterable();


    List<Board> boards = MakeList.makeList(its);


    // sort the board list by chinese name
    //Collections.sort(boards, new BoardListContent.ChineseComparator());
    boards.sort(new BoardListContent.EnglishComparator());
    Log.d("LoadAllBoardsFromWWW", String.format(Locale.CHINA,"%d boards loaded from network", boards.size()));

    // save boards to disk
    SaveBoardListToCache(boards, BOARD_TYPE_ALL, null);

    return boards;
  }

  public static Observable<Board> loadChildBoardsRecursivelyFromWWW(Board board) {
    if (board.isSection()) {
      BoardSection section = new BoardSection();
      section.sectionURL = board.getSectionID();
      section.sectionName = board.getSectionName();
      section.parentName = board.sectionPath;

//      Log.d(TAG, section.toString());

      // load recruisively
      return SMTHHelper.loadBoardsInSectionFromWWW(section)
              .subscribeOn(Schedulers.io())
              .observeOn(Schedulers.io())
              .flatMap((Function<Board, Observable<Board>>) SMTHHelper::loadChildBoardsRecursivelyFromWWW);
    } else {
      return Observable.just(board);
    }
  }

  public static Observable<Board> loadBoardsInSectionFromWWW(final BoardSection section) {
    String sectionURL = section.sectionURL;
    return SMTHHelper.getInstance().wService.getBoardsBySection(sectionURL).flatMap((Function<ResponseBody, Observable<Board>>) responseBody -> {
      try {
        String response = responseBody.string();
        List<Board> boards = SMTHHelper.ParseBoardsInSectionFromWWW(response, section);
        return Observable.fromIterable(boards);
      } catch (Exception e) {
        Log.e(TAG, Log.getStackTraceString(e));
        return null;
      }
    });
  }

  public static List<Board> ParseBoardsInSectionFromWWW(String content, BoardSection section) {
    List<Board> boards = new ArrayList<>();

    //        <tr><td class="title_1"><a href="/nForum/section/Association">协会社团</a><br />Association</td><td class="title_2">[二级目录]<br /></td><td class="title_3">&nbsp;</td><td class="title_4 middle c63f">&nbsp;</td><td class="title_5 middle c09f">&nbsp;</td><td class="title_6 middle c63f">&nbsp;</td><td class="title_7 middle c09f">&nbsp;</td></tr>
    //        <tr><td class="title_1"><a href="/nForum/board/BIT">北京理工大学</a><br />BIT</td><td class="title_2"><a href="/nForum/user/query/mahenry">mahenry</a><br /></td><td class="title_3"><a href="/nForum/article/BIT/250116">今年几万斤苹果都滞销了，果农欲哭无泪！</a><br />发贴人:&ensp;jingling6787 日期:&ensp;2016-03-22 09:19:09</td><td class="title_4 middle c63f">11</td><td class="title_5 middle c09f">2</td><td class="title_6 middle c63f">5529</td><td class="title_7 middle c09f">11854</td></tr>
    //        <tr><td class="title_1"><a href="/nForum/board/Orienteering">定向越野</a><br />Orienteering</td><td class="title_2"><a href="/nForum/user/query/onceloved">onceloved</a><br /></td><td class="title_3"><a href="/nForum/article/Orienteering/59193">圆明园定向</a><br />发贴人:&ensp;jiang2000 日期:&ensp;2016-03-19 14:19:10</td><td class="title_4 middle c63f">0</td><td class="title_5 middle c09f">0</td><td class="title_6 middle c63f">4725</td><td class="title_7 middle c09f">18864</td></tr>

    Document doc = Jsoup.parse(content);
    // get all tr
    Elements trs = doc.select("table.board-list tr");
    for (Element tr : trs) {
      //            Log.d("Node", tr.toString());

      Elements t1links = tr.select("td.title_1 a[href]");
      if (t1links.size() == 1) {
        Element link1 = t1links.first();
        assert link1 != null;
        String temp = link1.attr("href");

        String chsBoardName;
        String engBoardName;
        String moderator = "";
        String folderChsName;
        String folderEngName;

        Pattern boardPattern = Pattern.compile("/nForum/board/(\\w+)");
        Matcher boardMatcher = boardPattern.matcher(temp);
        if (boardMatcher.find()) {
          engBoardName = boardMatcher.group(1);
          chsBoardName = link1.text();
          // it's a normal board
          Elements t2links = tr.select("td.title_2 a[href]");
          if (t2links.size() == 1) {
            // if we can find link to moderator, set moderator
            // it's also possible that moderator is empty, so no link can be found
            Element link2 = t2links.first();
            assert link2 != null;
            moderator = link2.text();
          }

          Board board = new Board();
          board.initAsBoard(chsBoardName,engBoardName,  section.getSectionPath(), moderator);

          boards.add(board);

//          Log.d(TAG, board.toString());
        }

        Pattern sectionPattern = Pattern.compile("/nForum/section/(\\w+)");
        Matcher sectionMatcher = sectionPattern.matcher(temp);
        if (sectionMatcher.find()) {
          // it's a section
          folderEngName = sectionMatcher.group(1);
          folderChsName = link1.text();

          Board board = new Board();
          board.initAsSection(folderEngName, folderChsName);
          board.sectionPath = section.getSectionPath();
          boards.add(board);

//          Log.d(TAG, board.toString());
        }
      }
    }

    return boards;
  }
  /*
   * All Boards related methods
   * Ends here
   */

  // smth images might be relative URLs
  // <a target="_blank" href="//static.mysmth.net/nForum/att/FamilyLife/1763462541/17096">
  // <img border="0" title="单击此查看原图" src="//static.mysmth.net/nForum/att/FamilyLife/1763462541/17096/large" class="resizeable" /></a>

  public static String preprocessSMTHImageURL(String original) {
    String url = original;
    if(null != original) {
      if (original.startsWith("//")) {
        // images in post or avatar
        return "https:" + original;
      } else if (original.startsWith("/nForum")) {
        url = Settings.getInstance().getWebAddr() + original;
      }
      if(Settings.getInstance().isImageSourceCDN()) {
        url = url.replace(SMTH_IMAGE_PREFIX_DIRECT, SMTH_IMAGE_PREFIX_CDN);
      } else {
        url = url.replace(SMTH_IMAGE_PREFIX_CDN, SMTH_IMAGE_PREFIX_DIRECT);
      }
    }
    return url;
  }

}

class OkHttpUtil {
  /**
   * X509TrustManager instance which ignored SSL certification
   */
  @SuppressLint("CustomX509TrustManager")
  public static final X509TrustManager IGNORE_SSL_TRUST_MANAGER_X509 = new X509TrustManager() {
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
      Log.d("SSL", "checkClientTrusted");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) {
      Log.d("SSL", "checkClientTrusted");
      /*
      try {
        chain[0].checkValidity();
      } catch (CertificateExpiredException|CertificateNotYetValidException e ) {
        Log.d("zSMTH-v",e.toString());
      }
       */
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[] {};
    }
  };

  /**
   * Get initialized SSLContext instance which ignored SSL certification
   *
   */
  public static SSLContext getIgnoreInitedSslContext() throws NoSuchAlgorithmException, KeyManagementException {
    SSLContext sslContext = SSLContext.getInstance("SSL");
    sslContext.init(null, new TrustManager[] { IGNORE_SSL_TRUST_MANAGER_X509 }, new SecureRandom());
    return sslContext;
  }

  /**
   * Get HostnameVerifier which ignored SSL certification
   *
   */
  public static HostnameVerifier getIgnoreSslHostnameVerifier() {
    return (arg0, arg1) -> {
      return true;
      //return HttpsURLConnection.getDefaultHostnameVerifier().verify(arg0,arg1);
    };
  }

}

class RequestInterceptor implements Interceptor {
  @NonNull
  @Override
  public Response intercept(@NonNull Chain chain) throws IOException {
    Request request = chain.request();

    // 打印请求 URL 和方法
    Log.d("HTML_REQ", "Sending request to " + request.url());
    Log.d("HTML_REQ", "Method: " + request.method());

    // 可选：添加公共 Header，例如：
    Request newRequest = request.newBuilder()
            .build();

    return chain.proceed(newRequest);
  }
}

class ResponseInterceptor implements Interceptor {
  @NonNull
  @Override
  public Response intercept(@NonNull Chain chain) throws IOException {
    Response response = chain.proceed(chain.request());

    // 打印响应码和耗时
    Log.d("HTML_RSP", "Received response for " + response.request().url());
    Log.d("HTML_RSP", "Status Code: " + response.code());
    Log.d("HTML_RSP", "Time taken: " +
            (Long.parseLong(String.valueOf(response.receivedResponseAtMillis())) - response.sentRequestAtMillis()) + "ms");

    // 可选：读取响应体内容（注意不要多次调用 body.string()）
    ResponseBody responseBody = response.body();
    if (responseBody != null) {
      String bodyString = responseBody.string();
      Log.d("HTML_RSP", "Response Body:\n" + bodyString);

      // 重新设置回响应中以便后续处理继续使用
      responseBody = ResponseBody.create(bodyString, responseBody.contentType());
    }

    return response.newBuilder().body(responseBody).build();
  }
}



