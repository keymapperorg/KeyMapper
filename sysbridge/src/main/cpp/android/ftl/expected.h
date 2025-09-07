/*
 * Copyright 2024 The Android Open Source Project
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

#include "../libbase/expected.h"
#include <ftl/optional.h>
#include <ftl/unit.h>

#include <utility>

// Given an expression `expr` that evaluates to an ftl::Expected<T, E> result (R for short), FTL_TRY
// unwraps T out of R, or bails out of the enclosing function F if R has an error E. The return type
// of F must be R, since FTL_TRY propagates R in the error case. As a special case, ftl::Unit may be
// used as the error E to allow FTL_TRY expressions when F returns `void`.
//
// The non-standard syntax requires `-Wno-gnu-statement-expression-from-macro-expansion` to compile.
// The UnitToVoid conversion allows the macro to be used for early exit from a function that returns
// `void`.
//
// Example usage:
//
//   using StringExp = ftl::Expected<std::string, std::errc>;
//
//   StringExp repeat(StringExp exp) {
//     const std::string str = FTL_TRY(exp);
//     return StringExp(str + str);
//   }
//
//   assert(StringExp("haha"s) == repeat(StringExp("ha"s)));
//   assert(repeat(ftl::Unexpected(std::errc::bad_message)).has_error([](std::errc e) {
//     return e == std::errc::bad_message;
//   }));
//
//
// FTL_TRY may be used in void-returning functions by using ftl::Unit as the error type:
//
//   void uppercase(char& c, ftl::Optional<char> opt) {
//     c = std::toupper(FTL_TRY(std::move(opt).ok_or(ftl::Unit())));
//   }
//
//   char c = '?';
//   uppercase(c, std::nullopt);
//   assert(c == '?');
//
//   uppercase(c, 'a');
//   assert(c == 'A');
//
#define FTL_TRY(expr)                                                     \
  ({                                                                      \
    auto exp_ = (expr);                                                   \
    if (!exp_.has_value()) {                                              \
      using E = decltype(exp_)::error_type;                               \
      return android::ftl::details::UnitToVoid<E>::from(std::move(exp_)); \
    }                                                                     \
    exp_.value();                                                         \
  })

// Given an expression `expr` that evaluates to an ftl::Expected<T, E> result (R for short),
// FTL_EXPECT unwraps T out of R, or bails out of the enclosing function F if R has an error E.
// While FTL_TRY bails out with R, FTL_EXPECT bails out with E, which is useful when F does not
// need to propagate R because T is not relevant to the caller.
//
// Example usage:
//
//   using StringExp = ftl::Expected<std::string, std::errc>;
//
//   std::errc repeat(StringExp exp, std::string& out) {
//     const std::string str = FTL_EXPECT(exp);
//     out = str + str;
//     return std::errc::operation_in_progress;
//   }
//
//   std::string str;
//   assert(std::errc::operation_in_progress == repeat(StringExp("ha"s), str));
//   assert("haha"s == str);
//   assert(std::errc::bad_message == repeat(ftl::Unexpected(std::errc::bad_message), str));
//   assert("haha"s == str);
//
#define FTL_EXPECT(expr)              \
  ({                                  \
    auto exp_ = (expr);               \
    if (!exp_.has_value()) {          \
      return std::move(exp_.error()); \
    }                                 \
    exp_.value();                     \
  })

namespace android::ftl {

// Superset of base::expected<T, E> with monadic operations.
//
// TODO: Extend std::expected<T, E> in C++23.
//
    template<typename T, typename E>
    struct Expected final : base::expected<T, E> {
        using Base = base::expected<T, E>;
        using Base::expected;

        using Base::error;
        using Base::has_value;
        using Base::value;

        template<typename P>
        constexpr bool has_error(P predicate) const {
            return !has_value() && predicate(error());
        }

        constexpr Optional<T> value_opt() const &{
            return has_value() ? Optional(value()) : std::nullopt;
        }

        constexpr Optional<T> value_opt() &&{
            return has_value() ? Optional(std::move(value())) : std::nullopt;
        }

        // Delete new for this class. Its base doesn't have a virtual destructor, and
        // if it got deleted via base class pointer, it would cause undefined
        // behavior. There's not a good reason to allocate this object on the heap
        // anyway.
        static void *operator new(size_t) = delete;

        static void *operator new[](size_t) = delete;
    };

    template<typename E>
    constexpr auto Unexpected(E &&error) {
        return base::unexpected(std::forward<E>(error));
    }

}  // namespace android::ftl
