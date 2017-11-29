#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_edu_something_ar_1framework_ASUForia_nativePoseEstimation(
        JNIEnv *env,
        jobject /* this */) {

    //TODO: Implement nativePoseEstimation with OpenCV methods

    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
