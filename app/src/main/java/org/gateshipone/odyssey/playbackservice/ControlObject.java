/*
 * Copyright (C) 2017 Team Gateship-One
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

import org.gateshipone.odyssey.models.TrackModel;

import java.util.ArrayList;

/**
 * Message object which get passed between PlaybackServiceInterface ->
 * PlaybackServiceHandler
 *
 * @author hendrik
 */
public class ControlObject {
    public enum PLAYBACK_ACTION {
        ODYSSEY_PLAY, ODYSSEY_TOGGLEPAUSE, ODYSSEY_NEXT, ODYSSEY_PREVIOUS, ODYSSEY_SEEKTO, ODYSSEY_JUMPTO, ODYSSEY_REPEAT, ODYSSEY_RANDOM,
        ODYSSEY_ENQUEUETRACK, ODYSSEY_PLAYTRACK, ODYSSEY_DEQUEUETRACK, ODYSSEY_DEQUEUETRACKS,
        ODYSSEY_PLAYALLTRACKS,
        ODYSSEY_RESUMEBOOKMARK, ODYSSEY_DELETEBOOKMARK, ODYSSEY_CREATEBOOKMARK,
        ODYSSEY_SAVEPLAYLIST, ODYSSEY_CLEARPLAYLIST, ODYSSEY_ENQUEUEPLAYLIST, ODYSSEY_ENQUEUEPLAYLISTFILE, ODYSSEY_SHUFFLEPLAYLIST,
        ODYSSEY_ENQUEUEFILE, ODYSSEY_PLAYFILE,
        ODYSSEY_PLAYDIRECTORY, ODYSSEY_ENQUEUEDIRECTORYANDSUBDIRECTORIES, ODYSSEY_PLAYDIRECTORYANDSUBDIRECTORIES,
        ODYSSEY_ENQUEUEALBUM, ODYSSEY_PLAYALBUM, ODYSSEY_ENQUEUERECENTALBUMS,
        ODYSSEY_ENQUEUEARTIST, ODYSSEY_PLAYARTIST
    }

    private PLAYBACK_ACTION mAction;
    private boolean mBoolparam;
    private int mIntparam;
    private String mStringparam;
    private TrackModel mTrack;
    private long mLongParam;
    private ArrayList<TrackModel> mTrackList = null;

    public ControlObject(PLAYBACK_ACTION action) {
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

    public ControlObject(PLAYBACK_ACTION action, String param, boolean asNext) {
        mAction = action;
        mStringparam = param;
        mBoolparam = asNext;
    }

    public ControlObject(PLAYBACK_ACTION action, ArrayList<TrackModel> list) {
        mTrackList = list;
        mAction = action;
    }

    public ControlObject(PLAYBACK_ACTION action, TrackModel track) {
        mAction = action;
        mTrack = track;
    }

    public ControlObject(PLAYBACK_ACTION action, TrackModel track, boolean asNext) {
        mAction = action;
        mTrack = track;
        mBoolparam = asNext;
    }

    public ControlObject(PLAYBACK_ACTION action, long param) {
        mAction = action;
        mLongParam = param;
    }

    public ControlObject(PLAYBACK_ACTION action, long longParam, String stringParam) {
        mAction = action;
        mLongParam = longParam;
        mStringparam = stringParam;
    }

    public ControlObject(PLAYBACK_ACTION action, String stringParam, int intParam) {
        mAction = action;
        mStringparam = stringParam;
        mIntparam = intParam;
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

    public long getLongParam() {
        return mLongParam;
    }

    public TrackModel getTrack() {
        return mTrack;
    }
}
