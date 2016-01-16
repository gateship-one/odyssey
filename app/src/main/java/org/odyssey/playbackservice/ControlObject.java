package org.odyssey.playbackservice;

import org.odyssey.models.TrackModel;

import java.util.ArrayList;

/**
 * Message object which get passed between PlaybackServiceInterface ->
 * PlaybackServiceHandler
 *
 * @author hendrik
 *
 */
public class ControlObject {
    public static enum PLAYBACK_ACTION {
        ODYSSEY_PLAY, ODYSSEY_PAUSE, ODYSSEY_RESUME, ODYSSEY_TOGGLEPAUSE, ODYSSEY_STOP, ODYSSEY_NEXT, ODYSSEY_PREVIOUS, ODYSSEY_SEEKTO, ODYSSEY_JUMPTO, ODYSSEY_REPEAT, ODYSSEY_RANDOM, ODYSSEY_PLAYNEXT, ODYSSEY_ENQUEUETRACK, ODYSSEY_ENQUEUETRACKS, ODYSSEY_DEQUEUETRACK, ODYSSEY_DEQUEUEINDEX, ODYSSEY_DEQUEUETRACKS, ODYSSEY_SETNEXTRACK, ODYSSEY_CLEARPLAYLIST, ODYSSEY_SHUFFLEPLAYLIST, ODYSSEY_PLAYALLTRACKS, ODYSSEY_PLAYALLTRACKSSHUFFLED, ODYSSEY_SAVEPLAYLIST
    }

    private PLAYBACK_ACTION mAction;
    private boolean mBoolparam;
    private int mIntparam;
    private String mStringparam;
    private TrackModel mTrack;
    private ArrayList<TrackModel> mTrackList = null;

    public ControlObject(PLAYBACK_ACTION action) {
        mAction = action;
    }

    public ControlObject(PLAYBACK_ACTION action, boolean param) {
        mBoolparam = param;
        mAction = action;
    }

    public ControlObject(PLAYBACK_ACTION action, int param) {
        mIntparam = param;
        mAction = action;
    }

    public ControlObject(PLAYBACK_ACTION action, String param) {
        mStringparam = param;
        mAction = action;
    }

    public ControlObject(PLAYBACK_ACTION action, ArrayList<TrackModel> list) {
        mTrackList = list;
        mAction = action;
    }

    public ControlObject(PLAYBACK_ACTION action, TrackModel track) {
        mAction = action;
        mTrack = track;
    }

    public PLAYBACK_ACTION getAction() {
        return mAction;
    }

    public String getStringParam() {
        return mStringparam;
    }

    public ArrayList<TrackModel> getTrackList() {
        return mTrackList;
    }

    public int getIntParam() {
        return mIntparam;
    }

    public boolean getBoolParam() {
        return mBoolparam;
    }

    public TrackModel getTrack() {
        return mTrack;
    }
}
