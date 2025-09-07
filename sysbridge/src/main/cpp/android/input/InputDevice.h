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

#pragma once

#include <android/sensor.h>
#include "../ftl/flags.h"
#include "../ftl/mixins.h"
#include "Input.h"
#include <set>
#include <unordered_map>
#include <vector>
#include "../ui/LogicalDisplayId.h"

namespace android {

/*
 * Identifies a device.
 */
    struct InputDeviceIdentifier {
        inline InputDeviceIdentifier() :
                bus(0), vendor(0), product(0), version(0) {
        }

        // Information provided by the kernel.
        std::string name;
        std::string location;
        std::string uniqueId;
        int bus;
        int vendor;
        int product;
        int version;

        // A composite input device descriptor string that uniquely identifies the device
        // even across reboots or reconnections.  The value of this field is used by
        // upper layers of the input system to associate settings with individual devices.
        // It is hashed from whatever kernel provided information is available.
        // Ideally, the way this value is computed should not change between Android releases
        // because that would invalidate persistent settings that rely on it.
        std::string descriptor;

        // A value added to uniquely identify a device in the absence of a unique id. This
        // is intended to be a minimum way to distinguish from other active devices and may
        // reuse values that are not associated with an input anymore.
        uint16_t nonce;

        // The bluetooth address of the device, if known.
        std::optional<std::string> bluetoothAddress;

        /**
         * Return InputDeviceIdentifier.name that has been adjusted as follows:
         *     - all characters besides alphanumerics, dash,
         *       and underscore have been replaced with underscores.
         * This helps in situations where a file that matches the device name is needed,
         * while conforming to the filename limitations.
         */
        std::string getCanonicalName() const;

        bool operator==(const InputDeviceIdentifier &) const = default;

        bool operator!=(const InputDeviceIdentifier &) const = default;
    };

/**
 * Holds View related behaviors for an InputDevice.
 */
    struct InputDeviceViewBehavior {
        /**
         * The smooth scroll behavior that applies for all source/axis, if defined by the device.
         * Empty optional if the device has not specified the default smooth scroll behavior.
         */
        std::optional<bool> shouldSmoothScroll;
    };

/* Types of input device sensors. Keep sync with core/java/android/hardware/Sensor.java */
    enum class InputDeviceSensorType : int32_t {
        ACCELEROMETER = ASENSOR_TYPE_ACCELEROMETER,
        MAGNETIC_FIELD = ASENSOR_TYPE_MAGNETIC_FIELD,
        ORIENTATION = 3,
        GYROSCOPE = ASENSOR_TYPE_GYROSCOPE,
        LIGHT = ASENSOR_TYPE_LIGHT,
        PRESSURE = ASENSOR_TYPE_PRESSURE,
        TEMPERATURE = 7,
        PROXIMITY = ASENSOR_TYPE_PROXIMITY,
        GRAVITY = ASENSOR_TYPE_GRAVITY,
        LINEAR_ACCELERATION = ASENSOR_TYPE_LINEAR_ACCELERATION,
        ROTATION_VECTOR = ASENSOR_TYPE_ROTATION_VECTOR,
        RELATIVE_HUMIDITY = ASENSOR_TYPE_RELATIVE_HUMIDITY,
        AMBIENT_TEMPERATURE = ASENSOR_TYPE_AMBIENT_TEMPERATURE,
        MAGNETIC_FIELD_UNCALIBRATED = ASENSOR_TYPE_MAGNETIC_FIELD_UNCALIBRATED,
        GAME_ROTATION_VECTOR = ASENSOR_TYPE_GAME_ROTATION_VECTOR,
        GYROSCOPE_UNCALIBRATED = ASENSOR_TYPE_GYROSCOPE_UNCALIBRATED,
        SIGNIFICANT_MOTION = ASENSOR_TYPE_SIGNIFICANT_MOTION,

        ftl_first = ACCELEROMETER,
        ftl_last = SIGNIFICANT_MOTION
    };

    enum class InputDeviceSensorAccuracy : int32_t {
        NONE = 0,
        LOW = 1,
        MEDIUM = 2,
        HIGH = 3,

