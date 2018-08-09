package com.app.duplicatefilefinder.duplicate_documents;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.duplicatefilefinder.DuplicateFileRemover;
import com.app.duplicatefilefinder.R;
import com.app.duplicatefilefinder.utils.Constant;
import com.app.duplicatefilefinder.utils.ModelClass;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection;

/**
 * Created by ${Saquib} on 16-03-2018.
 */

public class MySectionDocument extends StatelessSection {

    private String index;
    private int sameDuplicateSize;
    private DuplicateDocuments context;
    private DuplicateFileRemover activity;
    private int unCheckedCount = 1;

    private ArrayList<ModelClass> modelList = new ArrayList<>();

    MySectionDocument(DuplicateFileRemover activity, DuplicateDocuments context, String index, HashMap<File, String> list) {

        super(R.layout.section_header, R.layout.section_item);

        for (Map.Entry entryInner : list.entrySet()) {

            if (modelList.size() != 0) {

                String path = (String) entryInner.getValue();

                if (!context.removeItem.contains(path))
                    context.removeItem.add((String) entryInner.getValue());

            }
            modelList.add(new ModelClass((String) entryInner.getValue(), (File) entryInner.getKey(), modelList.size() != 0));
        }

        sameDuplicateSize = modelList.size();
        this.index = index;
        this.context = context;
        this.activity = activity;

    }

    @Override
    public int getContentItemsTotal() {
        return modelList.size(); // number of items of this section
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        // return a custom instance of ViewHolder for the items of this section
        return new MyItemViewHolder(view);
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {

        MyItemViewHolder itemHolder = (MyItemViewHolder) holder;

        itemHolder.checkBox.setChecked(modelList.get(position).getSelected());

        itemHolder.imageView.setImageResource(R.drawable.file_doc);

        itemHolder.path.setText(modelList.get(position).getPath());

        itemHolder.fileSize.setText(Constant.getStringSizeLengthFile(modelList.get(position).getFile().length()));

        itemHolder.imageView.setOnClickListener(view -> {


            if (modelList.get(position).getPath().startsWith("/storage/emulated")) {

                File file = new File(modelList.get(position).getPath());

                String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
                String mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

                Intent intent = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                                        android.support.v4.content.FileProvider.getUriForFile(activity, activity.getPackageName() + ".provider", file) : Uri.fromFile(file),
                                (extension.equalsIgnoreCase("") || mimetype == null) ? "text/*" : mimetype).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                context.startActivity(intent);
            } else {
                Constant.toastShort(activity, "Can't open file");
            }

        });


        itemHolder.checkBox.setOnClickListener(view -> {

            if (modelList.get(position).getSelected()) {

                unCheckedCount++;
                itemHolder.checkBox.setChecked(!modelList.get(position).getSelected());
                modelList.get(position).setSelected(!modelList.get(position).getSelected());

                context.removeItem.remove(modelList.get(position).getPath());

            } else {
                if (unCheckedCount == 1) {
                    itemHolder.checkBox.setChecked(false);
                    Constant.toastShort(activity, "Can't select all document of same group");
                } else {
                    unCheckedCount--;
                    context.removeItem.add(modelList.get(position).getPath());
                    itemHolder.checkBox.setChecked(!modelList.get(position).getSelected());
                    modelList.get(position).setSelected(!modelList.get(position).getSelected());
                }

            }
        });
    }

    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
        return new MyHeaderViewHolder(view);
    }


    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder) {
        MyHeaderViewHolder headerHolder = (MyHeaderViewHolder) holder;
        String title = "Group " + index + ": " + sameDuplicateSize + " duplicate document";
        headerHolder.tvItem.setText(title);
    }


    private class MyHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvItem;

        MyHeaderViewHolder(View view) {
            super(view);
            tvItem = view.findViewById(R.id.header);

        }
    }

    private class MyItemViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView path,fileSize;
        CheckBox checkBox;

        MyItemViewHolder(View view) {
            super(view);
            fileSize = view.findViewById(R.id.file_size);
            imageView = view.findViewById(R.id.img);
            path = view.findViewById(R.id.imgPath);
            checkBox = view.findViewById(R.id.checkBox);

        }
    }

}
