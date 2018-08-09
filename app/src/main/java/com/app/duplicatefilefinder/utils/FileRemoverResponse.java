package com.app.duplicatefilefinder.utils;


import java.io.File;
import java.util.LinkedHashMap;

import static com.app.duplicatefilefinder.utils.Status.COMPLETED;
import static com.app.duplicatefilefinder.utils.Status.LOADING;
import static com.app.duplicatefilefinder.utils.Status.SUCCESS;


/**
 * Created by ${Saquib} on 19-03-2018.
 */

public class FileRemoverResponse {

    public final Status status;
    public final LinkedHashMap<Integer, LinkedHashMap<File, String>> duplicateList;
    public final LinkedHashMap<Integer, LinkedHashMap<File, String>> duplicateFinalList;

    private FileRemoverResponse(Status status, LinkedHashMap<Integer, LinkedHashMap<File, String>> duplicateList, LinkedHashMap<Integer, LinkedHashMap<File, String>> duplicateFinalList) {
        this.status = status;
        this.duplicateList = duplicateList;
        this.duplicateFinalList = duplicateFinalList;
    }

    public static FileRemoverResponse loading() {
        return new FileRemoverResponse(LOADING, null, null);
    }

    public static FileRemoverResponse success(LinkedHashMap<Integer, LinkedHashMap<File, String>> duplicateList) {
        return new FileRemoverResponse(SUCCESS, duplicateList, null);
    }

    public static FileRemoverResponse completed(LinkedHashMap<Integer, LinkedHashMap<File, String>> duplicateFinalList) {
        return new FileRemoverResponse(COMPLETED, null, duplicateFinalList);
    }

}
