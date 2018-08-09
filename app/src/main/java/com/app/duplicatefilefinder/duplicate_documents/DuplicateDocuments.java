package com.app.duplicatefilefinder.duplicate_documents;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
 * Created by ${Saquib} on 16-03-2018.
 */

public class DuplicateDocuments extends Fragment {
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
    MySectionDocument section;

    public ArrayList<String> removeItem = new ArrayList<>();
    LinkedHashMap<Integer, LinkedHashMap<File, String>> similarDocumentsCopy = new LinkedHashMap<>();

    @Inject
    ViewModelFactory viewModelFactory;

    DuplicateDocumentViewModel viewModel;


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

        setHasOptionsMenu(true);
        setRetainInstance(true);

        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        sectionAdapter = new SectionedRecyclerViewAdapter();

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(DuplicateDocumentViewModel.class);

        viewModel.getDuplicateDocumentResponse().observe(this, this::processResponse);

        viewModel.getDuplicateDocument(activity);

    }

    /*
   * method to handle response
   * */
    private void processResponse(FileRemoverResponse response) {

        switch (response.status) {
            case LOADING:
                Constant.sop("duplicateDoc==loading");
                progressBar.setVisibility(View.VISIBLE);
                if (clean != null) {
                    clean.setVisible(false);
                }
                break;

            case SUCCESS:
                Constant.sop("duplicateDoc==success=" + response.duplicateList.size());
                setSimilarDocumentInList(response.duplicateList);
                break;

            case COMPLETED:

                Constant.sop("duplicateDoc==COMPLETED=" + response.duplicateFinalList.size());

                progressBar.setVisibility(View.GONE);
                similarDocumentsCopy = response.duplicateFinalList;
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
        menu.findItem(R.id.action_refresh).setVisible(false);
        clean = menu.findItem(R.id.action_clean).setVisible((similarDocumentsCopy.size() > 0 && (progressBar.getVisibility() == View.GONE)));
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clean:

                if (removeItem.size() > 0) {

                    AlertDialog dialog = new AlertDialog.Builder(activity).create();
                    dialog.setTitle(R.string.delete_file);
                    dialog.setMessage("Do you want to delete selected Document(s)?");
                    dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes),
                            (dialog1, which) -> {

                                dialog.dismiss();

                                for (int i = 0; i < removeItem.size(); i++) {
                                    Constant.sop("duplicateDoc==removed=" + removeItem.get(i));
                                    deleteFile(removeItem.get(i));
                                }

                                resetAndRefreshList();

                            });

                    dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.no), (dialog1, which) -> {
                        dialog.dismiss();

                    });
                    dialog.show();


                } else {

                    Constant.toastShort(activity, "Select document to delete");
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
     * method to refresh list after deletion
     * */
    private void resetAndRefreshList() {

        sectionAdapter = new SectionedRecyclerViewAdapter();
        recyclerView.setAdapter(null);

        viewModel.refreshDuplicateDocumentList(activity);
    }

    /*
    * method to delete duplicate file
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
    * set duplicate documents file in list
    * */
    private void setSimilarDocumentInList(LinkedHashMap<Integer, LinkedHashMap<File, String>> list) {

        for (Map.Entry<Integer, LinkedHashMap<File, String>> entry : list.entrySet()) {

            section = new MySectionDocument(activity, DuplicateDocuments.this, String.valueOf(entry.getKey()), entry.getValue());

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
