package com.jeffern.movie;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.webkit.WebStorage;
import android.webkit.WebView;

import java.io.File;

public class UninstallReceiver extends BroadcastReceiver {
    
    private static final String TAG = "UninstallReceiver";
    private static final String PREFS_NAME = "JeffernMoviePrefs";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (Intent.ACTION_PACKAGE_REMOVED.equals(action) || 
            Intent.ACTION_PACKAGE_FULLY_REMOVED.equals(action)) {
            
            String packageName = intent.getData().getSchemeSpecificPart();
            
            // 检查是否是当前应用被卸载
            if (context.getPackageName().equals(packageName)) {
                Log.d(TAG, "App is being uninstalled, clearing cache...");
                clearAllCache(context);
            }
        }
    }
    
    private void clearAllCache(Context context) {
        try {
            // 清除SharedPreferences
            clearSharedPreferences(context);
            
            // 清除WebView缓存
            clearWebViewCache(context);
            
            // 清除应用缓存目录
            clearAppCache(context);
            
            Log.d(TAG, "Cache cleared successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing cache: " + e.getMessage());
        }
    }
    
    private void clearSharedPreferences(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
            
            // 删除SharedPreferences文件
            File prefsFile = new File(context.getApplicationInfo().dataDir + "/shared_prefs/" + PREFS_NAME + ".xml");
            if (prefsFile.exists()) {
                prefsFile.delete();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing SharedPreferences: " + e.getMessage());
        }
    }
    
    private void clearWebViewCache(Context context) {
        try {
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
                        if (dbFile.getName().startsWith("webview")) {
                            dbFile.delete();
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing WebView cache: " + e.getMessage());
        }
    }
    
    private void clearAppCache(Context context) {
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
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing app cache: " + e.getMessage());
        }
    }
    
    private boolean deleteDirectory(File directory) {
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
}
