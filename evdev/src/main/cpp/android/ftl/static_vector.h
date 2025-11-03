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
#include <ftl/initializer_list.h>

#include <algorithm>
#include <cassert>
#include <iterator>
#include <memory>
#include <type_traits>
#include <utility>

namespace android::ftl {

    constexpr struct IteratorRangeTag {
    } kIteratorRange;

// Fixed-capacity, statically allocated counterpart of std::vector. Like std::array, StaticVector
// allocates contiguous storage for N elements of type T at compile time, but stores at most (rather
// than exactly) N elements. Unlike std::array, its default constructor does not require T to have a
// default constructor, since elements are constructed in place as the vector grows. Operations that
// insert an element (emplace_back, push_back, etc.) fail when the vector is full. The API otherwise
// adheres to standard containers, except the unstable_erase operation that does not preserve order,
// and the replace operation that destructively emplaces.
//
// Unlike std::vector, T does not require copy/move assignment, so may be an object with const data
// members, or be const itself.
//
// StaticVector<T, 1> is analogous to an iterable std::optional.
// StaticVector<T, 0> is an error.
//
// Example usage:
//
//   ftl::StaticVector<char, 3> vector;
//   assert(vector.empty());
//
//   vector = {'a', 'b'};
//   assert(vector.size() == 2u);
//
//   vector.push_back('c');
//   assert(vector.full());
//
//   assert(!vector.push_back('d'));
//   assert(vector.size() == 3u);
//
//   vector.unstable_erase(vector.begin());
//   assert(vector == (ftl::StaticVector{'c', 'b'}));
//
//   vector.pop_back();
//   assert(vector.back() == 'c');
//
//   const char array[] = "hi";
//   vector = ftl::StaticVector(array);
//   assert(vector == (ftl::StaticVector{'h', 'i', '\0'}));
//
//   ftl::StaticVector strings = ftl::init::list<std::string>("abc")("123456", 3u)(3u, '?');
//   assert(strings.size() == 3u);
//   assert(strings[0] == "abc");
//   assert(strings[1] == "123");
//   assert(strings[2] == "???");
//
    template<typename T, std::size_t N>
    class StaticVector final : details::ArrayTraits<T>,
                               details::ArrayIterators<StaticVector<T, N>, T>,
                               details::ArrayComparators<StaticVector> {
        static_assert(N > 0);

        // For constructor that moves from a smaller convertible vector.
        template<typename, std::size_t>
        friend
        class StaticVector;

        using details::ArrayTraits<T>::construct_at;
        using details::ArrayTraits<T>::replace_at;
        using details::ArrayTraits<T>::in_place_swap_ranges;
        using details::ArrayTraits<T>::uninitialized_copy;

        using Iter = details::ArrayIterators<StaticVector, T>;
        friend Iter;

        // There is ambiguity when constructing from two iterator-like elements like pointers:
        // they could be an iterator range, or arguments for in-place construction. Assume the
        // latter unless they are input iterators and cannot be used to construct elements. If
        // the former is intended, the caller can pass an IteratorRangeTag to disambiguate.
        template<typename I, typename Traits = std::iterator_traits<I>>
        using is_input_iterator =
                std::conjunction<std::is_base_of<std::input_iterator_tag, typename Traits::iterator_category>,
                        std::negation<std::is_constructible<T, I>>>;

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
        StaticVector() = default;

        // Copies and moves a vector, respectively.
        StaticVector(const StaticVector &other)
                : StaticVector(kIteratorRange, other.begin(), other.end()) {}

        StaticVector(StaticVector &&other) { swap<true>(other); }

        // Copies at most N elements from a smaller convertible vector.
        template<typename U, std::size_t M>
        StaticVector(const StaticVector<U, M> &other)
                : StaticVector(kIteratorRange, other.begin(), other.end()) {
            static_assert(N >= M, "Insufficient capacity");
        }

        // Copies at most N elements from a smaller convertible array.
        template<typename U, std::size_t M>
        explicit StaticVector(U (&array)[M])
                : StaticVector(kIteratorRange, std::begin(array), std::end(array)) {
            static_assert(N >= M, "Insufficient capacity");
        }

        // Copies at most N elements from the range [first, last).
        //
        // IteratorRangeTag disambiguates with initialization from two iterator-like elements.
        //
        template<typename Iterator, typename = std::enable_if_t<is_input_iterator<Iterator>{}>>
        StaticVector(Iterator first, Iterator last) : StaticVector(kIteratorRange, first, last) {
            using V = typename std::iterator_traits<Iterator>::value_type;
            static_assert(std::is_constructible_v<value_type, V>, "Incompatible iterator range");
        }

        template<typename Iterator>
        StaticVector(IteratorRangeTag, Iterator first, Iterator last)
                : size_(std::min(max_size(), static_cast<size_type>(std::distance(first, last)))) {
            uninitialized_copy(first, first + size_, begin());
        }

        // Moves at most N elements from a smaller convertible vector.
        template<typename U, std::size_t M>
        StaticVector(StaticVector<U, M> &&other) {
            static_assert(N >= M, "Insufficient capacity");

            // Same logic as swap<true>, though M need not be equal to N.
            std::uninitialized_move(other.begin(), other.end(), begin());
            std::destroy(other.begin(), other.end());
            std::swap(size_, other.size_);
        }

        // Constructs at most N elements. The template arguments T and N are inferred using the
        // deduction guide defined below. Note that T is determined from the first element, and
        // subsequent elements must have convertible types:
        //
        //   ftl::StaticVector vector = {1, 2, 3};
        //   static_assert(std::is_same_v<decltype(vector), ftl::StaticVector<int, 3>>);
        //
        //   const auto copy = "quince"s;
        //   auto move = "tart"s;
        //   ftl::StaticVector vector = {copy, std::move(move)};
        //
        //   static_assert(std::is_same_v<decltype(vector), ftl::StaticVector<std::string, 2>>);
        //
        template<typename E, typename... Es,
                typename = std::enable_if_t<std::is_constructible_v<value_type, E>>>
        StaticVector(E &&element, Es &&... elements)
                : StaticVector(std::index_sequence<0>{}, std::forward<E>(element),
                               std::forward<Es>(elements)...) {
            static_assert(sizeof...(elements) < N, "Too many elements");
        }

        // Constructs at most N elements in place by forwarding per-element constructor arguments. The
        // template arguments T and N are inferred using the deduction guide defined below. The syntax
        // for listing arguments is as follows:
        //
        //   ftl::StaticVector vector = ftl::init::list<std::string>("abc")()(3u, '?');
        //
        //   static_assert(std::is_same_v<decltype(vector), ftl::StaticVector<std::string, 3>>);
        //   assert(vector.full());
        //   assert(vector[0] == "abc");
        //   assert(vector[1].empty());
        //   assert(vector[2] == "???");
        //
        template<typename U, std::size_t Size, std::size_t... Sizes, typename... Types>
        StaticVector(InitializerList<U, std::index_sequence<Size, Sizes...>, Types...> &&list)
                : StaticVector(std::index_sequence<0, 0, Size>{}, std::make_index_sequence<Size>{},
                               std::index_sequence<Sizes...>{}, list.tuple) {
            static_assert(sizeof...(Sizes) < N, "Too many elements");
        }

        ~StaticVector() { std::destroy(begin(), end()); }

        StaticVector &operator=(const StaticVector &other) {
            StaticVector copy(other);
            swap(copy);
            return *this;
        }

        StaticVector &operator=(StaticVector &&other) {
            clear();
            swap<true>(other);
            return *this;
        }

        // IsEmpty enables a fast path when the vector is known to be empty at compile time.
        template<bool IsEmpty = false>
        void swap(StaticVector &);

        static constexpr size_type max_size() { return N; }

        size_type size() const { return size_; }

        bool empty() const { return size() == 0; }

        bool full() const { return size() == max_size(); }

        iterator begin() { return std::launder(reinterpret_cast<pointer>(data_)); }

        iterator end() { return begin() + size(); }

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
            return replace_at(it, std::forward<Args>(args)...);
        }

