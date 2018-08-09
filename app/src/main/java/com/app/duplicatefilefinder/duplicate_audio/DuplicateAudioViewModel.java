package com.app.duplicatefilefinder.duplicate_audio;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

import com.app.duplicatefilefinder.utils.Constant;
import com.app.duplicatefilefinder.DuplicateFileRemover;
import com.app.duplicatefilefinder.utils.FileRemoverResponse;

import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by ${Saquib} on 19-03-2018.
 */

public class DuplicateAudioViewModel extends ViewModel {

    private final MutableLiveData<FileRemoverResponse> responseLiveData = new MutableLiveData<>();
    private final CompositeDisposable disposables = new CompositeDisposable();

    private LinkedHashMap<File, String> uniquePathAndFileHashCopy = new LinkedHashMap<>();
    private LinkedHashMap<Integer, LinkedHashMap<File, String>> similarAudioCopy = new LinkedHashMap<>();
    private ArrayList<String> pathOfAudio = new ArrayList<>();
    private int index;


    MutableLiveData<FileRemoverResponse> getDuplicateAudioResponse() {
        return responseLiveData;
    }

    @SuppressLint("CheckResult")
    void getDuplicateAudio(DuplicateFileRemover activity) {
//        new FindDuplicateAudio(activity).executeOnExecutor(THREAD_POOL_EXECUTOR);

        Observable.fromCallable(() -> getAudioPath(activity))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe((d) -> {
                    disposables.add(d);
                    responseLiveData.setValue(FileRemoverResponse.loading());
                })
                .subscribe((result) -> {
                    responseLiveData.setValue(FileRemoverResponse.completed(result));
                });
    }

    @Override
    protected void onCleared() {
        disposables.clear();
    }

    void refreshDuplicateAudioList(DuplicateFileRemover activity) {

        index = 0;
        uniquePathAndFileHashCopy.clear();
        similarAudioCopy.clear();
        uniquePathAndFileHashCopy.clear();
        pathOfAudio.clear();

        getDuplicateAudio(activity);

    }


    @SuppressLint("UseSparseArrays")
    private LinkedHashMap<Integer, LinkedHashMap<File, String>> getAudioPath(Activity activity) {

        Uri uri;
        Cursor cursor;
        int column_index_data;

        HashMap<Integer, ArrayList<String>> listOfAllAudio = new HashMap<>();

        String audioPath;
        uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION
        };

        cursor = activity.getContentResolver().query(uri, projection, selection, null, null);

        column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

        while (cursor.moveToNext()) {

            audioPath = cursor.getString(column_index_data);

            pathOfAudio.add(audioPath);

        }

        cursor.close();

        boolean installed = Constant.whatsAppInstalledOrNot(activity);

        if (installed) {

            try {
                File file1 = new File(android.os.Environment.getExternalStorageDirectory(), "/WhatsApp/Media/WhatsApp Audio/Sent/");

                if (file1.isDirectory()) {

                    File[] listFile = file1.listFiles();

                    for (File aListFile : listFile) {

                        if (!aListFile.getAbsolutePath().contains("/.nomedia")) {
                            pathOfAudio.add(aListFile.getAbsolutePath());
                        }

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (String path : pathOfAudio) {

            File file = new File(path);
            int file_size = Integer.parseInt(String.valueOf(file.length() / 1024));

            ArrayList<String> audioList;

            if (!listOfAllAudio.containsKey(file_size)) {
                audioList = new ArrayList<>();
            } else {
                audioList = listOfAllAudio.get(file_size);
            }
            audioList.add(path);
            listOfAllAudio.put(file_size, audioList);
        }


        Iterator iterator = listOfAllAudio.entrySet().iterator();

        int noOfCore = Constant.getNumberOfCores();

        ExecutorService executor = Executors.newFixedThreadPool(noOfCore);

        while (iterator.hasNext()) {

            Map.Entry entry = (Map.Entry) iterator.next();

            ArrayList<String> pathList = (ArrayList<String>) entry.getValue();

            if (pathList.size() < 2) {
                continue;
            }

            Runnable worker = new WorkerThread(pathList);
            executor.submit(worker);

        }

        executor.shutdown();

        while (!executor.isTerminated()) {
            Constant.sop("threads is terminated");
        }

        return similarAudioCopy;
    }


    class WorkerThread implements Runnable {
        ArrayList<String> pathList;

        WorkerThread(ArrayList<String> pathList) {
            this.pathList = pathList;
        }

        @Override
        public void run() {
            calculateDuplicate(pathList);
        }
    }

    /*
    * method to calculate Duplicate
    * */
    private void calculateDuplicate(ArrayList<String> pathList) {

        HashMap<String, byte[]> pathMdHashMap = new HashMap<>();

        for (int i = 0; i < pathList.size(); i++) {

            LinkedHashMap<File, String> uniquePathAndFileHash = new LinkedHashMap<>();

            if (pathList.get(i) == null || !(new File(pathList.get(i)).isFile()) || !(new File(pathList.get(i)).exists()))
                continue;

            byte[] fileHash1 = Constant.fileHash(new File(pathList.get(i)));

            if (fileHash1 == null)
                continue;

            for (int j = i + 1; j < pathList.size(); j++) {

                String path = pathList.get(j);

                if (path == null || !(new File(path).isFile()) || !(new File(path).exists()))
                    continue;

                byte[] fileHash2;

                if (pathMdHashMap.containsKey(path)) {
                    fileHash2 = pathMdHashMap.get(path);
                } else {
                    fileHash2 = Constant.fileHash(new File(pathList.get(j)));
                    pathMdHashMap.put(path, fileHash2);
                }

                boolean flag = MessageDigest.isEqual(fileHash1, fileHash2);

                if (flag) {

                    if (!uniquePathAndFileHashCopy.containsValue(pathList.get(i))) {
                        uniquePathAndFileHash.put(new File(pathList.get(i)), pathList.get(i));
                    }

                    if (!uniquePathAndFileHashCopy.containsValue(path)) {
                        uniquePathAndFileHash.put(new File(path), path);
                    }
                }
            }

            if (uniquePathAndFileHash.size() > 1) {

                index++;

                LinkedHashMap<Integer, LinkedHashMap<File, String>> similarAudio = new LinkedHashMap<>();

                similarAudio.put(index, uniquePathAndFileHash);

                similarAudioCopy.put(index, uniquePathAndFileHash);

                uniquePathAndFileHashCopy.putAll(uniquePathAndFileHash);

                /*runOnUiThread(() ->
                        responseLiveData.setValue(FileRemoverResponse.success(similarAudio)));*/

                new Handler(Looper.getMainLooper()).post(() -> responseLiveData.setValue(FileRemoverResponse.success(similarAudio)));

            }
        }

    }

/* @SuppressLint("StaticFieldLeak")
    class FindDuplicateAudio extends AsyncTask<Void, Void, LinkedHashMap<Integer, LinkedHashMap<File, String>>> {
        DuplicateFileRemover activity;

        FindDuplicateAudio(DuplicateFileRemover activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            responseLiveData.setValue(FileRemoverResponse.loading());
        }

        @Override
        protected LinkedHashMap<Integer, LinkedHashMap<File, String>> doInBackground(Void... voids) {
            return getAudioPath(activity);
        }

        @Override
        protected void onPostExecute(LinkedHashMap<Integer, LinkedHashMap<File, String>> list) {
            super.onPostExecute(list);
            responseLiveData.setValue(FileRemoverResponse.completed(list));

        }
    }*/

}
