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
#include <queue>
#include <variant>
#include <vector>
#include <sys/eventfd.h>

using aidl::io::github::sds100::keymapper::sysbridge::IEvdevCallback;

enum CommandType {
    STOP
};

struct Command {
    CommandType type;
};

struct DeviceContext {
    struct libevdev *evdev;
    struct libevdev_uinput *uinputDev;
    struct android::KeyLayoutMap keyLayoutMap;
    char devicePath[256];
};

static int epollFd = -1;
static int commandEventFd = -1;

static std::queue<Command> commandQueue;
static std::mutex commandMutex;

// This maps the file descriptor of an evdev device to its context.
static std::map<int, struct DeviceContext> *evdevDevices = new std::map<int, struct DeviceContext>();
static std::mutex evdevDevicesMutex;

#define DEBUG_PROBE false

static int findEvdevDevice(
        std::string name,
        int bus,
        int vendor,
        int product,
        libevdev **outDev,
        char *outPath
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

        // MUST be NONBLOCK so that the loop reading the evdev events eventually returns
        // due to an EAGAIN error.
        int fd = open(fullPath, O_RDONLY | O_NONBLOCK);

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

        if (DEBUG_PROBE) {
            LOGD("Evdev device: %s, bus: %d, vendor: %d, product: %d, path: %s",
                 devName, devBus, devVendor, devProduct, fullPath);
        }

        if (devName != name ||
            devVendor != vendor ||
            devProduct != product ||
            // The hidden device bus field was only added to InputDevice.java in Android 14.
            // So only check it if it is a real value
            (bus != -1 && devBus != bus)) {

            libevdev_free(*outDev);
            close(fd);
            continue;
        }

        closedir(dir);

        strcpy(outPath, fullPath);
        return 0;
    }

    closedir(dir);

    LOGE("Input device not found with name: %s, bus: %d, vendor: %d, product: %d", name.c_str(),
         bus,
         vendor, product);

    return -1;
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    evdevDevices = new std::map<int, struct DeviceContext>();
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_grabEvdevDeviceNative(JNIEnv *env,
                                                                                     jobject thiz,
                                                                                     jstring jDevicePath) {
    const char *devicePath = env->GetStringUTFChars(jDevicePath, nullptr);
    if (devicePath == nullptr) {
        return false;
    }

    bool result = false;

    {
        // Lock to prevent concurrent grab/ungrab operations on the same device
        std::lock_guard<std::mutex> lock(evdevDevicesMutex);

        // Check if device is already grabbed
        for (const auto &pair: *evdevDevices) {
            DeviceContext context = pair.second;
            if (strcmp(context.devicePath, devicePath) == 0) {
                LOGW("Device %s is already grabbed. Maybe it is a virtual uinput device.",
                     devicePath);
                env->ReleaseStringUTFChars(jDevicePath, devicePath);
                return false;
            }
        }

        // Perform synchronous grab operation
        struct libevdev *dev = nullptr;

        // MUST be NONBLOCK so that the loop reading the evdev events eventually returns
        // due to an EAGAIN error.
        int fd = open(devicePath, O_RDONLY | O_NONBLOCK);
        if (fd == -1) {
            LOGE("Failed to open device %s: %s", devicePath, strerror(errno));
            env->ReleaseStringUTFChars(jDevicePath, devicePath);
            return false;
        }

        int rc = libevdev_new_from_fd(fd, &dev);
        if (rc != 0) {
            LOGE("Failed to create libevdev device from %s: %s", devicePath, strerror(errno));
            close(fd);
            env->ReleaseStringUTFChars(jDevicePath, devicePath);
            return false;
        }

        rc = libevdev_grab(dev, LIBEVDEV_GRAB);
        if (rc < 0) {
            LOGE("Failed to grab evdev device %s: %s",
                 libevdev_get_name(dev), strerror(-rc));
            libevdev_free(dev);
            close(fd);
            env->ReleaseStringUTFChars(jDevicePath, devicePath);
            return false;
        }

        int evdevFd = libevdev_get_fd(dev);

        // Create a dummy InputDeviceIdentifier for key layout loading
        android::InputDeviceIdentifier identifier;
        identifier.name = std::string(libevdev_get_name(dev));
        identifier.bus = libevdev_get_id_bustype(dev);
        identifier.vendor = libevdev_get_id_vendor(dev);
        identifier.product = libevdev_get_id_product(dev);

        std::string klPath = android::getInputDeviceConfigurationFilePathByDeviceIdentifier(
                identifier, android::InputDeviceConfigurationFileType::KEY_LAYOUT);

        auto klResult = android::KeyLayoutMap::load(klPath, nullptr);

        if (!klResult.ok()) {
            LOGE("key layout map not found for device %s", libevdev_get_name(dev));
            libevdev_grab(dev, LIBEVDEV_UNGRAB);
            libevdev_free(dev);
            close(fd);
            env->ReleaseStringUTFChars(jDevicePath, devicePath);
            return false;
        }

        struct libevdev_uinput *uinputDev = nullptr;
        int uinputFd = open("/dev/uinput", O_RDWR);
        if (uinputFd < 0) {
            LOGE("Failed to open /dev/uinput to clone the device.");
            libevdev_grab(dev, LIBEVDEV_UNGRAB);
            libevdev_free(dev);
            close(fd);
            env->ReleaseStringUTFChars(jDevicePath, devicePath);
            return false;
        }

        rc = libevdev_uinput_create_from_device(dev, uinputFd, &uinputDev);

        if (rc < 0) {
            LOGE("Failed to create uinput device from evdev device %s: %s",
                 libevdev_get_name(dev), strerror(-rc));
            close(uinputFd);
            libevdev_grab(dev, LIBEVDEV_UNGRAB);
            libevdev_free(dev);
            close(fd);
            env->ReleaseStringUTFChars(jDevicePath, devicePath);
            return false;
        }

        DeviceContext context = {
                dev,
                uinputDev,
                *klResult.value(),
                {}  // Initialize devicePath array
        };

        strcpy(context.devicePath, devicePath);

        // Add to epoll for event monitoring (only if event loop is running)
        if (epollFd != -1) {
            struct epoll_event event{};
            event.events = EPOLLIN;
            event.data.fd = evdevFd;

            if (epoll_ctl(epollFd, EPOLL_CTL_ADD, evdevFd, &event) == -1) {
                LOGE("Failed to add new device to epoll: %s", strerror(errno));
                libevdev_uinput_destroy(uinputDev);
                libevdev_grab(dev, LIBEVDEV_UNGRAB);
                libevdev_free(dev);
                close(fd);
                env->ReleaseStringUTFChars(jDevicePath, devicePath);
                return false;
            }
        }

        evdevDevices->insert_or_assign(evdevFd, context);
        result = true;

        LOGI("Grabbed device %s, %s", libevdev_get_name(dev), context.devicePath);
    }

    env->ReleaseStringUTFChars(jDevicePath, devicePath);
    return result;
}


