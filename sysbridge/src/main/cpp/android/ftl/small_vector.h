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

#include <ftl/details/array_traits.h>
#include <ftl/static_vector.h>

#include <algorithm>
#include <iterator>
#include <utility>
#include <variant>
#include <vector>

#include <ftl/details/type_traits.h>

namespace android::ftl {

    template<typename>
    struct is_small_vector;

// ftl::StaticVector that promotes to std::vector when full. SmallVector is a drop-in replacement
// for std::vector with statically allocated storage for N elements, whose goal is to improve run
// time by avoiding heap allocation and increasing probability of cache hits. The standard API is
// augmented by an unstable_erase operation that does not preserve order, and a replace operation
// that destructively emplaces.
//
// Unlike std::vector, T does not require copy/move assignment, so may be an object with const data
// members, or be const itself.
//
// SmallVector<T, 0> is a specialization that thinly wraps std::vector.
//
// Example usage:
//
//   ftl::SmallVector<char, 3> vector;
//   assert(vector.empty());
//   assert(!vector.dynamic());
//
//   vector = {'a', 'b', 'c'};
//   assert(vector.size() == 3u);
//   assert(!vector.dynamic());
//
//   vector.push_back('d');
//   assert(vector.dynamic());
//
//   vector.unstable_erase(vector.begin());
//   assert(vector == (ftl::SmallVector{'d', 'b', 'c'}));
//
//   vector.pop_back();
//   assert(vector.back() == 'b');
//   assert(vector.dynamic());
//
//   const char array[] = "hi";
//   vector = ftl::SmallVector(array);
//   assert(vector == (ftl::SmallVector{'h', 'i', '\0'}));
//   assert(!vector.dynamic());
//
//   ftl::SmallVector strings = ftl::init::list<std::string>("abc")("123456", 3u)(3u, '?');
//   assert(strings.size() == 3u);
//   assert(!strings.dynamic());
//
//   assert(strings[0] == "abc");
//   assert(strings[1] == "123");
//   assert(strings[2] == "???");
//
    template<typename T, std::size_t N>
    class SmallVector final : details::ArrayTraits<T>, details::ArrayComparators<SmallVector> {
        using Static = StaticVector<T, N>;
        using Dynamic = SmallVector<T, 0>;

    public:
        FTL_ARRAY_TRAIT(T, value_type);
        FTL_ARRAY_TRAIT(T, size_type);
        FTL_ARRAY_TRAIT(T, difference_type);

        FTL_ARRAY_TRAIT(T, pointer);
        FTL_ARRAY_TRAIT(T, reference);
        FTL_ARRAY_TRAIT(T, iterator);
        FTL_ARRAY_TRAIT(T, reverse_iterator);

        FTL_ARRAY_TRAIT(T, const_pointer);
        FTL_ARRAY_TRAIT(T, const_reference);
        FTL_ARRAY_TRAIT(T, const_iterator);
        FTL_ARRAY_TRAIT(T, const_reverse_iterator);

        // Creates an empty vector.
        SmallVector() = default;

        // Constructs at most N elements. See StaticVector for underlying constructors.
        template<typename Arg, typename... Args,
                typename = std::enable_if_t<!is_small_vector<details::remove_cvref_t<Arg>>{}>>
        SmallVector(Arg &&arg, Args &&... args)
                : vector_(std::in_place_type<Static>, std::forward<Arg>(arg),
                          std::forward<Args>(args)...) {}

        // Copies or moves elements from a smaller convertible vector.
        template<typename U, std::size_t M, typename = std::enable_if_t<(M > 0)>>
        SmallVector(SmallVector<U, M> other) : vector_(convert(std::move(other))) {}

        void swap(SmallVector &other) { vector_.swap(other.vector_); }

        // Returns whether the vector is backed by static or dynamic storage.
        bool dynamic() const { return std::holds_alternative<Dynamic>(vector_); }

        // Avoid std::visit as it generates a dispatch table.
#define DISPATCH(T, F, ...)                                                            \
  T F() __VA_ARGS__ {                                                                  \
    return dynamic() ? std::get<Dynamic>(vector_).F() : std::get<Static>(vector_).F(); \
  }

        DISPATCH(size_type, max_size, const)

        DISPATCH(size_type, size, const)

        DISPATCH(bool, empty, const)

        DISPATCH(iterator, begin,)

        DISPATCH(const_iterator, begin, const)

        DISPATCH(const_iterator, cbegin, const)

        DISPATCH(iterator, end,)

        DISPATCH(const_iterator, end, const)

        DISPATCH(const_iterator, cend, const)

        DISPATCH(reverse_iterator, rbegin,)

        DISPATCH(const_reverse_iterator, rbegin, const)

        DISPATCH(const_reverse_iterator, crbegin, const)

        DISPATCH(reverse_iterator, rend,)

        DISPATCH(const_reverse_iterator, rend, const)

        DISPATCH(const_reverse_iterator, crend, const)

        DISPATCH(iterator, last,)

        DISPATCH(const_iterator, last, const)

        DISPATCH(reference, front,)

        DISPATCH(const_reference, front, const)

