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
#include <sys/eventfd.h>

using aidl::io::github::sds100::keymapper::sysbridge::IEvdevCallback;

struct GrabData {
    int deviceId;
    android::InputDeviceIdentifier identifier;
};

struct UngrabData {
    int deviceId;
};

enum CommandType {
    GRAB,
    UNGRAB,
    STOP
};

struct Command {
    CommandType type;
    std::variant<GrabData, UngrabData> data;
};

struct DeviceContext {
    int deviceId;
    struct android::InputDeviceIdentifier inputDeviceIdentifier;
    struct libevdev *evdev;
    struct android::KeyLayoutMap keyLayoutMap;
};

void ungrabDevice(jint device_id);

static int epollFd = -1;
static int commandEventFd = -1;

static std::queue<Command> commandQueue;
static std::mutex commandMutex;

// This maps the file descriptor of an evdev device to its context.
static std::map<int, struct DeviceContext> *evdevDevices = new std::map<int, struct DeviceContext>();
static std::mutex evdevDevicesMutex;

static int findEvdevDevice(
        std::string name,
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

        if (name != devName ||
            devVendor != vendor ||
            devProduct != product ||
            devBus != bus) {

            libevdev_free(*outDev);
            close(fd);
            continue;
        }

        closedir(dir);

        return 0;
    }

    closedir(dir);

    LOGE("Input device not found with name: %s, bus: %d, vendor: %d, product: %d", name.c_str(),
         bus,
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

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    evdevDevices = new std::map<int, struct DeviceContext>();
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_grabEvdevDeviceNative(JNIEnv *env,
                                                                                     jobject thiz,
                                                                                     jobject jInputDeviceIdentifier) {
    jclass inputDeviceIdentifierClass = env->GetObjectClass(jInputDeviceIdentifier);
    jfieldID idFieldId = env->GetFieldID(inputDeviceIdentifierClass, "id", "I");
    android::InputDeviceIdentifier identifier = convertJInputDeviceIdentifier(env,
                                                                              jInputDeviceIdentifier);
    int deviceId = env->GetIntField(jInputDeviceIdentifier, idFieldId);

    Command cmd = {GRAB, GrabData{deviceId, identifier}};

    std::lock_guard<std::mutex> lock(commandMutex);
    commandQueue.push(cmd);

    uint64_t val = 1;
    ssize_t written = write(commandEventFd, &val, sizeof(val));

    if (written < 0) {
        LOGE("Failed to write to commandEventFd: %s", strerror(errno));
        return false;
    }

    return true;
}


int onEpollEvent(DeviceContext *deviceContext, IEvdevCallback *callback) {
    struct input_event inputEvent{};

    // the number of ready file descriptors
    int rc = libevdev_next_event(deviceContext->evdev, LIBEVDEV_READ_FLAG_NORMAL, &inputEvent);

    if (rc == 0) {
        int32_t outKeycode = -1;
        uint32_t outFlags = -1;
        int deviceId = deviceContext->deviceId;
        deviceContext->keyLayoutMap.mapKey(inputEvent.code, 0, &outKeycode, &outFlags);

        callback->onEvdevEvent(deviceId,
                               inputEvent.time.tv_sec,
                               inputEvent.time.tv_usec,
                               inputEvent.type,
                               inputEvent.code,
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

void handleCommand(const Command &cmd) {
    if (cmd.type == GRAB) {
        const GrabData &data = std::get<GrabData>(cmd.data);

        struct libevdev *dev = nullptr;
        int rc = findEvdevDevice(data.identifier.name,
                                 data.identifier.bus,
                                 data.identifier.vendor,
                                 data.identifier.product,
                                 &dev);
        if (rc < 0) {
            LOGE("Failed to find device for grab command");
            return;
        }

        rc = libevdev_grab(dev, LIBEVDEV_GRAB);
        if (rc < 0) {
            LOGE("Failed to grab evdev device %s: %s",
                 libevdev_get_name(dev), strerror(-rc));
            libevdev_free(dev);
            return;
        }

        int evdevFd = libevdev_get_fd(dev);
        std::string klPath = android::getInputDeviceConfigurationFilePathByDeviceIdentifier(
                data.identifier, android::InputDeviceConfigurationFileType::KEY_LAYOUT);
        auto klResult = android::KeyLayoutMap::load(klPath, nullptr);

        if (!klResult.ok()) {
            LOGE("key layout map not found for device %s", libevdev_get_name(dev));
            return;
        }

        DeviceContext context{
                data.deviceId,
                data.identifier,
                dev,
                *klResult.value()
        };

        struct epoll_event event{};
        event.events = EPOLLIN;
        event.data.fd = evdevFd;
        if (epoll_ctl(epollFd, EPOLL_CTL_ADD, evdevFd, &event) == -1) {
            LOGE("Failed to add new device to epoll: %s", strerror(errno));
            libevdev_free(dev);
            return;
        }

        std::lock_guard<std::mutex> lock(evdevDevicesMutex);
        evdevDevices->insert_or_assign(evdevFd, context);

    } else if (cmd.type == UNGRAB) {
        const UngrabData &data = std::get<UngrabData>(cmd.data);

        std::lock_guard<std::mutex> lock(evdevDevicesMutex);
        for (auto it = evdevDevices->begin(); it != evdevDevices->end(); ++it) {
            if (it->second.deviceId == data.deviceId) {
                int fd = it->first;
                epoll_ctl(epollFd, EPOLL_CTL_DEL, fd, nullptr);
                libevdev_grab(it->second.evdev, LIBEVDEV_UNGRAB);
                libevdev_free(it->second.evdev);
                evdevDevices->erase(it);
                break;
            }
        }
    }
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

    epollFd = epoll_create1(EPOLL_CLOEXEC | EPOLLWAKEUP);
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

    while (running) {
        int n = epoll_wait(epollFd, events, MAX_EPOLL_EVENTS, -1);

        for (int i = 0; i < n; ++i) {
            if (events[i].data.fd == commandEventFd) {
                uint64_t val;
                ssize_t s = read(commandEventFd, &val, sizeof(val));
                if (s < 0) {
                    LOGE("Error reading from command event fd: %s", strerror(errno));
                }

                std::lock_guard<std::mutex> lock(commandMutex);
                while (!commandQueue.empty()) {
                    Command cmd = commandQueue.front();
                    commandQueue.pop();
                    if (cmd.type == STOP) {
                        running = false;
                        break;
                    }
                    handleCommand(cmd);
                }
            } else {
                std::lock_guard<std::mutex> lock(evdevDevicesMutex);
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
}

void ungrabDevice(int deviceId) {
    LOGI("Ungrab device %d", deviceId);

    Command cmd;
    cmd.type = UNGRAB;
    cmd.data = UngrabData{deviceId};

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
JNIEXPORT void JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_ungrabEvdevDeviceNative(JNIEnv *env,
                                                                                       jobject thiz,
                                                                                       jint device_id) {
    ungrabDevice(device_id);
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
                                                                                     jint device_id,
                                                                                     jint type,
                                                                                     jint code,
                                                                                     jint value) {
    // TODO: implement writeEvdevEvent()
}
extern "C"
JNIEXPORT void JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_ungrabAllEvdevDevicesNative(
        JNIEnv *env,
        jobject thiz) {
    std::vector<int> deviceIds;

    {
        std::lock_guard<std::mutex> evdevLock(evdevDevicesMutex);

        for (auto pair: *evdevDevices) {
            deviceIds.push_back(pair.second.deviceId);
        }
    }

    for (int id: deviceIds) {
        ungrabDevice(id);
    }
}