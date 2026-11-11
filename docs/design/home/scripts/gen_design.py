#!/usr/bin/env python3
"""Generates the Home design pages (direction E · Spotlight + filmstrip) into docs/design/home/src/pages/.
Run:  python3 docs/design/home/scripts/gen_design.py && python3 ~/.claude/skills/design/scripts/design_tools.py build docs/design/home/src/index.html docs/design/home/design.html
Every state is assembled from the same pieces and sample library, so states stay consistent; edit pieces here, not the generated HTML."""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent  # docs/design/home
PAGES = ROOT / "src" / "pages"

# ---------------------------------------------------------------- sample library (invented titles)
SLIDES = [
    dict(k="Northbound · S2 · E5", t="The Long Way Round", m="23 min left", p=56, art="sea", kind="resume", at="28:51",
         aria="Northbound, season 2 episode 5, The Long Way Round, 23 minutes left", series="Northbound"),
    dict(k="Movie · 2023 · 1 h 58 min", t="Salt and Signal", m="1 h 12 min left", p=31, art="dawn", kind="resume", at="46:10",
         aria="Salt and Signal, 1 hour 12 minutes left", series=None),
    dict(k="Copper Valley · S1 · E4", t="Ash on the Wind", m="Next up · 48 min", p=None, art="hills", kind="next", at=None,
         aria="Next up: Copper Valley, season 1 episode 4, Ash on the Wind, 48 minutes", series="Copper Valley"),
    dict(k="The Night Desk · S3 · E1", t="Deadline", m="9 min left", p=82, art="city", kind="resume", at="41:02",
         aria="The Night Desk, season 3 episode 1, Deadline, 9 minutes left", series="The Night Desk"),
    dict(k="Movie · 2021 · 1 h 46 min", t="Paper Lanterns", m="38 min left", p=64, art="ember", kind="resume", at="1:08:00",
         aria="Paper Lanterns, 38 minutes left", series=None),
]
JUST_ADDED = [
    dict(k="Movie · 2024 · 1 h 41 min", t="Low Tide", m="Added today", p=None, art="sea", kind="new", aria="Low Tide, movie, added today", series=None),
    dict(k="Movie · 2024 · 2 h 05 min", t="The Glass Orchard", m="Added yesterday", p=None, art="grove", kind="new", aria="The Glass Orchard, movie, added yesterday", series=None),
    dict(k="Field Notes · 3 new episodes", t="Field Notes", m="Added 2 days ago", p=None, art="hills", kind="new", aria="Field Notes, 3 new episodes", series="Field Notes"),
]
TOTAL = 8
NEW_MOVIES = [("Low Tide", "2024", "sea", "new"), ("The Glass Orchard", "2024", "grove", None), ("Kestrel", "2023", "frost", None),
              ("Halfway to Morning", "2024", "dawn", None), ("Ember Season", "2022", "ember", "new"), ("A Map of Small Hours", "2024", "night", None),
              ("The Quiet Harbour", "2019", "sea", "watched")]
NEW_SHOWS = [("Field Notes", "3 new episodes", "hills", None), ("Copper Valley", "Season 2 added", "frost", None), ("The Night Desk", "1 new episode", "city", None),
             ("Harbour Lights", "New series", "sea", "new"), ("Second Shift", "2 new episodes", "ember", None)]
FAVS = [("Kestrel", "2023", "frost", "watched"), ("Northbound", "Series", "sea", None), ("The Quiet Harbour", "2019", "night", None),
        ("Paper Lanterns", "2021", "ember", None), ("Halfway to Morning", "2024", "dawn", None)]
DOWNLOADED = [dict(k="Northbound · S2 · E6", t="Low Water", m="1.4 GB on this device", art="sea"),
              dict(k="Movie · 2024", t="Low Tide", m="4.2 GB on this device", art="frost")]


# ---------------------------------------------------------------- primitives
def ic(name, cls=""):
    c = f' class="{cls}"' if cls else ""
    return f'<i data-icon="{name}"{c}></i>'


def art(scene):
    return f'<div class="art {scene}" aria-hidden="true"><i></i><b></b><b></b></div>'


def attrs(**kw):
    out = ""
    for k, v in kw.items():
        if v is None or v is False:
            continue
        k = k.rstrip("_").replace("_", "-")
        out += f' {k}' if v is True else f' {k}="{v}"'
    return out


# ---------------------------------------------------------------- chrome
CHIPS = {
    "ok": ("", "cloud_done", "Harbour", "· Connected", "Source: Harbour server, connected. Show connection details"),
    "connecting": ("", "cloud", "Harbour", "· Connecting…", "Source: Harbour server, connecting. Show connection details"),
    "warn": ("warn", "sync", "Harbour", "· Reconnecting…", "Source: Harbour server, reconnecting. Show connection details"),
    "off": ("off", "cloud_off", "Offline", "· Downloads only", "Offline, showing downloads only. Show connection details"),
    "unreach": ("off", "cloud_off", "Harbour", "· Not responding", "Source: Harbour server, not responding. Show connection details"),
    "local": ("local", "smartphone", "This device", "", "Source: videos on this device. Show details"),
}
CHIP_TARGET = {"ok": "connected", "connecting": "connected", "warn": "reconnecting", "off": "offline", "unreach": "offline", "local": "device"}


def chip(kind="ok", state=""):
    cls, icon, name, st, aria = CHIPS[kind]
    return (f'<button class="src {cls} {state}" aria-label="{aria}" data-goto="shell/source/{CHIP_TARGET[kind]}">'
            f'{ic(icon)}<span class="truncate">{name} <span class="st">{st}</span></span></button>')


def topbar(kind="ok", busy=False):
    rb = (f'<button class="icon-btn solid{" is-loading" if busy else ""}" aria-label="Refresh Home" aria-keyshortcuts="Control+R" title="Refresh (Ctrl+R)"'
          f'{" aria-busy=\"true\" disabled" if busy else ""} data-state-to="refreshing">{ic("refresh")}</button>')
    line = '<span class="prog-line" role="progressbar" aria-label="Refreshing Home"></span>' if busy else ""
    return f'<header class="hm-top"><div class="hm-brand"><h1>EmbyEx</h1>{chip(kind)}</div>{rb}{line}</header>'


def backbar(title, count=None, back="home/home/loaded"):
    c = f'<span class="count">{count}</span>' if count else ""
    return (f'<header class="hm-top back"><button class="icon-btn" aria-label="Back to Home" data-goto="{back}">{ic("arrow_back")}</button>'
            f'<h1 class="title"><span class="truncate">{title}</span>{c}</h1></header>')


def nav(current="home"):
    tabs = [("home", "Home"), ("video_library", "Libraries"), ("search", "Search")]
    t = "".join(f'<button class="hm-tab"{" aria-current=\"page\"" if i == current else ""}>{ic(i)}<span>{l}</span></button>' for i, l in tabs)
    return (f'<nav class="hm-nav" aria-label="Main"><div class="hm-pill">{t}</div>'
            f'<button class="hm-acct" aria-label="Account and settings, Maya" aria-haspopup="menu" data-goto="shell/nav/account"><span class="avatar">MA</span></button></nav>')


def page_wrap(top, body, extra="", cls="", frozen=False):
    app_cls = "app frozen" if frozen else "app"
    return f'<div class="{app_cls}"><div class="hm {cls}">{top}<main class="hm-body">{body}</main>{nav()}{extra}</div></div>'


# ---------------------------------------------------------------- cards
def head(title, count=None, see_all=None, scroll=True, hid=None):
    cnt = f'<span class="count">{count}</span>' if count else ""
    hid_attr = f' id="{hid}"' if hid else ""
    see = (f'<a class="btn text" href="#" aria-label="See all {count or ""} in {title}" data-goto="{see_all}">See all{ic("chevron_right")}</a>' if see_all else "")
    scr = (f'<span class="scrollers"><button class="icon-btn" aria-label="Scroll {title} back" disabled>{ic("chevron_left")}</button>'
           f'<button class="icon-btn" aria-label="Scroll {title} forward">{ic("chevron_right")}</button></span>') if scroll else ""
    return f'<div class="hm-head"><h2{hid_attr}>{title}{cnt}</h2>{see}{scr}</div>'


