package com.app.duplicatefilefinder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.app.duplicatefilefinder.duplicate_audio.DuplicateAudio;
import com.app.duplicatefilefinder.duplicate_documents.DuplicateDocuments;
import com.app.duplicatefilefinder.duplicate_images.DuplicateImage;
import com.app.duplicatefilefinder.duplicate_videos.DuplicateVideo;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by ${Saquib} on 14-03-2018.
 */

public class DuplicateFileRemover extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.tabs)
    TabLayout tabLayout;

    @BindView(R.id.viewpager)
    ViewPager viewPager;

    private static final int REQUEST_STORAGE = 111;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.duplicate_file_remover);
        ButterKnife.bind(this);
        init();
    }

    private void init() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.duplicate_files));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!CheckingPermissionIsEnabledOrNot()) {
                ActivityCompat.requestPermissions(DuplicateFileRemover.this, PERMISSIONS_STORAGE, REQUEST_STORAGE);
            } else {
                setupViewPager(viewPager);
                tabLayout.setupWithViewPager(viewPager);
            }
        } else {
            setupViewPager(viewPager);
            tabLayout.setupWithViewPager(viewPager);
        }
    }

    /*
    * check storage permission
    * */
    public boolean CheckingPermissionIsEnabledOrNot() {

        int WRITE_EXTERNAL_STORAGE = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int READ_EXTERNAL_STORAGE = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE);

        return WRITE_EXTERNAL_STORAGE == PackageManager.PERMISSION_GRANTED &&
                READ_EXTERNAL_STORAGE == PackageManager.PERMISSION_GRANTED;
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private void showStorageRationale() {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle(R.string.rationale_title);
        dialog.setMessage("Grant storage permission");
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok),
                (dialog1, which) -> ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_STORAGE));
        dialog.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_STORAGE) {

            if (grantResults.length > 0 && permissions.length == grantResults.length) {

                setupViewPager(viewPager);
                tabLayout.setupWithViewPager(viewPager);

            } else {
                showStorageRationale();
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter pagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        pagerAdapter.addFragment(new DuplicateImage(), "Image");
        pagerAdapter.addFragment(new DuplicateVideo(), "Video");
        pagerAdapter.addFragment(new DuplicateAudio(), "Audio");
        pagerAdapter.addFragment(new DuplicateDocuments(), "Document");
        viewPager.setOffscreenPageLimit(4);
        viewPager.setAdapter(pagerAdapter);
    }


    class ViewPagerAdapter extends FragmentPagerAdapter {

        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return false;
    }
}
