/*
 * Copyright (C) 2005 The Android Open Source Project
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

#define LOG_TAG "unicode"

#include <limits.h>
#include "Unicode.h"
#include "../../logging.h"
#include "../liblog/log_main.h"

#include <android/log.h>

extern "C" {

static const char32_t kByteMask = 0x000000BF;
static const char32_t kByteMark = 0x00000080;

// Surrogates aren't valid for UTF-32 characters, so define some
// constants that will let us screen them out.
static const char32_t kUnicodeSurrogateHighStart = 0x0000D800;
// Unused, here for completeness:
// static const char32_t kUnicodeSurrogateHighEnd = 0x0000DBFF;
// static const char32_t kUnicodeSurrogateLowStart = 0x0000DC00;
static const char32_t kUnicodeSurrogateLowEnd = 0x0000DFFF;
static const char32_t kUnicodeSurrogateStart = kUnicodeSurrogateHighStart;
static const char32_t kUnicodeSurrogateEnd = kUnicodeSurrogateLowEnd;
static const char32_t kUnicodeMaxCodepoint = 0x0010FFFF;

// Mask used to set appropriate bits in first byte of UTF-8 sequence,
// indexed by number of bytes in the sequence.
// 0xxxxxxx
// -> (00-7f) 7bit. Bit mask for the first byte is 0x00000000
// 110yyyyx 10xxxxxx
// -> (c0-df)(80-bf) 11bit. Bit mask is 0x000000C0
// 1110yyyy 10yxxxxx 10xxxxxx
// -> (e0-ef)(80-bf)(80-bf) 16bit. Bit mask is 0x000000E0
// 11110yyy 10yyxxxx 10xxxxxx 10xxxxxx
// -> (f0-f7)(80-bf)(80-bf)(80-bf) 21bit. Bit mask is 0x000000F0
static const char32_t kFirstByteMark[] = {
        0x00000000, 0x00000000, 0x000000C0, 0x000000E0, 0x000000F0
};

// --------------------------------------------------------------------------
// UTF-32
// --------------------------------------------------------------------------

/**
 * Return number of UTF-8 bytes required for the character. If the character
 * is invalid, return size of 0.
 */
static inline size_t utf32_codepoint_utf8_length(char32_t srcChar) {
    // Figure out how many bytes the result will require.
    if (srcChar < 0x00000080) {
        return 1;
    } else if (srcChar < 0x00000800) {
        return 2;
    } else if (srcChar < 0x00010000) {
        if ((srcChar < kUnicodeSurrogateStart) || (srcChar > kUnicodeSurrogateEnd)) {
            return 3;
        } else {
            // Surrogates are invalid UTF-32 characters.
            return 0;
        }
    }
        // Max code point for Unicode is 0x0010FFFF.
    else if (srcChar <= kUnicodeMaxCodepoint) {
        return 4;
    } else {
        // Invalid UTF-32 character.
        return 0;
    }
}

// Write out the source character to <dstP>.

static inline void utf32_codepoint_to_utf8(uint8_t *dstP, char32_t srcChar, size_t bytes) {
    dstP += bytes;
    switch (bytes) {   /* note: everything falls through. */
        case 4:
            *--dstP = (uint8_t) ((srcChar | kByteMark) & kByteMask);
            srcChar >>= 6;
            [[fallthrough]];
        case 3:
            *--dstP = (uint8_t) ((srcChar | kByteMark) & kByteMask);
            srcChar >>= 6;
            [[fallthrough]];
        case 2:
            *--dstP = (uint8_t) ((srcChar | kByteMark) & kByteMask);
            srcChar >>= 6;
            [[fallthrough]];
        case 1:
            *--dstP = (uint8_t) (srcChar | kFirstByteMark[bytes]);
    }
}

static inline int32_t utf32_at_internal(const char *cur, size_t *num_read) {
    const char first_char = *cur;
    if ((first_char & 0x80) == 0) { // ASCII
        *num_read = 1;
        return *cur;
    }
    cur++;
    char32_t mask, to_ignore_mask;
    size_t num_to_read = 0;
    char32_t utf32 = first_char;
    for (num_to_read = 1, mask = 0x40, to_ignore_mask = 0xFFFFFF80;
         (first_char & mask);
         num_to_read++, to_ignore_mask |= mask, mask >>= 1) {
        // 0x3F == 00111111
        utf32 = (utf32 << 6) + (*cur++ & 0x3F);
    }
    to_ignore_mask |= mask;
    utf32 &= ~(to_ignore_mask << (6 * (num_to_read - 1)));

    *num_read = num_to_read;
    return static_cast<int32_t>(utf32);
}

