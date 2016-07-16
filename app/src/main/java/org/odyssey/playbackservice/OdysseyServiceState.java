package org.odyssey.playbackservice;

public class OdysseyServiceState {

    public int mTrackNumber;
    public int mTrackPosition;
    public int mRandomState;
    public int mRepeatState;

    public OdysseyServiceState() {
        mTrackNumber = -1;
        mTrackPosition = -1;
        mRandomState = -1;
        mRepeatState = -1;
    }
}
