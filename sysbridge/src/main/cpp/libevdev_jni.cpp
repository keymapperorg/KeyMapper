#include <jni.h>
#include <asm-generic/fcntl.h>
#include <fcntl.h>
#include <dirent.h>
#include <sys/stat.h>
#include <android/log.h>
#include "libevdev/libevdev.h"
#include "libevdev/libevdev-uinput.h"
#include "logging.h"
#include "android/input/KeyLayoutMap.h"
#include "android/libbase/result.h"
#include "android/input/InputDevice.h"
#include "aidl/io/github/sds100/keymapper/sysbridge/IEvdevCallback.h"
#include <android/binder_ibinder_jni.h>

using aidl::io::github::sds100::keymapper::sysbridge::IEvdevCallback;

static int findInputDevice(
        char *name,
        int bus,
        int vendor,
        int product,
        libevdev **outDev
) {
    DIR *dir = opendir("/dev/input");

    if (dir == nullptr) {
        LOGE("Failed to open /dev/input directory");
        return -1;
    }

    struct dirent *entry;

    while ((entry = readdir(dir)) != nullptr) {
        // Skip . and .. entries
        if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0) {
            continue;
        }

        char fullPath[256];
        snprintf(fullPath, sizeof(fullPath), "/dev/input/%s", entry->d_name);

        // Check if it's a character device (input device)
        struct stat st{};

        LOGD("Found input device: %s", fullPath);

        // Try to open the device to see if it's accessible
        int fd = open(fullPath, O_RDONLY);

        if (fd == -1) {
            continue;
        }

        struct libevdev *dev = nullptr;
        int status = libevdev_new_from_fd(fd, &dev);

        if (status != 0) {
            LOGE("Failed to open libevdev device from path %s: %s", fullPath, strerror(errno));
            close(fd);
            continue;
        }

        const char *devName = libevdev_get_name(dev);
        int devVendor = libevdev_get_id_vendor(dev);
        int devProduct = libevdev_get_id_product(dev);
        int devBus = libevdev_get_id_bustype(dev);

//        LOGD("Checking device: %s, bus: %d, vendor: %d, product: %d",
//             devName, devBus, devVendor, devProduct);

        if (strcmp(devName, name) != 0 ||
            devVendor != vendor ||
            devProduct != product ||
            devBus != bus) {

            libevdev_free(dev);
            close(fd);
            continue;
        }

        closedir(dir);
        *outDev = dev;

//        LOGD("Found input device %s", name);
        return 0;
    }

    closedir(dir);

    LOGE("Input device not found with name: %s, bus: %d, vendor: %d, product: %d", name, bus,
         vendor, product);

    return -1;
}

android::InputDeviceIdentifier
convertJInputDeviceIdentifier(JNIEnv *env, jobject jInputDeviceIdentifier) {
    android::InputDeviceIdentifier deviceIdentifier;

    jclass inputDeviceIdentifierClass = env->GetObjectClass(jInputDeviceIdentifier);

    jfieldID busFieldId = env->GetFieldID(inputDeviceIdentifierClass, "bus", "I");
    deviceIdentifier.bus = env->GetIntField(jInputDeviceIdentifier, busFieldId);

    jfieldID vendorFieldId = env->GetFieldID(inputDeviceIdentifierClass, "vendor", "I");
    deviceIdentifier.vendor = env->GetIntField(jInputDeviceIdentifier, vendorFieldId);

    jfieldID productFieldId = env->GetFieldID(inputDeviceIdentifierClass, "product", "I");
    deviceIdentifier.product = env->GetIntField(jInputDeviceIdentifier, productFieldId);

    jfieldID nameFieldId = env->GetFieldID(inputDeviceIdentifierClass, "name",
                                           "Ljava/lang/String;");
    auto nameString = (jstring) env->GetObjectField(jInputDeviceIdentifier, nameFieldId);

    const char *nameChars = env->GetStringUTFChars(nameString, nullptr);
    deviceIdentifier.name = std::string(nameChars);
    env->ReleaseStringUTFChars(nameString, nameChars);

    return deviceIdentifier;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_grabEvdevDevice(JNIEnv *env,
                                                                               jobject thiz,
                                                                               jobject jInputDeviceIdentifier,
                                                                               jobject jCallbackBinder) {
    LOGD("Start gravEvdevDevice");
    jclass inputDeviceIdentifierClass = env->GetObjectClass(jInputDeviceIdentifier);
    jfieldID idFieldId = env->GetFieldID(inputDeviceIdentifierClass, "id", "I");
    int deviceId = env->GetIntField(jInputDeviceIdentifier, idFieldId);

    android::InputDeviceIdentifier deviceIdentifier = convertJInputDeviceIdentifier(env,
                                                                                    jInputDeviceIdentifier);

    struct libevdev *dev = nullptr;

    int rc = findInputDevice(deviceIdentifier.name.data(),
                             deviceIdentifier.bus,
                             deviceIdentifier.vendor,
                             deviceIdentifier.product,
                             &dev);

    if (rc < 0) {
        return false;
    }

    AIBinder *callbackAIBinder = AIBinder_fromJavaBinder(env, jCallbackBinder);

    // Create a "strong pointer" to the callback binder.
    const ::ndk::SpAIBinder spBinder(callbackAIBinder);

    std::shared_ptr<IEvdevCallback> callback = IEvdevCallback::fromBinder(spBinder);

    std::string keyLayoutMapPath = android::getInputDeviceConfigurationFilePathByDeviceIdentifier(
            deviceIdentifier, android::InputDeviceConfigurationFileType::KEY_LAYOUT);

    LOGD("Key layout path for device %s = %s", deviceIdentifier.name.c_str(),
         keyLayoutMapPath.c_str());

    auto keyLayoutResult = android::KeyLayoutMap::load(keyLayoutMapPath, nullptr);

    if (!keyLayoutResult.ok()) {
        const auto &error = keyLayoutResult.error();

        LOGE("Failed to load key layout map for device %s: %d %s",
             deviceIdentifier.name.c_str(), error.code().value(), error.message().c_str());

        return false;
    }

    const auto &keyLayoutMap = keyLayoutResult.value();

    rc = libevdev_grab(dev, LIBEVDEV_GRAB);

    if (rc < 0) {
        LOGE("Failed to grab evdev device %s: %s",
             libevdev_get_name(dev), strerror(-rc));
        return false;
    }

    LOGD("Grabbed evdev device %s", libevdev_get_name(dev));

    struct input_event ev{};

    do {
        rc = libevdev_next_event(dev, LIBEVDEV_READ_FLAG_NORMAL, &ev);

        if (rc == 0) {
            int32_t outKeycode = -1;
            uint32_t outFlags = -1;
            keyLayoutMap->mapKey(ev.code, 0, &outKeycode, &outFlags);

            callback->onEvdevEvent(deviceId, ev.time.tv_sec, ev.time.tv_usec, ev.type, ev.code,
                                   ev.value,
                                   outKeycode);
        }

    } while (rc == 1 || rc == 0 || rc == -EAGAIN);

    libevdev_grab(dev, LIBEVDEV_UNGRAB);
    libevdev_free(dev);

    return true;
}


using aidl::io::github::sds100::keymapper::sysbridge::IEvdevCallback;
