package com.app.duplicatefilefinder.duplicate_videos;

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
import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection;

/**
 * Created by ${Saquib} on 10-01-2018.
 */

public class MySectionVideo extends StatelessSection {

    private String index;
    private int sameDuplicateSize;
    private DuplicateVideo context;
    private DuplicateFileRemover activity;
    private int unCheckedCount = 1;

    private ArrayList<ModelClass> modelList = new ArrayList<>();

    MySectionVideo(DuplicateFileRemover activity, DuplicateVideo context, String index, HashMap<File, String> list) {

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

        Glide.with(context)
                .load(Uri.fromFile(modelList.get(position).getFile()))
                .into(itemHolder.thumbnail);

        itemHolder.videoPath.setText(modelList.get(position).getPath());

        itemHolder.fileSize.setText(Constant.getStringSizeLengthFile(modelList.get(position).getFile().length()));

        itemHolder.thumbnail.setOnClickListener(view -> {

            File file = new File(modelList.get(position).getPath());

            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                                    android.support.v4.content.FileProvider.getUriForFile(activity, activity.getPackageName() + ".provider", file) : Uri.fromFile(file),
                            "video/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(intent);

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
                    Constant.toastShort(activity, "Can't select all video of same group");
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
        String title = "Group " + index + ": " + sameDuplicateSize + " duplicate video";
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
        ImageView thumbnail;
        TextView videoPath, fileSize;
        CheckBox checkBox;

        MyItemViewHolder(View view) {
            super(view);
            fileSize = view.findViewById(R.id.file_size);
            thumbnail = view.findViewById(R.id.img);
            videoPath = view.findViewById(R.id.imgPath);
            checkBox = view.findViewById(R.id.checkBox);

        }
    }

}
