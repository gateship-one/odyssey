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

import android.os.Message;
import android.os.RemoteException;

import org.gateshipone.odyssey.models.PlaylistModel;
import org.gateshipone.odyssey.models.TrackModel;

import java.lang.ref.WeakReference;

public class OdysseyPlaybackServiceInterface extends IOdysseyPlaybackService.Stub {
    // Holds the actual playback service for handling reasons
    private final WeakReference<PlaybackService> mService;

    OdysseyPlaybackServiceInterface(PlaybackService service) {
        mService = new WeakReference<>(service);
    }

    /**
     * Following are methods which call the handler thread (which runs at
     * audio priority) so that handling of playback is done in a seperate
     * thread for performance reasons.
     */
    @Override
    public void playURI(String uri) {
        // Create play control object
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAY)
                .addString(uri)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueTrack(TrackModel track, boolean asNext) {
        // Create enqueuetrack control object
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUETRACK)
                .addTrack(track)
                .addBool(asNext)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    public void playTrack(TrackModel track, boolean clearPlaylist) {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYTRACK)
                .addTrack(track)
                .addBool(clearPlaylist)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void toggleRandom() {
        // Create random control object
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_RANDOM)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void toggleRepeat() {
        // Create repeat control object
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_REPEAT)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public int getAudioSessionID() {
        return mService.get().getAudioSessionID();
    }

    @Override
    public void hideArtworkChanged(boolean enabled) {
        mService.get().hideArtwork(enabled);
    }

    @Override
    public void hideMediaOnLockscreenChanged(boolean enabled) {
        mService.get().hideMediaOnLockscreen(enabled);
    }

    @Override
    public void changeAutoBackwardsSeekAmount(int amount) {
        mService.get().setAutoBackwardsSeekAmount(amount);
    }

    @Override
    public void setSmartRandom(int intelligenceFactor) {
        // Create repeat control object
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_SET_SMARTRANDOM)
                .addInt(intelligenceFactor)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void startSleepTimer(long durationMS, boolean stopAfterCurrent) {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_START_SLEEPTIMER)
                .addLong(durationMS)
                .addBool(stopAfterCurrent)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void cancelSleepTimer() {
        final ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_CANCEL_SLEEPTIMER)
                .build();

        final Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public boolean hasActiveSleepTimer() {
        return mService.get().hasActiveSleepTimer();
    }

    @Override
    public boolean isBusy() {
        return mService.get().isBusy();
    }

    @Override
    public void seekTo(int position) {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_SEEKTO)
                .addInt(position)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void jumpTo(int position) {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_JUMPTO)
                .addInt(position)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void clearPlaylist() {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_CLEARPLAYLIST)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void next() {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_NEXT)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void previous() {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_PREVIOUS)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void togglePause() {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_TOGGLEPAUSE)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public int getTrackPosition() {
        return mService.get().getTrackPosition();
    }

    @Override
    public TrackModel getCurrentSong() {
        return mService.get().getCurrentTrack();
    }

    public NowPlayingInformation getNowPlayingInformation() {
        return mService.get().getNowPlayingInformation();
    }

    @Override
    public void dequeueTrack(int index) {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_DEQUEUETRACK)
                .addInt(index)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void dequeueTracks(int index) {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_DEQUEUETRACKS)
                .addInt(index)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public TrackModel getPlaylistSong(int index) {
        return mService.get().getPlaylistTrack(index);
    }

    @Override
    public int getPlaylistSize() {
        return mService.get().getPlaylistSize();
    }

    @Override
    public void shufflePlaylist() {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_SHUFFLEPLAYLIST)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playAllTracks(String filterString) {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYALLTRACKS)
                .addString(filterString)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public int getCurrentIndex() {
        return mService.get().getCurrentIndex();
    }

    @Override
    public void savePlaylist(String name) {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_SAVEPLAYLIST)
                .addString(name)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueuePlaylist(PlaylistModel playlist) throws RemoteException {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUEPLAYLIST)
                .addPlaylist(playlist)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playPlaylist(PlaylistModel playlist, int position) throws RemoteException {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYPLAYLIST)
                .addPlaylist(playlist)
                .addInt(position)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueAlbum(long albumId, String orderKey) {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUEALBUM)
                .addLong(albumId)
                .addString(orderKey)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playAlbum(long albumId, String orderKey, int position) {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYALBUM)
                .addLong(albumId)
                .addString(orderKey)
                .addInt(position)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueRecentAlbums() {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUERECENTALBUMS)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playRecentAlbums() {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYRECENTALBUMS)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueArtist(long artistId, String albumOrderKey, String trackOrderKey) {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUEARTIST)
                .addLong(artistId)
                .addString(albumOrderKey)
                .addString(trackOrderKey)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playArtist(long artistId, String albumOrderKey, String trackOrderKey) {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYARTIST)
                .addLong(artistId)
                .addString(albumOrderKey)
                .addString(trackOrderKey)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void resumeBookmark(long timestamp) {
        // create resume bookmark control object
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_RESUMEBOOKMARK)
                .addLong(timestamp)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void deleteBookmark(long timestamp) {
        // create delete bookmark control object
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_DELETEBOOKMARK)
                .addLong(timestamp)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void createBookmark(String bookmarkTitle) {
        // create create bookmark control object
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_CREATEBOOKMARK)
                .addString(bookmarkTitle)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueFile(String filePath, boolean asNext) {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUEFILE)
                .addString(filePath)
                .addBool(asNext)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playFile(String filePath, boolean clearPlaylist) {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYFILE)
                .addString(filePath)
                .addBool(clearPlaylist)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playDirectory(String directoryPath, int position) {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYDIRECTORY)
                .addString(directoryPath)
                .addInt(position)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void enqueueDirectoryAndSubDirectories(String directoryPath, String filterString) {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUEDIRECTORYANDSUBDIRECTORIES)
                .addString(directoryPath)
                .addString(filterString)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void playDirectoryAndSubDirectories(String directoryPath, String filterString) {
        ControlObject obj = new ControlObject.Builder(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYDIRECTORYANDSUBDIRECTORIES)
                .addString(directoryPath)
                .addString(filterString)
                .build();

        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = obj;
        mService.get().getHandler().sendMessage(msg);
    }
}
