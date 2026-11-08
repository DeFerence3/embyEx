#!/bin/sh
# Installs the latest EmbyEx desktop release on Linux (x86_64):
#   Arch Linux and derivatives: the .pkg.tar.zst, with pacman
#   Debian, Ubuntu and derivatives: the .deb, with apt
#
#   curl -fsSL https://raw.githubusercontent.com/DeFerence3/embyEx/master/scripts/install.sh | sh
#   curl -fsSL https://raw.githubusercontent.com/DeFerence3/embyEx/master/scripts/install.sh | sh -s -- --beta
#
# --beta installs the newest release including pre-releases (vX.Y.Z-beta.N).

set -eu

REPO="DeFerence3/embyEx"
API="https://api.github.com/repos/$REPO/releases"

say() { printf '%s\n' "$*"; }
die() { printf 'error: %s\n' "$*" >&2; exit 1; }

usage() {
    say "Usage: install.sh [--beta]"
    say "  --beta  install the newest release, pre-releases included"
}

fetch() {
    curl -fsSL -H "Accept: application/vnd.github+json" "$1" ||
        die "Could not reach GitHub ($1). Check your connection, or try again later if GitHub is rate limiting."
}

# Prints the tag of the newest release, pre-releases included: highest MAJOR.MINOR.PATCH,
# and a beta before the release it precedes.
newest_tag() {
    fetch "$API?per_page=30" |
        grep -o '"tag_name": *"[^"]*"' |
        sed 's/.*"\([^"]*\)"$/\1/' |
        grep -E '^v[0-9]+\.[0-9]+\.[0-9]+(-beta\.[0-9]+)?$' |
        awk '{
            v = substr($0, 2); beta = 999
            if (index(v, "-beta.") > 0) { beta = substr(v, index(v, "-beta.") + 6); v = substr(v, 1, index(v, "-beta.") - 1) }
            split(v, p, ".")
            printf "%d %d %d %d %s\n", p[1], p[2], p[3], beta, $0
        }' |
        sort -k1,1n -k2,2n -k3,3n -k4,4n |
        tail -n 1 |
        cut -d' ' -f5
}

main() {
    beta=0
    for arg in "$@"; do
        case "$arg" in
            --beta) beta=1 ;;
            -h | --help) usage; exit 0 ;;
            *) usage >&2; die "Unknown option: $arg" ;;
        esac
    done

    [ "$(uname -s)" = "Linux" ] || die "This installer is for Linux. On Windows, use install.ps1."
    case "$(uname -m)" in
        x86_64 | amd64) ;;
        *) die "EmbyEx is only published for x86_64 Linux, not $(uname -m)." ;;
    esac
    command -v curl > /dev/null 2>&1 || die "curl is required."

    if command -v pacman > /dev/null 2>&1; then
        format="Arch package"
        pattern='-x86_64\.pkg\.tar\.zst$'
    elif command -v apt-get > /dev/null 2>&1; then
        format="Debian package"
        pattern='_amd64\.deb$'
    else
        die "Unsupported distribution: EmbyEx is packaged for Arch Linux (pacman) and Debian/Ubuntu (apt)."
    fi

    if [ "$beta" -eq 1 ]; then
        tag=$(newest_tag)
        [ -n "$tag" ] || die "No published releases were found."
        release=$(fetch "$API/tags/$tag")
    else
        release=$(fetch "$API/latest")
        tag=$(printf '%s' "$release" | grep -o '"tag_name": *"[^"]*"' | head -n 1 | sed 's/.*"\([^"]*\)"$/\1/')
    fi

    url=$(printf '%s' "$release" |
        grep -o '"browser_download_url": *"[^"]*"' |
        sed 's/.*"\([^"]*\)"$/\1/' |
        grep -E "$pattern" |
        head -n 1) || true
    [ -n "$url" ] || die "Release $tag has no $format for x86_64."

    sudo=""
    if [ "$(id -u)" -ne 0 ]; then
        command -v sudo > /dev/null 2>&1 || die "Run as root, or install sudo."
        sudo="sudo"
    fi

    workdir=$(mktemp -d)
    trap 'rm -rf "$workdir"' EXIT INT TERM
    file="$workdir/${url##*/}"

    say "Downloading EmbyEx $tag ($format)..."
    curl -fL --progress-bar -o "$file" "$url" || die "Download failed: $url"

    say "Installing..."
    # --noconfirm / -y: when piped into sh, standard input is this script, not the terminal.
    case "$format" in
        "Arch package") $sudo pacman -U --noconfirm "$file" ;;
        *) $sudo apt-get install -y "$file" ;;
    esac

    say "EmbyEx $tag is installed. Launch it from your application menu or run: embyex"
}

# Everything runs from main, so a partially downloaded script does nothing.
main "$@"
