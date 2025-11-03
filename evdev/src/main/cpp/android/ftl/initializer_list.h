/*
 * Copyright 2020 The Android Open Source Project
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
#include <tuple>
#include <utility>

namespace android::ftl {

// Compile-time counterpart of std::initializer_list<T> that stores per-element constructor
// arguments with heterogeneous types. For a container with elements of type T, given Sizes
// (S0, S1, ..., SN), N elements are initialized: the first element is initialized with the
// first S0 arguments, the second element is initialized with the next S1 arguments, and so
// on. The list of Types (T0, ..., TM) is flattened, so M is equal to the sum of the Sizes.
//
// An InitializerList is created using ftl::init::list, and is consumed by constructors of
// containers. The function call operator is overloaded such that arguments are accumulated
// in a tuple with each successive call. For instance, the following calls initialize three
// strings using different constructors, i.e. string literal, default, and count/character:
//
//   ... = ftl::init::list<std::string>("abc")()(3u, '?');
//
// The following syntax is a shorthand for key-value pairs, where the first argument is the
// key, and the rest construct the value. The types of the key and value are deduced if the
// first pair contains exactly two arguments:
//
//   ... = ftl::init::map<int, std::string>(-1, "abc")(-2)(-3, 3u, '?');
//
//   ... = ftl::init::map(0, 'a')(1, 'b')(2, 'c');
//
// WARNING: The InitializerList returned by an ftl::init::list expression must be consumed
// immediately, since temporary arguments are destroyed after the full expression. Storing
// an InitializerList results in dangling references.
//
    template<typename T, typename Sizes = std::index_sequence<>, typename... Types>
    struct InitializerList;

    template<typename T, std::size_t... Sizes, typename... Types>
    struct InitializerList<T, std::index_sequence<Sizes...>, Types...> {
        // Creates a superset InitializerList by appending the number of arguments to Sizes, and
        // expanding Types with forwarding references for each argument.
        template<typename... Args>
        [[nodiscard]] constexpr auto operator()(Args &&... args) && -> InitializerList<
                T, std::index_sequence<Sizes..., sizeof...(Args)>, Types..., Args && ...> {
            return {std::tuple_cat(std::move(tuple),
                                   std::forward_as_tuple(std::forward<Args>(args)...))};
        }

        // The temporary InitializerList returned by operator() is bound to an rvalue reference in
        // container constructors, which extends the lifetime of any temporary arguments that this
        // tuple refers to until the completion of the full expression containing the construction.
        std::tuple<Types...> tuple;
    };

    template<typename K, typename V, typename KeyEqual = std::equal_to<K>>
    struct KeyValue {
    };

// Shorthand for key-value pairs that assigns the first argument to the key, and the rest to the
// value. The specialization is on KeyValue rather than std::pair, so that ftl::init::list works
// with the latter.
    template<typename K, typename V, typename E, std::size_t... Sizes, typename... Types>
    struct InitializerList<KeyValue<K, V, E>, std::index_sequence<Sizes...>, Types...> {
        // Accumulate the three arguments to std::pair's piecewise constructor.
        template<typename... Args>
        [[nodiscard]] constexpr auto operator()(K &&k, Args &&... args) && -> InitializerList<
                KeyValue<K, V, E>, std::index_sequence<Sizes..., 3>, Types..., std::piecewise_construct_t,
                std::tuple<K &&>, std::tuple<Args && ...>> {
            return {std::tuple_cat(
                    std::move(tuple),
                    std::forward_as_tuple(std::piecewise_construct,
                                          std::forward_as_tuple(std::forward<K>(k)),
                                          std::forward_as_tuple(std::forward<Args>(args)...)))};
        }

        std::tuple<Types...> tuple;
    };

    namespace init {

        template<typename T, typename... Args>
        [[nodiscard]] constexpr auto list(Args &&... args) {
            return InitializerList<T>{}(std::forward<Args>(args)...);
        }

        template<typename K, typename V, typename E = std::equal_to<K>, typename... Args>
        [[nodiscard]] constexpr auto map(Args &&... args) {
            return list<KeyValue<K, V, E>>(std::forward<Args>(args)...);
        }

        template<typename K, typename V>
        [[nodiscard]] constexpr auto map(K &&k, V &&v) {
            return list<KeyValue<K, V>>(std::forward<K>(k), std::forward<V>(v));
        }

    }  // namespace init
}  // namespace android::ftl