def poster(t, sub, scene, badge=None, state="", menu="#menu-poster"):
    b = {"new": '<span class="badge-art new">New</span>',
         "watched": f'<span class="badge-art">{ic("check")}<span class="v-sr">Watched</span></span>',
         "dl": f'<span class="badge-art">{ic("download_done")}<span class="v-sr">Downloaded</span></span>',
         "pending": f'<span class="badge-art pending">{ic("schedule")}Syncs later</span>'}.get(badge, "")
    sr = {"new": ", new", "watched": ", watched", "dl": ", downloaded"}.get(badge, "")
    return (f'<li class="mc poster"><a class="mc-hit lift {state}" href="#" aria-label="{t}, {sub}{sr}" data-goto="home/home/loaded"><div class="mc-art">{art(scene)}{b}</div>'
            f'<div class="mc-text"><h3 class="mc-t clamp-2">{t}</h3><span class="mc-m truncate">{sub}</span></div></a>'
            f'<button class="icon-btn mc-more" aria-label="More options for {t}" data-open="{menu}">{ic("more_vert")}</button></li>')


def wide(c, state="", menu="#menu-poster", dl=True):
    badge = f'<span class="badge-art">{ic("download_done")}<span class="v-sr">Downloaded</span></span>' if dl else ""
    return (f'<li class="mc wide"><a class="mc-hit lift {state}" href="#" aria-label="{c["k"].replace(" · ", ", ")}, {c["t"]}, {c["m"]}"><div class="mc-art">{art(c["art"])}{badge}</div>'
            f'<div class="mc-text"><span class="mc-k truncate">{c["k"]}</span><h3 class="mc-t truncate">{c["t"]}</h3><span class="mc-m truncate">{c["m"]}</span></div></a>'
            f'<button class="icon-btn mc-more" aria-label="More options for {c["t"]}" data-open="{menu}">{ic("more_vert")}</button></li>')


def cw_wide(s, state=""):
    """A Continue watching item as a normal 16:9 shelf card (Top of Home = Off, offline)."""
    bar = f'<span class="bar" style="--p:{s["p"]}%"></span>' if s.get("p") else ""
    tag = f'<span class="badge-art left new">{ic("skip_next")}Next up</span>' if s["kind"] == "next" else ""
    return (f'<li class="mc wide"><a class="mc-hit lift {state}" href="#" aria-label="{s["aria"]}" data-goto="shell/handoff/opening"><div class="mc-art">{art(s["art"])}{tag}{bar}</div>'
            f'<div class="mc-text"><span class="mc-k truncate">{s["k"]}</span><h3 class="mc-t truncate">{s["t"]}</h3><span class="mc-m truncate">{s["m"]}</span></div></a>'
            f'<button class="icon-btn mc-more" aria-label="More options for {s["t"]}" data-open="#menu-cw">{ic("more_vert")}</button></li>')


def rail(items, label):
    return f'<ul class="hm-rail" aria-label="{label}">{"".join(items)}</ul>'


def section(title, count, items, see="seeall/grid/loaded", hid=None):
    hid = hid or "h-" + title.lower().replace(" ", "-")
    return f'<section class="hm-sec" aria-labelledby="{hid}">{head(title, count, see, hid=hid)}{rail(items, title)}</section>'


# ---------------------------------------------------------------- spotlight + filmstrip
SLIDE_STATE = {0: "loaded", 1: "slide-2", 2: "next-up"}


def play_btn(s, disabled=False, label=None):
    lab = label or ("Play" if s["kind"] in ("next", "new") else "Resume")
    goto = "" if disabled else ' data-goto="shell/handoff/opening"'
    return f'<button class="btn huge" aria-label="{lab} {s["aria"]}"{goto}{" disabled" if disabled else ""}>{ic("play_arrow", "fill")}{lab}</button>'


def strip(slides, sel, wire=True, focus=None):
    out = ""
    for i, s in enumerate(slides):
        bar = f'<span class="bar" style="--p:{s["p"]}%"></span>' if s.get("p") else ""
        tag = f'<span class="badge-art left new">{ic("skip_next")}Next</span>' if s["kind"] == "next" else ""
        to = f' data-state-to="{SLIDE_STATE[i]}"' if wire and i in SLIDE_STATE else ""
        f = " is-focus" if focus == i else ""
        out += (f'<li><button class="lift{f}" role="tab" aria-selected="{"true" if i == sel else "false"}" aria-label="{s["aria"]}"{to}>'
                f'<span class="mc-art">{art(s["art"])}{tag}{bar}</span><span class="truncate">{s["t"]}</span></button></li>')
    return f'<ul class="strip" role="tablist" aria-label="Continue watching, {TOTAL} titles">{out}</ul>'


def spotlight(slides=SLIDES, sel=0, total=TOTAL, kicker="Continue watching", kicker_icon="history", see="seeall/list/loaded",
              wire=True, offline=False, single=False):
    s = slides[sel]
    pos = f"{sel + 1} of {total}"
    prev_to = f' data-state-to="{SLIDE_STATE[sel - 1]}"' if wire and sel - 1 in SLIDE_STATE else ""
    next_to = f' data-state-to="{SLIDE_STATE[sel + 1]}"' if wire and sel + 1 in SLIDE_STATE else ""
    tag = f'<span class="fs-kicker">{ic(kicker_icon)}{kicker}{"" if single else " · " + pos}</span>'
    prog = (f'<div class="prog"><span class="bar thick" style="--p:{s["p"]}%" aria-hidden="true"></span><span>{s["m"]}</span></div>'
            if s.get("p") else f'<div class="prog"><span>{s["m"]}</span></div>')
    hero = (f'<div class="fs" role="tabpanel" aria-label="{s["t"]}"><div class="artbox">{art(s["art"])}</div><div class="in">{tag}'
            f'<h2 class="title"><a href="#" aria-label="{s["t"]}, open details">{s["t"]}</a></h2>'
            f'<p class="meta">{s["k"]}</p>{prog}'
            f'<div class="acts">{play_btn(s)}<button class="icon-btn tonal xl" aria-label="More options for {s["t"]}" data-open="#menu-cw">{ic("more_vert")}</button></div></div></div>')
    if single:
        return f'<section class="hm-sec" aria-label="{kicker}">{hero}</section>'
    ctl = (f'<span class="ca-ctl"><button class="icon-btn" aria-label="Previous title"{" disabled" if sel == 0 else ""}{prev_to}>{ic("chevron_left")}</button>'
           f'<button class="icon-btn" aria-label="Next title"{next_to}>{ic("chevron_right")}</button></span>')
    shead = (f'<div class="strip-head"><h2 id="h-cw" class="v-sr">{kicker}</h2><span class="pos" aria-live="polite">{pos}</span>{ctl}'
             f'<a class="btn text" href="#" aria-label="See all {total} in {kicker}" data-goto="{see}">See all {total}{ic("chevron_right")}</a></div>')
    return f'<section class="hm-sec" aria-labelledby="h-cw" aria-roledescription="carousel">{hero}{shead}{strip(slides, sel, wire)}</section>'


# ---------------------------------------------------------------- page bodies
EDIT = (f'<div class="hm-edit"><button class="btn outline" data-goto="edit/edit/default">{ic("tune")}Edit Home</button>'
        '<p>Choose the top of Home, card size and which sections show.</p></div>')


def shelves(fail_shows=False, pending=False):
    movies = section("New in Movies", None, [poster(*m) for m in NEW_MOVIES])
    if fail_shows:
        shows = (f'<section class="hm-sec" aria-labelledby="h-new-in-tv-shows">{head("New in TV Shows", None, None, scroll=False, hid="h-new-in-tv-shows")}'
                 f'<div class="sec-error" role="status">{ic("error")}<div class="body">Couldn’t load New in TV Shows. The rest of Home is up to date.</div>'
                 f'<button class="btn text" data-state-to="loaded">Retry</button></div></section>')
    else:
        shows = section("New in TV Shows", None, [poster(*m) for m in NEW_SHOWS])
    favs_items = [poster(*f) for f in FAVS]
    if pending:
        favs_items[1] = poster("Northbound", "Series", "sea", "pending")
    favs = section("Favorites", "9", favs_items)
    dls = section("Downloaded", "2", [wide(c) for c in DOWNLOADED])
    return movies + shows + favs + dls + EDIT


