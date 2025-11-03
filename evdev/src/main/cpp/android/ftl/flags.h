/*
 * Copyright 2020 The Android Open Source Project
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

#include <ftl/enum.h>
#include <ftl/string.h>

#include <bitset>
#include <cstdint>
#include <iterator>
#include <initializer_list>
#include <string>
#include <type_traits>

// TODO(b/185536303): Align with FTL style.

namespace android::ftl {

/* A class for handling flags defined by an enum or enum class in a type-safe way. */
    template<typename F>
    class Flags {
        // F must be an enum or its underlying type is undefined. Theoretically we could specialize this
        // further to avoid this restriction but in general we want to encourage the use of enums
        // anyways.
        static_assert(std::is_enum_v<F>, "Flags type must be an enum");
        using U = std::underlying_type_t<F>;

    public:
        constexpr Flags(F f) : mFlags(static_cast<U>(f)) {}

        constexpr Flags(std::initializer_list<F> fs) : mFlags(combine(fs)) {}

        constexpr Flags() : mFlags(0) {}

        constexpr Flags(const Flags<F> &f) : mFlags(f.mFlags) {}

        // Provide a non-explicit construct for non-enum classes since they easily convert to their
        // underlying types (e.g. when used with bitwise operators). For enum classes, however, we
        // should force them to be explicitly constructed from their underlying types to make full use
        // of the type checker.
        template<typename T = U>
        constexpr Flags(T t, std::enable_if_t<!is_scoped_enum_v<F>, T> * = nullptr) : mFlags(t) {}

        template<typename T = U>
        explicit constexpr Flags(T t, std::enable_if_t<is_scoped_enum_v<F>, T> * = nullptr)
                : mFlags(t) {}

        class Iterator {
            using Bits = std::uint64_t;
            static_assert(sizeof(U) <= sizeof(Bits));

        public:
            constexpr Iterator() = default;

            Iterator(Flags<F> flags) : mRemainingFlags(flags.mFlags) { (*this)++; }

            // Pre-fix ++
            Iterator &operator++() {
                if (mRemainingFlags.none()) {
                    mCurrFlag = 0;
                } else {
                    // TODO: Replace with std::countr_zero in C++20.
                    const Bits bit = static_cast<Bits>(__builtin_ctzll(
                            mRemainingFlags.to_ullong()));
                    mRemainingFlags.reset(static_cast<std::size_t>(bit));
                    mCurrFlag = static_cast<U>(static_cast<Bits>(1) << bit);
                }
                return *this;
            }

            // Post-fix ++
            Iterator operator++(int) {
                Iterator iter = *this;
                ++*this;
                return iter;
            }

            bool operator==(Iterator other) const {
                return mCurrFlag == other.mCurrFlag && mRemainingFlags == other.mRemainingFlags;
            }

            bool operator!=(Iterator other) const { return !(*this == other); }

            F operator*() const { return F{mCurrFlag}; }

            // iterator traits

            // In the future we could make this a bidirectional const iterator instead of a forward
            // iterator but it doesn't seem worth the added complexity at this point. This could not,
            // however, be made a non-const iterator as assigning one flag to another is a non-sensical
            // operation.
            using iterator_category = std::input_iterator_tag;
            using value_type = F;
            // Per the C++ spec, because input iterators are not assignable the iterator's reference
            // type does not actually need to be a reference. In fact, making it a reference would imply
            // that modifying it would change the underlying Flags object, which is obviously wrong for
            // the same reason this can't be a non-const iterator.
            using reference = F;
            using difference_type = void;
            using pointer = void;

        private:
            std::bitset<sizeof(Bits) * 8> mRemainingFlags;
            U mCurrFlag = 0;
        };

        /*
         * Tests whether the given flag is set.
         */
        bool test(F flag) const {
            U f = static_cast<U>(flag);
            return (f & mFlags) == f;
        }

        /* Tests whether any of the given flags are set */
        bool any(Flags<F> f = ~Flags<F>()) const { return (mFlags & f.mFlags) != 0; }

        /* Tests whether all of the given flags are set */
        bool all(Flags<F> f) const { return (mFlags & f.mFlags) == f.mFlags; }

        constexpr Flags<F> operator|(Flags<F> rhs) const {
            return static_cast<F>(mFlags | rhs.mFlags);
        }

        Flags<F> &operator|=(Flags<F> rhs) {
            mFlags = mFlags | rhs.mFlags;
            return *this;
        }

        Flags<F> operator&(Flags<F> rhs) const { return static_cast<F>(mFlags & rhs.mFlags); }

        Flags<F> &operator&=(Flags<F> rhs) {
            mFlags = mFlags & rhs.mFlags;
            return *this;
        }

        Flags<F> operator^(Flags<F> rhs) const { return static_cast<F>(mFlags ^ rhs.mFlags); }

        Flags<F> &operator^=(Flags<F> rhs) {
            mFlags = mFlags ^ rhs.mFlags;
            return *this;
        }

        Flags<F> operator~() { return static_cast<F>(~mFlags); }

        bool operator==(Flags<F> rhs) const { return mFlags == rhs.mFlags; }

        bool operator!=(Flags<F> rhs) const { return !operator==(rhs); }

        Flags<F> &operator=(const Flags<F> &rhs) {
            mFlags = rhs.mFlags;
            return *this;
        }

        inline Flags<F> &clear(Flags<F> f = static_cast<F>(~static_cast<U>(0))) {
            return *this &= ~f;
        }

        Iterator begin() const { return Iterator(*this); }

        Iterator end() const { return Iterator(); }

        /*
         * Returns the stored set of flags.
         *
         * Note that this returns the underlying type rather than the base enum class. This is because
         * the value is no longer necessarily a strict member of the enum since the returned value could
         * be multiple enum variants OR'd together.
         */
        U get() const { return mFlags; }

        std::string string() const {
            std::string result;
            bool first = true;
            U unstringified = 0;
            for (const F f: *this) {
                if (const auto flagName = flag_name(f)) {
                    appendFlag(result, flagName.value(), first);
                } else {
                    unstringified |= static_cast<U>(f);
                }
            }

            if (unstringified != 0) {
                constexpr auto radix = sizeof(U) == 1 ? Radix::kBin : Radix::kHex;
                appendFlag(result, to_string(unstringified, radix), first);
            }

            if (first) {
                result += "0x0";
            }

            return result;
        }

    private:
        U mFlags;

        static constexpr U combine(std::initializer_list<F> fs) {
            U result = 0;
            for (const F f: fs) {
                result |= static_cast<U>(f);
            }
            return result;
        }

        static void appendFlag(std::string &str, const std::string_view &flag, bool &first) {
            if (first) {
                first = false;
            } else {
                str += " | ";
            }
            str += flag;
        }
    };

// This namespace provides operator overloads for enum classes to make it easier to work with them
// as flags. In order to use these, add them via a `using namespace` declaration.
    namespace flag_operators {

        template<typename F, typename = std::enable_if_t<is_scoped_enum_v<F>>>
        inline Flags<F> operator~(F f) {
            return static_cast<F>(~to_underlying(f));
        }

        template<typename F, typename = std::enable_if_t<is_scoped_enum_v<F>>>
        constexpr Flags<F> operator|(F lhs, F rhs) {
            return static_cast<F>(to_underlying(lhs) | to_underlying(rhs));
        }

    } // namespace flag_operators
} // namespace android::ftl
