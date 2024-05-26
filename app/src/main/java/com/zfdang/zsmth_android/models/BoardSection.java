package com.zfdang.zsmth_android.models;

import androidx.annotation.NonNull;

/**
 * Created by zfdang on 2016-3-24.
 */
public class BoardSection {
  public String sectionURL;
  public String sectionName;
  public String parentName;

  public String getSectionPath() {
    if (parentName == null || parentName.isEmpty()) {
      return sectionName;
    } else {
      return String.format("%s | %s", parentName, sectionName);
    }
  }

  @NonNull
  @Override
  public String toString() {
    return "BoardSection{" +
            "sectionURL='" + sectionURL + '\'' +
            ", sectionName='" + sectionName + '\'' +
            ", parentName='" + parentName + '\'' +
            '}';
  }
}
