package com.app.duplicatefilefinder;

import android.app.Application;
import android.content.Context;

import com.app.duplicatefilefinder.di.AppComponent;
import com.app.duplicatefilefinder.di.AppModule;
import com.app.duplicatefilefinder.di.DaggerAppComponent;
import com.app.duplicatefilefinder.di.UtilsModule;

/**
 * Created by Saquib on 28-12-2017.
 */

public class MyApplication extends Application {
    AppComponent appComponent;
    Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        appComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).utilsModule(new UtilsModule()).build();
    }

    public AppComponent getAppComponent() {
        return appComponent;
    }

}
