#!/usr/bin/env sh
set -eu
source_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
output=$1
jdk=$2
mkdir -p "$(dirname -- "$output")"
"${CXX:-c++}" -std=c++20 -O2 -fPIC -shared -pthread -Wall -Wextra -Werror \
    -I"$jdk/include" -I"$jdk/include/linux" -I"$source_dir/vendor" \
    "$source_dir/linux_readback.cpp" -ldl -o "$output"
