cmake_minimum_required(VERSION 3.22.1)
project("pocketbansuri")

# Declares the native library and its source files
add_library(
    native-lib
    SHARED
    native-lib.cpp
)

# Finds the Android-specific logging library
find_library(
    log-lib
    log
)

# Links the logging library to our native-lib
target_link_libraries(
    native-lib
    ${log-lib}
)
