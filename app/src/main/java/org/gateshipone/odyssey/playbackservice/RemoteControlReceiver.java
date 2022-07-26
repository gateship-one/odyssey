/*
 * Copyright (C) 2020 Team Gateship-One
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

package org.gateshipone.odyssey.playbackservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

import org.gateshipone.odyssey.BuildConfig;

public class RemoteControlReceiver extends BroadcastReceiver {
    private static final String TAG = "OdysseyRemoteReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {

            final KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            if (event.getAction() == KeyEvent.ACTION_UP) {

                if (BuildConfig.DEBUG) {
                    Log.v(TAG, "Received key: " + event);
                }

                Intent nextIntent;

                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        nextIntent = new Intent(PlaybackService.ACTION_NEXT);
                        nextIntent.setPackage(context.getPackageName());
                        context.sendBroadcast(nextIntent);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        nextIntent = new Intent(PlaybackService.ACTION_PREVIOUS);
                        nextIntent.setPackage(context.getPackageName());
                        context.sendBroadcast(nextIntent);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        nextIntent = new Intent(PlaybackService.ACTION_TOGGLEPAUSE);
                        nextIntent.setPackage(context.getPackageName());
                        context.sendBroadcast(nextIntent);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        nextIntent = new Intent(PlaybackService.ACTION_PLAY);
                        nextIntent.setPackage(context.getPackageName());
                        context.sendBroadcast(nextIntent);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        nextIntent = new Intent(PlaybackService.ACTION_PAUSE);
                        nextIntent.setPackage(context.getPackageName());
                        context.sendBroadcast(nextIntent);
                        break;
                }
            }
        }
    }
}
