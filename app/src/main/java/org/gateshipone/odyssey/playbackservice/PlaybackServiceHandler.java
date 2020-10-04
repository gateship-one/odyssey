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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;
import java.util.concurrent.Semaphore;

public class PlaybackServiceHandler extends Handler {
    private final WeakReference<PlaybackService> mService;

    private Semaphore mLock;

    public PlaybackServiceHandler(Looper looper, PlaybackService service) {
        super(looper);
        mService = new WeakReference<>(service);
        mLock = new Semaphore(1);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        ControlObject msgObj = (ControlObject) msg.obj;

        // Check if object is received
        if (msgObj != null && mLock.tryAcquire()) {
            // Parse message
            switch (msgObj.getAction()) {
                case ODYSSEY_PLAY:
                    mService.get().playURI(msgObj.getStringParam());
                    break;
                case ODYSSEY_TOGGLEPAUSE:
                    mService.get().togglePause();
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
                    mService.get().jumpToIndex(msgObj.getIntParam());
                    break;
                case ODYSSEY_REPEAT:
                    mService.get().toggleRepeat();
                    break;
                case ODYSSEY_RANDOM:
                    mService.get().toggleRandom();
                    break;
                case ODYSSEY_ENQUEUETRACK:
                    mService.get().enqueueTrack(msgObj.getTrack(), msgObj.getBoolParam());
                    break;
                case ODYSSEY_PLAYTRACK:
                    mService.get().playTrack(msgObj.getTrack(), msgObj.getBoolParam());
                    break;
                case ODYSSEY_DEQUEUETRACK:
                    mService.get().dequeueTrack(msgObj.getIntParam());
                    break;
                case ODYSSEY_DEQUEUETRACKS:
                    mService.get().dequeueTracks(msgObj.getIntParam());
                    break;
                case ODYSSEY_CLEARPLAYLIST:
                    mService.get().clearPlaylist();
                    break;
                case ODYSSEY_SHUFFLEPLAYLIST:
                    mService.get().shufflePlaylist();
                    break;
                case ODYSSEY_PLAYALLTRACKS:
                    mService.get().playAllTracks(msgObj.getStringParam());
                    break;
                case ODYSSEY_SAVEPLAYLIST:
                    mService.get().savePlaylist(msgObj.getStringParam());
                    break;
                case ODYSSEY_ENQUEUEPLAYLIST:
                    mService.get().enqueuePlaylist(msgObj.getPlaylist());
                    break;
                case ODYSSEY_PLAYPLAYLIST:
                    mService.get().playPlaylist(msgObj.getPlaylist(), msgObj.getIntParam());
                    break;
                case ODYSSEY_RESUMEBOOKMARK:
                    mService.get().resumeBookmark(msgObj.getLongParam());
                    break;
                case ODYSSEY_DELETEBOOKMARK:
                    mService.get().deleteBookmark(msgObj.getLongParam());
                    break;
                case ODYSSEY_CREATEBOOKMARK:
                    mService.get().createBookmark(msgObj.getStringParam());
                    break;
                case ODYSSEY_ENQUEUEFILE:
                    mService.get().enqueueFile(msgObj.getStringParam(), msgObj.getBoolParam());
                    break;
                case ODYSSEY_PLAYFILE:
                    mService.get().playFile(msgObj.getStringParam(), msgObj.getBoolParam());
                    break;
                case ODYSSEY_PLAYDIRECTORY:
                    mService.get().playDirectory(msgObj.getStringParam(), msgObj.getIntParam());
                    break;
                case ODYSSEY_ENQUEUEDIRECTORYANDSUBDIRECTORIES:
                    mService.get().enqueueDirectoryAndSubDirectories(msgObj.getStringParam(), msgObj.getSecondStringParam());
                    break;
                case ODYSSEY_PLAYDIRECTORYANDSUBDIRECTORIES:
                    mService.get().playDirectoryAndSubDirectories(msgObj.getStringParam(), msgObj.getSecondStringParam());
                    break;
                case ODYSSEY_ENQUEUEALBUM:
                    mService.get().enqueueAlbum(msgObj.getStringParam(), msgObj.getSecondStringParam());
                    break;
                case ODYSSEY_PLAYALBUM:
                    mService.get().playAlbum(msgObj.getStringParam(), msgObj.getSecondStringParam(), msgObj.getIntParam());
                    break;
                case ODYSSEY_ENQUEUEARTIST:
                    mService.get().enqueueArtist(msgObj.getLongParam(), msgObj.getStringParam(), msgObj.getSecondStringParam());
                    break;
                case ODYSSEY_PLAYARTIST:
                    mService.get().playArtist(msgObj.getLongParam(), msgObj.getStringParam(), msgObj.getSecondStringParam());
                    break;
                case ODYSSEY_ENQUEUERECENTALBUMS:
                    mService.get().enqueueRecentAlbums();
                    break;
                case ODYSSEY_PLAYRECENTALBUMS:
                    mService.get().playRecentAlbums();
                    break;
                case ODYSSEY_START_SLEEPTIMER:
                    mService.get().startSleepTimer(msgObj.getLongParam(), msgObj.getBoolParam());
                    break;
                case ODYSSEY_CANCEL_SLEEPTIMER:
                    mService.get().cancelSleepTimer();
                    break;
                case ODYSSEY_SET_SMARTRANDOM:
                    mService.get().setSmartRandom(msgObj.getIntParam());
                    break;
            }

            mLock.release();
        }
    }
}
