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

#include <utility>
#include <variant>

#include <ftl/details/match.h>

namespace android::ftl {

// Concise alternative to std::visit that compiles to branches rather than a dispatch table. For
// std::variant<T0, ..., TN> where N is small, this is slightly faster since the branches can be
// inlined unlike the function pointers.
//
//   using namespace std::chrono;
//   std::variant<seconds, minutes, hours> duration = 119min;
//
//   // Mutable match.
//   ftl::match(duration, [](auto& d) { ++d; });
//
//   // Immutable match. Exhaustive due to minutes being convertible to seconds.
//   assert("2 hours"s ==
//          ftl::match(duration,
//                     [](const seconds& s) {
//                       const auto h = duration_cast<hours>(s);
//                       return std::to_string(h.count()) + " hours"s;
//                     },
//                     [](const hours& h) { return std::to_string(h.count() / 24) + " days"s; }));
//
    template<typename... Ts, typename... Ms>
    decltype(auto) match(std::variant<Ts...> &variant, Ms &&... matchers) {
        const auto matcher = details::Matcher{std::forward<Ms>(matchers)...};
        static_assert(details::is_exhaustive_match_v<decltype(matcher), Ts &...>,
                      "Non-exhaustive match");

        return details::Match<Ts...>::match(variant, matcher);
    }

    template<typename... Ts, typename... Ms>
    decltype(auto) match(const std::variant<Ts...> &variant, Ms &&... matchers) {
        const auto matcher = details::Matcher{std::forward<Ms>(matchers)...};
        static_assert(details::is_exhaustive_match_v<decltype(matcher), const Ts &...>,
                      "Non-exhaustive match");

        return details::Match<Ts...>::match(variant, matcher);
    }

}  // namespace android::ftl
