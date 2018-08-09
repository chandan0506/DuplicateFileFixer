package com.app.duplicatefilefinder.duplicate_documents;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

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
 * Created by ${Saquib} on 16-03-2018.
 */

public class DuplicateDocumentViewModel extends ViewModel {

    private final MutableLiveData<FileRemoverResponse> responseLiveData = new MutableLiveData<>();
    private final CompositeDisposable disposables = new CompositeDisposable();

    private LinkedHashMap<File, String> uniquePathAndFileHashCopy = new LinkedHashMap<>();
    private LinkedHashMap<Integer, LinkedHashMap<File, String>> similarDocumentsCopy = new LinkedHashMap<>();
    private ArrayList<String> pathOfDocuments = new ArrayList<>();

    private int index;


    MutableLiveData<FileRemoverResponse> getDuplicateDocumentResponse() {
        return responseLiveData;
    }

    @SuppressLint("CheckResult")
    void getDuplicateDocument(DuplicateFileRemover activity) {

        Observable.fromCallable(() -> getDocumentsPath(activity))
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

    void refreshDuplicateDocumentList(DuplicateFileRemover activity) {

        index = 0;
        uniquePathAndFileHashCopy.clear();
        similarDocumentsCopy.clear();
        uniquePathAndFileHashCopy.clear();
        pathOfDocuments.clear();

        getDuplicateDocument(activity);

    }


    @SuppressLint("UseSparseArrays")
    private LinkedHashMap<Integer, LinkedHashMap<File, String>> getDocumentsPath(Activity activity) {

        HashMap<Integer, ArrayList<String>> listOfAllDocuments = new HashMap<>();
        String documentsPath;


        ContentResolver cr = activity.getContentResolver();
        Uri uri = MediaStore.Files.getContentUri("external");

        String selectionMimeType = MediaStore.Files.FileColumns.MIME_TYPE + "=?";

        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("pdf");
        String mimeTypeText = MimeTypeMap.getSingleton().getMimeTypeFromExtension("txt");

        String[] selectionArgsPdf = new String[]{mimeType};

        Cursor cursor = cr.query(uri, null, selectionMimeType, selectionArgsPdf, null);
        int column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);


        while (cursor.moveToNext()) {
            documentsPath = cursor.getString(column_index_data);
            pathOfDocuments.add(documentsPath);
        }

        String[] selectionArgsDoc = new String[]{mimeTypeText};
        Cursor cursorDoc = cr.query(uri, null, selectionMimeType, selectionArgsDoc, null);

        while (cursorDoc.moveToNext()) {
            documentsPath = cursorDoc.getString(column_index_data);
            pathOfDocuments.add(documentsPath);
        }

        cursor.close();
        cursorDoc.close();


        boolean installed = Constant.whatsAppInstalledOrNot(activity);

        if (installed) {

            try {

                File file1 = new File(Environment.getExternalStorageDirectory(), "/WhatsApp/Media/WhatsApp Documents/Sent/");

                if (file1.isDirectory()) {

                    File[] listFile = file1.listFiles();

                    for (File aListFile : listFile) {

                        if (!aListFile.getAbsolutePath().contains("/.nomedia"))
                            pathOfDocuments.add(aListFile.getAbsolutePath());

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (String documentPath : pathOfDocuments) {

            File file = new File(documentPath);
            int file_size = Integer.parseInt(String.valueOf(file.length() / 1024));

            ArrayList<String> documentsList;

            if (!listOfAllDocuments.containsKey(file_size)) {
                documentsList = new ArrayList<>();
            } else {
                documentsList = listOfAllDocuments.get(file_size);
            }
            documentsList.add(documentPath);
            listOfAllDocuments.put(file_size, documentsList);
        }

        Iterator iterator = listOfAllDocuments.entrySet().iterator();

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

        return similarDocumentsCopy;
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


                LinkedHashMap<Integer, LinkedHashMap<File, String>> similarDocument = new LinkedHashMap<>();

                similarDocument.put(index, uniquePathAndFileHash);

                similarDocumentsCopy.put(index, uniquePathAndFileHash);

                uniquePathAndFileHashCopy.putAll(uniquePathAndFileHash);

                new Handler(Looper.getMainLooper()).post(() ->
                        responseLiveData.setValue(FileRemoverResponse.success(similarDocument)));

            }
        }

    }


}
