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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, Tokenizer.cppeither express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "../../logging.h"
#include <android/keycodes.h>
#include "../utils/String8.h"
#include "KeyLayoutMap.h"
#include "../utils/Tokenizer.h"
#include "InputEventLabels.h"
#include <unordered_map>

#include <cstdlib>
#include <string_view>
#include <unordered_map>
#include "../libbase/result.h"
#include "../liblog/log_main.h"
#include "Input.h"

#define DEBUG_MAPPING false
#define DEBUG_PARSER false

// Enables debug output for parser performance.
#define DEBUG_PARSER_PERFORMANCE 0

namespace android {

    namespace {

        std::optional<int> parseInt(const char *str) {
            char *end;
            errno = 0;
            const int value = strtol(str, &end, 0);
            if (end == str) {
                LOGE("Could not parse %s", str);
                return {};
            }
            if (errno == ERANGE) {
                LOGE("Out of bounds: %s", str);
                return {};
            }
            return value;
        }

        constexpr const char *WHITESPACE = " \t\r";

    } // namespace

    KeyLayoutMap::KeyLayoutMap() = default;

    KeyLayoutMap::~KeyLayoutMap() = default;

    base::Result<std::shared_ptr<KeyLayoutMap>>
    KeyLayoutMap::loadContents(const std::string &filename,
                               const char *contents) {
        return load(filename, contents);
    }

    base::Result<std::shared_ptr<KeyLayoutMap>> KeyLayoutMap::load(const std::string &filename,
                                                                   const char *contents) {
        Tokenizer *tokenizer;
        status_t status;
        if (contents == nullptr) {
            status = Tokenizer::open(String8(filename.c_str()), &tokenizer);
        } else {
            status = Tokenizer::fromContents(String8(filename.c_str()), contents, &tokenizer);
        }

        if (status) {
            LOGE("Error %d opening key layout map file %s.", status, filename.c_str());
            return Errorf("Error {} opening key layout map file {}.", status, filename.c_str());
        }
        std::unique_ptr<Tokenizer> t(tokenizer);
        auto ret = load(t.get());
        if (!ret.ok()) {
            return ret;
        }
        const std::shared_ptr<KeyLayoutMap> &map = *ret;
        LOG_ALWAYS_FATAL_IF(map == nullptr, "Returned map should not be null if there's no error");

        map->mLoadFileName = filename;
        return ret;
    }

    base::Result<std::shared_ptr<KeyLayoutMap>> KeyLayoutMap::load(Tokenizer *tokenizer) {
        std::shared_ptr<KeyLayoutMap> map = std::shared_ptr<KeyLayoutMap>(new KeyLayoutMap());
        status_t status = OK;
        if (!map.get()) {
            LOGE("Error allocating key layout map.");
            return Errorf("Error allocating key layout map.");
        } else {
#if DEBUG_PARSER_PERFORMANCE
            nsecs_t startTime = systemTime(SYSTEM_TIME_MONOTONIC);
#endif
            Parser parser(map.get(), tokenizer);
            status = parser.parse();
#if DEBUG_PARSER_PERFORMANCE
            nsecs_t elapsedTime = systemTime(SYSTEM_TIME_MONOTONIC) - startTime;
            LOGD("Parsed key layout map file '%s' %d lines in %0.3fms.",
                  tokenizer->getFilename().c_str(), tokenizer->getLineNumber(),
                  elapsedTime / 1000000.0);
#endif
            LOGE("PARSE STATUS = %d", status);
            if (!status) {
                return std::move(map);
            }
        }
        return Errorf("Load KeyLayoutMap failed {}.", status);
    }

    status_t KeyLayoutMap::mapKey(int32_t scanCode, int32_t usageCode,
                                  int32_t *outKeyCode, uint32_t *outFlags) const {
        const Key *key = getKey(scanCode, usageCode);
        if (!key) {
            ALOGD_IF(DEBUG_MAPPING, "mapKey: scanCode=%d, usageCode=0x%08x ~ Failed.", scanCode,
                     usageCode);
            *outKeyCode = AKEYCODE_UNKNOWN;
            *outFlags = 0;
            return NAME_NOT_FOUND;
        }

        *outKeyCode = key->keyCode;
        *outFlags = key->flags;

        ALOGD_IF(DEBUG_MAPPING,
                 "mapKey: scanCode=%d, usageCode=0x%08x ~ Result keyCode=%d, outFlags=0x%08x.",
                 scanCode, usageCode, *outKeyCode, *outFlags);
        return NO_ERROR;
    }