SNACK = lambda sid, msg, act, hidden=True: (
    f'<div class="snackbar-host"><div class="snackbar" id="{sid}" role="status"{" hidden" if hidden else ""}><span class="msg">{msg}</span>'
    f'<button class="btn text" data-close>{act}</button></div></div>')


def menus():
    """Hidden overlays for the prototype: card menu (sheet on phone/tablet, menu on desktop) and status."""
    return card_menu_overlay("menu-cw", SLIDES[0], cw=True) + card_menu_overlay("menu-poster", dict(t="Kestrel", k="Movie · 2023", art="frost", series=None), cw=False)


def actions(item, cw=True, offline=False, fav=False):
    why = '<span class="why">Needs a connection to Harbour</span>'

    def a(icon, text, goto=None, net=True):
        dis = offline and net
        g = f' data-goto="{goto}"' if goto and not dis else ""
        w = why if dis else ""
        return f'<li><button role="menuitem"{g}{" disabled" if dis else ""}>{ic(icon)}<span class="text"><span>{text}</span>{w}</span></button></li>'

    # Offline (matches EmbyRepositoryImpl): watched changes on downloaded titles queue and sync later; favorites,
    # hide-from-resume and browsing a series need the server, so they stay visible, disabled, with the reason.
    items = a("check_circle", "Mark as watched", "actions/menu/marked", net=False)
    if cw:
        items += a("visibility_off", "Remove from Continue watching", "actions/menu/removed")
    if item.get("series"):
        items += a("tv", f'Go to {item["series"]}', None)
    items += a("favorite", "Remove from favorites" if fav else "Add to favorites", "actions/menu/failed" if not cw else None)
    items += a("download_done" if offline else "download", "Downloaded · Manage in Downloads" if offline else "Download", None if offline else "actions/menu/downloading", net=not offline)
    return f'<ul class="act-list" role="menu" aria-label="{item["t"]}">{items}</ul>'


def card_menu_overlay(oid, item, cw=True, offline=False, static=False, hidden=True):
    head_ = (f'<div class="sheet-head"><span class="thumb">{art(item["art"])}</span><span class="text"><strong class="truncate">{item["t"]}</strong>'
             f'<span class="truncate">{item["k"]}</span></span></div>')
    acts = actions(item, cw, offline)
    flags = " data-static" if static else (" hidden" if hidden else "")
    return (f'<div class="overlay sheet-overlay" id="{oid}" data-overlay{flags}>'
            f'<div class="sheet" role="dialog" aria-label="Options for {item["t"]}" data-only="phone tablet">{head_}{acts}</div>'
            f'<div class="menu card-menu" role="presentation" data-only="desktop landscape" style="align-self:start;justify-self:start;margin:var(--menu-top, 300px) 0 0 var(--menu-left, 560px)">{acts}</div></div>')


def home_loaded(sel=0, extra="", cls="", body_pre="", fail_shows=False, kind="ok", busy=False, pending=False, frozen=False):
    top = topbar(kind, busy)
    pre = ""
    body = body_pre + spotlight(sel=sel) + shelves(fail_shows, pending)
    return page_wrap(top + pre, body, menus() + extra, cls, frozen)


# ---------------------------------------------------------------- Home page
def st(state, label, trigger, html, note=None):
    n = f' data-note="{note}"' if note else ""
    return f'<div class="v-state" data-state="{state}" data-label="{label}" data-trigger="{trigger}"{n}>{html}</div>'


def skeleton():
    row = "".join('<span class="sk" style="width:144px;height:216px;border-radius:var(--radius-md)"></span>' for _ in range(4))
    strip_ = "".join('<span class="sk" style="width:112px;height:63px"></span>' for _ in range(4))
    return (f'<div class="app" aria-busy="true"><div class="hm">{topbar("connecting")}<main class="hm-body">'
            f'<div><span class="sk sk-hero"></span><div class="sk-lines"><span class="sk" style="width:180px;height:28px;border-radius:14px"></span>'
            f'<span class="sk" style="width:80%;height:40px"></span><span class="sk" style="width:50%;height:20px"></span>'
            f'<span class="sk" style="width:100%;height:64px;border-radius:32px"></span></div></div>'
            f'<div class="sk-row">{strip_}</div><div class="sk-lines" style="margin:0"><span class="sk" style="width:160px;height:24px"></span></div>'
            f'<div class="sk-row">{row}</div><p class="v-sr" role="status">Loading Home</p></main>{nav()}</div></div>')


def emblem(icon, err=False):
    return f'<span class="emblem{" err" if err else ""}" aria-hidden="true">{ic(icon)}</span>'


def full_state(kind, icon, title, text, acts, err=False, detail=None, extra_body=""):
    d = f'<p class="detail">{detail}</p>' if detail else ""
    cls = "screen-error" if err else "empty"
    return page_wrap(topbar(kind), f'{extra_body}<div class="{cls}" role="{"alert" if err else "status"}">{emblem(icon, err)}<h2>{title}</h2><p>{text}</p>{d}<div class="acts">{acts}</div></div>')


def offline_body():
    callout = (f'<div class="callout info" role="status">{ic("cloud_off")}<div class="body"><strong>You’re offline</strong>'
               'Showing the 2 titles on this device. Home updates when Harbour is back.</div><button class="btn text" data-goto="home/home/loaded">Try again</button></div>')
    cw = (f'<section class="hm-sec" aria-labelledby="h-cwo">{head("Continue watching", "1", None, scroll=False, hid="h-cwo")}'
          + rail([cw_wide(dict(SLIDES[0], k="Northbound · S2 · E6", t="Low Water", m="31 min left", p=30, kind="resume", art="sea", aria="Northbound, season 2 episode 6, Low Water, downloaded"))], "Continue watching") + '</section>')
    dls = section("Downloaded", "2", [wide(c) for c in DOWNLOADED], see="seeall/grid/loaded")
    return callout + cw + dls + f'<div class="hm-edit"><a class="btn outline" href="#">{ic("download")}Open Downloads</a><p>Sections that need Harbour come back when you reconnect.</p></div>'


def device_body(empty=False):
    callout = (f'<div class="callout info" role="status">{ic("smartphone")}<div class="body"><strong>Showing videos on this device</strong>'
               'Connect to a server to see your library, progress and favorites.</div><button class="btn text">Connect to a server</button></div>')
    if empty:
        return (callout.replace('<button class="btn text">Connect to a server</button>', "") +
                f'<div class="empty" role="status">{emblem("video_library")}<h2>No videos on this device yet</h2><p>Downloads and videos you add to this device show up here.</p>'
                f'<div class="acts"><button class="btn">{ic("dns")}Connect to a server</button></div></div>')
    local = [dict(k="Video · 2026", t="Beach day", m="12 min · 820 MB", art="sea"), dict(k="Video · 2026", t="Grandma’s 80th", m="41 min · 2.6 GB", art="ember"),
             dict(k="Movie · 2024", t="Low Tide", m="4.2 GB", art="frost")]
    return callout + section("On this device", "3", [wide(c) for c in local], see="seeall/grid/loaded")


def large_body():
    return spotlight() + shelves()


def stress_body():
    one = [dict(SLIDES[0], t="The Extraordinarily Long and Winding Way Round to the Northern Lighthouse", m="23 min left",
                aria="Northbound, season 2 episode 5, The Extraordinarily Long and Winding Way Round to the Northern Lighthouse, 23 minutes left")]
    hero = spotlight(slides=one, total=1, single=True, kicker="Continue watching")
    long_items = [poster("A Map of Small Hours: The Director’s Extended Cut", "2024", "none"), poster("Kestrel", "2023", "frost", "watched"),
                  poster("Halfway to Morning", "2024", "none"), poster("Ember Season", "2022", "ember")]
    movies = section("Favorites", "240", long_items)
    shows = section("New in TV Shows", None, [poster("The Night Desk", "12 new episodes", "city"), poster("Harbour Lights", "New series", "none")])
    return hero + movies + shows + EDIT


