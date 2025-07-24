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

namespace android::ftl::details {

    template<typename Self, template<typename> class>
    class Mixin {
    protected:
        constexpr Self &self() { return *static_cast<Self *>(this); }

        constexpr const Self &self() const { return *static_cast<const Self *>(this); }

        constexpr auto &mut() { return self().value_; }
    };

}  // namespace android::ftl::details
