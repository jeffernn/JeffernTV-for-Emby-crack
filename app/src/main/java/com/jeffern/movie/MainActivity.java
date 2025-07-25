package com.jeffern.movie;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private PreferenceManager prefManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefManager = new PreferenceManager(this);

        // 检查是否首次启动
        if (!prefManager.isFirstLaunch()) {
            // 不是首次启动，直接跳转到WebView
            launchWebView();
            return;
        }
        
        setContentView(R.layout.activity_main);

        Button btnEmby = findViewById(R.id.btn_emby);

        // 设置默认焦点
        btnEmby.requestFocus();

        btnEmby.setOnClickListener(v -> showEmbyConfigDialog());
    }


    
    private void showEmbyConfigDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_emby_config, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        EditText etUrl = dialogView.findViewById(R.id.et_url);
        EditText etUsername = dialogView.findViewById(R.id.et_username);
        EditText etPassword = dialogView.findViewById(R.id.et_password);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        
        // 设置默认焦点
        etUrl.requestFocus();
        
        btnConfirm.setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // 验证必填字段
            if (url.isEmpty()) {
                Toast.makeText(this, "请输入服务器地址", Toast.LENGTH_SHORT).show();
                etUrl.requestFocus();
                return;
            }

            if (username.isEmpty()) {
                Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show();
                etUsername.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
                etPassword.requestFocus();
                return;
            }

            // 确保URL格式正确
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            // 保存Emby配置
            prefManager.saveEmbyConfig(url, username, password);

            dialog.dismiss();
            launchWebView();
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private void launchWebView() {
        Intent intent = new Intent(this, WebViewActivity.class);
        startActivity(intent);
        finish();
    }
}