        ftl_last = HIGH,
    };

    enum class InputDeviceSensorReportingMode : int32_t {
        CONTINUOUS = 0,
        ON_CHANGE = 1,
        ONE_SHOT = 2,
        SPECIAL_TRIGGER = 3,
    };

    enum class InputDeviceLightType : int32_t {
        INPUT = 0,
        PLAYER_ID = 1,
        KEYBOARD_BACKLIGHT = 2,
        KEYBOARD_MIC_MUTE = 3,
        KEYBOARD_VOLUME_MUTE = 4,

        ftl_last = KEYBOARD_VOLUME_MUTE
    };

    enum class InputDeviceLightCapability : uint32_t {
        /** Capability to change brightness of the light */
        BRIGHTNESS = 0x00000001,
        /** Capability to change color of the light */
        RGB = 0x00000002,
    };

    struct InputDeviceSensorInfo {
        explicit InputDeviceSensorInfo(std::string name, std::string vendor, int32_t version,
                                       InputDeviceSensorType type,
                                       InputDeviceSensorAccuracy accuracy,
                                       float maxRange, float resolution, float power,
                                       int32_t minDelay,
                                       int32_t fifoReservedEventCount, int32_t fifoMaxEventCount,
                                       std::string stringType, int32_t maxDelay, int32_t flags,
                                       int32_t id)
                : name(name),
                  vendor(vendor),
                  version(version),
                  type(type),
                  accuracy(accuracy),
                  maxRange(maxRange),
                  resolution(resolution),
                  power(power),
                  minDelay(minDelay),
                  fifoReservedEventCount(fifoReservedEventCount),
                  fifoMaxEventCount(fifoMaxEventCount),
                  stringType(stringType),
                  maxDelay(maxDelay),
                  flags(flags),
                  id(id) {}

        // Name string of the sensor.
        std::string name;
        // Vendor string of this sensor.
        std::string vendor;
        // Version of the sensor's module.
        int32_t version;
        // Generic type of this sensor.
        InputDeviceSensorType type;
        // The current accuracy of sensor event.
        InputDeviceSensorAccuracy accuracy;
        // Maximum range of the sensor in the sensor's unit.
        float maxRange;
        // Resolution of the sensor in the sensor's unit.
        float resolution;
        // The power in mA used by this sensor while in use.
        float power;
        // The minimum delay allowed between two events in microsecond or zero if this sensor only
        // returns a value when the data it's measuring changes.
        int32_t minDelay;
        // Number of events reserved for this sensor in the batch mode FIFO.
        int32_t fifoReservedEventCount;
        // Maximum number of events of this sensor that could be batched.
        int32_t fifoMaxEventCount;
        // The type of this sensor as a string.
        std::string stringType;
        // The delay between two sensor events corresponding to the lowest frequency that this sensor
        // supports.
        int32_t maxDelay;
        // Sensor flags
        int32_t flags;
        // Sensor id, same as the input device ID it belongs to.
        int32_t id;
    };

    struct BrightnessLevel : ftl::DefaultConstructible<BrightnessLevel, std::uint8_t>,
                             ftl::Equatable<BrightnessLevel>,
                             ftl::Orderable<BrightnessLevel>,
                             ftl::Addable<BrightnessLevel> {
        using DefaultConstructible::DefaultConstructible;
    };

    struct InputDeviceLightInfo {
        explicit InputDeviceLightInfo(std::string name, int32_t id, InputDeviceLightType type,
                                      ftl::Flags<InputDeviceLightCapability> capabilityFlags,
                                      int32_t ordinal,
                                      std::set<BrightnessLevel> preferredBrightnessLevels)
                : name(name),
                  id(id),
                  type(type),
                  capabilityFlags(capabilityFlags),
                  ordinal(ordinal),
                  preferredBrightnessLevels(std::move(preferredBrightnessLevels)) {}

