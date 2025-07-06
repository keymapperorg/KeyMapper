#include <jni.h>

extern "C" JNIEXPORT jstring JNICALL
Java_io_github_sds100_keymapper_privstarter_NativeLib_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    char* hello = "Hello from C++";
    return env->NewStringUTF(hello);
}