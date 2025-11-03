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

#include <cinttypes>
#include <optional>
#include <string_view>

#include <ftl/details/hash.h>

namespace android::ftl {

// Non-cryptographic hash function (namely CityHash64) for strings with at most 64 characters.
// Unlike std::hash, which returns std::size_t and is only required to produce the same result
// for the same input within a single execution of a program, this hash is stable.
    inline std::optional<std::uint64_t> stable_hash(std::string_view view) {
        const auto length = view.length();
        if (length <= 16) {
            return details::hash_length_0_to_16(view.data(), length);
        }
        if (length <= 32) {
            return details::hash_length_17_to_32(view.data(), length);
        }
        if (length <= 64) {
            return details::hash_length_33_to_64(view.data(), length);
        }
        return {};
    }

}  // namespace android::ftl
