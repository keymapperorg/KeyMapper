/*
 * Copyright (C) 2016 The Android Open Source Project
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

// Portable error handling functions. This is only necessary for host-side
// code that needs to be cross-platform; code that is only run on Unix should
// just use errno and strerror() for simplicity.
//
// There is some complexity since Windows has (at least) three different error
// numbers, not all of which share the same type:
//   * errno: for C runtime errors.
//   * GetLastError(): Windows non-socket errors.
//   * WSAGetLastError(): Windows socket errors.
// errno can be passed to strerror() on all platforms, but the other two require
// special handling to get the error string. Refer to Microsoft documentation
// to determine which error code to check for each function.

#pragma once

#include <assert.h>

#include <string>

namespace android {
namespace base {

// Returns a string describing the given system error code. |error_code| must
// be errno on Unix or GetLastError()/WSAGetLastError() on Windows. Passing
// errno on Windows has undefined behavior.
std::string SystemErrorCodeToString(int error_code);

}  // namespace base
}  // namespace android

// Convenient macros for evaluating a statement, checking if the result is error, and returning it
// to the caller. If it is ok then the inner value is unwrapped (if applicable) and returned.
//
// Usage with Result<T>:
//
// Result<Foo> getFoo() {...}
//
// Result<Bar> getBar() {
//   Foo foo = OR_RETURN(getFoo());
//   return Bar{foo};
// }
//
// Usage with status_t:
//
// status_t getFoo(Foo*) {...}
//
// status_t getBar(Bar* bar) {
//   Foo foo;
//   OR_RETURN(getFoo(&foo));
//   *bar = Bar{foo};
//   return OK;
// }
//
// Actually this can be used for any type as long as the OkOrFail<T> contract is satisfied. See
// below.
// If implicit conversion compilation errors occur involving a value type with a templated
// forwarding ref ctor, compilation with cpp20 or explicitly converting to the desired
// return type is required.
#define OR_RETURN(expr) \
  UNWRAP_OR_DO(__or_return_expr, expr, { return ok_or_fail::Fail(std::move(__or_return_expr)); })

// Same as OR_RETURN, but aborts if expr is a failure.
#if defined(__BIONIC__)
#define OR_FATAL(expr)                                                               \
  UNWRAP_OR_DO(__or_fatal_expr, expr, {                                              \
    __assert(__FILE__, __LINE__, ok_or_fail::ErrorMessage(__or_fatal_expr).c_str()); \
  })
#else
#define OR_FATAL(expr)                                                    \
  UNWRAP_OR_DO(__or_fatal_expr, expr, {                                   \
    fprintf(stderr, "%s:%d: assertion \"%s\" failed", __FILE__, __LINE__, \
            ok_or_fail::ErrorMessage(__or_fatal_expr).c_str());           \
    abort();                                                              \
  })
#endif

// Variant for use in gtests, which aborts the test function with an assertion failure on error.
// This is akin to ASSERT_OK_AND_ASSIGN for absl::Status, except the assignment is external. It
// assumes the user depends on libgmock and includes gtest/gtest.h.
#define OR_ASSERT_FAIL(expr)                                             \
  UNWRAP_OR_DO(__or_assert_expr, expr, {                                 \
    FAIL() << "Value of: " << #expr << "\n"                              \
           << "  Actual: " << __or_assert_expr.error().message() << "\n" \
           << "Expected: is ok\n";                                       \
  })

// Generic macro to execute any statement(s) on error. Execution should never reach the end of them.
// result_var is assigned expr and is only visible to on_error_stmts.
#define UNWRAP_OR_DO(result_var, expr, on_error_stmts)                                         \
  ({                                                                                           \
    decltype(expr)&& result_var = (expr);                                                      \
    typedef android::base::OkOrFail<std::remove_reference_t<decltype(result_var)>> ok_or_fail; \
    if (!ok_or_fail::IsOk(result_var)) {                                                       \
      {                                                                                        \
        on_error_stmts;                                                                        \
      }                                                                                        \
      __builtin_unreachable();                                                                 \
    }                                                                                          \
    ok_or_fail::Unwrap(std::move(result_var));                                                 \
  })

namespace android {
namespace base {

// The OkOrFail contract for a type T. This must be implemented for a type T if you want to use
// OR_RETURN(stmt) where stmt evalues to a value of type T.
template <typename T, typename = void>
struct OkOrFail {
  // Checks if T is ok or fail.
  static bool IsOk(const T&);

  // Turns T into the success value.
  template <typename U>
  static U Unwrap(T&&);

  // Moves T into OkOrFail<T>, so that we can convert it to other types
  OkOrFail(T&& v);
  OkOrFail() = delete;
  OkOrFail(const T&) = delete;

  // And there need to be one or more conversion operators that turns the error value of T into a
  // target type. For example, for T = Result<V, E>, there can be ...
  //
  // // for the case where OR_RETURN is called in a function expecting E
  // operator E()&& { return val_.error().code(); }
  //
  // // for the case where OR_RETURN is called in a function expecting Result<U, E>
  // template <typename U>
  // operator Result<U, E>()&& { return val_.error(); }

  // And there needs to be a method that returns the string representation of the fail value.
  // static const std::string& ErrorMessage(const T& v);
  // or
  // static std::string ErrorMessage(const T& v);
};

}  // namespace base
}  // namespace android
