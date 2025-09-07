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

#include <future>
#include <type_traits>
#include <utility>
#include <variant>

#include <ftl/details/future.h>

namespace android::ftl {

// Thin wrapper around FutureImpl<T> (concretely std::future<T> or std::shared_future<T>) with
// extensions for pure values (created via ftl::yield) and continuations.
//
// See also SharedFuture<T> shorthand below.
//
    template<typename T, template<typename> class FutureImpl = std::future>
    class Future final : public details::BaseFuture<Future<T, FutureImpl>, T, FutureImpl> {
        using Base = details::BaseFuture<Future, T, FutureImpl>;

        friend Base;                                            // For BaseFuture<...>::self.
        friend details::BaseFuture<Future<T>, T, std::future>;  // For BaseFuture<...>::share.

    public:
        // Constructs an invalid future.
        Future() : future_(std::in_place_type<FutureImpl<T>>) {}

        // Constructs a future from its standard counterpart, implicitly.
        Future(FutureImpl<T> &&f) : future_(std::move(f)) {}

        bool valid() const {
            return std::holds_alternative<T>(future_) || std::get<FutureImpl<T>>(future_).valid();
        }

        // Forwarding functions. Base::share is only defined when FutureImpl is std::future, whereas the
        // following are defined for either FutureImpl:
        using Base::get;
        using Base::wait_for;

        // Attaches a continuation to the future. The continuation is a function that maps T to either R
        // or ftl::Future<R>. In the former case, the chain wraps the result in a future as if by
        // ftl::yield.
        //
        //   auto future = ftl::yield(123);
        //   ftl::Future<char> futures[] = {ftl::yield('a'), ftl::yield('b')};
        //
        //   auto chain =
        //       ftl::Future(std::move(future))
        //           .then([](int x) { return static_cast<std::size_t>(x % 2); })
        //           .then([&futures](std::size_t i) { return std::move(futures[i]); });
        //
        //   assert(chain.get() == 'b');
        //
        template<typename F, typename R = std::invoke_result_t<F, T>>
        auto then(F &&op) && -> Future<details::future_result_t<R>> {
            return defer(
                    [](auto &&f, F &&op) {
                        R r = op(f.get());
                        if constexpr (std::is_same_v<R, details::future_result_t<R>>) {
                            return r;
                        } else {
                            return r.get();
                        }
                    },
                    std::move(*this), std::forward<F>(op));
        }

    private:
        template<typename V>
        friend Future<V> yield(V &&);

        template<typename V, typename... Args>
        friend Future<V> yield(Args &&...);

        template<typename... Args>
        Future(details::ValueTag, Args &&... args)
                : future_(std::in_place_type<T>, std::forward<Args>(args)...) {}

        std::variant<T, FutureImpl<T>> future_;
    };

    template<typename T>
    using SharedFuture = Future<T, std::shared_future>;

// Deduction guide for implicit conversion.
    template<typename T, template<typename> class FutureImpl>
    Future(FutureImpl<T> &&) -> Future<T, FutureImpl>;

// Creates a future that wraps a value.
//
//   auto future = ftl::yield(42);
//   assert(future.get() == 42);
//
//   auto ptr = std::make_unique<char>('!');
//   auto future = ftl::yield(std::move(ptr));
//   assert(*future.get() == '!');
//
    template<typename V>
    inline Future<V> yield(V &&value) {
        return {details::ValueTag{}, std::move(value)};
    }

    template<typename V, typename... Args>
    inline Future<V> yield(Args &&... args) {
        return {details::ValueTag{}, std::forward<Args>(args)...};
    }

// Creates a future that defers a function call until its result is queried.
//
//   auto future = ftl::defer([](int x) { return x + 1; }, 99);
//   assert(future.get() == 100);
//
    template<typename F, typename... Args>
    inline auto defer(F &&f, Args &&... args) {
        return Future(
                std::async(std::launch::deferred, std::forward<F>(f), std::forward<Args>(args)...));
    }

}  // namespace android::ftl
