package org.odyssey.playbackservice;

public class OdysseyServiceState {

    public int mTrackNumber;
    public int mTrackPosition;
    public PlaybackService.RANDOMSTATE mRandomState;
    public PlaybackService.REPEATSTATE mRepeatState;

    public OdysseyServiceState() {
        mTrackNumber = -1;
        mTrackPosition = -1;
        mRandomState = PlaybackService.RANDOMSTATE.RANDOM_OFF;
        mRepeatState = PlaybackService.REPEATSTATE.REPEAT_OFF;
    }
}