def top_off_body():
    cw = section("Continue watching", "8", [cw_wide(s) for s in SLIDES], see="seeall/list/loaded")
    return cw + shelves()


def home_page():
    s = []
    s.append(st("loaded", "Loaded", "Home opened, Harbour connected", home_loaded(),
                "Primary action: Resume. Tap a filmstrip thumbnail or Next to change the spotlight; ⋮ opens the card menu; See all opens the grid."))
    s.append(st("slide-2", "Slide 2 · movie", "Next title or second thumbnail", home_loaded(sel=1),
                "Never moves by itself. '2 of 8' is announced politely; focus stays on the control the user pressed."))
    s.append(st("next-up", "Slide 3 · Next up", "Third thumbnail (a Next up episode)", home_loaded(sel=2),
                "Next up = the following unwatched episode of a show in progress: Play, no progress bar, tag with icon + text."))
    s.append(st("loading", "Loading", "First open, nothing cached", skeleton(),
                "Skeleton mirrors spotlight, filmstrip and the first shelf; no indicator for loads under 300 ms. Navigation stays usable."))
    s.append(st("refreshing", "Refreshing", "Refresh button, Ctrl+R or pull to refresh", home_loaded(busy=True),
                "Content stays; thin progress under the top bar (pull-to-refresh indicator on phone). Refresh button busy and disabled."))
    just = spotlight(slides=JUST_ADDED, total=12, kicker="Just added", kicker_icon="new_releases", see="seeall/grid/loaded", wire=False)
    s.append(st("nothing-in-progress", "Nothing in progress", "Nothing started or everything finished", page_wrap(topbar(), just + shelves()),
                "The spotlight falls back to the newest titles with Play, instead of leaving an empty top."))
    s.append(st("top-off", "Top of Home off", "Edit Home › Top of Home: Off", page_wrap(topbar(), top_off_body()),
                "Continue watching becomes the first normal shelf (16:9 cards with progress)."))
    s.append(st("large", "Large cards", "Edit Home › Card size: Large", page_wrap(topbar(), large_body(), cls="large"),
                "Bigger artwork and type, fewer cards per row. Good for low vision and couch distance; independent of the system text size."))
    s.append(st("partial", "Section failed", "One section's request failed, the rest loaded", home_loaded(fail_shows=True),
                "Partial failure stays inside its section. Retry reloads only that section."))
    s.append(st("refresh-failed", "Refresh failed", "Background refresh failed, content still on screen",
                home_loaded(extra=SNACK("sn-rf", "Couldn’t refresh Home. Showing what was loaded at 20:41.", "Retry", hidden=False)),
                "T3 snackbar: transient, content is still valid. Retry once; repeated failures turn into Reconnecting."))
    s.append(st("reconnecting", "Reconnecting", "Server stopped answering, retrying", home_loaded(kind="warn"),
                "Only the chip changes (icon + words). After about 10 s without an answer Home switches to Offline."))
    s.append(st("offline", "Offline · downloads", "No connection, titles downloaded", page_wrap(topbar("off"), offline_body()),
                "T4 callout below the top bar while the condition lasts; only titles on this device are shown. Mark as watched queues and syncs later; favorites, hiding and browsing a series are disabled with the reason."))
    s.append(st("offline-empty", "Offline · nothing downloaded", "No connection, nothing on the device",
                full_state("off", "cloud_off", "You’re offline", "Nothing is downloaded yet, so there’s nothing to show. Connect to Harbour to browse your library.",
                           f'<button class="btn" data-goto="home/home/loaded">{ic("refresh")}Try again</button>', err=True),
                "T5: Home's content can't be shown and nothing is cached. Top bar and navigation stay."))
    s.append(st("unreachable", "Server not responding", "Online, but Harbour doesn't answer and nothing is cached",
                full_state("unreach", "dns", "Couldn’t reach Harbour", "The server didn’t respond. Check that it’s running and on the same network, then try again.",
                           f'<button class="btn" data-goto="home/home/loaded">{ic("refresh")}Try again</button><button class="btn outline">Server settings</button>',
                           err=True, detail="harbour.local:8096 · last connected today at 18:02"),
                "T5 with the address in a small secondary line for troubleshooting."))
    s.append(st("device", "Browsing this device", "Local browsing mode (seen on the phone, 2026-11-11)", page_wrap(topbar("local"), device_body()),
                "Fixes today's 'No media available': the chip and a T4 callout say where Home's content comes from and how to see the library."))
    s.append(st("device-empty", "This device · empty", "Local browsing mode, no videos on the device", page_wrap(topbar("local"), device_body(empty=True)),
                "Empty (first use): explains what appears here and offers the one next step."))
    s.append(st("server-empty", "Server library empty", "Harbour connected but has no media",
                full_state("ok", "video_library", "Harbour has no movies or shows yet", "Add media folders in the Emby dashboard on your server, then refresh Home.",
                           f'<button class="btn" data-state-to="loading">{ic("refresh")}Refresh</button>'),
                "Empty, not an error: the connection works."))
    s.append(st("all-hidden", "Every section hidden", "Edit Home: all sections switched off",
                full_state("ok", "tune", "Your Home is empty", "Every section is switched off. Turn some back on to see your library here.",
                           f'<button class="btn" data-goto="edit/edit/default">{ic("tune")}Edit Home</button>'), None))
    s.append(st("stress", "Long content", "One title in progress, long titles, missing artwork, big counts", page_wrap(topbar(), stress_body()),
                "One title: no position, arrows or filmstrip. Spotlight titles wrap fully; cards clamp to 2 lines (full title in the label and on Details). Missing art = tinted fallback. Counts appear only where the server has a real total (not on New in rows) and show in full."))
    s.append(st("text-200", "Text 200 %", "System font size 200 %", page_wrap(topbar(), spotlight() + shelves(), cls="t200"),
                "Everything reflows; spotlight titles wrap, cards clamp to 2 lines. Bottom-bar labels stay visible and scale to 1.5× at most so all three tabs fit."))
    return f'''<section class="v-page" data-page="home" data-title="Home" data-group="Features">
<h1>Home</h1>
<p class="v-lead">The first screen after sign-in. Primary question: “What do I watch now?” The spotlight answers it with one Resume button; the filmstrip shows everything else in progress; shelves below show what's new.</p>
<article class="v-screen" data-screen="home" data-title="Home" data-purpose="Primary action: Resume the spotlight title.">
{"".join(s)}
</article>
</section>'''


# ---------------------------------------------------------------- Card actions page
def actions_page():
    s = []
    s.append(st("menu-continue", "Menu · Continue watching", "⋮, long-press or right-click on a Continue watching title",
                home_loaded(extra=card_menu_overlay("m1", SLIDES[0], cw=True, static=True), frozen=True),
                "Bottom sheet on phone/tablet (title in the header so it's clear what the actions apply to), menu on desktop. Esc or tapping outside closes."))
    s.append(st("menu-poster", "Menu · new title", "⋮ on a poster in New in Movies",
                home_loaded(extra=card_menu_overlay("m2", dict(t="Kestrel", k="Movie · 2023", art="frost", series=None), cw=False, static=True), frozen=True),
                "Only actions that apply: no 'Remove from Continue watching' for titles not in progress."))
    s.append(st("menu-offline", "Menu · offline", "⋮ while offline",
                page_wrap(topbar("off"), offline_body(), card_menu_overlay("m3", dict(t="Low Water", k="Northbound · S2 · E6", art="sea", series="Northbound"), cw=True, offline=True, static=True), frozen=True),
                "Mark as watched still works on downloaded titles and syncs when Harbour is back. Favorites, Remove from Continue watching and Go to series need the server, so they stay visible, disabled, each with the reason."))
    marked = SLIDES[1:]
    s.append(st("marked", "Marked as watched", "Mark as watched",
                page_wrap(topbar(), spotlight(slides=marked, total=7, wire=False) + shelves(), SNACK("sn-m", "Marked “The Long Way Round” as watched", "Undo", hidden=False)),
                "Reversible: done at once, T3 with Undo (restores the watch position). The title leaves the filmstrip; position becomes '1 of 7'."))
    s.append(st("removed", "Removed from Continue watching", "Remove from Continue watching",
                page_wrap(topbar(), spotlight(slides=marked, total=7, wire=False) + shelves(), SNACK("sn-r", "Removed from Continue watching. Progress is kept.", "Undo", hidden=False)),
                "Reversible, T3 with Undo. Watch progress stays, so it can come back."))
    s.append(st("failed", "Action failed", "Server rejected or didn't answer a favorite change",
                home_loaded(extra=SNACK("sn-f", "Couldn’t add “Kestrel” to favorites. Check your connection.", "Retry", hidden=False)),
                "T3, the change is reverted on screen so it never shows a state the server doesn't have."))
    s.append(st("downloading", "Download started", "Download on a title with one version",
                home_loaded(extra=SNACK("sn-d", "Downloading “Kestrel” · 4.1 GB", "View", hidden=False)),
                "One version: starts at once, T3 with View (opens Downloads). Several versions: the source picker from the Details design opens first."))
    return f'''<section class="v-page" data-page="actions" data-title="Card actions" data-group="Features">
<h1>Card actions</h1>
<p class="v-lead">Every card has a ⋮ button (48 px), plus long-press on touch and right-click / Shift+F10 on desktop. The menu lists only actions that apply to that title.</p>
<article class="v-screen" data-screen="menu" data-title="Card menu" data-purpose="Act on a title without opening it.">
{"".join(s)}
</article>
</section>'''


