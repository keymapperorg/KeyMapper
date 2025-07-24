/*
 * Copyright (C) 2017 The Android Open Source Project
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

// Result<T, E> is the type that is used to pass a success value of type T or an error code of type
// E, optionally together with an error message. T and E can be any type. If E is omitted it
// defaults to int, which is useful when errno(3) is used as the error code.
//
// Passing a success value or an error value:
//
// Result<std::string> readFile() {
//   std::string content;
//   if (base::ReadFileToString("path", &content)) {
//     return content; // ok case
//   } else {
//     return ErrnoError() << "failed to read"; // error case
//   }
// }
//
// Checking the result and then unwrapping the value or propagating the error:
//
// Result<bool> hasAWord() {
//   auto content = readFile();
//   if (!content.ok()) {
//     return Error() << "failed to process: " << content.error();
//   }
//   return (*content.find("happy") != std::string::npos);
// }
//
// Using custom error code type:
//
// enum class MyError { A, B }; // assume that this is the error code you already have
//
// // To use the error code with Result, define a wrapper class that provides the following
// operations and use the wrapper class as the second type parameter (E) when instantiating
// Result<T, E>
//
// 1. default constructor
// 2. copy constructor / and move constructor if copying is expensive
// 3. conversion operator to the error code type
// 4. value() function that return the error code value
// 5. print() function that gives a string representation of the error ode value
//
// struct MyErrorWrapper {
//   MyError val_;
//   MyErrorWrapper() : val_(/* reasonable default value */) {}
//   MyErrorWrapper(MyError&& e) : val_(std:forward<MyError>(e)) {}
//   operator const MyError&() const { return val_; }
//   MyError value() const { return val_; }
//   std::string print() const {
//     switch(val_) {
//       MyError::A: return "A";
//       MyError::B: return "B";
//     }
//   }
// };
//
// #define NewMyError(e) Error<MyErrorWrapper>(MyError::e)
//
// Result<T, MyError> val = NewMyError(A) << "some message";
//
// Formatting the error message using fmtlib:
//
// Errorf("{} errors", num); // equivalent to Error() << num << " errors";
// ErrnoErrorf("{} errors", num); // equivalent to ErrnoError() << num << " errors";
//
// Returning success or failure, but not the value:
//
// Result<void> doSomething() {
//   if (success) return {};
//   else return Error() << "error occurred";
// }
//
// Extracting error code:
//
// Result<T> val = Error(3) << "some error occurred";
// assert(3 == val.error().code());
//

#pragma once

#include <assert.h>
#include <errno.h>

#include <sstream>
#include <cstring>
#include <type_traits>
#include <sstream>
#include <cstdlib>

#include "errors.h"
#include "expected.h"

namespace android {
namespace base {

// Errno is a wrapper class for errno(3). Use this type instead of `int` when instantiating
// `Result<T, E>` and `Error<E>` template classes. This is required to distinguish errno from other
// integer-based error code types like `status_t`.
struct Errno {
  Errno() : val_(0) {}
  Errno(int e) : val_(e) {}
  int value() const { return val_; }
  operator int() const { return value(); }
  const char* print() const { return strerror(value()); }

  int val_;

  // TODO(b/209929099): remove this conversion operator. This currently is needed to not break
  // existing places where error().code() is used to construct enum values.
  template <typename E, typename = std::enable_if_t<std::is_enum_v<E>>>
  operator E() const {
    return E(val_);
  }
};

static_assert(std::is_trivially_copyable_v<Errno> == true);

template <typename E = Errno, bool include_message = true>
struct ResultError {
  template <typename T, typename P, typename = std::enable_if_t<std::is_convertible_v<P, E>>>
  ResultError(T&& message, P&& code)
      : message_(std::forward<T>(message)), code_(E(std::forward<P>(code))) {}

  ResultError(const ResultError& other) = default;
  ResultError(ResultError&& other) = default;
  ResultError& operator=(const ResultError& other) = default;
  ResultError& operator=(ResultError&& other) = default;

  template <typename T>
  // NOLINTNEXTLINE(google-explicit-constructor)
  operator android::base::expected<T, ResultError<E>>() && {
    return android::base::unexpected(std::move(*this));
  }

  template <typename T>
  // NOLINTNEXTLINE(google-explicit-constructor)
  operator android::base::expected<T, ResultError<E>>() const& {
    return android::base::unexpected(*this);
  }

