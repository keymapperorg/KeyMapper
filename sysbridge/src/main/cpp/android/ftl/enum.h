/*
 * Copyright 2021 The Android Open Source Project
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

#include <cstddef>
#include <limits>
#include <optional>
#include <string_view>
#include <type_traits>
#include <utility>

#include <ftl/string.h>

// Returns the name of enumerator E::V and optionally the class (i.e. "E::V" or "V") as
// std::optional<std::string_view> by parsing the compiler-generated string literal for the
// signature of this function. The function is defined in the global namespace with a short name
// and inferred return type to reduce bloat in the read-only data segment.
template<bool S, typename E, E V>
constexpr auto ftl_enum_builder() {
    static_assert(std::is_enum_v<E>);

    using R = std::optional<std::string_view>;
    using namespace std::literals;

    // The "pretty" signature has the following format:
    //
    //   auto ftl_enum() [E = android::test::Enum, V = android::test::Enum::kValue]
    //
    std::string_view view = __PRETTY_FUNCTION__;
    const auto template_begin = view.rfind('[');
    const auto template_end = view.rfind(']');
    if (template_begin == view.npos || template_end == view.npos) return R{};

    // Extract the template parameters without the enclosing brackets. Example (cont'd):
    //
    //   E = android::test::Enum, V = android::test::Enum::kValue
    //
    view = view.substr(template_begin + 1, template_end - template_begin - 1);
    const auto value_begin = view.rfind("V = "sv);
    if (value_begin == view.npos) return R{};

    // Example (cont'd):
    //
    //   V = android::test::Enum::kValue
    //
    view = view.substr(value_begin);
    const auto pos = S ? view.rfind("::"sv) - 2 : view.npos;

    const auto name_begin = view.rfind("::"sv, pos);
    if (name_begin == view.npos) return R{};

    // Chop off the leading "::".
    const auto name = view.substr(name_begin + 2);

    // A value that is not enumerated has the format "Enum)42".
    return name.find(')') == view.npos ? R{name} : R{};
}

// Returns the name of enumerator E::V (i.e. "V") as std::optional<std::string_view>
template<typename E, E V>
constexpr auto ftl_enum() {
    return ftl_enum_builder<false, E, V>();
}

// Returns the name of enumerator and class E::V (i.e. "E::V") as std::optional<std::string_view>
template<typename E, E V>
constexpr auto ftl_enum_full() {
    return ftl_enum_builder<true, E, V>();
}

namespace android::ftl {

// Trait for determining whether a type is specifically a scoped enum or not. By definition, a
// scoped enum is one that is not implicitly convertible to its underlying type.
//
// TODO: Replace with std::is_scoped_enum in C++23.
//
    template<typename T, bool = std::is_enum_v<T>>
    struct is_scoped_enum : std::false_type {
    };

    template<typename T>
    struct is_scoped_enum<T, true>
            : std::negation<std::is_convertible<T, std::underlying_type_t<T>>> {
    };

    template<typename T>
    inline constexpr bool is_scoped_enum_v = is_scoped_enum<T>::value;

// Shorthand for casting an enumerator to its integral value.
//
// TODO: Replace with std::to_underlying in C++23.
//
//   enum class E { A, B, C };
//   static_assert(ftl::to_underlying(E::B) == 1);
//
    template<typename E, typename = std::enable_if_t<std::is_enum_v<E>>>
    constexpr auto to_underlying(E v) {
        return static_cast<std::underlying_type_t<E>>(v);
    }

// Traits for retrieving an enum's range. An enum specifies its range by defining enumerators named
// ftl_first and ftl_last. If omitted, ftl_first defaults to 0, whereas ftl_last defaults to N - 1
// where N is the bit width of the underlying type, but only if that type is unsigned, assuming the
// enumerators are flags. Also, note that unscoped enums must define both bounds, as casting out-of-
// range values results in undefined behavior if the underlying type is not fixed.
//
//   enum class E { A, B, C, F = 5, ftl_last = F };
//
//   static_assert(ftl::enum_begin_v<E> == E::A);
//   static_assert(ftl::enum_last_v<E> == E::F);
//   static_assert(ftl::enum_size_v<E> == 6);
//
//   enum class F : std::uint16_t { X = 0b1, Y = 0b10, Z = 0b100 };
//
//   static_assert(ftl::enum_begin_v<F> == F{0});
//   static_assert(ftl::enum_last_v<F> == F{15});
//   static_assert(ftl::enum_size_v<F> == 16);
//
    template<typename E, typename = void>
    struct enum_begin {
        static_assert(is_scoped_enum_v<E>, "Missing ftl_first enumerator");
        static constexpr E value{0};
    };

    template<typename E>
    struct enum_begin<E, std::void_t<decltype(E::ftl_first)>> {
        static constexpr E value = E::ftl_first;
    };

    template<typename E>
    inline constexpr E enum_begin_v = enum_begin<E>::value;

    template<typename E, typename = void>
    struct enum_end {
        using U = std::underlying_type_t<E>;
        static_assert(is_scoped_enum_v<E> && std::is_unsigned_v<U>, "Missing ftl_last enumerator");

        static constexpr E value{std::numeric_limits<U>::digits};
    };

    template<typename E>
    struct enum_end<E, std::void_t<decltype(E::ftl_last)>> {
        static constexpr E value = E{to_underlying(E::ftl_last) + 1};
    };

    template<typename E>
    inline constexpr E enum_end_v = enum_end<E>::value;

    template<typename E>
    inline constexpr E enum_last_v = E{to_underlying(enum_end_v<E>) - 1};

    template<typename E>
    struct enum_size {
        static constexpr auto kBegin = to_underlying(enum_begin_v<E>);
        static constexpr auto kEnd = to_underlying(enum_end_v<E>);
        static_assert(kBegin < kEnd, "Invalid range");

        static constexpr std::size_t value = kEnd - kBegin;
        static_assert(value <= 64, "Excessive range size");
    };

    template<typename E>
    inline constexpr std::size_t enum_size_v = enum_size<E>::value;

    namespace details {

        template<auto V>
        struct Identity {
            static constexpr auto value = V;
        };

        template<typename E>
        using make_enum_sequence = std::make_integer_sequence<std::underlying_type_t<E>, enum_size_v<E>>;

        template<typename E, template<E> class = Identity, typename = make_enum_sequence<E>>
        struct EnumRange;

        template<typename E, template<E> class F, typename T, T... Vs>
        struct EnumRange<E, F, std::integer_sequence<T, Vs...>> {
            static constexpr auto kBegin = to_underlying(enum_begin_v<E>);
            static constexpr auto kSize = enum_size_v<E>;

            using R = decltype(F<E{}>::value);
            const R values[kSize] = {F<static_cast<E>(Vs + kBegin)>::value...};

            constexpr const auto *begin() const { return values; }

            constexpr const auto *end() const { return values + kSize; }
        };

        template<auto V>
        struct EnumName {
            static constexpr auto value = ftl_enum<decltype(V), V>();
        };

        template<auto V>
        struct EnumNameFull {
            static constexpr auto value = ftl_enum_full<decltype(V), V>();
        };

        template<auto I>
        struct FlagName {
            using E = decltype(I);
            using U = std::underlying_type_t<E>;

            static constexpr E V{U{1} << to_underlying(I)};
            static constexpr auto value = ftl_enum<E, V>();
        };

    }  // namespace details

// Returns an iterable over the range of an enum.
//
//   enum class E { A, B, C, F = 5, ftl_last = F };
//
//   std::string string;
//   for (E v : ftl::enum_range<E>()) {
//     string += ftl::enum_name(v).value_or("?");
//   }
//
//   assert(string == "ABC??F");
//
    template<typename E>
    constexpr auto enum_range() {
        return details::EnumRange<E>{};
    }

// Returns a stringified enumerator at compile time.
//
//   enum class E { A, B, C };
//   static_assert(ftl::enum_name<E::B>() == "B");
//
    template<auto V>
    constexpr std::string_view enum_name() {
        constexpr auto kName = ftl_enum<decltype(V), V>();
        static_assert(kName, "Unknown enumerator");
        return *kName;
    }

// Returns a stringified enumerator with class at compile time.
//
//   enum class E { A, B, C };
//   static_assert(ftl::enum_name<E::B>() == "E::B");
//
    template<auto V>
    constexpr std::string_view enum_name_full() {
        constexpr auto kName = ftl_enum_full<decltype(V), V>();
        static_assert(kName, "Unknown enumerator");
        return *kName;
    }

// Returns a stringified enumerator, possibly at compile time.
//
//   enum class E { A, B, C, F = 5, ftl_last = F };
//
//   static_assert(ftl::enum_name(E::C).value_or("?") == "C");
//   static_assert(ftl::enum_name(E{3}).value_or("?") == "?");
//
    template<typename E>
    constexpr std::optional<std::string_view> enum_name(E v) {
        const auto value = to_underlying(v);

        constexpr auto kBegin = to_underlying(enum_begin_v<E>);
        constexpr auto kLast = to_underlying(enum_last_v<E>);
        if (value < kBegin || value > kLast) return {};

        constexpr auto kRange = details::EnumRange<E, details::EnumName>{};
        return kRange.values[value - kBegin];
    }

// Returns a stringified enumerator with class, possibly at compile time.
//
//   enum class E { A, B, C, F = 5, ftl_last = F };
//
//   static_assert(ftl::enum_name(E::C).value_or("?") == "E::C");
//   static_assert(ftl::enum_name(E{3}).value_or("?") == "?");
//
    template<typename E>
    constexpr std::optional<std::string_view> enum_name_full(E v) {
        const auto value = to_underlying(v);

        constexpr auto kBegin = to_underlying(enum_begin_v<E>);
        constexpr auto kLast = to_underlying(enum_last_v<E>);
        if (value < kBegin || value > kLast) return {};

        constexpr auto kRange = details::EnumRange<E, details::EnumNameFull>{};
        return kRange.values[value - kBegin];
    }

// Returns a stringified flag enumerator, possibly at compile time.
//
//   enum class F : std::uint16_t { X = 0b1, Y = 0b10, Z = 0b100 };
//
//   static_assert(ftl::flag_name(F::Z).value_or("?") == "Z");
//   static_assert(ftl::flag_name(F{0b111}).value_or("?") == "?");
//
    template<typename E>
    constexpr std::optional<std::string_view> flag_name(E v) {
        const auto value = to_underlying(v);

        // TODO: Replace with std::popcount and std::countr_zero in C++20.
        if (__builtin_popcountll(value) != 1) return {};

        constexpr auto kRange = details::EnumRange<E, details::FlagName>{};
        return kRange.values[__builtin_ctzll(value)];
    }

// Returns a stringified enumerator, or its integral value if not named.
//
//   enum class E { A, B, C, F = 5, ftl_last = F };
//
//   assert(ftl::enum_string(E::C) == "C");
//   assert(ftl::enum_string(E{3}) == "3");
//
    template<typename E>
    inline std::string enum_string(E v) {
        if (const auto name = enum_name(v)) {
            return std::string(*name);
        }
        return to_string(to_underlying(v));
    }

// Returns a stringified enumerator with class, or its integral value if not named.
//
//   enum class E { A, B, C, F = 5, ftl_last = F };
//
//   assert(ftl::enum_string(E::C) == "E::C");
//   assert(ftl::enum_string(E{3}) == "3");
//
    template<typename E>
    inline std::string enum_string_full(E v) {
        if (const auto name = enum_name_full(v)) {
            return std::string(*name);
        }
        return to_string(to_underlying(v));
    }

// Returns a stringified flag enumerator, or its integral value if not named.
//
//   enum class F : std::uint16_t { X = 0b1, Y = 0b10, Z = 0b100 };
//
//   assert(ftl::flag_string(F::Z) == "Z");
//   assert(ftl::flag_string(F{7}) == "0b111");
//
    template<typename E>
    inline std::string flag_string(E v) {
        if (const auto name = flag_name(v)) {
            return std::string(*name);
        }
        constexpr auto radix = sizeof(E) == 1 ? Radix::kBin : Radix::kHex;
        return to_string(to_underlying(v), radix);
    }

}  // namespace android::ftl
