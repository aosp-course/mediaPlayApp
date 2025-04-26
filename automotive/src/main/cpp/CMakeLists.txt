# Definindo a versão mínima do CMake
cmake_minimum_required(VERSION 3.22.1)

# Adicione a biblioteca compartilhada
add_library(
        # Nome da biblioteca que será criada
        audioeffects-lib

        # Tipo da biblioteca
        SHARED

        # Lista dos arquivos de código fonte
        audio_effects_manager.cpp
)

find_library(
        log-lib
        log
)

# Especificar bibliotecas para linkar a biblioteca nativa
target_link_libraries(
        audioeffects-lib
        ${log-lib}
)