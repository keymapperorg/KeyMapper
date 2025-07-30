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
#include <sys/epoll.h>
#include <map>

using aidl::io::github::sds100::keymapper::sysbridge::IEvdevCallback;

struct DeviceContext {
    int deviceId;
    struct android::InputDeviceIdentifier inputDeviceIdentifier;
    struct libevdev *evdev;
    struct android::KeyLayoutMap keyLayoutMap;
};

static int epollFd = -1;
static std::mutex epollMutex;

// This maps the file descriptor of an evdev device to its context.
static std::map<int, struct DeviceContext> *evdevDevices = new std::map<int, struct DeviceContext>();
static std::mutex evdevDevicesMutex;

static int findEvdevDevice(
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

        int status = libevdev_new_from_fd(fd, outDev);

        if (status != 0) {
            LOGE("Failed to open libevdev device from path %s: %s", fullPath, strerror(errno));
            close(fd);
            continue;
        }

        const char *devName = libevdev_get_name(*outDev);
        int devVendor = libevdev_get_id_vendor(*outDev);
        int devProduct = libevdev_get_id_product(*outDev);
        int devBus = libevdev_get_id_bustype(*outDev);

//        LOGD("Checking device: %s, bus: %d, vendor: %d, product: %d",
//             devName, devBus, devVendor, devProduct);

        if (strcmp(devName, name) != 0 ||
            devVendor != vendor ||
            devProduct != product ||
            devBus != bus) {

            libevdev_free(*outDev); // libevdev_free also closes the fd
            continue;
        }

        closedir(dir);

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
                                                                               jobject jInputDeviceIdentifier) {

    LOGD("Start gravEvdevDevice");
    jclass inputDeviceIdentifierClass = env->GetObjectClass(jInputDeviceIdentifier);
    jfieldID idFieldId = env->GetFieldID(inputDeviceIdentifierClass, "id", "I");
    int deviceId = env->GetIntField(jInputDeviceIdentifier, idFieldId);

    android::InputDeviceIdentifier deviceIdentifier = convertJInputDeviceIdentifier(env,
                                                                                    jInputDeviceIdentifier);

    struct libevdev *dev = nullptr;

    int rc = findEvdevDevice(deviceIdentifier.name.data(),
                             deviceIdentifier.bus,
                             deviceIdentifier.vendor,
                             deviceIdentifier.product,
                             &dev);

    if (rc < 0) {
        return false;
    }

    std::string keyLayoutMapPath = android::getInputDeviceConfigurationFilePathByDeviceIdentifier(
            deviceIdentifier, android::InputDeviceConfigurationFileType::KEY_LAYOUT);

    LOGD("Key layout path for device %s = %s", deviceIdentifier.name.c_str(),
         keyLayoutMapPath.c_str());

    auto keyLayoutResult = android::KeyLayoutMap::load(keyLayoutMapPath, nullptr);

    if (!keyLayoutResult.ok()) {
        const auto &error = keyLayoutResult.error();

        LOGE("Failed to load key layout map for device %s: %d %s",
             deviceIdentifier.name.c_str(), error.code().value(), error.message().c_str());

        int fd = libevdev_get_fd(dev);
        libevdev_free(dev);
        close(fd);
        return false;
    }

    const auto &keyLayoutMap = keyLayoutResult.value();

    int originalFd = libevdev_get_fd(dev);
    int newFd = dup(originalFd);

    if (newFd == -1) {
        LOGE("Failed to duplicate file descriptor: %s", strerror(errno));
        libevdev_free(dev); // This also closes originalFd
        return false;
    }

    libevdev_free(dev); // Free the original device context, which also closes originalFd.

    struct libevdev *newDev = nullptr;
    rc = libevdev_new_from_fd(newFd, &newDev);
    if (rc < 0) {
        LOGE("Failed to create new libevdev device from duplicated fd: %s", strerror(-rc));
        close(newFd);
        return false;
    }

    rc = libevdev_grab(newDev, LIBEVDEV_GRAB);

    if (rc < 0) {
        LOGE("Failed to grab evdev device %s: %s",
             libevdev_get_name(newDev), strerror(-rc));
        libevdev_free(newDev); // This also closes newFd
        return false;
    }

    int evdevFd = libevdev_get_fd(newDev);

    std::lock_guard epollLock(epollMutex);

    struct epoll_event epollEvent{};
    epollEvent.events = EPOLLIN;

    rc = epoll_ctl(epollFd, EPOLL_CTL_ADD, evdevFd, &epollEvent);

    if (rc == -1) {
        LOGE("Error adding device to epoll: %s", strerror(errno));
        libevdev_free(newDev);
        return false;
    }

    std::lock_guard evdevLock(evdevDevicesMutex);

    if (evdevDevices->contains(evdevFd)) {
        LOGE("This evdev device is already being listened to. Ungrab it first");
        libevdev_free(newDev);
        return false;
    } else {
        DeviceContext deviceContext = DeviceContext{
                deviceId,
                deviceIdentifier,
                newDev,
                *keyLayoutMap
        };

        evdevDevices->insert_or_assign(evdevFd, deviceContext);
    }

    LOGD("Grabbed evdev device %s", libevdev_get_name(newDev));

    return true;
}


