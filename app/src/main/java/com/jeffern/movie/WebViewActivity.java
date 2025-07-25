package com.jeffern.movie;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class WebViewActivity extends AppCompatActivity {
    
    private static final String TAG = "WebViewActivity";
    
    // Android设备User-Agent (让网站识别为Android设备并使用Android界面)
    private static final String ANDROID_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; SHIELD Android TV) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Version/4.0 Chrome/120.0.0.0 Mobile Safari/537.36 " +
        "AndroidApp JeffernTV/1.0";

    // Android手机User-Agent（强制手机界面）
    private static final String ANDROID_PHONE_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; SM-G991B) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Version/4.0 Chrome/120.0.0.0 Mobile Safari/537.36";

    // Android平板User-Agent
    private static final String ANDROID_TABLET_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; SM-T870) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Version/4.0 Chrome/120.0.0.0 Safari/537.36";
    
    private WebView webView;
    private ProgressBar progressBar;
    private TextView tvLoading;
    private PreferenceManager prefManager;
    private static boolean embyLoginInjectedGlobal = false;  // 应用级别跟踪Emby登录是否已注入
    private static boolean embyCloseButtonInjectedGlobal = false;  // 应用级别跟踪关闭按钮是否已注入
    
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        
        prefManager = new PreferenceManager(this);
        
        initViews();
        setupWebView();
        loadWebsite();
    }
    
    private void initViews() {
        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progress_bar);
        tvLoading = findViewById(R.id.tv_loading);
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        
        // 启用JavaScript
        settings.setJavaScriptEnabled(true);
        
        // 强制设置为Android手机User-Agent（确保显示手机界面）
        settings.setUserAgentString(ANDROID_PHONE_USER_AGENT);
        Log.d(TAG, "User-Agent forced to Android Phone: " + ANDROID_PHONE_USER_AGENT);

        // 再次确认UA设置（防止被覆盖）
        String currentUA = settings.getUserAgentString();
        if (!currentUA.contains("Mobile")) {
            settings.setUserAgentString(ANDROID_PHONE_USER_AGENT);
            Log.w(TAG, "UA was overridden, forcing again: " + ANDROID_PHONE_USER_AGENT);
        }
        
        // 启用DOM存储
        settings.setDomStorageEnabled(true);
        
        // 启用数据库存储
        settings.setDatabaseEnabled(true);
        
        // 应用缓存在 API 33+ 中已被移除，使用其他缓存策略
        
        // 设置缓存模式
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // 支持缩放
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        
        // 强制手机视口（确保显示手机界面）
        settings.setUseWideViewPort(false);  // 关闭宽视口，强制手机布局
        settings.setLoadWithOverviewMode(false);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);  // 单列布局
        settings.setDefaultZoom(WebSettings.ZoomDensity.FAR);  // 设置远距离缩放，显示更多内容

        // 强制手机屏幕密度
        settings.setDefaultTextEncodingName("utf-8");
        settings.setSupportZoom(false);  // 禁用缩放
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        
        // 允许混合内容
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // 启用网络相关功能
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);

        // 设置更宽松的缓存策略
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        // 启用地理位置（某些网站需要）
        settings.setGeolocationEnabled(true);

        // 设置更多网络相关配置
        settings.setBlockNetworkImage(false);
        settings.setBlockNetworkLoads(false);
        settings.setLoadsImagesAutomatically(true);

        // 启用焦点处理
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus();

        // 设置WebViewClient
        webView.setWebViewClient(new CustomWebViewClient());
        
        // 设置WebChromeClient
        webView.setWebChromeClient(new CustomWebChromeClient());
        
        // 启用焦点
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
    }
    
    private void loadWebsite() {
        String url = prefManager.getUrl();
        if (!url.isEmpty()) {
            // 检查网络连接
            if (isNetworkAvailable()) {
                Log.d(TAG, "Loading URL: " + url);
                // 先进行DNS预解析
                performDNSLookup(url);
            } else {
                Toast.makeText(this, "网络连接不可用，请检查网络设置", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Network not available");
            }
        } else {
            Toast.makeText(this, R.string.error_invalid_url, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void performDNSLookup(String url) {
        // 直接加载URL，不进行DNS检测
        Log.d(TAG, "Loading URL directly: " + url);
        loadUrlDirectly(url);
    }



    private String extractDomain(String url) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            java.net.URL urlObj = new java.net.URL(url);
            return urlObj.getHost();
        } catch (Exception e) {
            Log.e(TAG, "Error extracting domain from URL: " + url, e);
            return null;
        }
    }

    private void loadUrlDirectly(String url) {
        webView.loadUrl(url);
        prefManager.updateLastVisitTime();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
            (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    /**
     * 根据用户设置和设备类型获取最佳的User-Agent
     */
    private String getOptimalUserAgent() {
        String userAgentType = prefManager.getUserAgentType();

        // 如果用户有特定选择，使用用户选择
        switch (userAgentType) {
            case "android_tv":
                Log.d(TAG, "Using user-selected Android TV User-Agent");
                return ANDROID_USER_AGENT;
            case "android_phone":
                Log.d(TAG, "Using user-selected Android Phone User-Agent");
                return ANDROID_PHONE_USER_AGENT;
            case "android_tablet":
                Log.d(TAG, "Using user-selected Android Tablet User-Agent");
                return ANDROID_TABLET_USER_AGENT;
            case "auto":
            default:
                // 自动检测设备类型
                return getAutoDetectedUserAgent();
        }
    }

    /**
     * 自动检测设备类型并返回相应的User-Agent
     */
    private String getAutoDetectedUserAgent() {
        try {
            // 检查是否是Android TV
            if (getPackageManager().hasSystemFeature("android.software.leanback")) {
                Log.d(TAG, "Auto-detected Android TV, using Android TV User-Agent");
                return ANDROID_USER_AGENT;
            }

            // 检查屏幕尺寸来判断是手机还是平板
            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);

            float screenInches = (float) Math.sqrt(
                Math.pow(metrics.widthPixels / metrics.xdpi, 2) +
                Math.pow(metrics.heightPixels / metrics.ydpi, 2)
            );

            if (screenInches >= 7.0) {
                Log.d(TAG, "Auto-detected tablet (" + screenInches + " inches), using tablet User-Agent");
                return ANDROID_TABLET_USER_AGENT;
            } else {
                Log.d(TAG, "Auto-detected phone (" + screenInches + " inches), using phone User-Agent");
                return ANDROID_PHONE_USER_AGENT;
            }

        } catch (Exception e) {
            Log.w(TAG, "Error auto-detecting device type, using default Android User-Agent", e);
            return ANDROID_USER_AGENT;
        }
    }
    
    private class CustomWebViewClient extends WebViewClient {
        
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            showLoading(true);

            // 使用应用级别标记，不重置任何标记
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            showLoading(false);

            // 强制手机界面样式
            injectMobileInterfaceScript();

            // 注入焦点处理JavaScript
            injectFocusHandlingScript();

            // 检查是否需要自动登录Emby（应用级别只注入一次）
            Log.d(TAG, "Login check: isEmbyService=" + prefManager.isEmbyService() +
                      ", embyLoginInjectedGlobal=" + embyLoginInjectedGlobal);
            if (prefManager.isEmbyService() && !embyLoginInjectedGlobal) {
                Log.d(TAG, "Scheduling Emby auto login - using original working method");
                // 使用原来可以工作的方法，延迟2秒执行
                webView.postDelayed(() -> {
                    Log.d(TAG, "Executing original auto login method");
                    injectEmbyAutoLogin(url);
                    embyLoginInjectedGlobal = true;  // 应用级别标记已注入
                    Log.d(TAG, "Emby login injected globally, will not inject again in this app session");
                }, 2000);
            } else {
                Log.d(TAG, "Skipping login injection - already executed or not Emby service");
            }

            // 自动点击Emby关闭按钮（应用级别只注入一次，简化执行）
            Log.d(TAG, "Close button check: isEmbyService=" + prefManager.isEmbyService() +
                      ", embyCloseButtonInjectedGlobal=" + embyCloseButtonInjectedGlobal);
            if (prefManager.isEmbyService() && !embyCloseButtonInjectedGlobal) {
                Log.d(TAG, "Scheduling Emby close button click - simplified approach");
                // 延迟6秒执行，确保登录完成且页面加载
                webView.postDelayed(() -> {
                    Log.d(TAG, "Executing simplified close button click");
                    autoClickEmbyCloseButtonSimple();
                    embyCloseButtonInjectedGlobal = true;  // 应用级别标记已注入
                    Log.d(TAG, "Emby close button executed globally, will not execute again in this app session");
                }, 6000);
            } else {
                Log.d(TAG, "Skipping close button execution - already executed or not Emby service");
            }
        }
        
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            // 在当前WebView中加载所有链接
            return false;
        }
        
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            Log.e(TAG, "WebView error: " + errorCode + " - " + description + " for URL: " + failingUrl);

            String errorMessage;
            switch (errorCode) {
                case ERROR_HOST_LOOKUP:
                    errorMessage = "无法解析域名，请检查网络连接或DNS设置";
                    break;
                case ERROR_CONNECT:
                    errorMessage = "连接失败，请检查网络连接";
                    break;
                case ERROR_TIMEOUT:
                    errorMessage = "连接超时，请稍后重试";
                    break;
                case ERROR_UNKNOWN:
                    errorMessage = "未知网络错误，请检查网址是否正确";
                    break;
                default:
                    errorMessage = "网络错误: " + description;
                    break;
            }

            Toast.makeText(WebViewActivity.this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }
    
    private class CustomWebChromeClient extends WebChromeClient {
        
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            progressBar.setProgress(newProgress);
        }
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        tvLoading.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    
    private void injectEmbyAutoLogin(String currentUrl) {
        String[] credentials = prefManager.getEmbyCredentials();
        String embyUser = credentials[0];
        String embyPass = credentials[1];

        // 如果用户名或密码为空，则不执行自动登录
        if (embyUser.isEmpty() || embyPass.isEmpty()) {
            Log.d(TAG, "Emby credentials not set, skipping auto login");
            return;
        }
        
        String js = String.format(
            "console.log('JeffernTV: Starting auto login');\n" +
            "var tryCount=0;var timer=setInterval(function(){\n" +
            "var userInput = document.querySelector('input[type=\"text\"],input[name*=\"user\"],input[placeholder*=\"用\"]');\n" +
            "var passInput = document.querySelector('input[type=\"password\"],input[name*=\"pass\"],input[placeholder*=\"密\"]');\n" +
            "var form = userInput ? userInput.form : null;\n" +
            "if(userInput&&passInput){\n" +
            "console.log('JeffernTV: Found login inputs, filling credentials');\n" +
            "userInput.focus();\n" +
            "userInput.value='%s';\n" +
            "userInput.dispatchEvent(new Event('input', {bubbles:true}));\n" +
            "userInput.dispatchEvent(new Event('change', {bubbles:true}));\n" +
            "passInput.focus();\n" +
            "passInput.value='%s';\n" +
            "passInput.dispatchEvent(new Event('input', {bubbles:true}));\n" +
            "passInput.dispatchEvent(new Event('change', {bubbles:true}));\n" +
            "passInput.blur();\n" +
            "}\n" +
            "if(form&&userInput&&passInput){\n" +
            "console.log('JeffernTV: Submitting login form');\n" +
            "try{\n" +
            "form.dispatchEvent(new Event('submit', {bubbles:true,cancelable:true}));\n" +
            "form.requestSubmit ? form.requestSubmit() : form.submit();\n" +
            "}catch(e){form.submit();}\n" +
            "clearInterval(timer);\n" +
            "console.log('JeffernTV: Auto login completed');\n" +
            "}\n" +
            "if(++tryCount>60){clearInterval(timer);console.log('JeffernTV: Auto login timeout');}\n" +
            "}, 100);", embyUser, embyPass);
        
        // 使用loadUrl方式执行JavaScript（兼容所有版本）
        webView.loadUrl("javascript:" + js);
        Log.d(TAG, "Emby auto login script injected");
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 处理返回键
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        // 处理菜单键 - 显示设置选项
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showSettingsDialog();
            return true;
        }

        // 阻止方向键的默认滚动行为，只让SmartFocus处理
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP ||
            keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
            keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
            keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
            keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
            keyCode == KeyEvent.KEYCODE_ENTER) {

            // 确保WebView有焦点
            if (!webView.hasFocus()) {
                webView.requestFocus();
            }

            // 只让JavaScript的SmartFocus处理，不传递给WebView的默认处理
            // 这样可以防止页面滚动
            return true;  // 消费事件，不传递给WebView
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * 显示设置对话框
     */
    private void showSettingsDialog() {
        String[] options = {
            "更换User-Agent",
            "清除缓存",
            "重新加载页面",
            "返回主页"
        };

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("设置选项");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    showUserAgentDialog();
                    break;
                case 1:
                    clearWebViewCache();
                    break;
                case 2:
                    webView.reload();
                    break;
                case 3:
                    finish();
                    break;
            }
        });
        builder.show();
    }

    /**
     * 显示User-Agent选择对话框
     */
    private void showUserAgentDialog() {
        String[] userAgentOptions = {
            "Android TV (推荐)",
            "Android 手机",
            "Android 平板",
            "自动检测"
        };

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("选择User-Agent类型");
        builder.setMessage("不同的User-Agent会让网站显示不同的界面：");

        builder.setItems(userAgentOptions, (dialog, which) -> {
            String selectedUA;
            String uaType;

            switch (which) {
                case 0:
                    selectedUA = ANDROID_USER_AGENT;
                    uaType = "android_tv";
                    break;
                case 1:
                    selectedUA = ANDROID_PHONE_USER_AGENT;
                    uaType = "android_phone";
                    break;
                case 2:
                    selectedUA = ANDROID_TABLET_USER_AGENT;
                    uaType = "android_tablet";
                    break;
                case 3:
                default:
                    selectedUA = getOptimalUserAgent();
                    uaType = "auto";
                    break;
            }

            // 保存用户选择
            prefManager.setUserAgentType(uaType);

            // 应用新的User-Agent
            webView.getSettings().setUserAgentString(selectedUA);
            Log.d(TAG, "User-Agent changed to: " + selectedUA);

            // 重新加载页面
            webView.reload();

            Toast.makeText(this, "User-Agent已更改，正在重新加载页面", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 清除WebView缓存
     */
    private void clearWebViewCache() {
        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();
        Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show();
    }

    /**
     * 自动点击Emby关闭按钮 - 参考登录逻辑
     */
    private void autoClickEmbyCloseButton() {
        // 带全局标记的关闭按钮脚本，确保整个应用生命周期只执行一次
        String js =
            "if(window.jeffernTVCloseButtonExecuted) {" +
            "console.log('JeffernTV: Close button already executed in this app session, skipping');" +
            "return;" +
            "}" +
            "window.jeffernTVCloseButtonExecuted = true;" +
            "console.log('JeffernTV: Starting close button auto-click (will only execute once per app session)');" +

            "var tryCount=0;" +
            "var timer=setInterval(function(){" +
            "console.log('JeffernTV: Attempt ' + (tryCount + 1) + ' to find close button');" +

            // 查找关闭按钮 - 使用多种选择器
            "var closeBtn = document.querySelector('button.btnPinNavDrawer') || " +
            "document.querySelector('button[title=\"关闭\"]') || " +
            "document.querySelector('button[aria-label=\"关闭\"]') || " +
            "document.querySelector('button[is=\"paper-icon-button-light\"][title=\"关闭\"]') || " +
            "document.querySelector('button[class*=\"btnPinNavDrawer\"]') || " +
            "document.querySelector('.btnPinNavDrawer');" +

            "if(closeBtn){" +
            "console.log('JeffernTV: Found close button:', closeBtn);" +

            // 检查按钮是否可见
            "var rect = closeBtn.getBoundingClientRect();" +
            "var style = getComputedStyle(closeBtn);" +
            "console.log('JeffernTV: Button visible:', rect.width > 0 && rect.height > 0 && style.display !== 'none');" +

            "if(rect.width > 0 && rect.height > 0 && style.display !== 'none'){" +
            "try{" +
            "closeBtn.focus();" +
            "closeBtn.click();" +
            "closeBtn.dispatchEvent(new MouseEvent('click', {bubbles:true,cancelable:true}));" +
            "console.log('JeffernTV: Close button clicked successfully - will not execute again');" +
            "}catch(e){" +
            "console.log('JeffernTV: Click error:', e);" +
            "}" +
            "clearInterval(timer);" +
            "}" +
            "}" +

            // 超时处理 - 30次尝试后停止（3秒）
            "if(++tryCount>30){" +
            "console.log('JeffernTV: Close button auto-click timeout');" +
            "clearInterval(timer);" +
            "}" +
            "}, 100);";


        // 使用loadUrl方式执行JavaScript（兼容所有版本，参考登录逻辑）
        webView.loadUrl("javascript:" + js);
        Log.d(TAG, "Auto-click close button script injected");
    }

    /**
     * 等待页面完全加载后再点击关闭按钮
     */
    private void waitForPageFullyLoadedThenClickCloseButton() {
        String pageLoadCheckScript =
            "console.log('JeffernTV: Checking if page is fully loaded for close button');" +
            "var checkCount = 0;" +
            "var maxChecks = 30;" +  // 最多检查30次（15秒）
            "var checkTimer = setInterval(function() {" +
            "  checkCount++;" +
            "  console.log('JeffernTV: Page load check attempt ' + checkCount + '/' + maxChecks);" +
            "  " +
            "  // 检查页面是否完全加载" +
            "  var isLoaded = document.readyState === 'complete' && " +
            "                 !document.querySelector('.loading, .spinner, [class*=\"loading\"], [class*=\"spinner\"]') && " +
            "                 document.querySelectorAll('*').length > 50;" +  // 确保页面有足够的元素
            "  " +
            "  console.log('JeffernTV: Page state - readyState:', document.readyState, " +
            "              'elementCount:', document.querySelectorAll('*').length, " +
            "              'isLoaded:', isLoaded);" +
            "  " +
            "  if (isLoaded) {" +
            "    console.log('JeffernTV: Page fully loaded, executing close button click');" +
            "    clearInterval(checkTimer);" +
            "    " +
            "    // 再延迟2秒确保所有动态内容加载完成" +
            "    setTimeout(function() {" +
            "      console.log('JeffernTV: Final delay completed, clicking close button now');" +
            "      " + getCloseButtonScript() +
            "    }, 2000);" +
            "  } else if (checkCount >= maxChecks) {" +
            "    console.log('JeffernTV: Page load check timeout, executing close button anyway');" +
            "    clearInterval(checkTimer);" +
            "    " + getCloseButtonScript() +
            "  }" +
            "}, 500);";  // 每500ms检查一次

        webView.loadUrl("javascript:" + pageLoadCheckScript);
        Log.d(TAG, "Page load detection script injected for close button");
    }

    /**
     * 等待页面完全加载后再注入登录脚本
     */
    private void waitForPageFullyLoadedThenLogin(String url) {
        String pageLoadCheckScript =
            "console.log('JeffernTV: Checking if page is fully loaded for login');" +
            "var checkCount = 0;" +
            "var maxChecks = 20;" +  // 最多检查20次（10秒）
            "var checkTimer = setInterval(function() {" +
            "  checkCount++;" +
            "  console.log('JeffernTV: Login page load check attempt ' + checkCount + '/' + maxChecks);" +
            "  " +
            "  // 检查页面是否完全加载，特别关注登录表单元素" +
            "  var isLoaded = document.readyState === 'complete' && " +
            "                 !document.querySelector('.loading, .spinner, [class*=\"loading\"], [class*=\"spinner\"]') && " +
            "                 document.querySelectorAll('*').length > 30 && " +  // 登录页面元素相对较少
            "                 (document.querySelector('input[type=\"text\"]') || " +
            "                  document.querySelector('input[type=\"email\"]') || " +
            "                  document.querySelector('input[name*=\"user\"]') || " +
            "                  document.querySelector('input[placeholder*=\"用\"]'));" +  // 确保有登录输入框
            "  " +
            "  var loginInputs = document.querySelectorAll('input[type=\"text\"], input[type=\"email\"], input[type=\"password\"]').length;" +
            "  console.log('JeffernTV: Login page state - readyState:', document.readyState, " +
            "              'elementCount:', document.querySelectorAll('*').length, " +
            "              'loginInputs:', loginInputs, " +
            "              'isLoaded:', isLoaded);" +
            "  " +
            "  if (isLoaded) {" +
            "    console.log('JeffernTV: Login page fully loaded, executing login injection');" +
            "    clearInterval(checkTimer);" +
            "    " +
            "    // 再延迟1秒确保所有输入框完全初始化" +
            "    setTimeout(function() {" +
            "      console.log('JeffernTV: Final delay completed, injecting login script now');" +
            "      executeLoginScript();" +
            "    }, 1000);" +
            "  } else if (checkCount >= maxChecks) {" +
            "    console.log('JeffernTV: Login page load check timeout, injecting login anyway');" +
            "    clearInterval(checkTimer);" +
            "    executeLoginScript();" +
            "  }" +
            "  " +
            "  function executeLoginScript() {" +
            "    " + getLoginScriptContent() +
            "  }" +
            "}, 500);";  // 每500ms检查一次

        webView.loadUrl("javascript:" + pageLoadCheckScript);
        Log.d(TAG, "Page load detection script injected for login");
    }

    /**
     * 获取登录脚本内容（用于嵌入到页面检测中）
     */
    private String getLoginScriptContent() {
        String embyUser = prefManager.getUsername();
        String embyPass = prefManager.getPassword();

        return String.format(
            "console.log('JeffernTV: Starting auto login');" +
            "var tryCount=0;" +
            "var timer=setInterval(function(){" +
            "  var userInput = document.querySelector('input[type=\"text\"],input[name*=\"user\"],input[placeholder*=\"用\"]');" +
            "  var passInput = document.querySelector('input[type=\"password\"],input[name*=\"pass\"],input[placeholder*=\"密\"]');" +
            "  var form = userInput ? userInput.form : null;" +
            "  " +
            "  if(userInput && passInput) {" +
            "    console.log('JeffernTV: Found login inputs, filling credentials');" +
            "    console.log('JeffernTV: User input element:', userInput);" +
            "    console.log('JeffernTV: Pass input element:', passInput);" +
            "    userInput.focus();" +
            "    userInput.value='%s';" +
            "    console.log('JeffernTV: User value set to:', userInput.value);" +
            "    userInput.dispatchEvent(new Event('input', {bubbles:true}));" +
            "    userInput.dispatchEvent(new Event('change', {bubbles:true}));" +
            "    passInput.focus();" +
            "    passInput.value='%s';" +
            "    console.log('JeffernTV: Pass value set to:', passInput.value);" +
            "    passInput.dispatchEvent(new Event('input', {bubbles:true}));" +
            "    passInput.dispatchEvent(new Event('change', {bubbles:true}));" +
            "    passInput.blur();" +
            "  } else {" +
            "    console.log('JeffernTV: Login inputs not found - userInput:', !!userInput, 'passInput:', !!passInput);" +
            "  }" +
            "  " +
            "  if(form && userInput && passInput) {" +
            "    console.log('JeffernTV: Submitting login form');" +
            "    try {" +
            "      form.dispatchEvent(new Event('submit', {bubbles:true,cancelable:true}));" +
            "      if(form.requestSubmit) form.requestSubmit(); else form.submit();" +
            "    } catch(e) {" +
            "      form.submit();" +
            "    }" +
            "    clearInterval(timer);" +
            "    console.log('JeffernTV: Auto login completed');" +
            "  }" +
            "  " +
            "  if(++tryCount > 60) {" +
            "    clearInterval(timer);" +
            "    console.log('JeffernTV: Auto login timeout');" +
            "  }" +
            "}, 100);", embyUser, embyPass);
    }

    /**
     * 获取独立的登录脚本（保留原方法用于兼容）
     */
    private String getLoginScript(String url) {
        return getLoginScriptContent();
    }

    /**
     * 执行简化的自动登录（直接注入，不做复杂检测）
     */
    private void executeSimpleAutoLogin() {
        String username = prefManager.getUsername();
        String password = prefManager.getPassword();

        Log.d(TAG, "Simple auto login - username: " + (username.isEmpty() ? "EMPTY" : "SET") +
                   ", password: " + (password.isEmpty() ? "EMPTY" : "SET"));

        if (username.isEmpty() || password.isEmpty()) {
            Log.d(TAG, "Username or password is empty, skipping auto login");
            return;
        }

        String loginScript = String.format(
            "console.log('JeffernTV: Simple auto login starting');" +
            "console.log('JeffernTV: Username to fill: %s');" +
            "console.log('JeffernTV: Password length: %d');" +
            "" +
            "// 查找所有可能的输入框" +
            "var allInputs = document.querySelectorAll('input');" +
            "console.log('JeffernTV: Found ' + allInputs.length + ' input elements');" +
            "allInputs.forEach(function(input, index) {" +
            "  console.log('JeffernTV: Input ' + index + ':', input.type, input.name, input.placeholder, input.id);" +
            "});" +
            "" +
            "// 尝试多种选择器查找用户名输入框" +
            "var userInput = document.querySelector('input[type=\"text\"]') ||" +
            "                document.querySelector('input[type=\"email\"]') ||" +
            "                document.querySelector('input[name*=\"user\"]') ||" +
            "                document.querySelector('input[name*=\"User\"]') ||" +
            "                document.querySelector('input[name*=\"name\"]') ||" +
            "                document.querySelector('input[placeholder*=\"用户\"]') ||" +
            "                document.querySelector('input[placeholder*=\"用\"]') ||" +
            "                document.querySelector('input[placeholder*=\"User\"]') ||" +
            "                document.querySelector('input[placeholder*=\"user\"]') ||" +
            "                document.querySelector('input[id*=\"user\"]') ||" +
            "                document.querySelector('input[id*=\"User\"]');" +
            "" +
            "// 尝试多种选择器查找密码输入框" +
            "var passInput = document.querySelector('input[type=\"password\"]') ||" +
            "                document.querySelector('input[name*=\"pass\"]') ||" +
            "                document.querySelector('input[name*=\"Pass\"]') ||" +
            "                document.querySelector('input[placeholder*=\"密码\"]') ||" +
            "                document.querySelector('input[placeholder*=\"密\"]') ||" +
            "                document.querySelector('input[placeholder*=\"Pass\"]') ||" +
            "                document.querySelector('input[placeholder*=\"pass\"]') ||" +
            "                document.querySelector('input[id*=\"pass\"]') ||" +
            "                document.querySelector('input[id*=\"Pass\"]');" +
            "" +
            "console.log('JeffernTV: User input found:', !!userInput);" +
            "console.log('JeffernTV: Pass input found:', !!passInput);" +
            "if(userInput) console.log('JeffernTV: User input element:', userInput);" +
            "if(passInput) console.log('JeffernTV: Pass input element:', passInput);" +
            "" +
            "if(userInput && passInput) {" +
            "  console.log('JeffernTV: Both inputs found, filling credentials');" +
            "  " +
            "  // 填充用户名" +
            "  userInput.focus();" +
            "  userInput.value = '%s';" +
            "  userInput.dispatchEvent(new Event('input', {bubbles: true}));" +
            "  userInput.dispatchEvent(new Event('change', {bubbles: true}));" +
            "  console.log('JeffernTV: Username filled:', userInput.value);" +
            "  " +
            "  // 填充密码" +
            "  passInput.focus();" +
            "  passInput.value = '%s';" +
            "  passInput.dispatchEvent(new Event('input', {bubbles: true}));" +
            "  passInput.dispatchEvent(new Event('change', {bubbles: true}));" +
            "  console.log('JeffernTV: Password filled, length:', passInput.value.length);" +
            "  " +
            "  // 查找并提交表单" +
            "  var form = userInput.form || passInput.form || document.querySelector('form');" +
            "  console.log('JeffernTV: Form found:', !!form);" +
            "  " +
            "  if(form) {" +
            "    console.log('JeffernTV: Submitting form');" +
            "    setTimeout(function() {" +
            "      try {" +
            "        form.dispatchEvent(new Event('submit', {bubbles: true, cancelable: true}));" +
            "        if(form.requestSubmit) {" +
            "          form.requestSubmit();" +
            "        } else {" +
            "          form.submit();" +
            "        }" +
            "        console.log('JeffernTV: Form submitted successfully');" +
            "      } catch(e) {" +
            "        console.log('JeffernTV: Form submit error:', e);" +
            "        form.submit();" +
            "      }" +
            "    }, 500);" +
            "  } else {" +
            "    console.log('JeffernTV: No form found, looking for submit button');" +
            "    var submitBtn = document.querySelector('button[type=\"submit\"]') ||" +
            "                    document.querySelector('input[type=\"submit\"]') ||" +
            "                    document.querySelector('button:contains(\"登录\")') ||" +
            "                    document.querySelector('button:contains(\"Login\")') ||" +
            "                    document.querySelector('button');" +
            "    if(submitBtn) {" +
            "      console.log('JeffernTV: Clicking submit button');" +
            "      submitBtn.click();" +
            "    }" +
            "  }" +
            "} else {" +
            "  console.log('JeffernTV: Login inputs not found');" +
            "  if(!userInput) console.log('JeffernTV: User input not found');" +
            "  if(!passInput) console.log('JeffernTV: Password input not found');" +
            "}",
            username, password.length(), username, password);

        webView.loadUrl("javascript:" + loginScript);
        Log.d(TAG, "Simple auto login script injected");
    }

    /**
     * 关闭按钮点击方法（完全参考登录方式）
     */
    private void autoClickEmbyCloseButtonSimple() {
        Log.d(TAG, "Starting close button click - using exact login approach");

        String js =
            "console.log('JeffernTV: Starting close button auto-click');\n" +
            "var tryCount=0;var timer=setInterval(function(){\n" +
            "var closeBtn = document.querySelector('button[is=\"paper-icon-button-light\"][title=\"关闭\"]') ||\n" +
            "               document.querySelector('button[aria-label=\"关闭\"]') ||\n" +
            "               document.querySelector('button.btnPinNavDrawer') ||\n" +
            "               document.querySelector('button[class*=\"btnPinNavDrawer\"]');\n" +
            "var iconElement = closeBtn ? closeBtn.querySelector('i.btnPinNavDrawerIcon') : null;\n" +
            "if(closeBtn&&iconElement){\n" +
            "var iconText = iconElement.textContent.trim();\n" +
            "console.log('JeffernTV: Found close button, icon text:', iconText);\n" +
            "if(iconText === 'view_sidebar'){\n" +
            "console.log('JeffernTV: Sidebar already closed, skipping');\n" +
            "clearInterval(timer);\n" +
            "return;\n" +
            "}\n" +
            "if(iconText === 'close'){\n" +
            "console.log('JeffernTV: Sidebar open, clicking close button');\n" +
            "closeBtn.focus();\n" +
            "closeBtn.click();\n" +
            "closeBtn.dispatchEvent(new MouseEvent('click', {bubbles:true,cancelable:true}));\n" +
            "clearInterval(timer);\n" +
            "console.log('JeffernTV: Close button clicked successfully');\n" +
            "return;\n" +
            "}\n" +
            "}\n" +
            "if(++tryCount>60){clearInterval(timer);console.log('JeffernTV: Close button timeout');}\n" +
            "}, 100);";

        // 使用loadUrl方式执行JavaScript（兼容所有版本，和登录一样）
        webView.loadUrl("javascript:" + js);
        Log.d(TAG, "Close button script injected (exact login style)");
    }

    /**
     * 获取关闭按钮点击脚本
     */
    private String getCloseButtonScript() {
        return
            "if(window.jeffernTVCloseButtonExecuted) {" +
            "  console.log('JeffernTV: Close button already executed, skipping');" +
            "} else {" +
            "  window.jeffernTVCloseButtonExecuted = true;" +
            "  console.log('JeffernTV: Starting close button execution');" +
            "  " +
            "  var tryCount=0;" +
            "  var timer=setInterval(function(){" +
            "    console.log('JeffernTV: Attempt ' + (tryCount + 1) + ' to find close button');" +
            "    " +
            "    var closeBtn = document.querySelector('button.btnPinNavDrawer') || " +
            "                   document.querySelector('button[title=\"关闭\"]') || " +
            "                   document.querySelector('button[aria-label=\"关闭\"]') || " +
            "                   document.querySelector('button[is=\"paper-icon-button-light\"][title=\"关闭\"]') || " +
            "                   document.querySelector('button[class*=\"btnPinNavDrawer\"]') || " +
            "                   document.querySelector('.btnPinNavDrawer');" +
            "    " +
            "    if(closeBtn){" +
            "      console.log('JeffernTV: Found close button:', closeBtn);" +
            "      var rect = closeBtn.getBoundingClientRect();" +
            "      var style = getComputedStyle(closeBtn);" +
            "      " +
            "      if(rect.width > 0 && rect.height > 0 && style.display !== 'none'){" +
            "        try{" +
            "          closeBtn.focus();" +
            "          closeBtn.click();" +
            "          closeBtn.dispatchEvent(new MouseEvent('click', {bubbles:true,cancelable:true}));" +
            "          console.log('JeffernTV: Close button clicked successfully');" +
            "        }catch(e){" +
            "          console.log('JeffernTV: Click error:', e);" +
            "        }" +
            "        clearInterval(timer);" +
            "      }" +
            "    }" +
            "    " +
            "    if(++tryCount>20){" +
            "      console.log('JeffernTV: Close button search timeout');" +
            "      clearInterval(timer);" +
            "    }" +
            "  }, 200);" +
            "}";
    }

    /**
     * 重置关闭按钮全局标记（用于调试或特殊情况）
     */
    public static void resetCloseButtonGlobalFlag() {
        embyCloseButtonInjectedGlobal = false;
        Log.d(TAG, "Emby close button global flag reset");
    }

    /**
     * 重置登录全局标记（用于调试或特殊情况）
     */
    public static void resetLoginGlobalFlag() {
        embyLoginInjectedGlobal = false;
        Log.d(TAG, "Emby login global flag reset");
    }

    /**
     * 重置所有全局标记（用于调试或特殊情况）
     */
    public static void resetAllGlobalFlags() {
        embyLoginInjectedGlobal = false;
        embyCloseButtonInjectedGlobal = false;
        Log.d(TAG, "All Emby global flags reset");
    }

    /**
     * 清除JavaScript中的全局标记（用于调试）
     */
    private void clearJavaScriptGlobalFlags() {
        String clearScript =
            "if(window.jeffernTVCloseButtonExecuted) {" +
            "delete window.jeffernTVCloseButtonExecuted;" +
            "console.log('JeffernTV: JavaScript global flags cleared');" +
            "}";
        webView.loadUrl("javascript:" + clearScript);
    }

    /**
     * 注入强制手机界面样式
     */
    private void injectMobileInterfaceScript() {
        String mobileScript =
            "(function() {" +
            "  console.log('JeffernTV: Forcing mobile interface');" +
            "  " +
            "  // 强制手机界面样式" +
            "  var mobileStyle = document.createElement('style');" +
            "  mobileStyle.textContent = `" +
            "    /* 强制手机布局 */" +
            "    body { " +
            "      max-width: 100% !important;" +
            "      width: 100% !important;" +
            "      margin: 0 !important;" +
            "      padding: 10px !important;" +
            "      font-size: 16px !important;" +
            "      line-height: 1.5 !important;" +
            "    }" +
            "    " +
            "    /* 强制单列布局 */" +
            "    .container, .main, .content, .wrapper {" +
            "      max-width: 100% !important;" +
            "      width: 100% !important;" +
            "      display: block !important;" +
            "      float: none !important;" +
            "    }" +
            "    " +
            "    /* 大按钮样式 */" +
            "    button, .btn, .button, a {" +
            "      min-height: 44px !important;" +
            "      padding: 12px 16px !important;" +
            "      font-size: 16px !important;" +
            "      border-radius: 8px !important;" +
            "      margin: 4px !important;" +
            "      display: inline-block !important;" +
            "      text-align: center !important;" +
            "    }" +
            "    " +
            "    /* 影片卡片手机样式 */" +
            "    .poster, .movie-item, .video-item, .card {" +
            "      width: calc(50% - 10px) !important;" +
            "      max-width: 200px !important;" +
            "      margin: 5px !important;" +
            "      display: inline-block !important;" +
            "      vertical-align: top !important;" +
            "    }" +
            "    " +
            "    /* 导航菜单手机样式 */" +
            "    .nav, .menu, .navbar {" +
            "      display: block !important;" +
            "      width: 100% !important;" +
            "    }" +
            "    " +
            "    .nav-item, .menu-item {" +
            "      display: block !important;" +
            "      width: 100% !important;" +
            "      padding: 12px !important;" +
            "      border-bottom: 1px solid #eee !important;" +
            "    }" +
            "    " +
            "    /* 隐藏桌面端元素 */" +
            "    .desktop-only, .pc-only, .sidebar {" +
            "      display: none !important;" +
            "    }" +
            "    " +
            "    /* 响应式图片 */" +
            "    img {" +
            "      max-width: 100% !important;" +
            "      height: auto !important;" +
            "    }" +
            "    " +
            "    /* 禁用键盘滚动 */" +
            "    html, body {" +
            "      overflow-x: hidden !important;" +
            "      scroll-behavior: auto !important;" +
            "    }" +
            "    " +
            "    /* 禁用焦点轮廓 */" +
            "    * {" +
            "      outline: none !important;" +
            "      -webkit-tap-highlight-color: transparent !important;" +
            "    }" +
            "  `;" +
            "  document.head.appendChild(mobileStyle);" +
            "  " +
            "  // 强制移除桌面端类名" +
            "  setTimeout(() => {" +
            "    document.body.classList.remove('desktop', 'pc', 'web');" +
            "    document.body.classList.add('mobile', 'touch', 'android');" +
            "    " +
            "    // 设置viewport" +
            "    var viewport = document.querySelector('meta[name=\"viewport\"]');" +
            "    if (!viewport) {" +
            "      viewport = document.createElement('meta');" +
            "      viewport.name = 'viewport';" +
            "      document.head.appendChild(viewport);" +
            "    }" +
            "    viewport.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';" +
            "  }, 100);" +
            "})();";

        webView.evaluateJavascript(mobileScript, null);
        Log.d(TAG, "Mobile interface script injected");
    }

    /**
     * 注入SmartFocus风格的聚焦系统
     */
    private void injectFocusHandlingScript() {
        String smartFocusScript =
            "(function() {" +
            "  console.log('JeffernTV: Injecting SmartFocus-style navigation system');" +

            // 防止重复注入
            "  if (window.jeffernSmartFocus) return;" +
            "  window.jeffernSmartFocus = true;" +

            // SmartFocus样式系统
            "  var style = document.createElement('style');" +
            "  style.textContent = `" +
            "    * { outline: none !important; }" +
            "    .smart-focus-border { " +
            "      position: absolute !important;" +
            "      pointer-events: none !important;" +
            "      border: 4px solid #00ff00 !important; " +
            "      border-radius: 8px !important;" +
            "      box-shadow: 0 0 20px rgba(0, 255, 0, 0.6) !important;" +
            "      background: rgba(0, 255, 0, 0.1) !important;" +
            "      z-index: 999999 !important;" +
            "      transition: all 0.3s ease !important;" +
            "      transform: scale(1.05) !important;" +
            "    }" +
            "    .smart-focus-hint {" +
            "      position: fixed !important;" +
            "      top: 20px !important;" +
            "      right: 20px !important;" +
            "      background: rgba(0, 0, 0, 0.8) !important;" +
            "      color: #00ff00 !important;" +
            "      padding: 12px 16px !important;" +
            "      border-radius: 8px !important;" +
            "      z-index: 1000000 !important;" +
            "      font-size: 14px !important;" +
            "      font-family: monospace !important;" +
            "      border: 1px solid #00ff00 !important;" +
            "    }" +
            "  `;" +
            "  document.head.appendChild(style);" +

            // SmartFocus管理器
            "  var SmartFocusManager = {" +
            "    currentIndex: 0," +
            "    elements: []," +
            "    focusBorder: null," +
            "    isPlayerFullscreen: false," +
            "    playerElement: null," +
            "    " +
            "    // 初始化焦点边框" +
            "    initFocusBorder: function() {" +
            "      if (this.focusBorder) return;" +
            "      this.focusBorder = document.createElement('div');" +
            "      this.focusBorder.className = 'smart-focus-border';" +
            "      this.focusBorder.style.display = 'none';" +
            "      document.body.appendChild(this.focusBorder);" +
            "    }," +
            "    " +
            "    // 显示焦点边框" +
            "    showFocusBorder: function(element) {" +
            "      if (!this.focusBorder || !element) return;" +
            "      " +
            "      var rect = element.getBoundingClientRect();" +
            "      var scrollX = window.pageXOffset || document.documentElement.scrollLeft;" +
            "      var scrollY = window.pageYOffset || document.documentElement.scrollTop;" +
            "      " +
            "      this.focusBorder.style.display = 'block';" +
            "      this.focusBorder.style.left = (rect.left + scrollX - 6) + 'px';" +
            "      this.focusBorder.style.top = (rect.top + scrollY - 6) + 'px';" +
            "      this.focusBorder.style.width = (rect.width + 12) + 'px';" +
            "      this.focusBorder.style.height = (rect.height + 12) + 'px';" +
            "    }," +
            "    " +
            "    // 隐藏焦点边框" +
            "    hideFocusBorder: function() {" +
            "      if (this.focusBorder) {" +
            "        this.focusBorder.style.display = 'none';" +
            "      }" +
            "    }" +
            "  };" +

            // SmartFocus元素识别系统 - 针对实际HTML结构
            "  SmartFocusManager.scanFocusableElements = function() {" +
            "    var elements = [];" +
            "    " +
            "    // 根据实际HTML结构的选择器 - 优先级排序" +
            "    var selectorGroups = [" +
            "      // 最高优先级：按钮和可点击区域" +
            "      {" +
            "        priority: 1," +
            "        selectors: [" +
            "          // 明确的按钮" +
            "          'button'," +
            "          // 可点击的div（如选集、换源）" +
            "          'div.cursor-pointer'," +
            "          'div[class*=\"cursor-pointer\"]'," +
            "          // 包含文字的可点击div" +
            "          'div.flex-1'," +
            "          'div[class*=\"text-center\"]'," +
            "          // 带有hover效果的元素" +
            "          'div[class*=\"hover:\"]'," +
            "          '[class*=\"hover:scale\"]'," +
            "          '[class*=\"hover:bg\"]'" +
            "        ]" +
            "      }," +
            "      // 高优先级：链接和表单元素" +
            "      {" +
            "        priority: 2," +
            "        selectors: [" +
            "          'a[href]:not([href=\"#\"]):not([href=\"javascript:\"])'," +
            "          'input[type=\"button\"]:not([disabled])'," +
            "          'input[type=\"submit\"]:not([disabled])'," +
            "          '[role=\"button\"]'" +
            "        ]" +
            "      }," +
            "      // 中优先级：可能可点击的元素" +
            "      {" +
            "        priority: 3," +
            "        selectors: [" +
            "          // 包含SVG图标的div" +
            "          'div:has(svg)'," +
            "          // 带有transition的元素" +
            "          '[class*=\"transition\"]'," +
            "          // 带有特定样式的元素" +
            "          '.btn', '.button'," +
            "          '[onclick]'" +
            "        ]" +
            "      }," +
            "      // 低优先级：其他可能的交互元素" +
            "      {" +
            "        priority: 4," +
            "        selectors: [" +
            "          'li', '.item', '.card'," +
            "          'span[class*=\"cursor\"]'," +
            "          'div[class*=\"rounded\"]'" +
            "        ]" +
            "      }" +
            "    ];" +
            "    " +
            "    // 按优先级收集元素" +
            "    selectorGroups.forEach(group => {" +
            "      group.selectors.forEach(selector => {" +
            "        try {" +
            "          document.querySelectorAll(selector).forEach(el => {" +
            "            if (elements.indexOf(el) === -1 && this.isElementFocusable(el)) {" +
            "              el._smartFocusPriority = group.priority;" +
            "              elements.push(el);" +
            "            }" +
            "          });" +
            "        } catch(e) { console.warn('Selector error:', selector, e); }" +
            "      });" +
            "    });" +
            "    " +
            "    // 智能排序：优先级 + 位置" +
            "    elements.sort((a, b) => {" +
            "      // 首先按优先级" +
            "      var priorityDiff = (a._smartFocusPriority || 999) - (b._smartFocusPriority || 999);" +
            "      if (priorityDiff !== 0) return priorityDiff;" +
            "      " +
            "      // 然后按位置（从上到下，从左到右）" +
            "      var rectA = a.getBoundingClientRect();" +
            "      var rectB = b.getBoundingClientRect();" +
            "      var rowDiff = Math.floor(rectA.top / 80) - Math.floor(rectB.top / 80);" +
            "      if (rowDiff !== 0) return rowDiff;" +
            "      return rectA.left - rectB.left;" +
            "    });" +
            "    " +
            "    this.elements = elements;" +
            "    console.log('SmartFocus: Scanned ' + elements.length + ' focusable elements');" +
            "    " +
            "    // 详细调试信息" +
            "    elements.forEach((el, index) => {" +
            "      var rect = el.getBoundingClientRect();" +
            "      console.log('SmartFocus Element ' + index + ':', {" +
            "        tag: el.tagName," +
            "        class: el.className," +
            "        text: el.textContent ? el.textContent.trim().substring(0, 20) : ''," +
            "        size: rect.width + 'x' + rect.height," +
            "        position: Math.round(rect.left) + ',' + Math.round(rect.top)," +
            "        priority: el._smartFocusPriority" +
            "      });" +
            "    });" +
            "    " +
            "    return elements;" +
            "  };" +
            "  " +
            "  // 检查元素是否可聚焦" +
            "  SmartFocusManager.isElementFocusable = function(el) {" +
            "    if (!el || el.disabled) return false;" +
            "    " +
            "    var rect = el.getBoundingClientRect();" +
            "    var style = getComputedStyle(el);" +
            "    " +
            "    // 基本可见性检查" +
            "    if (rect.width < 3 || rect.height < 3) return false;" +
            "    if (style.display === 'none' || style.visibility === 'hidden') return false;" +
            "    if (style.opacity < 0.05) return false;" +
            "    " +
            "    // 位置检查（允许部分超出屏幕）" +
            "    if (rect.bottom < -100 || rect.top > window.innerHeight + 100) return false;" +
            "    if (rect.right < -100 || rect.left > window.innerWidth + 100) return false;" +
            "    " +
            "    // 检查是否是按钮或可点击元素" +
            "    var isButton = el.tagName === 'BUTTON';" +
            "    var isClickable = el.className && (" +
            "      el.className.includes('cursor-pointer') || " +
            "      el.className.includes('hover:') || " +
            "      el.className.includes('transition') || " +
            "      el.className.includes('btn')" +
            "    );" +
            "    " +
            "    var hasClickEvent = el.onclick || el.getAttribute('onclick');" +
            "    var isLink = el.tagName === 'A' && el.href && el.href !== '#';" +
            "    " +
            "    // 检查是否包含文字内容" +
            "    var hasText = el.textContent && el.textContent.trim().length > 0;" +
            "    " +
            "    // 检查是否包含SVG图标" +
            "    var hasSVG = el.querySelector && el.querySelector('svg');" +
            "    " +
            "    // 按钮和明确可点击元素降低门槛" +
            "    if (isButton || isClickable || hasClickEvent || isLink) {" +
            "      return rect.width > 10 && rect.height > 10;" +
            "    }" +
            "    " +
            "    // 包含文字或图标的元素" +
            "    if (hasText || hasSVG) {" +
            "      return rect.width > 15 && rect.height > 15;" +
            "    }" +
            "    " +
            "    // 普通元素较高门槛" +
            "    return rect.width > 30 && rect.height > 30;" +
            "  };" +

            // 查找播放器元素
            "  function findPlayerElement() {" +
            "    var playerSelectors = [" +
            "      'video', 'iframe[src*=\"player\"]', '.player', '.video-player'," +
            "      '.dplayer', '.artplayer', '.jwplayer', '[class*=\"player\"]'" +
            "    ];" +
            "    " +
            "    for (var selector of playerSelectors) {" +
            "      var element = document.querySelector(selector);" +
            "      if (element) return element;" +
            "    }" +
            "    return null;" +
            "  }" +

            // 播放器全屏切换
            "  function togglePlayerFullscreen() {" +
            "    playerElement = findPlayerElement();" +
            "    if (!playerElement) {" +
            "      console.log('JeffernTV: No player element found');" +
            "      return;" +
            "    }" +
            "    " +
            "    if (!isPlayerFullscreen) {" +
            "      // 进入全屏" +
            "      if (playerElement.requestFullscreen) {" +
            "        playerElement.requestFullscreen();" +
            "      } else if (playerElement.webkitRequestFullscreen) {" +
            "        playerElement.webkitRequestFullscreen();" +
            "      } else if (playerElement.mozRequestFullScreen) {" +
            "        playerElement.mozRequestFullScreen();" +
            "      } else {" +
            "        // 手动全屏样式" +
            "        playerElement.style.position = 'fixed';" +
            "        playerElement.style.top = '0';" +
            "        playerElement.style.left = '0';" +
            "        playerElement.style.width = '100vw';" +
            "        playerElement.style.height = '100vh';" +
            "        playerElement.style.zIndex = '9999';" +
            "        playerElement.style.backgroundColor = 'black';" +
            "      }" +
            "      isPlayerFullscreen = true;" +
            "      console.log('JeffernTV: Player entered fullscreen');" +
            "    } else {" +
            "      // 退出全屏" +
            "      if (document.exitFullscreen) {" +
            "        document.exitFullscreen();" +
            "      } else if (document.webkitExitFullscreen) {" +
            "        document.webkitExitFullscreen();" +
            "      } else if (document.mozCancelFullScreen) {" +
            "        document.mozCancelFullScreen();" +
            "      } else {" +
            "        // 手动退出全屏" +
            "        playerElement.style.position = '';" +
            "        playerElement.style.top = '';" +
            "        playerElement.style.left = '';" +
            "        playerElement.style.width = '';" +
            "        playerElement.style.height = '';" +
            "        playerElement.style.zIndex = '';" +
            "        playerElement.style.backgroundColor = '';" +
            "      }" +
            "      isPlayerFullscreen = false;" +
            "      console.log('JeffernTV: Player exited fullscreen');" +
            "    }" +
            "  }" +

            // SmartFocus空间导航算法
            "  SmartFocusManager.findBestMatch = function(direction) {" +
            "    if (this.elements.length === 0) return -1;" +
            "    " +
            "    var current = this.elements[this.currentIndex];" +
            "    if (!current) return 0;" +
            "    " +
            "    var currentRect = current.getBoundingClientRect();" +
            "    var candidates = [];" +
            "    " +
            "    this.elements.forEach((el, index) => {" +
            "      if (index === this.currentIndex) return;" +
            "      " +
            "      var rect = el.getBoundingClientRect();" +
            "      var centerX = rect.left + rect.width / 2;" +
            "      var centerY = rect.top + rect.height / 2;" +
            "      var currentCenterX = currentRect.left + currentRect.width / 2;" +
            "      var currentCenterY = currentRect.top + currentRect.height / 2;" +
            "      " +
            "      var deltaX = centerX - currentCenterX;" +
            "      var deltaY = centerY - currentCenterY;" +
            "      var distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);" +
            "      " +
            "      var isInDirection = false;" +
            "      var directionScore = 0;" +
            "      " +
            "      switch(direction) {" +
            "        case 'up':" +
            "          isInDirection = deltaY < -20;" +
            "          directionScore = Math.abs(deltaX) + Math.max(0, deltaY) * 2;" +
            "          break;" +
            "        case 'down':" +
            "          isInDirection = deltaY > 20;" +
            "          directionScore = Math.abs(deltaX) + Math.max(0, -deltaY) * 2;" +
            "          break;" +
            "        case 'left':" +
            "          isInDirection = deltaX < -20;" +
            "          directionScore = Math.abs(deltaY) + Math.max(0, deltaX) * 2;" +
            "          break;" +
            "        case 'right':" +
            "          isInDirection = deltaX > 20;" +
            "          directionScore = Math.abs(deltaY) + Math.max(0, -deltaX) * 2;" +
            "          break;" +
            "      }" +
            "      " +
            "      if (isInDirection) {" +
            "        candidates.push({" +
            "          index: index," +
            "          distance: distance," +
            "          directionScore: directionScore," +
            "          priority: el._smartFocusPriority || 999" +
            "        });" +
            "      }" +
            "    });" +
            "    " +
            "    if (candidates.length === 0) {" +
            "      // 边界循环" +
            "      switch(direction) {" +
            "        case 'up': return this.elements.length - 1;" +
            "        case 'down': return 0;" +
            "        case 'left': return this.elements.length - 1;" +
            "        case 'right': return 0;" +
            "      }" +
            "    }" +
            "    " +
            "    // 综合评分：优先级 + 方向得分 + 距离" +
            "    candidates.sort((a, b) => {" +
            "      var scoreA = a.priority * 100 + a.directionScore + a.distance * 0.1;" +
            "      var scoreB = b.priority * 100 + b.directionScore + b.distance * 0.1;" +
            "      return scoreA - scoreB;" +
            "    });" +
            "    " +
            "    return candidates[0].index;" +
            "  };" +

            // SmartFocus焦点管理
            "  SmartFocusManager.setFocus = function(index) {" +
            "    if (this.elements.length === 0) {" +
            "      this.scanFocusableElements();" +
            "      if (this.elements.length === 0) return;" +
            "    }" +
            "    " +
            "    // 边界检查" +
            "    if (index < 0) index = this.elements.length - 1;" +
            "    if (index >= this.elements.length) index = 0;" +
            "    this.currentIndex = index;" +
            "    " +
            "    var element = this.elements[this.currentIndex];" +
            "    if (!element) return;" +
            "    " +
            "    // 显示焦点边框" +
            "    this.showFocusBorder(element);" +
            "    " +
            "    // 滚动到视图" +
            "    element.scrollIntoView({ " +
            "      behavior: 'smooth', " +
            "      block: 'center', " +
            "      inline: 'center' " +
            "    });" +
            "    " +
            "    // 设置焦点" +
            "    try {" +
            "      element.focus();" +
            "    } catch(e) {}" +
            "    " +
            "    console.log('SmartFocus: Focused element', this.currentIndex, element.tagName, element.className);" +
            "  };" +
            "  " +
            "  SmartFocusManager.navigate = function(direction) {" +
            "    var nextIndex = this.findBestMatch(direction);" +
            "    if (nextIndex !== -1) {" +
            "      this.setFocus(nextIndex);" +
            "    }" +
            "  };" +
            "  " +
            "  SmartFocusManager.click = function() {" +
            "    var element = this.elements[this.currentIndex];" +
            "    if (element) {" +
            "      element.click();" +
            "      console.log('SmartFocus: Clicked element', element);" +
            "    }" +
            "  };" +

            // TV导航 - 根据方向查找下一个元素
            "  function findNextElement(direction) {" +
            "    if (focusableElements.length === 0) return -1;" +
            "    " +
            "    var current = focusableElements[currentFocusIndex];" +
            "    if (!current) return 0;" +
            "    " +
            "    var currentRect = current.getBoundingClientRect();" +
            "    var candidates = [];" +
            "    " +
            "    focusableElements.forEach((el, index) => {" +
            "      if (index === currentFocusIndex) return;" +
            "      var rect = el.getBoundingClientRect();" +
            "      var distance = Math.sqrt(Math.pow(rect.left - currentRect.left, 2) + Math.pow(rect.top - currentRect.top, 2));" +
            "      " +
            "      var isValidDirection = false;" +
            "      switch(direction) {" +
            "        case 'up':" +
            "          isValidDirection = rect.bottom <= currentRect.top + 10;" +
            "          break;" +
            "        case 'down':" +
            "          isValidDirection = rect.top >= currentRect.bottom - 10;" +
            "          break;" +
            "        case 'left':" +
            "          isValidDirection = rect.right <= currentRect.left + 10;" +
            "          break;" +
            "        case 'right':" +
            "          isValidDirection = rect.left >= currentRect.right - 10;" +
            "          break;" +
            "      }" +
            "      " +
            "      if (isValidDirection) {" +
            "        candidates.push({ index: index, distance: distance, rect: rect });" +
            "      }" +
            "    });" +
            "    " +
            "    if (candidates.length === 0) {" +
            "      // 如果没有找到，循环到边界" +
            "      switch(direction) {" +
            "        case 'up': return focusableElements.length - 1;" +
            "        case 'down': return 0;" +
            "        case 'left': return focusableElements.length - 1;" +
            "        case 'right': return 0;" +
            "      }" +
            "    }" +
            "    " +
            "    // 选择最近的候选元素" +
            "    candidates.sort((a, b) => a.distance - b.distance);" +
            "    return candidates[0].index;" +
            "  }" +

            // 设置焦点
            "  function setFocus(index) {" +
            "    if (focusableElements.length === 0) {" +
            "      updateFocusableElements();" +
            "      if (focusableElements.length === 0) return;" +
            "    }" +
            "    " +
            "    // 移除之前的焦点样式" +
            "    document.querySelectorAll('.jeffern-focused').forEach(el => {" +
            "      el.classList.remove('jeffern-focused');" +
            "    });" +
            "    " +
            "    // 确保索引在有效范围内" +
            "    if (index < 0) index = focusableElements.length - 1;" +
            "    if (index >= focusableElements.length) index = 0;" +
            "    currentFocusIndex = index;" +
            "    " +
            "    var element = focusableElements[currentFocusIndex];" +
            "    if (element) {" +
            "      element.focus();" +
            "      element.classList.add('jeffern-focused');" +
            "      element.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'center' });" +
            "      console.log('JeffernTV: TV Focus on element ' + currentFocusIndex + ':', element.tagName, element.className);" +
            "    }" +
            "  }" +

            // 显示SmartFocus提示
            "  SmartFocusManager.showHint = function() {" +
            "    var hint = document.querySelector('.smart-focus-hint');" +
            "    if (!hint) {" +
            "      hint = document.createElement('div');" +
            "      hint.className = 'smart-focus-hint';" +
            "      document.body.appendChild(hint);" +
            "    }" +
            "    hint.innerHTML = 'SmartFocus: 方向键智能导航 | 回车选择 | 长按回车全屏';" +
            "    setTimeout(() => { if (hint.parentNode) hint.parentNode.removeChild(hint); }, 5000);" +
            "  };" +

            // 长按检测变量
            "  var enterPressTime = 0;" +
            "  var isLongPress = false;" +

            // 禁用页面默认滚动
            "  document.addEventListener('keydown', function(e) {" +
            "    // 阻止方向键的默认滚动行为" +
            "    if ([37, 38, 39, 40, 32, 33, 34, 35, 36].includes(e.keyCode)) {" +
            "      e.preventDefault();" +
            "      e.stopPropagation();" +
            "    }" +
            "    console.log('SmartFocus: Key pressed:', e.keyCode);" +
            "    " +
            "    // 回车键长按检测" +
            "    if (e.keyCode === 13) {" +
            "      if (enterPressTime === 0) {" +
            "        enterPressTime = Date.now();" +
            "        isLongPress = false;" +
            "        // 设置长按检测" +
            "        setTimeout(() => {" +
            "          if (enterPressTime > 0) {" +
            "            isLongPress = true;" +
            "            togglePlayerFullscreen();" +
            "            console.log('JeffernTV: Long press detected, toggling fullscreen');" +
            "          }" +
            "        }, 800);" +  // 800ms长按
            "        return;" +
            "      }" +
            "    }" +
            "    " +
            "    // SmartFocus导航" +
            "    if ([37, 38, 39, 40].includes(e.keyCode)) {" +
            "      e.preventDefault();" +
            "      " +
            "      switch(e.keyCode) {" +
            "        case 37: // 左" +
            "          SmartFocusManager.navigate('left');" +
            "          break;" +
            "        case 38: // 上" +
            "          SmartFocusManager.navigate('up');" +
            "          break;" +
            "        case 39: // 右" +
            "          SmartFocusManager.navigate('right');" +
            "          break;" +
            "        case 40: // 下" +
            "          SmartFocusManager.navigate('down');" +
            "          break;" +
            "      }" +
            "      return;" +
            "    }" +
            "    " +
            "    // 回车键确认（短按）" +
            "    if (e.keyCode === 13 && !isLongPress) {" +
            "      // 这里不处理，等keyup事件" +
            "      return;" +
            "    }" +
            "  });" +

            // 添加keyup事件处理
            "  document.addEventListener('keyup', function(e) {" +
            "    if (e.keyCode === 13) {" +
            "      var pressDuration = Date.now() - enterPressTime;" +
            "      enterPressTime = 0;" +
            "      " +
            "      if (!isLongPress && pressDuration < 800) {" +
            "        // 短按：SmartFocus点击" +
            "        e.preventDefault();" +
            "        SmartFocusManager.click();" +
            "      }" +
            "      isLongPress = false;" +
            "    }" +
            "  });" +

            // 监听全屏变化
            "  document.addEventListener('fullscreenchange', function() {" +
            "    isPlayerFullscreen = !!document.fullscreenElement;" +
            "  });" +

            // SmartFocus初始化
            "  SmartFocusManager.init = function() {" +
            "    console.log('SmartFocus: Initializing...');" +
            "    console.log('SmartFocus: User Agent:', navigator.userAgent);" +
            "    console.log('SmartFocus: Screen size:', window.innerWidth + 'x' + window.innerHeight);" +
            "    " +
            "    this.initFocusBorder();" +
            "    this.scanFocusableElements();" +
            "    " +
            "    console.log('SmartFocus: Found elements:', this.elements.length);" +
            "    if (this.elements.length > 0) {" +
            "      console.log('SmartFocus: First element:', this.elements[0]);" +
            "      this.setFocus(0);" +
            "    } else {" +
            "      console.warn('SmartFocus: No focusable elements found!');" +
            "      // 尝试更宽泛的选择器" +
            "      setTimeout(() => {" +
            "        console.log('SmartFocus: Retrying with broader selectors...');" +
            "        var allElements = document.querySelectorAll('*');" +
            "        console.log('SmartFocus: Total elements on page:', allElements.length);" +
            "        var clickableElements = document.querySelectorAll('a, button, [onclick], img, div, span');" +
            "        console.log('SmartFocus: Potentially clickable elements:', clickableElements.length);" +
            "        this.scanFocusableElements();" +
            "        if (this.elements.length > 0) {" +
            "          this.setFocus(0);" +
            "        }" +
            "      }, 1000);" +
            "    }" +
            "    this.showHint();" +
            "  };" +

            // 暴露全屏函数到全局
            "  window.togglePlayerFullscreen = togglePlayerFullscreen;" +

            // 延迟初始化，确保页面完全加载
            "  setTimeout(() => SmartFocusManager.init(), 2000);" +
            "  " +
            // 页面变化时重新扫描
            "  var observer = new MutationObserver(function() {" +
            "    setTimeout(() => SmartFocusManager.scanFocusableElements(), 1000);" +
            "  });" +
            "  observer.observe(document.body, { childList: true, subtree: true });" +

            "})();";

        webView.evaluateJavascript(smartFocusScript, null);
        Log.d(TAG, "SmartFocus navigation system injected");
    }
    
    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

}
