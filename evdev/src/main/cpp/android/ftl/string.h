/*
 * Copyright 2021 The Android Open Source Project
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

#include <cassert>
#include <charconv>
#include <limits>
#include <string>
#include <string_view>
#include <type_traits>

namespace android::ftl {

    enum class Radix {
        kBin = 2, kDec = 10, kHex = 16
    };

    template<typename T>
    struct to_chars_length {
        static_assert(std::is_integral_v<T>);
        // Maximum binary digits, plus minus sign and radix prefix.
        static constexpr std::size_t value =
                std::numeric_limits<std::make_unsigned_t<T>>::digits + 3;
    };

    template<typename T>
    constexpr std::size_t to_chars_length_v = to_chars_length<T>::value;

    template<typename T = std::int64_t>
    using to_chars_buffer_t = char[to_chars_length_v<T>];

// Lightweight (not allocating nor sprintf-based) alternative to std::to_string for integers, with
// optional radix. See also ftl::to_string below.
//
//   ftl::to_chars_buffer_t<> buffer;
//
//   assert(ftl::to_chars(buffer, 123u) == "123");
//   assert(ftl::to_chars(buffer, -42, ftl::Radix::kBin) == "-0b101010");
//   assert(ftl::to_chars(buffer, 0xcafe, ftl::Radix::kHex) == "0xcafe");
//   assert(ftl::to_chars(buffer, '*', ftl::Radix::kHex) == "0x2a");
//
    template<typename T, std::size_t N>
    std::string_view to_chars(char (&buffer)[N], T v, Radix radix = Radix::kDec) {
        static_assert(N >= to_chars_length_v<T>);

        auto begin = buffer + 2;
        const auto [end, err] = std::to_chars(begin, buffer + N, v, static_cast<int>(radix));
        assert(err == std::errc());

        if (radix == Radix::kDec) {
            // TODO: Replace with {begin, end} in C++20.
            return {begin, static_cast<std::size_t>(end - begin)};
        }

        const auto prefix = radix == Radix::kBin ? 'b' : 'x';
        if constexpr (std::is_unsigned_v<T>) {
            buffer[0] = '0';
            buffer[1] = prefix;
        } else {
            if (*begin == '-') {
                *buffer = '-';
            } else {
                --begin;
            }

            *begin-- = prefix;
            *begin = '0';
        }

        // TODO: Replace with {buffer, end} in C++20.
        return {buffer, static_cast<std::size_t>(end - buffer)};
    }

// Lightweight (not sprintf-based) alternative to std::to_string for integers, with optional radix.
//
//   assert(ftl::to_string(123u) == "123");
//   assert(ftl::to_string(-42, ftl::Radix::kBin) == "-0b101010");
//   assert(ftl::to_string(0xcafe, ftl::Radix::kHex) == "0xcafe");
//   assert(ftl::to_string('*', ftl::Radix::kHex) == "0x2a");
//
    template<typename T>
    inline std::string to_string(T v, Radix radix = Radix::kDec) {
        to_chars_buffer_t<T> buffer;
        return std::string(to_chars(buffer, v, radix));
    }

    std::string to_string(bool) = delete;

    std::string to_string(bool, Radix) = delete;

}  // namespace android::ftl
