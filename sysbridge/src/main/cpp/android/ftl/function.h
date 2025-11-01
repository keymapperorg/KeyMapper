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

#include <cstddef>
#include <functional>
#include <type_traits>
#include <utility>

#include <ftl/details/function.h>

namespace android::ftl {

// ftl::Function<F, N> is a container for function object, and can mostly be used in place of
// std::function<F>.
//
// Unlike std::function<F>, a ftl::Function<F, N>:
//
//  * Uses a static amount of memory (controlled by N), and never any dynamic allocation.
//  * Satisfies the std::is_trivially_copyable<> trait.
//  * Satisfies the std::is_trivially_destructible<> trait.
//
// However those same limits are also required from the contained function object in turn.
//
// The size of a ftl::Function<F, N> is guaranteed to be:
//
//     sizeof(std::intptr_t) * (N + 2)
//
// A ftl::Function<F, N> can always be implicitly converted to a larger size ftl::Function<F, M>.
// Trying to convert the other way leads to a compilation error.
//
// A default-constructed ftl::Function is in an empty state. The operator bool() overload returns
// false in this state. It is undefined behavior to attempt to invoke the function in this state.
//
// The ftl::Function<F, N> can also be constructed or assigned from ftl::no_op. This sets up the
// ftl::Function to be non-empty, with a function that when called does nothing except
// default-constructs a return value.
//
// The ftl::make_function() helpers construct a ftl::Function<F, N>, including deducing the
// values of F and N from the arguments it is given.
//
// The static ftl::Function<F, N>::make() helpers construct a ftl::Function<F, N> without that
// deduction, and also allow for implicit argument conversion if the target being called needs them.
//
// The construction helpers allow any of the following types of functions to be stored:
//
//  * Any SMALL function object (as defined by the C++ Standard), such as a lambda with a small
//    capture, or other "functor". The requirements are:
//
//      1) The function object must be trivial to destroy (in fact, the destructor will never
//         actually be called once copied to the internal storage).
//      2) The function object must be trivial to copy (the raw bytes will be copied as the
//         ftl::Function<F, N> is copied/moved).
//      3) The size of the function object cannot be larger than sizeof(std::intptr_t) * (N + 1),
//         and it cannot require stricter alignment than alignof(std::intptr_t).
//
//    With the default of N=0, a lambda can only capture a single pointer-sized argument. This is
//    enough to capture `this`, which is why N=0 is the default.
//
//  * A member function, with the address passed as the template value argument to the construction
//    helper function, along with the instance pointer needed to invoke it passed as an ordinary
//    argument.
//
//        ftl::make_function<&Class::member_function>(this);
//
//    Note that the indicated member function will be invoked non-virtually. If you need it to be
//    invoked virtually, you should invoke it yourself with a small lambda like so:
//
//        ftl::function([this] { virtual_member_function(); });
//
//  * An ordinary function ("free function"), with the address of the function passed as a template
//    value argument.
//
//        ftl::make_function<&std::atoi>();
//
//   As with the member function helper, as the function is known at compile time, it will be called
//   directly.
//
// Example usage:
//
//   class MyClass {
//    public:
//     void on_event() const {}
//     int on_string(int*, std::string_view) { return 1; }
//
//     auto get_function() {
//       return ftl::function([this] { on_event(); });
//     }
//   } cls;
//
//   // A function container with no arguments, and returning no value.
//   ftl::Function<void()> f;
//
//   // Construct a ftl::Function containing a small lambda.
//   f = cls.get_function();
//
//   // Construct a ftl::Function that calls `cls.on_event()`.
//   f = ftl::function<&MyClass::on_event>(&cls);
//
//   // Create a do-nothing function.
//   f = ftl::no_op;
//
//   // Invoke the contained function.
//   f();
//
//   // Also invokes it.
//   std::invoke(f);
//
//   // Create a typedef to give a more meaningful name and bound the size.
//   using MyFunction = ftl::Function<int(std::string_view), 2>;
//   int* ptr = nullptr;
//   auto f1 = MyFunction::make(
//       [cls = &cls, ptr](std::string_view sv) {
//           return cls->on_string(ptr, sv);
//       });
//   int r = f1("abc"sv);
//
//   // Returns a default-constructed int (0).
//   f1 = ftl::no_op;
//   r = f1("abc"sv);
//   assert(r == 0);

    template<typename F, std::size_t N = 0>
    class Function;

// Used to construct a Function that does nothing.
    struct NoOpTag {
    };

    constexpr NoOpTag no_op;

// Detects that a type is a `ftl::Function<F, N>` regardless of what `F` and `N` are.
    template<typename>
    struct is_function : public std::false_type {
    };

    template<typename F, std::size_t N>
    struct is_function<Function<F, N>> : public std::true_type {
    };

    template<typename T>
    constexpr bool is_function_v = is_function<T>::value;

    template<typename Ret, typename... Args, std::size_t N>
    class Function<Ret(Args...), N> final {
        // Enforce a valid size, with an arbitrary maximum allowed size for the container of
        // sizeof(std::intptr_t) * 16, though that maximum can be relaxed.
        static_assert(N <= details::kFunctionMaximumN);

        using OpaqueStorageTraits = details::function_opaque_storage<N>;

    public:
        // Defining result_type allows ftl::Function to be substituted for std::function.
        using result_type = Ret;

        // Constructs an empty ftl::Function.
        Function() = default;

        // Constructing or assigning from nullptr_t also creates an empty ftl::Function.
        Function(std::nullptr_t) {}

