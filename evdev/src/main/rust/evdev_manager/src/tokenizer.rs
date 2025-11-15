//! Simple tokenizer for parsing key layout map files.
//!
//! This tokenizer tracks position in a buffer and provides methods for
//! reading tokens, characters, and navigating through lines.

use std::str;

/// Simple tokenizer for parsing ASCII text files line by line.
pub struct Tokenizer {
    filename: String,
    buffer: String,
    current: usize,
    line_number: usize,
}

impl Tokenizer {
    /// Create a tokenizer from file contents.
    pub fn from_contents(filename: &str, contents: &str) -> Self {
        Self {
            filename: filename.to_string(),
            buffer: contents.to_string(),
            current: 0,
            line_number: 1,
        }
    }

    /// Create a tokenizer from a file path.
    pub fn from_file(file_path: &str) -> Result<Self, String> {
        let contents = std::fs::read_to_string(file_path)
            .map_err(|e| format!("Error opening file '{}': {}", file_path, e))?;
        Ok(Self::from_contents(file_path, &contents))
    }

    /// Returns true if at the end of the file.
    pub fn is_eof(&self) -> bool {
        self.current >= self.buffer.len()
    }

    /// Returns true if at the end of the line or end of the file.
    pub fn is_eol(&self) -> bool {
        self.is_eof() || self.peek_char() == '\n'
    }

    /// Gets the name of the file.
    pub fn filename(&self) -> &str {
        &self.filename
    }

    /// Gets a 1-based line number index for the current position.
    pub fn line_number(&self) -> usize {
        self.line_number
    }

    /// Formats a location string consisting of the filename and current line number.
    /// Returns a string like "MyFile.txt:33".
    pub fn get_location(&self) -> String {
        format!("{}:{}", self.filename, self.line_number)
    }

    /// Gets the character at the current position.
    /// Returns null character at end of file.
    pub fn peek_char(&self) -> char {
        if self.is_eof() {
            '\0'
        } else {
            self.buffer[self.current..].chars().next().unwrap_or('\0')
        }
    }

    /// Gets the remainder of the current line as a string, excluding the newline character.
    pub fn peek_remainder_of_line(&self) -> String {
        if self.is_eof() {
            return String::new();
        }

        let remaining = &self.buffer[self.current..];
        let line_end = remaining
            .find('\n')
            .map(|pos| self.current + pos)
            .unwrap_or(self.buffer.len());

        self.buffer[self.current..line_end].to_string()
    }

    /// Gets the character at the current position and advances past it.
    /// Returns null character at end of file.
    pub fn next_char(&mut self) -> char {
        if self.is_eof() {
            return '\0';
        }

        let ch = self.peek_char();
        if ch == '\n' {
            self.line_number += 1;
        }
        self.current += ch.len_utf8();
        ch
    }

    /// Gets the next token on this line stopping at the specified delimiters
    /// or the end of the line whichever comes first.
    /// Returns the token or an empty string if the current character is a delimiter
    /// or is at the end of the line.
    pub fn next_token(&mut self, delimiters: &str) -> String {
        self.skip_delimiters(delimiters);

        if self.is_eol() {
            return String::new();
        }

        let start = self.current;
        while !self.is_eol() && !is_delimiter(self.peek_char(), delimiters) {
            self.next_char();
        }

        self.buffer[start..self.current].to_string()
    }

    /// Advances to the next line.
    /// Does nothing if already at the end of the file.
    pub fn next_line(&mut self) {
        while !self.is_eof() && self.peek_char() != '\n' {
            self.next_char();
        }
        if !self.is_eof() {
            self.next_char(); // consume the newline
        }
    }

    /// Skips over the specified delimiters in the line.
    pub fn skip_delimiters(&mut self, delimiters: &str) {
        while !self.is_eol() && is_delimiter(self.peek_char(), delimiters) {
            self.next_char();
        }
    }
}

