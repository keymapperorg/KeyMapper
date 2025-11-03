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
#include <utility>

namespace android::ftl {

// The unit type, and its only value.
    constexpr struct Unit {
    } unit;

    constexpr bool operator==(Unit, Unit) {
        return true;
    }

    constexpr bool operator!=(Unit, Unit) {
        return false;
    }

// Adapts a function object F to return Unit. The return value of F is ignored.
//
// As a practical use, the function passed to ftl::Optional<T>::transform is not allowed to return
// void (cf. https://wg21.link/P0798R8#mapping-functions-returning-void), but may return Unit if
// only its side effects are meaningful:
//
//   ftl::Optional opt = "food"s;
//   opt.transform(ftl::unit_fn([](std::string& str) { str.pop_back(); }));
//   assert(opt == "foo"s);
//
    template<typename F>
    struct UnitFn {
        F f;

        template<typename... Args>
        Unit operator()(Args &&... args) {
            return f(std::forward<Args>(args)...), unit;
        }
    };

    template<typename F>
    constexpr auto unit_fn(F &&f) -> UnitFn<std::decay_t<F>> {
        return {std::forward<F>(f)};
    }

    namespace details {

// Identity function for all T except Unit, which maps to void.
        template<typename T>
        struct UnitToVoid {
            template<typename U>
            static auto from(U &&value) {
                return value;
            }
        };

        template<>
        struct UnitToVoid<Unit> {
            template<typename U>
            static void from(U &&) {}
        };

    }  // namespace details
}  // namespace android::ftl
