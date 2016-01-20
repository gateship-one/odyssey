package org.odyssey.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Parcelable;
import android.util.Log;
import android.widget.RemoteViews;

import org.odyssey.OdysseyMainActivity;
import org.odyssey.R;
import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.NowPlayingInformation;
import org.odyssey.playbackservice.PlaybackService;
import org.odyssey.utils.CoverBitmapGenerator;

import java.util.ArrayList;


public class OdysseyWidgetProvider  extends AppWidgetProvider {
    private static final String TAG = "OdysseyWidget";

    private CoverBitmapGenerator mCoverGenerator;
    private RemoteViews mViews;
    private AppWidgetManager mAppWidgetManager;
    private int[] mAppWidgets = null;
    private Context mContext;

    private final static int INTENT_OPENGUI = 0;
    private final static int INTENT_PREVIOUS = 1;
    private final static int INTENT_PLAYPAUSE = 2;
    private final static int INTENT_NEXT = 3;
    private final static int INTENT_STOP = 4;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.v(TAG, "onUpdate");
        mContext = context;

        final int N = appWidgetIds.length;
        mAppWidgets = appWidgetIds;

        // Perform this loop procedure for each App Widget that belongs to this
        // provider
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];

            // get remoteviews
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_odyssey_big);

            // Main action
            Intent mainIntent = new Intent(context, OdysseyMainActivity.class);
            mainIntent.putExtra("Fragment", "currentsong");
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION| Intent.FLAG_ACTIVITY_NO_HISTORY);
            PendingIntent mainPendingIntent = PendingIntent.getActivity(context, INTENT_OPENGUI, mainIntent, PendingIntent.FLAG_ONE_SHOT);
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

            // Stop action
//            Intent stopIntent = new Intent(context, PlaybackService.class);
//            stopIntent.putExtra("action", PlaybackService.ACTION_STOP);
//            PendingIntent stopPendingIntent = PendingIntent.getService(context, INTENT_STOP, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);
//            views.setOnClickPendingIntent(R.id.odysseyWidgetStopButton, stopPendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app
            // widget
            mAppWidgetManager = appWidgetManager;
            appWidgetManager.updateAppWidget(appWidgetId, views);
            mViews = views;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        super.onReceive(context, intent);
        Log.v(TAG, "Onreceive");
        mContext = context;
        // get remoteviews
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_odyssey_big);

        if (intent.getAction().equals(PlaybackService.MESSAGE_NEWTRACKINFORMATION)) {

            intent.setExtrasClassLoader(context.getClassLoader());

            NowPlayingInformation info = intent.getParcelableExtra(PlaybackService.INTENT_NOWPLAYINGNAME);
            if ( info != null ) {
                TrackModel item = info.getCurrentTrack();
                if ( item != null ) {
                    views.setTextViewText(R.id.widget_big_trackName, item.getTrackName());
                    views.setTextViewText(R.id.widget_big_ArtistAlbum, item.getTrackArtistName());

                    views.setImageViewResource(R.id.widget_big_cover, R.drawable.odyssey_notification);
                    mCoverGenerator = new CoverBitmapGenerator(context, new CoverReceiver(views));
                    mCoverGenerator.getImage(item);
                }

                if (info.getPlaying() == 0) {
                    // Show play icon
                    views.setImageViewResource(R.id.widget_big_play, R.drawable.ic_play_arrow_24dp);
                } else if (info.getPlaying() == 1) {
                    // Show pause icon
                    views.setImageViewResource(R.id.widget_big_play, R.drawable.ic_pause_24dp);
                }
            }
        }

        // TODO is there a better way?
        // reset button actions
        // Main action
        Intent mainIntent = new Intent(context, OdysseyMainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION| Intent.FLAG_ACTIVITY_NO_HISTORY);
        mainIntent.putExtra("Fragment", "currentsong");
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

        // Quit action
//        Intent stopIntent = new Intent(context, PlaybackService.class);
//        stopIntent.putExtra("action", PlaybackService.ACTION_STOP);
//        PendingIntent stopPendingIntent = PendingIntent.getService(context, INTENT_STOP, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);
//        views.setOnClickPendingIntent(R.id.odysseyWidgetStopButton, stopPendingIntent);
        mViews = views;
        mAppWidgetManager = AppWidgetManager.getInstance(context);
        mAppWidgetManager.updateAppWidget(new ComponentName(context, OdysseyWidgetProvider.class), views);
    }

    private class CoverReceiver implements CoverBitmapGenerator.CoverBitmapListener {

        public CoverReceiver(RemoteViews views) {
        }

        @Override
        public void receiveBitmap(BitmapDrawable bm) {

            if (mViews != null && bm != null) {
                mViews.setImageViewBitmap(R.id.widget_big_cover, bm.getBitmap());

                mAppWidgetManager.updateAppWidget(new ComponentName(mContext, OdysseyWidgetProvider.class), mViews);

            }
        }
    }
}
