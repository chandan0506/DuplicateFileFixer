package com.app.duplicatefilefinder.di;

import android.arch.lifecycle.ViewModelProvider;

import com.app.duplicatefilefinder.utils.ViewModelFactory;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by ${Saquib} on 12-01-2018.
 */

@Module
public class UtilsModule {

    @Provides
    @Singleton
    ViewModelProvider.Factory getViewModelFactory() {
        return new ViewModelFactory();
    }
}
