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

#include <ftl/initializer_list.h>
#include <ftl/optional.h>
#include <ftl/small_vector.h>

#include <algorithm>
#include <functional>
#include <type_traits>
#include <utility>

namespace android::ftl {

// Associative container with unique, unordered keys. Unlike std::unordered_map, key-value pairs are
// stored in contiguous storage for cache efficiency. The map is allocated statically until its size
// exceeds N, at which point mappings are relocated to dynamic memory. The try_emplace operation has
// a non-standard analogue try_replace that destructively emplaces. The API also defines an in-place
// counterpart to insert_or_assign: emplace_or_replace. Lookup is done not via a subscript operator,
// but immutable getters that can optionally transform the value.
//
// SmallMap<K, V, 0> unconditionally allocates on the heap.
//
// Example usage:
//
//   ftl::SmallMap<int, std::string, 3> map;
//   assert(map.empty());
//   assert(!map.dynamic());
//
//   map = ftl::init::map<int, std::string>(123, "abc")(-1)(42, 3u, '?');
//   assert(map.size() == 3u);
//   assert(!map.dynamic());
//
//   assert(map.contains(123));
//   assert(map.get(42).transform([](const std::string& s) { return s.size(); }) == 3u);
//
//   const auto opt = map.get(-1);
//   assert(opt);
//
//   std::string& ref = *opt;
//   assert(ref.empty());
//   ref = "xyz";
//
//   map.emplace_or_replace(0, "vanilla", 2u, 3u);
//   assert(map.dynamic());
//
//   assert(map == SmallMap(ftl::init::map(-1, "xyz"sv)(0, "nil"sv)(42, "???"sv)(123, "abc"sv)));
//
    template<typename K, typename V, std::size_t N, typename KeyEqual = std::equal_to<K>>
    class SmallMap final {
        using Map = SmallVector<std::pair<const K, V>, N>;

        template<typename, typename, std::size_t, typename>
        friend
        class SmallMap;

    public:
        using key_type = K;
        using mapped_type = V;

        using value_type = typename Map::value_type;
        using size_type = typename Map::size_type;
        using difference_type = typename Map::difference_type;

        using reference = typename Map::reference;
        using iterator = typename Map::iterator;

        using const_reference = typename Map::const_reference;
        using const_iterator = typename Map::const_iterator;

        // Creates an empty map.
        SmallMap() = default;

        // Constructs at most N key-value pairs in place by forwarding per-pair constructor arguments.
        // The template arguments K, V, and N are inferred using the deduction guide defined below.
        // The syntax for listing pairs is as follows:
        //
        //   ftl::SmallMap map = ftl::init::map<int, std::string>(123, "abc")(-1)(42, 3u, '?');
        //   static_assert(std::is_same_v<decltype(map), ftl::SmallMap<int, std::string, 3>>);
        //
        // The types of the key and value are deduced if the first pair contains exactly two arguments:
        //
        //   ftl::SmallMap map = ftl::init::map(0, 'a')(1, 'b')(2, 'c');
        //   static_assert(std::is_same_v<decltype(map), ftl::SmallMap<int, char, 3>>);
        //
        template<typename U, std::size_t... Sizes, typename... Types>
        SmallMap(InitializerList<U, std::index_sequence<Sizes...>, Types...> &&list)
                : map_(std::move(list)) {
            deduplicate();
        }

        // Copies or moves key-value pairs from a convertible map.
        template<typename Q, typename W, std::size_t M, typename E>
        SmallMap(SmallMap<Q, W, M, E> other) : map_(std::move(other.map_)) {}

        static constexpr size_type static_capacity() { return N; }

        size_type max_size() const { return map_.max_size(); }

        size_type size() const { return map_.size(); }

        bool empty() const { return map_.empty(); }

        // Returns whether the map is backed by static or dynamic storage.
        bool dynamic() const {
            if constexpr (static_capacity() > 0) {
                return map_.dynamic();
            } else {
                return true;
            }
        }

        iterator begin() { return map_.begin(); }

        const_iterator begin() const { return cbegin(); }

        const_iterator cbegin() const { return map_.cbegin(); }

        iterator end() { return map_.end(); }

        const_iterator end() const { return cend(); }

        const_iterator cend() const { return map_.cend(); }

        // Returns whether a mapping exists for the given key.
        bool contains(const key_type &key) const { return get(key).has_value(); }

        // Returns a reference to the value for the given key, or std::nullopt if the key was not found.
        //
        //   ftl::SmallMap map = ftl::init::map('a', 'A')('b', 'B')('c', 'C');
        //
        //   const auto opt = map.get('c');
        //   assert(opt == 'C');
        //
        //   char d = 'd';
        //   const auto ref = map.get('d').value_or(std::ref(d));
        //   ref.get() = 'D';
        //   assert(d == 'D');
        //
        auto get(const key_type &key) const -> Optional<std::reference_wrapper<const mapped_type>> {
            for (const auto &[k, v]: *this) {
                if (KeyEqual{}(k, key)) {
                    return std::cref(v);
                }
            }
            return {};
        }

