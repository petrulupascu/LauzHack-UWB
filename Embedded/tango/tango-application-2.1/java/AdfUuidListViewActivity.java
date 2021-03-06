/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.hefr.etu.zoutao_wen.tangoapplication;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;
import com.google.atap.tangoservice.TangoErrorException;

import java.io.File;
import java.util.ArrayList;

import static ch.hefr.etu.zoutao_wen.tangoapplication.MainActivity.ACTIVITY;

/**
 * This class lets you manage ADFs between this class's Application Package folder and API private
 * space. This showcases three things: Import, Export, and Delete an ADF file from API private
 * space to any known and accessible file path.
 */
public class AdfUuidListViewActivity extends Activity implements SetAdfNameDialog.CallbackListener {
    public static final String UUID =
            "ch.hefr.etu.zoutao_wen.tangoapplication.getuuid";
    public static final int UUID_OK = 1;
    public static final int UUID_NO = 2;
    public static final int UUID_REQUEST = 3;

    private ListView mTangoSpaceAdfListView, mAppSpaceAdfListView;
    private AdfUuidArrayAdapter mTangoSpaceAdfListAdapter, mAppSpaceAdfListAdapter;
    private ArrayList<AdfData> mTangoSpaceAdfDataList, mAppSpaceAdfDataList;
    private String[] mTangoSpaceMenuStrings, mAppSpaceMenuStrings;
    private String mAppSpaceAdfFolder;
    private Tango mTango;
    private volatile boolean mIsTangoReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.uuid_list_view);
        mTangoSpaceMenuStrings = getResources().getStringArray(
                R.array.set_dialog_menu_items_api_space);
        mAppSpaceMenuStrings = getResources().getStringArray(
                R.array.set_dialog_menu_items_app_space);

        // Get API ADF ListView ready.
        mTangoSpaceAdfListView = (ListView) findViewById(R.id.uuid_list_view_tango_space);
        mTangoSpaceAdfDataList = new ArrayList<AdfData>();
        mTangoSpaceAdfListAdapter = new AdfUuidArrayAdapter(this, mTangoSpaceAdfDataList);
        mTangoSpaceAdfListView.setAdapter(mTangoSpaceAdfListAdapter);
        mTangoSpaceAdfListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(AdfUuidListViewActivity.this, getIntent().getStringExtra(ACTIVITY), Toast.LENGTH_SHORT).show();
                if(getIntent().getStringExtra(ACTIVITY).equals(LocateActivity.class.toString())){
                    Intent intent = new Intent();
                    intent.putExtra(UUID,mTangoSpaceAdfDataList.get(position).uuid);
                    setResult(1,intent);
                    finish();
                }
            }
        });
        registerForContextMenu(mTangoSpaceAdfListView);

        // Get App Space ADF ListView ready.
        mAppSpaceAdfListView = (ListView) findViewById(R.id.uuid_list_view_application_space);
        mAppSpaceAdfFolder = getAppSpaceAdfFolder();
        mAppSpaceAdfDataList = new ArrayList<AdfData>();
        mAppSpaceAdfListAdapter = new AdfUuidArrayAdapter(this, mAppSpaceAdfDataList);
        mAppSpaceAdfListView.setAdapter(mAppSpaceAdfListAdapter);
        mAppSpaceAdfListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(getIntent().getStringExtra(ACTIVITY).equals(LocateActivity.class.toString())){
                    Intent intent = new Intent();
                    intent.putExtra(UUID,mAppSpaceAdfDataList.get(position).uuid);
                    setResult(UUID_OK,intent);
                    finish();
                }
            }
        });
        registerForContextMenu(mAppSpaceAdfListView);

        //To make sure that we have the permission
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Initialize Tango Service as a normal Android Service. Since we call
        // mTango.disconnect() in onPause, this will unbind Tango Service, so
        // every time onResume gets called we should create a new Tango object.
        mTango = new Tango(AdfUuidListViewActivity.this, new Runnable() {
            // Pass in a Runnable to be called from UI thread when Tango is ready;
            // this Runnable will be running on a new thread.
            // When Tango is ready, we can call Tango functions safely here only
            // when there are no UI thread changes involved.
            @Override
            public void run() {
                mIsTangoReady = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (AdfUuidListViewActivity.this) {
                            updateList();
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        synchronized (this) {
            // Unbinds Tango Service.
            mTango.disconnect();
        }
        mIsTangoReady = false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) { //when long click
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        if (v.getId() == R.id.uuid_list_view_tango_space) {
            menu.setHeaderTitle(mTangoSpaceAdfDataList.get(info.position).uuid);
            menu.add(mTangoSpaceMenuStrings[0]);
            menu.add(mTangoSpaceMenuStrings[1]);
            menu.add(mTangoSpaceMenuStrings[2]);
        }

        if (v.getId() == R.id.uuid_list_view_application_space) {
            menu.setHeaderTitle(mAppSpaceAdfDataList.get(info.position).uuid);
            menu.add(mAppSpaceMenuStrings[0]);
            menu.add(mAppSpaceMenuStrings[1]);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (!mIsTangoReady) {
            Toast.makeText(this, R.string.tango_not_ready, Toast.LENGTH_SHORT).show();
            return false;
        }
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        String itemName = (String) item.getTitle();
        int index = info.position;

        // Rename the ADF from API storage.
        if (itemName.equals(mTangoSpaceMenuStrings[0])) {
            // Delete the ADF from Tango space and update the Tango ADF Listview.
            showSetNameDialog(mTangoSpaceAdfDataList.get(index).uuid);
        } else if (itemName.equals(mTangoSpaceMenuStrings[1])) {
            // Delete the ADF from Tango space and update the Tango ADF Listview.
            deleteAdfFromTangoSpace(mTangoSpaceAdfDataList.get(index).uuid);
        } else if (itemName.equals(mTangoSpaceMenuStrings[2])) {
            // Export the ADF into application package folder and update the Listview.
            exportAdf(mTangoSpaceAdfDataList.get(index).uuid);
        } else if (itemName.equals(mAppSpaceMenuStrings[0])) {
            // Delete an ADF from App space and update the App space ADF Listview.
            deleteAdfFromAppSpace(mAppSpaceAdfDataList.get(index).uuid);
        } else if (itemName.equals(mAppSpaceMenuStrings[1])) {
            // Import an ADF from app space to Tango space.
            importAdf(mAppSpaceAdfDataList.get(index).uuid);
        }

        updateList();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to.
        if (requestCode == Tango.TANGO_INTENT_ACTIVITYCODE) {
            // Make sure the request was successful.
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, R.string.no_permissions, Toast.LENGTH_LONG).show();
            }
        }
        updateList();
    }

    /**
     * Implements SetAdfNameDialog.CallbackListener.
     */
    @Override
    public void onAdfNameOk(String name, String uuid) {
        if (!mIsTangoReady) {
            Toast.makeText(this, R.string.tango_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }
        TangoAreaDescriptionMetaData metadata;
        metadata = mTango.loadAreaDescriptionMetaData(uuid);
        byte[] adfNameBytes = metadata.get(TangoAreaDescriptionMetaData.KEY_NAME);
        if (adfNameBytes != name.getBytes()) {//set the same name
            metadata.set(TangoAreaDescriptionMetaData.KEY_NAME, name.getBytes());
        }
        mTango.saveAreaDescriptionMetadata(uuid, metadata);
        updateList();
    }

    /**
     * Implements SetAdfNameDialog.CallbackListener.
     */
    @Override
    public void onAdfNameCancelled() {
        // Nothing to do here.
    }

    /**
     * Import an ADF from app space to Tango space.
     */
    private void importAdf(String uuid) {
        try {
            mTango.importAreaDescriptionFile(mAppSpaceAdfFolder + File.separator + uuid);
        } catch (TangoErrorException e) {
            Toast.makeText(this, R.string.adf_exists_api_space, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Export an ADF from Tango space to app space.
     */
    private void exportAdf(String uuid) {
        try {
            mTango.exportAreaDescriptionFile(uuid, mAppSpaceAdfFolder);
        } catch (TangoErrorException e) {
            Toast.makeText(this, R.string.adf_exists_app_space, Toast.LENGTH_SHORT).show();
        }
    }

    public void deleteAdfFromTangoSpace(String uuid) {
        try {
            mTango.deleteAreaDescription(uuid);
        } catch (TangoErrorException e) {
            Toast.makeText(this, R.string.no_uuid_tango_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteAdfFromAppSpace(String uuid) {
        File file = new File(mAppSpaceAdfFolder + File.separator + uuid);
        file.delete();
    }

    /*
     * Returns maps storage location in the App package folder. Creates a folder called Maps, if it
     * does not exist.
     */
    private String getAppSpaceAdfFolder() {
        String mapsFolder = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + "Maps";
        File file = new File(mapsFolder);
        if (!file.exists()) {
            file.mkdirs();
        }
        return mapsFolder;
    }

    /**
     * Updates the list of AdfData corresponding to the App space.
     */
    private void updateAppSpaceAdfList() {
        File file = new File(mAppSpaceAdfFolder);
        File[] adfFileList = file.listFiles();
        mAppSpaceAdfDataList.clear();

        for (int i = 0; i < adfFileList.length; ++i) {
            mAppSpaceAdfDataList.add(new AdfData(adfFileList[i].getName(), ""));
        }

    }

    /**
     * Updates the list of AdfData corresponding to the Tango space.
     */
    private void updateTangoSpaceAdfList() {
        ArrayList<String> fullUuidList;
        TangoAreaDescriptionMetaData metadata = new TangoAreaDescriptionMetaData();

        try {
            // Get all ADF UUIDs.
            fullUuidList = mTango.listAreaDescriptions();
            // Get the names from the UUIDs.
            mTangoSpaceAdfDataList.clear();
            for (String uuid : fullUuidList) {
                String name;
                try {
                    metadata = mTango.loadAreaDescriptionMetaData(uuid);
                } catch (TangoErrorException e) {
                    Toast.makeText(this, R.string.tango_error, Toast.LENGTH_SHORT).show();
                }
                name = new String(metadata.get(TangoAreaDescriptionMetaData.KEY_NAME));
                mTangoSpaceAdfDataList.add(new AdfData(uuid, name));
            }
        } catch (TangoErrorException e) {
            Toast.makeText(this, R.string.tango_error, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Updates the list of AdfData from Tango and App space and sets it to the adapters.
     */
    private void updateList() {
        // Update App space ADF Listview.
        updateAppSpaceAdfList();
        mAppSpaceAdfListAdapter.setAdfData(mAppSpaceAdfDataList);
        mAppSpaceAdfListAdapter.notifyDataSetChanged();

        // Update Tango space ADF Listview.
        updateTangoSpaceAdfList();
        mTangoSpaceAdfListAdapter.setAdfData(mTangoSpaceAdfDataList);
        mTangoSpaceAdfListAdapter.notifyDataSetChanged();
    }

    private void showSetNameDialog(String mCurrentUuid) {
        if (!mIsTangoReady) {
            Toast.makeText(this, R.string.tango_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }
        Bundle bundle = new Bundle();
        TangoAreaDescriptionMetaData metaData = mTango.loadAreaDescriptionMetaData(mCurrentUuid);
        byte[] adfNameBytes = metaData.get(TangoAreaDescriptionMetaData.KEY_NAME);
        if (adfNameBytes != null) {
            String fillDialogName = new String(adfNameBytes);
            bundle.putString(TangoAreaDescriptionMetaData.KEY_NAME, fillDialogName);
        }
        bundle.putString(TangoAreaDescriptionMetaData.KEY_UUID, mCurrentUuid);
        FragmentManager manager = getFragmentManager();
        SetAdfNameDialog setAdfNameDialog = new SetAdfNameDialog();
        setAdfNameDialog.setArguments(bundle);
        setAdfNameDialog.show(manager, "ADFNameDialog");
    }
}
