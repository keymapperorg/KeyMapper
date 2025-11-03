/*
 * Copyright 2022 The Android Open Source Project
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

#include <type_traits>
#include <variant>

namespace android::ftl::details {

    template<typename... Ms>
    struct Matcher : Ms ... {
        using Ms::operator()...;
    };

// Deduction guide.
    template<typename... Ms>
    Matcher(Ms...) -> Matcher<Ms...>;

    template<typename Matcher, typename... Ts>
    constexpr bool is_exhaustive_match_v = (std::is_invocable_v<Matcher, Ts> && ...);

    template<typename...>
    struct Match;

    template<typename T, typename U, typename... Ts>
    struct Match<T, U, Ts...> {
        template<typename Variant, typename Matcher>
        static decltype(auto) match(Variant &variant, const Matcher &matcher) {
            if (auto *const ptr = std::get_if<T>(&variant)) {
                return matcher(*ptr);
            } else {
                return Match<U, Ts...>::match(variant, matcher);
            }
        }
    };

    template<typename T>
    struct Match<T> {
        template<typename Variant, typename Matcher>
        static decltype(auto) match(Variant &variant, const Matcher &matcher) {
            return matcher(std::get<T>(variant));
        }
    };

}  // namespace android::ftl::details