int32_t utf32_from_utf8_at(const char *src, size_t src_len, size_t index, size_t *next_index) {
    if (index >= src_len) {
        return -1;
    }
    size_t unused_index;
    if (next_index == nullptr) {
        next_index = &unused_index;
    }
    size_t num_read;
    int32_t ret = utf32_at_internal(src + index, &num_read);
    if (ret >= 0) {
        *next_index = index + num_read;
    }

    return ret;
}

ssize_t utf32_to_utf8_length(const char32_t *src, size_t src_len) {
    if (src == nullptr || src_len == 0) {
        return -1;
    }

    size_t ret = 0;
    const char32_t *end = src + src_len;
    while (src < end) {
        size_t char_len = utf32_codepoint_utf8_length(*src++);
        if (SSIZE_MAX - char_len < ret) {
            // If this happens, we would overflow the ssize_t type when
            // returning from this function, so we cannot express how
            // long this string is in an ssize_t.
//            android_errorWriteLog(0x534e4554, "37723026");
            return -1;
        }
        ret += char_len;
    }
    return ret;
}

void utf32_to_utf8(const char32_t *src, size_t src_len, char *dst, size_t dst_len) {
    if (src == nullptr || src_len == 0 || dst == nullptr) {
        return;
    }

    const char32_t *cur_utf32 = src;
    const char32_t *end_utf32 = src + src_len;
    char *cur = dst;
    while (cur_utf32 < end_utf32) {
        size_t len = utf32_codepoint_utf8_length(*cur_utf32);
        LOG_ALWAYS_FATAL_IF(dst_len < len, "%zu < %zu", dst_len, len);
        utf32_codepoint_to_utf8((uint8_t *) cur, *cur_utf32++, len);
        cur += len;
        dst_len -= len;
    }
    LOG_ALWAYS_FATAL_IF(dst_len < 1, "dst_len < 1: %zu < 1", dst_len);
    *cur = '\0';
}

// --------------------------------------------------------------------------
// UTF-16
// --------------------------------------------------------------------------

int strcmp16(const char16_t *s1, const char16_t *s2) {
    char16_t ch;
    int d = 0;

    while (1) {
        d = (int) (ch = *s1++) - (int) *s2++;
        if (d || !ch)
            break;
    }

    return d;
}

int strncmp16(const char16_t *s1, const char16_t *s2, size_t n) {
    char16_t ch;
    int d = 0;

    if (n == 0) {
        return 0;
    }

    do {
        d = (int) (ch = *s1++) - (int) *s2++;
        if (d || !ch) {
            break;
        }
    } while (--n);

    return d;
}

size_t strlen16(const char16_t *s) {
    const char16_t *ss = s;
    while (*ss)
        ss++;
    return ss - s;
}

size_t strnlen16(const char16_t *s, size_t maxlen) {
    const char16_t *ss = s;

    /* Important: the maxlen test must precede the reference through ss;
       since the byte beyond the maximum may segfault */
    while ((maxlen > 0) && *ss) {
        ss++;
        maxlen--;
    }
    return ss - s;
}

char16_t *strstr16(const char16_t *src, const char16_t *target) {
    const char16_t needle = *target;
    if (needle == '\0') return (char16_t *) src;

    const size_t target_len = strlen16(++target);
    do {
        do {
            if (*src == '\0') {
                return nullptr;
            }
        } while (*src++ != needle);
    } while (strncmp16(src, target, target_len) != 0);
    src--;

    return (char16_t *) src;
}

int strzcmp16(const char16_t *s1, size_t n1, const char16_t *s2, size_t n2) {
    const char16_t *e1 = s1 + n1;
    const char16_t *e2 = s2 + n2;

    while (s1 < e1 && s2 < e2) {
        const int d = (int) *s1++ - (int) *s2++;
        if (d) {
            return d;
        }
    }

    return n1 < n2
           ? (0 - (int) *s2)
           : (n1 > n2
              ? ((int) *s1 - 0)
              : 0);
}

// is_any_surrogate() returns true if w is either a high or low surrogate
static constexpr bool is_any_surrogate(char16_t w) {
    return (w & 0xf800) == 0xd800;
}

// is_surrogate_pair() returns true if w1 and w2 form a valid surrogate pair
static constexpr bool is_surrogate_pair(char16_t w1, char16_t w2) {
    return ((w1 & 0xfc00) == 0xd800) && ((w2 & 0xfc00) == 0xdc00);
}

