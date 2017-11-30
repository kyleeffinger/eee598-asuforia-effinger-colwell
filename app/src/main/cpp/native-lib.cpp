#include <jni.h>
#include <string>

extern "C"

JNIEXPORT jstring JNICALL

// Native method for pose estimation with OpenCV
Java_edu_something_ar_1framework_ASUForia_nativePoseEstimation(
        JNIEnv *env,
        jobject /* this */) {

    //TODO: Implement nativePoseEstimation with OpenCV methods

    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

// Native method for feature detection with OpenCVextern "C"
JNIEXPORT jstring JNICALL
Java_edu_something_ar_1framework_ASUForia_nativeFeatureDetection(JNIEnv *env, jobject) {

    // TODO: Implement nativeFeatureDetection to extract ORB features from reference image



    std::string testing = "Hello from C++";
    return env->NewStringUTF(testing.c_str());
}