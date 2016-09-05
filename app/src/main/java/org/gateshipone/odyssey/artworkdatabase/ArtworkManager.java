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

package org.gateshipone.odyssey.artworkdatabase;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.android.volley.Response;

import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class ArtworkManager implements ArtistFetchError{
    private static final String TAG = ArtworkManager.class.getSimpleName();

    private ArtworkDatabaseManager mDBManager;
    private ArrayList<onNewArtistImageListener> mArtistListeners;

    private static ArtworkManager mInstance;
    private Context mContext;

    private ArtworkManager(Context context) {

        mDBManager = ArtworkDatabaseManager.getInstance(context);

        mArtistListeners = new ArrayList<>();

        mContext = context;
    }

    public static synchronized ArtworkManager getInstance(Context context) {
        if ( null == mInstance ) {
            mInstance = new ArtworkManager(context);
        }
        return mInstance;
    }

    public Bitmap getArtistImage(final ArtistModel artist) throws ImageNotInDatabaseException  {
        if ( null == artist ) {
            return null;
        }

        Log.v(TAG,"Requested in thread: " + Thread.currentThread().getId());

        long artistID = artist.getArtistID();

        byte[] image;

        if (artistID == -1) {
            Log.v(TAG,"SLOW RESOLVING");
            image = mDBManager.getArtistImage(artist.getArtistName());
        } else {
            image = mDBManager.getArtistImage(artistID);
        }

        if ( null != image) {
            if ( image.length != 0) {
                return BitmapFactory.decodeByteArray(image, 0, image.length);
            }
        } else {
            throw new ImageNotInDatabaseException();
        }
        return null;
    }

    public void fetchArtistImage(final ArtistModel artist) {
        ConnectivityManager cm =
                (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        boolean isWifi = cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI;

        if ( !isWifi ) {
            return;
        }

        LastFMManager.getInstance().fetchImage(artist, new Response.Listener<Pair<byte[], ArtistModel>>() {
            @Override
            public void onResponse(Pair<byte[], ArtistModel> response) {
                new InsertArtistImageTask().execute(response);
            }
        }, this);
    }

    public Drawable getAlbumImage(AlbumModel album) {
        if ( null == album ) {
            return null;
        }
        byte[] image = mDBManager.getAlbumImage( album.getAlbumID());
        if ( null == image ) {
            // FIXME fetch image here


            // FIXME insert image to database here

            // FIXME return image
        }
        return null;
    }


    public void registerOnNewArtistImageListener(onNewArtistImageListener listener) {
        if ( null != listener) {
            mArtistListeners.add(listener);
        }
    }

    public void unregisterOnNewArtistImageListener(onNewArtistImageListener listener) {
        if ( null != listener) {
            mArtistListeners.remove(listener);
        }
    }

    @Override
    public void fetchError(ArtistModel artist) {
        Log.e(TAG, "Error fetching: " + artist.getArtistName());
        // FIXME check if retrying again and again is a problem
//        mDBManager.insertArtistImage(artist, new byte[0]);
    }

    public interface onNewArtistImageListener {
        void newArtistImage(ArtistModel artist);
    }

    public class ImageNotInDatabaseException extends Exception {

    }

    private class InsertArtistImageTask extends AsyncTask<Pair<byte[], ArtistModel>, Object, ArtistModel> {

        @Override
        protected ArtistModel doInBackground(Pair<byte[], ArtistModel>... params) {
            Pair<byte[], ArtistModel> response = params[0];

            Log.v(TAG, "Received image with: " + response.first.length + "bytes for artist: " +  response.second.getArtistName() +
                    " with MBID: " + response.second.getMBID());

            Log.v(TAG,"Inserting in thread: " + Thread.currentThread().getId());
            Bitmap bm = BitmapFactory.decodeByteArray(response.first,0,response.first.length);

            // Scale down the bitmap
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bm = bm.createScaledBitmap(bm, 400,400,true);
            bm.compress(Bitmap.CompressFormat.JPEG, 85, stream);

            mDBManager.insertArtistImage(response.second, stream.toByteArray());



            return response.second;
        }

        protected void onPostExecute(ArtistModel result) {
            for ( onNewArtistImageListener artistListener: mArtistListeners) {
                artistListener.newArtistImage(result);
            }
        }

    }

}
