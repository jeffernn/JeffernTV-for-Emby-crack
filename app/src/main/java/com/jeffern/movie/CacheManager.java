package com.jeffern.movie;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.webkit.WebStorage;
import android.webkit.WebView;

import java.io.File;

public class CacheManager {
    
    private static final String TAG = "CacheManager";
    private static final String PREFS_NAME = "JeffernMoviePrefs";
    
    /**
     * 清除所有缓存数据
     */
    public static void clearAllCache(Context context) {
        try {
            clearWebViewCache(context);
            clearAppCache(context);
            Log.d(TAG, "All cache cleared successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing cache: " + e.getMessage());
        }
    }
    
    /**
     * 清除WebView相关缓存
     */
    public static void clearWebViewCache(Context context) {
        try {
            // 在主线程中清除WebView缓存
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                WebView webView = new WebView(context);
                webView.clearCache(true);
                webView.clearHistory();
                webView.clearFormData();
                webView.destroy();
            }
            
            // 清除WebView数据存储
            WebStorage.getInstance().deleteAllData();
            
            // 清除WebView缓存目录
            File webViewCacheDir = new File(context.getApplicationInfo().dataDir + "/app_webview");
            if (webViewCacheDir.exists()) {
                deleteDirectory(webViewCacheDir);
            }
            
            // 清除WebView数据库
            File webViewDbDir = new File(context.getApplicationInfo().dataDir + "/databases");
            if (webViewDbDir.exists()) {
                File[] dbFiles = webViewDbDir.listFiles();
                if (dbFiles != null) {
                    for (File dbFile : dbFiles) {
                        if (dbFile.getName().startsWith("webview") || 
                            dbFile.getName().contains("WebView")) {
                            dbFile.delete();
                        }
                    }
                }
            }
            
            Log.d(TAG, "WebView cache cleared");
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing WebView cache: " + e.getMessage());
        }
    }
    
    /**
     * 清除应用缓存
     */
    public static void clearAppCache(Context context) {
        try {
            // 清除应用缓存目录
            File cacheDir = context.getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                deleteDirectory(cacheDir);
            }
            
            // 清除外部缓存目录
            File externalCacheDir = context.getExternalCacheDir();
            if (externalCacheDir != null && externalCacheDir.exists()) {
                deleteDirectory(externalCacheDir);
            }
            
            Log.d(TAG, "App cache cleared");
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing app cache: " + e.getMessage());
        }
    }
    
    /**
     * 重置应用设置（用于测试或重新配置）
     */
    public static void resetAppSettings(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
            Log.d(TAG, "App settings reset");
        } catch (Exception e) {
            Log.e(TAG, "Error resetting app settings: " + e.getMessage());
        }
    }
    
    /**
     * 获取缓存大小
     */
    public static long getCacheSize(Context context) {
        long size = 0;
        try {
            File cacheDir = context.getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                size += getDirectorySize(cacheDir);
            }
            
            File externalCacheDir = context.getExternalCacheDir();
            if (externalCacheDir != null && externalCacheDir.exists()) {
                size += getDirectorySize(externalCacheDir);
            }
            
            File webViewCacheDir = new File(context.getApplicationInfo().dataDir + "/app_webview");
            if (webViewCacheDir.exists()) {
                size += getDirectorySize(webViewCacheDir);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating cache size: " + e.getMessage());
        }
        return size;
    }
    
    /**
     * 递归删除目录
     */
    private static boolean deleteDirectory(File directory) {
        if (directory != null && directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            return directory.delete();
        }
        return false;
    }
    
    /**
     * 计算目录大小
     */
    private static long getDirectorySize(File directory) {
        long size = 0;
        if (directory != null && directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += getDirectorySize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        }
        return size;
    }
    
    /**
     * 格式化文件大小显示
     */
    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
