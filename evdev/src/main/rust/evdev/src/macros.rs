macro_rules! string_getter {
    ( $( #[$doc:meta], $func_name:ident, $c_func: ident ),* ) => {
        $(
            #[$doc]
            fn $func_name (&self) -> Option<&str> {
                unsafe {
                    ptr_to_str(libevdev::$c_func(self.raw()))
                }
            }
        )*
    };
}

macro_rules! string_setter {
    ( $( $func_name:ident, $c_func: ident ),* ) => {
        $(
            fn $func_name (&self, field: &str) {
                let field = CString::new(field).unwrap();
                unsafe {
                    libevdev::$c_func(self.raw(), field.as_ptr())
                }
            }
        )*
    };
}

macro_rules! product_getter {
    ( $( $func_name:ident, $c_func: ident ),* ) => {
        $(
            fn $func_name (&self) -> u16 {
                unsafe {
                    libevdev::$c_func(self.raw()) as u16
                }
            }
        )*
    };
}

macro_rules! product_setter {
    ( $( $func_name:ident, $c_func: ident ),* ) => {
        $(
            fn $func_name (&self, field: u16) {
                unsafe {
                    libevdev::$c_func(self.raw(), field as c_int);
                }
            }
        )*
    };
}

macro_rules! abs_getter {
    ( $( $func_name:ident, $c_func: ident ),* ) => {
        $(
            fn $func_name (&self,
                               code: u32) -> std::io::Result<i32> {
                let result = unsafe {
                    libevdev::$c_func(self.raw(), code as c_uint) as i32
                };

                match result {
                    0 => Err(std::io::Error::from_raw_os_error(0)),
                    k => Ok(k)
                }
            }
        )*
    };
}

macro_rules! abs_setter {
    ( $( $func_name:ident, $c_func: ident ),* ) => {
        $(
            fn $func_name (&self,
                               code: u32,
                               val: i32) {
                unsafe {
                    libevdev::$c_func(self.raw(), code as c_uint, val as c_int);
                }
            }
        )*
    };
}