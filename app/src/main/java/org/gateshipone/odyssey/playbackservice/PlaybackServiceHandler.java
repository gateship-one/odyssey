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

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.concurrent.Semaphore;

public class PlaybackServiceHandler extends Handler {
    private final WeakReference<PlaybackService> mService;

    private final Semaphore mLock;

    public PlaybackServiceHandler(Looper looper, PlaybackService service) {
        super(looper);
        mService = new WeakReference<>(service);
        mLock = new Semaphore(1);
    }

    @Override
    public void handleMessage(@Nullable Message msg) {
        if (msg == null) {
            return;
        }

        ControlObject msgObj = (ControlObject) msg.obj;

        // Check if object is received
        if (msgObj != null && mLock.tryAcquire()) {
            // Parse message
            switch (msgObj.getAction()) {
                case PLAY:
                    mService.get().playURI(msgObj.nextString());
                    break;
                case TOGGLE_PAUSE:
                    mService.get().togglePause();
                    break;
                case NEXT:
                    mService.get().setNextTrack();
                    break;
                case PREVIOUS:
                    mService.get().setPreviousTrack();
                    break;
                case SEEK_TO:
                    mService.get().seekTo(msgObj.nextInt());
                    break;
                case JUMP_TO:
                    mService.get().jumpToIndex(msgObj.nextInt());
                    break;
                case REPEAT:
                    mService.get().toggleRepeat();
                    break;
                case RANDOM:
                    mService.get().toggleRandom();
                    break;
                case ENQUEUE_TRACK:
                    mService.get().enqueueTrack(msgObj.getTrack(), msgObj.nextBool());
                    break;
                case PLAY_TRACK:
                    mService.get().playTrack(msgObj.getTrack(), msgObj.nextBool());
                    break;
                case DEQUEUE_TRACK:
                    mService.get().dequeueTrack(msgObj.nextInt());
                    break;
                case DEQUEUE_TRACKS:
                    mService.get().dequeueTracks(msgObj.nextInt());
                    break;
                case CLEAR_PLAYLIST:
                    mService.get().clearPlaylist();
                    break;
                case SHUFFLE_PLAYLIST:
                    mService.get().shufflePlaylist();
                    break;
                case PLAY_ALL_TRACKS:
                    mService.get().playAllTracks(msgObj.nextString());
                    break;
                case SAVE_PLAYLIST:
                    mService.get().savePlaylist(msgObj.nextString());
                    break;
                case ENQUEUE_PLAYLIST:
                    mService.get().enqueuePlaylist(msgObj.getPlaylist());
                    break;
                case PLAY_PLAYLIST:
                    mService.get().playPlaylist(msgObj.getPlaylist(), msgObj.nextInt());
                    break;
                case RESUME_BOOKMARK:
                    mService.get().resumeBookmark(msgObj.nextLong());
                    break;
                case DELETE_BOOKMARK:
                    mService.get().deleteBookmark(msgObj.nextLong());
                    break;
                case CREATE_BOOKMARK:
                    mService.get().createBookmark(msgObj.nextString());
                    break;
                case ENQUEUE_FILE:
                    mService.get().enqueueFile(msgObj.nextString(), msgObj.nextBool());
                    break;
                case PLAY_FILE:
                    mService.get().playFile(msgObj.nextString(), msgObj.nextBool());
                    break;
                case PLAY_DIRECTORY:
                    mService.get().playDirectory(msgObj.nextString(), msgObj.nextInt());
                    break;
                case ENQUEUE_DIRECTORY_AND_SUBDIRECTORIES:
                    mService.get().enqueueDirectoryAndSubDirectories(msgObj.nextString(), msgObj.nextString());
                    break;
                case PLAY_DIRECTORY_AND_SUBDIRECTORIES:
                    mService.get().playDirectoryAndSubDirectories(msgObj.nextString(), msgObj.nextString());
                    break;
                case ENQUEUE_ALBUM:
                    mService.get().enqueueAlbum(msgObj.nextLong(), msgObj.nextString());
                    break;
                case PLAY_ALBUM:
                    mService.get().playAlbum(msgObj.nextLong(), msgObj.nextString(), msgObj.nextInt());
                    break;
                case ENQUEUE_ARTIST:
                    mService.get().enqueueArtist(msgObj.nextLong(), msgObj.nextString(), msgObj.nextString());
                    break;
                case PLAY_ARTIST:
                    mService.get().playArtist(msgObj.nextLong(), msgObj.nextString(), msgObj.nextString());
                    break;
                case ENQUEUE_RECENT_ALBUMS:
                    mService.get().enqueueRecentAlbums();
                    break;
                case PLAY_RECENT_ALBUMS:
                    mService.get().playRecentAlbums();
                    break;
                case START_SLEEP_TIMER:
                    mService.get().startSleepTimer(msgObj.nextLong(), msgObj.nextBool());
                    break;
                case CANCEL_SLEEP_TIMER:
                    mService.get().cancelSleepTimer();
                    break;
                case SET_SMART_RANDOM:
                    mService.get().setSmartRandom(msgObj.nextInt());
                    break;
            }

            mLock.release();
        }
    }
}
