//! Simple tokenizer for parsing key layout map files.
//!
//! This tokenizer tracks position in a buffer and provides methods for
//! reading tokens, characters, and navigating through lines.

use std::path::PathBuf;
use std::str;

/// Simple tokenizer for parsing ASCII text files line by line.
pub struct Tokenizer {
    file_path: PathBuf,
    buffer: String,
    current: usize,
    line_number: usize,
}

impl Tokenizer {
    /// Create a tokenizer from file contents.
    pub fn from_contents(file_path: PathBuf, contents: &str) -> Self {
        Self {
            file_path,
            buffer: contents.to_string(),
            current: 0,
            line_number: 1,
        }
    }

    /// Create a tokenizer from a file path.
    pub fn from_file(file_path: PathBuf) -> Result<Self, String> {
        let contents = std::fs::read_to_string(file_path.clone())
            .map_err(|e| format!("Error opening file '{:?}': {}", file_path.clone(), e))?;
        Ok(Self::from_contents(file_path.clone(), &contents))
    }

    /// Returns true if at the end of the file.
    pub fn is_eof(&self) -> bool {
        self.current >= self.buffer.len()
    }

    /// Returns true if at the end of the line or end of the file.
    pub fn is_eol(&self) -> bool {
        self.is_eof() || self.peek_char() == '\n'
    }

    /// Formats a location string consisting of the filename and current line number.
    /// Returns a string like "MyFile.txt:33".
    pub fn get_location(&self) -> String {
        format!(
            "{}:{}",
            self.file_path.to_str().unwrap_or(""),
            self.line_number
        )
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