        // Appends an element, and returns an iterator to it. If the vector is full, the element is not
        // inserted, and the end() iterator is returned.
        //
        // On success, the end() iterator is invalidated.
        //
        template<typename... Args>
        iterator emplace_back(Args &&... args) {
            if (full()) return end();
            const iterator it = construct_at(end(), std::forward<Args>(args)...);
            ++size_;
            return it;
        }

        // Appends an element unless the vector is full, and returns whether the element was inserted.
        //
        // On success, the end() iterator is invalidated.
        //
        bool push_back(const value_type &v) {
            // Two statements for sequence point.
            const iterator it = emplace_back(v);
            return it != end();
        }

        bool push_back(value_type &&v) {
            // Two statements for sequence point.
            const iterator it = emplace_back(std::move(v));
            return it != end();
        }

        // Removes the last element. The vector must not be empty, or the call is erroneous.
        //
        // The last() and end() iterators are invalidated.
        //
        void pop_back() { unstable_erase(last()); }

        // Removes all elements.
        //
        // All iterators are invalidated.
        //
        void clear() {
            std::destroy(begin(), end());
            size_ = 0;
        }

        // Erases an element, but does not preserve order. Rather than shifting subsequent elements,
        // this moves the last element to the slot of the erased element.
        //
        // The last() and end() iterators, as well as those to the erased element, are invalidated.
        //
        void unstable_erase(const_iterator it) {
            std::destroy_at(it);
            if (it != last()) {
                // Move last element and destroy its source for destructor side effects. This is only
                // safe because exceptions are disabled.
                construct_at(it, std::move(back()));
                std::destroy_at(last());
            }
            --size_;
        }

