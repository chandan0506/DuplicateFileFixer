package com.app.duplicatefilefinder.utils;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.app.duplicatefilefinder.duplicate_audio.DuplicateAudioViewModel;
import com.app.duplicatefilefinder.duplicate_documents.DuplicateDocumentViewModel;
import com.app.duplicatefilefinder.duplicate_images.DuplicateImageViewModel;
import com.app.duplicatefilefinder.duplicate_videos.DuplicateVideoViewModel;

import javax.inject.Inject;

/**
 * Created by ${Saquib} on 07-02-2018.
 */

public class ViewModelFactory implements ViewModelProvider.Factory {

    @Inject
    public ViewModelFactory() {

    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(DuplicateImageViewModel.class)) {
            return (T) new DuplicateImageViewModel();
        } else if (modelClass.isAssignableFrom(DuplicateAudioViewModel.class)) {
            return (T) new DuplicateAudioViewModel();
        } else if (modelClass.isAssignableFrom(DuplicateDocumentViewModel.class)) {
            return (T) new DuplicateDocumentViewModel();
        } else if (modelClass.isAssignableFrom(DuplicateVideoViewModel.class)) {
            return (T) new DuplicateVideoViewModel();
        }
        throw new IllegalArgumentException("Unknown class name");
    }
}
