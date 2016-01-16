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