void onEpollEvent(DeviceContext *deviceContext, IEvdevCallback *callback) {
    struct input_event inputEvent{};

    int rc = libevdev_next_event(deviceContext->evdev, LIBEVDEV_READ_FLAG_NORMAL, &inputEvent);

    do {
        if (rc == LIBEVDEV_READ_STATUS_SUCCESS) { // rc == 0
            int32_t outKeycode = -1;
            uint32_t outFlags = -1;
            deviceContext->keyLayoutMap.mapKey(inputEvent.code, 0, &outKeycode, &outFlags);

            bool returnValue;
            callback->onEvdevEvent(deviceContext->devicePath,
                                   inputEvent.time.tv_sec,
                                   inputEvent.time.tv_usec,
                                   inputEvent.type,
                                   inputEvent.code,
                                   inputEvent.value,
                                   outKeycode,
                                   &returnValue);

            if (!returnValue) {
                libevdev_uinput_write_event(deviceContext->uinputDev,
                                            inputEvent.type,
                                            inputEvent.code,
                                            inputEvent.value);
            }

            rc = libevdev_next_event(deviceContext->evdev, LIBEVDEV_READ_FLAG_NORMAL, &inputEvent);

        } else if (rc == LIBEVDEV_READ_STATUS_SYNC) {
            rc = libevdev_next_event(deviceContext->evdev,
                                     LIBEVDEV_READ_FLAG_NORMAL | LIBEVDEV_READ_FLAG_SYNC,
                                     &inputEvent);
        }
    } while (rc != -EAGAIN);
}

