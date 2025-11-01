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

#define FTL_ATTRIBUTE(a) __attribute__((a))

namespace android::ftl {

// Granular alternative to [[clang::no_thread_safety_analysis]]. Given a std::mutex-like object,
// FakeGuard suppresses enforcement of thread-safe access to guarded variables within its scope.
// While FakeGuard is scoped to a block, there are macro shorthands for a single expression, as
// well as function/lambda scope (though calls must be indirect, e.g. virtual or std::function):
//
//   struct {
//     std::mutex mutex;
//     int x FTL_ATTRIBUTE(guarded_by(mutex)) = -1;
//
//     int f() {
//       {
//         ftl::FakeGuard guard(mutex);
//         x = 0;
//       }
//
//       return FTL_FAKE_GUARD(mutex, x + 1);
//     }
//
//      std::function<int()> g() const {
//        return [this]() FTL_FAKE_GUARD(mutex) { return x; };
//      }
//   } s;
//
//   assert(s.f() == 1);
//   assert(s.g()() == 0);
//
// An example of a situation where FakeGuard helps is a mutex that guards writes on Thread 1, and
// reads on Thread 2. Reads on Thread 1, which is the only writer, need not be under lock, so can
// use FakeGuard to appease the thread safety analyzer. Another example is enforcing and documenting
// exclusive access by a single thread. This is done by defining a global constant that represents a
// thread context, and annotating guarded variables as if it were a mutex (though without any effect
// at run time):
//
//   constexpr class [[clang::capability("mutex")]] {
//   } kMainThreadContext;
//
    template<typename Mutex>
    struct [[clang::scoped_lockable]] FakeGuard final {
        explicit FakeGuard(const Mutex &mutex) FTL_ATTRIBUTE(acquire_capability(mutex)) {}

        [[clang::release_capability()]] ~FakeGuard() {}

        FakeGuard(const FakeGuard &) = delete;

        FakeGuard &operator=(const FakeGuard &) = delete;
    };

}  // namespace android::ftl

// TODO: Enable in C++23 once standard attributes can be used on lambdas.
#if 0
#define FTL_FAKE_GUARD1(mutex) [[using clang: acquire_capability(mutex), release_capability(mutex)]]
#else
#define FTL_FAKE_GUARD1(mutex)             \
  FTL_ATTRIBUTE(acquire_capability(mutex)) \
  FTL_ATTRIBUTE(release_capability(mutex))
#endif

#define FTL_FAKE_GUARD2(mutex, expr) \
    (android::ftl::FakeGuard(mutex), expr)

#define FTL_MAKE_FAKE_GUARD(arg1, arg2, guard, ...) guard

#define FTL_FAKE_GUARD(...) \
  FTL_MAKE_FAKE_GUARD(__VA_ARGS__, FTL_FAKE_GUARD2, FTL_FAKE_GUARD1, )(__VA_ARGS__)
