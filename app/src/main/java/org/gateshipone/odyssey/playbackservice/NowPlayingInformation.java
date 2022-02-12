/*
 * Copyright (C) 2020 Team Gateship-One
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

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.gateshipone.odyssey.models.TrackModel;

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
    private final int mPlayingIndex;
    private final PlaybackService.REPEATSTATE mRepeat;
    private final PlaybackService.RANDOMSTATE mRandom;
    private final int mPlaylistLength;
    @NonNull
    private final TrackModel mCurrentTrack;

    public static Parcelable.Creator<NowPlayingInformation> CREATOR = new Parcelable.Creator<NowPlayingInformation>() {

        @Override
        public NowPlayingInformation createFromParcel(Parcel source) {
            PlaybackService.PLAYSTATE playState = PlaybackService.PLAYSTATE.values()[source.readInt()];
            int playingIndex = source.readInt();
            PlaybackService.REPEATSTATE repeat = PlaybackService.REPEATSTATE.values()[source.readInt()];
            PlaybackService.RANDOMSTATE random = PlaybackService.RANDOMSTATE.values()[source.readInt()];
            int playlistlength = source.readInt();
            TrackModel currentTrack = source.readParcelable(TrackModel.class.getClassLoader());
            return new NowPlayingInformation(playState, playingIndex, repeat, random, playlistlength, currentTrack);
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

    public NowPlayingInformation() {
        mPlayState = PlaybackService.PLAYSTATE.STOPPED;
        mPlayingIndex = -1;
        mRepeat = PlaybackService.REPEATSTATE.REPEAT_OFF;
        mRandom = PlaybackService.RANDOMSTATE.RANDOM_OFF;
        mPlaylistLength = 0;
        mCurrentTrack = new TrackModel();
    }

    public NowPlayingInformation(PlaybackService.PLAYSTATE playing, int playingIndex, PlaybackService.REPEATSTATE repeat, PlaybackService.RANDOMSTATE random, int playlistlength, @NonNull TrackModel currentTrack) {
        mPlayState = playing;
        mPlayingIndex = playingIndex;
        mRepeat = repeat;
        mRandom = random;
        mPlaylistLength = playlistlength;
        mCurrentTrack = currentTrack;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mPlayState.ordinal());
        dest.writeInt(mPlayingIndex);
        dest.writeInt(mRepeat.ordinal());
        dest.writeInt(mRandom.ordinal());
        dest.writeInt(mPlaylistLength);
        dest.writeParcelable(mCurrentTrack, flags);
    }

    public PlaybackService.PLAYSTATE getPlayState() {
        return mPlayState;
    }

    @NonNull
    @Override
    public String toString() {
        return "Playstate: " + mPlayState.name() + " index: " + mPlayingIndex + "repeat: " + mRepeat + "random: " + mRandom + "playlistlength: " + mPlaylistLength + "track: " + mCurrentTrack;
    }

    public int getPlayingIndex() {
        return mPlayingIndex;
    }

    public PlaybackService.REPEATSTATE getRepeat() {
        return mRepeat;
    }

    public PlaybackService.RANDOMSTATE getRandom() {
        return mRandom;
    }

    public int getPlaylistLength() {
        return mPlaylistLength;
    }

    @NonNull
    public TrackModel getCurrentTrack() {
        return mCurrentTrack;
    }

}
