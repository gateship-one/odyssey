package org.odyssey.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.RemoteViews;

import org.odyssey.OdysseyMainActivity;
import org.odyssey.OdysseySplashActivity;
import org.odyssey.R;
import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.NowPlayingInformation;
import org.odyssey.playbackservice.PlaybackService;
import org.odyssey.playbackservice.managers.PlaybackStatusHelper;
import org.odyssey.utils.CoverBitmapGenerator;

public class
OdysseyWidgetProvider extends AppWidgetProvider {
    private RemoteViews mViews;
    private AppWidgetManager mAppWidgetManager;
    private Context mContext;

    private static TrackModel mLastTrack = null;
    private static Bitmap mLastCover = null;

    private final static int INTENT_OPENGUI = 0;
    private final static int INTENT_PREVIOUS = 1;
    private final static int INTENT_PLAYPAUSE = 2;
    private final static int INTENT_NEXT = 3;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        mContext = context;

        // Perform this loop procedure for each App Widget that belongs to this
        // provider
        for (int appWidgetId : appWidgetIds) {
            // Tell the AppWidgetManager to perform an update on the current app widget
            mAppWidgetManager = appWidgetManager;
            appWidgetManager.updateAppWidget(appWidgetId, mViews);
        }
    }

    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);

        mLastTrack = null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        mContext = context;
        boolean nowPlaying = false;
        // get remoteviews
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_odyssey_big);

        if (intent.getAction().equals(PlaybackStatusHelper.MESSAGE_NEWTRACKINFORMATION)) {

            intent.setExtrasClassLoader(context.getClassLoader());

            NowPlayingInformation info = intent.getParcelableExtra(PlaybackStatusHelper.INTENT_NOWPLAYINGNAME);
            if (info != null) {
                TrackModel item = info.getCurrentTrack();
                if (item != null) {
                    views.setTextViewText(R.id.widget_big_trackName, item.getTrackName());
                    views.setTextViewText(R.id.widget_big_ArtistAlbum, item.getTrackArtistName());

                    if (mLastTrack == null || !mLastTrack.getTrackAlbumKey().equals(item.getTrackAlbumKey())) {
                        views.setImageViewResource(R.id.widget_big_cover, R.drawable.odyssey_notification);
                        CoverBitmapGenerator mCoverGenerator = new CoverBitmapGenerator(context, new CoverReceiver());
                        mCoverGenerator.getImage(item);
                        mLastCover = null;
                    } else if (mLastTrack.getTrackAlbumKey().equals(item.getTrackAlbumKey()) && mLastCover != null) {
                        views.setImageViewBitmap(R.id.widget_big_cover, mLastCover);
                    }
                }
                if (info.getPlaying() == 0) {
                    // Show play icon
                    views.setImageViewResource(R.id.widget_big_play, R.drawable.ic_play_arrow_48dp);
                } else if (info.getPlaying() == 1) {
                    // Show pause icon
                    nowPlaying = true;
                    views.setImageViewResource(R.id.widget_big_play, R.drawable.ic_pause_48dp);
                }

                mLastTrack = item;
            }
        }

        // set button actions
        // Main action
        Intent mainIntent = new Intent(context, OdysseySplashActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        if (nowPlaying) {
            // add intent only if playing is active
            mainIntent.putExtra(OdysseyMainActivity.MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW, OdysseyMainActivity.MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW_NOWPLAYINGVIEW);
        }
        PendingIntent mainPendingIntent = PendingIntent.getActivity(context, INTENT_OPENGUI, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_big_cover, mainPendingIntent);

        // Play/Pause action
        Intent playPauseIntent = new Intent(context, PlaybackService.class);
        playPauseIntent.putExtra("action", PlaybackService.ACTION_TOGGLEPAUSE);
        PendingIntent playPausePendingIntent = PendingIntent.getService(context, INTENT_PLAYPAUSE, playPauseIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_big_play, playPausePendingIntent);

        // Previous song action
        Intent prevIntent = new Intent(context, PlaybackService.class);
        prevIntent.putExtra("action", PlaybackService.ACTION_PREVIOUS);
        PendingIntent prevPendingIntent = PendingIntent.getService(context, INTENT_PREVIOUS, prevIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_big_previous, prevPendingIntent);

        // Next song action
        Intent nextIntent = new Intent(context, PlaybackService.class);
        nextIntent.putExtra("action", PlaybackService.ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getService(context, INTENT_NEXT, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_big_next, nextPendingIntent);

        mViews = views;
        mAppWidgetManager = AppWidgetManager.getInstance(context);
        mAppWidgetManager.updateAppWidget(new ComponentName(context, OdysseyWidgetProvider.class), views);
    }

    private class CoverReceiver implements CoverBitmapGenerator.CoverBitmapListener {

        public CoverReceiver() {
        }

        @Override
        public void receiveBitmap(BitmapDrawable bm) {
            if (mViews != null && bm != null) {
                mLastCover = bm.getBitmap();
                mViews.setImageViewBitmap(R.id.widget_big_cover, mLastCover);

                mAppWidgetManager.updateAppWidget(new ComponentName(mContext, OdysseyWidgetProvider.class), mViews);

            }
        }
    }
}
