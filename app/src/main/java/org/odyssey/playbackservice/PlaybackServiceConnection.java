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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class PlaybackServiceConnection implements ServiceConnection {

    private static final String TAG = "ServiceConnection";
    private IOdysseyPlaybackService mPlaybackService;

    private Context mContext;
    private ConnectionNotifier mNotifier;

    public PlaybackServiceConnection(Context context) {
        mContext = context;
        mPlaybackService = null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.v(TAG, "Service connection created");
        mPlaybackService = IOdysseyPlaybackService.Stub.asInterface(service);
        if (mPlaybackService != null) {
            Log.v(TAG, "Got interface");
        } else {
            Log.v(TAG, "No interface -_-");
        }
        if (mNotifier != null) {
            mNotifier.onConnect();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.v(TAG, "Service disconnected");
        mPlaybackService = null;
        if (mNotifier != null) {
            mNotifier.onDisconnect();
        }
        openConnection();
    }

    public void openConnection() {
        Intent serviceStartIntent = new Intent(mContext, PlaybackService.class);
        mContext.bindService(serviceStartIntent, this, Context.BIND_AUTO_CREATE);
    }

    public void closeConnection() {
        mContext.unbindService(this);
    }

    public synchronized IOdysseyPlaybackService getPBS() throws RemoteException {
        if (mPlaybackService != null) {
            return mPlaybackService;
        } else {
            throw new RemoteException();
        }

    }

    public void setNotifier(ConnectionNotifier notifier) {
        mNotifier = notifier;
    }

    public interface ConnectionNotifier {
        void onConnect();

        void onDisconnect();
    }
}