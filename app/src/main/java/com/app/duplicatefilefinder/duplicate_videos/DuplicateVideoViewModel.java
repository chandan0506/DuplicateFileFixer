package com.app.duplicatefilefinder.duplicate_videos;

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
 * Created by ${Saquib} on 13-03-2018.
 */

public class DuplicateVideoViewModel extends ViewModel {

    private final MutableLiveData<FileRemoverResponse> responseLiveData = new MutableLiveData<>();
    private final CompositeDisposable disposables = new CompositeDisposable();

    private LinkedHashMap<File, String> uniquePathAndFileHashCopy = new LinkedHashMap<>();
    private LinkedHashMap<Integer, LinkedHashMap<File, String>> similarVideoCopy = new LinkedHashMap<>();
    private ArrayList<String> pathOfVideos = new ArrayList<>();

    private int index;


    MutableLiveData<FileRemoverResponse> getDuplicateVideoResponse() {
        return responseLiveData;
    }

    @SuppressLint("CheckResult")
    void getDuplicateVideo(DuplicateFileRemover activity) {

        Observable.fromCallable(() -> getVideoPath(activity))
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

    void refreshDuplicateVideoList(DuplicateFileRemover activity) {
        index = 0;
        uniquePathAndFileHashCopy.clear();
        similarVideoCopy.clear();
        uniquePathAndFileHashCopy.clear();
        pathOfVideos.clear();

        getDuplicateVideo(activity);

    }


    @SuppressLint("UseSparseArrays")
    public LinkedHashMap<Integer, LinkedHashMap<File, String>> getVideoPath(Activity activity) {

        Uri uri;
        Cursor cursor;
        int column_index_data, column_index_folder_name, column_id, thumb;

        HashMap<Integer, ArrayList<String>> listOfAllVideos = new HashMap<>();

        String PathOfVideo = null;
        uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.Video.Media.BUCKET_DISPLAY_NAME, MediaStore.Video.Media._ID, MediaStore.Video.Thumbnails.DATA};

        final String orderBy = MediaStore.Video.Media.DATE_TAKEN;

        cursor = activity.getContentResolver().query(uri, projection, null, null, orderBy + " DESC");

        column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        column_index_folder_name = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
        column_id = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
        thumb = cursor.getColumnIndexOrThrow(MediaStore.Video.Thumbnails.DATA);

        while (cursor.moveToNext()) {

            PathOfVideo = cursor.getString(column_index_data);

            pathOfVideos.add(PathOfVideo);

        }

        cursor.close();


        boolean installed = Constant.whatsAppInstalledOrNot(activity);

        if (installed) {

            try {

                File file1 = new File(android.os.Environment.getExternalStorageDirectory(), "/WhatsApp/Media/WhatsApp Video/Sent/");

                if (file1.isDirectory()) {

                    File[] listFile = file1.listFiles();

                    for (File aListFile : listFile) {
                        if (!aListFile.getAbsolutePath().contains("/.nomedia"))
                            pathOfVideos.add(aListFile.getAbsolutePath());

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (String imgPath : pathOfVideos) {

            File file = new File(imgPath);
            int file_size = Integer.parseInt(String.valueOf(file.length() / 1024));

            ArrayList<String> videosList;

            if (!listOfAllVideos.containsKey(file_size)) {
                videosList = new ArrayList<>();
            } else {
                videosList = listOfAllVideos.get(file_size);
            }
            videosList.add(imgPath);
            listOfAllVideos.put(file_size, videosList);
        }


        Iterator iterator = listOfAllVideos.entrySet().iterator();

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

        return similarVideoCopy;
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


                LinkedHashMap<Integer, LinkedHashMap<File, String>> similarVideo = new LinkedHashMap<>();

                similarVideo.put(index, uniquePathAndFileHash);

                similarVideoCopy.put(index, uniquePathAndFileHash);

                uniquePathAndFileHashCopy.putAll(uniquePathAndFileHash);


                new Handler(Looper.getMainLooper()).post(() ->
                        responseLiveData.setValue(FileRemoverResponse.success(similarVideo)));

            }
        }

    }
}
