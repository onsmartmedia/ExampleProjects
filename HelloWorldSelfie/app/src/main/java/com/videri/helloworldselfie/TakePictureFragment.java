package com.videri.helloworldselfie;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.hardware.usb.UsbManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * 6/15/16
 * Ayal Fieldust
 * Videri - Camera Demo
 * HelloWorldSelfie
 *
 * Description: This Demo is a base app for anyone trying to build a camera app for the Videri screens.
 * Things to keep in mind:
 * As of 6/17/16 the current level of API is 18, so only Camera V1 is usable at this point.
 * The Videri-ops scheduler will call onPause, onStop, and onResume, when switching between  apps so notice that the camera preview works properly.
 *
 */
public class TakePictureFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private static final String TAG = "Fragment One TAG-------";
    private View view;
    private ImageView  captureBtn;
    private boolean isCountDown = false;
    private Handler mHandler;
    private int takingPictureCountDown = 4;
    private static Camera mCamera = null;
    private static CameraPreview mPreview = null;
    public String pathOfPicture = "";
    public File pictureFile = null;
    private MediaPlayer mPlayer;
    private RelativeLayout cameraPreviewRelative;
    private TextView countDownView = null;
    private TextView mCameraInfo = null;
    private static int cameraId = 0;
    BroadcastReceiver mUsbReceiver = null;
    private static boolean DEBUGGING = true;
    private ImageView backgroundImage = null;
    public TakePictureFragment() {
        // Required empty public constructor
    }

    private ImageView countDownImageView = null;
    private Camera.PictureCallback mPicture;
    private boolean usbConnected = false;
    private static boolean mCameraInfoShowing = false;
    private static final int TAKE_PHOTO_CODE = 100;
    private boolean cameraFront = false;




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_take_picture, container, false);
        Log.v(TAG, "inflated fragment 1. now listening to button 1.");

        //listen for new usb devices
        mUsbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if((UsbManager.ACTION_USB_DEVICE_DETACHED).equalsIgnoreCase(action)) {
                    if (DEBUGGING) { Log.d(TAG, "vCameraMainActivity::onCreate()...camera REMOVED"); }
                    Toast toast = Toast.makeText(getActivity(), "Camera removed!", Toast.LENGTH_LONG);
                    toast.show();
                    usbConnected = false;
                    mPreview.stopCamera();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        getActivity().registerReceiver(mUsbReceiver, filter);
        saveLogcatToFile(getActivity());
        initialize();
        return view;
    }

    private Runnable TimerTask = new Runnable() {
        @Override
        public void run() {
            takingPictureCountDown--;
            countDownView.setText(String.valueOf(takingPictureCountDown));
            Log.v(TAG, (String.valueOf(takingPictureCountDown)));
            if(takingPictureCountDown == 1){
                mPreview.getCamera().takePicture(null, null, pictureCallback);

                isCountDown = false;
                takingPictureCountDown = 4;
            }
            else
                mHandler.postDelayed(this,1000);
        }
    };


    @Override
    public void onPause() {
        super.onPause();

        Log.v(TAG, "onPause..................");
//        mPreview.stopPreview();
        if(TimerTask != null) {
            mHandler.removeCallbacks(TimerTask);
        }
        takingPictureCountDown = 4;
//        mPlayer.release();

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "onResume..................");

        try {
            countDownView.setText("Ready?");
            captureBtn.setEnabled(true);
        }
        catch (Exception e){
            Log.v(TAG,"OnResume error: "+ e.toString());
        }
        if(isCountDown)
            mHandler.post(TimerTask);
//        if(isPreviewDestroyed)
        mPreview.startPreview();
    }



    @Override
    public void onStop() {
        super.onStop();
        Log.v(TAG, "onStop..................");
        takingPictureCountDown = 4;
        isCountDown = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy..................");

    }








    Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            if (data != null) {
                byte[] copy = new byte[data.length];
                System.arraycopy(data, 0, copy, 0, data.length);
                //make a new pictureCallback file
                FileOutputStream fos = null;
                pictureFile = getOutputJpegFile();
//                    if (pictureFile == null) {
//                        Toast noFileToast = Toast.makeText(getActivity(),"no file saved in pictureFile", Toast.LENGTH_LONG);
//
//                    }
                Log.v(TAG, "pictureFile2: " + pictureFile);

                try {
                    //write the file
                    mPlayer.start();
                    Log.v(TAG,"SAVING FILE NOW:");
                    fos = new FileOutputStream(pictureFile);
                    fos.write(copy);
                    fos.flush();
                    fos.close();
//                    Toast toast = Toast.makeText(getActivity(), "JPG saved: " + pictureFile.getName(), Toast.LENGTH_LONG);
//                    toast.show();
                    pathOfPicture = pictureFile.getPath();
                    ((MainActivity)getActivity()).respond(pathOfPicture);
                    ((MainActivity)getActivity()).changeFragment(2);
                    Log.v(TAG, "Picture Taken. now going to preview page to show the pictureCallback.....");
                }  catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            //refresh camera to continue preview
            mPreview.refreshCamera(mCamera);
        }

    };

    private static File getOutputJpegFile() {
        //make a new file directory inside the "sdcard" folder
        File mediaStorageDir = new File("/sdcard/com.videri.vcamera/captured", "jpg");

        //if this "JCGCamera folder does not exist
        if (!mediaStorageDir.exists()) {
            //if you cannot make this folder return
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        //take the current timeStamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        //and make a media file:
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }


    public void saveLogcatToFile(Context context) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "logcat_"+timeStamp+".txt";
        File outputFile = new File(context.getExternalCacheDir(),fileName);
        //eg. /storage/emulated/0/Android/data/com.videri.vcamera/cache/logcat_20160429_115948.txt
        try {
            Process process = Runtime.getRuntime().exec("logcat -f " + outputFile.getAbsolutePath());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void initialize() {
        if (DEBUGGING) { Log.d(TAG, "vCameraMainActivity::initialize()..."); }

        captureBtn = (ImageView) view.findViewById(R.id.button_capture);
        Util.loadImage(getActivity(),captureBtn,R.drawable.capture);

        captureBtn.setOnClickListener(captureListener);

        backgroundImage = (ImageView) view.findViewById(R.id.bg_image);
        Util.loadImage(getActivity(),backgroundImage,R.drawable.selfie_before);
        countDownView = (TextView)view.findViewById(R.id.TextViewCountDown);

        mHandler = new Handler();

        mPlayer = MediaPlayer.create(getActivity(), R.raw.automatic_camera);

        cameraPreviewRelative = (RelativeLayout) view.findViewById(R.id.preview);
        mPreview = new CameraPreview(getActivity());
        cameraPreviewRelative.addView(mPreview);
        mPreview.setCamera(0);
    }

    boolean isCameraStarted = true;
    View.OnClickListener captureListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.v(TAG,"BUTTON CLICKED........" );

//            if(isCameraStarted){
//                mPreview.stopPreview();
//                isCameraStarted = false;
//            }
//            else{
//                mPreview.startPreview();
//                isCameraStarted = true;
//            }

            if (DEBUGGING) { Log.v(TAG, "vCameraMainActivity::captureListener.onClick().. BUTTON CLICKED........"); }
               captureBtn.setEnabled(false);
//            mCamera.takePicture(null, null, pictureCallback);
            Log.v(TAG, "Timer count down clicked. 3-2-1-capture");
            if(!isCountDown ) {
                mHandler.post(TimerTask);

                isCountDown = true;
            }

        }
    };


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }





    //TO DELETE:

