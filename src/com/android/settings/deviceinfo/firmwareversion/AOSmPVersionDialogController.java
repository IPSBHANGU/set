/*
 * Copyright (C) 2018 AospExtended ROM Project
 * Copyright (C) 2019 AOSmP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.text.BidiFormatter;
import android.util.Log;
import android.view.View;

import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;

public class AOSmPVersionDialogController  implements View.OnClickListener {

    private static final String TAG = "ExtfirmwareDialogCtrl";
    private static final int DELAY_TIMER_MILLIS = 500;
    private static final int ACTIVITY_TRIGGER_COUNT = 3;

    @VisibleForTesting
    static final int AOSMP_VERSION_VALUE_ID = R.id.aosmp_version_value;
    @VisibleForTesting
    static final int AOSMP_VERSION_LABEL_ID = R.id.aosmp_version_label;

    private final FirmwareVersionDialogFragment mDialog;
    private final Context mContext;
    private final UserManager mUserManager;
    private final long[] mHits = new long[ACTIVITY_TRIGGER_COUNT];

    private RestrictedLockUtils.EnforcedAdmin mFunDisallowedAdmin;
    private boolean mFunDisallowedBySystem;

    public AOSmPVersionDialogController(FirmwareVersionDialogFragment dialog) {
        mDialog = dialog;
        mContext = dialog.getContext();
        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
    }

    @Override
    public void onClick(View v) {
        arrayCopy();
        mHits[mHits.length - 1] = SystemClock.uptimeMillis();
        if (mHits[0] >= (SystemClock.uptimeMillis() - DELAY_TIMER_MILLIS)) {
            if (mUserManager.hasUserRestriction(UserManager.DISALLOW_FUN)) {
                if (mFunDisallowedAdmin != null && !mFunDisallowedBySystem) {
                    RestrictedLockUtils.sendShowAdminSupportDetailsIntent(mContext,
                            mFunDisallowedAdmin);
                }
                Log.d(TAG, "Sorry, no fun for you!");
                return;
            }

            final Intent intent = new Intent(Intent.ACTION_MAIN)
                    .setClassName(
                            "android", com.android.internal.app.PlatLogoActivity.class.getName());
            try {
                mContext.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Unable to start activity " + intent.toString());
            }
        }
    }

    /**
     * Updates the extended version to the dialog.
     */
    public void initialize() {
        initializeAdminPermissions();
        registerClickListeners();

        mDialog.setText(AOSMP_VERSION_VALUE_ID,
                BidiFormatter.getInstance().unicodeWrap(Build.AOSMP_DISPLAY_VERSION));
    }

    private void registerClickListeners() {
        mDialog.registerClickListener(AOSMP_VERSION_LABEL_ID, this /* listener */);
        mDialog.registerClickListener(AOSMP_VERSION_VALUE_ID, this /* listener */);
    }

    /**
     * Copies the array onto itself to remove the oldest hit.
     */
    @VisibleForTesting
    void arrayCopy() {
        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
    }

    @VisibleForTesting
    void initializeAdminPermissions() {
        mFunDisallowedAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_FUN, UserHandle.myUserId());
        mFunDisallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(
                mContext, UserManager.DISALLOW_FUN, UserHandle.myUserId());
    }
}
