cmake_minimum_required(VERSION 3.22.1)

project("magic-native")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(LLAMA_SRC
    ${CMAKE_CURRENT_LIST_DIR}/../../../../../llama.cpp
)

add_library(magic-native SHARED
    magic_jni.cpp
)

target_include_directories(magic-native PRIVATE
    ${LLAMA_SRC}/include
    ${LLAMA_SRC}/common
    ${LLAMA_SRC}/ggml/include
)

target_link_libraries(magic-native
    ${LLAMA_SRC}/build-android/bin/libllama.so
    ${LLAMA_SRC}/build-android/bin/libllama-common.so
    ${LLAMA_SRC}/build-android/bin/libggml.so
    ${LLAMA_SRC}/build-android/bin/libggml-base.so
    ${LLAMA_SRC}/build-android/bin/libggml-cpu.so
    android
    log
)
