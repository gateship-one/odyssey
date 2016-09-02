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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.android.volley.Response;

import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;

public class ArtworkManager {

    private ArtworkDatabaseManager mDBManager;

    private Context mContext;

    public ArtworkManager(Context context) {
        mContext = context;
        mDBManager = new ArtworkDatabaseManager(context);
    }

    public Drawable getArtistImage(final ArtistModel artist)  {
        if ( null == artist ) {
            return null;
        }
        byte[] image = mDBManager.getArtistImage(artist.getArtistID());
        if ( null == image ) {
            // FIXME fetch image here
            MusicBrainzManager.getInstance(mContext).fetchImage(artist.getArtistName(), new Response.Listener<byte[]>() {
                @Override
                public void onResponse(byte[] response) {
                    Log.v(ArtworkManager.class.getSimpleName(), "Received image with: " + response.length + "bytes for artist: " +  artist.getArtistName());
                }
            });

            // FIXME insert image to database here

            // FIXME return image
        }

        return null;
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
}
