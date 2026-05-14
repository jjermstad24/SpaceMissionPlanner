function(configure_settings)
    if(CMAKE_CXX_COMPILER_ID MATCHES "GNU|Clang")
        add_compile_options(
            -Wall
            -Wextra
            -Wpedantic
            -Werror
        )
    elseif(MSVC)
        add_compile_options(/W4 /permissive-)
    endif()

    if(CMAKE_BUILD_TYPE STREQUAL "Debug")
        add_compile_options(-g -O0)
    elseif(CMAKE_BUILD_TYPE STREQUAL "Release")
        add_compile_options(-O3)
    endif()
endfunction()