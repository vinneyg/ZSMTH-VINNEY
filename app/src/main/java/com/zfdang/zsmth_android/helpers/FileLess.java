package com.zfdang.zsmth_android.helpers;

import java.io.File;
import java.util.Objects;
/*
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
*/

/**
 * 文件操作相关的工具类
 */
public final class FileLess {

  /*
    读取文件为字符串

    @param file 文件
   * @return 文件内容字符串
  */
  /*
  public static String $read(File file) throws IOException {
    String text;
    try (InputStream is = Files.newInputStream(file.toPath())) {
      text = $read(is);
    }
    return text;
  }
  */

  /*
    读取输入流为字符串,最常见的是网络请求

    @param is 输入流
   * @return 输入流内容字符串
   */
  /*
  public static String $read(InputStream is) throws IOException {
    StringBuilder strbuffer = new StringBuilder();
    String line;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      while ((line = reader.readLine()) != null) {
        strbuffer.append(line).append("\r\n");
      }
    }
    return strbuffer.toString();
  }
  */

  /*
    把字符串写入到文件中

    @param file 被写入的目标文件
   * @param str 要写入的字符串内容
   */
  /*
  public static void $write(File file, String str) throws IOException {
    try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
      out.write(str.getBytes());
    }
  }
  */

  /*
    unzip zip file to dest folder
   */
  /*
  public static void $unzip(String zipFilePath, String destPath) throws IOException {
    // check or create dest folder
    File destFile = new File(destPath);
    if (!destFile.exists()) {
      destFile.mkdirs();
    }


    // start unzip
    ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath));
    ZipEntry zipEntry;
    String zipEntryName;
    while ((zipEntry = zipInputStream.getNextEntry()) != null) {
      zipEntryName = zipEntry.getName();
      if (zipEntry.isDirectory()) {
        File folder = new File(destPath + File.separator + zipEntryName);
        folder.mkdirs();
      } else {
        File file = new File(destPath + File.separator + zipEntryName);
        if (!Objects.requireNonNull(file.getParentFile()).exists()) {
          file.getParentFile().mkdirs();
        }
        file.createNewFile();
        FileOutputStream out = new FileOutputStream(file);
        int len;
        byte[] buffer = new byte[1024];
        while ((len = zipInputStream.read(buffer)) > 0) {
          out.write(buffer, 0, len);
          out.flush();
        }
        out.close();
      }
    }
    zipInputStream.close();
  }
  */

  /**
   * 删除文件或者文件夹，默认保留根目录
   */
  public static void $del(File directory) {
    $del(directory, false);
  }

  /**
   * 删除文件或者文件夹
   */
  public static void $del(File directory, boolean keepRoot) {
    if (directory != null && directory.exists()) {
      if (directory.isDirectory()) {
        for (File subDirectory : Objects.requireNonNull(directory.listFiles())) {
          $del(subDirectory, false);
        }
      }

      if (!keepRoot) {
        directory.delete();
      }
    }
  }
}