  const std::string& message() const { return message_; }
  const E& code() const { return code_; }

 private:
  std::string message_;
  E code_;
};

template <typename E>
auto format_as(ResultError<E, true> error) {
  return error.message();
}

template <typename E>
struct ResultError<E, /* include_message */ false> {
  template <typename P, typename = std::enable_if_t<std::is_convertible_v<P, E>>>
  ResultError(P&& code) : code_(E(std::forward<P>(code))) {}

  template <typename T>
  operator android::base::expected<T, ResultError<E, false>>() const {
    return android::base::unexpected(ResultError<E, false>(code_));
  }

  const E& code() const { return code_; }

 private:
  E code_;
};

template <typename E>
inline bool operator==(const ResultError<E>& lhs, const ResultError<E>& rhs) {
  return lhs.message() == rhs.message() && lhs.code() == rhs.code();
}

template <typename E>
inline bool operator!=(const ResultError<E>& lhs, const ResultError<E>& rhs) {
  return !(lhs == rhs);
}

template <typename E>
inline std::ostream& operator<<(std::ostream& os, const ResultError<E>& t) {
  os << t.message();
  return os;
}

namespace internal {
// Stream class that does nothing and is has zero (actually 1) size. It is used instead of
// std::stringstream when include_message is false so that we use less on stack.
// sizeof(std::stringstream) is 280 on arm64.
struct DoNothingStream {
  template <typename T>
  DoNothingStream& operator<<(T&&) {
    return *this;
  }

  std::string str() const { return ""; }
};
}  // namespace internal

template <typename E = Errno, bool include_message = true,
          typename = std::enable_if_t<!std::is_same_v<E, int>>>
class Error {
 public:
  Error() : code_(0), has_code_(false) {}
  template <typename P, typename = std::enable_if_t<std::is_convertible_v<P, E>>>
  // NOLINTNEXTLINE(google-explicit-constructor)
  Error(P&& code) : code_(std::forward<P>(code)), has_code_(true) {}

  template <typename T, typename P, typename = std::enable_if_t<std::is_convertible_v<E, P>>>
  // NOLINTNEXTLINE(google-explicit-constructor)
  operator android::base::expected<T, ResultError<P>>() const {
    return android::base::unexpected(ResultError<P>(str(), static_cast<P>(code_)));
  }

  template <typename T, typename P, typename = std::enable_if_t<std::is_convertible_v<E, P>>>
  // NOLINTNEXTLINE(google-explicit-constructor)
  operator android::base::expected<T, ResultError<P, false>>() const {
    return android::base::unexpected(ResultError<P, false>(static_cast<P>(code_)));
  }

  template <typename T>
  Error& operator<<(T&& t) {
    static_assert(include_message, "<< not supported when include_message = false");
    // NOLINTNEXTLINE(bugprone-suspicious-semicolon)
    if constexpr (std::is_same_v<std::remove_cv_t<std::remove_reference_t<T>>, ResultError<E>>) {
      if (!has_code_) {
        code_ = t.code();
      }
      return (*this) << t.message();
    }
    int saved = errno;
    ss_ << t;
    errno = saved;
    return *this;
  }

  const std::string str() const {
    static_assert(include_message, "str() not supported when include_message = false");
    std::string str = ss_.str();
    if (has_code_) {
      if (str.empty()) {
        return code_.print();
      }
      return std::move(str) + ": " + code_.print();
    }
    return str;
  }

  Error(const Error&) = delete;
  Error(Error&&) = delete;
  Error& operator=(const Error&) = delete;
  Error& operator=(Error&&) = delete;

  template <typename... Args>
  friend Error ErrorfImpl(const std::string &fmt, const Args &... args);

  template <typename... Args>
  friend Error ErrnoErrorfImpl(const std::string &fmt, const Args &... args);

 private:
  Error(bool has_code, E code, const std::string& message) : code_(code), has_code_(has_code) {
    (*this) << message;
  }

