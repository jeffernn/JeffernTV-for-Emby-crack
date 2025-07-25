package com.jeffern.movie;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class PreferenceManagerTest {

    private PreferenceManager preferenceManager;
    private Context context;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        context = RuntimeEnvironment.getApplication();
        preferenceManager = new PreferenceManager(context);
    }

    @Test
    public void testFirstLaunch() {
        // 默认应该是首次启动
        assertTrue(preferenceManager.isFirstLaunch());
        
        // 设置为非首次启动
        preferenceManager.setFirstLaunch(false);
        assertFalse(preferenceManager.isFirstLaunch());
    }

    @Test
    public void testWebsiteConfig() {
        String testUrl = "https://test.website.com";

        // 保存网站配置
        preferenceManager.saveWebsiteConfig(testUrl);

        // 验证配置
        assertFalse(preferenceManager.isFirstLaunch());
        assertEquals(PreferenceManager.SERVICE_WEBSITE, preferenceManager.getServiceType());
        assertEquals(testUrl, preferenceManager.getUrl());
        assertTrue(preferenceManager.isWebsiteService());
        assertFalse(preferenceManager.isEmbyService());
    }

    @Test
    public void testEmbyConfig() {
        String testUrl = "https://test.emby.com";
        String testUsername = "testuser";
        String testPassword = "testpass";
        
        // 保存Emby配置
        preferenceManager.saveEmbyConfig(testUrl, testUsername, testPassword);
        
        // 验证配置
        assertFalse(preferenceManager.isFirstLaunch());
        assertEquals(PreferenceManager.SERVICE_EMBY, preferenceManager.getServiceType());
        assertEquals(testUrl, preferenceManager.getUrl());
        assertEquals(testUsername, preferenceManager.getUsername());
        assertEquals(testPassword, preferenceManager.getPassword());
        assertTrue(preferenceManager.isEmbyService());
        assertFalse(preferenceManager.isMoonTVService());
    }

    @Test
    public void testEmbyCredentialsWithEmptyValues() {
        // 测试空凭据时返回空值
        preferenceManager.saveEmbyConfig("https://test.com", "", "");
        String[] credentials = preferenceManager.getEmbyCredentials();

        assertEquals("", credentials[0]);
        assertEquals("", credentials[1]);
    }

    @Test
    public void testEmbyCredentialsWithCustomValues() {
        // 测试自定义凭据
        String customUser = "myuser";
        String customPass = "mypass";
        preferenceManager.saveEmbyConfig("https://test.com", customUser, customPass);
        String[] credentials = preferenceManager.getEmbyCredentials();
        
        assertEquals(customUser, credentials[0]);
        assertEquals(customPass, credentials[1]);
    }

    @Test
    public void testUrlFormatting() {
        // 测试URL自动添加协议
        preferenceManager.saveEmbyConfig("test.com", "user", "pass");
        assertEquals("https://test.com", preferenceManager.getUrl());
        
        // 测试已有协议的URL不被修改
        preferenceManager.saveEmbyConfig("http://test.com", "user", "pass");
        assertEquals("http://test.com", preferenceManager.getUrl());
    }

    @Test
    public void testConfigurationStatus() {
        // 初始状态应该未配置
        assertFalse(preferenceManager.isConfigured());
        
        // 配置后应该显示已配置
        preferenceManager.saveWebsiteConfig("https://test.com");
        assertTrue(preferenceManager.isConfigured());
    }

    @Test
    public void testResetToFirstLaunch() {
        // 先配置
        preferenceManager.saveWebsiteConfig("https://test.com");
        assertTrue(preferenceManager.isConfigured());
        
        // 重置
        preferenceManager.resetToFirstLaunch();
        assertTrue(preferenceManager.isFirstLaunch());
        assertFalse(preferenceManager.isConfigured());
    }

    @Test
    public void testConfigSummary() {
        // 未配置时
        assertEquals("未配置", preferenceManager.getConfigSummary());
        
        // 网站配置
        preferenceManager.saveWebsiteConfig("https://test.website.com");
        assertTrue(preferenceManager.getConfigSummary().contains("影视封装"));
        
        // Emby配置
        preferenceManager.saveEmbyConfig("https://emby.test.com", "testuser", "testpass");
        String summary = preferenceManager.getConfigSummary();
        assertTrue(summary.contains("Emby"));
        assertTrue(summary.contains("testuser"));
    }
}
