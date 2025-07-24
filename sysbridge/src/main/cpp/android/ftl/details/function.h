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

#include <array>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <type_traits>

namespace android::ftl::details {

// The maximum allowed value for the template argument `N` in
// `ftl::Function<F, N>`.
    constexpr size_t kFunctionMaximumN = 14;

// Converts a member function pointer type `Ret(Class::*)(Args...)` to an equivalent non-member
// function type `Ret(Args...)`.

    template<typename>
    struct remove_member_function_pointer;

    template<typename Class, typename Ret, typename... Args>
    struct remove_member_function_pointer<Ret (Class::*)(Args...)> {
        using type = Ret(Args...);
    };

    template<typename Class, typename Ret, typename... Args>
    struct remove_member_function_pointer<Ret (Class::*)(Args...) const> {
        using type = Ret(Args...);
    };

    template<auto MemberFunction>
    using remove_member_function_pointer_t =
            typename remove_member_function_pointer<decltype(MemberFunction)>::type;

// Helper functions for binding to the supported targets.

    template<typename Ret, typename... Args>
    auto bind_opaque_no_op() -> Ret (*)(void *, Args...) {
        return [](void *, Args...) -> Ret {
            if constexpr (!std::is_void_v<Ret>) {
                return Ret{};
            }
        };
    }

    template<typename F, typename Ret, typename... Args>
    auto bind_opaque_function_object(const F &) -> Ret (*)(void *, Args...) {
        return [](void *opaque, Args... args) -> Ret {
            return std::invoke(*static_cast<F *>(opaque), std::forward<Args>(args)...);
        };
    }

    template<auto MemberFunction, typename Class, typename Ret, typename... Args>
    auto bind_member_function(Class *instance, Ret (*)(Args...) = nullptr) {
        return [instance](Args... args) -> Ret {
            return std::invoke(MemberFunction, instance, std::forward<Args>(args)...);
        };
    }

    template<auto FreeFunction, typename Ret, typename... Args>
    auto bind_free_function(Ret (*)(Args...) = nullptr) {
        return [](Args... args) -> Ret {
            return std::invoke(FreeFunction, std::forward<Args>(args)...);
        };
    }

// Traits class for the opaque storage used by Function.

    template<std::size_t N>
    struct function_opaque_storage {
        // The actual type used for the opaque storage. An `N` of zero specifies the minimum useful size,
        // which allows a lambda with zero or one capture args.
        using type = std::array<std::intptr_t, N + 1>;

        template<typename S>
        static constexpr bool require_trivially_copyable = std::is_trivially_copyable_v<S>;

        template<typename S>
        static constexpr bool require_trivially_destructible = std::is_trivially_destructible_v<S>;

        template<typename S>
        static constexpr bool require_will_fit_in_opaque_storage = sizeof(S) <= sizeof(type);

        template<typename S>
        static constexpr bool require_alignment_compatible =
                std::alignment_of_v<S> <= std::alignment_of_v<type>;

        // Copies `src` into the opaque storage, and returns that storage.
        template<typename S>
        static type opaque_copy(const S &src) {
            // TODO: Replace with C++20 concepts/constraints which can give more details.
            static_assert(require_trivially_copyable<S>,
                          "ftl::Function can only store lambdas that capture trivially copyable data.");
            static_assert(
                    require_trivially_destructible<S>,
                    "ftl::Function can only store lambdas that capture trivially destructible data.");
            static_assert(require_will_fit_in_opaque_storage<S>,
                          "ftl::Function has limited storage for lambda captured state. Maybe you need to "
                          "increase N?");
            static_assert(require_alignment_compatible<S>);

            type opaque;
            std::memcpy(opaque.data(), &src, sizeof(S));
            return opaque;
        }
    };

// Traits class to help determine the template parameters to use for a ftl::Function, given a
// function object.

    template<typename F, typename = decltype(&F::operator())>
    struct function_traits {
        // The function type `F` with which to instantiate the `Function<F, N>` template.
        using type = remove_member_function_pointer_t<&F::operator()>;

        // The (minimum) size `N` with which to instantiate the `Function<F, N>` template.
        static constexpr std::size_t size =
                (std::max(sizeof(std::intptr_t), sizeof(F)) - 1) / sizeof(std::intptr_t);
    };

}  // namespace android::ftl::details
