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

namespace android::ftl::details {

// Exponent whose power of 2 is the (exclusive) upper bound of T.
    template<typename T, typename L = std::numeric_limits<T>>
    constexpr int max_exponent = std::is_floating_point_v<T> ? L::max_exponent : L::digits;

// Extension of std::numeric_limits<T> that reduces the maximum for integral types T such that it
// has an exact representation for floating-point types F. For example, the maximum int32_t value
// is 2'147'483'647, but casting it to float commonly rounds up to 2'147'483'650.f, which cannot
// be safely converted back lest the signed overflow invokes undefined behavior. This pitfall is
// avoided by clearing the lower (31 - 24 =) 7 bits of precision to 2'147'483'520. Note that the
// minimum is representable.
    template<typename T, typename F>
    struct safe_limits : std::numeric_limits<T> {
        static constexpr T max() {
            using Base = std::numeric_limits<T>;

            if constexpr (std::is_integral_v<T> && std::is_floating_point_v<F>) {
                // Assume the mantissa is 24 bits for float, or 53 bits for double.
                using Float = std::numeric_limits<F>;
                static_assert(Float::is_iec559);

                // If the integer is wider than the mantissa, clear the excess bits of precision.
                constexpr int kShift = Base::digits - Float::digits;
                if constexpr (kShift > 0) {
                    using U = std::make_unsigned_t<T>;
                    constexpr U kOne = static_cast<U>(1);
                    return static_cast<U>(Base::max()) & ~((kOne << kShift) - kOne);
                }
            }

            return Base::max();
        }
    };

}  // namespace android::ftl::details
