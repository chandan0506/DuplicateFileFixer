package com.app.duplicatefilefinder.di;


import com.app.duplicatefilefinder.duplicate_audio.DuplicateAudio;
import com.app.duplicatefilefinder.duplicate_documents.DuplicateDocuments;
import com.app.duplicatefilefinder.duplicate_images.DuplicateImage;
import com.app.duplicatefilefinder.duplicate_videos.DuplicateVideo;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by Saquib on 12-01-2018.
 */

@Component(modules = {AppModule.class, UtilsModule.class})
@Singleton
public interface AppComponent {

    void doInjection(DuplicateAudio duplicateAudio);

    void doInjection(DuplicateDocuments duplicateDocuments);

    void doInjection(DuplicateVideo duplicateVideo);

    void doInjection(DuplicateImage duplicateImage);
}
