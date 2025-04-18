package com.zfdang.zsmth_android.helpers;

/*
  Created by zfdang on 2016-5-14.
 */

import java.util.regex.Pattern;

// https://github.com/klinker24/Talon-for-Twitter/blob/master/src/main/java/com/klinker/android/twitter/utils/text/Regex.java
public class Regex {

  // https://mathiasbynens.be/demo/url-regex
  /*
  public static final Pattern Emosol_WEB_URL_PATTERN = Pattern.compile(
      "((?:https?:\\/\\/|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}\\/)(?:[^\\s()<>]+|\\((?:[^\\s()<>]+|(?:\\([^\\s()<>]+\\)))*\\))+(?:\\((?:[^\\s()<>]+|(?:\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:\\'\".,<>?\\xab\\xbb\\u201c\\u201d\\u2018\\u2019]))",
      Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

  public static final Pattern Diegoperini_WEB_URL_PATTERN = Pattern.compile(
      "(?i)\\b(?:(?:https?|ftp)://)(?:\\S+(?::\\S*)?@)?(?:(?!(?:10|127)(?:\\.\\d{1,3}){3})(?!(?:169\\.254|192\\.168)(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+)(?:\\.(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+)*(?:\\.(?:[a-z\\u00a1-\\uffff]{2,}))\\.?)(?::\\d{2,5})?(?:[/?#]\\S*)?(?:\\b|$)");

  */

  /**
   * Regular expression pattern to match RFC 1738 URLs
   * List accurate as of 2007/06/15.  List taken from:
   * <a href="http://data.iana.org/TLD/tlds-alpha-by-domain.txt">...</a>
   * This pattern is auto-generated by //device/tools/make-iana-tld-pattern.py
   */
  public static final Pattern WEB_URL_PATTERN = Pattern.compile(
      "((?:(http|https|Http|Https):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
          + "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
          + "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?"
          + "((?:(?:[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}\\.)+"
          // named host
          + "(?:"
          // plus top level domain
          + "(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])"
          + "|(?:biz|b[abdefghijmnorstvwyz])"
          + "|(?:cat|com|coop|c[acdfghiklmnoruvxyz])"
          + "|d[ejkmoz]"
          + "|(?:edu|e[cegrstu])"
          + "|f[ijkmor]"
          + "|(?:gov|g[abdefghilmnpqrstuwy])"
          + "|h[kmnrtu]"
          + "|(?:info|int|i[delmnoqrst])"
          + "|(?:jobs|j[emop])"
          + "|k[eghimnrwyz]"
          + "|l[abcikrstuvy]"
          + "|(?:mil|mobi|museum|m[acdghklmnopqrstuvwxyz])"
          + "|(?:name|net|n[acefgilopruz])"
          + "|(?:org|om)"
          + "|(?:pro|p[aefghklmnrstwy])"
          + "|qa"
          + "|r[eouw]"
          + "|s[abcdeghijklmnortuvyz]"
          + "|(?:tel|travel|t[cdfghjklmnoprtvwz])"
          + "|u[agkmsyz]"
          + "|v[aceginu]"
          + "|w[fs]"
          + "|y[etu]"
          + "|z[amw]))"
          + "|(?:(?:25[0-5]|2[0-4]"
          // or ip address
          + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(?:25[0-5]|2[0-4][0-9]"
          + "|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1]"
          + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
          + "|[1-9][0-9]|[0-9])))"
          + "|\\.\\.\\."
          + "(?:\\:\\d{1,5})?)"
          // plus option port number
          + "(\\/(?:(?:[a-zA-Z0-9\\;\\/\\?\\:\\@\\&\\=\\#\\~"
          // plus option query params
          + "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?"
          + "(?:\\b|$)"); // and finally, a word boundary or end of
  // input.  This is to stop foo.sure from
  // matching as foo.su

  public static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile("[a-zA-Z0-9\\+\\.\\_\\%\\-]{1,256}"
      + "\\@"
      + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}"
      + "("
      + "\\."
      + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}"
      + ")+");
}