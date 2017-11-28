package edu.something.ar_framework;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;
import org.opencv.android.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.opencv.core.Core.VERSION;
import static org.opencv.core.Core.VERSION_MAJOR;

import edu.something.ar_framework.ASUForia;

public class MainActivity extends AppCompatActivity {

    /**
        Load the 'native-lib' library on application startup. This library contains nativePoseEstimation(),
        which is used to get the image features/points and compare them to the reference image
        features/points. It also returns the rotation and translation vectors used in onPose() to draw
        the cube in the camera image.
     */
    static {
        System.loadLibrary("native-lib");
    }


    //TODO: For checkpoint purposes, check that OpenCV Compiles, log a tag, and print message to UI window
    // Tag used to find OpenCV logging in debug window
    private static final String CVTAG ="OpenCVTag";

    // Call OpenCVLoader to determine if OpenCV is compiling, and log the result
    static {
        if (OpenCVLoader.initDebug()) {
            Log.d(CVTAG, "OpenCV successfully compiled!");
        }
        else {
            Log.d(CVTAG, "OpenCV did not compile correctly...");
        }
    }

    // Get OpenCV version using built-in getVersion(), referenced as "VERSION" in org.opencv.core.Core
    // This version will be outputted to the camera preview when camera onOpened() within CameraDevice.StateCallback()
    private static String version = VERSION;

    /**
    A TextureView is a class in Android that can be used to display an image stream. It does not create a new window,
     and therefore can be moved or transformed within a particular activity. In this case, we are going to use our
     TextureView to continuously stream the camera preview images to the user interface, where onPose() will draw a cube
     on the image preview. The TextureView is used through its SurfaceTexture, which is gathered using a
     SurfaceTextureListener interface. The SurfaceTextureListener has several different callback functions, based on the
     type of event detected by the listener. This listener and associated callbacks are defined below.
     */
    // Instantiate TextureView object
    private TextureView myTextureView;

    // Setup SurfaceTexture listener object, which is type TextureView.SurfaceTextureListener
    private final TextureView.SurfaceTextureListener mySurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        // Callback that defines what happens when the SurfaceTexture is available for use
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            // When SurfaceTextureView is available, pass width and height to setupCamera(), which
            // contacts the camera service and starts process of initializing camera
            setupCamera(width, height);
            connectCamera();
        }

        // Callback that defines what happens when the SurfaceTexture size is changed. In this case, rotation.
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

        }

        // Callback that defines what happens when the SurfaceTexture is closed
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        // Callback that defines what happens when the SurfaceTexture is altered in anyway. Not happening here.
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };


    /**
    A CameraDevice is a class in Android that represents a single hardware camera connected to an Android device. In
     order to use a CameraDevice, the user must have allowed access through the camera permissions. We are using the
     newest Android camera API, called the camera2 API. There are several "states" that the camera can be in, and each
     state has a specific method associated with it. In this case, we are going to use the camera to provide the
     continuous vision needed to draw a cube in the camera preview. Therefore, we need to define what happens when the
     camera is opened through the public class onOpened(). We also don't want to continue to use the camera when the app
     is no longer in use, so we use the public class onDisconnected() to define what happens when the app is no longer
     using the CameraDevice and disconnects from it. We also define what happens if the CameraDevice encounters a
     serious error.
     */
    private CameraDevice myCameraDevice;

    // Start StateCallback Listener on CameraDevice in order to know when CameraDevice is active.
    // Returns CameraDevice object corresponding to specified physical camera
    private final CameraDevice.StateCallback myStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            // Use returned cameraDevice corresponding to specified camera
            // by assigning to our created CameraDevice object
            myCameraDevice = camera;
            startPreview();
//            Toast.makeText(getApplicationContext(), "Camera connection success!", Toast.LENGTH_SHORT).show();
            Toast.makeText(getApplicationContext(), version, Toast.LENGTH_LONG).show();
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

    // Create CaptureRequest.Builder object to initialize camera preview using startPreview()
    private CaptureRequest.Builder myCaptureRequestBuilder;

    // Listener for orientation changes in order to update the camera preview
    private OrientationEventListener myOrientationEventListener;

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
            // if no options, just return a default value
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


    private void startPreview() {
        // Get a  SurfaceTexture object that camera understands from our TextureView to start preview process
        SurfaceTexture surfaceTexture = myTextureView.getSurfaceTexture();
        // Set size based on our preview size method
        surfaceTexture.setDefaultBufferSize(myPreviewSize.getWidth(), myPreviewSize.getHeight());
        // Create surface object to display data
        Surface previewSurface = new Surface(surfaceTexture);

        // setup capture request builder
        try {
            myCaptureRequestBuilder = myCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            myCaptureRequestBuilder.addTarget(previewSurface);

            myCameraDevice.createCaptureSession(Arrays.asList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            try {
                                // second argument is what to do with data being previewed via callback
                                // for preview purposes only, set to null
                                cameraCaptureSession.setRepeatingRequest(myCaptureRequestBuilder.build(),
                                        null, myBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(getApplicationContext(),
                                    "Unable to setup camera preview", Toast.LENGTH_SHORT).show();
                        }
                    }, null);
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

        // Complete list of actions in text file




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
            connectCamera();
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

        // endEstimation() function call
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