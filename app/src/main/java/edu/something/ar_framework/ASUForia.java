package edu.something.ar_framework;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.opencv.*;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.ORB;


public class ASUForia {


    /**
     Load the 'native-lib' library on application startup. This library contains nativePoseEstimation(),
     which is used to get the image features/points and compare them to the reference image
     features/points. It also returns the rotation and translation vectors used in onPose() to draw
     the cube in the camera image.
     */
    static {
        System.loadLibrary("native-lib");
    }

    private PoseListener mylistener;
    private Activity myAct;
    /*************************************** ASUForia Constructor ***************************************/
    ASUForia(PoseListener listener_arg, Bitmap referenceImage, Surface cameraSurface, Activity act) {
        mylistener = listener_arg;
        myAct = act;

        // Call nativeFeatureDetection() to get features for reference image. Returned reference points need to be saved
        nativeFeatureDetection(referenceImage);


    }


    /**
     * Several objects will be needed in multiple methods used to gain access and control of the physical camera. For
     * the sake of readability in these methods, the single-line object instantiations are collected here, with a
     * single line comment meant to give a general idea of how the object will be used.
     */

    // Background Handler and HandlerThread for processing camera tasks behind UI, to keep UI seamless
    private HandlerThread myBackgroundHandlerThread;
    private Handler myBackgroundHandler;

    // Initialize string to contain CameraID when returned from CameraManager
    private String myCameraID;

    // Initialize Size object for preview size. Preview size object contains dimensions for our Surface
    private Size myPreviewSize;

    // Create CaptureRequest.Builder object to initialize camera preview using startPreview()
    private CaptureRequest.Builder myCaptureRequestBuilder;

    // Listener for orientation changes in order to update the camera preview size as device rotates
    private OrientationEventListener myOrientationEventListener;

    // Instantiate TextureView object
    private TextureView myTextureView;



    /*************************************** Begin startEstimation() ************************************/
    //TODO: Create startEstimation() that will setup and open the camera
    public void startEstimation(TextureView myTextureView) {
        this.myTextureView = myTextureView;
        // Start background thread
        startBackgroundThread();

        // Assign our TextureView the correct cameraPreview
        //myTextureView = (TextureView) findViewById(R.id.cameraPreview);

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



        // Nothing else to do in this function - linked to onImageAvailable() by setupCamera() and connectCamera()

    }
    /*************************************** End startEstimation() **************************************/



    /*************************************** Begin onImageAvailable() ***********************************/
    //TODO: Create onImageAvailable() which will be used to pass the image to nativePoseEstimation()

