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

#include <ftl/details/concat.h>

namespace android::ftl {

// Lightweight (not allocating nor sprintf-based) concatenation. The variadic arguments can be
// values of integral type (including bool and char), string literals, or strings whose length
// is constrained:
//
//   std::string_view name = "Volume";
//   ftl::Concat string(ftl::truncated<3>(name), ": ", -3, " dB");
//
//   assert(string.str() == "Vol: -3 dB");
//   assert(string.c_str()[string.size()] == '\0');
//
    template<std::size_t, typename... Ts>
    struct Concat;

    template<std::size_t N, typename T, typename... Ts>
    struct Concat<N, T, Ts...> : Concat<N + details::StaticString<T>::N, Ts...> {
        explicit constexpr Concat(T v, Ts... args) { append(v, args...); }

    protected:
        constexpr Concat() = default;

        constexpr void append(T v, Ts... args) {
            using Str = details::StaticString<T>;
            const Str str(v);

            // TODO: Replace with constexpr std::copy in C++20.
            for (auto it = str.view.begin(); it != str.view.end();) {
                *this->end_++ = *it++;
            }

            using Base = Concat<N + Str::N, Ts...>;
            this->Base::append(args...);
        }
    };

    template<std::size_t N>
    struct Concat<N> {
        static constexpr std::size_t max_size() { return N; }

        constexpr std::size_t size() const { return static_cast<std::size_t>(end_ - buffer_); }

        constexpr const char *c_str() const { return buffer_; }

        constexpr std::string_view str() const {
            // TODO: Replace with {buffer_, end_} in C++20.
            return {buffer_, size()};
        }

    protected:
        constexpr Concat() : end_(buffer_) {}

        constexpr Concat(const Concat &) = delete;

        constexpr void append() { *end_ = '\0'; }

        char buffer_[N + 1];
        char *end_;
    };

// Deduction guide.
    template<typename... Ts>
    Concat(Ts &&...) -> Concat<0, Ts...>;

    template<std::size_t N>
    constexpr auto truncated(std::string_view v) {
        return details::Truncated<N>{v};
    }

}  // namespace android::ftl
