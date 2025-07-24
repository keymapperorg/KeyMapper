/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "InputDevice"

#include <cstdlib>
#include <unistd.h>
#include <cctype>

#include "../logging.h"
#include "../libbase/stringprintf.h"
#include <ftl/enum.h>
#include "InputDevice.h"
#include "InputEventLabels.h"
#include "../ui/LogicalDisplayId.h"

using android::base::StringPrintf;

namespace android {

// Set to true to log detailed debugging messages about IDC file probing.
    static constexpr bool DEBUG_PROBE = false;

    static const char *CONFIGURATION_FILE_DIR[] = {
            "idc/",
            "keylayout/",
            "keychars/",
    };

    static const char *CONFIGURATION_FILE_EXTENSION[] = {
            ".idc",
            ".kl",
            ".kcm",
    };

    static bool isValidNameChar(char ch) {
        return isascii(ch) && (isdigit(ch) || isalpha(ch) || ch == '-' || ch == '_');
    }

    static void appendInputDeviceConfigurationFileRelativePath(std::string &path,
                                                               const std::string &name,
                                                               InputDeviceConfigurationFileType type) {
        path += CONFIGURATION_FILE_DIR[static_cast<int32_t>(type)];
        path += name;
        path += CONFIGURATION_FILE_EXTENSION[static_cast<int32_t>(type)];
    }

    std::string getInputDeviceConfigurationFilePathByDeviceIdentifier(
            const InputDeviceIdentifier &deviceIdentifier, InputDeviceConfigurationFileType type,
            const char *suffix) {
        if (deviceIdentifier.vendor != 0 && deviceIdentifier.product != 0) {
            if (deviceIdentifier.version != 0) {
                // Try vendor product version.
                std::string versionPath =
                        getInputDeviceConfigurationFilePathByName(
                                StringPrintf("Vendor_%04x_Product_%"
                                             "04x_Version_%04x%s",
                                             deviceIdentifier.vendor,
                                             deviceIdentifier.product,
                                             deviceIdentifier.version,
                                             suffix),
                                type);
                if (!versionPath.empty()) {
                    return versionPath;
                }
            }

            // Try vendor product.
            std::string productPath =
                    getInputDeviceConfigurationFilePathByName(
                            StringPrintf("Vendor_%04x_Product_%04x%s",
                                         deviceIdentifier.vendor,
                                         deviceIdentifier.product,
                                         suffix),
                            type);
            if (!productPath.empty()) {
                return productPath;
            }
        }

        // Try device name.
        return getInputDeviceConfigurationFilePathByName(
                deviceIdentifier.getCanonicalName() + suffix,
                type);
    }