// TODO: currently utf16_to_utf8_length() returns -1 if src_len == 0,
// which is inconsistent with utf8_to_utf16_length(), here we keep the
// current behavior as intended not to break compatibility
ssize_t utf16_to_utf8_length(const char16_t *src, size_t src_len) {
    if (src == nullptr || src_len == 0)
        return -1;

    const char16_t *const end = src + src_len;
    const char16_t *in = src;
    size_t utf8_len = 0;

    while (in < end) {
        char16_t w = *in++;
        if (w < 0x0080) [[likely]] {
            utf8_len += 1;
            continue;
        }
        if (w < 0x0800) [[likely]] {
            utf8_len += 2;
            continue;
        }
        if (!is_any_surrogate(w)) [[likely]] {
            utf8_len += 3;
            continue;
        }
        if (in < end && is_surrogate_pair(w, *in)) {
            utf8_len += 4;
            in++;
            continue;
        }
        /* skip if at the end of the string or invalid surrogate pair */
    }
    return (in == end && utf8_len < SSIZE_MAX) ? utf8_len : -1;
}

void utf16_to_utf8(const char16_t *src, size_t src_len, char *dst, size_t dst_len) {
    if (src == nullptr || src_len == 0 || dst == nullptr) {
        return;
    }

    const char16_t *in = src;
    const char16_t *const in_end = src + src_len;
    char *out = dst;
    const char *const out_end = dst + dst_len;
    char16_t w2;

    auto err_out = [&out, &out_end, &dst_len]() {
        LOG_ALWAYS_FATAL_IF(out >= out_end,
                            "target utf8 string size %zu too short", dst_len);
    };

    while (in < in_end) {
        char16_t w = *in++;
        if (w < 0x0080) [[likely]] {
            if (out + 1 > out_end)
                return err_out();
            *out++ = (char) (w & 0xff);
            continue;
        }
        if (w < 0x0800) [[likely]] {
            if (out + 2 > out_end)
                return err_out();
            *out++ = (char) (0xc0 | ((w >> 6) & 0x1f));
            *out++ = (char) (0x80 | ((w >> 0) & 0x3f));
            continue;
        }
        if (!is_any_surrogate(w)) [[likely]] {
            if (out + 3 > out_end)
                return err_out();
            *out++ = (char) (0xe0 | ((w >> 12) & 0xf));
            *out++ = (char) (0x80 | ((w >> 6) & 0x3f));
            *out++ = (char) (0x80 | ((w >> 0) & 0x3f));
            continue;
        }
        /* surrogate pair */
        if (in < in_end && (w2 = *in, is_surrogate_pair(w, w2))) {
            if (out + 4 > out_end)
                return err_out();
            char32_t dw = (char32_t) (0x10000 + ((w - 0xd800) << 10) + (w2 - 0xdc00));
            *out++ = (char) (0xf0 | ((dw >> 18) & 0x07));
            *out++ = (char) (0x80 | ((dw >> 12) & 0x3f));
            *out++ = (char) (0x80 | ((dw >> 6) & 0x3f));
            *out++ = (char) (0x80 | ((dw >> 0) & 0x3f));
            in++;
        }
        /* We reach here in two cases:
         *  1) (in == in_end), which means end of the input string
         *  2) (w2 & 0xfc00) != 0xdc00, which means invalid surrogate pair
         * In either case, we intentionally do nothing and skip
         */
    }
    *out = '\0';
    return;
}

// --------------------------------------------------------------------------
// UTF-8
// --------------------------------------------------------------------------

static char32_t utf8_4b_to_utf32(uint8_t c1, uint8_t c2, uint8_t c3, uint8_t c4) {
    return ((c1 & 0x07) << 18) | ((c2 & 0x3f) << 12) | ((c3 & 0x3f) << 6) | (c4 & 0x3f);
}

// TODO: current behavior of converting UTF8 to UTF-16 has a few issues below
//
// 1. invalid trailing bytes (i.e. not b'10xxxxxx) are treated as valid trailing
//    bytes and follows normal conversion rules
// 2. invalid leading byte (b'10xxxxxx) is treated as a valid single UTF-8 byte
// 3. invalid leading byte (b'11111xxx) is treated as a valid leading byte
//    (same as b'11110xxx) for a 4-byte UTF-8 sequence
// 4. an invalid 4-byte UTF-8 sequence that translates to a codepoint < U+10000
//    will be converted as a valid UTF-16 character
//
// We keep the current behavior as is but with warnings logged, so as not to
// break compatibility.  However, this needs to be addressed later.

