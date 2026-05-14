add_subdirectory(two_body)

add_library(astro INTERFACE)
target_include_directories(astro INTERFACE ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(astro INTERFACE two_body)