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

#include <functional>
#include <string_view>

#include <ftl/details/type_traits.h>
#include <ftl/string.h>

namespace android::ftl::details {

    template<typename T, typename = void>
    struct StaticString;

// Booleans.
    template<typename T>
    struct StaticString<T, std::enable_if_t<is_bool_v<T>>> {
        static constexpr std::size_t N = 5;  // Length of "false".

        explicit constexpr StaticString(bool b) : view(b ? "true" : "false") {}

        const std::string_view view;
    };

// Characters.
    template<typename T>
    struct StaticString<T, std::enable_if_t<is_char_v<T>>> {
        static constexpr std::size_t N = 1;

        explicit constexpr StaticString(char c) : character(c) {}

        const char character;
        const std::string_view view{&character, 1u};
    };

// Integers, including the integer value of other character types like char32_t.
    template<typename T>
    struct StaticString<
            T, std::enable_if_t<
                    std::is_integral_v<remove_cvref_t<T>> && !is_bool_v<T> && !is_char_v<T>>> {
        using U = remove_cvref_t<T>;
        static constexpr std::size_t N = to_chars_length_v<U>;

        // TODO: Mark this and to_chars as `constexpr` in C++23.
        explicit StaticString(U v) : view(to_chars(buffer, v)) {}

        to_chars_buffer_t<U> buffer;
        const std::string_view view;
    };

// Character arrays.
    template<std::size_t M>
    struct StaticString<const char (&)[M], void> {
        static constexpr std::size_t N = M - 1;

        explicit constexpr StaticString(const char (&str)[M]) : view(str, N) {}

        const std::string_view view;
    };

    template<std::size_t N>
    struct Truncated {
        std::string_view view;
    };

// Strings with constrained length.
    template<std::size_t M>
    struct StaticString<Truncated<M>, void> {
        static constexpr std::size_t N = M;

        explicit constexpr StaticString(Truncated<M> str) : view(str.view.substr(0, N)) {}

        const std::string_view view;
    };

}  // namespace android::ftl::details
