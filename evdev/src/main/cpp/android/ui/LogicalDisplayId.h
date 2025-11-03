/*
 * Copyright 2024 The Android Open Source Project
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

#include <ftl/mixins.h>
#include <sys/types.h>
#include <string>

#include <ostream>

namespace android::ui {

// Type-safe wrapper for a logical display id.
    struct LogicalDisplayId : ftl::Constructible<LogicalDisplayId, int32_t>,
                              ftl::Equatable<LogicalDisplayId>,
                              ftl::Orderable<LogicalDisplayId> {
        using Constructible::Constructible;

        constexpr auto val() const { return ftl::to_underlying(*this); }

        constexpr bool isValid() const { return val() >= 0; }

        std::string toString() const { return std::to_string(val()); }

        static const LogicalDisplayId INVALID;
        static const LogicalDisplayId DEFAULT;
    };

    constexpr inline LogicalDisplayId LogicalDisplayId::INVALID{-1};
    constexpr inline LogicalDisplayId LogicalDisplayId::DEFAULT{0};

    inline std::ostream &operator<<(std::ostream &stream, LogicalDisplayId displayId) {
        return stream << displayId.val();
    }

} // namespace android::ui

namespace std {
    template<>
    struct hash<android::ui::LogicalDisplayId> {
        size_t operator()(const android::ui::LogicalDisplayId &displayId) const {
            return hash<int32_t>()(displayId.val());
        }
    };
} // namespace std