        DISPATCH(reference, back,)

        DISPATCH(const_reference, back, const)

        reference operator[](size_type i) {
            return dynamic() ? std::get<Dynamic>(vector_)[i] : std::get<Static>(vector_)[i];
        }

        const_reference
        operator[](size_type i) const { return const_cast<SmallVector &>(*this)[i]; }

        // Replaces an element, and returns a reference to it. The iterator must be dereferenceable, so
        // replacing at end() is erroneous.
        //
        // The element is emplaced via move constructor, so type T does not need to define copy/move
        // assignment, e.g. its data members may be const.
        //
        // The arguments may directly or indirectly refer to the element being replaced.
        //
        // Iterators to the replaced element point to its replacement, and others remain valid.
        //
        template<typename... Args>
        reference replace(const_iterator it, Args &&... args) {
            if (dynamic()) {
                return std::get<Dynamic>(vector_).replace(it, std::forward<Args>(args)...);
            } else {
                return std::get<Static>(vector_).replace(it, std::forward<Args>(args)...);
            }
        }

        // Appends an element, and returns a reference to it.
        //
        // If the vector reaches its static or dynamic capacity, then all iterators are invalidated.
        // Otherwise, only the end() iterator is invalidated.
        //
        template<typename... Args>
        reference emplace_back(Args &&... args) {
            constexpr auto kInsertStatic = &Static::template emplace_back<Args...>;
            constexpr auto kInsertDynamic = &Dynamic::template emplace_back<Args...>;
            return *insert<kInsertStatic, kInsertDynamic>(std::forward<Args>(args)...);
        }

        // Appends an element.
        //
        // If the vector reaches its static or dynamic capacity, then all iterators are invalidated.
        // Otherwise, only the end() iterator is invalidated.
        //
        void push_back(const value_type &v) {
            constexpr auto kInsertStatic =
                    static_cast<bool (Static::*)(const value_type &)>(&Static::push_back);
            constexpr auto kInsertDynamic =
                    static_cast<bool (Dynamic::*)(const value_type &)>(&Dynamic::push_back);
            insert<kInsertStatic, kInsertDynamic>(v);
        }

        void push_back(value_type &&v) {
            constexpr auto kInsertStatic = static_cast<bool (Static::*)(
                    value_type &&)>(&Static::push_back);
            constexpr auto kInsertDynamic =
                    static_cast<bool (Dynamic::*)(value_type &&)>(&Dynamic::push_back);
            insert<kInsertStatic, kInsertDynamic>(std::move(v));
        }

        // Removes the last element. The vector must not be empty, or the call is erroneous.
        //
        // The last() and end() iterators are invalidated.
        //
        DISPATCH(void, pop_back,)

        // Removes all elements.
        //
        // All iterators are invalidated.
        //
        DISPATCH(void, clear,)

#undef DISPATCH

        // Erases an element, but does not preserve order. Rather than shifting subsequent elements,
        // this moves the last element to the slot of the erased element.
        //
        // The last() and end() iterators, as well as those to the erased element, are invalidated.
        //
        void unstable_erase(iterator it) {
            if (dynamic()) {
                std::get<Dynamic>(vector_).unstable_erase(it);
            } else {
                std::get<Static>(vector_).unstable_erase(it);
            }
        }

        // Extracts the elements as std::vector.
        std::vector<std::remove_const_t<T>> promote() &&{
            if (dynamic()) {
                return std::get<Dynamic>(std::move(vector_)).promote();
            } else {
                return {std::make_move_iterator(begin()), std::make_move_iterator(end())};
            }
        }

    private:
        template<typename, std::size_t>
        friend
        class SmallVector;

        template<typename U, std::size_t M>
        static std::variant<Static, Dynamic> convert(SmallVector<U, M> &&other) {
            using Other = SmallVector<U, M>;

            if (other.dynamic()) {
                return std::get<typename Other::Dynamic>(std::move(other.vector_));
            } else {
                return std::get<typename Other::Static>(std::move(other.vector_));
            }
        }

        template<auto InsertStatic, auto InsertDynamic, typename... Args>
        auto insert(Args &&... args) {
            if (Dynamic *const vector = std::get_if<Dynamic>(&vector_)) {
                return (vector->*InsertDynamic)(std::forward<Args>(args)...);
            }

            auto &vector = std::get<Static>(vector_);
            if (vector.full()) {
                return (promote(vector).*InsertDynamic)(std::forward<Args>(args)...);
            } else {
                return (vector.*InsertStatic)(std::forward<Args>(args)...);
            }
        }

        Dynamic &promote(Static &static_vector) {
            assert(static_vector.full());

            // Allocate double capacity to reduce probability of reallocation.
            Dynamic vector;
            vector.reserve(Static::max_size() * 2);
            std::move(static_vector.begin(), static_vector.end(), std::back_inserter(vector));

            return vector_.template emplace<Dynamic>(std::move(vector));
        }

