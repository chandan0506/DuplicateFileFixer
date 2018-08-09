package com.app.duplicatefilefinder.utils;

import java.io.File;

/**
 * Created by ${Saquib} on 09-02-2018.
 */

public class ModelClass {

    private String path;
    private File file;
    private boolean isSelected;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public boolean getSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    public ModelClass(String path, File file, boolean isSelected) {
        this.path = path;
        this.file = file;
        this.isSelected = isSelected;
    }

}
