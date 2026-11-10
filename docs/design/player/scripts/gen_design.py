#!/usr/bin/env python3
"""Generates the full Dockside player design: src/index.html and src/pages/*.html.
Run from anywhere:  python3 docs/design/player/scripts/gen_design.py
Every frame is assembled from the same pieces, so a change to a piece changes every state. Edit pieces here, not the generated HTML.
Parts: lib (pieces + player frames), menus, pages_player, pages_keyboard, pages_foundations."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from design import pages_foundations, pages_keyboard, pages_player  # noqa: E402
from design.lib import SRC  # noqa: E402

INDEX = '''<title>EmbyEx Player Design</title>
<!-- tokens: ../tokens.json -->
<style><!-- include: viewer.css --></style>
<style><!-- include: components.css --></style>
<style><!-- include: app.css --></style>
<script>window.DESIGN = { app: "EmbyEx · Player", mode: "design", themes: ["light", "dark"], viewports: ["desktop"] };</script>
<div class="v-app">
  <nav class="v-nav" aria-label="Pages"></nav>
  <main class="v-main">
    <!-- include: pages/system.html -->
    <!-- include: pages/shell.html -->
    <!-- include: pages/desktop.html -->
    <!-- include: pages/android.html -->
    <!-- include: pages/menus.html -->
    <!-- include: pages/keyboard.html -->
  </main>
</div>
<script><!-- include: viewer.js --></script>
'''


def main():
    pages = {
        "system": pages_foundations.system_page(),
        "shell": pages_foundations.shell_page(),
        "desktop": pages_player.desktop_page(),
        "android": pages_player.android_page(),
        "menus": pages_player.menus_page(),
        "keyboard": pages_keyboard.keyboard_page(),
    }
    (SRC / "pages").mkdir(parents=True, exist_ok=True)
    (SRC / "index.html").write_text(INDEX)
    total = 0
    for name, html in pages.items():
        (SRC / "pages" / f"{name}.html").write_text(html + "\n")
        total += html.count('class="v-state"')
        print(f"{name}: {html.count('class=\"v-state\"')} states, {len(html) // 1024} KB")
    print("total states:", total)


if __name__ == "__main__":
    main()
