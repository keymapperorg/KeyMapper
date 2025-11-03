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

#include <cstdlib>
#include <type_traits>
#include <utility>

namespace android::ftl {

// Enforces and documents non-null pre/post-condition for (raw or smart) pointers.
//
//   void get_length(const ftl::NonNull<std::shared_ptr<std::string>>& string_ptr,
//                   ftl::NonNull<std::size_t*> length_ptr) {
//     // No need for `nullptr` checks.
//     *length_ptr = string_ptr->length();
//   }
//
//   const auto string_ptr = ftl::as_non_null(std::make_shared<std::string>("android"));
//   std::size_t size;
//   get_length(string_ptr, ftl::as_non_null(&size));
//   assert(size == 7u);
//
// For compatibility with std::unique_ptr<T> and performance with std::shared_ptr<T>, move
// operations are allowed despite breaking the invariant:
//
//   using Pair = std::pair<ftl::NonNull<std::shared_ptr<int>>, std::shared_ptr<int>>;
//
//   Pair dupe_if(ftl::NonNull<std::unique_ptr<int>> non_null_ptr, bool condition) {
//     // Move the underlying pointer out, so `non_null_ptr` must not be accessed after this point.
//     auto unique_ptr = std::move(non_null_ptr).take();
//
//     auto non_null_shared_ptr = ftl::as_non_null(std::shared_ptr<int>(std::move(unique_ptr)));
//     auto nullable_shared_ptr = condition ? non_null_shared_ptr.get() : nullptr;
//
//     return {std::move(non_null_shared_ptr), std::move(nullable_shared_ptr)};
//   }
//
//   auto ptr = ftl::as_non_null(std::make_unique<int>(42));
//   const auto [ptr1, ptr2] = dupe_if(std::move(ptr), true);
//   assert(ptr1.get() == ptr2);
//
    template<typename Pointer>
    class NonNull final {
        struct Passkey {
        };

    public:
        // Disallow `nullptr` explicitly for clear compilation errors.
        NonNull() = delete;

        NonNull(std::nullptr_t) = delete;

        // Copy operations.

        constexpr NonNull(const NonNull &) = default;

        constexpr NonNull &operator=(const NonNull &) = default;

        template<typename U, typename = std::enable_if_t<std::is_convertible_v<U, Pointer>>>
        constexpr NonNull(const NonNull<U> &other) : pointer_(other.get()) {}

        template<typename U, typename = std::enable_if_t<std::is_convertible_v<U, Pointer>>>
        constexpr NonNull &operator=(const NonNull<U> &other) {
            pointer_ = other.get();
            return *this;
        }

        [[nodiscard]] constexpr const Pointer &get() const { return pointer_; }

        [[nodiscard]] constexpr explicit operator const Pointer &() const { return get(); }

        // Move operations. These break the invariant, so care must be taken to avoid subsequent access.

        constexpr NonNull(NonNull &&) = default;

        constexpr NonNull &operator=(NonNull &&) = default;

        [[nodiscard]] constexpr Pointer take() &&{ return std::move(pointer_); }

        [[nodiscard]] constexpr explicit operator Pointer() && { return take(); }

        // Dereferencing.
        [[nodiscard]] constexpr decltype(auto) operator*() const { return *get(); }

        [[nodiscard]] constexpr decltype(auto) operator->() const { return get(); }

        [[nodiscard]] constexpr explicit operator bool() const { return !(pointer_ == nullptr); }

        // Private constructor for ftl::as_non_null. Excluded from candidate constructors for conversions
        // through the passkey idiom, for clear compilation errors.
        template<typename P>
        constexpr NonNull(Passkey, P &&pointer) : pointer_(std::forward<P>(pointer)) {
            if (pointer_ == nullptr) std::abort();
        }

    private:
        template<typename P>
        friend constexpr auto as_non_null(P &&) -> NonNull<std::decay_t<P>>;

        Pointer pointer_;
    };

    template<typename P>
    [[nodiscard]] constexpr auto as_non_null(P &&pointer) -> NonNull<std::decay_t<P>> {
        using Passkey = typename NonNull<std::decay_t<P>>::Passkey;
        return {Passkey{}, std::forward<P>(pointer)};
    }

// NonNull<P> <=> NonNull<Q>

    template<typename P, typename Q>
    constexpr bool operator==(const NonNull<P> &lhs, const NonNull<Q> &rhs) {
        return lhs.get() == rhs.get();
    }

    template<typename P, typename Q>
    constexpr bool operator!=(const NonNull<P> &lhs, const NonNull<Q> &rhs) {
        return !operator==(lhs, rhs);
    }

    template<typename P, typename Q>
    constexpr bool operator<(const NonNull<P> &lhs, const NonNull<Q> &rhs) {
        return lhs.get() < rhs.get();
    }

    template<typename P, typename Q>
    constexpr bool operator<=(const NonNull<P> &lhs, const NonNull<Q> &rhs) {
        return lhs.get() <= rhs.get();
    }

    template<typename P, typename Q>
    constexpr bool operator>=(const NonNull<P> &lhs, const NonNull<Q> &rhs) {
        return lhs.get() >= rhs.get();
    }

    template<typename P, typename Q>
    constexpr bool operator>(const NonNull<P> &lhs, const NonNull<Q> &rhs) {
        return lhs.get() > rhs.get();
    }

// NonNull<P> <=> Q

    template<typename P, typename Q>
    constexpr bool operator==(const NonNull<P> &lhs, const Q &rhs) {
        return lhs.get() == rhs;
    }

    template<typename P, typename Q>
    constexpr bool operator!=(const NonNull<P> &lhs, const Q &rhs) {
        return lhs.get() != rhs;
    }

    template<typename P, typename Q>
    constexpr bool operator<(const NonNull<P> &lhs, const Q &rhs) {
        return lhs.get() < rhs;
    }

    template<typename P, typename Q>
    constexpr bool operator<=(const NonNull<P> &lhs, const Q &rhs) {
        return lhs.get() <= rhs;
    }

    template<typename P, typename Q>
    constexpr bool operator>=(const NonNull<P> &lhs, const Q &rhs) {
        return lhs.get() >= rhs;
    }

    template<typename P, typename Q>
    constexpr bool operator>(const NonNull<P> &lhs, const Q &rhs) {
        return lhs.get() > rhs;
    }

// P <=> NonNull<Q>

    template<typename P, typename Q>
    constexpr bool operator==(const P &lhs, const NonNull<Q> &rhs) {
        return lhs == rhs.get();
    }

    template<typename P, typename Q>
    constexpr bool operator!=(const P &lhs, const NonNull<Q> &rhs) {
        return lhs != rhs.get();
    }

    template<typename P, typename Q>
    constexpr bool operator<(const P &lhs, const NonNull<Q> &rhs) {
        return lhs < rhs.get();
    }

    template<typename P, typename Q>
    constexpr bool operator<=(const P &lhs, const NonNull<Q> &rhs) {
        return lhs <= rhs.get();
    }

    template<typename P, typename Q>
    constexpr bool operator>=(const P &lhs, const NonNull<Q> &rhs) {
        return lhs >= rhs.get();
    }

    template<typename P, typename Q>
    constexpr bool operator>(const P &lhs, const NonNull<Q> &rhs) {
        return lhs > rhs.get();
    }

}  // namespace android::ftl

// Specialize std::hash for ftl::NonNull<T>
template<typename P>
struct std::hash<android::ftl::NonNull<P>> {
    std::size_t operator()(const android::ftl::NonNull<P> &ptr) const {
        return std::hash<P>()(ptr.get());
    }
};
