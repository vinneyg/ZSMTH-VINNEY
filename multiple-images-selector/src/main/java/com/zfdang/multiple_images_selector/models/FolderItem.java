package com.zfdang.multiple_images_selector.models;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class FolderItem {
    public String name;
    public String path;
    public String coverImagePath;
    public ArrayList<ImageItem> mImages = new ArrayList<>();

    public FolderItem(String name, String path, String coverImagePath) {
        this.name = name;
        this.path = path;
        this.coverImagePath = coverImagePath;
    }

    public void addImageItem(ImageItem imageItem) {
        this.mImages.add(imageItem);
    }

    @SuppressLint("DefaultLocale")
    public String getNumOfImages() {
        return String.format("%d", mImages.size());
    }

    @NonNull
    @Override
    public String toString() {
        return "FolderItem{" +
                "coverImagePath='" + coverImagePath + '\'' +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", numOfImages=" + mImages.size() +
                '}';
    }
}
