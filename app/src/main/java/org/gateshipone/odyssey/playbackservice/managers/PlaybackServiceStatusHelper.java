/*
 * Copyright (C) 2016  Hendrik Borghorst & Frederik Luetkes
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

package org.gateshipone.odyssey.playbackservice.managers;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.playbackservice.NowPlayingInformation;
import org.gateshipone.odyssey.playbackservice.PlaybackService;
import org.gateshipone.odyssey.playbackservice.RemoteControlReceiver;
import org.gateshipone.odyssey.utils.CoverBitmapLoader;

public class PlaybackServiceStatusHelper {
    public enum SLS_STATES {SLS_START, SLS_RESUME, SLS_PAUSE, SLS_COMPLETE}

    /**
     * INTENT Name of the NowPlayingInformation.
     */
    public static final String INTENT_NOWPLAYINGNAME = "OdysseyNowPlaying";

    /**
     * Broadcast message to filter to.
     */
    public static final String MESSAGE_NEWTRACKINFORMATION = "org.gateshipone.odyssey.newtrackinfo";
    public static final String MESSAGE_WORKING = "org.gateshipone.odyssey.working";
    public static final String MESSAGE_IDLE = "org.gateshipone.odyssey.idle";

    private PlaybackService mPlaybackService;

    // MediaSession objects
    private MediaSessionCompat mMediaSession;

    // Asynchronous cover fetcher
    private CoverBitmapLoader mCoverLoader;

    // Save last track to update cover art only if needed
    private TrackModel mLastTrack = null;

    // Notification manager
    OdysseyNotificationManager mNotificationManager;

    public PlaybackServiceStatusHelper(PlaybackService playbackService) {
        mPlaybackService = playbackService;

        // Get MediaSession objects
        mMediaSession = new MediaSessionCompat(mPlaybackService, "OdysseyPBS");

        // Register the callback for the MediaSession
        mMediaSession.setCallback(new OdysseyMediaSessionCallback());

        mCoverLoader = new CoverBitmapLoader(mPlaybackService, new BitmapCoverListener());

        // Register the button receiver
        PendingIntent mediaButtonPendingIntent = PendingIntent.getBroadcast(mPlaybackService, 0, new Intent(mPlaybackService, RemoteControlReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
        mMediaSession.setMediaButtonReceiver(mediaButtonPendingIntent);
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS + MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Initialize the notification manager
        mNotificationManager = new OdysseyNotificationManager(mPlaybackService);
    }


    /**
     * Starts the android mediasession.
     */
    public void startMediaSession() {
        mMediaSession.setActive(true);
    }

    /**
     * Stops the android mediasession.
     */
    public void stopMediaSession() {
        // Make sure to remove the old metadata.
        mMediaSession.setPlaybackState(new PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_STOPPED, 0, 0.0f).build());
        // Clear last track so that covers load again when resuming.
        mLastTrack = null;
        // Actual session disable.
        mMediaSession.setActive(false);
    }

    /**
     * This method should be safe to call at any time. So it should check the
     * current state of PlaybackService and so on.
     */
    public synchronized void updateStatus() {
        NowPlayingInformation info = mPlaybackService.getNowPlayingInformation();
        TrackModel currentTrack = info.getCurrentTrack();
        PlaybackService.PLAYSTATE currentState = info.getPlayState();

        // Ask playback service for its state
        switch (currentState) {
            case PLAYING:
            case PAUSE:
                // Call the notification manager, it handles the rest.
                mNotificationManager.updateNotification(currentTrack, currentState, mMediaSession.getSessionToken());

                // Update MediaSession metadata.
                updateMetadata(currentTrack, currentState);

                // Broadcast all the information.
                broadcastPlaybackInformation(info);

                // Only update cover image if album changed to preserve energy
                if (mLastTrack == null || !info.getCurrentTrack().getTrackAlbumKey().equals(mLastTrack.getTrackAlbumKey())) {
                    mLastTrack = info.getCurrentTrack();
                    startCoverImageTask();
                }
                break;
            case RESUMED:
                // In this state all broadcast listeners should be informed already.
                // Notification should NOT be created in this state, so skip it in contrast to state PAUSE
                // Update MediaSession metadata.
                updateMetadata(currentTrack, currentState);

                // Broadcast all the information.
                broadcastPlaybackInformation(info);

                // Reset the last track
                mLastTrack = null;

                // Clear possible notifications here. This could be the case when the PBS quits itself after a certain amount of time.
                // Depending on the situation (if the GUI is open) the service will instantly restart and go into the resume state.
                mNotificationManager.clearNotification();
                break;
            case STOPPED:
                stopMediaSession();
                broadcastPlaybackInformation(info);
                mNotificationManager.clearNotification();
                break;
        }

    }

    /**
     * Updates the Metadata from Androids MediaSession. This sets track/album and stuff
     * for a lockscreen image for example.
     *
     * @param track         Current track.
     * @param playbackState State of the PlaybackService.
     */
    private void updateMetadata(TrackModel track, PlaybackService.PLAYSTATE playbackState) {
        if (track != null) {
            if (playbackState == PlaybackService.PLAYSTATE.PLAYING) {
                mMediaSession.setPlaybackState(new PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                        .setActions(PlaybackStateCompat.ACTION_SKIP_TO_NEXT + PlaybackStateCompat.ACTION_PAUSE +
                                PlaybackStateCompat.ACTION_PLAY + PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS +
                                PlaybackStateCompat.ACTION_STOP + PlaybackStateCompat.ACTION_SEEK_TO).build());
            } else {
                mMediaSession.setPlaybackState(new PlaybackStateCompat.Builder().
                        setState(PlaybackStateCompat.STATE_PAUSED, 0, 1.0f).setActions(PlaybackStateCompat.ACTION_SKIP_TO_NEXT +
                        PlaybackStateCompat.ACTION_PAUSE + PlaybackStateCompat.ACTION_PLAY +
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS + PlaybackStateCompat.ACTION_STOP +
                        PlaybackStateCompat.ACTION_SEEK_TO).build());
            }
            // Try to get old metadata to save image retrieval.
            MediaMetadataCompat oldData = mMediaSession.getController().getMetadata();
            MediaMetadataCompat.Builder metaDataBuilder;
            if (oldData == null) {
                metaDataBuilder = new MediaMetadataCompat.Builder();
            } else {
                metaDataBuilder = new MediaMetadataCompat.Builder(mMediaSession.getController().getMetadata());
            }
            metaDataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.getTrackName());
            metaDataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.getTrackAlbumName());
            metaDataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.getTrackArtistName());
            metaDataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, track.getTrackArtistName());
            metaDataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, track.getTrackName());
            metaDataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, track.getTrackNumber());
            metaDataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.getTrackDuration());

            mMediaSession.setMetadata(metaDataBuilder.build());
        }
    }

    /**
     * Broadcasts the new NowPlayingInformation which is received by multiple instances.
     * NowPlayingView in the GUI, Widget for example receives it.
     *
     * @param info The current NowPlayingInformation
     */
    private void broadcastPlaybackInformation(NowPlayingInformation info) {

        // Create the broadcast intent
        Intent broadcastIntent = new Intent(MESSAGE_NEWTRACKINFORMATION);

        // Add nowplayingInfo to parcel
        broadcastIntent.putExtra(INTENT_NOWPLAYINGNAME, info);

        // We're good to go, send it away
        mPlaybackService.sendBroadcast(broadcastIntent);
    }

    /**
     * Broadcasts the state of the PlaybackService in order to show a progressDialog for long operations.
     *
     * @param state State of the PlaybackService
     */
    public void broadcastPlaybackServiceState(PlaybackService.PLAYBACKSERVICESTATE state) {
        if (state == PlaybackService.PLAYBACKSERVICESTATE.WORKING) {
            // Create the broadcast intent
            Intent broadcastIntent = new Intent(MESSAGE_WORKING);

            // We're good to go, send it away
            mPlaybackService.sendBroadcast(broadcastIntent);
        } else if (state == PlaybackService.PLAYBACKSERVICESTATE.IDLE) {
            // Create the broadcast intent
            Intent broadcastIntent = new Intent(MESSAGE_IDLE);

            // We're good to go, send it away
            mPlaybackService.sendBroadcast(broadcastIntent);
        }
    }

    /**
     * Notify the Simple Last.FM scrobbler with its specific api.
     * Documentation here: https://github.com/tgwizard/sls/wiki/Developer's-API.
     * <p/>
     * It is better to call this directly from the PlaybackService because it knows
     * when a song starts AND finishes.
     *
     * @param currentTrack currently changed track.
     * @param slsState     PlaybackState but NOT in the same format as the PlaybackService States. See
     *                     documentation.
     */
    public void notifyLastFM(TrackModel currentTrack, SLS_STATES slsState) {
        Intent bCast = new Intent("com.adam.aslfms.notify.playstatechanged");
        bCast.putExtra("state", slsState.ordinal());
        bCast.putExtra("app-name", "Odyssey");
        bCast.putExtra("app-package", "org.gateshipone.odyssey");
        bCast.putExtra("artist", currentTrack.getTrackArtistName());
        bCast.putExtra("album", currentTrack.getTrackAlbumName());
        bCast.putExtra("track", currentTrack.getTrackName());
        bCast.putExtra("duration", currentTrack.getTrackDuration() / 1000);
        mPlaybackService.sendBroadcast(bCast);
    }

    /**
     * Starts the cover fetching task. Make sure that mLastTrack is set correctly before.
     */
    private void startCoverImageTask() {
        // Try to get old metadata to save image retrieval.
        MediaMetadataCompat oldData = mMediaSession.getController().getMetadata();
        MediaMetadataCompat.Builder metaDataBuilder;
        if (oldData == null) {
            metaDataBuilder = new MediaMetadataCompat.Builder();
        } else {
            metaDataBuilder = new MediaMetadataCompat.Builder(mMediaSession.getController().getMetadata());
        }
        // Reset metadata image in case covergenerator fails
        metaDataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null);
        mMediaSession.setMetadata(metaDataBuilder.build());

        // Start the actual task based on the current track. (mLastTrack get sets before in updateStatus())
        mCoverLoader.getImage(mLastTrack);
    }

    /**
     * Callback class for MediaControls controlled by android system like BT remotes, etc and
     * Volume keys on some android versions.
     */
    private class OdysseyMediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onPlay() {
            super.onPlay();
            mPlaybackService.resume();
        }

        @Override
        public void onPause() {
            super.onPause();
            mPlaybackService.pause();
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            mPlaybackService.setNextTrack();
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            mPlaybackService.setPreviousTrack();
        }

        @Override
        public void onStop() {
            super.onStop();
            mPlaybackService.stop();
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
            mPlaybackService.seekTo((int) pos);
        }
    }

    /**
     * Receives the generated album picture from a separate thread for the
     * lockscreen controls. Also sets the title/artist/album again otherwise
     * android would sometimes set it to the track before
     */
    private class BitmapCoverListener implements CoverBitmapLoader.CoverBitmapListener {

        @Override
        public void receiveBitmap(Bitmap bm) {
            if (bm != null) {
                // Try to get old metadata to save image retrieval.
                MediaMetadataCompat.Builder metaDataBuilder;
                metaDataBuilder = new MediaMetadataCompat.Builder(mMediaSession.getController().getMetadata());
                metaDataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bm);
                mMediaSession.setMetadata(metaDataBuilder.build());
                mNotificationManager.setNotificationImage(bm);
            }
        }
    }
}
