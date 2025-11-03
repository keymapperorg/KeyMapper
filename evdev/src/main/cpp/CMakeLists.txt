# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("evdev")

set(CMAKE_CXX_STANDARD 20)

set(C_FLAGS "-Werror=format -fdata-sections -ffunction-sections -fno-exceptions -fno-rtti -fno-threadsafe-statics")
set(LINKER_FLAGS "-Wl,--hash-style=both")

if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    message("Building Release...")

    set(C_FLAGS "${C_FLAGS} -O2 -fvisibility=hidden -fvisibility-inlines-hidden")
    set(LINKER_FLAGS "${LINKER_FLAGS} -Wl,-exclude-libs,ALL -Wl,--gc-sections")
else ()
    message("Building Debug...")

    add_definitions(-DDEBUG)
endif ()

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${C_FLAGS}")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${C_FLAGS}")

set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} ${LINKER_FLAGS}")
set(CMAKE_MODULE_LINKER_FLAGS "${CMAKE_MODULE_LINKER_FLAGS} ${LINKER_FLAGS}")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
#
# In this top level CMakeLists.txt, ${CMAKE_PROJECT_NAME} is used to define
# the target library name; in the sub-module's CMakeLists.txt, ${PROJECT_NAME}
# is preferred for the same purpose.
#
# In order to load a library into your app from Java/Kotlin, you must call
# System.loadLibrary() and pass the name of the library defined here;
# for GameActivity/NativeActivity derived applications, the same library name must be
# used in the AndroidManifest.xml file.
add_library(evdev SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        libevdev_jni.cpp
        libevdev/libevdev.c
        libevdev/libevdev-names.c
        libevdev/libevdev-uinput.c
        android/input/KeyLayoutMap.cpp
        android/input/InputEventLabels.cpp
        android/libbase/result.cpp
        android/utils/Tokenizer.cpp
        android/utils/String16.cpp
        android/utils/String8.cpp
        android/utils/SharedBuffer.cpp
        android/utils/FileMap.cpp
        android/utils/Unicode.cpp
        android/input/InputDevice.cpp
        android/input/Input.cpp
        android/libbase/stringprintf.cpp
        ${aidl_src_dir}/io/github/sds100/keymapper/evdev/IEvdevCallback.cpp)

find_library(
        binder_ndk-lib
        binder_ndk
)

# Specifies libraries CMake should link to your target library. You
# can link libraries from various origins, such as libraries defined in this
# build script, prebuilt third-party libraries, or Android system libraries.
target_link_libraries(evdev
        # List libraries link to the target library
        android
        log
        ${binder_ndk-lib})

# Add include directories for header files
target_include_directories(evdev PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/android
        ${CMAKE_CURRENT_SOURCE_DIR}/android/input
        ${CMAKE_CURRENT_SOURCE_DIR}/android/libbase
        ${CMAKE_CURRENT_SOURCE_DIR}/android/utils
        ${CMAKE_CURRENT_SOURCE_DIR}/libevdev)

