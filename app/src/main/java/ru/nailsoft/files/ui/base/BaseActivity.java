package ru.nailsoft.files.ui.base;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import ru.nailsoft.files.utils.AppSettingsUtils;
import ru.nailsoft.files.R;

import static ru.nailsoft.files.App.prefs;
import static ru.nailsoft.files.diagnostics.Logger.traceUi;

public abstract class BaseActivity extends AppCompatActivity {
    private Toolbar mToolbar;
    private boolean resumed;

    private boolean requestPermission;
    private boolean requestSettings;
    private int explanationStrResId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        traceUi(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        traceUi(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        traceUi(this);

//        final Toolbar toolbar = getToolbar();
//        if (toolbar != null) {
//            toolbar.setNavigationOnClickListener(this::onUpButtonClick);
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        traceUi(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        traceUi(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        traceUi(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        traceUi(this);

        if (Build.VERSION_CODES.LOLLIPOP <= Build.VERSION.SDK_INT && Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        resumed = true;
    }

    @Override
    protected void onPause() {
        resumed = false;
        super.onPause();
        traceUi(this);
    }

    public boolean requestPermissions(int reqCode, @StringRes int explanationStrResId, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.explanationStrResId = explanationStrResId;
            requestPermission = false;
            requestSettings = false;
            for (String permission : permissions) {
                if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, permission)) {
                    if (!prefs().isPermissionRequested(permission) || shouldShowRequestPermissionRationale(permission)) {
                        requestPermission = true;
                    } else {
                        requestSettings = true;
                    }
                }
            }

            if (requestPermission || requestSettings) {
                requestPermissions(permissions, reqCode);
                return false;
            }
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0)
            return;

        for (String permission : permissions) {
            prefs().onPermissionRequested(permission);
        }

        if (requestSettings) {
            if (this.explanationStrResId != 0) {
                Toast.makeText(this, this.explanationStrResId, Toast.LENGTH_SHORT).show();
            }
            AppSettingsUtils.openAppSettings(this);
        } else {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED)
                    return;
            }

            onRequestedPermissionsGranted(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        traceUi(this);
    }

    protected void onRequestedPermissionsGranted(int requestCode, String[] permissions, int[] grantResults) {

    }


    public Toolbar getToolbar() {
        if (mToolbar == null) {
            mToolbar = findViewById(R.id.toolbar);
            if (mToolbar != null)
                setSupportActionBar(mToolbar);
        }
        return mToolbar;
    }

    protected void onUpButtonClick(View view) {
        final Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.content);
        boolean handled = false;
        if (currentFragment != null && currentFragment instanceof FragmentInterface)
            handled = ((FragmentInterface) currentFragment).onUpPressed();

        if (!handled)
            finish();
    }



    public interface FragmentInterface {

        /**
         * handle system back button
         * @return true if consumed, false otherwise
         */
        boolean onBackPressed();

        /**
         * handle toolbar up button
         * @return true if consumed, false otherwise
         */
        boolean onUpPressed() ;
    }


}
