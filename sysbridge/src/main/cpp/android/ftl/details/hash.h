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
#include <cstring>

namespace android::ftl::details {

// Based on CityHash64 v1.0.1 (http://code.google.com/p/cityhash/), but slightly
// modernized and trimmed for cases with bounded lengths.

    template<typename T = std::uint64_t>
    inline T read_unaligned(const void *ptr) {
        T v;
        std::memcpy(&v, ptr, sizeof(T));
        return v;
    }

    template<bool NonZeroShift = false>
    constexpr std::uint64_t rotate(std::uint64_t v, std::uint8_t shift) {
        if constexpr (!NonZeroShift) {
            if (shift == 0) return v;
        }
        return (v >> shift) | (v << (64 - shift));
    }

    constexpr std::uint64_t shift_mix(std::uint64_t v) {
        return v ^ (v >> 47);
    }

    __attribute__((no_sanitize("unsigned-integer-overflow")))
    constexpr std::uint64_t hash_length_16(std::uint64_t u, std::uint64_t v) {
        constexpr std::uint64_t kPrime = 0x9ddfea08eb382d69ull;
        auto a = (u ^ v) * kPrime;
        a ^= (a >> 47);
        auto b = (v ^ a) * kPrime;
        b ^= (b >> 47);
        b *= kPrime;
        return b;
    }

    constexpr std::uint64_t kPrime0 = 0xc3a5c85c97cb3127ull;
    constexpr std::uint64_t kPrime1 = 0xb492b66fbe98f273ull;
    constexpr std::uint64_t kPrime2 = 0x9ae16a3b2f90404full;
    constexpr std::uint64_t kPrime3 = 0xc949d7c7509e6557ull;

    __attribute__((no_sanitize("unsigned-integer-overflow")))
    inline std::uint64_t hash_length_0_to_16(const char *str, std::uint64_t length) {
        if (length > 8) {
            const auto a = read_unaligned(str);
            const auto b = read_unaligned(str + length - 8);
            return hash_length_16(a, rotate<true>(b + length, static_cast<std::uint8_t>(length))) ^
                   b;
        }
        if (length >= 4) {
            const auto a = read_unaligned<std::uint32_t>(str);
            const auto b = read_unaligned<std::uint32_t>(str + length - 4);
            return hash_length_16(length + (a << 3), b);
        }
        if (length > 0) {
            const auto a = static_cast<unsigned char>(str[0]);
            const auto b = static_cast<unsigned char>(str[length >> 1]);
            const auto c = static_cast<unsigned char>(str[length - 1]);
            const auto y = static_cast<std::uint32_t>(a) + (static_cast<std::uint32_t>(b) << 8);
            const auto z =
                    static_cast<std::uint32_t>(length) + (static_cast<std::uint32_t>(c) << 2);
            return shift_mix(y * kPrime2 ^ z * kPrime3) * kPrime2;
        }
        return kPrime2;
    }

    __attribute__((no_sanitize("unsigned-integer-overflow")))
    inline std::uint64_t hash_length_17_to_32(const char *str, std::uint64_t length) {
        const auto a = read_unaligned(str) * kPrime1;
        const auto b = read_unaligned(str + 8);
        const auto c = read_unaligned(str + length - 8) * kPrime2;
        const auto d = read_unaligned(str + length - 16) * kPrime0;
        return hash_length_16(rotate(a - b, 43) + rotate(c, 30) + d,
                              a + rotate(b ^ kPrime3, 20) - c + length);
    }

    __attribute__((no_sanitize("unsigned-integer-overflow")))
    inline std::uint64_t hash_length_33_to_64(const char *str, std::uint64_t length) {
        auto z = read_unaligned(str + 24);
        auto a = read_unaligned(str) + (length + read_unaligned(str + length - 16)) * kPrime0;
        auto b = rotate(a + z, 52);
        auto c = rotate(a, 37);

        a += read_unaligned(str + 8);
        c += rotate(a, 7);
        a += read_unaligned(str + 16);

        const auto vf = a + z;
        const auto vs = b + rotate(a, 31) + c;

        a = read_unaligned(str + 16) + read_unaligned(str + length - 32);
        z += read_unaligned(str + length - 8);
        b = rotate(a + z, 52);
        c = rotate(a, 37);
        a += read_unaligned(str + length - 24);
        c += rotate(a, 7);
        a += read_unaligned(str + length - 16);

        const auto wf = a + z;
        const auto ws = b + rotate(a, 31) + c;
        const auto r = shift_mix((vf + ws) * kPrime2 + (wf + vs) * kPrime0);
        return shift_mix(r * kPrime0 + vs) * kPrime2;
    }

}  // namespace android::ftl::details
