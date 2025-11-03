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

#include <functional>
#include <optional>

#include <ftl/details/type_traits.h>

namespace android::ftl {

    template<typename>
    struct Optional;

    namespace details {

        template<typename>
        struct is_optional : std::false_type {
        };

        template<typename T>
        struct is_optional<std::optional<T>> : std::true_type {
        };

        template<typename T>
        struct is_optional<Optional<T>> : std::true_type {
        };

        template<typename F, typename T>
        struct transform_result {
            using type = Optional<std::remove_cv_t<std::invoke_result_t<F, T>>>;
        };

        template<typename F, typename T>
        using transform_result_t = typename transform_result<F, T>::type;

        template<typename F, typename T>
        struct and_then_result {
            using type = remove_cvref_t<std::invoke_result_t<F, T>>;
            static_assert(is_optional<type>{}, "and_then function must return an optional");
        };

        template<typename F, typename T>
        using and_then_result_t = typename and_then_result<F, T>::type;

        template<typename F, typename T>
        struct or_else_result {
            using type = remove_cvref_t<std::invoke_result_t<F>>;
            static_assert(
                    std::is_same_v<type, std::optional<T>> || std::is_same_v<type, Optional<T>>,
                    "or_else function must return an optional T");
        };

        template<typename F, typename T>
        using or_else_result_t = typename or_else_result<F, T>::type;

    }  // namespace details
}  // namespace android::ftl
