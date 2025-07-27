#include <jni.h>
#include <asm-generic/fcntl.h>
#include <fcntl.h>
#include <android/log.h>
#include "libevdev/libevdev.h"
#include "libevdev/libevdev-uinput.h"

#define LOG_TAG "KeyMapperSystemBridge"

#include "logging.h"
#include "android/input/KeyLayoutMap.h"
#include "android/libbase/result.h"
#include "android/input/InputDevice.h"
#include "aidl/io/github/sds100/keymapper/sysbridge/IEvdevCallback.h"
#include <android/binder_ibinder_jni.h>

using aidl::io::github::sds100::keymapper::sysbridge::IEvdevCallback;

extern "C"
JNIEXPORT jboolean JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_grabEvdevDevice(JNIEnv *env,
                                                                               jobject thiz,
                                                                               jobject deviceId,
                                                                               jobject jcallbackBinder) {
    LOGE("PRE BINDER");
    AIBinder *callbackAIBinder = AIBinder_fromJavaBinder(env, jcallbackBinder);

    // Create a "strong pointer" to the callback binder.
    const ::ndk::SpAIBinder spBinder(callbackAIBinder);
    std::shared_ptr<IEvdevCallback> callback = IEvdevCallback::fromBinder(spBinder);
    LOGE("POST BINDER");

    char *input_file_path = "/dev/input/event12";
    // TODO call libevdev_free when done with the object.
    struct libevdev *dev = nullptr;
    int fd;
    int rc = 1;

    fd = open(input_file_path, O_RDONLY);

    if (fd == -1) {
        LOGE("Failed to open input file (%s)",
             input_file_path);
    }
    rc = libevdev_new_from_fd(fd, &dev);
    if (rc < 0) {
        LOGE("Failed to init libevdev");
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

    android::InputDeviceIdentifier deviceIdentifier = android::InputDeviceIdentifier();
    deviceIdentifier.bus = libevdev_get_id_bustype(dev);
    deviceIdentifier.vendor = libevdev_get_id_vendor(dev);
    deviceIdentifier.product = libevdev_get_id_product(dev);
    deviceIdentifier.version = libevdev_get_id_version(dev);
    deviceIdentifier.name = libevdev_get_name(dev);

    std::string keyLayoutMapPath = android::getInputDeviceConfigurationFilePathByDeviceIdentifier(
            deviceIdentifier, android::InputDeviceConfigurationFileType::KEY_LAYOUT);

    LOGE("Key layout path = %s", keyLayoutMapPath.c_str());

    auto keyLayoutResult = android::KeyLayoutMap::load(keyLayoutMapPath, nullptr);

    if (keyLayoutResult.ok()) {
        LOGE("KEY LAYOUT RESULT OKAY");
    } else {
        LOGE("KEY LAYOUT RESULT FAILED");
    }

    // Create a virtual device that is a duplicate of the existing one.
//    struct libevdev_uinput *virtual_dev_uninput = nullptr;
//    int uinput_fd = open("/dev/uinput", O_RDWR);
//    libevdev_uinput_create_from_device(dev, uinput_fd, &virtual_dev_uninput);
//    const char *virtual_dev_path = libevdev_uinput_get_devnode(virtual_dev_uninput);
//    LOGE("Virtual keyboard device: %s", virtual_dev_path);

    do {
        struct input_event ev;
        rc = libevdev_next_event(dev, LIBEVDEV_READ_FLAG_NORMAL, &ev);
        if (rc == 0)
            __android_log_print(ANDROID_LOG_ERROR, "Key Mapper",
                                "Event: %s %s %d, Event code: %d, Time: %ld.%ld\n",
                                libevdev_event_type_get_name(ev.type),
                                libevdev_event_code_get_name(ev.type, ev.code),
                                ev.value,
                                ev.code,
                                ev.time.tv_sec,
                                ev.time.tv_usec);

        int32_t outKeycode = -1;
        uint32_t outFlags = -1;
        keyLayoutResult.value()->mapKey(ev.code, 0, &outKeycode, &outFlags);

        LOGE("Key code = %d Flags = %d", outKeycode, outFlags);

        callback->onEvdevEvent(ev.type, ev.code, ev.value);
//        libevdev_uinput_write_event(virtual_dev_uninput, ev.type, ev.code, ev.value);

    } while (rc == 1 || rc == 0 || rc == -EAGAIN);

    return true;


}