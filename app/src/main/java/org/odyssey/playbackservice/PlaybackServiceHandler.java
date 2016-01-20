package org.odyssey.playbackservice;

import java.lang.ref.WeakReference;
import java.util.concurrent.Semaphore;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class PlaybackServiceHandler extends Handler {
    private final WeakReference<PlaybackService> mService;

    private Semaphore mLock;

    public PlaybackServiceHandler(Looper looper, PlaybackService service) {
        super(looper);
        mService = new WeakReference<PlaybackService>(service);
        mLock = new Semaphore(1);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        ControlObject msgObj = (ControlObject) msg.obj;

        // Check if object is received
        if (msgObj != null && mLock.tryAcquire()) {
            // Parse message
            switch(msgObj.getAction()) {
                case ODYSSEY_PLAY:
                    mService.get().playURI(msgObj.getTrack());
                    break;
                case ODYSSEY_PAUSE:
                    break;
                case ODYSSEY_RESUME:
                    mService.get().resume();
                    break;
                case ODYSSEY_TOGGLEPAUSE:
                    mService.get().togglePause();
                    break;
                case ODYSSEY_STOP:
                    mService.get().stop();
                    break;
                case ODYSSEY_NEXT:
                    mService.get().setNextTrack();
                    break;
                case ODYSSEY_PREVIOUS:
                    mService.get().setPreviousTrack();
                    break;
                case ODYSSEY_SEEKTO:
                    mService.get().seekTo(msgObj.getIntParam());
                    break;
                case ODYSSEY_JUMPTO:
                    mService.get().jumpToIndex(msgObj.getIntParam(), true);
                    break;
                case ODYSSEY_REPEAT:
                    mService.get().setRepeat(msgObj.getIntParam());
                    break;
                case ODYSSEY_RANDOM:
                    mService.get().setRandom(msgObj.getIntParam());
                    break;
                case ODYSSEY_PLAYNEXT:
                    mService.get().enqueueAsNextTrack(msgObj.getTrack());
                    break;
                case ODYSSEY_ENQUEUETRACK:
                    mService.get().enqueueTrack(msgObj.getTrack());
                    break;
                case ODYSSEY_ENQUEUETRACKS:
                    mService.get().enqueueTracks(msgObj.getTrackList());
                    break;
                case ODYSSEY_DEQUEUETRACK:
                    break;
                case ODYSSEY_DEQUEUEINDEX:
                    mService.get().dequeueTrack(msgObj.getIntParam());
                    break;
                case ODYSSEY_DEQUEUETRACKS:
                    break;
                case ODYSSEY_SETNEXTRACK:
                    break;
                case ODYSSEY_CLEARPLAYLIST:
                    mService.get().clearPlaylist();
                    break;
                case ODYSSEY_SHUFFLEPLAYLIST:
                    mService.get().shufflePlaylist();
                    break;
                case ODYSSEY_PLAYALLTRACKS:
                    mService.get().playAllTracks();
                    break;
                case ODYSSEY_PLAYALLTRACKSSHUFFLED:
                    mService.get().playAllTracksShuffled();
                    break;
                case ODYSSEY_SAVEPLAYLIST:
                    mService.get().savePlaylist(msgObj.getStringParam());
                    break;
            }

            mLock.release();
        }
    }
}