# ---------------------------------------------------------------- Edit Home page
SECTIONS = [("Continue watching", "Top of Home", True), ("New in Movies", "", True), ("New in TV Shows", "", True),
            ("Favorites", "", True), ("Downloaded", "", True), ("New in Kids", "", False), ("New in Concerts", "", False)]


def edit_content(status="saved", top="carousel", size="comfortable", callout=""):
    status_html = {"saved": f'<p class="ed-status" role="status">{ic("cloud_done")}Saved to your account · used on all your devices</p>',
                   "saving": f'<p class="ed-status" role="status">{ic("sync")}Saving…</p>',
                   "failed": f'<p class="ed-status" role="status">{ic("smartphone")}Saved on this device only</p>',
                   "offline": f'<p class="ed-status" role="status">{ic("smartphone")}Saved on this device · syncs later</p>'}[status]
    seg = lambda opts, cur, lab: (f'<div class="segmented" role="radiogroup" aria-label="{lab}">' + "".join(
        f'<button role="radio" aria-checked="{"true" if v == cur else "false"}" aria-pressed="{"true" if v == cur else "false"}">{ic(i) if v == cur else ""}{t}</button>' for v, t, i in opts) + '</div>')
    rows = ""
    shown = [r for r in SECTIONS if r[2]]
    for idx, (name, note, on) in enumerate(SECTIONS):
        pinned = idx == 0 and top == "carousel"
        pos = f"Shown · {['1st', '2nd', '3rd', '4th', '5th'][idx]}" if on else "Hidden"
        sub = "Pinned to the top · change it under Top of Home" if pinned else pos
        first, last = idx <= 1 or pinned, idx == len(shown) - 1 or not on or pinned
        rows += (f'<div class="edit-row"><span class="text"><span>{name}</span><small>{sub}</small></span>'
                 f'<button class="icon-btn" aria-label="Move {name} up"{" disabled" if first or not on else ""}>{ic("arrow_upward")}</button>'
                 f'<button class="icon-btn" aria-label="Move {name} down"{" disabled" if last else ""}>{ic("arrow_downward")}</button>'
                 f'<label class="switch"><input type="checkbox" role="switch"{" checked" if on else ""} aria-label="Show {name}"></label></div>')
    libs = "".join(f'<label class="check-row"><input type="checkbox"{" checked" if on else ""}>{n}</label>'
                   for n, on in [("Movies", True), ("TV Shows", True), ("Kids", False), ("Concerts", False)])
    return f'''<div class="ed">{callout}{status_html}
<div class="ed-group"><h3 id="g-top">Top of Home</h3>{seg([("carousel", "Continue watching", "check"), ("off", "Off", "check")], top, "Top of Home")}</div>
<div class="ed-group"><h3>Card size</h3>{seg([("comfortable", "Comfortable", "check"), ("large", "Large", "check")], size, "Card size")}</div>
<div class="ed-group"><h3>Sections</h3><div>{rows}</div></div>
<div class="ed-group"><h3>“New in” rows for these libraries</h3>{libs}</div>
<div class="ed-foot"><button class="btn text" data-state-to="reset">{ic("restart_alt")}Reset to default</button><button class="btn" data-goto="home/home/loaded">Done</button></div></div>'''


def edit_overlay(content):
    return (f'<div class="overlay sheet-overlay" data-overlay data-static data-only="phone"><div class="sheet" role="dialog" aria-labelledby="ed-t" style="max-height:94%">'
            f'<h2 id="ed-t">Edit Home</h2>{content}</div></div>'
            f'<div class="overlay" data-overlay data-static data-only="tablet desktop landscape"><div class="dialog wide" role="dialog" aria-labelledby="ed-t2">'
            f'<h2 id="ed-t2">Edit Home</h2>{content}</div></div>')


def edit_page():
    fail = (f'<div class="callout warning" role="status">{ic("sync_problem")}<div class="body"><strong>Couldn’t save to Harbour</strong>'
            'Your layout is kept on this device and syncs when it can.</div><button class="btn text" data-state-to="saving">Retry</button></div>')
    off = (f'<div class="callout info" role="status">{ic("cloud_off")}<div class="body"><strong>You’re offline</strong>'
           'Changes apply here now and sync to your account when you’re back online.</div></div>')
    bg = lambda kind="ok", extra="": page_wrap(topbar(kind), spotlight() + shelves(), extra, frozen=True)
    s = [st("default", "Edit Home", "Edit Home at the end of Home, or Settings › Home", bg(extra=edit_overlay(edit_content())),
            "Every change applies and saves at once (no Save button to forget). Move up / Move down instead of drag-only reordering; the first item can't move up, the last can't move down."),
         st("saving", "Saving", "A change was just made", bg(extra=edit_overlay(edit_content("saving", size="large"))),
            "Status line only; no spinner over the controls, they stay usable."),
         st("save-failed", "Save failed", "Server rejected or didn't answer the save", bg(extra=edit_overlay(edit_content("failed", size="large", callout=fail))),
            "T4 inside the sheet: the layout still applies on this device and keeps retrying. Nothing is lost."),
         st("offline", "Offline", "Edit Home opened while offline", bg("off", edit_overlay(edit_content("offline", callout=off))),
            "T4: changes apply locally and sync later."),
         st("reset", "Reset to default", "Reset to default", page_wrap(topbar(), spotlight() + shelves(), SNACK("sn-reset", "Home reset to the default layout", "Undo", hidden=False)),
            "Reversible, so no confirmation dialog: T3 with Undo restores the previous layout.")]
    return f'''<section class="v-page" data-page="edit" data-title="Edit Home" data-group="Features">
<h1>Edit Home</h1>
<p class="v-lead">Each person's Home, saved to their account on the server so it follows them to every device. Sheet on phone, dialog on tablet and desktop.</p>
<article class="v-screen" data-screen="edit" data-title="Edit Home" data-purpose="Choose the top of Home, card size, sections and their order.">
{"".join(s)}
</article>
</section>'''


# ---------------------------------------------------------------- See all page
def grid_items(n_extra=0):
    base = NEW_MOVIES + [("Salt and Signal", "2023", "dawn", None), ("Paper Lanterns", "2021", "ember", None), ("Second Wind", "2024", "hills", "new"),
                         ("North of Nowhere", "2022", "frost", None), ("Deep Water", "2020", "night", None)]
    return "".join(poster(*m) for m in base)


def lrow(s):
    bar = f'<span class="bar" style="--p:{s["p"]}%" aria-hidden="true"></span>' if s.get("p") else ""
    tag = f'<span class="badge-art left new">{ic("skip_next")}Next up</span>' if s["kind"] == "next" else ""
    return (f'<li class="lrow"><a class="hit lift" href="#" aria-label="{s["aria"]}" data-goto="shell/handoff/opening"><span class="mc-art">{art(s["art"])}{tag}</span>'
            f'<span class="txt"><span class="mc-k truncate">{s["k"]}</span><span class="mc-t clamp-2">{s["t"]}</span><span class="mc-m">{s["m"]}</span>{bar}</span></a>'
            f'<button class="icon-btn" aria-label="More options for {s["t"]}" data-open="#menu-cw">{ic("more_vert")}</button></li>')