        // Name string of the light.
        std::string name;
        // Light id
        int32_t id;
        // Type of the light.
        InputDeviceLightType type;
        // Light capabilities.
        ftl::Flags<InputDeviceLightCapability> capabilityFlags;
        // Ordinal of the light
        int32_t ordinal;
        // Custom brightness levels for the light
        std::set<BrightnessLevel> preferredBrightnessLevels;
    };

    struct InputDeviceBatteryInfo {
        explicit InputDeviceBatteryInfo(std::string name, int32_t id) : name(name), id(id) {}

        // Name string of the battery.
        std::string name;
        // Battery id
        int32_t id;
    };

    struct KeyboardLayoutInfo {
        explicit KeyboardLayoutInfo(std::string languageTag, std::string layoutType)
                : languageTag(languageTag), layoutType(layoutType) {}

        // A BCP 47 conformant language tag such as "en-US".
        std::string languageTag;
        // The layout type such as QWERTY or AZERTY.
        std::string layoutType;

        inline bool operator==(const KeyboardLayoutInfo &other) const {
            return languageTag == other.languageTag && layoutType == other.layoutType;
        }

        inline bool operator!=(const KeyboardLayoutInfo &other) const { return !(*this == other); }
    };

// The version of the Universal Stylus Initiative (USI) protocol supported by the input device.
    struct InputDeviceUsiVersion {
        int32_t majorVersion = -1;
        int32_t minorVersion = -1;
    };

/*
 * Describes the characteristics and capabilities of an input device.
 */
    class InputDeviceInfo {
    public:
        InputDeviceInfo();

        InputDeviceInfo(const InputDeviceInfo &other);

        InputDeviceInfo &operator=(const InputDeviceInfo &other);

        ~InputDeviceInfo();

        struct MotionRange {
            int32_t axis;
            uint32_t source;
            float min;
            float max;
            float flat;
            float fuzz;
            float resolution;
        };

        void initialize(int32_t id, int32_t generation, int32_t controllerNumber,
                        const InputDeviceIdentifier &identifier, const std::string &alias,
                        bool isExternal, bool hasMic, ui::LogicalDisplayId associatedDisplayId,
                        InputDeviceViewBehavior viewBehavior = {{}}, bool enabled = true);

        inline int32_t getId() const { return mId; }

        inline int32_t getControllerNumber() const { return mControllerNumber; }

        inline int32_t getGeneration() const { return mGeneration; }

        inline const InputDeviceIdentifier &getIdentifier() const { return mIdentifier; }

        inline const std::string &getAlias() const { return mAlias; }

        inline const std::string &getDisplayName() const {
            return mAlias.empty() ? mIdentifier.name : mAlias;
        }

        inline bool isExternal() const { return mIsExternal; }

        inline bool hasMic() const { return mHasMic; }

        inline uint32_t getSources() const { return mSources; }

        const MotionRange *getMotionRange(int32_t axis, uint32_t source) const;

        void addSource(uint32_t source);

        void addMotionRange(int32_t axis, uint32_t source,
                            float min, float max, float flat, float fuzz, float resolution);

        void addMotionRange(const MotionRange &range);

        void addSensorInfo(const InputDeviceSensorInfo &info);

        void addBatteryInfo(const InputDeviceBatteryInfo &info);

        void addLightInfo(const InputDeviceLightInfo &info);

        void setKeyboardType(int32_t keyboardType);

        inline int32_t getKeyboardType() const { return mKeyboardType; }

        void setKeyboardLayoutInfo(KeyboardLayoutInfo keyboardLayoutInfo);

        inline const std::optional<KeyboardLayoutInfo> &getKeyboardLayoutInfo() const {
            return mKeyboardLayoutInfo;
        }

        inline const InputDeviceViewBehavior &getViewBehavior() const { return mViewBehavior; }

        inline void setVibrator(bool hasVibrator) { mHasVibrator = hasVibrator; }

        inline bool hasVibrator() const { return mHasVibrator; }

        inline void setHasBattery(bool hasBattery) { mHasBattery = hasBattery; }

        inline bool hasBattery() const { return mHasBattery; }

        inline void setButtonUnderPad(bool hasButton) { mHasButtonUnderPad = hasButton; }

