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

#include <shared_mutex>

namespace android::ftl {

// Wrapper around std::shared_mutex to provide capabilities for thread-safety
// annotations.
// TODO(b/257958323): This class is no longer needed once b/135688034 is fixed (currently blocked on
// b/175635923).
    class [[clang::capability("shared_mutex")]] SharedMutex final {
    public:
        [[clang::acquire_capability()]] void lock() {
            mutex_.lock();
        }

        [[clang::release_capability()]] void unlock() {
            mutex_.unlock();
        }

        [[clang::acquire_shared_capability()]] void lock_shared() {
            mutex_.lock_shared();
        }

        [[clang::release_shared_capability()]] void unlock_shared() {
            mutex_.unlock_shared();
        }

    private:
        std::shared_mutex mutex_;
    };

}  // namespace android::ftl
