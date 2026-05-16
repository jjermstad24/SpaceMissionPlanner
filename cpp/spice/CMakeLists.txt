add_library(spice STATIC
    src/ephemeris.cpp
)

target_include_directories(spice PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/eigen
    ${CMAKE_SOURCE_DIR}/cpp
    ${CMAKE_SOURCE_DIR}/cpp/core
    ${CMAKE_SOURCE_DIR}/cpp/core/time/include
    ${CMAKE_SOURCE_DIR}/cpp/core/frames/include
    ${CMAKE_SOURCE_DIR}/cpp/core/state/include
    ${CMAKE_SOURCE_DIR}/cpp/cspice/include
)

if(EXISTS ${CMAKE_SOURCE_DIR}/cpp/cspice/lib/cspice.a)
    add_library(cspice STATIC IMPORTED)
    set_target_properties(cspice PROPERTIES
        IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/cpp/cspice/lib/cspice.a
        INTERFACE_INCLUDE_DIRECTORIES ${CMAKE_SOURCE_DIR}/cpp/cspice/include
    )
    
    if(EXISTS ${CMAKE_SOURCE_DIR}/cpp/cspice/lib/csupport.a)
        add_library(cspice_support STATIC IMPORTED)
        set_target_properties(cspice_support PROPERTIES
            IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/cpp/cspice/lib/csupport.a
        )
        target_link_libraries(spice PUBLIC cspice_support)
    endif()
    
    target_link_libraries(spice PUBLIC cspice)
else()
    message(FATAL_ERROR "CSPICE library not found in cpp/cspice/lib/")
endif()

target_compile_options(spice PRIVATE
    -Wno-unused-parameter
    -Wno-sign-compare
    -fPIC
)