def seeall_page():
    grid = lambda extra="": f'<ul class="grid" aria-label="New in Movies">{grid_items()}</ul>{extra}'
    more = '<div class="end-note"><span class="spinner" style="display:inline-block;width:24px;height:24px" aria-hidden="true"></span><p role="status">Loading more titles…</p></div>'
    end = '<p class="end-note">That’s everything added recently.</p>'
    wrap = lambda top, body, extra="": f'<div class="app"><div class="hm">{top}<main class="hm-body">{body}</main>{nav()}{menus()}{extra}</div></div>'
    g = [st("loaded", "Loaded", "See all on New in Movies", wrap(backbar("New in Movies"), grid()), "Vertical grid: the keyboard and screen-reader alternative to a sideways shelf. Arrow keys move in two dimensions."),
         st("loading-more", "Loading more", "Scrolled to the end of the loaded page", wrap(backbar("New in Movies"), grid(more)), "Loads in pages of 50; the spinner sits below the last row, nothing jumps."),
         st("end", "End of list", "Everything loaded", wrap(backbar("New in Movies"), grid(end)), None),
         st("empty", "Empty", "Favorites with no titles",
            wrap(backbar("Favorites", "0"), f'<div class="empty" role="status">{emblem("favorite")}<h2>No favorites yet</h2><p>Use ⋮ › Add to favorites on any title, or the heart on its Details page.</p>'
                                            f'<div class="acts"><button class="btn outline" data-goto="home/home/loaded">Back to Home</button></div></div>'),
            "Empty (first use) teaches how to add one."),
         st("error", "Failed", "Section can't load, nothing cached",
            wrap(backbar("New in Movies"), f'<div class="screen-error" role="alert">{emblem("cloud_off", True)}<h2>Couldn’t load New in Movies</h2><p>Harbour didn’t respond. Try again in a moment.</p>'
                                           f'<div class="acts"><button class="btn" data-state-to="loaded">{ic("refresh")}Try again</button></div></div>'),
            "T5 with Back kept in the top bar.")]
    lst = [st("loaded", "Loaded", "See all 8 under the filmstrip", wrap(backbar("Continue watching", "8"), f'<ul class="list-rows" aria-label="Continue watching, 8 titles">{"".join(lrow(x) for x in SLIDES)}'
                                                                                   + "".join(lrow(dict(x, t=x["t"] + "", k=x["k"])) for x in SLIDES[:3]).replace("The Long Way Round", "Into the Squall").replace("Salt and Signal", "Kestrel").replace("Ash on the Wind", "First Frost") + "</ul>"),
              "List with full titles (2 lines), progress and Next up; ⋮ on every row.")]
    return f'''<section class="v-page" data-page="seeall" data-title="See all" data-group="Features">
<h1>See all</h1>
<p class="v-lead">Every section's full list. A grid for posters, a list for Continue watching. Back returns to the same place on Home.</p>
<article class="v-screen" data-screen="grid" data-title="Section grid" data-purpose="Browse a whole section without sideways scrolling.">{"".join(g)}</article>
<article class="v-screen" data-screen="list" data-title="Continue watching list" data-purpose="See everything in progress at once.">{"".join(lst)}</article>
</section>'''


# ---------------------------------------------------------------- Shell page
def status_content(kind):
    data = {
        "connected": ("ok", "cloud_done", "Connected to Harbour", [("Server", "Harbour · Emby 4.11"), ("Address", "harbour.local:8096"), ("Signed in as", "Maya"), ("Home updated", "Today at 20:41")],
                      "", f'<button class="btn" data-goto="home/home/refreshing">{ic("refresh")}Refresh Home</button>'),
        "reconnecting": ("warn", "sync", "Reconnecting to Harbour", [("Address", "harbour.local:8096"), ("Last answer", "20:41, 40 s ago")],
                         "Home keeps showing what was loaded. If Harbour doesn’t answer soon, Home switches to your downloads.", f'<button class="btn outline">Server settings</button>'),
        "offline": ("off", "cloud_off", "You’re offline", [("Showing", "2 titles on this device"), ("Last connected", "Today at 20:41")],
                    "Home shows what’s downloaded. Progress you make is saved and syncs when you’re back online.",
                    f'<button class="btn" data-goto="home/home/loaded">{ic("refresh")}Try again</button><button class="btn outline">Open Downloads</button>'),
        "device": ("ok", "smartphone", "Videos on this device", [("Showing", "3 videos in Movies and Downloads")],
                   "You’re not connected to a server. Connect to one to see your library, progress and favorites.",
                   f'<button class="btn">{ic("dns")}Connect to a server</button>'),
    }[kind]
    em, icon, title, rows, p, acts = data
    dl = "".join(f'<dt>{a}</dt><dd>{b}</dd>' for a, b in rows)
    para = f'<p>{p}</p>' if p else ""
    return f'<div class="stat"><div class="hd"><span class="emblem {em}" aria-hidden="true">{ic(icon)}</span><h2>{title}</h2></div><dl>{dl}</dl>{para}<div class="acts">{acts}</div></div>'


def status_overlay(kind):
    c = status_content(kind)
    return (f'<div class="overlay sheet-overlay" data-overlay data-static data-only="phone"><div class="sheet" role="dialog" aria-label="Connection">{c}</div></div>'
            f'<div class="overlay" data-overlay data-static data-only="tablet desktop landscape" style="place-items:start start;padding:var(--pop-top, 96px) 0 0 24px"><div class="dialog" role="dialog" aria-label="Connection">{c}</div></div>')


def account_overlay():
    menu = (f'<div class="menu card-menu" role="menu" aria-label="Account"><div class="menu-label"><span>Maya · Harbour</span></div>'
            f'<button class="menu-item" role="menuitem">{ic("download")}<span class="text"><span>Downloads</span></span></button>'
            f'<button class="menu-item" role="menuitem" data-goto="edit/edit/default">{ic("tune")}<span class="text"><span>Edit Home</span></span></button>'
            f'<button class="menu-item" role="menuitem">{ic("settings")}<span class="text"><span>Settings</span></span></button></div>')
    return f'<div class="overlay" data-overlay data-static style="place-items:end center;padding:0 0 104px 0;background:none">{menu}</div>'