        inline bool hasButtonUnderPad() const { return mHasButtonUnderPad; }

        inline void setHasSensor(bool hasSensor) { mHasSensor = hasSensor; }

        inline bool hasSensor() const { return mHasSensor; }

        inline const std::vector<MotionRange> &getMotionRanges() const {
            return mMotionRanges;
        }

        std::vector<InputDeviceSensorInfo> getSensors();

        std::vector<InputDeviceLightInfo> getLights();

        inline void setUsiVersion(std::optional<InputDeviceUsiVersion> usiVersion) {
            mUsiVersion = std::move(usiVersion);
        }

        inline std::optional<InputDeviceUsiVersion> getUsiVersion() const { return mUsiVersion; }

        inline ui::LogicalDisplayId getAssociatedDisplayId() const { return mAssociatedDisplayId; }

        inline void setEnabled(bool enabled) { mEnabled = enabled; }

        inline bool isEnabled() const { return mEnabled; }

    private:
        int32_t mId;
        int32_t mGeneration;
        int32_t mControllerNumber;
        InputDeviceIdentifier mIdentifier;
        std::string mAlias;
        bool mIsExternal;
        bool mHasMic;
        std::optional<KeyboardLayoutInfo> mKeyboardLayoutInfo;
        uint32_t mSources;
        int32_t mKeyboardType;
        std::optional<InputDeviceUsiVersion> mUsiVersion;
        ui::LogicalDisplayId mAssociatedDisplayId{ui::LogicalDisplayId::INVALID};
        bool mEnabled;

        bool mHasVibrator;
        bool mHasBattery;
        bool mHasButtonUnderPad;
        bool mHasSensor;

        std::vector<MotionRange> mMotionRanges;
        std::unordered_map<InputDeviceSensorType, InputDeviceSensorInfo> mSensors;
        /* Map from light ID to light info */
        std::unordered_map<int32_t, InputDeviceLightInfo> mLights;
        /* Map from battery ID to battery info */
        std::unordered_map<int32_t, InputDeviceBatteryInfo> mBatteries;
        /** The View related behaviors for the device. */
        InputDeviceViewBehavior mViewBehavior;
    };

/* Types of input device configuration files. */
    enum class InputDeviceConfigurationFileType : int32_t {
        CONFIGURATION = 0,     /* .idc file */
        KEY_LAYOUT = 1,        /* .kl file */
        KEY_CHARACTER_MAP = 2, /* .kcm file */
        ftl_last = KEY_CHARACTER_MAP,
    };

/*
 * Gets the path of an input device configuration file, if one is available.
 * Considers both system provided and user installed configuration files.
 * The optional suffix is appended to the end of the file name (before the
 * extension).
 *
 * The device identifier is used to construct several default configuration file
 * names to try based on the device name, vendor, product, and version.
 *
 * Returns an empty string if not found.
 */
    extern std::string getInputDeviceConfigurationFilePathByDeviceIdentifier(
            const InputDeviceIdentifier &deviceIdentifier, InputDeviceConfigurationFileType type,
            const char *suffix = "");

/*
 * Gets the path of an input device configuration file, if one is available.
 * Considers both system provided and user installed configuration files.
 *
 * The name is case-sensitive and is used to construct the filename to resolve.
 * All characters except 'a'-'z', 'A'-'Z', '0'-'9', '-', and '_' are replaced by underscores.
 *
 * Returns an empty string if not found.
 */
    extern std::string getInputDeviceConfigurationFilePathByName(
            const std::string &name, InputDeviceConfigurationFileType type);

    enum ReservedInputDeviceId : int32_t {
        // Device id representing an invalid device
        INVALID_INPUT_DEVICE_ID = -2,
        // Device id of a special "virtual" keyboard that is always present.
        VIRTUAL_KEYBOARD_ID = -1,
        // Device id of the "built-in" keyboard if there is one.
        BUILT_IN_KEYBOARD_ID = 0,
        // First device id available for dynamic devices
        END_RESERVED_ID = 1,
    };

} // namespace android
