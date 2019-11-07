/*
 * Copyright (C) 2019 Team Gateship-One
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

package org.gateshipone.odyssey.artwork.network.artprovider;

import android.content.Context;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.gateshipone.odyssey.artwork.network.ArtworkRequestModel;
import org.gateshipone.odyssey.artwork.network.ImageResponse;
import org.json.JSONException;

public abstract class ArtProvider {

    public interface ArtFetchError {
        void fetchJSONException(final ArtworkRequestModel model, final Context context, final JSONException exception);

        void fetchVolleyError(final ArtworkRequestModel model, final Context context, final VolleyError error);

        void fetchError(final ArtworkRequestModel model, final Context context);
    }

    public abstract void fetchImage(final ArtworkRequestModel model, final Context context, final Response.Listener<ImageResponse> listener, final ArtFetchError errorListener);
}
