package ru.nailsoft.files.ui.base;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Toast;

import ru.nailsoft.files.utils.AppSettingsUtils;

import static ru.nailsoft.files.App.prefs;
import static ru.nailsoft.files.diagnostics.Logger.traceUi;


public class BaseFragment extends Fragment implements FragmentInterface {

    private boolean requestPermission;
    private boolean requestSettings;
    private int explanationStrResId;

    public BaseFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        traceUi(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        traceUi(this);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        traceUi(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        traceUi(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        traceUi(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        traceUi(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        traceUi(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        traceUi(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        traceUi(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        traceUi(this);
    }

    public boolean requestPermissions(int reqCode, @StringRes int explanationStrResId, String... permissions) {
        FragmentActivity context = getActivity();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.explanationStrResId = explanationStrResId;
            requestPermission = false;
            requestSettings = false;
            for (String permission : permissions) {
                if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(context, permission)) {
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
            FragmentActivity context = getActivity();
            if (this.explanationStrResId != 0) {
                Toast.makeText(context, this.explanationStrResId, Toast.LENGTH_SHORT).show();
            }
            AppSettingsUtils.openAppSettings(context);
        } else {
            for (int result : grantResults) {
                if(result != PackageManager.PERMISSION_GRANTED)
                    return;
            }

            onRequestedPermissionsGranted(requestCode, permissions, grantResults);
        }
    }

    protected void onRequestedPermissionsGranted(int requestCode, String[] permissions, int[] grantResults) {

    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public boolean onUpPressed() {
        return false;
    }
}