// Set this to some upper limit. It is unlikely that Key Mapper will be polling
// more than a few evdev devices at once.
static int MAX_EPOLL_EVENTS = 100;

void handleCommand(const Command &cmd) {
    // Only STOP commands are handled here now, grab/ungrab are synchronous
}

extern "C"
JNIEXPORT void JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_startEvdevEventLoop(JNIEnv *env,
                                                                                   jobject thiz,
                                                                                   jobject jCallbackBinder) {
    if (epollFd != -1 || commandEventFd != -1) {
        LOGE("The evdev event loop has already started.");
        return;
    }

    epollFd = epoll_create1(EPOLL_CLOEXEC);
    if (epollFd == -1) {
        LOGE("Failed to create epoll fd: %s", strerror(errno));
        return;
    }

    commandEventFd = eventfd(0, EFD_CLOEXEC | EFD_NONBLOCK);
    if (commandEventFd == -1) {
        LOGE("Failed to create command eventfd: %s", strerror(errno));
        close(epollFd);
        return;
    }

    struct epoll_event event{};
    event.events = EPOLLIN;
    event.data.fd = commandEventFd;

    if (epoll_ctl(epollFd, EPOLL_CTL_ADD, commandEventFd, &event) == -1) {
        LOGE("Failed to add command eventfd to epoll: %s", strerror(errno));
        close(epollFd);
        close(commandEventFd);
        return;
    }

    AIBinder *callbackAIBinder = AIBinder_fromJavaBinder(env, jCallbackBinder);
    const ::ndk::SpAIBinder spBinder(callbackAIBinder);
    std::shared_ptr<IEvdevCallback> callback = IEvdevCallback::fromBinder(spBinder);

    struct epoll_event events[MAX_EPOLL_EVENTS];
    bool running = true;

    LOGI("Start evdev event loop");

    callback->onEvdevEventLoopStarted();

    while (running) {
        int n = epoll_wait(epollFd, events, MAX_EPOLL_EVENTS, -1);

        for (int i = 0; i < n; ++i) {
            if (events[i].data.fd == commandEventFd) {
                uint64_t val;
                ssize_t s = read(commandEventFd, &val, sizeof(val));

                if (s < 0) {
                    LOGE("Error reading from command event fd: %s", strerror(errno));
                }

                std::vector<Command> commandsToProcess;
                {
                    std::lock_guard<std::mutex> lock(commandMutex);
                    while (!commandQueue.empty()) {
                        commandsToProcess.push_back(commandQueue.front());
                        commandQueue.pop();
                    }
                }

                // Process commands without holding the mutex
                for (const auto &cmd: commandsToProcess) {
                    if (cmd.type == STOP) {
                        running = false;
                        break;
                    }
                    handleCommand(cmd);
                }
            } else {
                DeviceContext *dc = &evdevDevices->at(events[i].data.fd);
                onEpollEvent(dc, callback.get());
            }
        }
    }

    // Cleanup
    std::lock_guard<std::mutex> lock(evdevDevicesMutex);

    for (auto const &[fd, dc]: *evdevDevices) {
        libevdev_grab(dc.evdev, LIBEVDEV_UNGRAB);
        libevdev_free(dc.evdev);
    }

    evdevDevices->clear();
    close(commandEventFd);
    close(epollFd);

    LOGI("Stopped evdev event loop");
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_ungrabEvdevDeviceNative(JNIEnv *env,
                                                                                       jobject thiz,
                                                                                       jstring jDevicePath) {
    const char *devicePath = env->GetStringUTFChars(jDevicePath, nullptr);
    if (devicePath == nullptr) {
        return false;
    }

    bool result = false;

    {
        // Lock to prevent concurrent grab/ungrab operations
        std::lock_guard<std::mutex> lock(evdevDevicesMutex);

        for (auto it = evdevDevices->begin(); it != evdevDevices->end(); ++it) {
            if (strcmp(it->second.devicePath, devicePath) == 0) {
                // Remove from epoll first (if event loop is running)
                if (epollFd != -1) {
                    int fd = it->first;
                    if (epoll_ctl(epollFd, EPOLL_CTL_DEL, fd, nullptr) == -1) {
                        LOGW("Failed to remove device from epoll: %s", strerror(errno));
                        // Continue with ungrab even if epoll removal fails
                    }
                }

                // Do this before freeing the evdev file descriptor
                libevdev_uinput_destroy(it->second.uinputDev);

                // Ungrab the device
                libevdev_grab(it->second.evdev, LIBEVDEV_UNGRAB);

                // Free resources
                libevdev_free(it->second.evdev);

                // Remove from device map
                evdevDevices->erase(it);
                result = true;

                LOGI("Ungrabbed device %s", devicePath);
                break;
            }
        }

        if (!result) {
            LOGW("Device %s was not found in grabbed devices list", devicePath);
        }
    }

    env->ReleaseStringUTFChars(jDevicePath, devicePath);
    return result;
}


extern "C"
JNIEXPORT void JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_stopEvdevEventLoop(JNIEnv *env,
                                                                                  jobject thiz) {
    Command cmd = {STOP};

    std::lock_guard<std::mutex> lock(commandMutex);
    commandQueue.push(cmd);

    // Notify the event loop
    uint64_t val = 1;
    ssize_t written = write(commandEventFd, &val, sizeof(val));
    if (written < 0) {
        LOGE("Failed to write to commandEventFd: %s", strerror(errno));
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_writeEvdevEventNative(JNIEnv *env,
                                                                                     jobject thiz,
                                                                                     jstring jDevicePath,
                                                                                     jint type,
                                                                                     jint code,
                                                                                     jint value) {
    const char *devicePath = env->GetStringUTFChars(jDevicePath, nullptr);
    if (devicePath == nullptr) {
        return false;
    }

    bool result = false;
    {
        std::lock_guard<std::mutex> lock(evdevDevicesMutex);
        for (const auto &pair: *evdevDevices) {
            if (strcmp(pair.second.devicePath, devicePath) == 0) {
                int rc = libevdev_uinput_write_event(pair.second.uinputDev, type, code, value);
                if (rc == 0) {
                    rc = libevdev_uinput_write_event(pair.second.uinputDev, EV_SYN, SYN_REPORT, 0);
                }
                result = (rc == 0);
                break;
            }
        }
    }

    env->ReleaseStringUTFChars(jDevicePath, devicePath);
    return result;
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_ungrabAllEvdevDevicesNative(
        JNIEnv *env,
        jobject thiz) {

    {
        // Lock to prevent concurrent grab/ungrab operations
        std::lock_guard<std::mutex> lock(evdevDevicesMutex);

        // Create a copy of the iterator to avoid issues with erasing during iteration
        auto devicesCopy = *evdevDevices;

        for (const auto &pair: devicesCopy) {
            int fd = pair.first;
            const DeviceContext &context = pair.second;

            // Remove from epoll first (if event loop is running)
            if (epollFd != -1) {
                if (epoll_ctl(epollFd, EPOLL_CTL_DEL, fd, nullptr) == -1) {
                    LOGW("Failed to remove device %s from epoll: %s", context.devicePath,
                         strerror(errno));
                    // Continue with ungrab even if epoll removal fails
                }
            }

            // Do this before freeing the evdev file descriptor
            libevdev_uinput_destroy(context.uinputDev);

            // Ungrab the device
            libevdev_grab(context.evdev, LIBEVDEV_UNGRAB);

            // Free resources
            libevdev_free(context.evdev);

            LOGI("Ungrabbed device %s", context.devicePath);
        }

        // Clear all devices from the map
        evdevDevices->clear();
    }

    return true;
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_grabAllEvdevDevicesNative(
        JNIEnv *env, jobject thiz) {
    // TODO: implement grabAllEvdevDevicesNative()
}

// Helper function to create a Java EvdevDeviceHandle object
jobject
createEvdevDeviceHandle(JNIEnv *env, const char *path, const char *name, int bus, int vendor,
                        int product) {
    // Find the EvdevDeviceHandle class
    jclass evdevDeviceHandleClass = env->FindClass(
            "io/github/sds100/keymapper/common/models/EvdevDeviceHandle");
    if (evdevDeviceHandleClass == nullptr) {
        LOGE("Failed to find EvdevDeviceHandle class");
        return nullptr;
    }

    // Get the constructor
    jmethodID constructor = env->GetMethodID(evdevDeviceHandleClass, "<init>",
                                             "(Ljava/lang/String;Ljava/lang/String;III)V");
    if (constructor == nullptr) {
        LOGE("Failed to find EvdevDeviceHandle constructor");
        return nullptr;
    }

    // Create Java strings
    jstring jPath = env->NewStringUTF(path);
    jstring jName = env->NewStringUTF(name);

    // Create the object
    jobject evdevDeviceHandle = env->NewObject(evdevDeviceHandleClass, constructor, jPath, jName,
                                               bus, vendor, product);

    // Clean up local references
    env->DeleteLocalRef(jPath);
    env->DeleteLocalRef(jName);
    env->DeleteLocalRef(evdevDeviceHandleClass);

    return evdevDeviceHandle;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_getEvdevDevicesNative(JNIEnv *env,
                                                                                     jobject thiz) {
    DIR *dir = opendir("/dev/input");

    if (dir == nullptr) {
        LOGE("Failed to open /dev/input directory");
        return nullptr;
    }

    std::vector<jobject> deviceHandles;
    struct dirent *entry;

    while ((entry = readdir(dir)) != nullptr) {
        // Skip . and .. entries
        if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0) {
            continue;
        }

        char fullPath[256];
        snprintf(fullPath, sizeof(fullPath), "/dev/input/%s", entry->d_name);

        bool ignoreDevice = false;

        {
            std::lock_guard<std::mutex> lock(evdevDevicesMutex);
            // Ignore this device if it is a uinput device we created
            for (const auto &pair: *evdevDevices) {
                DeviceContext context = pair.second;
                const char *uinputDevicePath = libevdev_uinput_get_devnode(context.uinputDev);

                if (strcmp(fullPath, uinputDevicePath) == 0) {
                    LOGW("Ignoring uinput device %s.", uinputDevicePath);
                    ignoreDevice = true;
                    break;
                }
            }
        }

        if (ignoreDevice) {
            continue;
        }

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

        if (DEBUG_PROBE) {
            LOGD("Evdev device: %s, bus: %d, vendor: %d, product: %d, path: %s",
                 devName, devBus, devVendor, devProduct, fullPath);
        }

        // Create EvdevDeviceHandle object
        jobject deviceHandle = createEvdevDeviceHandle(env, fullPath, devName, devBus, devVendor,
                                                       devProduct);
        if (deviceHandle != nullptr) {
            deviceHandles.push_back(deviceHandle);
        }

        libevdev_free(dev);
        close(fd);
    }

    closedir(dir);

    // Create the Java array
    jclass evdevDeviceHandleClass = env->FindClass(
            "io/github/sds100/keymapper/common/models/EvdevDeviceHandle");
    if (evdevDeviceHandleClass == nullptr) {
        LOGE("Failed to find EvdevDeviceHandle class for array creation");
        return nullptr;
    }

    jobjectArray result = env->NewObjectArray(deviceHandles.size(), evdevDeviceHandleClass,
                                              nullptr);
    if (result == nullptr) {
        LOGE("Failed to create EvdevDeviceHandle array");
        env->DeleteLocalRef(evdevDeviceHandleClass);
        return nullptr;
    }

    // Fill the array
    for (size_t i = 0; i < deviceHandles.size(); i++) {
        env->SetObjectArrayElement(result, i, deviceHandles[i]);
        env->DeleteLocalRef(deviceHandles[i]); // Clean up local reference
    }

    env->DeleteLocalRef(evdevDeviceHandleClass);
    return result;
}