    const KeyLayoutMap::Key *KeyLayoutMap::getKey(int32_t scanCode, int32_t usageCode) const {
        if (usageCode) {
            auto it = mKeysByUsageCode.find(usageCode);
            if (it != mKeysByUsageCode.end()) {
                return &it->second;
            }
        }
        if (scanCode) {
            auto it = mKeysByScanCode.find(scanCode);
            if (it != mKeysByScanCode.end()) {
                return &it->second;
            }
        }
        return nullptr;
    }

    std::vector<int32_t> KeyLayoutMap::findScanCodesForKey(int32_t keyCode) const {
        std::vector<int32_t> scanCodes;
        // b/354333072: Only consider keys without FUNCTION flag
        for (const auto &[scanCode, key]: mKeysByScanCode) {
            if (keyCode == key.keyCode && !(key.flags & POLICY_FLAG_FUNCTION)) {
                scanCodes.push_back(scanCode);
            }
        }
        return scanCodes;
    }

    std::vector<int32_t> KeyLayoutMap::findUsageCodesForKey(int32_t keyCode) const {
        std::vector<int32_t> usageCodes;
        for (const auto &[usageCode, key]: mKeysByUsageCode) {
            if (keyCode == key.keyCode && !(key.flags & POLICY_FLAG_FALLBACK_USAGE_MAPPING)) {
                usageCodes.push_back(usageCode);
            }
        }
        return usageCodes;
    }

    std::optional<AxisInfo> KeyLayoutMap::mapAxis(int32_t scanCode) const {
        auto it = mAxes.find(scanCode);
        if (it == mAxes.end()) {
            ALOGD_IF(DEBUG_MAPPING, "mapAxis: scanCode=%d ~ Failed.", scanCode);
            return std::nullopt;
        }

        const AxisInfo &axisInfo = it->second;
        ALOGD_IF(DEBUG_MAPPING,
                 "mapAxis: scanCode=%d ~ Result mode=%d, axis=%d, highAxis=%d, "
                 "splitValue=%d, flatOverride=%d.",
                 scanCode, axisInfo.mode, axisInfo.axis, axisInfo.highAxis, axisInfo.splitValue,
                 axisInfo.flatOverride);
        return axisInfo;
    }

// --- KeyLayoutMap::Parser ---

    KeyLayoutMap::Parser::Parser(KeyLayoutMap *map, Tokenizer *tokenizer) :
            mMap(map), mTokenizer(tokenizer) {
    }

    KeyLayoutMap::Parser::~Parser() {
    }

    status_t KeyLayoutMap::Parser::parse() {
        while (!mTokenizer->isEof()) {
            ALOGD_IF(DEBUG_PARSER, "Parsing %s: '%s'.", mTokenizer->getLocation().c_str(),
                     mTokenizer->peekRemainderOfLine().c_str());

            mTokenizer->skipDelimiters(WHITESPACE);

            if (!mTokenizer->isEol() && mTokenizer->peekChar() != '#') {
                String8 keywordToken = mTokenizer->nextToken(WHITESPACE);
                if (keywordToken == "key") {
                    mTokenizer->skipDelimiters(WHITESPACE);
                    status_t status = parseKey();
                    if (status) return status;
                } else if (keywordToken == "axis") {
                    mTokenizer->skipDelimiters(WHITESPACE);
                    status_t status = parseAxis();
                    if (status) return status;
                } else if (keywordToken == "led") {
                    // Skip LEDs, we don't need them for Key Mapper
                    mTokenizer->nextLine();
                    continue;
                } else if (keywordToken == "sensor") {
                    // Skip Sensors, we don't need them for Key Mapper
                    mTokenizer->nextLine();
                    continue;
                } else if (keywordToken == "requires_kernel_config") {
                    mTokenizer->skipDelimiters(WHITESPACE);
                    status_t status = parseRequiredKernelConfig();
                    if (status) return status;
                } else {
                    LOGE("%s: Expected keyword, got '%s'.", mTokenizer->getLocation().c_str(),
                         keywordToken.c_str());
                    return BAD_VALUE;
                }

                mTokenizer->skipDelimiters(WHITESPACE);

                if (!mTokenizer->isEol() && mTokenizer->peekChar() != '#') {
                    LOGW("%s: Expected end of line or trailing comment, got '%s'.",
                         mTokenizer->getLocation().c_str(),
                         mTokenizer->peekRemainderOfLine().c_str());
                    return BAD_VALUE;
                }
            }

            mTokenizer->nextLine();
        }
        return NO_ERROR;
    }