        Function &operator=(std::nullptr_t) { return *this = Function(nullptr); }

        // Constructing from NoOpTag sets up a a special no-op function which is valid to call, and which
        // returns a default constructed return value.
        Function(NoOpTag) : function_(details::bind_opaque_no_op<Ret, Args...>()) {}

        Function &operator=(NoOpTag) { return *this = Function(no_op); }

        // Constructing/assigning from a function object stores a copy of that function object, however:
        //  * It must be trivially copyable, as the implementation makes a copy with memcpy().
        //  * It must be trivially destructible, as the implementation doesn't destroy the copy!
        //  * It must fit in the limited internal storage, which enforces size/alignment restrictions.

        template<typename F, typename = std::enable_if_t<std::is_invocable_r_v<Ret, F, Args...>>>
        Function(const F &f)
                : opaque_(OpaqueStorageTraits::opaque_copy(f)),
                  function_(details::bind_opaque_function_object<F, Ret, Args...>(f)) {}

        template<typename F, typename = std::enable_if_t<std::is_invocable_r_v<Ret, F, Args...>>>
        Function &operator=(const F &f) noexcept {
            return *this = Function{OpaqueStorageTraits::opaque_copy(f),
                                    details::bind_opaque_function_object<F, Ret, Args...>(f)};
        }

        // Constructing/assigning from a smaller ftl::Function is allowed, but not anything else.

        template<std::size_t M>
        Function(const Function<Ret(Args...), M> &other)
                : opaque_{OpaqueStorageTraits::opaque_copy(other.opaque_)},
                  function_(other.function_) {}

        template<std::size_t M>
        auto &operator=(const Function<Ret(Args...), M> &other) {
            return *this = Function{OpaqueStorageTraits::opaque_copy(other.opaque_),
                                    other.function_};
        }

        // Returns true if a function is set.
        explicit operator bool() const { return function_ != nullptr; }

        // Checks if the other function has the same contents as this one.
        bool operator==(const Function &other) const {
            return other.opaque_ == opaque_ && other.function_ == function_;
        }

        bool operator!=(const Function &other) const { return !operator==(other); }

        // Alternative way of testing for a function being set.
        bool operator==(std::nullptr_t) const { return function_ == nullptr; }

        bool operator!=(std::nullptr_t) const { return function_ != nullptr; }

        // Invokes the function.
        Ret operator()(Args... args) const {
            return std::invoke(function_, opaque_.data(), std::forward<Args>(args)...);
        }

        // Creation helper for function objects, such as lambdas.
        template<typename F>
        static auto make(const F &f) -> decltype(Function{f}) {
            return Function{f};
        }

        // Creation helper for a class pointer and a compile-time chosen member function to call.
        template<auto MemberFunction, typename Class>
        static auto make(Class *instance) -> decltype(Function{
                details::bind_member_function<MemberFunction>(instance,
                                                              static_cast<Ret (*)(
                                                                      Args...)>(nullptr))}) {
            return Function{details::bind_member_function<MemberFunction>(
                    instance, static_cast<Ret (*)(Args...)>(nullptr))};
        }

        // Creation helper for a compile-time chosen free function to call.
        template<auto FreeFunction>
        static auto make() -> decltype(Function{
                details::bind_free_function<FreeFunction>(
                        static_cast<Ret (*)(Args...)>(nullptr))}) {
            return Function{
                    details::bind_free_function<FreeFunction>(
                            static_cast<Ret (*)(Args...)>(nullptr))};
        }

    private:
        // Needed so a Function<F, M> can be converted to a Function<F, N>.
        template<typename, std::size_t>
        friend
        class Function;

        // The function pointer type of function stored in `function_`. The first argument is always
        // `&opaque_`.
        using StoredFunction = Ret(void *, Args...);

        // The type of the opaque storage, used to hold an appropriate function object.
        // The type stored here is ONLY known to the StoredFunction.
        // We always use at least one std::intptr_t worth of storage, and always a multiple of that size.
        using OpaqueStorage = typename OpaqueStorageTraits::type;

        // Internal constructor for creating from a raw opaque blob + function pointer.
        Function(const OpaqueStorage &opaque, StoredFunction *function)
                : opaque_(opaque), function_(function) {}

        // Note: `mutable` so that `operator() const` can use it.
        mutable OpaqueStorage opaque_{};
        StoredFunction *function_{nullptr};
    };

// Makes a ftl::Function given a function object `F`.
    template<typename F, typename T = details::function_traits<F>>
    Function(const F &) -> Function<typename T::type, T::size>;

    template<typename F>
    auto make_function(const F &f) -> decltype(Function{f}) {
        return Function{f};
    }

// Makes a ftl::Function given a `MemberFunction` and a instance pointer to the associated `Class`.
    template<auto MemberFunction, typename Class>
    auto make_function(Class *instance)
    -> decltype(Function{details::bind_member_function<MemberFunction>(
            instance,
            static_cast<details::remove_member_function_pointer_t<MemberFunction> *>(nullptr))}) {
        return Function{details::bind_member_function<MemberFunction>(
                instance,
                static_cast<details::remove_member_function_pointer_t<MemberFunction> *>(nullptr))};
    }

// Makes a ftl::Function given an ordinary free function.
    template<auto FreeFunction>
    auto make_function() -> decltype(Function{
            details::bind_free_function<FreeFunction>(
                    static_cast<decltype(FreeFunction)>(nullptr))}) {
        return Function{
                details::bind_free_function<FreeFunction>(
                        static_cast<decltype(FreeFunction)>(nullptr))};
    }

}  // namespace android::ftl
