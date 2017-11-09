package edu.something.ar_framework;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private TextureView myTextureView;

    private final TextureView.SurfaceTextureListener mySurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            // When SurfaceTextureView is available, pass width and height to setupCamera(), which
            // contacts the camera service and starts process of initializing camera
            setupCamera(width, height);

        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };


    private CameraDevice myCameraDevice;

    // Start StateCallback Listener on CameraDevice in order to know when CameraDevice is active.
    // Returns CameraDevice object corresponding to specified physical camera
    private final CameraDevice.StateCallback myStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            // Use returned cameraDevice corresponding to specified camera
            // by assigning to our created CameraDevice object
            myCameraDevice = camera;
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            // Free up CameraDevice resources when not connected to specified camera
            cameraDevice.close();
            myCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            // Free up CameraDevice resources when not connected to specified camera due to error
            cameraDevice.close();
            myCameraDevice = null;
        }
    };


    private final ImageReader.OnImageAvailableListener myImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {

        }
    };

    // Create background handler and thread for processing camera tasks behind UI
    private HandlerThread myBackgroundHandlerThread;
    private Handler myBackgroundHandler;

    // Initialize string to contain CameraID when returned from CameraManager
    private String myCameraID;

    // Initialize Size object for preview size within app
    private Size myPreviewSize;

    // Create array to translate device rotation into real-world rotation
    private static SparseIntArray Orientations = new SparseIntArray();
    static {
        Orientations.append(Surface.ROTATION_0, 0);
        Orientations.append(Surface.ROTATION_90, 90);
        Orientations.append(Surface.ROTATION_180, 180);
        Orientations.append(Surface.ROTATION_270, 270);
    }


    // Setup camera preview resolution based on camera sensor resolution
    private static class compareArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() / (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    private static Size choosePreviewSize(Size[] choices, int width, int height) {
        List<Size> bigEnoughForDisplay = new ArrayList<Size>();
        for (Size option : choices) {
            //Check if option is big enough for display
            if (option.getHeight() == option.getWidth() * height/width
                    && option.getWidth() >= width
                    && option.getHeight() >= height) {
                // if it is, add it to list of options
                bigEnoughForDisplay.add(option);
            }
        }

        // if at least one option exists, return minimum value
        if (bigEnoughForDisplay.size() > 0) {
            return Collections.min(bigEnoughForDisplay, new compareArea());
        }
        else {
            // if not options, just return a default value
            return choices[0];
        }
    }


    private void setupCamera(int width, int height) {
        /*
        Connect CameraManager object to device camera service in order to get information
        about camera hardware
        */
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        // Get a list of cameras contained in device
        try {
            // Traverse through all available cameraID's on device
            for (String cameraID : cameraManager.getCameraIdList()){
                // Get characteristics of each cameraID
                CameraCharacteristics cameraCharacteristics =
                        cameraManager.getCameraCharacteristics(cameraID);
                // Only want to use rear facing camera - if front camera is selected first, skip it
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                // get list of available resolutions from CameraCharacteristics
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                // Get device orientation
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                // Calculate total rotation
                int totalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);

                // Check if in landscape orientation
                boolean swapRotation = totalRotation == 90 || totalRotation == 270;

                int rotatedWidth = width;
                int rotatedHeight = height;

                if (swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }

                myPreviewSize = choosePreviewSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);

                // If cameraID is rear facing, then set current cameraID to our cameraId for use
                // and return out of method
                myCameraID = cameraID;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraManager.openCamera(myCameraID, myStateCallback, myBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myTextureView = (TextureView) findViewById(R.id.cameraPreview);

        // Example of a call to a native method
//        TextView tv = (TextView) findViewById(R.id.sample_text);
//        tv.setText(stringFromJNI());
    }

    // Define what happens when our application comes into the foreground
    @Override
    protected void onResume() {
        // call super constructor in order to apply onResume() to entire Activity
        super.onResume();

        // Start background thread
        startBackgroundThread();

        // See if our TextureView is available
        if (myTextureView.isAvailable()) {
            // if myTextureView is available for use, setupCamera() to initialize camera process
            setupCamera(myTextureView.getWidth(), myTextureView.getHeight());
        }
        // If not, start our SurfaceTextureListener to alert us when TextureView is available
        else {
            myTextureView.setSurfaceTextureListener(mySurfaceTextureListener);
        }
    }


    // Define what happens when app is put in the background
    @Override
    protected void onPause() {
        // Call super constructor to apply to entire activity
        super.onPause();
        // stop background thread
        stopBackgroundThread();
        // Close the camera being used
        closeCamera();
    }


    // Create method to close camera when no longer needed to free up resources
    private void closeCamera() {
        // if myCameraDevice is active, then close it
        if (myCameraDevice != null) {
            myCameraDevice.close();
            myCameraDevice = null;
        }
    }

    private void startBackgroundThread() {
        myBackgroundHandlerThread = new HandlerThread("Camera2API");
        myBackgroundHandlerThread.start();
        myBackgroundHandler = new Handler(myBackgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        myBackgroundHandlerThread.quitSafely();
        try {
            myBackgroundHandlerThread.join();
            myBackgroundHandlerThread = null;
            myBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = Orientations.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}