    std::string getInputDeviceConfigurationFilePathByName(
            const std::string &name, InputDeviceConfigurationFileType type) {
        // Search system repository.
        std::string path;

        // Treblized input device config files will be located /product/usr, /system_ext/usr,
        // /odm/usr or /vendor/usr.
        std::vector<std::string> pathPrefixes{
                "/product/usr/",
                "/system_ext/usr/",
                "/odm/usr/",
                "/vendor/usr/",
        };
        // These files may also be in the APEX pointed by input_device.config_file.apex sysprop.
//        if (auto apex = GetProperty("input_device.config_file.apex", ""); !apex.empty()) {
//            pathPrefixes.push_back("/apex/" + apex + "/etc/usr/");
//        }
        // ANDROID_ROOT may not be set on host
        if (auto android_root = getenv("ANDROID_ROOT"); android_root != nullptr) {
            pathPrefixes.push_back(std::string(android_root) + "/usr/");
        }
        for (const auto &prefix: pathPrefixes) {
            path = prefix;
            appendInputDeviceConfigurationFileRelativePath(path, name, type);
            if (!access(path.c_str(), R_OK)) {
                if (DEBUG_PROBE) {
                    LOGI("Found system-provided input device configuration file at %s",
                         path.c_str());
                }
                return path;
            } else if (errno != ENOENT) {
                LOGW("Couldn't find a system-provided input device configuration file at %s due to error %d (%s); there may be an IDC file there that cannot be loaded.",
                     path.c_str(), errno, strerror(errno));
            } else {
                if (DEBUG_PROBE) {
                    LOGE("Didn't find system-provided input device configuration file at %s: %s",
                         path.c_str(), strerror(errno));
                }
            }
        }

        // Search user repository.
        // TODO Should only look here if not in safe mode.
        path = "";
        char *androidData = getenv("ANDROID_DATA");
        if (androidData != nullptr) {
            path += androidData;
        }
        path += "/system/devices/";
        appendInputDeviceConfigurationFileRelativePath(path, name, type);
        if (!access(path.c_str(), R_OK)) {
            if (DEBUG_PROBE) {
                LOGI("Found system user input device configuration file at %s", path.c_str());
            }
            return path;
        } else if (errno != ENOENT) {
            LOGW("Couldn't find a system user input device configuration file at %s due to error %d (%s); there may be an IDC file there that cannot be loaded.",
                 path.c_str(), errno, strerror(errno));
        } else {
            if (DEBUG_PROBE) {
                LOGE("Didn't find system user input device configuration file at %s: %s",
                     path.c_str(), strerror(errno));
            }
        }

        // Not found.
        if (DEBUG_PROBE) {
            LOGI("Probe failed to find input device configuration file with name '%s' and type %s",
                 name.c_str(), ftl::enum_string(type).c_str());
        }
        return "";
    }

// --- InputDeviceIdentifier

    std::string InputDeviceIdentifier::getCanonicalName() const {
        std::string replacedName = name;
        for (char &ch: replacedName) {
            if (!isValidNameChar(ch)) {
                ch = '_';
            }
        }
        return replacedName;
    }


// --- InputDeviceInfo ---

    InputDeviceInfo::InputDeviceInfo() {
        initialize(-1, 0, -1, InputDeviceIdentifier(), "", false, false,
                   ui::LogicalDisplayId::INVALID);
    }

    InputDeviceInfo::InputDeviceInfo(const InputDeviceInfo &other)
            : mId(other.mId),
              mGeneration(other.mGeneration),
              mControllerNumber(other.mControllerNumber),
              mIdentifier(other.mIdentifier),
              mAlias(other.mAlias),
              mIsExternal(other.mIsExternal),
              mHasMic(other.mHasMic),
              mKeyboardLayoutInfo(other.mKeyboardLayoutInfo),
              mSources(other.mSources),
              mKeyboardType(other.mKeyboardType),
              mUsiVersion(other.mUsiVersion),
              mAssociatedDisplayId(other.mAssociatedDisplayId),
              mEnabled(other.mEnabled),
              mHasVibrator(other.mHasVibrator),
              mHasBattery(other.mHasBattery),
              mHasButtonUnderPad(other.mHasButtonUnderPad),
              mHasSensor(other.mHasSensor),
              mMotionRanges(other.mMotionRanges),
              mSensors(other.mSensors),
              mLights(other.mLights),
              mViewBehavior(other.mViewBehavior) {}

    InputDeviceInfo &InputDeviceInfo::operator=(const InputDeviceInfo &other) {
        mId = other.mId;
        mGeneration = other.mGeneration;
        mControllerNumber = other.mControllerNumber;
        mIdentifier = other.mIdentifier;
        mAlias = other.mAlias;
        mIsExternal = other.mIsExternal;
        mHasMic = other.mHasMic;
        mKeyboardLayoutInfo = other.mKeyboardLayoutInfo;
        mSources = other.mSources;
        mKeyboardType = other.mKeyboardType;
        mUsiVersion = other.mUsiVersion;
        mAssociatedDisplayId = other.mAssociatedDisplayId;
        mEnabled = other.mEnabled;
        mHasVibrator = other.mHasVibrator;
        mHasBattery = other.mHasBattery;
        mHasButtonUnderPad = other.mHasButtonUnderPad;
        mHasSensor = other.mHasSensor;
        mMotionRanges = other.mMotionRanges;
        mSensors = other.mSensors;
        mLights = other.mLights;
        mViewBehavior = other.mViewBehavior;
        return *this;
    }