        std::variant<Static, Dynamic> vector_;
    };

// Partial specialization without static storage.
    template<typename T>
    class SmallVector<T, 0> final : details::ArrayTraits<T>,
                                    details::ArrayComparators<SmallVector>,
                                    details::ArrayIterators<SmallVector<T, 0>, T>,
                                    std::vector<std::remove_const_t<T>> {
        using details::ArrayTraits<T>::replace_at;

        using Iter = details::ArrayIterators<SmallVector, T>;
        using Impl = std::vector<std::remove_const_t<T>>;

        friend Iter;

    public:
        FTL_ARRAY_TRAIT(T, value_type);
        FTL_ARRAY_TRAIT(T, size_type);
        FTL_ARRAY_TRAIT(T, difference_type);

        FTL_ARRAY_TRAIT(T, pointer);
        FTL_ARRAY_TRAIT(T, reference);
        FTL_ARRAY_TRAIT(T, iterator);
        FTL_ARRAY_TRAIT(T, reverse_iterator);

        FTL_ARRAY_TRAIT(T, const_pointer);
        FTL_ARRAY_TRAIT(T, const_reference);
        FTL_ARRAY_TRAIT(T, const_iterator);
        FTL_ARRAY_TRAIT(T, const_reverse_iterator);

        // See std::vector for underlying constructors.
        using Impl::Impl;

        // Copies and moves a vector, respectively.
        SmallVector(const SmallVector &) = default;

        SmallVector(SmallVector &&) = default;

        // Constructs elements in place. See StaticVector for underlying constructor.
        template<typename U, std::size_t... Sizes, typename... Types>
        SmallVector(InitializerList<U, std::index_sequence<Sizes...>, Types...> &&list)
                : SmallVector(SmallVector<T, sizeof...(Sizes)>(std::move(list))) {}

        // Copies or moves elements from a convertible vector.
        template<typename U, std::size_t M>
        SmallVector(SmallVector<U, M> other) : Impl(convert(std::move(other))) {}

        SmallVector &operator=(SmallVector other) {
            // Define copy/move assignment in terms of copy/move construction.
            swap(other);
            return *this;
        }

        void swap(SmallVector &other) { Impl::swap(other); }

        using Impl::empty;
        using Impl::max_size;
        using Impl::size;

        using Impl::reserve;

        // std::vector iterators are not necessarily raw pointers.
        iterator begin() { return Impl::data(); }

        iterator end() { return Impl::data() + size(); }

        using Iter::begin;
        using Iter::end;

        using Iter::cbegin;
        using Iter::cend;

        using Iter::rbegin;
        using Iter::rend;

        using Iter::crbegin;
        using Iter::crend;

        using Iter::last;

        using Iter::back;
        using Iter::front;

        using Iter::operator[];

        template<typename... Args>
        reference replace(const_iterator it, Args &&... args) {
            return replace_at(it, std::forward<Args>(args)...);
        }

        template<typename... Args>
        iterator emplace_back(Args &&... args) {
            return &Impl::emplace_back(std::forward<Args>(args)...);
        }

        bool push_back(const value_type &v) {
            Impl::push_back(v);
            return true;
        }

        bool push_back(value_type &&v) {
            Impl::push_back(std::move(v));
            return true;
        }

        using Impl::clear;
        using Impl::pop_back;

        void unstable_erase(iterator it) {
            if (it != last()) replace(it, std::move(back()));
            pop_back();
        }

        std::vector<std::remove_const_t<T>> promote() &&{ return std::move(*this); }

    private:
        template<typename U, std::size_t M>
        static Impl convert(SmallVector<U, M> &&other) {
            if constexpr (std::is_constructible_v<Impl, std::vector<std::remove_const_t<U>> &&>) {
                return std::move(other).promote();
            } else {
                SmallVector vector(other.size());

                // Consistently with StaticVector, T only requires copy/move construction from U, rather than
                // copy/move assignment.
                auto it = vector.begin();
                for (auto &element: other) {
                    vector.replace(it++, std::move(element));
                }

                return vector;
            }
        }
    };

    template<typename>
    struct is_small_vector : std::false_type {
    };

    template<typename T, std::size_t N>
    struct is_small_vector<SmallVector<T, N>> : std::true_type {
    };

// Deduction guide for array constructor.
    template<typename T, std::size_t N>
    SmallVector(T (&)[N]) -> SmallVector<std::remove_cv_t<T>, N>;

// Deduction guide for variadic constructor.
    template<typename T, typename... Us, typename V = std::decay_t<T>,
            typename = std::enable_if_t<(std::is_constructible_v<V, Us> && ...)>>
    SmallVector(T &&, Us &&...) -> SmallVector<V, 1 + sizeof...(Us)>;

// Deduction guide for in-place constructor.
    template<typename T, std::size_t... Sizes, typename... Types>
    SmallVector(InitializerList<T, std::index_sequence<Sizes...>, Types...> &&)
    -> SmallVector<T, sizeof...(Sizes)>;

// Deduction guide for StaticVector conversion.
    template<typename T, std::size_t N>
    SmallVector(StaticVector<T, N> &&) -> SmallVector<T, N>;

    template<typename T, std::size_t N>
    inline void swap(SmallVector<T, N> &lhs, SmallVector<T, N> &rhs) {
        lhs.swap(rhs);
    }

}  // namespace android::ftl