ssize_t utf8_to_utf16_length(const uint8_t *u8str, size_t u8len, bool overreadIsFatal) {
    if (u8str == nullptr)
        return -1;

    const uint8_t *const in_end = u8str + u8len;
    const uint8_t *in = u8str;
    size_t utf16_len = 0;

    while (in < in_end) {
        uint8_t c = *in;
        utf16_len++;
        if ((c & 0x80) == 0) [[likely]] {
            in++;
            continue;
        }
        if (c < 0xc0) [[unlikely]] {
            LOGW("Invalid UTF-8 leading byte: 0x%02x", c);
            in++;
            continue;
        }
        if (c < 0xe0) [[likely]] {
            in += 2;
            continue;
        }
        if (c < 0xf0) [[likely]] {
            in += 3;
            continue;
        } else {
            uint8_t c2, c3, c4;
            if (c >= 0xf8) [[unlikely]] {
                LOGW("Invalid UTF-8 leading byte: 0x%02x", c);
            }
            c2 = in[1];
            c3 = in[2];
            c4 = in[3];
            if (utf8_4b_to_utf32(c, c2, c3, c4) >= 0x10000) {
                utf16_len++;
            }
            in += 4;
            continue;
        }
    }
    if (in == in_end) {
        return utf16_len < SSIZE_MAX ? utf16_len : -1;
    }
    if (overreadIsFatal)
        LOG_ALWAYS_FATAL("Attempt to overread computing length of utf8 string");
    return -1;
}

char16_t *utf8_to_utf16(const uint8_t *u8str, size_t u8len, char16_t *u16str, size_t u16len) {
    // A value > SSIZE_MAX is probably a negative value returned as an error and casted.
    LOG_ALWAYS_FATAL_IF(u16len == 0 || u16len > SSIZE_MAX, "u16len is %zu", u16len);
    char16_t *end = utf8_to_utf16_no_null_terminator(u8str, u8len, u16str, u16len - 1);
    *end = 0;
    return end;
}

char16_t *utf8_to_utf16_no_null_terminator(
        const uint8_t *src, size_t srcLen, char16_t *dst, size_t dstLen) {
    if (src == nullptr || srcLen == 0 || dstLen == 0) {
        return dst;
    }
    // A value > SSIZE_MAX is probably a negative value returned as an error and casted.
    LOG_ALWAYS_FATAL_IF(dstLen > SSIZE_MAX, "dstLen is %zu", dstLen);

    const uint8_t *const in_end = src + srcLen;
    const uint8_t *in = src;
    const char16_t *const out_end = dst + dstLen;
    char16_t *out = dst;
    uint8_t c, c2, c3, c4;
    char32_t w;

    auto err_in = [&c, &out]() {
        LOGW("Unended UTF-8 byte: 0x%02x", c);
        return out;
    };

    while (in < in_end && out < out_end) {
        c = *in++;
        if ((c & 0x80) == 0) [[likely]] {
            *out++ = (char16_t) (c);
            continue;
        }
        if (c < 0xc0) [[unlikely]] {
            ALOGW("Invalid UTF-8 leading byte: 0x%02x", c);
            *out++ = (char16_t) (c);
            continue;
        }
        if (c < 0xe0) [[likely]] {
            if (in + 1 > in_end) [[unlikely]] {
                return err_in();
            }
            c2 = *in++;
            *out++ = (char16_t) (((c & 0x1f) << 6) | (c2 & 0x3f));
            continue;
        }
        if (c < 0xf0) [[likely]] {
            if (in + 2 > in_end) [[unlikely]] {
                return err_in();
            }
            c2 = *in++;
            c3 = *in++;
            *out++ = (char16_t) (((c & 0x0f) << 12) |
                                 ((c2 & 0x3f) << 6) | (c3 & 0x3f));
            continue;
        } else {
            if (in + 3 > in_end) [[unlikely]] {
                return err_in();
            }
            if (c >= 0xf8) [[unlikely]] {
                LOGW("Invalid UTF-8 leading byte: 0x%02x", c);
            }
            // Multiple UTF16 characters with surrogates
            c2 = *in++;
            c3 = *in++;
            c4 = *in++;
            w = utf8_4b_to_utf32(c, c2, c3, c4);
            if (w < 0x10000) [[unlikely]] {
                *out++ = (char16_t) (w);
            } else {
                if (out + 2 > out_end) [[unlikely]] {
                    // Ooops.... not enough room for this surrogate pair.
                    return out;
                }
                *out++ = (char16_t) (((w - 0x10000) >> 10) + 0xd800);
                *out++ = (char16_t) (((w - 0x10000) & 0x3ff) + 0xdc00);
            }
            continue;
        }
    }
    return out;
}

}
