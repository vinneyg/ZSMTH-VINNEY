package com.zfdang.zsmth_android.fresco;

import android.net.Uri;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import android.os.Handler;
import android.os.Looper;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.cache.DefaultCacheKeyFactory;
import com.facebook.cache.common.CacheKey;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSubscriber;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FrescoUtils {

    private static final String TAG = "FrescoUtils";

    /**
     * Get cached image file from disk (optimized for custom cache configuration)
     * @param context Application context
     * @param loadUri Image URI
     * @return Cached file or null
     */
    public static File getCachedImageOnDisk(Context context, Uri loadUri) {
        Log.d(TAG, "=== getCachedImageOnDisk called with URI: " + loadUri + " ===");

        if (context == null || loadUri == null) {
            //Log.e(TAG, "Context or URI is null");
            return null;
        }

        try {
            // Strategy 1: Search in custom configured cache directory
            File cachedFile = searchInCustomCacheDirectory(context, loadUri);
            if (cachedFile != null && cachedFile.exists() && isActualImageCacheFile(cachedFile)) {
                //Log.d(TAG, "✓ Found cached file: " + cachedFile.getAbsolutePath());
                return cachedFile;
            }

            // Strategy 1.5: Try Fresco cache inspection
            cachedFile = tryFrescoCacheInspection(context, loadUri);
            if (cachedFile != null && cachedFile.exists() && isActualImageCacheFile(cachedFile)) {
                //Log.d(TAG, "✓ Found cached file via Fresco inspection: " + cachedFile.getAbsolutePath());
                return cachedFile;
            }

            // Strategy 2: Try prefetch and access
            cachedFile = tryPrefetchAndAccess(context, loadUri);
            if (cachedFile != null && cachedFile.exists() && isActualImageCacheFile(cachedFile)) {
                //Log.d(TAG, "✓ Found cached file via prefetch: " + cachedFile.getAbsolutePath());
                return cachedFile;
            }

            // Strategy 3: Direct download as last resort (async)
            Log.d(TAG, "Trying direct download as last resort");
            return null; // Return null here and handle download asynchronously

        } catch (Exception e) {
            Log.e(TAG, "Error in getCachedImageOnDisk: " + e.getMessage(), e);
        }

        return null;
    }

    /**
     * Search specifically in the configured Fresco cache directory
     */
    private static File searchInCustomCacheDirectory(Context context, Uri loadUri) {
        try {
            // Create image request and cache key
            ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(loadUri).build();
            CacheKey cacheKey = DefaultCacheKeyFactory.getInstance()
                    .getEncodedCacheKey(imageRequest, context);

            // Use the exact cache directory from your configuration
            File cacheDir = new File(context.getCacheDir(), "fresco_cache");

            if (cacheDir.exists()) {
                //Log.d(TAG, "Searching in configured cache directory: " + cacheDir.getAbsolutePath());
                return findCachedFile(cacheDir, cacheKey, loadUri);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error searching in custom cache directory: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Try using Fresco's built-in cache inspection
     */
    private static File tryFrescoCacheInspection(Context context, Uri loadUri) {
        try {
            ImagePipeline imagePipeline = Fresco.getImagePipeline();

            // Check if image is available in any cache
            if (imagePipeline.isInBitmapMemoryCache(loadUri) ||
                    imagePipeline.isInDiskCacheSync(loadUri)) {
                //Log.d(TAG, "Image confirmed in cache, trying alternative retrieval");
                // Try a more comprehensive search
                return searchComprehensiveCache(context, loadUri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in Fresco cache inspection: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Comprehensive search through all subdirectories
     */
    private static File searchComprehensiveCache(Context context, Uri loadUri) {
        try {
            File cacheDir = new File(context.getCacheDir(), "fresco_cache");
            if (cacheDir.exists()) {
                return searchDirectoryRecursively(cacheDir, loadUri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in comprehensive cache search: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Recursively search directories for .cnt files
     */
    private static File searchDirectoryRecursively(File dir, Uri loadUri) {
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }

        File[] files = dir.listFiles();
        if (files != null) {
            // First check for .cnt files in current directory
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".cnt") && file.length() > 100) {
                    //Log.d(TAG, "Found potential cache file: " + file.getAbsolutePath() + " (size: " + file.length() + ")");
                    return file;
                }
            }

            // Then recursively check subdirectories
            for (File file : files) {
                if (file.isDirectory()) {
                    File result = searchDirectoryRecursively(file, loadUri);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Check if file is a valid image file
     */
    private static boolean isValidImageFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }

        String name = file.getName().toLowerCase();
        String path = file.getAbsolutePath().toLowerCase();

        // Exclude non-image cache directories
        if (path.contains("/webview/") ||
                path.contains("/http cache/") ||
                path.contains("/code cache/") ||
                name.endsWith(".js") ||
                name.endsWith(".css") ||
                name.endsWith(".html") ||
                name.endsWith(".txt")) {
            return false;
        }

        // Check for actual image file extensions or Fresco cache files
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
                name.endsWith(".gif") || name.endsWith(".webp") || name.endsWith(".bmp") ||
                name.endsWith(".cnt") || name.matches("^[a-f0-9]+.*"); // Fresco cache files often start with hex chars
    }

    /**
     * Enhanced cache file validation to ensure it's actually an image
     */
    private static boolean isActualImageCacheFile(File file) {
        if (!isValidImageFile(file)) {
            return false;
        }

        // Additional checks for file size and content
        // Image files should typically be larger than a few KB
        // Less than 100 bytes is likely not an image
        return file.length() >= 100;
    }

    /**
     * Strategy 1: Optimized cache search method for custom configuration
     */
    private static File tryCurrentCacheSearch(Context context, Uri loadUri) {
        // For custom configuration, delegate to the optimized search method
        return searchInCustomCacheDirectory(context, loadUri);
    }

    /**
     * Strategy 2: Prefetch and access
     */
    private static File tryPrefetchAndAccess(Context context, Uri loadUri) {
        try {
            ImagePipeline imagePipeline = Fresco.getImagePipeline();

            // Check if already in cache
            if (isInDiskCacheSync(loadUri)) {
                // Already in cache, try comprehensive search
                File result = searchComprehensiveCache(context, loadUri);
                if (result != null) {
                    return result;
                }
                // Fallback to current search method
                return searchInCustomCacheDirectory(context, loadUri);
            }

            // Not in cache, prefetch it
            ImageRequest request = ImageRequestBuilder.newBuilderWithSource(loadUri)
                    .setCacheChoice(ImageRequest.CacheChoice.DEFAULT)
                    .setRequestPriority(Priority.HIGH)
                    .build();

            DataSource<Void> prefetchSource = imagePipeline.prefetchToDiskCache(request, context);

            // Wait for prefetch to complete (with timeout)
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Boolean> prefetchSuccess = new AtomicReference<>(false);

            prefetchSource.subscribe(new DataSubscriber<Void>() {
                @Override
                public void onNewResult(@NonNull DataSource<Void> dataSource) {
                    prefetchSuccess.set(true);
                    latch.countDown();
                }

                @Override
                public void onFailure(@NonNull DataSource<Void> dataSource) {
                    Log.e(TAG, "Prefetch failed: " + dataSource.getFailureCause());
                    latch.countDown();
                }

                @Override
                public void onCancellation(@NonNull DataSource<Void> dataSource) {
                    latch.countDown();
                }

                @Override
                public void onProgressUpdate(@NonNull DataSource<Void> dataSource) {
                    // Ignore progress updates
                }
            }, UiThreadImmediateExecutorService.getInstance());

            // Wait up to 5 seconds for prefetch to complete
            if (latch.await(5, TimeUnit.SECONDS)) {
                if (prefetchSuccess.get()) {
                    // Prefetch completed successfully, try to access the cached file
                    File result = searchComprehensiveCache(context, loadUri);
                    if (result != null) {
                        return result;
                    }
                    // Fallback to current search method
                    return searchInCustomCacheDirectory(context, loadUri);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in tryPrefetchAndAccess: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Async direct download to avoid NetworkOnMainThreadException
     */
    public static void downloadImageDirectlyAsync(Uri uri, Context context, DownloadCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        try {
            executor.execute(() -> {
                File result = tryDirectDownload(uri, context);
                handler.post(() -> {
                    if (callback != null) {
                        if (result != null) {
                            callback.onSuccess(result);
                        } else {
                            callback.onError("Download failed");
                        }
                    }
                });
            });
        } catch (Exception e) {
            Log.e(TAG, "Error executing download task: " + e.getMessage(), e);
            if (callback != null) {
                callback.onError("Execution failed: " + e.getMessage());
            }
        } finally {
            executor.shutdown(); // Ensure executor is shut down
        }
    }

    /**
     * Strategy 3: Direct download (should only be called from background thread)
     */
    private static File tryDirectDownload(Uri uri, Context context) {
        try {
            URL url = new URL(uri.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set appropriate headers
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String fileName = getFileNameFromUri(uri.toString());
                if (fileName.isEmpty()) {
                    fileName = "downloaded_" + System.currentTimeMillis();
                }

                // Try to determine extension from content type
                String contentType = connection.getContentType();
                if (contentType != null && contentType.startsWith("image/")) {
                    if (contentType.contains("jpeg") || contentType.contains("jpg")) {
                        fileName = fileName.replaceAll("\\.[^.]*$", "") + ".jpg";
                    } else if (contentType.contains("png")) {
                        fileName = fileName.replaceAll("\\.[^.]*$", "") + ".png";
                    } else if (contentType.contains("gif")) {
                        fileName = fileName.replaceAll("\\.[^.]*$", "") + ".gif";
                    }
                }

                File outputFile = new File(context.getCacheDir(), fileName);
                try (InputStream inputStream = connection.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(outputFile)) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    //Log.d(TAG, "Direct download successful: " + outputFile.getAbsolutePath());
                    return outputFile;
                }
            } else {
                Log.e(TAG, "Direct download failed with HTTP " + connection.getResponseCode());
            }
        } catch (Exception e) {
            Log.e(TAG, "Direct download failed: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Check if image is in Fresco disk cache (sync)
     * @param uri Image URI
     * @return Whether in cache
     */
    public static boolean isInDiskCacheSync(Uri uri) {
        try {
            ImagePipeline imagePipeline = Fresco.getImagePipeline();
            return imagePipeline.isInDiskCacheSync(uri);
        } catch (Exception e) {
            Log.e(TAG, "Error checking disk cache: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get Fresco cache directories (optimized for custom configuration)
     */
    private static File[] getFrescoCacheDirs(Context context) {
        // Only search in the configured directory
        return new File[] {
                new File(context.getCacheDir(), "fresco_cache")
        };
    }

    /**
     * Find cached file in specified directory (enhanced)
     */
    private static File findCachedFile(File dir, CacheKey cacheKey, Uri originalUri) {
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }

        /*
        // Add debug logging to see what files are actually present
        File[] allFiles = dir.listFiles();
        if (allFiles != null) {
            Log.d(TAG, "Cache directory contains " + allFiles.length + " items:");
            for (File f : allFiles) {
                Log.d(TAG, "  File: " + f.getName() + " (size: " + f.length() + ", isDir: " + f.isDirectory() + ")");
            }
        }
        */

        String keyHash = String.valueOf(cacheKey.hashCode());
        String uriString = originalUri.toString();
        String fileNameFromUri = getFileNameFromUri(uriString);

        //Log.d(TAG, "Looking for cache key hash: " + keyHash);
        //Log.d(TAG, "Looking for URI filename: " + fileNameFromUri);

        // Use recursive search for .cnt files
        return searchDirectoryRecursively(dir, originalUri);
    }

    /**
     * Extract filename from URI
     */
    private static String getFileNameFromUri(String uri) {
        if (uri == null) return "";

        // Handle query parameters and fragments
        int queryIndex = uri.indexOf('?');
        if (queryIndex > 0) {
            uri = uri.substring(0, queryIndex);
        }

        int fragmentIndex = uri.indexOf('#');
        if (fragmentIndex > 0) {
            uri = uri.substring(0, fragmentIndex);
        }

        int lastSlash = uri.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < uri.length() - 1) {
            return uri.substring(lastSlash + 1);
        }
        return uri;
    }

    public interface DownloadCallback {
        void onSuccess(File file);
        void onError(String error);
    }
}
