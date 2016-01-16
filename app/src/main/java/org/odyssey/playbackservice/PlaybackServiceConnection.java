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
        public void onConnect();

        public void onDisconnect();
    }
}