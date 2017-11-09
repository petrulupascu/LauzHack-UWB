package ch.hefr.etu.zoutao_wen.tangoapplication;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.provider.Settings.Secure;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoException;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.projecttango.tangosupport.TangoPointCloudManager;
import com.projecttango.tangosupport.TangoSupport;

import org.rajawali3d.math.Matrix4;
import org.rajawali3d.scene.ASceneFrameCallback;
import org.rajawali3d.view.SurfaceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.hefr.etu.zoutao_wen.tangoapplication.SetUdpIpDFragment;

import static ch.hefr.etu.zoutao_wen.tangoapplication.AdfUuidListViewActivity.UUID;
import static ch.hefr.etu.zoutao_wen.tangoapplication.AdfUuidListViewActivity.UUID_OK;
import static ch.hefr.etu.zoutao_wen.tangoapplication.AdfUuidListViewActivity.UUID_REQUEST;
import static ch.hefr.etu.zoutao_wen.tangoapplication.MainActivity.ACTIVITY;
import static com.google.atap.tangoservice.TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION;

/**
 *  This class allow us to localize an environment using an ADF file chosen by the user.
 *  After the charge of the file, the device began to locate itself in the environment. If it finds
 *  itself in the previous environment, the state will turn to "Localised" and the guess position
 *  will be printed. The three distances will also be shown. The distance 1 means distance between
 *  the position and the origin where this activity began. The distance 2 means distance between
 *  the position and the origin where the ADF file began. The distance 3 means distance between the
 *  two origins. Three distances become a triangle.
 *  The data of Tango will be sent to server computer
 */