fn is_delimiter(ch: char, delimiters: &str) -> bool {
    delimiters.contains(ch)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_is_eof() {
        let tokenizer = Tokenizer::from_contents("test.txt", "");
        assert!(tokenizer.is_eof());

        let tokenizer = Tokenizer::from_contents("test.txt", "a");
        assert!(!tokenizer.is_eof());
    }

    #[test]
    fn test_is_eol() {
        let tokenizer = Tokenizer::from_contents("test.txt", "");
        assert!(tokenizer.is_eol());

        let mut tokenizer = Tokenizer::from_contents("test.txt", "a\nb");
        assert!(!tokenizer.is_eol());
        tokenizer.next_char();
        assert!(tokenizer.is_eol());
    }

    #[test]
    fn test_peek_char() {
        let mut tokenizer = Tokenizer::from_contents("test.txt", "abc");
        assert_eq!(tokenizer.peek_char(), 'a');
        assert_eq!(tokenizer.peek_char(), 'a'); // Should not advance

        tokenizer.next_char();
        assert_eq!(tokenizer.peek_char(), 'b');
    }

    #[test]
    fn test_next_char() {
        let mut tokenizer = Tokenizer::from_contents("test.txt", "abc");
        assert_eq!(tokenizer.next_char(), 'a');
        assert_eq!(tokenizer.next_char(), 'b');
        assert_eq!(tokenizer.next_char(), 'c');
        assert_eq!(tokenizer.next_char(), '\0');
    }

    #[test]
    fn test_next_token() {
        let mut tokenizer = Tokenizer::from_contents("test.txt", "key 1 ESCAPE");
        assert_eq!(tokenizer.next_token(" \t"), "key");
        assert_eq!(tokenizer.next_token(" \t"), "1");
        assert_eq!(tokenizer.next_token(" \t"), "ESCAPE");
        assert_eq!(tokenizer.next_token(" \t"), "");
    }

    #[test]
    fn test_next_token_with_multiple_delimiters() {
        let mut tokenizer = Tokenizer::from_contents("test.txt", "key\t1\nESCAPE");
        assert_eq!(tokenizer.next_token(" \t"), "key");
        assert_eq!(tokenizer.next_token(" \t"), "1");
        tokenizer.next_line();
        assert_eq!(tokenizer.next_token(" \t"), "ESCAPE");
    }

    #[test]
    fn test_next_line() {
        let mut tokenizer = Tokenizer::from_contents("test.txt", "line1\nline2\nline3");
        assert_eq!(tokenizer.peek_remainder_of_line(), "line1");
        tokenizer.next_line();
        assert_eq!(tokenizer.peek_remainder_of_line(), "line2");
        assert_eq!(tokenizer.line_number(), 2);
        tokenizer.next_line();
        assert_eq!(tokenizer.peek_remainder_of_line(), "line3");
        assert_eq!(tokenizer.line_number(), 3);
    }

    #[test]
    fn test_skip_delimiters() {
        let mut tokenizer = Tokenizer::from_contents("test.txt", "   key");
        tokenizer.skip_delimiters(" \t");
        assert_eq!(tokenizer.peek_char(), 'k');
    }

    #[test]
    fn test_get_location() {
        let mut tokenizer = Tokenizer::from_contents("test.txt", "line1\nline2");
        assert_eq!(tokenizer.get_location(), "test.txt:1");
        tokenizer.next_line();
        assert_eq!(tokenizer.get_location(), "test.txt:2");
    }

    #[test]
    fn test_peek_remainder_of_line() {
        let tokenizer = Tokenizer::from_contents("test.txt", "key 1 ESCAPE\nnext line");
        assert_eq!(tokenizer.peek_remainder_of_line(), "key 1 ESCAPE");

        let mut tokenizer = Tokenizer::from_contents("test.txt", "key 1 ESCAPE\nnext line");
        tokenizer.next_line();
        assert_eq!(tokenizer.peek_remainder_of_line(), "next line");
    }

    #[test]
    fn test_handles_comments() {
        let mut tokenizer = Tokenizer::from_contents("test.txt", "key 1 ESCAPE # comment");
        assert_eq!(tokenizer.next_token(" \t"), "key");
        assert_eq!(tokenizer.next_token(" \t"), "1");
        assert_eq!(tokenizer.next_token(" \t"), "ESCAPE");
        assert_eq!(tokenizer.peek_char(), ' ');
        tokenizer.skip_delimiters(" \t");
        assert_eq!(tokenizer.peek_char(), '#');
    }

    #[test]
    fn test_handles_empty_lines() {
        let mut tokenizer = Tokenizer::from_contents("test.txt", "line1\n\nline3");
        assert_eq!(tokenizer.peek_remainder_of_line(), "line1");
        tokenizer.next_line();
        assert_eq!(tokenizer.peek_remainder_of_line(), "");
        tokenizer.next_line();
        assert_eq!(tokenizer.peek_remainder_of_line(), "line3");
    }

    #[test]
    fn test_handles_whitespace_only_lines() {
        let mut tokenizer = Tokenizer::from_contents("test.txt", "line1\n   \nline3");
        assert_eq!(tokenizer.peek_remainder_of_line(), "line1");
        tokenizer.next_line();
        assert_eq!(tokenizer.peek_remainder_of_line(), "   ");
        tokenizer.next_line();
        assert_eq!(tokenizer.peek_remainder_of_line(), "line3");
    }

    #[test]
    fn test_multiple_lines_sequential() {
        let mut tokenizer = Tokenizer::from_contents("test.txt", "key 1 A\nkey 2 B\nkey 3 C");
        assert_eq!(tokenizer.next_token(" \t"), "key");
        assert_eq!(tokenizer.next_token(" \t"), "1");
        assert_eq!(tokenizer.next_token(" \t"), "A");
        tokenizer.next_line();

        assert_eq!(tokenizer.next_token(" \t"), "key");
        assert_eq!(tokenizer.next_token(" \t"), "2");
        assert_eq!(tokenizer.next_token(" \t"), "B");
        tokenizer.next_line();

        assert_eq!(tokenizer.next_token(" \t"), "key");
        assert_eq!(tokenizer.next_token(" \t"), "3");
        assert_eq!(tokenizer.next_token(" \t"), "C");
    }
}

