/*
 * Copyright (C) 2018 Team Team Gateship-One
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

package org.gateshipone.odyssey.viewmodels;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.AsyncTask;

import org.gateshipone.odyssey.models.FileModel;
import org.gateshipone.odyssey.utils.PermissionHelper;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class FileViewModel extends GenericViewModel<FileModel> {

    /**
     * The parent directory.
     */
    private final FileModel mCurrentDirectory;

    private FileViewModel(@NonNull final Application application, final FileModel directory) {
        super(application);

        mCurrentDirectory = directory;
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    void loadData() {
        new AsyncTask<Void, Void, List<FileModel>>() {

            @Override
            protected List<FileModel> doInBackground(Void... voids) {
                return PermissionHelper.getFilesForDirectory(getApplication(), mCurrentDirectory);
            }

            @Override
            protected void onPostExecute(List<FileModel> result) {
                setData(result);
            }
        }.execute();
    }

    public static class FileViewModelFactory extends ViewModelProvider.NewInstanceFactory {

        private final Application mApplication;

        private final FileModel mCurrentDirectory;

        public FileViewModelFactory(final Application application, final FileModel directory) {
            mApplication = application;
            mCurrentDirectory = directory;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new FileViewModel(mApplication, mCurrentDirectory);
        }
    }
}