public class LocateActivity extends Activity implements
        SetAdfNameDialog.CallbackListener,
        SaveAdfTask.SaveAdfListener,
        SetUdpIpDFragment.CallbackListener,
        View.OnTouchListener{

    private static final String TAG = LocateActivity.class.getSimpleName();
    public static final String TANGO_TAG = "ch.hefr.etu.zoutao_wen.tangoapplication.gettango";

    private static final int SECS_TO_MILLISECS = 1000;
    private Tango mTango;
    private TangoConfig mConfig;

    private static final double UPDATE_INTERVAL_MS = 100.0;

    private double mPreviousPoseTimeStamp;
    private double mTimeToNextUpdate = UPDATE_INTERVAL_MS;

    // Long-running task to save the ADF.
    private SaveAdfTask mSaveAdfTask;

    private final Object mSharedLock = new Object();

    private String mPosistion="\nX(m): n\\a\nY(m): n\\a\nZ(m): n\\a";
    private double mDistance_1=0.0D;
    private double mDistance_2=0.0D;
    private double mDistance_3=0.0D;
    private boolean mStatus=false;
    private float translation_real[];
    private float translation_guess[];
    private float rotation_guess[];
    private float translation_between[];

    //UI Elements
    private TextView mStatusText;
    private TextView mPositionText;
    private TextView mDistanceText;
    private ToggleButton mLocateToggleButton;
    private Button mChargeAdfButton;
    private Button mSaveNewAdfButton;
    private Button mShowInARButton;
    private Button mUdpIpButton; // EG

    private ProgressDialog mDialogForReady;

    private String mAdfUuid;

    //For AR SurfaceView
    private SurfaceView mSurfaceView;
    private ARRenderer mRenderer;
    private boolean mIsConnected = false;

    private int mDisplayRotation;
    private int mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
    private static final int INVALID_TEXTURE_ID = 0;

    private double mCameraPoseTimestamp = 0;
    private AtomicBoolean mIsFrameAvailableTangoThread = new AtomicBoolean(false);
    private double mRgbTimestampGlThread;

    private TangoPointCloudManager mPointCloudManager;

    private TangoPoseData mOriginPose_1 = new TangoPoseData();
    private TangoPoseData mOriginPose_2 = new TangoPoseData();

    private boolean ismFrameShow = false;

    //Data Transmission via Wifi
    private String udp_ip;
    private int udp_port = 7585;//For Tango Data port : 7585
    private String mAndroid_id;
    private float mPosX;
    private float mPosY;
    private float mPosZ;
    private float mThetaX;
    private float mThetaY;
    private float mThetaZ;

    private UdpClientSend udpClient;
    private SntpClient sntpClient;
    private boolean isSntpTimeSet = false;

    private TangoData mTangoData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locate);

        mPointCloudManager = new TangoPointCloudManager();

        //Set up UI elements
        mPositionText = (TextView) findViewById(R.id.positionText_locate);
        mStatusText = (TextView) findViewById(R.id.statusText_locate);
        mDistanceText = (TextView) findViewById(R.id.distanceText_locate);
        mChargeAdfButton = (Button) findViewById(R.id.charge_adf);
        mLocateToggleButton = (ToggleButton) findViewById(R.id.locate_toggle);
        mSaveNewAdfButton = (Button) findViewById(R.id.save_new_adf);
        mShowInARButton = (Button) findViewById(R.id.show_in_ar);
        mSurfaceView = (SurfaceView) findViewById(R.id.ar_surface);
        mUdpIpButton = (Button) findViewById(R.id.udp_ip_button); //EG

        mAndroid_id = Secure.getString(getApplicationContext().getContentResolver(), Secure.ANDROID_ID);

        mSurfaceView.setOnTouchListener(this);

        mRenderer = new ARRenderer(this);
        mSurfaceView.setSurfaceRenderer(mRenderer);

        translation_real = new float[3];
        translation_guess = new float[3];
        translation_between = new float[3];
        mAdfUuid = null;

        if(mAdfUuid == null){
            mLocateToggleButton.setEnabled(false);
            mSaveNewAdfButton.setEnabled(false);
            mShowInARButton.setEnabled(false);
        }

        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (displayManager != null) {
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {
                }

                @Override
                public void onDisplayChanged(int displayId) {
                    synchronized (this) {
                        setDisplayRotation();
                    }
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                }
            }, null);
        }

        //udp_ip = getResources().getString(R.string.udp_ip);
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        String defaultValue = getResources().getString(R.string.udp_ip_saved);
        udp_ip = sharedPref.getString(getString(R.string.udp_ip_saved), defaultValue);
        Log.d(TAG, "Get saved IP: " + udp_ip);

        mUdpIpButton.setText(udp_ip);
    }

    /**
     * Initialize Tango Service as a normal Android Service.
     */
    private void bindTangoService() {
        // Initialize Tango Service as a normal Android Service. Since we call mTango.disconnect()
        // in onPause, this will unbind Tango Service, so every time onResume gets called we
        // should create a new Tango object.

        mTango = new Tango(LocateActivity.this, new Runnable() {
            // Pass in a Runnable to be called from UI thread when Tango is ready; this Runnable
            // will be running on a new thread.
            // When Tango is ready, we can call Tango functions safely here only when there are no
            // UI thread changes involved.
            @Override
            public void run() {
                synchronized (LocateActivity.this) {
                    try {
                        TangoSupport.initialize();
                        mConfig = setTangoConfig(mTango);//set up for mTango
                        mTango.connect(mConfig);
                        udpClient = new UdpClientSend(udp_ip, udp_port);
                        sntpClient = new SntpClient();

                        //setSntpTime if not done yet
                        if(!isSntpTimeSet) {

                            new Thread(new Runnable() {
                                public void run() {
                                    if (sntpClient.requestTime("time2.ethz.ch", 60000)) {
                                        isSntpTimeSet = true;
                                    } else {
                                        Log.i(TAG, "sntpTimeNotSetYet");
                                    }
                                }
                            }).start();
                        }
                    } catch (TangoOutOfDateException e) {
                        Log.e(TAG, getString(R.string.tango_out_of_date_exception), e);
                        showsToastAndFinishOnUiThread(R.string.tango_out_of_date_exception);
                    } catch (TangoErrorException e) {
                        Log.e(TAG, getString(R.string.tango_error), e);
                        showsToastAndFinishOnUiThread(R.string.tango_error);
                    } catch (TangoInvalidException e) {
                        Log.e(TAG, getString(R.string.tango_invalid), e);
                        showsToastAndFinishOnUiThread(R.string.tango_invalid);
                    } catch (SecurityException e) {
                        // Area Learning permissions are required. If they are not available,
                        // SecurityException is thrown.
                        Log.e(TAG, getString(R.string.no_permissions), e);
                        showsToastAndFinishOnUiThread(R.string.no_permissions);
                    }finally {
                        mDialogForReady.dismiss();
                    }
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        synchronized (this) {
            try {
                if(mRenderer != null)
                    mRenderer.getCurrentScene().clearFrameCallbacks();
                if(mTango != null){
                    mTango.disconnectCamera(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                    mTango.disconnect();
                }
                if(udpClient != null) {
                    Toast.makeText(this,"Socket Closed",Toast.LENGTH_SHORT).show();
                    udpClient.close();
                }
                mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
                mIsConnected = false;
            } catch (TangoErrorException e) {
                Log.e(TAG, getString(R.string.tango_error), e);
            }
        }
    }

    /**
     * Sets up the Tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
    private TangoConfig setTangoConfig(Tango tango) {
        // Use default configuration for Tango Service.
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        //for the motion tracking
        config.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_AUTORECOVERY, true);
        // Set learning mode to config.
        //config.putBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE, true);
        //For AR use
        config.putBoolean(TangoConfig.KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
        config.putInt(TangoConfig.KEY_INT_DEPTH_MODE, TangoConfig.TANGO_DEPTH_MODE_POINT_CLOUD);
        // Drift correction allows motion tracking to recover after it loses tracking.
        // The drift-corrected pose is available through the frame pair with
        // base frame AREA_DESCRIPTION and target frame DEVICE.
 //       config.putBoolean(TangoConfig.KEY_BOOLEAN_DRIFT_CORRECTION, true);//avoid this because this is similar to learning mode
        //ADF file chooses by the user
        if(mAdfUuid !=null)
            config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION, mAdfUuid);
        else
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(LocateActivity.this, "No ADF file available!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        //load the latest ADF file
  /*      ArrayList<String> fullUuidList;
        // Returns a list of ADFs with their UUIDs.
        fullUuidList = tango.listAreaDescriptions();
        // Load the latest ADF if ADFs are found.
        if (fullUuidList.size() > 0) {
            config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION,
                    fullUuidList.get(fullUuidList.size() - 1));//get the latest ADF
        }else{
            //if not
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(LocateActivity.this, "No ADF file available!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }*/

        return config;
    }

    /**
     * Set up the callback listeners for the Tango Service and obtain other parameters required
     * after Tango connection.
     * Listen to new Pose data.
     */
    private void startupTango() {
        // Lock configuration and connect to Tango.
        // Select coordinate frame pair.
        final ArrayList<TangoCoordinateFramePair> framePairs =
                new ArrayList<TangoCoordinateFramePair>();
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));
        framePairs.add(new TangoCoordinateFramePair(//the data from ADF refer to the device
                COORDINATE_FRAME_AREA_DESCRIPTION,
                TangoPoseData.COORDINATE_FRAME_DEVICE));
        framePairs.add(new TangoCoordinateFramePair(//compare the data from ADF with the data from camera
                COORDINATE_FRAME_AREA_DESCRIPTION,
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE));

        // Listen for new Tango data.
        mTango.connectListener(framePairs, new Tango.TangoUpdateCallback() {
            @Override
            public void onPoseAvailable(final TangoPoseData pose) {
                // Make sure to have atomic access to Tango data so that UI loop doesn't interfere
                // while Pose call back is updating the data.
                synchronized (mSharedLock) {
                    logPose(pose);
                    if (pose.baseFrame == COORDINATE_FRAME_AREA_DESCRIPTION
                            && pose.targetFrame == TangoPoseData
                            .COORDINATE_FRAME_START_OF_SERVICE) {//the third pair which compare the existed ADF with the camera(data now)
                        if (pose.statusCode == TangoPoseData.POSE_VALID) {//the same trace for the existed ADF and the data just received
                            mStatus = true;
                        } else {
                            mStatus = false;
                        }
                    }
                }

                final double deltaTime = (pose.timestamp - mPreviousPoseTimeStamp) *
                        SECS_TO_MILLISECS;
                mPreviousPoseTimeStamp = pose.timestamp;
                mTimeToNextUpdate -= deltaTime;

                if (mTimeToNextUpdate < 0.0) {
                    mTimeToNextUpdate = UPDATE_INTERVAL_MS;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (mSharedLock) {
                                mSaveNewAdfButton.setEnabled(mStatus);
                                //TODO:
                                //mShowInARButton.setEnabled(mStatus);
                                mStatusText.setText("Status: "+(mStatus?"Localized":"Not Localized"));
                                mPositionText.setText("Your position that I guess:\n" + mPosistion);
                                if(mStatus) {
                                    mDistanceText.setText("Distance 1(m): " + String.format("%.3f", mDistance_1) +
                                            "\nDistance 2(m): " + String.format("%.3f", mDistance_2) +
                                            "\nDistance 3(m): " + String.format("%.3f", mDistance_3));
                                    mTangoData = new TangoData(sntpClient.now(), mAndroid_id, mAdfUuid, mPosX,
                                            mPosY, mPosZ, mThetaX, mThetaY, mThetaZ);
                                    try {
                                        udpClient.send(mTangoData.getMsg());
                                    }catch (IOException e){
                                        e.printStackTrace();
                                    }
                                }else
                                    mDistanceText.setText("Distance 1(m): "+ String.format("%.3f", mDistance_1) +
                                            "\nDistance 2(m): "+ "n\\a" +
                                            "\nDistance 3(m): "+ "n\\a");
                            }
                        }
                    });
                }
            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData xyzIj) {
                // We are not using onXyzIjAvailable for this app.
            }

            @Override
            public void onPointCloudAvailable(TangoPointCloudData pointCloud) {
                // Save the cloud and point data for later use.
                mPointCloudManager.updatePointCloud(pointCloud);
            }

            @Override
            public void onTangoEvent(final TangoEvent event) {
                // Ignoring TangoEvents.
            }

            @Override
            public void onFrameAvailable(int cameraId) {
                if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR) {
                    // Mark a camera frame as available for rendering in the OpenGL thread.
                    mIsFrameAvailableTangoThread.set(true);
                    mSurfaceView.requestRender();
                }
            }
        });
    }

    /**
     * Upload the position and the distances
     * @param pose
     */
    private void logPose(TangoPoseData pose){
        final StringBuilder stringBuilder = new StringBuilder();

        //the distance from the original place in direction of XYZ
        float translation[] = pose.getTranslationAsFloats();
        float rotation[] =pose.getRotationAsFloats();
        //0:X 1:Y 2:Z follow the left hand Cartesian coordinate
        stringBuilder.append("X(m): " + translation[0] + "\n" +
                "Y(m): " + translation[1] + "\n" +
                "Z(m): " + translation[2] + "\n");

        if(pose.baseFrame==TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE &&
                pose.targetFrame==TangoPoseData.COORDINATE_FRAME_DEVICE){
            //the distance from the origin of locate
            translation_real = translation;//for further use
            mDistance_1 = Math.sqrt(Math.pow(translation_real[0],2) +
                    Math.pow(translation_real[1],2) + Math.pow(translation_real[2],2));
            if(Math.abs(translation_real[0]) <= 1 && Math.abs(translation_real[1]) <= 1){
                //mOriginPose_1 = pose;

            }
        }
        else if(pose.baseFrame== COORDINATE_FRAME_AREA_DESCRIPTION &&
                pose.targetFrame==TangoPoseData.COORDINATE_FRAME_DEVICE && mStatus){
            //the position guessed
            //the distance from the origin in the adf file
            mPosistion = stringBuilder.toString();
            translation_guess = translation;
            rotation_guess = rotation;
            mPosX = translation_guess[0];
            mPosY = translation_guess[1];
            mPosZ = translation_guess[2];
            calculateQuatToEuler(rotation_guess[3],rotation_guess[0],rotation_guess[1],
                    rotation_guess[2]);
            mDistance_2 = Math.sqrt(Math.pow(translation_guess[0],2) +
                    Math.pow(translation_guess[1],2) + Math.pow(translation_guess[2],2));

            if(Math.abs(translation_guess[0]) <= 1 && Math.abs(translation_guess[1]) <= 1){
                //mOriginPose_2 = pose;

            }

        }else if(pose.baseFrame== COORDINATE_FRAME_AREA_DESCRIPTION &&
                pose.targetFrame==TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE && mStatus){
            //the distance between two origins
            translation_between=translation;
            mDistance_3 = Math.sqrt(Math.pow(translation_between[0],2) +
                    Math.pow(translation_between[1],2) + Math.pow(translation_between[2],2));
        }

    }

    /**
     * Implements SetAdfNameDialog.CallbackListener.
     */
    @Override
    public void onAdfNameOk(String name, String uuid) {
        mSaveAdfTask = new SaveAdfTask(this, this, mTango, name);
        mSaveAdfTask.execute();
    }

    /**
     * Implements SetAdfNameDialog.CallbackListener.
     */
    @Override
    public void onAdfNameCancelled() {  finish();  }


    /**
     * Handles failed save from mSaveAdfTask.
     */
    @Override
    public void onSaveAdfFailed(String adfName) {
        String toastMessage = String.format(
                getResources().getString(R.string.save_adf_failed_toast_format),
                adfName);
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
        mSaveAdfTask = null;
    }

    /**
     * Handles successful save from mSaveAdfTask.
     */
    @Override
    public void onSaveAdfSuccess(String adfName, String adfUuid) {
        String toastMessage = String.format(
                getResources().getString(R.string.save_adf_success_toast_format),
                adfName, adfUuid);
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
        mSaveAdfTask = null;
        finish();
    }

    /**
     * charge the ADF file
     * @param v
     */
    public void chargeAdfClicked(View v){
        mDialogForReady = ProgressDialog.show(this,"Getting ready","Please wait...",true);
        Intent startAdfListViewIntent = new Intent(this, AdfUuidListViewActivity.class);
        startAdfListViewIntent.putExtra(ACTIVITY , LocateActivity.class.toString());
        startActivityForResult(startAdfListViewIntent,UUID_REQUEST);
    }

    /**
     * Localize begins
     * @param v
     */
    public void locateToggleClicked(View v){
        if(mLocateToggleButton.isChecked()){
            mShowInARButton.setEnabled(true);
            startupTango();
            connectRenderer();
            mIsConnected = true;
            setDisplayRotation();
        }

        else
            saveNewAdfClicked(v);
    }

    //EG
    public void udpIpClicked(View v){
        FragmentManager manager = getFragmentManager();
        SetUdpIpDFragment setUdpIpDFragment = new SetUdpIpDFragment();
        setUdpIpDFragment.show(manager, "UdpIpDialog");
    }

    //EG
    // A new IP have been chosen. We have to set up a new udp connection
    public void onSetUdpIpOk(String new_udp_ip) {
        if (!udp_ip.equals(new_udp_ip)){
            udp_ip = new_udp_ip;

            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(getString(R.string.udp_ip_saved), udp_ip);
            editor.commit();

            mUdpIpButton.setText(udp_ip);

            if (udpClient != null) {
                udpClient.close();
                udpClient = new UdpClientSend(udp_ip, udp_port);
                Log.d(TAG, "New udp client created with ip " + udp_ip);
            }
        }
    }

    //EG
    public void onSetUdpIpCancelled() {
        //finish();
    }

    /**
     * If the learning mode is on, you can save a new ADF file after localization
     * @param v
     */
    public void saveNewAdfClicked(View v){
        Bundle bundle = new Bundle();
        bundle.putString(TangoAreaDescriptionMetaData.KEY_NAME, "New ADF");
        // UUID is generated after the ADF is saved.
        bundle.putString(TangoAreaDescriptionMetaData.KEY_UUID, "");

        FragmentManager manager = getFragmentManager();
        SetAdfNameDialog setAdfNameDialog = new SetAdfNameDialog();
        setAdfNameDialog.setArguments(bundle);
        setAdfNameDialog.show(manager, "ADFNameDialog");
    }

    public void showInARClicked(View v){
        if(!ismFrameShow){
            mSurfaceView.setVisibility(SurfaceView.VISIBLE);
            mPositionText.setVisibility(TextView.INVISIBLE);
            mDistanceText.setVisibility(TextView.INVISIBLE);
            mChargeAdfButton.setVisibility(Button.INVISIBLE);
            ismFrameShow = true;
        }else{
            mSurfaceView.setVisibility(SurfaceView.INVISIBLE);
            mPositionText.setVisibility(TextView.VISIBLE);
            mDistanceText.setVisibility(TextView.VISIBLE);
            mChargeAdfButton.setVisibility(Button.VISIBLE);
            ismFrameShow = false;
        }
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // The result of the permission activity.
        // Check which request we're responding to.
        if (requestCode == UUID_REQUEST) {
            // Make sure the request was successful.
            if (resultCode == UUID_OK) {
                mAdfUuid = data.getStringExtra(UUID);
                mLocateToggleButton.setEnabled(true);
                bindTangoService();
            }else{
                Toast.makeText(this,"Error in receiving the uuid",Toast.LENGTH_SHORT).show();
                mDialogForReady.dismiss();
                finish();
            }
        }
    }

    private void setDisplayRotation() {
        Display display = getWindowManager().getDefaultDisplay();
        mDisplayRotation = display.getRotation();

        // We also need to update the camera texture UV coordinates. This must be run in the OpenGL
        // thread.
        mSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mIsConnected) {
                    mRenderer.updateColorCameraTextureUvGlThread(mDisplayRotation);
                }
            }
        });
    }

    /**
     * Connects the view and renderer to the color camara and callbacks.
     */
    private void connectRenderer() {
        // Register a Rajawali Scene Frame Callback to update the scene camera pose whenever a new
        // RGB frame is rendered.
        // (@see https://github.com/Rajawali/Rajawali/wiki/Scene-Frame-Callbacks)
        mRenderer.getCurrentScene().registerFrameCallback(new ASceneFrameCallback() {
            @Override
            public void onPreFrame(long sceneTime, double deltaTime) {
                // NOTE: This is called from the OpenGL render thread, after all the renderer
                // onRender callbacks have a chance to run and before scene objects are rendered
                // into the scene.

                try {
                    synchronized (LocateActivity.this) {
                        // Don't execute any tango API actions if we're not connected to the
                        // service.
                        if (!mIsConnected) {
                            return;
                        }

                        // Set up scene camera projection to match RGB camera intrinsics
                        if (!mRenderer.isSceneCameraConfigured()) {
                            TangoCameraIntrinsics intrinsics =
                                    TangoSupport.getCameraIntrinsicsBasedOnDisplayRotation(
                                            TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                                            mDisplayRotation);
                            mRenderer.setProjectionMatrix(
                                    projectionMatrixFromCameraIntrinsics(intrinsics));
                        }

                        // Connect the camera texture to the OpenGL Texture if necessary
                        // NOTE: When the OpenGL context is recycled, Rajawali may regenerate the
                        // texture with a different ID.
                        if (mConnectedTextureIdGlThread != mRenderer.getTextureId()) {
                            mTango.connectTextureId(TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                                    mRenderer.getTextureId());
                            mConnectedTextureIdGlThread = mRenderer.getTextureId();
                            Log.d(TAG, "connected to texture id: " + mRenderer.getTextureId());
                        }

                        // If there is a new RGB camera frame available, update the texture with
                        // it.
                        if (mIsFrameAvailableTangoThread.compareAndSet(true, false)) {
                            mRgbTimestampGlThread =
                                    mTango.updateTexture(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                        }

                        if (mRgbTimestampGlThread > mCameraPoseTimestamp) {
                            // Calculate the camera color pose at the camera frame update time in
                            // OpenGL engine.
                            //
                            // When drift correction mode is enabled in config file, we need
                            // to query the device with respect to Area Description pose in
                            // order to use the drift-corrected pose.
                            //
                            // Note that if you don't want to use the drift-corrected pose, the
                            // normal device with respect to start of service pose is still
                            // available.
                            TangoPoseData lastFramePose = TangoSupport.getPoseAtTime(
                                    mRgbTimestampGlThread,
                                    TangoPoseData.//COORDINATE_FRAME_START_OF_SERVICE,//use start of service the origin should be in the locate start point
                                                 COORDINATE_FRAME_AREA_DESCRIPTION,//use the coordinate of area description, it should stop at the origin of the ADF file
                                    TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                                    TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                                    TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                                    mDisplayRotation);
                            if (lastFramePose.statusCode == TangoPoseData.POSE_VALID) {
                                // Update the camera pose from the renderer.
                                mRenderer.updateRenderCameraPose(lastFramePose);
                                mCameraPoseTimestamp = lastFramePose.timestamp;
                            } else {
                                // When the pose status is not valid, it indicates the tracking has
                                // been lost. In this case, we simply stop rendering.
                                //
                                // This is also the place to display UI to suggest that the user
                                // walk to recover tracking.
                                Log.w(TAG, "Can't get device pose at time: " +
                                        mRgbTimestampGlThread);
                            }
                        }
                        //TODO:
                        //float[] origin;
                        //origin = setOrigin(0.5f, 0.5f, mRgbTimestampGlThread);
                    }
                    // Avoid crashing the application due to unhandled exceptions.
                } catch (TangoErrorException e) {
                    Log.e(TAG, "Tango API call error within the OpenGL render thread", e);
                } catch (Throwable t) {
                    Log.e(TAG, "Exception on the OpenGL thread", t);
                }
            }

            @Override
            public void onPreDraw(long sceneTime, double deltaTime) {

            }

            @Override
            public void onPostFrame(long sceneTime, double deltaTime) {

            }

            @Override
            public boolean callPreFrame() {
                return true;
            }
        });
    }

    /**
     * Click for transmit the data
     * @param view
     * @param motionEvent
     * @return
     */
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                try{
                    synchronized (this) {
                        mOriginPose_1.translation[0]=translation_between[0];
                        mOriginPose_1.translation[1]=translation_between[2];//the change of coordinate Z
                        mOriginPose_1.translation[2]=-translation_between[1];//Y
                        mOriginPose_1.rotation[0]=0;
                        mOriginPose_1.rotation[1]=0;
                        mOriginPose_1.rotation[2]=0;

                        mOriginPose_2.translation[0]=0;
                        mOriginPose_2.translation[1]=0;
                        mOriginPose_2.translation[2]=0;
                        mOriginPose_2.rotation[0]=0;
                        mOriginPose_2.rotation[1]=0;
                        mOriginPose_2.rotation[2]=0;
                        Matrix4 origin_1 = ScenePoseCalculator.tangoPoseToMatrix(mOriginPose_1);
                        Matrix4 origin_2 = ScenePoseCalculator.tangoPoseToMatrix(mOriginPose_2);
                        if(origin_1 != null){
                            // Update the position of the rendered origin to the pose of the detected place.
                            // This update is made thread-safe by the renderer.
                            mRenderer.updateObjectPose_1(origin_1);
                        }
                        if(origin_2 != null){
                            // Update the position of the rendered origin to the pose of the detected place.
                            // This update is made thread-safe by the renderer.
                            mRenderer.updateObjectPose_2(origin_2);
                        }
                    }
                }catch (TangoException t) {
                    Toast.makeText(getApplicationContext(),
                            "1",
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "1", t);
                } catch (SecurityException t) {
                    Toast.makeText(getApplicationContext(),
                            "2",
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "2", t);
                }
            }
            return true;
        }

    /**
     * Use Tango camera intrinsics to calculate the projection Matrix for the Rajawali scene.
     */
    private static float[] projectionMatrixFromCameraIntrinsics(TangoCameraIntrinsics intrinsics) {
        // Uses frustumM to create a projection matrix taking into account calibrated camera
        // intrinsic parameter.
        // Reference: http://ksimek.github.io/2013/06/03/calibrated_cameras_in_opengl/
        float near = 0.1f;
        float far = 100;

        double cx = intrinsics.cx;
        double cy = intrinsics.cy;
        double width = intrinsics.width;
        double height = intrinsics.height;
        double fx = intrinsics.fx;
        double fy = intrinsics.fy;

        double xscale = near / fx;
        double yscale = near / fy;

        double xoffset = (cx - (width / 2.0)) * xscale;
        // Color camera's coordinates has y pointing downwards so we negate this term.
        double yoffset = -(cy - (height / 2.0)) * yscale;

        float m[] = new float[16];
        Matrix.frustumM(m, 0,
                (float) (xscale * -width / 2.0 - xoffset),
                (float) (xscale * width / 2.0 - xoffset),
                (float) (yscale * -height / 2.0 - yoffset),
                (float) (yscale * height / 2.0 - yoffset), near, far);
        return m;
    }

    /*
    private float[] setOrigin(float u, float v, double rgbTimestamp){
        TangoPointCloudData pointCloud = mPointCloudManager.getLatestPointCloud();

        if (pointCloud == null) {
            return null;
        }

        // Get pose transforms for depth/color cameras from world frame in
        // OpenGL engine space.
        TangoPoseData openglToDepthPose = TangoSupport.getPoseAtTime(//TODO:no use
                pointCloud.timestamp,
                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                TangoSupport.TANGO_SUPPORT_ENGINE_TANGO,
                TangoSupport.ROTATION_IGNORED);
        if (openglToDepthPose.statusCode != TangoPoseData.POSE_VALID) {
            Log.d(TAG, "Could not get a valid pose from area description "
                    + "to depth camera at time " + pointCloud.timestamp);
            //return null;
        }

        TangoPoseData openglToColorPose = TangoSupport.getPoseAtTime(
                rgbTimestamp,
                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                TangoSupport.TANGO_SUPPORT_ENGINE_TANGO,
                TangoSupport.ROTATION_IGNORED);
        if (openglToColorPose.statusCode != TangoPoseData.POSE_VALID) {
            Log.d(TAG, "Could not get a valid pose from area description "
                    + "to color camera at time " + rgbTimestamp);
            return null;
        }

        // Plane model is in OpenGL space due to input poses.

        IntersectionPointPlaneModelPair intersectionPointPlaneModelPair =
                TangoSupport.fitPlaneModelNearPoint(pointCloud,
                        mOriginPose_1.translation,
                        mOriginPose_1.rotation, u, v,
                        mDisplayRotation,
                        openglToColorPose.translation,
                        openglToColorPose.rotation);
        // Convert plane model into 4x4 matrix. The first 3 elements of
        // the plane model make up the normal vector.
        float[] openglUp = new float[]{0, 1, 0, 0};
        float[] openglToPlaneMatrix = matrixFromPointNormalUp(
                intersectionPointPlaneModelPair.intersectionPoint,
                intersectionPointPlaneModelPair.planeModel,
                openglUp);
        return openglToPlaneMatrix;
    }

    private float[] matrixFromPointNormalUp(double[] point, double[] normal, float[] up) {
        float[] zAxis = new float[]{(float) normal[0], (float) normal[1], (float) normal[2]};
        normalize(zAxis);
        float[] xAxis = crossProduct(up, zAxis);
        normalize(xAxis);
        float[] yAxis = crossProduct(zAxis, xAxis);
        normalize(yAxis);
        float[] m = new float[16];
        Matrix.setIdentityM(m, 0);
        m[0] = xAxis[0];
        m[1] = xAxis[1];
        m[2] = xAxis[2];
        m[4] = yAxis[0];
        m[5] = yAxis[1];
        m[6] = yAxis[2];
        m[8] = zAxis[0];
        m[9] = zAxis[1];
        m[10] = zAxis[2];
        m[12] = (float) point[0];
        m[13] = (float) point[1];
        m[14] = (float) point[2];
        return m;
    }*/

    /**
     * Normalize a vector.
     */
    private void normalize(float[] v) {
        double norm = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        v[0] /= norm;
        v[1] /= norm;
        v[2] /= norm;
    }

    /**
     * Cross product between two vectors following the right-hand rule.
     */
    private float[] crossProduct(float[] v1, float[] v2) {
        float[] result = new float[3];
        result[0] = v1[1] * v2[2] - v2[1] * v1[2];
        result[1] = v1[2] * v2[0] - v2[2] * v1[0];
        result[2] = v1[0] * v2[1] - v2[0] * v1[1];
        return result;
    }

    /**
     * Transform the quaternion to Euler
     */
    private void calculateQuatToEuler(float w, float x, float y, float z){
        mThetaX = (float)Math.atan2(2*x*w-2*y*z,1-2*x*x-2*z*z);
        mThetaY = (float)Math.atan2(2*y*w-2*x*z,1-2*y*y-2*z*z);
        mThetaZ = (float)Math.asin((2*x*y+2*z*w));
    }

    /**
     * Display toast on UI thread.
     *
     * @param resId The resource id of the string resource to use. Can be formatted text.
     */
    private void showsToastAndFinishOnUiThread(final int resId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LocateActivity.this,
                        getString(resId), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }
}
