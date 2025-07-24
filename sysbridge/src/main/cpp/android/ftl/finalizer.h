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

#include <cstddef>

#include <functional>
#include <type_traits>
#include <utility>

#include <ftl/function.h>

namespace android::ftl {

// An RAII wrapper that invokes a function object as a finalizer when destroyed.
//
// The function object must take no arguments, and must return void. If the function object needs
// any context for the call, it must store it itself, for example with a lambda capture.
//
// The stored function object will be called once (unless canceled via the `cancel()` member
// function) at the first of:
//
//   - The Finalizer instance is destroyed.
//   - `operator()` is used to invoke the contained function.
//   - The Finalizer instance is move-assigned a new value. The function being replaced will be
//     invoked, and the replacement will be stored to be called later.
//
// The intent with this class is to keep cleanup code next to the code that requires that
// cleanup be performed.
//
//   bool read_file(std::string filename) {
//     FILE* f = fopen(filename.c_str(), "rb");
//     if (f == nullptr) return false;
//     const auto cleanup = ftl::Finalizer([f]() { fclose(f); });
//     // fread(...), etc
//     return true;
//   }
//
// The `FinalFunction` template argument to Finalizer<FinalFunction> allows a polymorphic function
// type for storing the finalization function, such as `std::function` or `ftl::Function`.
//
// For convenience, this header defines a few useful aliases for using those types.
//
//   - `FinalizerStd`, an alias for `Finalizer<std::function<void()>>`
//   - `FinalizerFtl`, an alias for `Finalizer<ftl::Function<void()>>`
//   - `FinalizerFtl1`, an alias for `Finalizer<ftl::Function<void(), 1>>`
//   - `FinalizerFtl2`, an alias for `Finalizer<ftl::Function<void(), 2>>`
//   - `FinalizerFtl3`, an alias for `Finalizer<ftl::Function<void(), 3>>`
//
// Clients of this header are free to define other aliases they need.
//
// A Finalizer that uses a polymorphic function type can be returned from a function call and/or
// stored as member data (to be destroyed along with the containing class).
//
//   auto register(Observer* observer) -> ftl::FinalizerStd<void()> {
//      const auto id = observers.add(observer);
//      return ftl::Finalizer([id]() { observers.remove(id); });
//   }
//
//   {
//     const auto _ = register(observer);
//     // do the things that required the registered observer.
//   }
//   // the observer is removed.
//
// Cautions:
//
//   1. When a Finalizer is stored as member data, you will almost certainly want that cleanup to
//      happen first, before the rest of the other member data is destroyed. For safety you should
//      assume that the finalization function will access that data directly or indirectly.
//
//      This means that Finalizers should be defined last, after all other normal member data in a
//      class.
//
//          class MyClass {
//           public:
//            bool initialize() {
//              ready_ = true;
//              cleanup_ = ftl::Finalizer([this]() { ready_ = false; });
//              return true;
//            }
//
//            bool ready_ = false;
//
//            // Finalizers should be last so other class members can be accessed before being
//            // destroyed.
//            ftl::FinalizerStd<void()> cleanup_;
//          };
//
//   2. Care must be taken to use `ftl::Finalizer()` when constructing locally from a lambda. If you
//      forget to do so, you are just creating a lambda that won't be automatically invoked!
//
//          const auto bad = [&counter](){ ++counter; }; // Just a lambda instance
//          const auto good = ftl::Finalizer([&counter](){ ++counter; });
//
    template<typename FinalFunction>
    class Finalizer final {
        // requires(std::is_invocable_r_v<void, FinalFunction>)
        static_assert(std::is_invocable_r_v<void, FinalFunction>);

    public:
        // A default constructed Finalizer does nothing when destroyed.
        // requires(std::is_default_constructible_v<FinalFunction>)
        constexpr Finalizer() = default;

        // Constructs a Finalizer from a function object.
        // requires(std::is_invocable_v<F>)
        template<typename F, typename = std::enable_if_t<std::is_invocable_v<F>>>
        [[nodiscard]] explicit constexpr Finalizer(F &&function)
                : Finalizer(std::forward<F>(function), false) {}

        constexpr ~Finalizer() { maybe_invoke(); }

        // Disallow copying.
        Finalizer(const Finalizer &that) = delete;

        auto operator=(const Finalizer &that) = delete;

        // Move construction
        // requires(std::is_move_constructible_v<FinalFunction>)
        [[nodiscard]] constexpr Finalizer(Finalizer &&that)
                : Finalizer(std::move(that.function_), std::exchange(that.canceled_, true)) {}

        // Implicit conversion move construction
        // requires(!std::is_same_v<Finalizer, Finalizer<F>>)
        template<typename F, typename = std::enable_if_t<!std::is_same_v<Finalizer, Finalizer<F>>>>
        // NOLINTNEXTLINE(google-explicit-constructor, cppcoreguidelines-rvalue-reference-param-not-moved)
        [[nodiscard]] constexpr Finalizer(Finalizer<F> &&that)
                : Finalizer(std::move(that.function_), std::exchange(that.canceled_, true)) {}

        // Move assignment
        // requires(std::is_move_assignable_v<FinalFunction>)
        constexpr auto operator=(Finalizer &&that) -> Finalizer & {
            maybe_invoke();

            function_ = std::move(that.function_);
            canceled_ = std::exchange(that.canceled_, true);

            return *this;
        }

        // Implicit conversion move assignment
        // requires(!std::is_same_v<Finalizer, Finalizer<F>>)
        template<typename F, typename = std::enable_if_t<!std::is_same_v<Finalizer, Finalizer<F>>>>
        // NOLINTNEXTLINE(cppcoreguidelines-rvalue-reference-param-not-moved)
        constexpr auto operator=(Finalizer<F> &&that) -> Finalizer & {
            *this = Finalizer(std::move(that.function_), std::exchange(that.canceled_, true));
            return *this;
        }

        // Cancels the final function, preventing it from being invoked.
        constexpr void cancel() {
            canceled_ = true;
            maybe_nullify_function();
        }

        // Invokes the final function now, if not already invoked.
        constexpr void operator()() { maybe_invoke(); }

    private:
        template<typename>
        friend
        class Finalizer;

        template<typename F, typename = std::enable_if_t<std::is_invocable_v<F>>>
        [[nodiscard]] explicit constexpr Finalizer(F &&function, bool canceled)
                : function_(std::forward<F>(function)), canceled_(canceled) {}

        constexpr void maybe_invoke() {
            if (!std::exchange(canceled_, true)) {
                std::invoke(function_);
                maybe_nullify_function();
            }
        }

        constexpr void maybe_nullify_function() {
            // Sets function_ to nullptr if that is supported for the backing type.
            if constexpr (std::is_assignable_v<FinalFunction, nullptr_t>) {
                function_ = nullptr;
            }
        }

        FinalFunction function_;
        bool canceled_ = true;
    };

    template<typename F>
    Finalizer(F &&) -> Finalizer<std::decay_t<F>>;

// A standard alias for using `std::function` as the polymorphic function type.
    using FinalizerStd = Finalizer<std::function<void()>>;

// Helpful aliases for using `ftl::Function` as the polymorphic function type.
    using FinalizerFtl = Finalizer<Function<void()>>;
    using FinalizerFtl1 = Finalizer<Function<void(), 1>>;
    using FinalizerFtl2 = Finalizer<Function<void(), 2>>;
    using FinalizerFtl3 = Finalizer<Function<void(), 3>>;

}  // namespace android::ftl