//        resumeCameraActivity();
//        cameraId = findFrontFacingCamera();
//        if (DEBUGGING) { Log.d(TAG, "vCameraMainActivity::resumeCameraActivity()..."); }
//        try {
//            mCamera = Camera.open(cameraId);
//        } catch (RuntimeException rte) {
//            Toast toast = Toast.makeText(getActivity(), "Your device does not have a camera!", Toast.LENGTH_LONG);
//            toast.show();
//        }
//        if (mCamera != null) {
//            mPreview = new CameraPreview(getActivity(), cameraId, mCamera);
//            if (mPreview != null) {
//                mCamera = mPreview.getCamera();
//

//
//                cameraPreviewRelative = (RelativeLayout) view.findViewById(R.id.preview);
//                //I took out:
//                //          RelativeLayout.LayoutParams previewLayoutParamsRelative = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
//                //          cameraPreviewRelative.setLayoutParams(previewLayoutParamsRelative);
//                cameraPreviewRelative.addView(mPreview);
//
//
//            }
//        }



    private void showCameraInfo() {
        if (DEBUGGING) { Log.v(TAG, "vCameraMainActivity::showCameraInfo()"); }
        if (mCamera != null) {
            StringBuilder strBuilder = new StringBuilder();
            strBuilder.append("Focus mode: " + mCamera.getParameters().getFocusMode() + "\n");
            strBuilder.append("Focal length: " + mCamera.getParameters().getFocalLength() + "\n");
            //we only support JPEG for now
            strBuilder.append("Picture format: JPEG" + "\n");//==256
            strBuilder.append("Jpeg quality: " + mCamera.getParameters().getJpegQuality() + "(highest)\n");
            strBuilder.append("Picture size: " + mCamera.getParameters().getPictureSize().width + "x" + mCamera.getParameters().getPictureSize().height);
            mCameraInfo.setText(strBuilder);
        }

        if (mCameraInfo != null) {
            if (mCameraInfo.getVisibility() != View.VISIBLE) {
                mCameraInfo.setVisibility(View.VISIBLE);
                mCameraInfo.bringToFront();
                mCameraInfoShowing = true;
            }
        }
    }


    private void hideCameraInfo() {
        if (DEBUGGING) { Log.v(TAG, "vCameraMainActivity::hideCameraInfo()"); }
        if (mCameraInfo != null) {
            if (mCameraInfo.getVisibility() != View.GONE) {
                mCameraInfo.setVisibility(View.GONE);
                mCameraInfoShowing = false;
            }
        }
    }
    //in case the media server died
//    private void resumeCameraActivity() {
//        if (DEBUGGING) { Log.d(TAG, "vCameraMainActivity::resumeCameraActivity()..."); }
//        if(mCamera == null) {
//            if (DEBUGGING) { Log.d(TAG, "Camera is null initializing."); }
//            cameraId = findFrontFacingCamera();
//            try {
//                mCamera = Camera.open(cameraId);
//                mPreview.setCamera(mCamera, cameraId);
//            } catch (RuntimeException ex) {
////            Toast.makeText(getActivity(),
////                    "Your device does not have a camera! " + ex.toString(), Toast.LENGTH_LONG).show();
//                Log.v(TAG, "Camera init error: " + ex.toString());
//            }
//        }
//        else
//            if (DEBUGGING) { Log.d(TAG, "Camera is not null initializing."); }
//        mCamera.startPreview();
//    }

    public int findFrontFacingCamera() {
        if (DEBUGGING) { Log.d(TAG, "vCameraMainActivity::findFrontFacingCamera()..."); }
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                cameraFront = true;
                break;
            }
        }
        return cameraId;
    }


    public int findBackFacingCamera() {
        if (DEBUGGING) { Log.d(TAG, "vCameraMainActivity::findBackFacingCamera()..."); }
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                cameraFront = false;
                break;
            }
        }
        return cameraId;
    }
}
