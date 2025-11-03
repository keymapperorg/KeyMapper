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

#include <ftl/details/mixins.h>

namespace android::ftl {

// CRTP mixins for defining type-safe wrappers that are distinct from their underlying type. Common
// uses are IDs, opaque handles, and physical quantities. The constructor is provided by (and must
// be inherited from) the `Constructible` mixin, whereas operators (equality, ordering, arithmetic,
// etc.) are enabled through inheritance:
//
//   struct Id : ftl::Constructible<Id, std::int32_t>, ftl::Equatable<Id> {
//     using Constructible::Constructible;
//   };
//
//   static_assert(!std::is_default_constructible_v<Id>);
//
// Unlike `Constructible`, `DefaultConstructible` allows default construction. The default value is
// zero-initialized unless specified:
//
//   struct Color : ftl::DefaultConstructible<Color, std::uint8_t>,
//                  ftl::Equatable<Color>,
//                  ftl::Orderable<Color> {
//     using DefaultConstructible::DefaultConstructible;
//   };
//
//   static_assert(Color() == Color(0u));
//   static_assert(ftl::to_underlying(Color(-1)) == 255u);
//   static_assert(Color(1u) < Color(2u));
//
//   struct Sequence : ftl::DefaultConstructible<Sequence, std::int8_t, -1>,
//                     ftl::Equatable<Sequence>,
//                     ftl::Orderable<Sequence>,
//                     ftl::Incrementable<Sequence> {
//     using DefaultConstructible::DefaultConstructible;
//   };
//
//   static_assert(Sequence() == Sequence(-1));
//
// The underlying type need not be a fundamental type:
//
//   struct Timeout : ftl::DefaultConstructible<Timeout, std::chrono::seconds, 10>,
//                    ftl::Equatable<Timeout>,
//                    ftl::Addable<Timeout> {
//     using DefaultConstructible::DefaultConstructible;
//   };
//
//   using namespace std::chrono_literals;
//   static_assert(Timeout() + Timeout(5s) == Timeout(15s));
//
    template<typename Self, typename T>
    struct Constructible {
        explicit constexpr Constructible(T value) : value_(value) {}

        explicit constexpr operator const T &() const { return value_; }

    private:
        template<typename, template<typename> class>
        friend
        class details::Mixin;

        T value_;
    };

    template<typename Self, typename T, auto kDefault = T{}>
    struct DefaultConstructible : Constructible<Self, T> {
        using Constructible<Self, T>::Constructible;

        constexpr DefaultConstructible() : DefaultConstructible(T{kDefault}) {}
    };

// Shorthand for casting a type-safe wrapper to its underlying value.
    template<typename Self, typename T>
    constexpr const T &to_underlying(const Constructible<Self, T> &c) {
        return static_cast<const T &>(c);
    }

// Comparison operators for equality.
    template<typename Self>
    struct Equatable : details::Mixin<Self, Equatable> {
        constexpr bool operator==(const Self &other) const {
            return to_underlying(this->self()) == to_underlying(other);
        }

        constexpr bool operator!=(const Self &other) const { return !(*this == other); }
    };

// Comparison operators for ordering.
    template<typename Self>
    struct Orderable : details::Mixin<Self, Orderable> {
        constexpr bool operator<(const Self &other) const {
            return to_underlying(this->self()) < to_underlying(other);
        }

        constexpr bool operator>(const Self &other) const { return other < this->self(); }

        constexpr bool operator>=(const Self &other) const { return !(*this < other); }

        constexpr bool operator<=(const Self &other) const { return !(*this > other); }
    };

// Pre-increment and post-increment operators.
    template<typename Self>
    struct Incrementable : details::Mixin<Self, Incrementable> {
        constexpr Self &operator++() {
            ++this->mut();
            return this->self();
        }

        constexpr Self operator++(int) {
            const Self tmp = this->self();
            operator++();
            return tmp;
        }
    };

// Additive operators, including incrementing.
    template<typename Self>
    struct Addable : details::Mixin<Self, Addable>, Incrementable<Self> {
        constexpr Self &operator+=(const Self &other) {
            this->mut() += to_underlying(other);
            return this->self();
        }

        constexpr Self operator+(const Self &other) const {
            Self tmp = this->self();
            return tmp += other;
        }

    private:
        using Base = details::Mixin<Self, Addable>;
        using Base::mut;
        using Base::self;
    };

}  // namespace android::ftl
