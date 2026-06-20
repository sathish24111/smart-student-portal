package com.smartportal;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private WebView myWebView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar loadingProgressBar;
    private RelativeLayout splashOverlay;
    private LinearLayout offlineOverlay;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private String pendingOrigin = null;
    private GeolocationPermissions.Callback pendingCallback = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Customize status bar color for seamless premium look (matching violet theme `#5046e6`)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(Color.parseColor("#5046e6"));
        }

        // Initialize UI Elements
        myWebView = findViewById(R.id.webView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        splashOverlay = findViewById(R.id.splashOverlay);
        offlineOverlay = findViewById(R.id.offlineOverlay);
        Button btnRetry = findViewById(R.id.btnRetry);

        // Configure WebView Settings
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setGeolocationEnabled(true);

        // Bind JavaScript Interface Bridge
        myWebView.addJavascriptInterface(new WebAppInterface(this), "Android");

        // Set up Pull-To-Refresh logic
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                myWebView.reload();
            }
        });

        // Set up Offline recovery retry logic
        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                offlineOverlay.setVisibility(View.GONE);
                splashOverlay.setVisibility(View.VISIBLE);
                myWebView.reload();
            }
        });

        // Set WebView Client
        myWebView.setWebViewClient(new WebViewClient() {
            private boolean hasError = false;

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                hasError = false;
                loadingProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                loadingProgressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                
                if (!hasError) {
                    splashOverlay.setVisibility(View.GONE);
                    offlineOverlay.setVisibility(View.GONE);
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                hasError = true;
                offlineOverlay.setVisibility(View.VISIBLE);
                splashOverlay.setVisibility(View.GONE);
                loadingProgressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onReceivedError(WebView view, android.webkit.WebResourceRequest request, android.webkit.WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    hasError = true;
                    offlineOverlay.setVisibility(View.VISIBLE);
                    splashOverlay.setVisibility(View.GONE);
                    loadingProgressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });

        // Set Chrome Client for Geolocation & Progress Loading bar
        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                loadingProgressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    loadingProgressBar.setVisibility(View.GONE);
                } else {
                    loadingProgressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) 
                        != PackageManager.PERMISSION_GRANTED) {
                    
                    pendingOrigin = origin;
                    pendingCallback = callback;
                    
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_PERMISSION_REQUEST_CODE);
                } else {
                    callback.invoke(origin, true, false);
                }
            }
        });

        // Load live deployed site
        myWebView.loadUrl("https://sathish24111.github.io/smart-student-portal/");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (pendingCallback != null && pendingOrigin != null) {
                pendingCallback.invoke(pendingOrigin, granted, false);
                pendingOrigin = null;
                pendingCallback = null;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (myWebView.canGoBack()) {
            myWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // Biometric Prompter logic
    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                myWebView.evaluateJavascript("console.warn('Biometric auth error: " + errString + "');", null);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                
                SharedPreferences sharedPreferences = getSharedPreferences("BiometricPrefs", MODE_PRIVATE);
                String username = sharedPreferences.getString("username", "");
                String password = sharedPreferences.getString("password", "");
                String role = sharedPreferences.getString("role", "");

                // Run JS callback to fill credentials and submit form
                myWebView.evaluateJavascript("if (window.onBiometricSuccess) { window.onBiometricSuccess('" + 
                        username + "', '" + password + "', '" + role + "'); }", null);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                myWebView.evaluateJavascript("console.warn('Biometric auth failed');", null);
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Quick Unlock")
                .setSubtitle("Log in to Smart Student Portal using your biometric credential")
                .setNegativeButtonText("Cancel")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    // JavaScript Bridge implementation
    public class WebAppInterface {
        private Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public boolean isBiometricsSupported() {
            BiometricManager biometricManager = BiometricManager.from(mContext);
            int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
            return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS;
        }

        @JavascriptInterface
        public void registerCredentials(String username, String password, String role) {
            SharedPreferences sharedPreferences = mContext.getSharedPreferences("BiometricPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("username", username);
            editor.putString("password", password);
            editor.putString("role", role);
            editor.putBoolean("isSaved", true);
            editor.apply();
        }

        @JavascriptInterface
        public boolean isCredentialsSaved() {
            SharedPreferences sharedPreferences = mContext.getSharedPreferences("BiometricPrefs", MODE_PRIVATE);
            return sharedPreferences.getBoolean("isSaved", false);
        }

        @JavascriptInterface
        public void triggerBiometricPrompt() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showBiometricPrompt();
                }
            });
        }
    }
}