    /**
     * The ImageReader class is a class that allows applications to directly access image data that is rendered to a
     * surface, in this case our SurfaceTexture defined in our TextureView. As with all listeners, the nested public
     * class ImageReader.OnImageAvailableListener acts as the interface between the Android HAL and our application.
     * The listener is used to define the callback function that occurs when a new image is available from the camera2
     * device we're using. In this case, the callback fires every time a new frame is available from the device camera.
     * For example, if the camera is capturing frames at 30 FPS, this callback will fire 30 times/sec. For our purposes,
     * we want to take the image from the camera, and estimate the camera pose based on the features OpenCV detects in
     * the image. Therefore, when a new frame (image) is available, we want to pass it to nativePoseEstimation() where
     * OpenCV will be used to determine the pose and compare this frames points against the reference points. It will
     * then return a rotation and translation vector, that we will then pass to the PoseListener defined in MainActivity.
     * The PoseListener callback method onPose() will then use these vectors to draw a cube on the camera image.
     */
    // Instantiate ImageReader.OnImageAvailableListener object
    private final ImageReader.OnImageAvailableListener myImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        // Define what happens when a new (preview) frame is available from the camera
        @Override
        public void onImageAvailable(ImageReader imageReader) {

            //TODO: Call nativePoseEstimation() to get rotation and translation (R and T) vectors

            //TODO: Call PoseListeners callback function, onPose(), passing the R and T vectors

        }
    };


    /*************************************** End onImageAvailable() *************************************/



    /*************************************** Begin endEstimation() **************************************/
    //TODO: Create endEstimation() that will close the camera
    public void endEstimation() {

        // stop background thread
        stopBackgroundThread();
        // Close the camera being used
        closeCamera();

    }
    /*************************************** End endEstimation() ****************************************/








    /******************************* Start of Camera2 API Methods *******************************************/

    /**
     * A TextureView is a class in Android that can be used to display an image stream. It does not create a new window,
     * and therefore can be moved or transformed within a particular activity. In this case, we are going to use our
     * TextureView to continuously stream the camera preview images to the user interface, where onPose() will draw a cube
     * on the image preview. The TextureView is used through its SurfaceTexture, which is gathered using a
     * SurfaceTextureListener interface. The SurfaceTextureListener has several different callback functions, based on the
     * type of event detected by the listener. This listener and associated callbacks are defined below.
     */

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
     * A CameraDevice is a class in Android that represents a single hardware camera connected to an Android device. In
     * order to use a CameraDevice, the user must have allowed access through the camera permissions. We are using the
     * newest Android camera API, called the camera2 API. There are several "states" that the camera can be in, and each
     * state has a specific method associated with it. In this case, we are going to use the camera to provide the
     * continuous vision needed to draw a cube in the camera preview. Therefore, we need to define what happens when the
     * camera is opened through the public class onOpened(). We also don't want to continue to use the camera when the app
     * is no longer in use, so we use the public class onDisconnected() to define what happens when the app is no longer
     * using the CameraDevice and disconnects from it. We also define what happens if the CameraDevice encounters a
     * serious error.
     */
    // Instantiate CameraDevice object
    private CameraDevice myCameraDevice;

    // Start StateCallback Listener on CameraDevice in order to know when CameraDevice is active.
    // Returns CameraDevice object corresponding to specific physical camera
    private final CameraDevice.StateCallback myStateCallback = new CameraDevice.StateCallback() {

        // Define what happens when the camera is opened
        @Override
        public void onOpened(@NonNull CameraDevice camera) {

            // When the camera device is opened, assign it to our CameraDevice object so we can use it!
            myCameraDevice = camera;

            // Once our camera is selected, start the camera preview so we can see what the camera sees
            startPreview();

            // For initial troubleshooting purposes. Display success message when camera is opened
//            Toast.makeText(getApplicationContext(), "Camera connection success!", Toast.LENGTH_SHORT).show();

            // When the camera opens, we also want to prove that OpenCV has compiled correctly, so we display the
            // OpenCV VERSION string gathered above and use Toast to display it on the screen temporarily.
//            Toast.makeText(getApplicationContext(), version, Toast.LENGTH_LONG).show();
        }

        // Define what happens when the camera is closed (disconnected)
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            // Free up CameraDevice resources and close our CameraDevice
            cameraDevice.close();
            // Reset our CameraDevice object so that it can be assigned a new camera in the future
            myCameraDevice = null;
        }

        // Define what happens when the camera has a serious error
        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            // Perform same actions as when disconnected
            cameraDevice.close();
            myCameraDevice = null;
        }
    };


    // Create array to translate device rotation into real-world rotation
    private static SparseIntArray Orientations = new SparseIntArray();

    static {
        Orientations.append(Surface.ROTATION_0, 0);
        Orientations.append(Surface.ROTATION_90, 90);
        Orientations.append(Surface.ROTATION_180, 180);
        Orientations.append(Surface.ROTATION_270, 270);
    }


    private void setupCamera(int width, int height) {
        /*
        Connect CameraManager object to device camera service in order to get information
        about camera hardware
        */
        CameraManager cameraManager = (CameraManager) myAct.getSystemService(Context.CAMERA_SERVICE);
        // Get a list of cameras contained in device
        try {
            // Traverse through all available cameraID's on device
            for (String cameraID : cameraManager.getCameraIdList()) {
                // Get characteristics of each cameraID
                CameraCharacteristics cameraCharacteristics =
                        cameraManager.getCameraCharacteristics(cameraID);
                // Only want to use rear facing camera - if front camera is selected first, skip it
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                // get list of available resolutions from CameraCharacteristics
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                // Get device orientation
                int deviceOrientation = myAct.getWindowManager().getDefaultDisplay().getRotation();
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
        CameraManager cameraManager = (CameraManager) myAct.getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraManager.openCamera(myCameraID, myStateCallback, myBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void openCamera() {
    }


    /**
     * A simple method closeCamera() will be created to simplify the process needed to close the camera, since the camera
     * will be closed in multiple methods
     */
    private void closeCamera() {
        // if myCameraDevice is active, then close it
        if (myCameraDevice != null) {
            myCameraDevice.close();
            // set the camera device to null so that it can be assigned a new CameraDevice in the future
            myCameraDevice = null;
        }
    }


    /******************************* End of Camera2 API Methods *******************************************/


    /******************************* Start of Camera Preview Methods **************************************/


    /**
     * We want to make sure that the preview window (Surface) we're displaying our images on matches the camera
     * resolution. This is something we could probably just take for granted in this case, but also seems like
     * a good thing to be sure of, and provides more coding practice... We're going to compare the area of the
     * camera resolution and the current preview area to determine if they are compatible. Then we will find a
     * list of options, and choose the best match.
     */
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
            if (option.getHeight() == option.getWidth() * height / width
                    && option.getWidth() >= width
                    && option.getHeight() >= height) {
                // if it is, add it to list of options
                bigEnoughForDisplay.add(option);
            }
        }

        // if at least one option exists, return minimum value
        if (bigEnoughForDisplay.size() > 0) {
            return Collections.min(bigEnoughForDisplay, new compareArea());
        } else {
            // if no options, just return a default value
            return choices[0];
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
                            Toast.makeText(myAct.getApplicationContext(),
                                    "Unable to setup camera preview", Toast.LENGTH_SHORT).show();
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }


    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = Orientations.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }


    /******************************* End of Camera Preview Methods *****************************************/


    /******************************* Start of BackgroundHandlerThread Setup *******************************************/


    /**
     * A background thread is needed to perform tasks on the camera without interrupting the user interface. In this case,
     * this is needed to not interrupt the camera preview and OpenCV-based cube drawing happening continuously in the
     * user interface. We need a method to start and stop the background thread, so that when the app goes into the
     * background, the background thread can be stopped to free up resources for other applications.
     */
    // Method to start the background thread, using the BackgroundHandlerThread object
    private void startBackgroundThread() {
        // Set our BackgroundHandlerthread object to a new HandlerThread, give it a name, of type string.
        // HandlerThread is used because it is a thread that has a Looper
        myBackgroundHandlerThread = new HandlerThread("Camera2API");
        // Start the BackgroundHandlerThread
        myBackgroundHandlerThread.start();
        // Handler is needed to send and process objects associated with a thread.
        // Associate our BackgroundHandler with the Looper of our BackgroundHandlerThread
        myBackgroundHandler = new Handler(myBackgroundHandlerThread.getLooper());
    }

    // Method to stop the background thread
    private void stopBackgroundThread() {
        /* Terminate our thread's looper as soon as all remaining messages in the que that are already due to be
        delivered have been processed, as opposed to just terminating the looper immediately. */
        myBackgroundHandlerThread.quitSafely();
        // Try-catch statement added per Android Studio's request.
        try {
            // Set the thread and handler to null so they can be reassigned in the future
            myBackgroundHandlerThread.join();
            myBackgroundHandlerThread = null;
            myBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /******************************* End of BackgroundHandlerThread Setup *********************************************/







    /*************************************** PoseListener Definition ************************************/
    interface PoseListener {
        public void onPose(Image cameraFrame, float[] rvec, float[] tvec);
    }



    /*************************************** nativePoseEstimation Definition ****************************/
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application. CALL THESE FUNCTIONS ABOVE
     */
    // Native method for pose estimation in OpenCV
    public native String nativePoseEstimation();

    // Native method for getting ORB features in OpenCV
    public native String nativeFeatureDetection(Bitmap referenceImage);


}
