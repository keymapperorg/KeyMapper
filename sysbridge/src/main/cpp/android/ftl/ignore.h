/*
 * Copyright 2025 The Android Open Source Project
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

namespace android::ftl {

// An alternative to `std::ignore` that makes it easy to ignore multiple values.
//
// Examples:
//
//   void ftl_ignore_multiple(int arg1, const char* arg2, std::string arg3) {
//     // When invoked, all the arguments are ignored.
//     ftl::ignore(arg1, arg2, arg3);
//   }
//
//   void ftl_ignore_single(int arg) {
//     // It can be used like std::ignore to ignore a single value
//     ftl::ignore = arg;
//   }
//
    inline constexpr struct {
        // NOLINTNEXTLINE(misc-unconventional-assign-operator, readability-named-parameter)
        constexpr auto operator=(auto &&) const -> decltype(*this) { return *this; }

        // NOLINTNEXTLINE(readability-named-parameter)
        constexpr void operator()(auto &&...) const {}
    } ignore;

}  // namespace android::ftl