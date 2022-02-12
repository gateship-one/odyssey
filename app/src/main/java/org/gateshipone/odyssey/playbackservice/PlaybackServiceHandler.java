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
                case ODYSSEY_PLAY:
                    mService.get().playURI(msgObj.nextString());
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
                    mService.get().seekTo(msgObj.nextInt());
                    break;
                case ODYSSEY_JUMPTO:
                    mService.get().jumpToIndex(msgObj.nextInt());
                    break;
                case ODYSSEY_REPEAT:
                    mService.get().toggleRepeat();
                    break;
                case ODYSSEY_RANDOM:
                    mService.get().toggleRandom();
                    break;
                case ODYSSEY_ENQUEUETRACK:
                    mService.get().enqueueTrack(msgObj.getTrack(), msgObj.nextBool());
                    break;
                case ODYSSEY_PLAYTRACK:
                    mService.get().playTrack(msgObj.getTrack(), msgObj.nextBool());
                    break;
                case ODYSSEY_DEQUEUETRACK:
                    mService.get().dequeueTrack(msgObj.nextInt());
                    break;
                case ODYSSEY_DEQUEUETRACKS:
                    mService.get().dequeueTracks(msgObj.nextInt());
                    break;
                case ODYSSEY_CLEARPLAYLIST:
                    mService.get().clearPlaylist();
                    break;
                case ODYSSEY_SHUFFLEPLAYLIST:
                    mService.get().shufflePlaylist();
                    break;
                case ODYSSEY_PLAYALLTRACKS:
                    mService.get().playAllTracks(msgObj.nextString());
                    break;
                case ODYSSEY_SAVEPLAYLIST:
                    mService.get().savePlaylist(msgObj.nextString());
                    break;
                case ODYSSEY_ENQUEUEPLAYLIST:
                    mService.get().enqueuePlaylist(msgObj.getPlaylist());
                    break;
                case ODYSSEY_PLAYPLAYLIST:
                    mService.get().playPlaylist(msgObj.getPlaylist(), msgObj.nextInt());
                    break;
                case ODYSSEY_RESUMEBOOKMARK:
                    mService.get().resumeBookmark(msgObj.nextLong());
                    break;
                case ODYSSEY_DELETEBOOKMARK:
                    mService.get().deleteBookmark(msgObj.nextLong());
                    break;
                case ODYSSEY_CREATEBOOKMARK:
                    mService.get().createBookmark(msgObj.nextString());
                    break;
                case ODYSSEY_ENQUEUEFILE:
                    mService.get().enqueueFile(msgObj.nextString(), msgObj.nextBool());
                    break;
                case ODYSSEY_PLAYFILE:
                    mService.get().playFile(msgObj.nextString(), msgObj.nextBool());
                    break;
                case ODYSSEY_PLAYDIRECTORY:
                    mService.get().playDirectory(msgObj.nextString(), msgObj.nextInt());
                    break;
                case ODYSSEY_ENQUEUEDIRECTORYANDSUBDIRECTORIES:
                    mService.get().enqueueDirectoryAndSubDirectories(msgObj.nextString(), msgObj.nextString());
                    break;
                case ODYSSEY_PLAYDIRECTORYANDSUBDIRECTORIES:
                    mService.get().playDirectoryAndSubDirectories(msgObj.nextString(), msgObj.nextString());
                    break;
                case ODYSSEY_ENQUEUEALBUM:
                    mService.get().enqueueAlbum(msgObj.nextLong(), msgObj.nextString());
                    break;
                case ODYSSEY_PLAYALBUM:
                    mService.get().playAlbum(msgObj.nextLong(), msgObj.nextString(), msgObj.nextInt());
                    break;
                case ODYSSEY_ENQUEUEARTIST:
                    mService.get().enqueueArtist(msgObj.nextLong(), msgObj.nextString(), msgObj.nextString());
                    break;
                case ODYSSEY_PLAYARTIST:
                    mService.get().playArtist(msgObj.nextLong(), msgObj.nextString(), msgObj.nextString());
                    break;
                case ODYSSEY_ENQUEUERECENTALBUMS:
                    mService.get().enqueueRecentAlbums();
                    break;
                case ODYSSEY_PLAYRECENTALBUMS:
                    mService.get().playRecentAlbums();
                    break;
                case ODYSSEY_START_SLEEPTIMER:
                    mService.get().startSleepTimer(msgObj.nextLong(), msgObj.nextBool());
                    break;
                case ODYSSEY_CANCEL_SLEEPTIMER:
                    mService.get().cancelSleepTimer();
                    break;
                case ODYSSEY_SET_SMARTRANDOM:
                    mService.get().setSmartRandom(msgObj.nextInt());
                    break;
            }

            mLock.release();
        }
    }
}