  std::conditional_t<include_message, std::stringstream, internal::DoNothingStream> ss_;
  E code_;
  const bool has_code_;
};

inline Error<Errno> ErrnoError() {
  return Error<Errno>(Errno{errno});
}

template <typename E>
inline E ErrorCode(E code) {
  return code;
}

// Return the error code of the last ResultError object, if any.
// Otherwise, return `code` as it is.
template <typename T, typename E, typename... Args>
inline E ErrorCode(E code, T&& t, const Args&... args) {
  if constexpr (std::is_same_v<std::remove_cv_t<std::remove_reference_t<T>>, ResultError<E>>) {
    return ErrorCode(t.code(), args...);
  }
  return ErrorCode(code, args...);
}

__attribute__((noinline)) ResultError<Errno> MakeResultErrorWithCode(std::string&& message,
                                                                     Errno code);

template <typename... Args>
inline ResultError<Errno> ErrorfImpl(const std::string &fmt, const Args &... args) {
    std::ostringstream oss;
    formatHelper(oss, fmt, args...);
    return ResultError(oss.str(), ErrorCode(Errno{}, args...));
}

    template<typename T>
    void formatHelper(std::ostringstream &oss, const std::string &fmt, const T &arg) {
        size_t pos = fmt.find("{}");
        if (pos != std::string::npos) {
            oss << fmt.substr(0, pos) << arg << fmt.substr(pos + 2);
        } else {
            oss << fmt;
        }
    }

    template<typename T, typename... Args>
    void formatHelper(std::ostringstream &oss, const std::string &fmt, const T &arg,
                      const Args &... args) {
        size_t pos = fmt.find("{}");
        if (pos != std::string::npos) {
            oss << fmt.substr(0, pos) << arg;
            formatHelper(oss, fmt.substr(pos + 2), args...);
        } else {
            oss << fmt;
        }
    }

    void formatHelper(std::ostringstream &oss, const std::string &fmt) {
        oss << fmt;
}

template <typename... Args>
inline ResultError<Errno> ErrnoErrorfImpl(const std::string &fmt, const Args &... args) {
    Errno code{errno};
    std::ostringstream oss;
    formatHelper(oss, fmt, args...);
    return MakeResultErrorWithCode(oss.str(), code);
}

#define Errorf(fmt, ...) android::base::ErrorfImpl(fmt, ##__VA_ARGS__)
#define ErrnoErrorf(fmt, ...) android::base::ErrnoErrorfImpl(fmt, ##__VA_ARGS__)

template <typename T, typename E = Errno, bool include_message = true>
using Result = android::base::expected<T, ResultError<E, include_message>>;

// Specialization of android::base::OkOrFail<V> for V = Result<T, E>. See android-base/errors.h
// for the contract.

namespace impl {
template <typename U>
using Code = std::decay_t<decltype(std::declval<U>().error().code())>;

template <typename U>
using ErrorType = std::decay_t<decltype(std::declval<U>().error())>;

template <typename U>
constexpr bool IsNumeric = std::is_integral_v<U> || std::is_floating_point_v<U> ||
                           (std::is_enum_v<U> && std::is_convertible_v<U, size_t>);

// This base class exists to take advantage of shadowing
// We include the conversion in this base class so that if the conversion in NumericConversions
// overlaps, we (arbitrarily) choose the implementation in NumericConversions due to shadowing.
template <typename T>
struct ConversionBase {
  ErrorType<T> error_;
  // T is a expected<U, ErrorType<T>>.
  operator T() const& { return unexpected(error_); }
  operator T() && { return unexpected(std::move(error_)); }

