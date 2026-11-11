#!/usr/bin/env python3
"""Round 2 of the Home directions: the A Shelf + B Spotlight remix in two carousel styles (D card carousel, E spotlight + filmstrip).
Run:  python3 docs/design/home/scripts/gen_round2.py
Reuses the round 1 pieces from gen_options.py; Libraries section and the duplicate Continue watching row are gone."""
import importlib.util
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("r1", HERE / "gen_options.py")
r1 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(r1)
ic, art, poster, wide_card, section, topbar, nav, EDIT, frame, cell = (
    r1.ic, r1.art, r1.poster, r1.wide_card, r1.section, r1.topbar, r1.nav, r1.EDIT, r1.frame, r1.cell)

OUT = HERE.parent / "options" / "round-2"
SLIDES = [
    dict(k="Northbound · S2 · E5", t="The Long Way Round", m="23 min left", p=56, art="sea", kind="resume",
         aria="Northbound, season 2 episode 5, The Long Way Round, 23 minutes left"),
    dict(k="Movie · 2023", t="Salt and Signal", m="1 h 12 min left", p=31, art="dawn", kind="resume", aria="Salt and Signal, 1 hour 12 minutes left"),
    dict(k="Copper Valley · S1 · E4", t="Ash on the Wind", m="Next up · 48 min", p=None, art="hills", kind="next", aria="Next up: Copper Valley, season 1 episode 4, Ash on the Wind"),
    dict(k="The Night Desk · S3 · E1", t="Deadline", m="9 min left", p=82, art="city", kind="resume", aria="The Night Desk, season 3 episode 1, Deadline, 9 minutes left"),
    dict(k="Movie · 2021", t="Paper Lanterns", m="38 min left", p=64, art="ember", kind="resume", aria="Paper Lanterns, 38 minutes left"),
]
TOTAL = 8


def lifted(html):
    """Every card link gets the shared lift animation."""
    return html.replace('class="mc-hit ', 'class="mc-hit lift ')


def controls(pos=1, dots=False):
    d = ('<span class="ca-dots" aria-hidden="true">' + "".join(f'<span{" class=\"on\"" if i == pos - 1 else ""}></span>' for i in range(5)) + '</span>') if dots else ""
    return (f'<div class="ca-ctl"><span class="pos" aria-live="polite">{pos} of {TOTAL}</span>{d}'
            f'<button class="icon-btn" aria-label="Previous title"{" disabled" if pos == 1 else ""}>{ic("chevron_left")}</button>'
            f'<button class="icon-btn" aria-label="Next title">{ic("chevron_right")}</button></div>')


def play_btn(s, cls):
    label, icon = ("Play", "play_arrow") if s["kind"] == "next" else ("Resume", "play_arrow")
    return f'<button class="btn {cls}" aria-label="{label} {s["aria"]}">{ic(icon, "fill")}{label}</button>'


def shelves(desk):
    n = 7 if desk else 5
    movies = section("New in Movies", "24", r1.rail([poster(t, y, s, "new" if b else None) for t, y, s, b in r1.NEW_MOVIES[:n]], "New in Movies"))
    shows = section("New in TV Shows", "11", r1.rail([poster(t, sub, s) for t, sub, s in r1.NEW_SHOWS], "New in TV Shows"))
    favs = section("Favorites", "9", r1.rail([poster(t, sub, s, "watched" if i == 0 else None) for i, (t, sub, s) in enumerate(r1.FAVS)], "Favorites"))
    dls = section("Downloaded", "2", r1.rail([wide_card(c) for c in r1.DOWNLOADED], "Downloaded"))
    return lifted(movies + shows + favs + dls) + EDIT


def screen(top, desk):
    return f'<div class="hm r2{" desk" if desk else ""}">{topbar()}<main class="hm-body">{top}{shelves(desk)}</main>{nav(desk)}</div>'


# ---------------------------------------------------------------- D · card carousel
def d_big(s):
    tag = f'<span class="ca-tag">{ic("skip_next")}Next up</span>' if s["kind"] == "next" else ""
    bar = f'<span class="bar" style="--p:{s["p"]}%" aria-hidden="true"></span>' if s["p"] else ""
    return (f'<div class="ca-item big" role="group" aria-roledescription="slide" aria-label="1 of {TOTAL}: {s["t"]}">{art(s["art"])}{tag}<div class="in">'
            f'<span class="k">{s["k"]}</span><h3><a href="#" aria-label="{s["t"]}, open details">{s["t"]}</a></h3>'
            f'<div class="meta">{bar}<span>{s["m"]}</span></div>'
            f'<div class="acts">{play_btn(s, "big")}<button class="icon-btn" aria-label="More options for {s["t"]}">{ic("more_vert")}</button></div></div></div>')


def d_small(s, cls, i):
    return f'<a class="ca-item {cls}" href="#" role="group" aria-roledescription="slide" aria-label="{i} of {TOTAL}: {s["t"]}. Show this title">{art(s["art"])}</a>'