    InputDeviceInfo::~InputDeviceInfo() {
    }

    void InputDeviceInfo::initialize(int32_t id, int32_t generation, int32_t controllerNumber,
                                     const InputDeviceIdentifier &identifier,
                                     const std::string &alias,
                                     bool isExternal, bool hasMic,
                                     ui::LogicalDisplayId associatedDisplayId,
                                     InputDeviceViewBehavior viewBehavior, bool enabled) {
        mId = id;
        mGeneration = generation;
        mControllerNumber = controllerNumber;
        mIdentifier = identifier;
        mAlias = alias;
        mIsExternal = isExternal;
        mHasMic = hasMic;
        mSources = 0;
        mKeyboardType = AINPUT_KEYBOARD_TYPE_NONE;
        mAssociatedDisplayId = associatedDisplayId;
        mEnabled = enabled;
        mHasVibrator = false;
        mHasBattery = false;
        mHasButtonUnderPad = false;
        mHasSensor = false;
        mViewBehavior = viewBehavior;
        mUsiVersion.reset();
        mMotionRanges.clear();
        mSensors.clear();
        mLights.clear();
    }

    const InputDeviceInfo::MotionRange *InputDeviceInfo::getMotionRange(
            int32_t axis, uint32_t source) const {
        for (const MotionRange &range: mMotionRanges) {
            if (range.axis == axis && isFromSource(range.source, source)) {
                return &range;
            }
        }
        return nullptr;
    }

    void InputDeviceInfo::addSource(uint32_t source) {
        mSources |= source;
    }

    void InputDeviceInfo::addMotionRange(int32_t axis, uint32_t source, float min, float max,
                                         float flat, float fuzz, float resolution) {
        MotionRange range = {axis, source, min, max, flat, fuzz, resolution};
        mMotionRanges.push_back(range);
    }

    void InputDeviceInfo::addMotionRange(const MotionRange &range) {
        mMotionRanges.push_back(range);
    }

    void InputDeviceInfo::addSensorInfo(const InputDeviceSensorInfo &info) {
        if (mSensors.find(info.type) != mSensors.end()) {
            LOGW("Sensor type %s already exists, will be replaced by new sensor added.",
                 ftl::enum_string(info.type).c_str());
        }
        mSensors.insert_or_assign(info.type, info);
    }

    void InputDeviceInfo::addBatteryInfo(const InputDeviceBatteryInfo &info) {
        if (mBatteries.find(info.id) != mBatteries.end()) {
            LOGW("Battery id %d already exists, will be replaced by new battery added.", info.id);
        }
        mBatteries.insert_or_assign(info.id, info);
    }

    void InputDeviceInfo::addLightInfo(const InputDeviceLightInfo &info) {
        if (mLights.find(info.id) != mLights.end()) {
            LOGW("Light id %d already exists, will be replaced by new light added.", info.id);
        }
        mLights.insert_or_assign(info.id, info);
    }

    void InputDeviceInfo::setKeyboardType(int32_t keyboardType) {
        mKeyboardType = keyboardType;
    }

    void InputDeviceInfo::setKeyboardLayoutInfo(KeyboardLayoutInfo layoutInfo) {
        mKeyboardLayoutInfo = std::move(layoutInfo);
    }

    std::vector<InputDeviceSensorInfo> InputDeviceInfo::getSensors() {
        std::vector<InputDeviceSensorInfo> infos;
        infos.reserve(mSensors.size());
        for (const auto &[type, info]: mSensors) {
            infos.push_back(info);
        }
        return infos;
    }

    std::vector<InputDeviceLightInfo> InputDeviceInfo::getLights() {
        std::vector<InputDeviceLightInfo> infos;
        infos.reserve(mLights.size());
        for (const auto &[id, info]: mLights) {
            infos.push_back(info);
        }
        return infos;
    }

} // namespace android