        auto get(const key_type &key) -> Optional<std::reference_wrapper<mapped_type>> {
            for (auto &[k, v]: *this) {
                if (KeyEqual{}(k, key)) {
                    return std::ref(v);
                }
            }
            return {};
        }

        // Returns an iterator to an existing mapping for the given key, or the end() iterator otherwise.
        const_iterator find(const key_type &key) const {
            return const_cast<SmallMap &>(*this).find(key);
        }

        iterator find(const key_type &key) { return find(key, begin()); }

        // Inserts a mapping unless it exists. Returns an iterator to the inserted or existing mapping,
        // and whether the mapping was inserted.
        //
        // On emplace, if the map reaches its static or dynamic capacity, then all iterators are
        // invalidated. Otherwise, only the end() iterator is invalidated.
        //
        template<typename... Args>
        std::pair<iterator, bool> try_emplace(const key_type &key, Args &&... args) {
            if (const auto it = find(key); it != end()) {
                return {it, false};
            }

            decltype(auto) ref_or_it =
                    map_.emplace_back(std::piecewise_construct, std::forward_as_tuple(key),
                                      std::forward_as_tuple(std::forward<Args>(args)...));

            if constexpr (static_capacity() > 0) {
                return {&ref_or_it, true};
            } else {
                return {ref_or_it, true};
            }
        }

        // Replaces a mapping if it exists, and returns an iterator to it. Returns the end() iterator
        // otherwise.
        //
        // The value is replaced via move constructor, so type V does not need to define copy/move
        // assignment, e.g. its data members may be const.
        //
        // The arguments may directly or indirectly refer to the mapping being replaced.
        //
        // Iterators to the replaced mapping point to its replacement, and others remain valid.
        //
        template<typename... Args>
        iterator try_replace(const key_type &key, Args &&... args) {
            const auto it = find(key);
            if (it == end()) return it;
            map_.replace(it, std::piecewise_construct, std::forward_as_tuple(key),
                         std::forward_as_tuple(std::forward<Args>(args)...));
            return it;
        }

        // In-place counterpart of std::unordered_map's insert_or_assign. Returns true on emplace, or
        // false on replace.
        //
        // The value is emplaced and replaced via move constructor, so type V does not need to define
        // copy/move assignment, e.g. its data members may be const.
        //
        // On emplace, if the map reaches its static or dynamic capacity, then all iterators are
        // invalidated. Otherwise, only the end() iterator is invalidated. On replace, iterators
        // to the replaced mapping point to its replacement, and others remain valid.
        //
        template<typename... Args>
        std::pair<iterator, bool> emplace_or_replace(const key_type &key, Args &&... args) {
            const auto [it, ok] = try_emplace(key, std::forward<Args>(args)...);
            if (ok) return {it, ok};
            map_.replace(it, std::piecewise_construct, std::forward_as_tuple(key),
                         std::forward_as_tuple(std::forward<Args>(args)...));
            return {it, ok};
        }

        // Removes a mapping if it exists, and returns whether it did.
        //
        // The last() and end() iterators, as well as those to the erased mapping, are invalidated.
        //
        bool erase(const key_type &key) { return erase(key, begin()); }

        // Removes a mapping.
        //
        // The last() and end() iterators, as well as those to the erased mapping, are invalidated.
        //
        void erase(iterator it) { map_.unstable_erase(it); }

        // Removes all mappings.
        //
        // All iterators are invalidated.
        //
        void clear() { map_.clear(); }

    private:
        iterator find(const key_type &key, iterator first) {
            return std::find_if(first, end(),
                                [&key](const auto &pair) { return KeyEqual{}(pair.first, key); });
        }

        bool erase(const key_type &key, iterator first) {
            const auto it = find(key, first);
            if (it == end()) return false;
            map_.unstable_erase(it);
            return true;
        }

        void deduplicate() {
            for (auto it = begin(); it != end();) {
                if (const auto key = it->first; ++it != end()) {
                    while (erase(key, it));
                }
            }
        }

        Map map_;
    };

// Deduction guide for in-place constructor.
    template<typename K, typename V, typename E, std::size_t... Sizes, typename... Types>
    SmallMap(InitializerList<KeyValue<K, V, E>, std::index_sequence<Sizes...>, Types...> &&)
    -> SmallMap<K, V, sizeof...(Sizes), E>;

// Returns whether the key-value pairs of two maps are equal.
    template<typename K, typename V, std::size_t N, typename Q, typename W, std::size_t M, typename E>
    bool operator==(const SmallMap<K, V, N, E> &lhs, const SmallMap<Q, W, M, E> &rhs) {
        if (lhs.size() != rhs.size()) return false;

        for (const auto &[k, v]: lhs) {
            const auto &lv = v;
            if (!rhs.get(k).transform([&lv](const W &rv) { return lv == rv; }).value_or(false)) {
                return false;
            }
        }

        return true;
    }

// TODO: Remove in C++20.
    template<typename K, typename V, std::size_t N, typename Q, typename W, std::size_t M, typename E>
    inline bool operator!=(const SmallMap<K, V, N, E> &lhs, const SmallMap<Q, W, M, E> &rhs) {
        return !(lhs == rhs);
    }

}  // namespace android::ftl