    status_t KeyLayoutMap::Parser::parseKey() {
        String8 codeToken = mTokenizer->nextToken(WHITESPACE);
        bool mapUsage = false;
        if (codeToken == "usage") {
            mapUsage = true;
            mTokenizer->skipDelimiters(WHITESPACE);
            codeToken = mTokenizer->nextToken(WHITESPACE);
        }

        std::optional<int> code = parseInt(codeToken.c_str());
        if (!code) {
            LOGE("%s: Expected key %s number, got '%s'.", mTokenizer->getLocation().c_str(),
                 mapUsage ? "usage" : "scan code", codeToken.c_str());
            return BAD_VALUE;
        }
        std::unordered_map<int32_t, Key> &map =
                mapUsage ? mMap->mKeysByUsageCode : mMap->mKeysByScanCode;
        if (map.find(*code) != map.end()) {
            LOGE("%s: Duplicate entry for key %s '%s'.", mTokenizer->getLocation().c_str(),
                 mapUsage ? "usage" : "scan code", codeToken.c_str());
            return BAD_VALUE;
        }

        mTokenizer->skipDelimiters(WHITESPACE);
        String8 keyCodeToken = mTokenizer->nextToken(WHITESPACE);
        std::optional<int> keyCode = InputEventLookup::getKeyCodeByLabel(keyCodeToken.c_str());

        if (!keyCode) {
            LOGW("%s: Unknown key code label %s", mTokenizer->getLocation().c_str(),
                 keyCodeToken.c_str());
            // Do not crash at this point because there may be more flags afterwards that need parsing.
        }

        uint32_t flags = 0;
        for (;;) {
            mTokenizer->skipDelimiters(WHITESPACE);
            if (mTokenizer->isEol() || mTokenizer->peekChar() == '#') break;

            String8 flagToken = mTokenizer->nextToken(WHITESPACE);
            std::optional<int> flag = InputEventLookup::getKeyFlagByLabel(flagToken.c_str());
            if (!flag) {
                LOGE("%s: Expected key flag label, got '%s'.", mTokenizer->getLocation().c_str(),
                     flagToken.c_str());
                return BAD_VALUE;
            }
            if (flags & *flag) {
                LOGE("%s: Duplicate key flag '%s'.", mTokenizer->getLocation().c_str(),
                     flagToken.c_str());
                return BAD_VALUE;
            }
            flags |= *flag;
        }

        ALOGD_IF(DEBUG_PARSER, "Parsed key %s: code=%d, keyCode=%d, flags=0x%08x.",
                 mapUsage ? "usage" : "scan code", *code, *keyCode, flags);

        // The key code may be unknown so only insert a key if it is known.
        if (keyCode) {
            Key key;
            key.keyCode = *keyCode;
            key.flags = flags;
            map.insert({*code, key});
        }

        return NO_ERROR;
    }

