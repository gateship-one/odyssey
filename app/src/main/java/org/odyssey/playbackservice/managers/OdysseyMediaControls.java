package org.odyssey.playbackservice.managers;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;

import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.PlaybackService;
import org.odyssey.playbackservice.RemoteControlReceiver;
import org.odyssey.utils.CoverBitmapGenerator;

public class OdysseyMediaControls {

    private PlaybackService mPlaybackService;

    // MediaSession objects
    private MediaSession mMediaSession;

    // Asynchronous cover fetcher
    private CoverBitmapGenerator mLockscreenCoverGenerator;

    // Save last track to update cover art only if needed
    private TrackModel mLastTrack = null;

    // Notification manager
    OdysseyNotificationManager mNotificationManager;

    public OdysseyMediaControls(PlaybackService playbackService) {
        mPlaybackService = playbackService;

        // Get MediaSession objects
        mMediaSession = new MediaSession(mPlaybackService, "OdysseyPBS");

        // Register the callback for the MediaSession
        mMediaSession.setCallback(new OdysseyMediaSessionCallback());

        mLockscreenCoverGenerator = new CoverBitmapGenerator(mPlaybackService, new LockscreenCoverListener());

        PendingIntent mediaButtonPendingIntent = PendingIntent.getBroadcast(mPlaybackService,0, new Intent(mPlaybackService,RemoteControlReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT);
        mMediaSession.setMediaButtonReceiver(mediaButtonPendingIntent);

        // Initialize the notification manager
        mNotificationManager = new OdysseyNotificationManager(mPlaybackService);
    }


    /*
     * Receives the generated album picture from a separate thread for the
     * lockscreen controls. Also sets the title/artist/album again otherwise
     * android would sometimes set it to the track before
     */
    private class LockscreenCoverListener implements CoverBitmapGenerator.CoverBitmapListener {

        @Override
        public void receiveBitmap(BitmapDrawable bm) {

            if (bm != null) {
                // Try to get old metadata to save image retrieval.
                MediaMetadata.Builder metaDataBuilder;
                metaDataBuilder = new MediaMetadata.Builder(mMediaSession.getController().getMetadata());
                metaDataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bm.getBitmap());
                mMediaSession.setMetadata(metaDataBuilder.build());
            }
        }
    }

    public void startMediaSession() {
        // Start media session
        mMediaSession.setActive(true);
    }

    public void stopMediaSession() {
        mLastTrack = null;
        mMediaSession.setActive(false);
    }

    /*
     * This method updates the metadata submitted to androids media system.
     *
     * Gets an MetadataEditor from android system. Sets all the attributes
     * (playing/paused), title/artist/album and applies it. Also sets which
     * buttons android should show.
     *
     * Starts an thread for Cover generation.
     *
     */
    public void updateMetadata(TrackModel track, PlaybackService.PLAYSTATE playbackState) {
        if (track != null && playbackState != PlaybackService.PLAYSTATE.STOPPED) {
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


            // Only update notification image if album changed to preserve energy
            if ( mLastTrack == null || !track.getTrackAlbumName().equals(mLastTrack.getTrackAlbumName())) {
                // Reset image in case covergenerator fails
                metaDataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, null);
                mLockscreenCoverGenerator.getImage(track);
                mLastTrack = track;
            }
            mMediaSession.setMetadata(metaDataBuilder.build());

            mNotificationManager.updateNotification(track,playbackState,mMediaSession.getSessionToken());
        } else {
            // Clear lockscreen
            mMediaSession.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_STOPPED, 0, 0.0f).build());
            mMediaSession.setActive(false);
            mNotificationManager.clearNotification();
        }
    }

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
}
