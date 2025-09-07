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

#include <limits>
#include <type_traits>

#include <ftl/details/cast.h>

namespace android::ftl {

    enum class CastSafety {
        kSafe, kUnderflow, kOverflow
    };

// Returns whether static_cast<R>(v) is safe, or would result in underflow or overflow.
//
//   static_assert(ftl::cast_safety<uint8_t>(-1) == ftl::CastSafety::kUnderflow);
//   static_assert(ftl::cast_safety<int8_t>(128u) == ftl::CastSafety::kOverflow);
//
//   static_assert(ftl::cast_safety<uint32_t>(-.1f) == ftl::CastSafety::kUnderflow);
//   static_assert(ftl::cast_safety<int32_t>(static_cast<float>(INT32_MAX)) ==
//                                           ftl::CastSafety::kOverflow);
//
//   static_assert(ftl::cast_safety<float>(-DBL_MAX) == ftl::CastSafety::kUnderflow);
//
    template<typename R, typename T>
    constexpr CastSafety cast_safety(T v) {
        static_assert(std::is_arithmetic_v<T>);
        static_assert(std::is_arithmetic_v<R>);

        constexpr bool kFromSigned = std::is_signed_v<T>;
        constexpr bool kToSigned = std::is_signed_v<R>;

        using details::max_exponent;

        // If the R range contains the T range, then casting is always safe.
        if constexpr ((kFromSigned == kToSigned && max_exponent<R> >= max_exponent<T>) ||
                      (!kFromSigned && kToSigned && max_exponent<R> > max_exponent<T>)) {
            return CastSafety::kSafe;
        }

        using C = std::common_type_t<R, T>;

        if constexpr (kFromSigned) {
            using L = details::safe_limits<R, T>;

            if constexpr (kToSigned) {
                // Signed to signed.
                if (v < L::lowest()) return CastSafety::kUnderflow;
                return v <= L::max() ? CastSafety::kSafe : CastSafety::kOverflow;
            } else {
                // Signed to unsigned.
                if (v < 0) return CastSafety::kUnderflow;
                return static_cast<C>(v) <= static_cast<C>(L::max()) ? CastSafety::kSafe
                                                                     : CastSafety::kOverflow;
            }
        } else {
            using L = std::numeric_limits<R>;

            if constexpr (kToSigned) {
                // Unsigned to signed.
                return static_cast<C>(v) <= static_cast<C>(L::max()) ? CastSafety::kSafe
                                                                     : CastSafety::kOverflow;
            } else {
                // Unsigned to unsigned.
                return v <= L::max() ? CastSafety::kSafe : CastSafety::kOverflow;
            }
        }
    }

}  // namespace android::ftl