def d_screen(desk=False):
    items = d_big(SLIDES[0]) + (d_small(SLIDES[1], "mid", 2) + d_small(SLIDES[2], "peek", 3) if desk else d_small(SLIDES[1], "peek", 2))
    head = (f'<div class="ca-head"><h2 id="h-cw">Continue watching</h2>'
            + (controls() if desk else "") + f'<a class="btn text" href="#" aria-label="See all {TOTAL} in Continue watching">See all{ic("chevron_right")}</a></div>')
    top = (f'<section class="hm-sec" aria-labelledby="h-cw" aria-roledescription="carousel">{head}<div class="ca">{items}</div>'
           + ("" if desk else controls(dots=True)) + '</section>')
    return screen(top, desk)


# ---------------------------------------------------------------- E · spotlight + filmstrip
def e_strip(selected=0):
    out = ""
    for i, s in enumerate(SLIDES):
        bar = f'<span class="bar" style="--p:{s["p"]}%"></span>' if s["p"] else ""
        tag = f'<span class="badge-art left new">{ic("skip_next")}Next</span>' if s["kind"] == "next" else ""
        out += (f'<li><button class="lift" role="tab" aria-selected="{"true" if i == selected else "false"}" aria-label="{s["aria"]}">'
                f'<span class="mc-art">{art(s["art"])}{tag}{bar}</span><span class="truncate">{s["t"]}</span></button></li>')
    return f'<ul class="strip" role="tablist" aria-label="Continue watching, {TOTAL} titles">{out}</ul>'


def e_screen(desk=False):
    s = SLIDES[0]
    hero = (f'<div class="fs" role="tabpanel"><div class="artbox">{art(s["art"])}</div><div class="in">'
            f'<span class="hb-kicker">{ic("history")}Continue watching · 1 of {TOTAL}</span>'
            f'<h2 class="title"><a href="#" aria-label="{s["t"]}, open details">{s["t"]}</a></h2>'
            f'<p class="meta">{s["k"]}</p>'
            f'<div class="prog"><span class="bar thick" style="--p:{s["p"]}%" aria-hidden="true"></span><span>{s["m"]}</span></div>'
            f'<div class="acts">{play_btn(s, "huge")}<button class="icon-btn tonal xl" aria-label="More options for {s["t"]}">{ic("more_vert")}</button></div></div></div>')
    strip_head = (f'<div class="hm-head"><h2 id="h-cw" class="v-sr">Continue watching</h2><span style="flex:1"></span>'
                  + (controls() if desk else "") + f'<a class="btn text" href="#" aria-label="See all {TOTAL} in Continue watching">See all {TOTAL}{ic("chevron_right")}</a></div>')
    top = f'<section class="hm-sec" aria-labelledby="h-cw" aria-roledescription="carousel">{hero}{strip_head}{e_strip()}</section>'
    return screen(top, desk)


OPTIONS = {
    "d": dict(
        name="Marquee carousel",
        lead="A's card, B's scale. Continue watching becomes the top of Home: a Material 3 hero carousel with one large card (text on the dark scrim, headline title, 56 px Resume) and the next title peeking at the edge so it's obvious there's more. '1 of 8' with Previous / Next buttons sits under it on phone and beside the heading on desktop, where two more titles show.",
        trade="Trade-off: only one title is readable at a time; the peeking cards are artwork only (their names are in their labels).",
        mood="Mood: familiar, cinematic, tactile · Carousel: large card + peek, never auto-advances · Shelves: A's cards at B's larger sizes, shared lift on hover",
    ),
    "e": dict(
        name="Spotlight + filmstrip",
        lead="B's full-bleed hero, A's calm shelves. The artwork of the selected title fills the top and fades into the page, with the title in display size and a 64 px Resume. A filmstrip of all titles in Continue watching sits right under it: tap or arrow to a thumbnail and the spotlight changes. Every title, its progress and Next up stay visible at once.",
        trade="Trade-off: the most dramatic and closest to the Details screen; the spotlight takes most of the first phone screen.",
        mood="Mood: cinematic, bold, clear · Carousel: spotlight + filmstrip tabs, never auto-advances · Shelves: A's cards at B's larger sizes, shared lift on hover",
    ),
}
SCREENS = {"d": d_screen, "e": e_screen}


