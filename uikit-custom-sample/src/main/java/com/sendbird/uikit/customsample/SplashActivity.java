package com.sendbird.uikit.customsample;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.sendbird.uikit.SendbirdUIKit;
import com.sendbird.uikit.customsample.utils.PreferenceUtils;
import com.sendbird.uikit.customsample.widgets.WaitingDialog;
import com.sendbird.uikit.log.Logger;

/**
 * Displays a splash screen.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        BaseApplication.initStateChanges().observe(this, initState -> {
            Logger.i("++ init state : %s", initState);
            WaitingDialog.dismiss();
            switch (initState) {
                case MIGRATING:
                    WaitingDialog.show(SplashActivity.this);
                    break;
                case FAILED:
                case SUCCEED:
                    String userId = PreferenceUtils.getUserId();
                    if (!TextUtils.isEmpty(userId)) {
                        SendbirdUIKit.connect((user, e) -> {
                            startActivity(getNextIntent());
                            finish();
                        });
                    } else {
                        startActivity(getNextIntent());
                        finish();
                    }
                    break;
            }
        });
    }

    private Intent getNextIntent() {
        String userId = PreferenceUtils.getUserId();
        if (!TextUtils.isEmpty(userId)) {
            return new Intent(SplashActivity.this, HomeActivity.class);
        }

        return new Intent(SplashActivity.this, LoginActivity.class);
    }
}
