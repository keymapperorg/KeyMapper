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

namespace android::ftl::details {

// TODO: Replace with std::remove_cvref_t in C++20.
    template<typename U>
    using remove_cvref_t = std::remove_cv_t<std::remove_reference_t<U>>;

    template<typename T>
    constexpr bool is_bool_v = std::is_same_v<remove_cvref_t<T>, bool>;

    template<typename T>
    constexpr bool is_char_v = std::is_same_v<remove_cvref_t<T>, char>;

}  // namespace android::ftl::details