def shell_page():
    nav_states = [st("bar", "Bottom bar", "Always on Home, Libraries and Search", page_wrap(topbar(), spotlight() + shelves()),
                     "Every tab has an icon and a label (icon above the label on phones, beside it on wider windows). Current tab: indicator + filled icon + bold label. Tabs 56 px tall on phone; account button 64 px."),
                  st("account", "Account menu", "Account button", page_wrap(topbar(), spotlight() + shelves(), account_overlay(), frozen=True),
                     "Label: 'Account and settings, Maya'. Adds Edit Home next to Downloads and Settings so it's findable without scrolling to the end of Home.")]
    src_states = [st(k, lab, trig, page_wrap(topbar(chipk), spotlight() + shelves(), status_overlay(k), frozen=True), note) for k, lab, trig, chipk, note in [
        ("connected", "Connected", "Tap the chip while connected", "ok", "Popover on desktop, sheet on phone. Explains where Home comes from, with the one useful action."),
        ("reconnecting", "Reconnecting", "Tap the chip while reconnecting", "warn", None),
        ("offline", "Offline", "Tap the chip while offline", "off", None),
        ("device", "This device", "Tap the chip in local browsing mode", "local", None)]]
    handoff = [st("opening", "Opening the player", "Resume or Play", f'<div class="app"><div class="hm" style="background:var(--color-scrim);color:var(--color-on-scrim)">'
                  f'<div class="empty" role="status" style="color:var(--color-on-scrim)"><span class="spinner" aria-hidden="true"></span><h2 style="color:var(--color-on-scrim)">Getting ready to play…</h2>'
                  f'<p style="color:var(--color-on-scrim-variant)">The Long Way Round · from 28:51</p><div class="acts"><button class="btn text" data-goto="home/home/loaded" style="--btn-fg:var(--color-on-scrim)">Cancel</button></div></div></div></div>',
                  "Hands off to the player from the Player design. Back from the player returns to Home with the filmstrip on the same title.")]
    keys = [("Tab / Shift+Tab", "Move between groups: chip, Refresh, spotlight title, Resume, ⋮, filmstrip, See all, each shelf, Edit Home, bottom bar"),
            ("← →", "Inside the filmstrip or a shelf: previous / next title (the filmstrip changes the spotlight)"),
            ("Home / End", "First / last title in a filmstrip or shelf"),
            ("Enter", "Open the focused title (on the filmstrip: play it)"),
            ("Shift+F10 or Menu key", "Open the card menu for the focused title"),
            ("Ctrl+R", "Refresh Home"), ("Esc", "Close a menu, sheet or dialog; focus returns to the control that opened it")]
    ktable = "".join(f'<div><span class="kbd">{k}</span><span>{v}</span></div>' for k, v in keys)
    order = "".join(f'<li>{x}</li>' for x in ["Source chip", "Refresh", "Spotlight title (opens Details)", "Resume", "⋮ More options", "Filmstrip (one tab stop, ← → inside)",
                                             "Previous / Next (desktop)", "See all 8", "New in Movies: See all, then the shelf (one tab stop, ← → inside), then ⋮ of the focused card",
                                             "… each following shelf the same way", "Edit Home", "Bottom bar: Home, Libraries, Search, Account"])
    return f'''<section class="v-page" data-page="shell" data-title="Shell &amp; global states" data-group="App shell">
<h1>Shell and global states</h1>
<p class="v-lead">Navigation, the source chip and its status, the player handoff, and keyboard operation. The bottom bar keeps today's pill; the fixes are icons and labels on every tab, a labelled account button and 48 px+ targets.</p>
<article class="v-screen" data-screen="nav" data-title="Navigation" data-purpose="Switch between Home, Libraries and Search; reach account options.">{"".join(nav_states)}</article>
<article class="v-screen" data-screen="source" data-title="Source status" data-purpose="Answer 'where is Home's content coming from, and is it current?'">{"".join(src_states)}</article>
<article class="v-screen" data-screen="handoff" data-title="Player handoff" data-purpose="Show that Resume worked while the player starts.">{"".join(handoff)}</article>
<div class="v-section"><h2>Keyboard and focus</h2>
<p>Focus is always visible (3 px ring in focus-ring, plus the card lift) and never hidden behind the sticky top bar or bottom bar: scrolling keeps 72 px clear above and 96 px below the focused element.</p>
<div class="v-card"><h3>Focus order on Home</h3><ol>{order}</ol><h3>Keys</h3><div class="keys">{ktable}</div></div>
<p class="v-note">Reduced motion: the spotlight switches without sliding, the lift keeps only the shadow and ring, and the reconnecting icon stops spinning.</p></div>
</section>'''