int onEpollEvent(DeviceContext *deviceContext, std::shared_ptr<IEvdevCallback> callback) {
    struct input_event inputEvent{};

    // the number of ready file descriptors
    int rc = libevdev_next_event(deviceContext->evdev, LIBEVDEV_READ_FLAG_NORMAL, &inputEvent);

    if (rc == 0) {
        int32_t outKeycode = -1;
        uint32_t outFlags = -1;
        deviceContext->keyLayoutMap.mapKey(inputEvent.code, 0, &outKeycode, &outFlags);

        callback->onEvdevEvent(deviceContext->deviceId, inputEvent.time.tv_sec,
                               inputEvent.time.tv_usec,
                               inputEvent.type, inputEvent.code,
                               inputEvent.value,
                               outKeycode);
    }

    if (rc == 1 || rc == 0 || rc == -EAGAIN) {
        return 0;
    } else {
        return rc;
    }
}

// Set this to some upper limit. It is unlikely that Key Mapper will be polling
// more than a few evdev devices at once.
static int MAX_EPOLL_EVENTS = 100;

extern "C"
JNIEXPORT void JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_startEvdevEventLoop(JNIEnv *env,
                                                                                   jobject thiz,
                                                                                   jobject jCallbackBinder) {
    AIBinder *callbackAIBinder = AIBinder_fromJavaBinder(env, jCallbackBinder);

    // Create a "strong pointer" to the callback binder.
    const ::ndk::SpAIBinder spBinder(callbackAIBinder);

    std::shared_ptr<IEvdevCallback> callback = IEvdevCallback::fromBinder(spBinder);

    std::unique_lock<std::mutex> epollLock(epollMutex);

    epollFd = epoll_create1(EPOLL_CLOEXEC);

    if (epollFd == -1) {
        LOGE("Error creating epoll file descriptor: %s", strerror(errno));

        epollLock.unlock();
        return;
    }

    struct epoll_event epollEvent{};
    epollEvent.events = EPOLLIN;

    epollLock.unlock();

    LOGD("Starting evdev event loop");

    struct epoll_event events[MAX_EPOLL_EVENTS];

    while (true) {
        LOGD("epoll_wait");
        int rc = epoll_wait(epollFd, events, MAX_EPOLL_EVENTS, -1);
        LOGD("Post epoll wait %d", rc);

        if (rc == -1) {
            // Error
            LOGE("epoll_wait error %s", strerror(errno));
            continue;
        } else if (rc == 0) {
            // timeout
            continue;
        } else {
            std::lock_guard evdevDevicesLock(evdevDevicesMutex);

            for (int i = 0; i < MAX_EPOLL_EVENTS; i++) {
                epoll_event ev = events[i];
                int epollDataFd = ev.data.fd;

                if (!evdevDevices->contains(epollDataFd)) {
                    // Stop polling this file descriptor since it is not in the list of evdev devices
                    // to listen to.
                    epoll_ctl(epollFd, EPOLL_CTL_DEL, epollDataFd, &epollEvent);
                    continue;
                }

                DeviceContext *device = &(evdevDevices->at(epollDataFd));

                rc = onEpollEvent(device, callback);
                // TODO handle evdevevent errors
            }

        }
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_ungrabEvdevDevice(JNIEnv *env,
                                                                                 jobject thiz,
                                                                                 jint device_id) {

    std::lock_guard evdevLock(evdevDevicesMutex);

    // Find the device by device_id
    for (auto it = evdevDevices->begin(); it != evdevDevices->end(); ++it) {
        if (it->second.deviceId == device_id) {
            int evdevFd = it->first;
            DeviceContext *deviceContext = &(it->second);

            // Ungrab the device
            libevdev_grab(deviceContext->evdev, LIBEVDEV_UNGRAB);

            // Remove from epoll
            std::lock_guard epollLock(epollMutex);
            if (epollFd != -1) {
                epoll_ctl(epollFd, EPOLL_CTL_DEL, evdevFd, nullptr);
            }

            // Free the libevdev device
            libevdev_free(deviceContext->evdev);

            // Remove from our map
            evdevDevices->erase(it);

            LOGD("Ungrabbed evdev device with id %d", device_id);
            return;
        }
    }

    LOGE("Device with id %d not found for ungrab", device_id);
}


extern "C"
JNIEXPORT void JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_stopEvdevEventLoop(JNIEnv *env,
                                                                                  jobject thiz) {
    std::lock_guard evdevLock(evdevDevicesMutex);

    // Clean up all devices
    for (auto it = evdevDevices->begin(); it != evdevDevices->end(); ++it) {
        DeviceContext *deviceContext = &(it->second);

        // Ungrab the device
        libevdev_grab(deviceContext->evdev, LIBEVDEV_UNGRAB);

        // Free the libevdev device
        libevdev_free(deviceContext->evdev);
    }

    // Clear the map
    evdevDevices->clear();

    // Close epoll file descriptor
    std::lock_guard epollLock(epollMutex);
    if (epollFd != -1) {
        close(epollFd);
        epollFd = -1;
    }

    LOGD("Stopped evdev event loop and cleaned up all devices");
}