    status_t KeyLayoutMap::Parser::parseAxis() {
        String8 scanCodeToken = mTokenizer->nextToken(WHITESPACE);
        std::optional<int> scanCode = parseInt(scanCodeToken.c_str());
        if (!scanCode) {
            LOGE("%s: Expected axis scan code number, got '%s'.", mTokenizer->getLocation().c_str(),
                 scanCodeToken.c_str());
            return BAD_VALUE;
        }
        if (mMap->mAxes.find(*scanCode) != mMap->mAxes.end()) {
            LOGE("%s: Duplicate entry for axis scan code '%s'.", mTokenizer->getLocation().c_str(),
                 scanCodeToken.c_str());
            return BAD_VALUE;
        }

        AxisInfo axisInfo;

        mTokenizer->skipDelimiters(WHITESPACE);
        String8 token = mTokenizer->nextToken(WHITESPACE);
        if (token == "invert") {
            axisInfo.mode = AxisInfo::MODE_INVERT;

            mTokenizer->skipDelimiters(WHITESPACE);
            String8 axisToken = mTokenizer->nextToken(WHITESPACE);
            std::optional<int> axis = InputEventLookup::getAxisByLabel(axisToken.c_str());
            if (!axis) {
                LOGE("%s: Expected inverted axis label, got '%s'.",
                     mTokenizer->getLocation().c_str(),
                     axisToken.c_str());
                return BAD_VALUE;
            }
            axisInfo.axis = *axis;
        } else if (token == "split") {
            axisInfo.mode = AxisInfo::MODE_SPLIT;

            mTokenizer->skipDelimiters(WHITESPACE);
            String8 splitToken = mTokenizer->nextToken(WHITESPACE);
            std::optional<int> splitValue = parseInt(splitToken.c_str());
            if (!splitValue) {
                LOGE("%s: Expected split value, got '%s'.", mTokenizer->getLocation().c_str(),
                     splitToken.c_str());
                return BAD_VALUE;
            }
            axisInfo.splitValue = *splitValue;

            mTokenizer->skipDelimiters(WHITESPACE);
            String8 lowAxisToken = mTokenizer->nextToken(WHITESPACE);
            std::optional<int> axis = InputEventLookup::getAxisByLabel(lowAxisToken.c_str());
            if (!axis) {
                LOGE("%s: Expected low axis label, got '%s'.", mTokenizer->getLocation().c_str(),
                     lowAxisToken.c_str());
                return BAD_VALUE;
            }
            axisInfo.axis = *axis;

            mTokenizer->skipDelimiters(WHITESPACE);
            String8 highAxisToken = mTokenizer->nextToken(WHITESPACE);
            std::optional<int> highAxis = InputEventLookup::getAxisByLabel(highAxisToken.c_str());
            if (!highAxis) {
                LOGE("%s: Expected high axis label, got '%s'.", mTokenizer->getLocation().c_str(),
                     highAxisToken.c_str());
                return BAD_VALUE;
            }
            axisInfo.highAxis = *highAxis;
        } else {
            std::optional<int> axis = InputEventLookup::getAxisByLabel(token.c_str());
            if (!axis) {
                LOGE("%s: Expected axis label, 'split' or 'invert', got '%s'.",
                     mTokenizer->getLocation().c_str(), token.c_str());
                return BAD_VALUE;
            }
            axisInfo.axis = *axis;
        }

        for (;;) {
            mTokenizer->skipDelimiters(WHITESPACE);
            if (mTokenizer->isEol() || mTokenizer->peekChar() == '#') {
                break;
            }
            String8 keywordToken = mTokenizer->nextToken(WHITESPACE);
            if (keywordToken == "flat") {
                mTokenizer->skipDelimiters(WHITESPACE);
                String8 flatToken = mTokenizer->nextToken(WHITESPACE);
                std::optional<int> flatOverride = parseInt(flatToken.c_str());
                if (!flatOverride) {
                    LOGE("%s: Expected flat value, got '%s'.", mTokenizer->getLocation().c_str(),
                         flatToken.c_str());
                    return BAD_VALUE;
                }
                axisInfo.flatOverride = *flatOverride;
            } else {
                LOGE("%s: Expected keyword 'flat', got '%s'.", mTokenizer->getLocation().c_str(),
                     keywordToken.c_str());
                return BAD_VALUE;
            }
        }

        ALOGD_IF(DEBUG_PARSER,
                 "Parsed axis: scanCode=%d, mode=%d, axis=%d, highAxis=%d, "
                 "splitValue=%d, flatOverride=%d.",
                 *scanCode, axisInfo.mode, axisInfo.axis, axisInfo.highAxis, axisInfo.splitValue,
                 axisInfo.flatOverride);
        mMap->mAxes.insert({*scanCode, axisInfo});
        return NO_ERROR;
    }

// Parse the name of a required kernel config.
// The layout won't be used if the specified kernel config is not present
// Examples:
// requires_kernel_config CONFIG_HID_PLAYSTATION
    status_t KeyLayoutMap::Parser::parseRequiredKernelConfig() {
        String8 codeToken = mTokenizer->nextToken(WHITESPACE);
        std::string configName = codeToken.c_str();

        const auto result = mMap->mRequiredKernelConfigs.emplace(configName);
        if (!result.second) {
            LOGE("%s: Duplicate entry for required kernel config %s.",
                 mTokenizer->getLocation().c_str(), configName.c_str());
            return BAD_VALUE;
        }

//        ALOGD_IF(DEBUG_PARSER, "Parsed required kernel config: name=%s", configName.c_str());
        return NO_ERROR;
    }
} // namespace android