    private:
        // Recursion for variadic constructor.
        template<std::size_t I, typename E, typename... Es>
        StaticVector(std::index_sequence<I>, E &&element, Es &&... elements)
                : StaticVector(std::index_sequence<I + 1>{}, std::forward<Es>(elements)...) {
            construct_at(begin() + I, std::forward<E>(element));
        }

        // Base case for variadic constructor.
        template<std::size_t I>
        explicit StaticVector(std::index_sequence<I>) : size_(I) {}

        // Recursion for in-place constructor.
        //
        // Construct element I by extracting its arguments from the InitializerList tuple. ArgIndex
        // is the position of its first argument in Args, and ArgCount is the number of arguments.
        // The Indices sequence corresponds to [0, ArgCount).
        //
        // The Sizes sequence lists the argument counts for elements after I, so Size is the ArgCount
        // for the next element. The recursion stops when Sizes is empty for the last element.
        //
        template<std::size_t I, std::size_t ArgIndex, std::size_t ArgCount, std::size_t... Indices,
                std::size_t Size, std::size_t... Sizes, typename... Args>
        StaticVector(std::index_sequence<I, ArgIndex, ArgCount>, std::index_sequence<Indices...>,
                     std::index_sequence<Size, Sizes...>, std::tuple<Args...> &tuple)
                : StaticVector(std::index_sequence<I + 1, ArgIndex + ArgCount, Size>{},
                               std::make_index_sequence<Size>{}, std::index_sequence<Sizes...>{},
                               tuple) {
            construct_at(begin() + I, std::move(std::get<ArgIndex + Indices>(tuple))...);
        }

        // Base case for in-place constructor.
        template<std::size_t I, std::size_t ArgIndex, std::size_t ArgCount, std::size_t... Indices,
                typename... Args>
        StaticVector(std::index_sequence<I, ArgIndex, ArgCount>, std::index_sequence<Indices...>,
                     std::index_sequence<>, std::tuple<Args...> &tuple)
                : size_(I + 1) {
            construct_at(begin() + I, std::move(std::get<ArgIndex + Indices>(tuple))...);
        }

        size_type size_ = 0;
        std::aligned_storage_t<sizeof(value_type), alignof(value_type)> data_[N];
    };

// Deduction guide for array constructor.
    template<typename T, std::size_t N>
    StaticVector(T (&)[N]) -> StaticVector<std::remove_cv_t<T>, N>;

// Deduction guide for variadic constructor.
    template<typename T, typename... Us, typename V = std::decay_t<T>,
            typename = std::enable_if_t<(std::is_constructible_v<V, Us> && ...)>>
    StaticVector(T &&, Us &&...) -> StaticVector<V, 1 + sizeof...(Us)>;

// Deduction guide for in-place constructor.
    template<typename T, std::size_t... Sizes, typename... Types>
    StaticVector(InitializerList<T, std::index_sequence<Sizes...>, Types...> &&)
    -> StaticVector<T, sizeof...(Sizes)>;

    template<typename T, std::size_t N>
    template<bool IsEmpty>
    void StaticVector<T, N>::swap(StaticVector &other) {
        auto [to, from] = std::make_pair(this, &other);
        if (from == this) return;

        // Assume this vector has fewer elements, so the excess of the other vector will be moved to it.
        auto [min, max] = std::make_pair(size(), other.size());

        // No elements to swap if moving into an empty vector.
        if constexpr (IsEmpty) {
            assert(min == 0);
        } else {
            if (min > max) {
                std::swap(from, to);
                std::swap(min, max);
            }

            // Swap elements [0, min).
            in_place_swap_ranges(begin(), begin() + min, other.begin());

            // No elements to move if sizes are equal.
            if (min == max) return;
        }

        // Move elements [min, max) and destroy their source for destructor side effects.
        const auto [first, last] = std::make_pair(from->begin() + min, from->begin() + max);
        std::uninitialized_move(first, last, to->begin() + min);
        std::destroy(first, last);

        std::swap(size_, other.size_);
    }

    template<typename T, std::size_t N>
    inline void swap(StaticVector<T, N> &lhs, StaticVector<T, N> &rhs) {
        lhs.swap(rhs);
    }

}  // namespace android::ftl
