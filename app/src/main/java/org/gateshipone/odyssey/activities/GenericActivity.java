/*
 * Copyright (C) 2023 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/odyssey/blob/master/AUTHORS.md>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.odyssey.activities;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.playbackservice.IOdysseyPlaybackService;
import org.gateshipone.odyssey.playbackservice.PlaybackServiceConnection;
import org.gateshipone.odyssey.playbackservice.managers.PlaybackServiceStatusHelper;

public abstract class GenericActivity extends AppCompatActivity {

    public ProgressDialog mProgressDialog;

    private PBSOperationFinishedReceiver mPBSOperationFinishedReceiver = null;

    @Nullable
    private PlaybackServiceConnection mServiceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Read theme preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String themePref = sharedPref.getString(getString(R.string.pref_theme_key), getString(R.string.pref_theme_default));
        String themeMaterialPref = sharedPref.getString(getString(R.string.pref_materialyou_theme_selector_key), getString(R.string.pref_theme_materialyou_default));
        boolean darkTheme = sharedPref.getBoolean(getString(R.string.pref_dark_theme_key), getResources().getBoolean(R.bool.pref_theme_dark_default));
        boolean legacyTheme = sharedPref.getBoolean(getString(R.string.pref_legacy_theme_key), getResources().getBoolean(R.bool.pref_theme_legacy_default));
        if (darkTheme && legacyTheme) {
            if (themePref.equals(getString(R.string.pref_indigo_key))) {
                setTheme(R.style.AppTheme_indigo);
            } else if (themePref.equals(getString(R.string.pref_orange_key))) {
                setTheme(R.style.AppTheme_orange);
            } else if (themePref.equals(getString(R.string.pref_deeporange_key))) {
                setTheme(R.style.AppTheme_deepOrange);
            } else if (themePref.equals(getString(R.string.pref_blue_key))) {
                setTheme(R.style.AppTheme_blue);
            } else if (themePref.equals(getString(R.string.pref_darkgrey_key))) {
                setTheme(R.style.AppTheme_darkGrey);
            } else if (themePref.equals(getString(R.string.pref_brown_key))) {
                setTheme(R.style.AppTheme_brown);
            } else if (themePref.equals(getString(R.string.pref_lightgreen_key))) {
                setTheme(R.style.AppTheme_lightGreen);
            } else if (themePref.equals(getString(R.string.pref_red_key))) {
                setTheme(R.style.AppTheme_red);
            } else if (themePref.equals(getString(R.string.pref_oleddark_key))) {
                setTheme(R.style.AppTheme_oledDark);
            } else if (themePref.equals(getString(R.string.pref_materialyou_auto_key))) {
                setTheme(R.style.AppTheme_materialyou);
            } else if (themePref.equals(getString(R.string.pref_materialyou_key))) {
                setTheme(R.style.AppTheme_materialyou_dark);
            }
        } else if (!darkTheme && legacyTheme){
            if (themePref.equals(getString(R.string.pref_indigo_key))) {
                setTheme(R.style.AppTheme_light_indigo);
            } else if (themePref.equals(getString(R.string.pref_orange_key))) {
                setTheme(R.style.AppTheme_light_orange);
            } else if (themePref.equals(getString(R.string.pref_deeporange_key))) {
                setTheme(R.style.AppTheme_light_deepOrange);
            } else if (themePref.equals(getString(R.string.pref_blue_key))) {
                setTheme(R.style.AppTheme_light_blue);
            } else if (themePref.equals(getString(R.string.pref_darkgrey_key))) {
                setTheme(R.style.AppTheme_light_darkGrey);
            } else if (themePref.equals(getString(R.string.pref_brown_key))) {
                setTheme(R.style.AppTheme_light_brown);
            } else if (themePref.equals(getString(R.string.pref_lightgreen_key))) {
                setTheme(R.style.AppTheme_light_lightGreen);
            } else if (themePref.equals(getString(R.string.pref_red_key))) {
                setTheme(R.style.AppTheme_light_red);
            } else if (themePref.equals(getString(R.string.pref_oleddark_key))) {
                setTheme(R.style.AppTheme_oledDark);
            } else if (themePref.equals(getString(R.string.pref_materialyou_auto_key))) {
                setTheme(R.style.AppTheme_materialyou);
            } else if (themePref.equals(getString(R.string.pref_materialyou_key))) {
                setTheme(R.style.AppTheme_materialyou_light);
            }
        } else if (darkTheme && !legacyTheme) {
            if (themeMaterialPref.equals(getString(R.string.pref_indigo_key))) {
                setTheme(R.style.AppTheme_materialyou_indigo_dark);
            } else if (themeMaterialPref.equals(getString(R.string.pref_orange_key))) {
                setTheme(R.style.AppTheme_materialyou_orange_dark);
            } else if (themeMaterialPref.equals(getString(R.string.pref_blue_key))) {
                setTheme(R.style.AppTheme_materialyou_blue_dark);
            } else if (themeMaterialPref.equals(getString(R.string.pref_lightgreen_key))) {
                setTheme(R.style.AppTheme_materialyou_green_dark);
            } else if (themeMaterialPref.equals(getString(R.string.pref_red_key))) {
                setTheme(R.style.AppTheme_materialyou_red_dark);
            } else if (themeMaterialPref.equals(getString(R.string.pref_materialyou_auto_key))) {
                setTheme(R.style.AppTheme_materialyou);
            } else if (themeMaterialPref.equals(getString(R.string.pref_materialyou_key))) {
                setTheme(R.style.AppTheme_materialyou_dark);
            } else {
                setTheme(R.style.AppTheme_materialyou);
            }
        } else {
            if (themeMaterialPref.equals(getString(R.string.pref_indigo_key))) {
                setTheme(R.style.AppTheme_materialyou_indigo_light);
            } else if (themeMaterialPref.equals(getString(R.string.pref_orange_key)))  {
                setTheme(R.style.AppTheme_materialyou_orange_light);
            } else if (themeMaterialPref.equals(getString(R.string.pref_blue_key))) {
                setTheme(R.style.AppTheme_materialyou_blue_light);
            } else if (themeMaterialPref.equals(getString(R.string.pref_lightgreen_key))) {
                setTheme(R.style.AppTheme_materialyou_green_light);
            } else if (themeMaterialPref.equals(getString(R.string.pref_red_key))) {
                setTheme(R.style.AppTheme_materialyou_red_light);
            } else if (themeMaterialPref.equals(getString(R.string.pref_materialyou_auto_key))) {
                setTheme(R.style.AppTheme_materialyou);
            } else if (themeMaterialPref.equals(getString(R.string.pref_materialyou_key))) {
                setTheme(R.style.AppTheme_materialyou_light);
            } else {
                setTheme(R.style.AppTheme_materialyou);
            }
        }

        super.onCreate(savedInstanceState);

        // setup progressdialog
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(R.string.playbackservice_working));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setIndeterminate(true);

        mServiceConnection = new PlaybackServiceConnection(getApplicationContext());

        // Create service connection
        mServiceConnection.setNotifier(new ServiceConnectionListener());

        // suggest that we want to change the music audio stream by hardware volume controls even
        // if no music is currently played
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mPBSOperationFinishedReceiver != null) {
            unregisterReceiver(mPBSOperationFinishedReceiver);
            mPBSOperationFinishedReceiver = null;
        }
        mPBSOperationFinishedReceiver = new PBSOperationFinishedReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(PlaybackServiceStatusHelper.MESSAGE_IDLE);
        filter.addAction(PlaybackServiceStatusHelper.MESSAGE_WORKING);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(mPBSOperationFinishedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(mPBSOperationFinishedReceiver, filter);
        }

        if (mServiceConnection != null) {
            mServiceConnection.openConnection();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mPBSOperationFinishedReceiver != null) {
            unregisterReceiver(mPBSOperationFinishedReceiver);
            mPBSOperationFinishedReceiver = null;
        }

        // Close connection to unbind from service to allow it to be stopped by the system
        if (mServiceConnection != null) {
            mServiceConnection.closeConnection();
        }
    }

    public IOdysseyPlaybackService getPlaybackService() throws RemoteException {
        if (mServiceConnection != null) {
            return mServiceConnection.getPBS();
        } else {
            throw new RemoteException();
        }
    }

    abstract void onServiceConnected();

    abstract void onServiceDisconnected();

    private class PBSOperationFinishedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (PlaybackServiceStatusHelper.MESSAGE_WORKING.equals(intent.getAction())) {
                runOnUiThread(() -> {
                    if (mProgressDialog != null) {
                        mProgressDialog.show();
                    }
                });
            } else if (PlaybackServiceStatusHelper.MESSAGE_IDLE.equals(intent.getAction())) {
                runOnUiThread(() -> {
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                    }
                });
            }
        }
    }

    private class ServiceConnectionListener implements PlaybackServiceConnection.ConnectionNotifier {

        @Override
        public void onConnect() {
            try {
                if (mServiceConnection != null && mServiceConnection.getPBS().isBusy()) {
                    // pbs is still working so show the progress dialog again
                    mProgressDialog.show();
                } else {
                    // pbs is not working so dismiss the progress dialog
                    mProgressDialog.dismiss();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                // error occured so dismiss the progress dialog anyway
                mProgressDialog.dismiss();
            }

            onServiceConnected();
        }

        @Override
        public void onDisconnect() {
            // disconnected so dismiss dialog anyway
            mProgressDialog.dismiss();

            onServiceDisconnected();
        }
    }
}
