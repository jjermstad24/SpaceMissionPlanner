# CMake config for CSPICE
# CSPICE is loaded as a submodule from https://github.com/nravic/CSPICE

if(EXISTS "${CMAKE_SOURCE_DIR}/cpp/cspice/lib/cspice.a")
    add_library(cspice STATIC IMPORTED GLOBAL)
    set_target_properties(cspice PROPERTIES
        IMPORTED_LOCATION "${CMAKE_SOURCE_DIR}/cpp/cspice/lib/cspice.a"
        INTERFACE_INCLUDE_DIRECTORIES "${CMAKE_SOURCE_DIR}/cpp/cspice/include"
    )
    
    if(EXISTS "${CMAKE_SOURCE_DIR}/cpp/cspice/lib/csupport.a")
        add_library(cspice_support STATIC IMPORTED GLOBAL)
        set_target_properties(cspice_support PROPERTIES
            IMPORTED_LOCATION "${CMAKE_SOURCE_DIR}/cpp/cspice/lib/csupport.a"
        )
        target_link_libraries(cspice INTERFACE cspice_support)
    endif()
    
    message(STATUS "CSPICE linked successfully")
else()
    message(FATAL_ERROR "CSPICE library not found at cpp/cspice/lib/cspice.a")
endif()