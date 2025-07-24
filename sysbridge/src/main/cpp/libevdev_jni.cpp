#include <jni.h>
#include <asm-generic/fcntl.h>
#include <fcntl.h>
#include <android/log.h>
#include "libevdev/libevdev.h"

#define LOG_TAG "KeyMapperSystemBridge"

#include "logging.h"
#include "android/input/KeyLayoutMap.h"
#include "android/libbase/result.h"

extern "C"
JNIEXPORT jstring JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_stringFromJNI(JNIEnv *env, jobject) {
    auto result = android::KeyLayoutMap::load("/system/usr/keylayout/Generic.kl", nullptr);

    if (result.ok()) {
        LOGE("RESULT OKAY");
    } else {
        LOGE("RESULT FAILED");
    }

    char *input_file_path = "/dev/input/event12";
    struct libevdev *dev = NULL;
    int fd;
    int rc = 1;

    fd = open(input_file_path, O_RDONLY);

    if (fd == -1) {
        LOGE("Failed to open input file (%s)",
             input_file_path);
        return env->NewStringUTF("Failed");
    }

    rc = libevdev_new_from_fd(fd, &dev);
    if (rc < 0) {
        LOGE("Failed to init libevdev");
        return env->NewStringUTF("Failed to init");
    }

    __android_log_print(ANDROID_LOG_ERROR, "Key Mapper", "Input device name: \"%s\"\n",
                        libevdev_get_name(dev));
    __android_log_print(ANDROID_LOG_ERROR, "Key Mapper",
                        "Input device ID: bus %#x vendor %#x product %#x\n",
                        libevdev_get_id_bustype(dev),
                        libevdev_get_id_vendor(dev),
                        libevdev_get_id_product(dev));

//    if (!libevdev_has_event_type(dev, EV_REL) ||
//        !libevdev_has_event_code(dev, EV_KEY, BTN_LEFT)) {
//        printf("This device does not look like a mouse\n");
//        exit(1);
//    }
    libevdev_grab(dev, LIBEVDEV_GRAB);

    do {
        struct input_event ev;
        rc = libevdev_next_event(dev, LIBEVDEV_READ_FLAG_NORMAL, &ev);
        if (rc == 0)
            __android_log_print(ANDROID_LOG_ERROR, "Key Mapper",
                                "Event: %s %s %d, Event code: %d\n",
                                libevdev_event_type_get_name(ev.type),
                                libevdev_event_code_get_name(ev.type, ev.code),
                                ev.value,
                                ev.code);
    } while (rc == 1 || rc == 0 || rc == -EAGAIN);

    return env->NewStringUTF("Hello!");
}