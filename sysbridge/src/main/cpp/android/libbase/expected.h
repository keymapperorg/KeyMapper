/*
 * Copyright (C) 2019 The Android Open Source Project
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
#include <initializer_list>
#include <type_traits>
#include <utility>
#include <variant>

// android::base::expected is a partial implementation of C++23's std::expected
// for Android.
//
// Usage:
// using android::base::expected;
// using android::base::unexpected;
//
// expected<double,std::string> safe_divide(double i, double j) {
//   if (j == 0) return unexpected("divide by zero");
//   else return i / j;
// }
//
// void test() {
//   auto q = safe_divide(10, 0);
//   if (q.ok()) { printf("%f\n", q.value()); }
//   else { printf("%s\n", q.error().c_str()); }
// }
//
// Once the Android platform has moved to C++23, this will be removed and
// android::base::expected will be type aliased to std::expected.
//

namespace android {
namespace base {

// Synopsis
template <class T, class E>
class expected;

template <class E>
class unexpected;
template <class E>
unexpected(E) -> unexpected<E>;

template <class E>
class bad_expected_access;

template <>
class bad_expected_access<void>;

struct unexpect_t {
   explicit unexpect_t() = default;
};
inline constexpr unexpect_t unexpect{};

// macros for SFINAE
#define _ENABLE_IF(...) \
  , std::enable_if_t<(__VA_ARGS__)>* = nullptr

// Define NODISCARD_EXPECTED to prevent expected<T,E> from being
// ignored when used as a return value. This is off by default.
#ifdef NODISCARD_EXPECTED
#define _NODISCARD_ [[nodiscard]]
#else
#define _NODISCARD_
#endif

#define _EXPLICIT(cond)                                                     \
  _Pragma("clang diagnostic push")                                          \
  _Pragma("clang diagnostic ignored \"-Wc++20-extensions\"") explicit(cond) \
  _Pragma("clang diagnostic pop")

#define _COMMA ,

namespace expected_internal {

template <class T>
struct remove_cvref {
  using type = std::remove_cv_t<std::remove_reference_t<T>>;
};

template <class T>
using remove_cvref_t = typename remove_cvref<T>::type;

// Can T be constructed from W (or W converted to T)? W can be lvalue or rvalue,
// const or not.
template <class T, class W>
inline constexpr bool converts_from_any_cvref =
    std::disjunction_v<std::is_constructible<T, W&>, std::is_convertible<W&, T>,
                       std::is_constructible<T, W>, std::is_convertible<W, T>,
                       std::is_constructible<T, const W&>, std::is_convertible<const W&, T>,
                       std::is_constructible<T, const W>, std::is_convertible<const W, T>>;

template <class T>
struct is_expected : std::false_type {};

template <class T, class E>
struct is_expected<expected<T, E>> : std::true_type {};

template <class T>
inline constexpr bool is_expected_v = is_expected<T>::value;

template <class T>
struct is_unexpected : std::false_type {};

template <class E>
struct is_unexpected<unexpected<E>> : std::true_type {};

template <class T>
inline constexpr bool is_unexpected_v = is_unexpected<T>::value;

// Constraints on constructing an expected<T, ...> from an expected<U, G>
// related to T and U. UF is either "const U&" or "U".
template <class T, class U, class G, class UF>
inline constexpr bool convert_value_constraints =
    std::is_constructible_v<T, UF> &&
    (std::is_same_v<std::remove_cv_t<T>, bool> || !converts_from_any_cvref<T, expected<U, G>>);

// Constraints on constructing an expected<..., E> from an expected<U, G>
// related to E, G, and expected<U, G>. GF is either "const G&" or "G".
template <class E, class U, class G, class GF>
inline constexpr bool convert_error_constraints =
    std::is_constructible_v<E, GF> &&
    !std::is_constructible_v<unexpected<E>, expected<U, G>&> &&
    !std::is_constructible_v<unexpected<E>, expected<U, G>> &&
    !std::is_constructible_v<unexpected<E>, const expected<U, G>&> &&
    !std::is_constructible_v<unexpected<E>, const expected<U, G>>;

// If an exception is thrown in expected::operator=, while changing the expected
// object between a value and an error, the expected object is supposed to
// retain its original value, which is only possible if certain constructors
// are noexcept. This implementation doesn't try to be exception-safe, but
// enforce these constraints anyway because std::expected also will enforce
// them, and we intend to switch to it eventually.
template <class T, class E, class Self, class Value>
inline constexpr bool eh_assign_constraints =
    std::is_nothrow_constructible_v<Self, Value> ||
    std::is_nothrow_move_constructible_v<T> ||
    std::is_nothrow_move_constructible_v<E>;

// Implement expected<..., E>::expected([const] unexpected<G> [&/&&]).
#define _CONSTRUCT_EXPECTED_FROM_UNEXPECTED(GF, ParamType, forward_func)     \
  template <class G _ENABLE_IF(std::is_constructible_v<E, GF>)>              \
  constexpr _EXPLICIT((!std::is_convertible_v<GF, E>))                       \
      expected(ParamType e) noexcept(std::is_nothrow_constructible_v<E, GF>) \
      : var_(std::in_place_index<1>, forward_func(e.error())) {}

// Implement expected<..., E>::operator=([const] unexpected<G> [&/&&]).
#define _ASSIGN_UNEXPECTED_TO_EXPECTED(GF, ParamType, forward_func, extra_constraints)          \
  template <class G _ENABLE_IF(std::is_constructible_v<E, GF> &&                                \
                               std::is_assignable_v<E&, GF>) &&                                 \
                               extra_constraints>                                               \
  constexpr expected& operator=(ParamType e) noexcept(std::is_nothrow_constructible_v<E, GF> && \
                                                      std::is_nothrow_assignable_v<E&, GF>) {   \
    if (has_value()) {                                                                          \
      var_.template emplace<1>(forward_func(e.error()));                                        \
    } else {                                                                                    \
      error() = forward_func(e.error());                                                        \
    }                                                                                           \
    return *this;                                                                               \
  }

}  // namespace expected_internal

// Class expected
template <class T, class E>
class _NODISCARD_ expected {
  static_assert(std::is_object_v<T> && !std::is_array_v<T> &&
                    !std::is_same_v<std::remove_cv_t<T>, std::in_place_t> &&
                    !std::is_same_v<std::remove_cv_t<T>, unexpect_t> &&
                    !expected_internal::is_unexpected_v<std::remove_cv_t<T>>,
                "expected value type cannot be a reference, a function, an array, in_place_t, "
                "unexpect_t, or unexpected");

 public:
  using value_type = T;
  using error_type = E;
  using unexpected_type = unexpected<E>;

  template <class U>
  using rebind = expected<U, error_type>;

  // Delegate simple operations to the underlying std::variant. std::variant
  // doesn't set noexcept well, at least for copy ctor/assign, so set it
  // explicitly. Technically the copy/move assignment operators should also be
  // deleted if neither T nor E satisfies is_nothrow_move_constructible_v, but
  // that would require making these operator= methods into template functions.
  constexpr expected() = default;
  constexpr expected(const expected& rhs) noexcept(
      std::is_nothrow_copy_constructible_v<T> && std::is_nothrow_copy_constructible_v<E>) = default;
  constexpr expected(expected&& rhs) noexcept(std::is_nothrow_move_constructible_v<T> &&
                                              std::is_nothrow_move_constructible_v<E>) = default;
  constexpr expected& operator=(const expected& rhs) noexcept(
      std::is_nothrow_copy_constructible_v<T> && std::is_nothrow_copy_assignable_v<T> &&
      std::is_nothrow_copy_constructible_v<E> && std::is_nothrow_copy_assignable_v<E>) = default;
  constexpr expected& operator=(expected&& rhs) noexcept(
      std::is_nothrow_move_constructible_v<T> && std::is_nothrow_move_assignable_v<T> &&
      std::is_nothrow_move_constructible_v<E> && std::is_nothrow_move_assignable_v<E>) = default;

  // Construct this expected<T, E> from a different expected<U, G> type.
#define _CONVERTING_CTOR(UF, GF, ParamType, forward_func)                                      \
  template <class U,                                                                           \
            class G _ENABLE_IF(expected_internal::convert_value_constraints<T, U, G, UF> &&    \
                               expected_internal::convert_error_constraints<E, U, G, GF>)>     \
  constexpr _EXPLICIT((!std::is_convertible_v<UF, T> || !std::is_convertible_v<GF, E>))        \
      expected(ParamType rhs) noexcept(std::is_nothrow_constructible_v<T, UF> &&               \
                                       std::is_nothrow_constructible_v<E, GF>)                 \
      : var_(rhs.has_value() ? variant_type(std::in_place_index<0>, forward_func(rhs.value())) \
                             : variant_type(std::in_place_index<1>, forward_func(rhs.error()))) {}

  // NOLINTNEXTLINE(google-explicit-constructor)
  _CONVERTING_CTOR(const U&, const G&, const expected<U _COMMA G>&, )
  // NOLINTNEXTLINE(google-explicit-constructor)
  _CONVERTING_CTOR(U, G, expected<U _COMMA G>&&, std::move)

#undef _CONVERTING_CTOR

  // Construct from (converted) success value, using a forwarding reference.
  template <class U = T _ENABLE_IF(
                !std::is_same_v<expected_internal::remove_cvref_t<U>, std::in_place_t> &&
                !std::is_same_v<expected_internal::remove_cvref_t<U>, expected> &&
                !expected_internal::is_unexpected_v<expected_internal::remove_cvref_t<U>> &&
                std::is_constructible_v<T, U> &&
                (!std::is_same_v<std::remove_cv_t<T>, bool> ||
                 !expected_internal::is_expected_v<expected_internal::remove_cvref_t<U>>))>
  constexpr _EXPLICIT((!std::is_convertible_v<U, T>))
      // NOLINTNEXTLINE(google-explicit-constructor)
      expected(U&& v) noexcept(std::is_nothrow_constructible_v<T, U>)
      : var_(std::in_place_index<0>, std::forward<U>(v)) {}

  // NOLINTNEXTLINE(google-explicit-constructor)
  _CONSTRUCT_EXPECTED_FROM_UNEXPECTED(const G&, const unexpected<G>&, )
  // NOLINTNEXTLINE(google-explicit-constructor)
  _CONSTRUCT_EXPECTED_FROM_UNEXPECTED(G, unexpected<G>&&, std::move)

  // in_place_t construction
  template <class... Args _ENABLE_IF(std::is_constructible_v<T, Args...>)>
  constexpr explicit expected(std::in_place_t, Args&&... args)
      noexcept(std::is_nothrow_constructible_v<T, Args...>)
      : var_(std::in_place_index<0>, std::forward<Args>(args)...) {}

  // in_place_t with initializer_list construction
  template <class U, class... Args _ENABLE_IF(
      std::is_constructible_v<T, std::initializer_list<U>&, Args...>)>
  constexpr explicit expected(std::in_place_t, std::initializer_list<U> il, Args&&... args)
      noexcept(std::is_nothrow_constructible_v<T, std::initializer_list<U>&, Args...>)
      : var_(std::in_place_index<0>, il, std::forward<Args>(args)...) {}

  // unexpect_t construction
  template <class... Args _ENABLE_IF(std::is_constructible_v<E, Args...>)>
  constexpr explicit expected(unexpect_t, Args&&... args)
      noexcept(std::is_nothrow_constructible_v<E, Args...>)
      : var_(std::in_place_index<1>, unexpected_type(std::forward<Args>(args)...)) {}

  // unexpect_t with initializer_list construction
  template <class U, class... Args _ENABLE_IF(
      std::is_constructible_v<E, std::initializer_list<U>&, Args...>)>
  constexpr explicit expected(unexpect_t, std::initializer_list<U> il, Args&&... args)
      noexcept(std::is_nothrow_constructible_v<E, std::initializer_list<U>&, Args...>)
      : var_(std::in_place_index<1>, unexpected_type(il, std::forward<Args>(args)...)) {}

  // Assignment from (converted) success value, using a forwarding reference.
  template <class U = T _ENABLE_IF(
                !std::is_same_v<expected, expected_internal::remove_cvref_t<U>> &&
                !expected_internal::is_unexpected_v<expected_internal::remove_cvref_t<U>> &&
                std::is_constructible_v<T, U> && std::is_assignable_v<T&, U> &&
                expected_internal::eh_assign_constraints<T, E, T, U>)>
  constexpr expected& operator=(U&& v) noexcept(std::is_nothrow_constructible_v<T, U> &&
                                                std::is_nothrow_assignable_v<T&, U>) {
    if (has_value()) {
      value() = std::forward<U>(v);
    } else {
      var_.template emplace<0>(std::forward<U>(v));
    }
    return *this;
  }

  _ASSIGN_UNEXPECTED_TO_EXPECTED(const G&, const unexpected<G>&, ,
                                 (expected_internal::eh_assign_constraints<T, E, E, const G&>))
  _ASSIGN_UNEXPECTED_TO_EXPECTED(G, unexpected<G>&&, std::move,
                                 (expected_internal::eh_assign_constraints<T, E, E, G>))

  // modifiers
  template <class... Args _ENABLE_IF(std::is_nothrow_constructible_v<T, Args...>)>
  constexpr T& emplace(Args&&... args) noexcept {
    var_.template emplace<0>(std::forward<Args>(args)...);
    return value();
  }

  template <class U, class... Args _ENABLE_IF(
                         std::is_nothrow_constructible_v<T, std::initializer_list<U>&, Args...>)>
  constexpr T& emplace(std::initializer_list<U> il, Args&&... args) noexcept {
    var_.template emplace<0>(il, std::forward<Args>(args)...);
    return value();
  }

  // Swap. This function takes a template argument so that _ENABLE_IF works.
  template <class U = T _ENABLE_IF(
                std::is_same_v<U, T> &&
                    std::is_swappable_v<T> && std::is_swappable_v<E> &&
                    std::is_move_constructible_v<T> && std::is_move_constructible_v<E> &&
                (std::is_nothrow_move_constructible_v<T> ||
                 std::is_nothrow_move_constructible_v<E>))>
  constexpr void swap(expected& rhs) noexcept(std::is_nothrow_move_constructible_v<T> &&
                                              std::is_nothrow_swappable_v<T> &&
                                              std::is_nothrow_move_constructible_v<E> &&
                                              std::is_nothrow_swappable_v<E>) {
    var_.swap(rhs.var_);
  }

  // observers
  constexpr const T* operator->() const { return std::addressof(value()); }
  constexpr T* operator->() { return std::addressof(value()); }
  constexpr const T& operator*() const& { return value(); }
  constexpr T& operator*() & { return value(); }
  constexpr const T&& operator*() const&& { return std::move(std::get<T>(var_)); }
  constexpr T&& operator*() && { return std::move(std::get<T>(var_)); }

  constexpr bool has_value() const noexcept { return var_.index() == 0; }
  constexpr bool ok() const noexcept { return has_value(); }
  constexpr explicit operator bool() const noexcept { return has_value(); }

  constexpr const T& value() const& { return std::get<T>(var_); }
  constexpr T& value() & { return std::get<T>(var_); }
  constexpr const T&& value() const&& { return std::move(std::get<T>(var_)); }
  constexpr T&& value() && { return std::move(std::get<T>(var_)); }

  constexpr const E& error() const& { return std::get<unexpected_type>(var_).error(); }
  constexpr E& error() & { return std::get<unexpected_type>(var_).error(); }
  constexpr const E&& error() const&& { return std::move(std::get<unexpected_type>(var_)).error(); }
  constexpr E&& error() && { return std::move(std::get<unexpected_type>(var_)).error(); }

  template<class U _ENABLE_IF(
    std::is_copy_constructible_v<T> &&
    std::is_convertible_v<U, T>
  )>
  constexpr T value_or(U&& v) const& {
    if (has_value()) return value();
    else return static_cast<T>(std::forward<U>(v));
  }

  template<class U _ENABLE_IF(
    std::is_move_constructible_v<T> &&
    std::is_convertible_v<U, T>
  )>
  constexpr T value_or(U&& v) && {
    if (has_value()) return std::move(value());
    else return static_cast<T>(std::forward<U>(v));
  }

  // expected equality operators
  template<class T1, class E1, class T2, class E2>
  friend constexpr bool operator==(const expected<T1, E1>& x, const expected<T2, E2>& y);
  template<class T1, class E1, class T2, class E2>
  friend constexpr bool operator!=(const expected<T1, E1>& x, const expected<T2, E2>& y);

  // Comparison with unexpected<E>
  template<class T1, class E1, class E2>
  friend constexpr bool operator==(const expected<T1, E1>&, const unexpected<E2>&);
  template<class T1, class E1, class E2>
  friend constexpr bool operator==(const unexpected<E2>&, const expected<T1, E1>&);
  template<class T1, class E1, class E2>
  friend constexpr bool operator!=(const expected<T1, E1>&, const unexpected<E2>&);
  template<class T1, class E1, class E2>
  friend constexpr bool operator!=(const unexpected<E2>&, const expected<T1, E1>&);

 private:
  using variant_type = std::variant<value_type, unexpected_type>;
  variant_type var_;
};

template<class T1, class E1, class T2, class E2>
constexpr bool operator==(const expected<T1, E1>& x, const expected<T2, E2>& y) {
  if (x.has_value() != y.has_value()) return false;
  if (!x.has_value()) return x.error() == y.error();
  return *x == *y;
}

template<class T1, class E1, class T2, class E2>
constexpr bool operator!=(const expected<T1, E1>& x, const expected<T2, E2>& y) {
  return !(x == y);
}

// Comparison with unexpected<E>
template<class T1, class E1, class E2>
constexpr bool operator==(const expected<T1, E1>& x, const unexpected<E2>& y) {
  return !x.has_value() && (x.error() == y.error());
}
template<class T1, class E1, class E2>
constexpr bool operator==(const unexpected<E2>& x, const expected<T1, E1>& y) {
  return !y.has_value() && (x.error() == y.error());
}
template<class T1, class E1, class E2>
constexpr bool operator!=(const expected<T1, E1>& x, const unexpected<E2>& y) {
  return x.has_value() || (x.error() != y.error());
}
template<class T1, class E1, class E2>
constexpr bool operator!=(const unexpected<E2>& x, const expected<T1, E1>& y) {
  return y.has_value() || (x.error() != y.error());
}

template<class E>
class _NODISCARD_ expected<void, E> {
 public:
  using value_type = void;
  using error_type = E;
  using unexpected_type = unexpected<E>;

  template <class U>
  using rebind = expected<U, error_type>;

  // Delegate simple operations to the underlying std::variant.
  constexpr expected() = default;
  constexpr expected(const expected& rhs) noexcept(std::is_nothrow_copy_constructible_v<E>) =
      default;
  constexpr expected(expected&& rhs) noexcept(std::is_nothrow_move_constructible_v<E>) = default;
  constexpr expected& operator=(const expected& rhs) noexcept(
      std::is_nothrow_copy_constructible_v<E> && std::is_nothrow_copy_assignable_v<E>) = default;
  constexpr expected& operator=(expected&& rhs) noexcept(
      std::is_nothrow_move_constructible_v<E> && std::is_nothrow_move_assignable_v<E>) = default;

  // Construct this expected<void, E> from a different expected<U, G> type.
#define _CONVERTING_CTOR(GF, ParamType, forward_func)                                 \
  template <class U, class G _ENABLE_IF(std::is_void_v<U> &&                          \
        expected_internal::convert_error_constraints<E, U, G, GF>)>                   \
  constexpr _EXPLICIT((!std::is_convertible_v<GF, E>))                                \
      expected(ParamType rhs) noexcept(std::is_nothrow_constructible_v<E, GF>)        \
      : var_(rhs.has_value() ? variant_type(std::in_place_index<0>, std::monostate()) \
                             : variant_type(std::in_place_index<1>, forward_func(rhs.error()))) {}

  // NOLINTNEXTLINE(google-explicit-constructor)
  _CONVERTING_CTOR(const G&, const expected<U _COMMA G>&, )
  // NOLINTNEXTLINE(google-explicit-constructor)
  _CONVERTING_CTOR(G, expected<U _COMMA G>&&, std::move)

#undef _CONVERTING_CTOR

  // NOLINTNEXTLINE(google-explicit-constructor)
  _CONSTRUCT_EXPECTED_FROM_UNEXPECTED(const G&, const unexpected<G>&, )
  // NOLINTNEXTLINE(google-explicit-constructor)
  _CONSTRUCT_EXPECTED_FROM_UNEXPECTED(G, unexpected<G>&&, std::move)

  // in_place_t construction
  constexpr explicit expected(std::in_place_t) noexcept {}

  // unexpect_t construction
  template <class... Args _ENABLE_IF(std::is_constructible_v<E, Args...>)>
  constexpr explicit expected(unexpect_t, Args&&... args)
      noexcept(std::is_nothrow_constructible_v<E, Args...>)
      : var_(std::in_place_index<1>, unexpected_type(std::forward<Args>(args)...)) {}

  // unexpect_t with initializer_list construction
  template <class U, class... Args _ENABLE_IF(
                         std::is_constructible_v<E, std::initializer_list<U>&, Args...>)>
  constexpr explicit expected(unexpect_t, std::initializer_list<U> il, Args&&... args)
      noexcept(std::is_nothrow_constructible_v<E, std::initializer_list<U>&, Args...>)
      : var_(std::in_place_index<1>, unexpected_type(il, std::forward<Args>(args)...)) {}

  _ASSIGN_UNEXPECTED_TO_EXPECTED(const G&, const unexpected<G>&, , true)
  _ASSIGN_UNEXPECTED_TO_EXPECTED(G, unexpected<G>&&, std::move, true)

  // modifiers
  constexpr void emplace() noexcept { var_.template emplace<0>(std::monostate()); }

  // Swap. This function takes a template argument so that _ENABLE_IF works.
  template <class G = E _ENABLE_IF(std::is_same_v<G, E> &&
                                   std::is_swappable_v<E> && std::is_move_constructible_v<E>)>
  constexpr void swap(expected& rhs) noexcept(std::is_nothrow_move_constructible_v<E> &&
                                              std::is_nothrow_swappable_v<E>) {
    var_.swap(rhs.var_);
  }

  // observers
  constexpr bool has_value() const noexcept { return var_.index() == 0; }
  constexpr bool ok() const noexcept { return has_value(); }
  constexpr explicit operator bool() const noexcept { return has_value(); }

  constexpr void value() const& { if (!has_value()) std::get<0>(var_); }

  constexpr const E& error() const& { return std::get<1>(var_).error(); }
  constexpr E& error() & { return std::get<1>(var_).error(); }
  constexpr const E&& error() const&& { return std::move(std::get<1>(var_)).error(); }
  constexpr E&& error() && { return std::move(std::get<1>(var_)).error(); }

  // expected equality operators
  template<class E1, class E2>
  friend constexpr bool operator==(const expected<void, E1>& x, const expected<void, E2>& y);

 private:
  using variant_type = std::variant<std::monostate, unexpected_type>;
  variant_type var_;
};

template<class E1, class E2>
constexpr bool operator==(const expected<void, E1>& x, const expected<void, E2>& y) {
  if (x.has_value() != y.has_value()) return false;
  if (!x.has_value()) return x.error() == y.error();
  return true;
}

template<class T1, class E1, class E2>
constexpr bool operator==(const expected<T1, E1>& x, const expected<void, E2>& y) {
  if (x.has_value() != y.has_value()) return false;
  if (!x.has_value()) return x.error() == y.error();
  return false;
}

template<class E1, class T2, class E2>
constexpr bool operator==(const expected<void, E1>& x, const expected<T2, E2>& y) {
  if (x.has_value() != y.has_value()) return false;
  if (!x.has_value()) return x.error() == y.error();
  return false;
}

template <class T, class E, typename = decltype(
    std::declval<expected<T, E>&>().swap(std::declval<expected<T, E>&>()))>
constexpr void swap(expected<T, E>& x, expected<T, E>& y) noexcept(noexcept(x.swap(y))) {
  x.swap(y);
}

template<class E>
class unexpected {
  static_assert(std::is_object_v<E> && !std::is_array_v<E> && !std::is_const_v<E> &&
                    !std::is_volatile_v<E> && !expected_internal::is_unexpected_v<E>,
                "unexpected error type cannot be a reference, a function, an array, cv-qualified, "
                "or unexpected");

 public:
  // constructors
  constexpr unexpected(const unexpected&) = default;
  constexpr unexpected(unexpected&&) = default;

  template <class Err = E _ENABLE_IF(
                !std::is_same_v<expected_internal::remove_cvref_t<Err>, unexpected> &&
                !std::is_same_v<expected_internal::remove_cvref_t<Err>, std::in_place_t> &&
                std::is_constructible_v<E, Err>)>
  constexpr explicit unexpected(Err&& e) noexcept(std::is_nothrow_constructible_v<E, Err>)
      : val_(std::forward<Err>(e)) {}

  template <class... Args _ENABLE_IF(std::is_constructible_v<E, Args...>)>
  constexpr explicit unexpected(std::in_place_t, Args&&... args)
      noexcept(std::is_nothrow_constructible_v<E, Args...>)
      : val_(std::forward<Args>(args)...) {}

  template <class U, class... Args _ENABLE_IF(
                         std::is_constructible_v<E, std::initializer_list<U>&, Args...>)>
  constexpr explicit unexpected(std::in_place_t, std::initializer_list<U> il, Args&&... args)
      noexcept(std::is_nothrow_constructible_v<E, std::initializer_list<U>&, Args...>)
      : val_(il, std::forward<Args>(args)...) {}

  constexpr unexpected& operator=(const unexpected&) = default;
  constexpr unexpected& operator=(unexpected&&) = default;

  // observer
  constexpr const E& error() const& noexcept { return val_; }
  constexpr E& error() & noexcept { return val_; }
  constexpr const E&& error() const&& noexcept { return std::move(val_); }
  constexpr E&& error() && noexcept { return std::move(val_); }

  // Swap. This function takes a template argument so that _ENABLE_IF works.
  template <typename G = E _ENABLE_IF(std::is_same_v<G, E> && std::is_swappable_v<E>)>
  void swap(unexpected& other) noexcept(std::is_nothrow_swappable_v<E>) {
    // Make std::swap visible to provide swap for STL and builtin types, but use
    // an unqualified swap to invoke argument-dependent lookup to find the swap
    // functions for user-declared types.
    using std::swap;
    swap(val_, other.val_);
  }

  template<class E1, class E2>
  friend constexpr bool
  operator==(const unexpected<E1>& e1, const unexpected<E2>& e2);
  template<class E1, class E2>
  friend constexpr bool
  operator!=(const unexpected<E1>& e1, const unexpected<E2>& e2);

 private:
  E val_;
};

template<class E1, class E2>
constexpr bool
operator==(const unexpected<E1>& e1, const unexpected<E2>& e2) {
  return e1.error() == e2.error();
}

template<class E1, class E2>
constexpr bool
operator!=(const unexpected<E1>& e1, const unexpected<E2>& e2) {
  return e1.error() != e2.error();
}

template <class E1 _ENABLE_IF(std::is_swappable_v<E1>)>
void swap(unexpected<E1>& x, unexpected<E1>& y) noexcept(noexcept(x.swap(y))) {
  x.swap(y);
}

// TODO: bad_expected_access class

#undef _ENABLE_IF
#undef _NODISCARD_
#undef _EXPLICIT
#undef _COMMA
#undef _CONSTRUCT_EXPECTED_FROM_UNEXPECTED
#undef _ASSIGN_UNEXPECTED_TO_EXPECTED

}  // namespace base
}  // namespace android