  operator Code<T>() const { return error_.code(); }
};

// User defined conversions can be followed by numeric conversions
// Although we template specialize for the exact code type, we need
// specializations for conversions to all numeric types to avoid an
// ambiguous conversion sequence.
template <typename T, typename = void>
struct NumericConversions : public ConversionBase<T> {};
template <typename T>
struct NumericConversions<T,
    std::enable_if_t<impl::IsNumeric<impl::Code<T>>>
    > : public ConversionBase<T>
{
#pragma push_macro("SPECIALIZED_CONVERSION")
#define SPECIALIZED_CONVERSION(type)                                                  \
  operator expected<type, ErrorType<T>>() const& { return unexpected(this->error_); } \
  operator expected<type, ErrorType<T>>()&& { return unexpected(std::move(this->error_)); }

  SPECIALIZED_CONVERSION(int)
  SPECIALIZED_CONVERSION(short int)
  SPECIALIZED_CONVERSION(unsigned short int)
  SPECIALIZED_CONVERSION(unsigned int)
  SPECIALIZED_CONVERSION(long int)
  SPECIALIZED_CONVERSION(unsigned long int)
  SPECIALIZED_CONVERSION(long long int)
  SPECIALIZED_CONVERSION(unsigned long long int)
  SPECIALIZED_CONVERSION(bool)
  SPECIALIZED_CONVERSION(char)
  SPECIALIZED_CONVERSION(unsigned char)
  SPECIALIZED_CONVERSION(signed char)
  SPECIALIZED_CONVERSION(wchar_t)
  SPECIALIZED_CONVERSION(char16_t)
  SPECIALIZED_CONVERSION(char32_t)
  SPECIALIZED_CONVERSION(float)
  SPECIALIZED_CONVERSION(double)
  SPECIALIZED_CONVERSION(long double)

#undef SPECIALIZED_CONVERSION
#pragma pop_macro("SPECIALIZED_CONVERSION")
  // For debugging purposes
  using IsNumericT = std::true_type;
};

#ifdef __cpp_concepts
template <class U>
// Define a concept which **any** type matches to
concept Universal = std::is_same_v<U, U>;
#endif

// A type that is never used.
struct Never {};
} // namespace impl

template <typename T, typename E, bool include_message>
struct OkOrFail<Result<T, E, include_message>>
    : public impl::NumericConversions<Result<T, E, include_message>> {
  using V = Result<T, E, include_message>;
  using Err = impl::ErrorType<V>;
  using C = impl::Code<V>;
private:
   OkOrFail(Err&& v): impl::NumericConversions<V>{std::move(v)} {}
   OkOrFail(const OkOrFail& other) = delete;
   OkOrFail(const OkOrFail&& other) = delete;
public:
  // Checks if V is ok or fail
  static bool IsOk(const V& val) { return val.ok(); }

  // Turns V into a success value
  static T Unwrap(V&& val) {
    if constexpr (std::is_same_v<T, void>) {
      assert(IsOk(val));
      return;
    } else {
      return std::move(val.value());
    }
  }

  // Consumes V when it's a fail value
  static OkOrFail<V> Fail(V&& v) {
    assert(!IsOk(v));
    return OkOrFail<V>{std::move(v.error())};
  }

  // We specialize as much as possible to avoid ambiguous conversion with templated expected ctor.
  // We don't need this specialization if `C` is numeric because that case is already covered by
  // `NumericConversions`.
  operator Result<std::conditional_t<impl::IsNumeric<C>, impl::Never, C>, E, include_message>()
      const& {
    return unexpected(this->error_);
  }
  operator Result<std::conditional_t<impl::IsNumeric<C>, impl::Never, C>, E, include_message>() && {
    return unexpected(std::move(this->error_));
  }

#ifdef __cpp_concepts
  // The idea here is to match this template method to any type (not simply trivial types).
  // The reason for including a constraint is to take advantage of the fact that a constrained
  // method always has strictly lower precedence than a non-constrained method in template
  // specialization rules (thus avoiding ambiguity). So we use a universally matching constraint to
  // mark this function as less preferable (but still accepting of all types).
  template <impl::Universal U>
  operator Result<U, E, include_message>() const& {
    return unexpected(this->error_);
  }
  template <impl::Universal U>
  operator Result<U, E, include_message>() && {
    return unexpected(std::move(this->error_));
  }
#else
  template <typename U>
  operator Result<U, E, include_message>() const& {
    return unexpected(this->error_);
  }
  template <typename U>
  operator Result<U, E, include_message>() && {
    return unexpected(std::move(this->error_));
  }
#endif

  static const std::string& ErrorMessage(const V& val) { return val.error().message(); }
};

// Macros for testing the results of functions that return android::base::Result. These also work
// with base::android::expected. They assume the user depends on libgmock and includes
// gtest/gtest.h. For advanced matchers and customized error messages, see result-gmock.h.

#define ASSERT_RESULT_OK(stmt)                            \
  if (const auto& tmp = (stmt); !tmp.ok())                \
  FAIL() << "Value of: " << #stmt << "\n"                 \
         << "  Actual: " << tmp.error().message() << "\n" \
         << "Expected: is ok\n"

#define EXPECT_RESULT_OK(stmt)                                   \
  if (const auto& tmp = (stmt); !tmp.ok())                       \
  ADD_FAILURE() << "Value of: " << #stmt << "\n"                 \
                << "  Actual: " << tmp.error().message() << "\n" \
                << "Expected: is ok\n"

}  // namespace base
}  // namespace android
