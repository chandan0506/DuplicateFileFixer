package com.app.duplicatefilefinder.duplicate_images;

import android.Manifest;
import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.app.duplicatefilefinder.utils.Constant;
import com.app.duplicatefilefinder.DuplicateFileRemover;
import com.app.duplicatefilefinder.utils.FileRemoverResponse;
import com.app.duplicatefilefinder.MyApplication;
import com.app.duplicatefilefinder.R;
import com.app.duplicatefilefinder.utils.ViewModelFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;

/**
 * Created by ${Saquib} on 05-03-2018.
 */

public class DuplicateImage extends Fragment {
    View rootView;
    MenuItem clean;
    DuplicateFileRemover activity;

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    @BindView(R.id.recyclerview)
    RecyclerView recyclerView;

    @BindView(R.id.no_duplicate)
    TextView noDuplicate;

    public SectionedRecyclerViewAdapter sectionAdapter;
    MySectionImage section;

    public ArrayList<String> removeItem = new ArrayList<>();
    LinkedHashMap<Integer, LinkedHashMap<File, String>> similarImageCopy = new LinkedHashMap<>();

    private static final int REQUEST_STORAGE = 123;

    @SuppressLint("InlinedApi")
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Inject
    ViewModelFactory viewModelFactory;

    DuplicateImageViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.duplicate_file, container, false);
        ButterKnife.bind(this, rootView);
        activity = (DuplicateFileRemover) getActivity();
        ((MyApplication) activity.getApplication()).getAppComponent().doInjection(this);
        initialize();
        return rootView;
    }

    private void initialize() {
//        activity.getSupportActionBar().setTitle(getString(R.string.Duplicate_Images));

        setHasOptionsMenu(true);
        setRetainInstance(true);

        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        sectionAdapter = new SectionedRecyclerViewAdapter();

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(DuplicateImageViewModel.class);

        viewModel.getDuplicateImagesResponse().observe(this, this::processResponse);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!CheckingPermissionIsEnabledOrNot()) {
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_STORAGE);
            } else {
                viewModel.getDuplicateImages(activity);
            }
        } else {
            viewModel.getDuplicateImages(activity);
        }

    }


    /*
  * method to handle response
  * */
    private void processResponse(FileRemoverResponse response) {

        switch (response.status) {
            case LOADING:
                Constant.sop("duplicateImage==loading");
                progressBar.setVisibility(View.VISIBLE);
                if (clean != null) {
                    clean.setVisible(false);
                }
                break;

            case SUCCESS:
                Constant.sop("duplicateImage==success=" + response.duplicateList.size());
                setSimilarImageInList(response.duplicateList);
                break;

            case COMPLETED:

                Constant.sop("duplicateImage==COMPLETED=" + response.duplicateFinalList.size());

                progressBar.setVisibility(View.GONE);
                similarImageCopy = response.duplicateFinalList;
                noDuplicate.setVisibility(response.duplicateFinalList.size() == 0 ? View.VISIBLE : View.GONE);

                if (clean != null && response.duplicateFinalList.size() > 0) {
                    clean.setVisible(true);
                }

                break;

            default:
                break;
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_STORAGE) {

            if (grantResults.length > 0 && permissions.length == grantResults.length) {

                viewModel.getDuplicateImages(activity);

            } else {
                showStorageRationale();
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public boolean CheckingPermissionIsEnabledOrNot() {

        int WRITE_EXTERNAL_STORAGE = ContextCompat.checkSelfPermission(activity.getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int READ_EXTERNAL_STORAGE = ContextCompat.checkSelfPermission(activity.getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE);

        return WRITE_EXTERNAL_STORAGE == PackageManager.PERMISSION_GRANTED &&
                READ_EXTERNAL_STORAGE == PackageManager.PERMISSION_GRANTED;
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private void showStorageRationale() {
        AlertDialog dialog = new AlertDialog.Builder(activity).create();
        dialog.setTitle(R.string.rationale_title);
        dialog.setMessage("Grant storage permission");
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok),
                (dialog1, which) -> ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_STORAGE));
        dialog.show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
        menu.findItem(R.id.action_refresh).setVisible(false);
        clean = menu.findItem(R.id.action_clean).setVisible((similarImageCopy.size() > 0 && (progressBar.getVisibility() == View.GONE)));
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.action_clean:

                if (removeItem.size() > 0) {

                    AlertDialog dialog = new AlertDialog.Builder(activity).create();
                    dialog.setTitle(R.string.delete_file);
                    dialog.setMessage("Do you want to delete selected Image(s)?");
                    dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes),
                            (dialog1, which) -> {

                                dialog.dismiss();

                                for (int i = 0; i < removeItem.size(); i++) {
                                    Constant.sop("duplicateImage==removed=" + removeItem.get(i));
                                    deleteFile(removeItem.get(i));
                                }

                                resetAndRefreshList();

                            });

                    dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.no), (dialog1, which) -> {
                        dialog.dismiss();

                    });
                    dialog.show();


                } else {
                    Constant.toastShort(activity, "Select image to delete");
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
    * method to reset and refresh duplicate images list
    * */
    private void resetAndRefreshList() {

        sectionAdapter = new SectionedRecyclerViewAdapter();
        recyclerView.setAdapter(null);

        viewModel.refreshDuplicateImageList(activity);

    }


    /*
    * method to delete file
    * */
    public void deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
            if (file.exists()) {
                try {
                    file.getCanonicalFile().delete();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (file.exists()) {
                    activity.deleteFile(file.getName());
                }
            }
        }
    }

    /*
    * method to set duplicate images in list
    * */
    private void setSimilarImageInList(LinkedHashMap<Integer, LinkedHashMap<File, String>> list) {

        for (Map.Entry<Integer, LinkedHashMap<File, String>> entry : list.entrySet()) {

            section = new MySectionImage(activity, DuplicateImage.this, String.valueOf(entry.getKey()), entry.getValue());

            if (sectionAdapter.getItemCount() == 0) {
                sectionAdapter.addSection(section);
                recyclerView.setAdapter(sectionAdapter);
            } else {
                sectionAdapter.addSection(section);
                sectionAdapter.notifyItemInserted(sectionAdapter.getItemCount() + 1);
            }

        }

    }
}
