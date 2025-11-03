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

#include <algorithm>
#include <functional>
#include <utility>

#include <ftl/optional.h>

namespace android::ftl {

// Determines if a container contains a value. This is a simplified version of the C++23
// std::ranges::contains function.
//
//   const ftl::StaticVector vector = {1, 2, 3};
//   assert(ftl::contains(vector, 1));
//
// TODO: Remove in C++23.
    template<typename Container, typename Value>
    auto contains(const Container &container, const Value &value) -> bool {
        return std::find(container.begin(), container.end(), value) != container.end();
    }

// Adapter for std::find_if that converts the return value from iterator to optional.
//
//   const ftl::StaticVector vector = {"upside"sv, "down"sv, "cake"sv};
//   assert(ftl::find_if(vector, [](const auto& str) { return str.front() == 'c'; }) == "cake"sv);
//
    template<typename Container, typename Predicate, typename V = typename Container::value_type>
    constexpr auto find_if(const Container &container, Predicate &&predicate)
    -> Optional<std::reference_wrapper<const V>> {
        const auto it = std::find_if(std::cbegin(container), std::cend(container),
                                     std::forward<Predicate>(predicate));
        if (it == std::cend(container)) return {};
        return std::cref(*it);
    }

// Transformers for ftl::find_if on a map-like `Container` that contains key-value pairs.
//
//   const ftl::SmallMap map = ftl::init::map<int, ftl::StaticVector<std::string_view, 3>>(
//       12, "snow"sv, "cone"sv)(13, "tiramisu"sv)(14, "upside"sv, "down"sv, "cake"sv);
//
//   using Map = decltype(map);
//
//   assert(14 == ftl::find_if(map, [](const auto& pair) {
//                  return pair.second.size() == 3;
//                }).transform(ftl::to_key<Map>));
//
//   const auto opt = ftl::find_if(map, [](const auto& pair) {
//                      return pair.second.size() == 1;
//                    }).transform(ftl::to_mapped_ref<Map>);
//
//   assert(opt);
//   assert(opt->get() == ftl::StaticVector("tiramisu"sv));
//
    template<typename Map, typename Pair = typename Map::value_type,
            typename Key = typename Map::key_type>
    constexpr auto to_key(const Pair &pair) -> Key {
        return pair.first;
    }

    template<typename Map, typename Pair = typename Map::value_type,
            typename Mapped = typename Map::mapped_type>
    constexpr auto to_mapped_ref(const Pair &pair) -> std::reference_wrapper<const Mapped> {
        return std::cref(pair.second);
    }

// Combinator for ftl::Optional<T>::or_else when T is std::reference_wrapper<const V>. Given a
// lambda argument that returns a `constexpr` value, ftl::static_ref<T> binds a reference to a
// static T initialized to that constant.
//
//   const ftl::SmallMap map = ftl::init::map(13, "tiramisu"sv)(14, "upside-down cake"sv);
//   assert("???"sv ==
//          map.get(20).or_else(ftl::static_ref<std::string_view>([] { return "???"sv; }))->get());
//
//   using Map = decltype(map);
//
//   assert("snow cone"sv ==
//          ftl::find_if(map, [](const auto& pair) { return pair.second.front() == 's'; })
//              .transform(ftl::to_mapped_ref<Map>)
//              .or_else(ftl::static_ref<std::string_view>([] { return "snow cone"sv; }))
//              ->get());
//
    template<typename T, typename F>
    constexpr auto static_ref(F &&f) {
        return [f = std::forward<F>(f)] {
            constexpr auto kInitializer = f();
            static const T kValue = kInitializer;
            return Optional(std::cref(kValue));
        };
    }

}  // namespace android::ftl
