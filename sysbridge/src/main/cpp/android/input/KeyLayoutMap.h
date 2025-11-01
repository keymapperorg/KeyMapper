/*
 * Copyright (C) 2008 The Android Open Source Project
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

#include <cstdint>
#include <set>
#include "../utils/Tokenizer.h"
#include "../libbase/result.h"
#include <unordered_map>

namespace android {

    struct AxisInfo {
        enum Mode {
            // Axis value is reported directly.
            MODE_NORMAL = 0,
            // Axis value should be inverted before reporting.
            MODE_INVERT = 1,
            // Axis value should be split into two axes
            MODE_SPLIT = 2,
        };

        // Axis mode.
        Mode mode;

        // Axis id.
        // When split, this is the axis used for values smaller than the split position.
        int32_t axis;

        // When split, this is the axis used for values after higher than the split position.
        int32_t highAxis;

        // The split value, or 0 if not split.
        int32_t splitValue;

        // The flat value, or -1 if none.
        int32_t flatOverride;

        AxisInfo() : mode(MODE_NORMAL), axis(-1), highAxis(-1), splitValue(0), flatOverride(-1) {
        }
    };

/**
 * Describes a mapping from keyboard scan codes and joystick axes to Android key codes and axes.
 *
 * This object is immutable after it has been loaded.
 */
    class KeyLayoutMap {
    public:
        static base::Result<std::shared_ptr<KeyLayoutMap>> load(const std::string &filename,
                                                                const char *contents = nullptr);

        static base::Result<std::shared_ptr<KeyLayoutMap>> loadContents(const std::string &filename,
                                                                        const char *contents);

        status_t mapKey(int32_t scanCode, int32_t usageCode,
                        int32_t *outKeyCode, uint32_t *outFlags) const;

        std::vector<int32_t> findScanCodesForKey(int32_t keyCode) const;

        std::optional<int32_t> findScanCodeForLed(int32_t ledCode) const;

        std::vector<int32_t> findUsageCodesForKey(int32_t keyCode) const;

        std::optional<int32_t> findUsageCodeForLed(int32_t ledCode) const;

        std::optional<AxisInfo> mapAxis(int32_t scanCode) const;

        virtual ~KeyLayoutMap();

    private:
        static base::Result<std::shared_ptr<KeyLayoutMap>> load(Tokenizer *tokenizer);

        struct Key {
            int32_t keyCode;
            uint32_t flags;
        };

        std::unordered_map<int32_t, Key> mKeysByScanCode;
        std::unordered_map<int32_t, Key> mKeysByUsageCode;
        std::unordered_map<int32_t, AxisInfo> mAxes;
        std::set<std::string> mRequiredKernelConfigs;
        std::string mLoadFileName;

        KeyLayoutMap();

        const Key *getKey(int32_t scanCode, int32_t usageCode) const;

        class Parser {
            KeyLayoutMap *mMap;
            Tokenizer *mTokenizer;

        public:
            Parser(KeyLayoutMap *map, Tokenizer *tokenizer);

            ~Parser();

            status_t parse();

        private:
            status_t parseKey();

            status_t parseAxis();

            status_t parseRequiredKernelConfig();
        };
    };

} // namespace android
