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

package org.odyssey.utils;

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Pair;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

/*
 * Loaderclass for covers
 */
public class AsyncLoader extends AsyncTask<AsyncLoader.CoverViewHolder, Void, Bitmap> {

    private CoverViewHolder mCover;
    private static boolean mIsScaled;
    private static final String TAG = "OdysseyAsyncLoader";

    /*
     * Wrapperclass for covers
     */
    public static class CoverViewHolder {
        public String imagePath;
        // public String labelText;
        public WeakReference<ImageView> coverViewReference;
        public WeakReference<ViewSwitcher> coverViewSwitcher;
        public AsyncLoader task;
        //public WeakReference<LruCache<String, Bitmap>> cache;
        public Pair<Integer,Integer> imageDimension;
    }

    @Override
    protected Bitmap doInBackground(CoverViewHolder... params) {
        mCover = params[0];
        if (mCover.imagePath != null) {
            return decodeSampledBitmapFromResource(mCover.imagePath, mCover.imageDimension.first, mCover.imageDimension.second);
        }

        return null;
    }

    public static Bitmap decodeSampledBitmapFromResource(String pathName, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);

        // Calculate inSampleSize
        if (reqWidth == 0 && reqHeight == 0) {
            // check if the layout of the view already set
            options.inSampleSize = 1;
            mIsScaled = false;
        } else {
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            mIsScaled = true;
        }

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(pathName, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and
            // keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    @Override
    protected void onPostExecute(Bitmap result) {

        super.onPostExecute(result);

        // set mCover if exists
        if (mCover.coverViewReference.get() != null && result != null) {
            // FIXME Disable cache for now because it is never checked, just stored
//            if (mCover.cache != null && mIsScaled) {
//                // only use cache if image was scaled
//                mCover.cache.get().put(mCover.imagePath, result);
//            }
            mCover.coverViewReference.get().setImageBitmap(result);
            mCover.coverViewSwitcher.get().setDisplayedChild(1);
        }
    }
}