# ---------------------------------------------------------------- Design system page
def system_page():
    s0 = SLIDES[0]
    btns = (f'<figure><div class="line">{play_btn(s0)}</div><figcaption>Resume · primary, 64 px (one per screen)</figcaption></figure>'
            f'<figure><div class="line"><button class="btn huge is-hover">{ic("play_arrow", "fill")}Resume</button></div><figcaption>hover</figcaption></figure>'
            f'<figure><div class="line"><button class="btn huge is-focus">{ic("play_arrow", "fill")}Resume</button></div><figcaption>focus</figcaption></figure>'
            f'<figure><div class="line"><button class="btn huge is-pressed">{ic("play_arrow", "fill")}Resume</button></div><figcaption>pressed</figcaption></figure>'
            f'<figure><div class="line"><button class="btn huge is-loading" aria-busy="true">{ic("play_arrow", "fill")}Resume</button></div><figcaption>loading (keeps its width)</figcaption></figure>'
            f'<figure><div class="line"><button class="btn huge" disabled>{ic("play_arrow", "fill")}Resume</button></div><figcaption>disabled</figcaption></figure>'
            f'<figure><div class="line"><button class="btn outline">{ic("tune")}Edit Home</button><a class="btn text" href="#">See all{ic("chevron_right")}</a></div><figcaption>outline · text</figcaption></figure>'
            f'<figure><div class="line"><button class="icon-btn tonal xl" aria-label="More options">{ic("more_vert")}</button><button class="icon-btn" aria-label="More options">{ic("more_vert")}</button>'
            f'<button class="icon-btn is-focus" aria-label="More options">{ic("more_vert")}</button></div><figcaption>⋮ · tonal 64 px (spotlight), plain 48 px (cards), focus</figcaption></figure>')
    chips = "".join(chip(k) for k in ("ok", "connecting", "warn", "off", "unreach", "local")) + chip("ok", "is-focus")
    cards = rail([cw_wide(s0), cw_wide(s0, "is-hover"), cw_wide(s0, "is-focus"), cw_wide(SLIDES[2])], "Card states")
    posters = rail([poster("Low Tide", "2024", "sea", "new"), poster("Kestrel", "2023", "frost", "watched", "is-hover"), poster("Ember Season", "2022", "ember", "dl", "is-pressed"),
                    poster("Northbound", "Series", "sea", "pending"), poster("A Map of Small Hours: The Director’s Cut", "2024", "none", None, "is-focus")], "Poster states")
    return f'''<section class="v-page" data-page="ds" data-title="Design system" data-group="Foundations">
<h1>Design system</h1>
<p class="v-lead">Marquee, extended for Home: artwork leads, the system font keeps EmbyEx part of the OS, and the mint/amber palette with asymmetric shapes gives it its own identity. Three rules: one primary action per screen, nothing that only works with a gesture, and status always in words and icons, never colour alone.</p>
<div class="v-section"><h2>Color primitives</h2><div data-render="primitives"></div></div>
<div class="v-section"><h2>Semantic roles</h2><p>New for Home: on-scrim roles for text and controls on artwork, and three artwork stand-in hues (real artwork is images).</p><div data-render="roles"></div></div>
<div class="v-section"><h2>Contrast (WCAG 2.2 AA)</h2><div data-render="contrast"></div></div>
<div class="v-section"><h2>Typography</h2><p>System font only (system-ui / Roboto / Segoe UI / Cantarell). Identity comes from colour, shape and motion.</p><div class="v-card" data-render="type" data-sample="The Long Way Round · 23 min left"></div></div>
<div class="v-section"><h2>Spacing</h2><div class="v-card" data-render="space"></div></div>
<div class="v-section"><h2>Shape</h2><p>Cards md (20), spotlight controls full, sheets xl. Emblems keep the asymmetric EmbyEx corner (36/24/36/12).</p><div data-render="radius"></div></div>
<div class="v-section"><h2>Elevation</h2><div data-render="elevation"></div></div>
<div class="v-section"><h2>Icons</h2><div data-render="icons" data-names="home,video_library,search,refresh,cloud_done,cloud,sync,cloud_off,smartphone,dns,play_arrow,more_vert,chevron_left,chevron_right,history,new_releases,skip_next,download,download_done,check,check_circle,visibility_off,favorite,tv,tune,arrow_upward,arrow_downward,restart_alt,schedule,error,sync_problem,arrow_back,settings"></div></div>

<div class="v-section" id="c-buttons"><h2>Buttons</h2><p>Resume is the only filled button on Home. Labels are verbs. Targets ≥ 48 px.</p><div class="app sp">{btns}</div></div>

<div class="v-section" id="c-chip"><h2>Source chip</h2><p>Tells people where Home's content comes from. Status = icon + words, never colour alone. 32 px visual, 48 px hit area. Opens the status sheet.</p>
<div class="app sp"><figure class="wide"><div class="line">{chips}</div><figcaption>connected · connecting · reconnecting (icon spins, stops with reduced motion) · offline · not responding · this device · focus</figcaption></figure></div></div>

<div class="v-section" id="c-lift"><h2>Cards and the shared lift</h2><p>One animation for every card, so no card needs its own shape change: hover and keyboard focus scale the art 1.04 with elevation 2 and zoom the artwork 1.06 inside its frame; pressed 0.97; focus adds a 3 px ring. Spring: motionScheme fast spatial. Reduced motion: shadow and ring only.</p>
<div class="app sp"><figure class="wide"><div class="hm">{head("Continue watching", "8", "#")}{cards}</div><figcaption>16:9 card · rest, hover, focus, Next up. Whole card is one target; ⋮ is a separate 48 px button. Label: “Northbound, season 2 episode 5, The Long Way Round, 23 minutes left”.</figcaption></figure>
<figure class="wide"><div class="hm">{posters}</div><figcaption>Poster · New, watched + hover, downloaded + pressed, change waiting to sync, missing artwork + long title + focus (2 lines, then ellipsis; full title in the label).</figcaption></figure>
<figure class="wide"><div class="hm"><ul class="list-rows" style="padding:0">{lrow(SLIDES[0])}{lrow(SLIDES[2])}</ul></div><figcaption>List row (See all › Continue watching) · full title on 2 lines, progress, ⋮.</figcaption></figure></div></div>

<div class="v-section" id="c-spot"><h2>Spotlight and filmstrip</h2><p>The top of Home. The filmstrip is a tab list controlling the spotlight; it never moves by itself. Position (“1 of 8”) is announced politely. Desktop adds Previous / Next.</p>
<div class="app sp"><figure class="wide"><div class="hm">{spotlight(wire=False)}</div><figcaption>Spotlight · artwork fades into the page; title (display, wraps fully), meta, progress with time left, Resume, ⋮. Text never sits on artwork; the fade reaches the page colour before the title.</figcaption></figure>
<figure class="wide"><div class="hm">{strip(SLIDES, 0, wire=False, focus=2)}</div><figcaption>Filmstrip · selected (3 px primary ring + bold name), keyboard focus (double ring), Next up tag. ← → move, Enter plays, Home/End jump.</figcaption></figure></div></div>

<div class="v-section" id="c-nav"><h2>Bottom bar</h2><p>Today's pill, fixed: icon on every tab, label always visible, current = indicator + filled icon + bold label. Account button labelled “Account and settings, Maya”.</p>
<div class="app sp"><figure class="wide">{nav()}<figcaption>Phone: icon above label, tabs 76 × 56 px, account 64 px. Wider windows: icon beside label, 48 px tall.</figcaption></figure></div></div>

<div class="v-section" id="c-menu"><h2>Card menu and sheet</h2><div class="app sp">
<figure><div class="sheet" role="dialog"><div class="sheet-head"><span class="thumb">{art("sea")}</span><span class="text"><strong>The Long Way Round</strong><span>Northbound · S2 · E5</span></span></div>{actions(SLIDES[0])}</div><figcaption>Phone/tablet: bottom sheet with the title in the header.</figcaption></figure>
<figure><div class="menu card-menu">{actions(SLIDES[0])}</div><figcaption>Desktop: menu at the ⋮ or pointer. Rows 56 px.</figcaption></figure>
<figure><div class="menu card-menu">{actions(dict(t="Low Water", series="Northbound"), offline=True)}</div><figcaption>Offline: Mark as watched queues and syncs later; actions that need the server stay visible, disabled, with the reason.</figcaption></figure></div></div>

<div class="v-section" id="c-edit"><h2>Edit Home controls</h2><div class="app sp"><figure class="wide" style="max-width:560px">{edit_content()}</figure>
<figure><figcaption>Segmented (radio group, 48 px), switch rows with Move up / Move down (disabled at the ends), checkboxes 48 px rows. Every change saves at once; the status line says where it's saved.</figcaption></figure></div></div>

<div class="v-section" id="c-load"><h2>Loading, empty and error pieces</h2><div class="app sp">
<figure><div class="hm"><span class="prog-line"></span></div><figcaption>Refresh progress under the top bar (content stays).</figcaption></figure>
<figure><div class="line"><span class="sk" style="width:112px;height:63px"></span><span class="sk" style="width:144px;height:24px"></span></div><figcaption>Skeleton blocks mirror the loaded layout.</figcaption></figure>
<figure><div class="hm"><div class="empty">{emblem("favorite")}<h2>No favorites yet</h2><p>Use ⋮ › Add to favorites on any title.</p></div></div><figcaption>Empty: emblem, title, one sentence, one action.</figcaption></figure>
<figure><div class="hm"><div class="sec-error">{ic("error")}<div class="body">Couldn’t load New in TV Shows.</div><button class="btn text">Retry</button></div></div><figcaption>Section error: stays inside the section that failed.</figcaption></figure></div></div>

<div class="v-section" id="errors"><h2>Error and feedback framework</h2>
<p>Every message lands on one surface. Decision order: 1. needs a decision or can't be undone → T6; 2. tied to one input → T1/T2; 3. Home can't show its content → T5; 4. a lasting condition → T4; 5. otherwise → T3.</p>
<div class="app sp">
<figure><div class="callout info">{ic("info")}<div class="body"><strong>T1 / T2 · field errors</strong>Not used on Home (no text input). The server address lives in Settings and uses the field error there.</div></div><figcaption>T1, T2</figcaption></figure>
<figure>{SNACK("x1", "Marked “The Long Way Round” as watched", "Undo", hidden=False)}<figcaption>T3 snackbar · reversible actions (Undo), refresh failed (Retry), download started (View). Above the bottom bar, one at a time.</figcaption></figure>
<figure><div class="callout info" role="status">{ic("cloud_off")}<div class="body"><strong>You’re offline</strong>Showing the 2 titles on this device. Home updates when Harbour is back.</div><button class="btn text">Try again</button></div>
<div class="callout warning" role="status">{ic("sync_problem")}<div class="body"><strong>Couldn’t save to Harbour</strong>Your layout is kept on this device and syncs when it can.</div><button class="btn text">Retry</button></div><figcaption>T4 callout · offline, this device, save failed. Below the top bar while the condition lasts.</figcaption></figure>
<figure><div class="hm"><div class="screen-error">{emblem("dns", True)}<h2>Couldn’t reach Harbour</h2><p>The server didn’t respond. Check that it’s running, then try again.</p><div class="acts"><button class="btn">{ic("refresh")}Try again</button></div></div></div><figcaption>T5 · replaces Home's content; top bar and bottom bar stay.</figcaption></figure>
<figure><div class="dialog" role="alertdialog"><h2>Remove download?</h2><p>“Low Tide” (4.2 GB) is removed from this device. You can still stream it from Harbour.</p><div class="actions"><button class="btn text">Keep download</button><button class="btn danger">Remove download</button></div></div>
<figcaption>T6 · only for removing a download (from Downloads/Details; re-downloading costs gigabytes). Nothing on Home itself needs a dialog.</figcaption></figure>
</div></div>
</section>'''


INDEX = '''<title>EmbyEx Home Design</title>
<!-- tokens: ../tokens.json -->
<style><!-- include: viewer.css --></style>
<style><!-- include: components.css --></style>
<style><!-- include: app.css --></style>
<script>window.DESIGN = { app: "EmbyEx · Home", mode: "design", themes: ["light", "dark"], viewports: ["phone", "landscape", "tablet", "desktop"] };</script>
<div class="v-app">
  <nav class="v-nav" aria-label="Pages"></nav>
  <main class="v-main">
    <!-- include: pages/system.html -->
    <!-- include: pages/shell.html -->
    <!-- include: pages/home.html -->
    <!-- include: pages/actions.html -->
    <!-- include: pages/edit.html -->
    <!-- include: pages/seeall.html -->
  </main>
</div>
<script><!-- include: viewer.js --></script>
<script><!-- include: prototype.js --></script>
'''


def main():
    PAGES.mkdir(parents=True, exist_ok=True)
    for name, fn in [("system", system_page), ("shell", shell_page), ("home", home_page), ("actions", actions_page), ("edit", edit_page), ("seeall", seeall_page)]:
        (PAGES / f"{name}.html").write_text(fn() + "\n")
    (ROOT / "src" / "index.html").write_text(INDEX)
    print("wrote", PAGES)


if __name__ == "__main__":
    main()
