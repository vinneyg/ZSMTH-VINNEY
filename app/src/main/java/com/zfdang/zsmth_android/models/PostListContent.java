package com.zfdang.zsmth_android.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class PostListContent {

  public static final List<Post> POSTS = new ArrayList<>();

  public static final Map<String, Post> POST_MAP = new HashMap<>();

  public static void addItem(Post item) {
    POSTS.add(item);
    POST_MAP.put(item.getPostID(), item);
  }

  public static void clear() {
    POSTS.clear();
    POST_MAP.clear();
  }

  public static void InsertItem(int index,Post item) {
    POSTS.add(index,item);
    POST_MAP.put(item.getPostID(), item);
  }

}
