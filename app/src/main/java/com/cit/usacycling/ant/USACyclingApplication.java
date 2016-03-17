package com.cit.usacycling.ant;

import android.app.Application;

import com.cit.usacycling.ant.dagger.ApplicationModule;

import java.util.Arrays;
import java.util.List;

import dagger.ObjectGraph;

/**
 * Created by nikolay.nikolov on 2.11.2015
 */
public class USACyclingApplication extends Application {

    private static ObjectGraph objectGraph;

    public static ObjectGraph getObjectGraph() {
        return objectGraph;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        objectGraph = ObjectGraph.create(getModules().toArray());
        Thread.setDefaultUncaughtExceptionHandler(new USACyclingExceptionHandler(this));
    }

    private List<Object> getModules() {
        return Arrays.<Object>asList(new ApplicationModule(this));
    }
}
