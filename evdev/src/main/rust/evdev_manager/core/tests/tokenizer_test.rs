use evdev_manager_core::android::keylayout::tokenizer::Tokenizer;
#[cfg(test)]
use pretty_assertions::assert_eq;
use std::path::PathBuf;

#[test]
fn test_is_eof() {
    let tokenizer = Tokenizer::from_contents(PathBuf::new(), "");
    assert!(tokenizer.is_eof());

    let tokenizer = Tokenizer::from_contents(PathBuf::new(), "a");
    assert!(!tokenizer.is_eof());
}

#[test]
fn test_is_eol() {
    let tokenizer = Tokenizer::from_contents(PathBuf::new(), "");
    assert!(tokenizer.is_eol());

    let mut tokenizer = Tokenizer::from_contents(PathBuf::new(), "a\nb");
    assert!(!tokenizer.is_eol());
    tokenizer.next_char();
    assert!(tokenizer.is_eol());
}

#[test]
fn test_peek_char() {
    let mut tokenizer = Tokenizer::from_contents(PathBuf::new(), "abc");
    assert_eq!(tokenizer.peek_char(), 'a');
    assert_eq!(tokenizer.peek_char(), 'a'); // Should not advance

    tokenizer.next_char();
    assert_eq!(tokenizer.peek_char(), 'b');
}

#[test]
fn test_next_char() {
    let mut tokenizer = Tokenizer::from_contents(PathBuf::new(), "abc");
    assert_eq!(tokenizer.next_char(), 'a');
    assert_eq!(tokenizer.next_char(), 'b');
    assert_eq!(tokenizer.next_char(), 'c');
    assert_eq!(tokenizer.next_char(), '\0');
}

#[test]
fn test_next_token() {
    let mut tokenizer = Tokenizer::from_contents(PathBuf::new(), "key 1 ESCAPE");
    assert_eq!(tokenizer.next_token(" \t"), "key");
    assert_eq!(tokenizer.next_token(" \t"), "1");
    assert_eq!(tokenizer.next_token(" \t"), "ESCAPE");
    assert_eq!(tokenizer.next_token(" \t"), "");
}

#[test]
fn test_next_token_with_multiple_delimiters() {
    let mut tokenizer = Tokenizer::from_contents(PathBuf::new(), "key\t1\nESCAPE");
    assert_eq!(tokenizer.next_token(" \t"), "key");
    assert_eq!(tokenizer.next_token(" \t"), "1");
    tokenizer.next_line();
    assert_eq!(tokenizer.next_token(" \t"), "ESCAPE");
}

#[test]
fn test_next_line() {
    let mut tokenizer = Tokenizer::from_contents(PathBuf::new(), "line1\nline2\nline3");
    assert_eq!(tokenizer.peek_remainder_of_line(), "line1");
    tokenizer.next_line();
    assert_eq!(tokenizer.peek_remainder_of_line(), "line2");
    tokenizer.next_line();
    assert_eq!(tokenizer.peek_remainder_of_line(), "line3");
}

#[test]
fn test_skip_delimiters() {
    let mut tokenizer = Tokenizer::from_contents(PathBuf::new(), "   key");
    tokenizer.skip_delimiters(" \t");
    assert_eq!(tokenizer.peek_char(), 'k');
}

#[test]
fn test_get_location() {
    let mut tokenizer = Tokenizer::from_contents(PathBuf::new().join("test.txt"), "line1\nline2");
    assert_eq!(tokenizer.get_location(), "test.txt:1");
    tokenizer.next_line();
    assert_eq!(tokenizer.get_location(), "test.txt:2");
}

#[test]
fn test_peek_remainder_of_line() {
    let tokenizer = Tokenizer::from_contents(PathBuf::new(), "key 1 ESCAPE\nnext line");
    assert_eq!(tokenizer.peek_remainder_of_line(), "key 1 ESCAPE");

    let mut tokenizer = Tokenizer::from_contents(PathBuf::new(), "key 1 ESCAPE\nnext line");
    tokenizer.next_line();
    assert_eq!(tokenizer.peek_remainder_of_line(), "next line");
}

#[test]
fn test_handles_comments() {
    let mut tokenizer = Tokenizer::from_contents(PathBuf::new(), "key 1 ESCAPE # comment");
    assert_eq!(tokenizer.next_token(" \t"), "key");
    assert_eq!(tokenizer.next_token(" \t"), "1");
    assert_eq!(tokenizer.next_token(" \t"), "ESCAPE");
    assert_eq!(tokenizer.peek_char(), ' ');
    tokenizer.skip_delimiters(" \t");
    assert_eq!(tokenizer.peek_char(), '#');
}

#[test]
fn test_handles_empty_lines() {
    let mut tokenizer = Tokenizer::from_contents(PathBuf::new(), "line1\n\nline3");
    assert_eq!(tokenizer.peek_remainder_of_line(), "line1");
    tokenizer.next_line();
    assert_eq!(tokenizer.peek_remainder_of_line(), "");
    tokenizer.next_line();
    assert_eq!(tokenizer.peek_remainder_of_line(), "line3");
}

#[test]
fn test_handles_whitespace_only_lines() {
    let mut tokenizer = Tokenizer::from_contents(PathBuf::new(), "line1\n   \nline3");
    assert_eq!(tokenizer.peek_remainder_of_line(), "line1");
    tokenizer.next_line();
    assert_eq!(tokenizer.peek_remainder_of_line(), "   ");
    tokenizer.next_line();
    assert_eq!(tokenizer.peek_remainder_of_line(), "line3");
}

#[test]
fn test_multiple_lines_sequential() {
    let mut tokenizer = Tokenizer::from_contents(PathBuf::new(), "key 1 A\nkey 2 B\nkey 3 C");
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
