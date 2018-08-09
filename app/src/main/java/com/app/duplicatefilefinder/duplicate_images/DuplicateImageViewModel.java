package com.app.duplicatefilefinder.duplicate_images;

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

public class DuplicateImageViewModel extends ViewModel {

    private final MutableLiveData<FileRemoverResponse> responseLiveData = new MutableLiveData<>();
    private final CompositeDisposable disposables = new CompositeDisposable();

    private LinkedHashMap<File, String> uniquePathAndFileHashCopy = new LinkedHashMap<>();
    private LinkedHashMap<Integer, LinkedHashMap<File, String>> similarImageCopy = new LinkedHashMap<>();
    private ArrayList<String> pathOfImages = new ArrayList<>();


    private int index;

    MutableLiveData<FileRemoverResponse> getDuplicateImagesResponse() {
        return responseLiveData;
    }

    @SuppressLint("CheckResult")
    void getDuplicateImages(DuplicateFileRemover activity) {

        Observable.fromCallable(() -> getImagesPath(activity))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe((d) -> {
                    disposables.add(d);
                    responseLiveData.setValue(FileRemoverResponse.loading());
                })
                .subscribe((result) -> responseLiveData.setValue(FileRemoverResponse.completed(result)));
    }

    @Override
    protected void onCleared() {
        disposables.clear();
    }

    void refreshDuplicateImageList(DuplicateFileRemover activity) {

        index = 0;
        uniquePathAndFileHashCopy.clear();
        similarImageCopy.clear();
        uniquePathAndFileHashCopy.clear();
        pathOfImages.clear();

        getDuplicateImages(activity);

    }


    @SuppressLint("UseSparseArrays")
    private LinkedHashMap<Integer, LinkedHashMap<File, String>> getImagesPath(Activity activity) {

        HashMap<Integer, ArrayList<String>> listOfAllImages = new HashMap<>();

        Cursor cursor;
        int column_index_data;
        String PathOfImage;
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {MediaStore.MediaColumns.DATA,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME};

        cursor = activity.getContentResolver().query(uri, projection, null,
                null, null);

        assert cursor != null;

        column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

        while (cursor.moveToNext()) {

            PathOfImage = cursor.getString(column_index_data);
            pathOfImages.add(PathOfImage);

        }

        cursor.close();

        boolean installed = Constant.whatsAppInstalledOrNot(activity);

        if (installed) {

            try {

                File file1 = new File(android.os.Environment.getExternalStorageDirectory(), "/WhatsApp/Media/WhatsApp Images/Sent/");

                if (file1.isDirectory()) {

                    File[] listFile = file1.listFiles();

                    for (File aListFile : listFile) {

                        if (!aListFile.getAbsolutePath().contains("/.nomedia"))
                            pathOfImages.add(aListFile.getAbsolutePath());

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (String imgPath : pathOfImages) {

            File file = new File(imgPath);
            int file_size = Integer.parseInt(String.valueOf(file.length() / 1024));

            ArrayList<String> imageList;

            if (!listOfAllImages.containsKey(file_size)) {
                imageList = new ArrayList<>();
            } else {
                imageList = listOfAllImages.get(file_size);
            }
            imageList.add(imgPath);
            listOfAllImages.put(file_size, imageList);
        }

        Iterator iterator = listOfAllImages.entrySet().iterator();

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

        return similarImageCopy;
    }


    private class WorkerThread implements Runnable {
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

                LinkedHashMap<Integer, LinkedHashMap<File, String>> similarImage = new LinkedHashMap<>();

                similarImage.put(index, uniquePathAndFileHash);

                similarImageCopy.put(index, uniquePathAndFileHash);

                uniquePathAndFileHashCopy.putAll(uniquePathAndFileHash);

                new Handler(Looper.getMainLooper()).post(() ->
                        responseLiveData.setValue(FileRemoverResponse.success(similarImage)));

            }
        }

    }
}
