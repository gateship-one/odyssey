/*
 * Copyright (C) 2016  Hendrik Borghorst & Frederik Luetkes
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

package org.odyssey.playbackservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

public class RemoteControlReceiver extends BroadcastReceiver {
    private final static String TAG = "OdysseyRemoteReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event.getAction() == KeyEvent.ACTION_UP) {
                Log.v(TAG, "Received key: " + event);
                if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_NEXT) {
                    Intent nextIntent = new Intent(PlaybackService.ACTION_NEXT);
                    context.sendBroadcast(nextIntent);
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                    Intent nextIntent = new Intent(PlaybackService.ACTION_PREVIOUS);
                    context.sendBroadcast(nextIntent);
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                    Intent nextIntent = new Intent(PlaybackService.ACTION_TOGGLEPAUSE);
                    context.sendBroadcast(nextIntent);
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY) {
                    Intent nextIntent = new Intent(PlaybackService.ACTION_PLAY);
                    context.sendBroadcast(nextIntent);
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                    Intent nextIntent = new Intent(PlaybackService.ACTION_PAUSE);
                    context.sendBroadcast(nextIntent);
                }
            }
        }
    }

}