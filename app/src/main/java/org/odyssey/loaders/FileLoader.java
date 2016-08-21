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

package org.odyssey.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import org.odyssey.models.FileModel;

import java.util.List;

public class FileLoader extends AsyncTaskLoader<List<FileModel>> {

    /**
     * The parent directory.
     */
    private final FileModel mCurrentDirectory;

    public FileLoader(Context context, FileModel directory) {
        super(context);

        mCurrentDirectory = directory;
    }

    /**
     * Load all FileModel objects for the given directory FileModel.
     */
    @Override
    public List<FileModel> loadInBackground() {

        return mCurrentDirectory.listFilesSorted();
    }

    /**
     * Start loading the data.
     * A previous load dataset will be ignored
     */
    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    /**
     * Stop the loader and cancel the current task.
     */
    @Override
    protected void onStopLoading() {
        cancelLoad();
    }
}
