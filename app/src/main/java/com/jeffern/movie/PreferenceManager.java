package com.jeffern.movie;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class PreferenceManager {
    
    private static final String TAG = "PreferenceManager";
    private static final String PREFS_NAME = "JeffernMoviePrefs";
    
    // 配置键名
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_SERVICE_TYPE = "service_type";
    private static final String KEY_URL = "url";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_LAST_VISIT_TIME = "last_visit_time";
    private static final String KEY_AUTO_LOGIN_ENABLED = "auto_login_enabled";
    private static final String KEY_USER_AGENT_TYPE = "user_agent_type";
    
    // 服务类型常量
    public static final String SERVICE_EMBY = "emby";
    
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    
    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }
    
    /**
     * 检查是否首次启动
     */
    public boolean isFirstLaunch() {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true);
    }
    
    /**
     * 设置首次启动标记
     */
    public void setFirstLaunch(boolean isFirst) {
        editor.putBoolean(KEY_FIRST_LAUNCH, isFirst).apply();
        Log.d(TAG, "First launch set to: " + isFirst);
    }
    
    /**
     * 获取服务类型
     */
    public String getServiceType() {
        return prefs.getString(KEY_SERVICE_TYPE, "");
    }
    
    /**
     * 设置服务类型
     */
    public void setServiceType(String serviceType) {
        editor.putString(KEY_SERVICE_TYPE, serviceType).apply();
        Log.d(TAG, "Service type set to: " + serviceType);
    }
    
    /**
     * 获取网址
     */
    public String getUrl() {
        return prefs.getString(KEY_URL, "");
    }
    
    /**
     * 设置网址
     */
    public void setUrl(String url) {
        editor.putString(KEY_URL, url).apply();
        Log.d(TAG, "URL set to: " + url);
    }
    
    /**
     * 获取用户名
     */
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }
    
    /**
     * 设置用户名
     */
    public void setUsername(String username) {
        editor.putString(KEY_USERNAME, username).apply();
        Log.d(TAG, "Username set");
    }
    
    /**
     * 获取密码
     */
    public String getPassword() {
        return prefs.getString(KEY_PASSWORD, "");
    }
    
    /**
     * 设置密码
     */
    public void setPassword(String password) {
        editor.putString(KEY_PASSWORD, password).apply();
        Log.d(TAG, "Password set");
    }
    
    /**
     * 获取上次访问时间
     */
    public long getLastVisitTime() {
        return prefs.getLong(KEY_LAST_VISIT_TIME, 0);
    }
    
    /**
     * 设置上次访问时间
     */
    public void setLastVisitTime(long time) {
        editor.putLong(KEY_LAST_VISIT_TIME, time).apply();
    }
    
    /**
     * 更新上次访问时间为当前时间
     */
    public void updateLastVisitTime() {
        setLastVisitTime(System.currentTimeMillis());
    }
    
    /**
     * 检查是否启用自动登录
     */
    public boolean isAutoLoginEnabled() {
        return prefs.getBoolean(KEY_AUTO_LOGIN_ENABLED, true);
    }
    
    /**
     * 设置自动登录开关
     */
    public void setAutoLoginEnabled(boolean enabled) {
        editor.putBoolean(KEY_AUTO_LOGIN_ENABLED, enabled).apply();
        Log.d(TAG, "Auto login enabled: " + enabled);
    }

    /**
     * 获取User-Agent类型
     */
    public String getUserAgentType() {
        return prefs.getString(KEY_USER_AGENT_TYPE, "auto");
    }

    /**
     * 设置User-Agent类型
     */
    public void setUserAgentType(String type) {
        editor.putString(KEY_USER_AGENT_TYPE, type).apply();
        Log.d(TAG, "User-Agent type set to: " + type);
    }
    

    
    /**
     * 保存Emby配置
     */
    public void saveEmbyConfig(String url, String username, String password) {
        // 确保URL格式正确
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        
        editor.putBoolean(KEY_FIRST_LAUNCH, false)
              .putString(KEY_SERVICE_TYPE, SERVICE_EMBY)
              .putString(KEY_URL, url)
              .putString(KEY_USERNAME, username)
              .putString(KEY_PASSWORD, password)
              .apply();
        updateLastVisitTime();
        Log.d(TAG, "Emby config saved");
    }
    
    /**
     * 检查是否已配置
     */
    public boolean isConfigured() {
        return !isFirstLaunch() && !getUrl().isEmpty();
    }
    
    /**
     * 检查是否是Emby服务
     */
    public boolean isEmbyService() {
        return SERVICE_EMBY.equals(getServiceType());
    }
    

    
    /**
     * 获取Emby登录凭据（不使用默认值）
     */
    public String[] getEmbyCredentials() {
        String username = getUsername();
        String password = getPassword();

        return new String[]{username, password};
    }
    
    /**
     * 清除所有配置
     */
    public void clearAllConfig() {
        editor.clear().apply();
        Log.d(TAG, "All config cleared");
    }
    
    /**
     * 重置为首次启动状态
     */
    public void resetToFirstLaunch() {
        clearAllConfig();
        setFirstLaunch(true);
        Log.d(TAG, "Reset to first launch state");
    }
    
    /**
     * 获取配置摘要信息
     */
    public String getConfigSummary() {
        if (!isConfigured()) {
            return "未配置";
        }
        
        String serviceType = getServiceType();
        String url = getUrl();
        
        if (SERVICE_EMBY.equals(serviceType)) {
            String username = getUsername();
            return "Emby - " + url + (username.isEmpty() ? "" : " (" + username + ")");
        } else {
            return "未知服务 - " + url;
        }
    }
}
