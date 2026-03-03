cmake_minimum_required(VERSION 3.16)
project(sdtp_validator)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(OpenSSL REQUIRED)

add_executable(validator src/main.cpp)
target_link_libraries(validator PRIVATE OpenSSL::SSL OpenSSL::Crypto pthread)
