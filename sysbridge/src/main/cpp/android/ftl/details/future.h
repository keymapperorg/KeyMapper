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

namespace android::ftl {

    template<typename, template<typename> class>
    class Future;

    namespace details {

        template<typename T>
        struct future_result {
            using type = T;
        };

        template<typename T>
        struct future_result<std::future < T>> {
        using type = T;
    };

    template<typename T>
    struct future_result<std::shared_future < T>> {
    using type = T;
};

template<typename T, template<typename> class FutureImpl>
struct future_result<Future < T, FutureImpl>> {
using type = T;
};

template<typename T>
using future_result_t = typename future_result<T>::type;

struct ValueTag {
};

template<typename, typename T, template<typename> class>
class BaseFuture;

template<typename Self, typename T>
class BaseFuture<Self, T, std::future> {
    using Impl = std::future<T>;

public:
    Future <T, std::shared_future> share() {
        if (T *value = std::get_if<T>(&self())) {
            return {ValueTag{}, std::move(*value)};
        }

        return std::get<Impl>(self()).share();
    }

protected:
    T get() {
        if (T *value = std::get_if<T>(&self())) {
            return std::move(*value);
        }

        return std::get<Impl>(self()).get();
    }

    template<class Rep, class Period>
    std::future_status wait_for(const std::chrono::duration <Rep, Period> &timeout_duration) const {
        if (std::holds_alternative<T>(self())) {
            return std::future_status::ready;
        }

        return std::get<Impl>(self()).wait_for(timeout_duration);
    }

private:
    auto &self() { return static_cast<Self &>(*this).future_; }

    const auto &self() const { return static_cast<const Self &>(*this).future_; }
};

template<typename Self, typename T>
class BaseFuture<Self, T, std::shared_future> {
    using Impl = std::shared_future<T>;

protected:
    const T &get() const {
        if (const T *value = std::get_if<T>(&self())) {
            return *value;
        }

        return std::get<Impl>(self()).get();
    }

    template<class Rep, class Period>
    std::future_status wait_for(const std::chrono::duration <Rep, Period> &timeout_duration) const {
        if (std::holds_alternative<T>(self())) {
            return std::future_status::ready;
        }

        return std::get<Impl>(self()).wait_for(timeout_duration);
    }

private:
    const auto &self() const { return static_cast<const Self &>(*this).future_; }
};

}  // namespace details
}  // namespace android::ftl
