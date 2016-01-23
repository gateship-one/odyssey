package org.odyssey.playbackservice.managers;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;

import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.NowPlayingInformation;
import org.odyssey.playbackservice.PlaybackService;
import org.odyssey.playbackservice.RemoteControlReceiver;
import org.odyssey.utils.CoverBitmapGenerator;

public class PlaybackStatusHelper {
    public enum SLS_STATES { SLS_START, SLS_RESUME, SLS_PAUSE, SLS_COMPLETE }

    /**
     * INTENT Name of the NowPlayingInformation.
     */
    public static final String INTENT_NOWPLAYINGNAME = "OdysseyNowPlaying";

    /**
     *  Broadcast message to filter to.
     */
    public static final String MESSAGE_NEWTRACKINFORMATION = "org.odyssey.newtrackinfo";


    private PlaybackService mPlaybackService;

    // MediaSession objects
    private MediaSession mMediaSession;

    // Asynchronous cover fetcher
    private CoverBitmapGenerator mBitmapGenerator;

    // Save last track to update cover art only if needed
    private TrackModel mLastTrack = null;

    // Notification manager
    OdysseyNotificationManager mNotificationManager;

    public PlaybackStatusHelper(PlaybackService playbackService) {
        mPlaybackService = playbackService;

        // Get MediaSession objects
        mMediaSession = new MediaSession(mPlaybackService, "OdysseyPBS");

        // Register the callback for the MediaSession
        mMediaSession.setCallback(new OdysseyMediaSessionCallback());

        mBitmapGenerator = new CoverBitmapGenerator(mPlaybackService, new BitmapCoverListener());

        // Register the button receiver
        PendingIntent mediaButtonPendingIntent = PendingIntent.getBroadcast(mPlaybackService,0, new Intent(mPlaybackService,RemoteControlReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT);
        mMediaSession.setMediaButtonReceiver(mediaButtonPendingIntent);

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
        mMediaSession.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_STOPPED, 0, 0.0f).build());
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
        TrackModel currentTrack = mPlaybackService.getCurrentTrack();
        PlaybackService.PLAYSTATE playbackState = mPlaybackService.getPlaybackState();
        // Ask playback service for its state
        switch ( playbackState ) {
            case PLAYING:
            case PAUSE:
                // Call the notification manager, it handles the rest.
                mNotificationManager.updateNotification(currentTrack,playbackState,mMediaSession.getSessionToken());

                // Update MediaSession metadata.
                updateMetadata(currentTrack, playbackState);

                // Broadcast all the information.
                broadcastPlaybackInformation(currentTrack, playbackState);

                // Only update cover image if album changed to preserve energy
                if ( mLastTrack == null || !currentTrack.getTrackAlbumName().equals(mLastTrack.getTrackAlbumName())) {
                    mLastTrack = currentTrack;
                    startCoverImageTask();
                }
                break;
            case STOPPED:
                stopMediaSession();
                broadcastPlaybackInformation(null, PlaybackService.PLAYSTATE.STOPPED);
                mNotificationManager.clearNotification();
                break;
        }

    }

    /**
     * Updates the Metadata from Androids MediaSession. This sets track/album and stuff
     * for a lockscreen image for example.
     * @param track Current track.
     * @param playbackState State of the PlaybackService.
     */
    private void updateMetadata(TrackModel track, PlaybackService.PLAYSTATE playbackState) {
        if (track != null) {
            if ( playbackState == PlaybackService.PLAYSTATE.PLAYING ) {
                mMediaSession.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0, 1.0f)
                        .setActions(PlaybackState.ACTION_SKIP_TO_NEXT + PlaybackState.ACTION_PAUSE +
                                PlaybackState.ACTION_PLAY + PlaybackState.ACTION_SKIP_TO_PREVIOUS +
                                PlaybackState.ACTION_STOP + PlaybackState.ACTION_SEEK_TO).build());
            } else {
                mMediaSession.setPlaybackState(new PlaybackState.Builder().
                        setState(PlaybackState.STATE_PAUSED, 0, 1.0f).setActions(PlaybackState.ACTION_SKIP_TO_NEXT +
                        PlaybackState.ACTION_PAUSE + PlaybackState.ACTION_PLAY +
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS + PlaybackState.ACTION_STOP +
                        PlaybackState.ACTION_SEEK_TO).build());
            }
            // Try to get old metadata to save image retrieval.
            MediaMetadata oldData = mMediaSession.getController().getMetadata();
            MediaMetadata.Builder metaDataBuilder;
            if (oldData == null ) {
                metaDataBuilder = new MediaMetadata.Builder();
            } else {
                metaDataBuilder = new MediaMetadata.Builder(mMediaSession.getController().getMetadata());
            }
            metaDataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, track.getTrackName());
            metaDataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM, track.getTrackAlbumName());
            metaDataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, track.getTrackArtistName());
            metaDataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, track.getTrackArtistName());
            metaDataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, track.getTrackName());
            metaDataBuilder.putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, track.getTrackNumber());
            metaDataBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION, track.getTrackDuration());

            mMediaSession.setMetadata(metaDataBuilder.build());
        }
    }

    /**
     * Broadcasts the new NowPlayingInformation which is received by multiple instances.
     * NowPlayingView in the GUI, Widget for example receives it.
     * @param track Currently played track.
     * @param state State of the PlaybackService
     */
    private void broadcastPlaybackInformation(TrackModel track, PlaybackService.PLAYSTATE state) {
        int repeat = mPlaybackService.getRepeat();
        int random = mPlaybackService.getRandom();
        int playlistLength = mPlaybackService.getPlaylistSize();
        if (track != null) {
            // Create the broadcast intent
            Intent broadcastIntent = new Intent(MESSAGE_NEWTRACKINFORMATION);

            // Create NowPlayingInfo for parcel
            int playing = (state == PlaybackService.PLAYSTATE.PLAYING ? 1 : 0);
            String playingURL = track.getTrackURL();
            int playingIndex = mPlaybackService.getCurrentIndex();

            NowPlayingInformation info = new NowPlayingInformation(playing, playingURL, playingIndex, repeat, random, playlistLength,track);

            // Add nowplayingInfo to parcel
            broadcastIntent.putExtra(INTENT_NOWPLAYINGNAME, info);

            // We're good to go, send it away
            mPlaybackService.sendBroadcast(broadcastIntent);
        } else {
            // Send empty broadcast with stopped information
            Intent broadcastIntent = new Intent(MESSAGE_NEWTRACKINFORMATION);

            NowPlayingInformation info = new NowPlayingInformation(0, "", -1, repeat, random, playlistLength, new TrackModel());
            // Add nowplayingInfo to parcel
            broadcastIntent.putExtra(INTENT_NOWPLAYINGNAME, info);

            // We're good to go, send it away
            mPlaybackService.sendBroadcast(broadcastIntent);
        }
    }

    /**
     * Notify the Simple Last.FM scrobbler with its specific api.
     * Documentation here: https://github.com/tgwizard/sls/wiki/Developer's-API.
     *
     * It is better to call this directly from the PlaybackService because it knows
     * when a song starts AND finishes.
     * @param currentTrack currently changed track.
     * @param slsState PlaybackState but NOT in the same format as the PlaybackService States. See
     *                 documentation.
     */
    public void notifyLastFM(TrackModel currentTrack, SLS_STATES slsState ) {
        Intent bCast = new Intent("com.adam.aslfms.notify.playstatechanged");
        bCast.putExtra("state", slsState.ordinal());
        bCast.putExtra("app-name", "Odyssey");
        bCast.putExtra("app-package", "org.odyssey");
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
        MediaMetadata oldData = mMediaSession.getController().getMetadata();
        MediaMetadata.Builder metaDataBuilder;
        if (oldData == null ) {
            metaDataBuilder = new MediaMetadata.Builder();
        } else {
            metaDataBuilder = new MediaMetadata.Builder(mMediaSession.getController().getMetadata());
        }
        // Reset metadata image in case covergenerator fails
        metaDataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, null);
        mMediaSession.setMetadata(metaDataBuilder.build());

        // Start the actual task based on the current track. (mLastTrack get sets before in updateStatus())
        mBitmapGenerator.getImage(mLastTrack);
    }

    /**
     * Callback class for MediaControls controlled by android system like BT remotes, etc and
     * Volume keys on some android versions.
     */
    private class OdysseyMediaSessionCallback extends MediaSession.Callback {

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
            mPlaybackService.seekTo((int)pos);
        }
    }

    /**
     * Receives the generated album picture from a separate thread for the
     * lockscreen controls. Also sets the title/artist/album again otherwise
     * android would sometimes set it to the track before
     */
    private class BitmapCoverListener implements CoverBitmapGenerator.CoverBitmapListener {

        @Override
        public void receiveBitmap(BitmapDrawable bm) {
            if (bm != null) {
                // Try to get old metadata to save image retrieval.
                MediaMetadata.Builder metaDataBuilder;
                metaDataBuilder = new MediaMetadata.Builder(mMediaSession.getController().getMetadata());
                metaDataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bm.getBitmap());
                mMediaSession.setMetadata(metaDataBuilder.build());
                mNotificationManager.setNotificationImage(bm);
            }
        }
    }
}
