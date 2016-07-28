package org.odyssey.playbackservice;

import android.os.Parcel;
import android.os.Parcelable;

import org.odyssey.models.TrackModel;

/**
 * This class is the parcelable which got send from the PlaybackService to notify
 * receivers like the main-GUI or possible later home screen widgets
 * <p/>
 * PlaybackService --> NowPlayingInformation --> OdysseyApplication --> MainActivity
 * |-> Homescreen Widget (later)
 */

public final class NowPlayingInformation implements Parcelable {

    // Parcel data
    private final PlaybackService.PLAYSTATE mPlayState;
    private final String mPlayingURL;
    private final int mPlayingIndex;
    private final int mRepeat;
    private final int mRandom;
    private final int mPlaylistLength;
    private final TrackModel mCurrentTrack;

    public static Parcelable.Creator<NowPlayingInformation> CREATOR = new Parcelable.Creator<NowPlayingInformation>() {

        @Override
        public NowPlayingInformation createFromParcel(Parcel source) {
            PlaybackService.PLAYSTATE playState = PlaybackService.PLAYSTATE.values()[source.readInt()];
            String playingURL = source.readString();
            int playingIndex = source.readInt();
            int repeat = source.readInt();
            int random = source.readInt();
            int playlistlength = source.readInt();
            TrackModel currentTrack = source.readParcelable(TrackModel.class.getClassLoader());
            return new NowPlayingInformation(playState, playingURL, playingIndex, repeat, random, playlistlength, currentTrack);
        }

        @Override
        public NowPlayingInformation[] newArray(int size) {
            return new NowPlayingInformation[size];
        }
    };

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    public NowPlayingInformation(PlaybackService.PLAYSTATE playing, String playingURL, int playingIndex, int repeat, int random, int playlistlength, TrackModel currentTrack) {
        mPlayState = playing;
        mPlayingURL = playingURL;
        mPlayingIndex = playingIndex;
        mRepeat = repeat;
        mRandom = random;
        mPlaylistLength = playlistlength;
        mCurrentTrack = currentTrack;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mPlayState.ordinal());
        dest.writeString(mPlayingURL);
        dest.writeInt(mPlayingIndex);
        dest.writeInt(mRepeat);
        dest.writeInt(mRandom);
        dest.writeInt(mPlaylistLength);
        dest.writeParcelable(mCurrentTrack, flags);
    }

    public PlaybackService.PLAYSTATE getPlayState() {
        return mPlayState;
    }

    public String getPlayingURL() {
        return mPlayingURL;
    }

    public String toString() {
        return "Playing: " + mPlayState.name() + " URL: " + mPlayingURL + " index: " + mPlayingIndex + "repeat: " + mRepeat + "random: " + mRandom;
    }

    public int getPlayingIndex() {
        return mPlayingIndex;
    }

    public int getRepeat() {
        return mRepeat;
    }

    public int getRandom() {
        return mRandom;
    }

    public int getPlaylistLength() {
        return mPlaylistLength;
    }

    public TrackModel getCurrentTrack() {
        return mCurrentTrack;
    }

}