def specimen(key):
    p = lambda st: poster("Kestrel", "2023", "frost", None, st).replace('class="mc-hit ', 'class="mc-hit lift ')
    states = r1.rail([p(""), p("is-hover"), p("is-pressed"), p("is-focus")], "Lift states")
    car = ({"d": f'<div class="hm r2"><div class="ca" style="padding:0">{d_big(SLIDES[2])}{d_small(SLIDES[3], "peek is-hover", 4)}</div>{controls(3, dots=True)}</div>',
            "e": f'<div class="hm r2">{e_strip(selected=2)}{controls(3)}</div>'})[key]
    cap = {"d": "Carousel · a Next up slide (Play instead of Resume, tag with icon + text) and the peeking card on hover. Previous / Next and '3 of 8' are always visible; arrow keys move one title; screen readers hear 'slide 3 of 8'.",
           "e": "Filmstrip · a tab list: the selected thumbnail has a 3 px ring and bold name (not colour alone). Arrow keys move the selection, Enter plays. Previous / Next on desktop."}[key]
    return f'''<div class="app sp">
<figure class="wide"><div class="hm r2">{states}</div><figcaption>Shared lift on every card · rest, hover (scale 1.04 + shadow, artwork zooms 1.06 inside the frame), pressed (0.97), keyboard focus (lift + 3 px ring). Hover the cards in the frames above to see it move. With reduced motion only the shadow and ring change.</figcaption></figure>
<figure class="wide">{car}<figcaption>{cap}</figcaption></figure>
</div>'''


def option_section(key):
    o = OPTIONS[key]
    s = SCREENS[key]
    return f'''<section class="v-option opt-{key}" data-tokens="opt-{key}" id="opt-{key}">
<header><h2>{key.upper()} · {o["name"]}</h2><p>{o["lead"]}</p><p>{o["trade"]}</p><p class="v-label">{o["mood"]}</p></header>
<div class="v-section"><h2>Phone</h2><p>Scroll inside the frames; hover any card to see the lift.</p>
<div class="px-row">{cell("Phone · light", frame("phone", s(), "light"))}{cell("Phone · dark", frame("phone", s(), "dark"))}</div></div>
<div class="v-section"><h2>Desktop</h2>
<div class="px-row">{cell("Desktop · dark · 1280 × 800", frame("desktop", s(desk=True), "dark", "z60"))}{cell("Desktop · light", frame("desktop", s(desk=True), "light", "z60"))}</div></div>
<div class="v-section"><h2>New pieces</h2>{specimen(key)}</div>
</section>'''


INTRO = '''<section class="v-section"><h2>Round 2 · A + B remix, two carousel styles</h2>
<p>Your feedback, applied to both: the Libraries section is gone (it's a tab), Continue watching moved into the top of Home as a carousel so it no longer appears twice, shelves use A's cards at B's larger sizes, and every card shares one lift animation on hover and focus (no per-card shape morph). The carousel never moves by itself. The two options differ only in how the carousel looks.</p>
<div class="px-scroll"><table class="px-table"><thead><tr><th></th><th>D · Marquee carousel</th><th>E · Spotlight + filmstrip</th></tr></thead><tbody>
<tr><th>Top of Home</th><td>Large card with text on a scrim + peek of the next card</td><td>Full-bleed artwork fading into the page, display title below</td></tr>
<tr><th>Moving between titles</th><td>Swipe, Previous / Next, arrow keys; '1 of 8'</td><td>Tap a filmstrip thumbnail, arrow keys; Previous / Next on desktop</td></tr>
<tr><th>Titles visible at once</th><td>1 readable + 1–2 peeking</td><td>All 5 thumbnails with names and progress</td></tr>
<tr><th>Resume button</th><td>56 px on the card</td><td>64 px, full width on phone</td></tr>
<tr><th>Closest to</th><td>Round 1 A</td><td>Round 1 B / the Details screen</td></tr>
</tbody></table></div></section>'''


def page():
    tokens = "\n".join(f'<!-- tokens: ../option-{k}.tokens.json scope=.opt-{k} id=opt-{k} -->' for k in OPTIONS)
    compare = "".join(f'<div class="px-cell opt-{k}" data-tokens="opt-{k}"><p class="v-label">{k.upper()} · {o["name"]}</p>{frame("phone", SCREENS[k]())}</div>'
                      for k, o in OPTIONS.items())
    body = INTRO + "".join(option_section(k) for k in OPTIONS) + (
        f'<section class="v-section" id="compare"><h2>Side by side</h2><p>Follows the theme toggle.</p><div class="px-row">{compare}</div></section>')
    return f'''<title>EmbyEx Home Directions</title>
{tokens}
<style><!-- include: viewer.css --></style>
<style><!-- include: components.css --></style>
<style><!-- include: options.css --></style>
<script>window.DESIGN = {{ app: "EmbyEx Home · Round 2", mode: "options", themes: ["light", "dark"] }};</script>
<div class="v-options">
{body}
</div>
<script><!-- include: viewer.js --></script>
'''


def main():
    for key, o in OPTIONS.items():
        data = r1.make_tokens(f'Marquee · {o["name"]}', o["lead"].split(". ")[0] + ".", {"hero": 380})
        (OUT / f"option-{key}.tokens.json").write_text(json.dumps(data, indent=1, ensure_ascii=False) + "\n")
    (OUT / "src" / "index.html").write_text(page())
    print("wrote", OUT / "src" / "index.html")


if __name__ == "__main__":
    main()
