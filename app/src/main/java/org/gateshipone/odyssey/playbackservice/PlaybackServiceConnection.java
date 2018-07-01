/*
 * Copyright (C) 2018 Team Gateship-One
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

public class PlaybackServiceConnection implements ServiceConnection {

    private static final String TAG = "ServiceConnection";

    /**
     * The service interface that is created when the connection is established.
     */
    private IOdysseyPlaybackService mPlaybackService;

    /**
     * Context used for binding to the service
     */
    private Context mContext;

    /**
     * Callback handler for connection state changes
     */
    private ConnectionNotifier mNotifier;

    public PlaybackServiceConnection(Context context) {
        mContext = context;
        mPlaybackService = null;
    }

    /**
     * Called when the connection is established successfully
     *
     * @param name    Name of the connected component
     * @param service Service that the connection was established to
     */
    @Override
    public synchronized void onServiceConnected(ComponentName name, IBinder service) {
        mPlaybackService = IOdysseyPlaybackService.Stub.asInterface(service);
        if (mPlaybackService != null && mNotifier != null) {
            mNotifier.onConnect();
        }
    }

    /**
     * Called when the service connection was disconnected for some reason (crash?)
     *
     * @param name Name of the closed component
     */
    @Override
    public synchronized void onServiceDisconnected(ComponentName name) {
        mPlaybackService = null;
        if (mNotifier != null) {
            mNotifier.onDisconnect();
        }
        //openConnection();
    }

    /**
     * This initiates the connection to the PlaybackService by binding to it
     */
    public void openConnection() {
        Intent serviceStartIntent = new Intent(mContext, PlaybackService.class);
        mContext.bindService(serviceStartIntent, this, Context.BIND_AUTO_CREATE);
    }

    /**
     * Disconnects the connection by unbinding from the service (not needed anymore)
     */
    public synchronized void closeConnection() {
        mContext.unbindService(this);
        mPlaybackService = null;
        if (mNotifier != null) {
            mNotifier.onDisconnect();
        }
    }

    public synchronized IOdysseyPlaybackService getPBS() throws RemoteException {
        if (mPlaybackService != null) {
            return mPlaybackService;
        } else {
            throw new RemoteException();
        }

    }

    /**
     * Sets an callback handler
     *
     * @param notifier Callback handler for connection state changes
     */
    public void setNotifier(ConnectionNotifier notifier) {
        mNotifier = notifier;
    }

    public interface ConnectionNotifier {
        void onConnect();

        void onDisconnect();
    }
}