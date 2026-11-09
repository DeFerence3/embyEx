"""Generates src/pages/details.html (EmbyEx details feature page) from shared building blocks.
Run from docs/design:  python3 scripts/gen_details.py
Every state reuses the same pieces so layouts stay consistent; edit pieces here, not the generated HTML."""
from pathlib import Path

ART = '<div class="dx-art{v}" aria-hidden="true"><div class="sun"></div><div class="wave"></div><div class="wave w2"></div><div class="boat"></div></div>'


def art(variant=""):
    return ART.format(v=(" " + variant) if variant else "")


BACK = '<div class="dx-bar"><button class="icon-btn" aria-label="Back"><i data-icon="arrow_back"></i></button></div>'
SEP = '<span class="sep" aria-hidden="true">·</span>'


def meta(year="2024", runtime="2 h 18 min", rating="PG-13", score="7.8", video="4K · HDR10"):
    parts = [f"<span>{year}</span>", SEP, f"<span>{runtime}</span>", SEP, f'<span class="tag" aria-label="Rated {rating}">{rating}</span>']
    if score:
        parts.append(f'<span class="rating" aria-label="Community rating {score} out of 10"><i data-icon="star"></i>{score}</span>')
    parts += [SEP, f"<span>{video}</span>"]
    return '<div class="dx-meta num">' + "".join(parts) + "</div>"


def genres(*g):
    return '<div class="dx-genres" aria-label="Genres">' + "".join(f'<span class="tag">{x}</span>' for x in g) + "</div>"


def play(kind="resume", sid=""):
    if kind == "resume":
        return ('<div class="dx-play-block"><button class="btn dx-play" data-goto="shell/player/handoff"><i data-icon="play_arrow"></i>Resume from 1:12:40</button>'
                '<div class="dx-resume num"><div class="dx-track-bar" role="progressbar" aria-label="Watched 52%" aria-valuenow="52" aria-valuemin="0" aria-valuemax="100"><i></i></div>'
                '<span>66 min left</span><button class="btn text" data-goto="shell/player/handoff"><i data-icon="replay"></i>Start over</button></div></div>')
    if kind == "start":
        return '<div class="dx-play-block"><button class="btn dx-play" data-goto="shell/player/handoff"><i data-icon="play_arrow"></i>Play</button></div>'
    if kind == "again":
        return ('<div class="dx-play-block"><button class="btn dx-play" data-goto="shell/player/handoff"><i data-icon="replay"></i>Play again</button>'
                '<div class="dx-resume"><span>Watched on 3 Nov</span></div></div>')
    if kind == "offline":
        return ('<div class="dx-play-block"><button class="btn dx-play"><i data-icon="play_arrow"></i>Resume from 1:12:40</button>'
                '<div class="dx-resume num"><div class="dx-track-bar" role="progressbar" aria-label="Watched 52%" aria-valuenow="52" aria-valuemin="0" aria-valuemax="100"><i></i></div>'
                '<span>Plays the downloaded copy</span></div></div>')
    if kind == "episode":
        return ('<div class="dx-play-block"><button class="btn dx-play" data-goto="shell/player/handoff"><i data-icon="play_arrow"></i>Play episode</button></div>')
    raise ValueError(kind)


def fav(on=True, pending=False):
    p = '<i data-icon="sync" class="pending" aria-hidden="true"></i>' if pending else ""
    label = "Favorite" + (", waiting to sync" if pending else "")
    return f'<button class="dx-act" aria-pressed="{str(on).lower()}" aria-label="{label}">{p}<i data-icon="favorite"></i>Favorite</button>'


def watched(on=False, snack=""):
    s = f' data-snackbar="#{snack}"' if snack else ""
    return f'<button class="dx-act" aria-pressed="{str(on).lower()}"{s}><i data-icon="check_circle"></i>{"Watched" if on else "Mark watched"}</button>'


def download(kind, sid=""):
    """Inline download status button. Menus open from the button; on phone they open as a sheet."""
    if kind == "idle":
        return f'<button class="dx-act" data-open="#pick-{sid}"><i data-icon="download"></i>Download</button>' if sid else '<button class="dx-act"><i data-icon="download"></i>Download</button>'
    if kind == "queued":
        to = ' data-state-to="downloading"' if sid else ""
        return f'<button class="dx-act" aria-label="Download queued. Cancel download"{to}><i data-icon="schedule"></i>Queued</button>'
    if kind == "downloading":
        o = f' data-open="#dlm-{sid}"' if sid else ""
        return (f'<div class="menu-anchor" style="display:grid"><button class="dx-act" aria-haspopup="menu" aria-label="Downloading, 42%. Download options"{o}><span class="dx-ring" style="--p:42" aria-hidden="true"></span><span class="num">42%</span></button>'
                + (dl_menu_downloading(sid) if sid else "") + "</div>")
    if kind == "paused":
        return '<button class="dx-act" aria-haspopup="menu" aria-label="Download paused at 42%. Download options"><span class="dx-ring paused" style="--p:42" aria-hidden="true"></span><span class="num">Paused 42%</span></button>'
    if kind == "done":
        o = f' data-open="#dlm-{sid}"' if sid else ""
        return (f'<div class="menu-anchor" style="display:grid"><button class="dx-act is-done" aria-haspopup="menu" aria-label="Downloaded. Download options"{o}><i data-icon="download_done"></i>Downloaded</button>'
                + (dl_menu_done(sid) if sid else "") + "</div>")
    if kind == "done-open":
        return ('<div class="menu-anchor" style="display:grid"><button class="dx-act is-done" aria-haspopup="menu" aria-expanded="true"><i data-icon="download_done"></i>Downloaded</button>'
                + dl_menu_done("", static=True) + "</div>")
    if kind == "failed":
        to = ' data-state-to="downloading"' if sid else ""
        return f'<button class="dx-act is-failed" aria-label="Download failed. Retry download"{to}><i data-icon="error"></i>Retry</button>'
    if kind == "offline":
        return '<button class="dx-act is-done" aria-label="Downloaded"><i data-icon="download_done"></i>Downloaded</button>'
    if kind == "offline-none":
        return '<button class="dx-act" disabled aria-describedby="offline-why"><i data-icon="download"></i>Download</button>'
    raise ValueError(kind)


def dl_menu_downloading(sid):
    return (f'<div class="menu end" role="menu" id="dlm-{sid}" data-overlay hidden aria-label="Download options">'
            '<div class="menu-label num">42% · 7.7 of 18.4 GB · about 6 min left</div>'
            '<button class="menu-item" role="menuitem" data-state-to="paused"><i data-icon="pause"></i><span class="text">Pause download</span></button>'
            '<button class="menu-item danger" role="menuitem" data-state-to="resuming"><i data-icon="close"></i><span class="text">Cancel download</span></button></div>')


def dl_menu_done(sid, static=False):
    attrs = 'data-overlay hidden' if not static else ''
    idattr = f' id="dlm-{sid}"' if sid else ""
    rm = f' data-goto="details/movie/remove-confirm"'
    return (f'<div class="menu end" role="menu"{idattr} {attrs} aria-label="Download options">'
            '<div class="menu-label num">On this device · 18.4 GB · 2160p HEVC</div>'
            '<button class="menu-item" role="menuitem" data-goto="shell/player/handoff"><i data-icon="cloud"></i><span class="text"><span>Play from server</span><span>Streams instead of the downloaded file</span></span></button>'
            f'<hr><button class="menu-item danger" role="menuitem"{rm}><i data-icon="delete"></i><span class="text">Remove download</span></button></div>')


def actions(dl="idle", fav_on=True, watched_on=False, sid="", pending=False, snack=""):
    return f'<div class="dx-actions">{fav(fav_on, pending)}{watched(watched_on, snack)}{download(dl, sid)}</div>'


AUDIO = [("English", "5.1 · Dolby Digital+ · Default", True), ("English", "Stereo · Commentary with Hanne Solberg", False), ("Spanish", "5.1 · Dolby Digital", False), ("Norwegian", "Stereo · AAC", False)]
SUBS = ([("Off", "", True)] + [("English", "SRT", False), ("English", "SDH · for deaf and hard of hearing", False), ("English", "Forced · signs and foreign dialogue only", False),
        ("Arabic", "PGS", False), ("Chinese (Simplified)", "SRT", False), ("Chinese (Traditional)", "SRT", False), ("Czech", "SRT", False), ("Danish", "SRT", False), ("Dutch", "SRT", False),
        ("Finnish", "SRT", False), ("French", "SRT", False), ("French", "Forced", False), ("German", "SRT", False), ("German", "SDH", False), ("Greek", "PGS", False), ("Hebrew", "PGS", False),
        ("Hindi", "SRT", False), ("Italian", "SRT", False), ("Japanese", "PGS", False), ("Korean", "PGS", False), ("Norwegian", "SRT", False), ("Polish", "SRT", False), ("Portuguese (Brazil)", "SRT", False), ("Spanish (Latin America)", "SRT", False)])


def radio_items(items, close=True):
    out = []
    for name, sub, on in items:
        tag = ""
        for t in ("SDH", "Forced", "Commentary"):
            if t in sub:
                tag = f'<span class="tag">{t}</span>'
        c = " data-close" if close else ""
        s = f"<span>{sub}</span>" if sub else ""
        out.append(f'<button class="menu-item" role="menuitemradio" aria-checked="{str(on).lower()}"{c}><span class="radio" aria-hidden="true"></span><span class="text"><span>{name}</span>{s}</span>{tag}</button>')
    return "".join(out)


def track_menu(kind, mid="", static=False):
    items = AUDIO if kind == "audio" else SUBS
    label = "Audio track" if kind == "audio" else f"Subtitles · {len(SUBS) - 1} tracks"
    attrs = "" if static else f' id="{mid}" data-overlay hidden'
    return f'<div class="menu" role="menu" aria-label="{label}"{attrs}><div class="menu-label">{label}</div>{radio_items(items, not static)}</div>'


def track_sheet(kind, mid="", static=False):
    items = AUDIO if kind == "audio" else SUBS
    title = "Audio" if kind == "audio" else "Subtitles"
    sub = "Applies to this title. Change it any time during playback." if kind == "audio" else f"{len(SUBS) - 1} tracks. Forced shows only signs and foreign dialogue."
    attrs = 'data-overlay data-static data-only="phone"' if static else f'id="{mid}" data-overlay hidden'
    return (f'<div class="overlay sheet-overlay" {attrs} role="presentation"><div class="sheet" role="dialog" aria-labelledby="{kind}-sh-{mid or "s"}">'
            f'<h2 id="{kind}-sh-{mid or "s"}">{title}</h2><p class="muted t-body-sm" style="margin:0 0 var(--space-3)">{sub}</p>'
            f'<div class="sheet-list" role="menu" aria-label="{title}">{radio_items(items)}</div></div></div>')


def tracks(sid="", audio="English · 5.1", subs="Off", single_audio=False, open_menu=None):
    """Chips show the current choice. Phone opens a bottom sheet, tablet/desktop an anchored menu."""
    def chip(kind, icon, k, v):
        if kind == "audio" and single_audio:
            return f'<span class="chip dx-track static"><i data-icon="{icon}"></i><span class="k">{k}</span>{v}</span>'
        if not sid:
            exp = ' aria-expanded="true"' if open_menu == kind else ""
            menu = f'<div data-only="tablet desktop">{track_menu(kind, static=True)}</div>' if open_menu == kind else ""
            return f'<div class="menu-anchor"><button class="chip dx-track" aria-haspopup="menu"{exp}><i data-icon="{icon}"></i><span class="k">{k}</span>{v}<i data-icon="arrow_drop_down"></i></button>{menu}</div>'
        base = f'<i data-icon="{icon}"></i><span class="k">{k}</span>{v}<i data-icon="arrow_drop_down"></i>'
        return (f'<div class="menu-anchor"><button class="chip dx-track" aria-haspopup="menu" data-only="phone" data-open="#{kind}-sheet-{sid}">{base}</button>'
                f'<button class="chip dx-track" aria-haspopup="menu" data-only="tablet desktop" data-open="#{kind}-menu-{sid}">{base}</button>{track_menu(kind, f"{kind}-menu-{sid}")}</div>')
    return f'<div class="dx-tracks">{chip("audio", "graphic_eq", "Audio", audio)}{chip("subs", "subtitles", "Subtitles", subs)}</div>'


def track_sheets(sid):
    return track_sheet("audio", f"audio-sheet-{sid}") + track_sheet("subs", f"subs-sheet-{sid}") if sid else ""


OVERVIEW_TEXT = ("When a fishing trawler returns to Port Ainsley with no crew aboard, harbour pilot Ines Varga is the only one who believes the sea didn't take them. "
                 "As the tide charts stop matching the calendar and the town closes ranks, she follows the last radio log out past the breakwater, into a storm the village has been hiding for forty years.")


def overview(text=OVERVIEW_TEXT, sid=""):
    return (f'<section class="dx-section"><h2>Overview</h2><p class="dx-overview" id="ov-{sid or "x"}">{text}</p>'
            f'<button class="btn text dx-more" aria-expanded="false" aria-controls="ov-{sid or "x"}">Read more<i data-icon="expand_more"></i></button></section>')


MOVIE_PEOPLE = [("Mara Ellison", "Ines Varga", "photo", None), ("Tomás Okafor", "Callum Reid", "photo", None), ("Juno Larsen", "Dr. Hale", "p2", "JL"),
                ("Ravi Bhatt", "Father Moss", "photo", None), ("Edda Kristin", "Sela", "p3", "EK"), ("Paul Wren", "Harbourmaster", "photo", None),
                ("Hanne Solberg", "Director", "photo", "crew"), ("Idris Calloway", "Writer", "", "IC crew")]
EP_PEOPLE = [("Mara Ellison", "Ines Varga", "photo", None), ("Tomás Okafor", "Callum Reid", "photo", None), ("Lio Andersen", "Guest · Kai", "p3", "LA"),
             ("Sunniva Holt", "Director", "photo", "crew"), ("Idris Calloway", "Writer", "", "IC crew")]


def person(name, role, cls, extra):
    initials = ""
    is_crew = extra and "crew" in extra
    if extra and extra.split()[0] != "crew":
        initials = extra.split()[0]
    if cls != "photo" and not initials:
        initials = "".join(w[0] for w in name.split()[:2])
    ph = f'<span class="ph {cls}" aria-hidden="true">{initials if cls != "photo" else ""}</span>'
    r = f'<span class="crew">{role}</span>' if is_crew else role
    return f'<li><a class="dx-person" href="#" aria-label="{name}, {role}">{ph}<span class="n">{name}</span><span class="r">{r}</span></a></li>'


def cast(people=MOVIE_PEOPLE, title="Cast &amp; crew"):
    return f'<section class="dx-section wide"><h2>{title}</h2><ul class="dx-rail">' + "".join(person(*p) for p in people) + "</ul></section>"


def facts(rows):
    return '<section class="dx-section dx-info"><h2>Media info</h2><dl class="dx-facts">' + "".join(f"<dt>{k}</dt><dd>{v}</dd>" for k, v in rows) + "</dl></section>"


MOVIE_FACTS = [("Released", "14 Mar 2024"), ("Video", "2160p · HEVC · HDR10"), ("Audio", "4 tracks · English, Spanish, Norwegian"), ("Subtitles", "24 tracks"), ("File", '<span class="num">MKV · 18.4 GB</span>')]
EP_FACTS = [("Aired", "9 Oct 2023"), ("Video", "1080p · H.264"), ("Audio", "2 tracks · English, Norwegian"), ("Subtitles", "6 tracks"), ("File", '<span class="num">MKV · 2.1 GB</span>')]


def snackbar(sid, msg, action, static=False, dur=6000):
    h = "" if static else " hidden"
    a = f'<button class="btn text" data-close>{action}</button>' if action else ""
    return f'<div class="snackbar-host"><div class="snackbar" id="{sid}" role="status" data-duration="{dur}"{h}><span class="msg">{msg}</span>{a}</div></div>'


SOURCES = [("2160p · HEVC · HDR10", "MKV · Dolby Digital+ 5.1 · 24 subtitles", "18.4 GB", True), ("1080p · H.264", "MP4 · AAC 5.1 · 6 subtitles", "6.1 GB", False)]


def source_rows(selected=0):
    rows = []
    for i, (a, b, size, _) in enumerate(SOURCES):
        rows.append(f'<button class="dx-source" role="radio" aria-checked="{str(i == selected).lower()}"><span class="radio" aria-hidden="true"></span><span class="text"><span>{a}</span><span>{b}</span></span><span class="size num">{size}</span></button>')
    return '<div role="radiogroup" aria-label="Version to download" style="display:grid;gap:var(--space-1)">' + "".join(rows) + "</div>"


def picker(pid="", static=False, no_space=False):
    """Download version picker: bottom sheet on phone, dialog on tablet/desktop (CSS switches by width)."""
    attrs = "data-overlay data-static" if static else f'id="{pid}" data-overlay hidden'
    space = ('<div class="dx-space num"><span>32.0 GB free on this device</span><div class="meter" aria-hidden="true"><i style="--w:38%"></i></div></div>' if not no_space else
             '<div class="callout error" role="alert" id="nospace"><i data-icon="error"></i><div class="body"><strong>Not enough space</strong>This version needs 18.4 GB and the device has 6.2 GB free. Pick the 1080p version (6.1 GB) or free up space.</div></div>')
    go = ' data-state-to="queued"' if not static else ""
    dis = ' aria-describedby="nospace"' if no_space else ""
    return (f'<div class="overlay dx-pick-overlay" {attrs} role="presentation"><div class="dx-pick" role="dialog" aria-modal="true" aria-labelledby="pk-{pid or "s"}">'
            f'<h2 id="pk-{pid or "s"}">Download The Long Tide</h2><p class="muted">Your server has 2 versions of this movie. Both download as the original file.</p>'
            f'{source_rows()}{space}<div class="actions"><button class="btn text" data-close>Cancel</button><button class="btn"{go}{dis}>Download 18.4 GB</button></div></div></div>')


def head(title="The Long Tide", extra_top="", m=None, g=("Drama", "Mystery", "Thriller"), pl="resume", acts=None, tr=None, callout=""):
    m = m if m is not None else meta()
    return (f'<div class="dx-head">{extra_top}<h1 class="dx-logo">{title}</h1>{m}{genres(*g) if g else ""}{callout}{pl}{acts}{tr}</div>')


def movie(sid="", pl="resume", dl="idle", fav_on=True, watched_on=False, callout="", tr=None, body_extra="", after="", pending=False, snack="",
          art_variant="", hero_cls="", title="The Long Tide", m=None, g=("Drama", "Mystery", "Thriller"), body=None, wrap_cls="", top=""):
    acts = actions(dl, fav_on, watched_on, sid, pending, snack)
    tr = tr if tr is not None else tracks(sid)
    poster = f'<div class="dx-poster" aria-hidden="true">{art()}</div>' if "no-art" in hero_cls else ""
    body = body if body is not None else (overview(sid=sid) + facts(MOVIE_FACTS) + cast())
    return (f'<div class="app{(" " + wrap_cls) if wrap_cls else ""}">{top}<div class="dx"><div class="dx-hero {hero_cls}"><div class="dx-artbox">{art(art_variant)}</div>{BACK}'
            f'<div class="dx-head">{poster}<h1 class="dx-logo">{title}</h1>{m if m is not None else meta()}{genres(*g) if g else ""}{callout}{play(pl)}{acts}{tr}</div></div>'
            f'<div class="dx-body">{body}{body_extra}</div></div>{track_sheets(sid)}{after}</div>')


def state(sid, label, trigger, content, note=""):
    n = f' data-note="{note}"' if note else ""
    return f'<div class="v-state" data-state="{sid}" data-label="{label}" data-trigger="{trigger}"{n}>{content}</div>'


# ---------------------------------------------------------------- movie states
S = []
S.append(state("resuming", "Resuming", "Opened a movie with saved progress",
               movie("r", after=picker("pick-r") + snackbar("sn-w-r", "Marked as watched. Resume position cleared.", "Undo"), snack="sn-w-r"),
               "Default. Play is the only filled button. Try: Download → picker → Queued; Mark watched → T3 undo; Audio/Subtitles chips."))
S.append(state("not-started", "Not started", "No saved progress", movie("ns", pl="start", fav_on=False, after=picker("pick-ns"))))
S.append(state("watched", "Watched", "Played to the end or marked watched", movie("wa", pl="again", watched_on=True, after=picker("pick-wa"))))

skel = ('<div class="app" aria-busy="true" aria-label="Loading details"><div class="dx dx-skel"><div class="dx-hero"><div class="dx-artbox"><div class="skeleton"></div></div>' + BACK +
        '<div class="dx-head"><div class="skeleton title"></div><div class="skeleton line" style="width:60%"></div><div class="skeleton pill"></div>'
        '<div class="dx-actions"><div class="skeleton act"></div><div class="skeleton act"></div><div class="skeleton act"></div></div></div></div>'
        '<div class="dx-body"><div class="stack"><div class="skeleton line" style="width:30%"></div><div class="skeleton line"></div><div class="skeleton line"></div><div class="skeleton line" style="width:80%"></div></div>'
        '<div class="row wide" style="gap:var(--space-3)"><div class="skeleton tile"></div><div class="skeleton tile"></div><div class="skeleton tile"></div><div class="skeleton tile"></div></div></div></div></div>')
S.append(state("loading", "Loading", "First open, nothing cached", skel, "Skeleton mirrors the loaded layout. Back stays usable. No indicator for loads under 300 ms."))

err = ('<div class="app"><div class="dx" style="display:flex;flex-direction:column"><header class="topbar"><button class="icon-btn" aria-label="Back"><i data-icon="arrow_back"></i></button><span class="title">Details</span></header>'
       '<div class="screen-error"><svg class="art" viewBox="0 0 160 120" aria-hidden="true"><rect x="24" y="22" width="112" height="70" rx="14" style="fill:var(--color-surface-container-high)"/>'
       '<path d="M58 57a22 22 0 0 1 44 0" style="fill:none;stroke:var(--color-primary);stroke-width:7;stroke-linecap:round"/><circle cx="80" cy="66" r="6" style="fill:var(--color-primary)"/>'
       '<path d="M70 104h20" style="stroke:var(--color-outline);stroke-width:6;stroke-linecap:round"/></svg>'
       '<h2>Couldn\'t load this title</h2><p>Your server at media.home.lan didn\'t respond. Check that it\'s running, then try again.</p>'
       '<button class="btn" data-state-to="loading">Try again</button><button class="btn text" data-goto="details/movie/resuming">Back</button></div></div></div>')
S.append(state("error", "Load failed", "Request failed, nothing cached", err, "T5: the screen's content can't be shown. Top bar stays so the user isn't trapped. Server name helps people with several servers."))

off_call = ('<div class="callout warning" role="status"><i data-icon="cloud_off"></i><div class="body"><strong>You\'re offline</strong>Playing the downloaded copy. Favorite and watched changes sync when you reconnect.</div></div>')
off_tracks = tracks("", audio="English · 5.1", subs="Off")
S.append(state("offline-dl", "Offline (downloaded)", "No connection, title is downloaded",
               movie(pl="offline", dl="offline", callout=off_call, pending=True, body=overview(sid="od") + facts(MOVIE_FACTS) +
                     cast([(n, r, ("" if c == "photo" else c), (x if x else "".join(w[0] for w in n.split()[:2])) if c == "photo" else x) for n, r, c, x in MOVIE_PEOPLE])),
               note="T4 callout while the condition lasts. Favorite shows a sync marker (queued). Cast photos weren't downloaded, so initials. Play from server is unavailable."))

off_none = ('<div class="app"><div class="dx" style="display:flex;flex-direction:column"><header class="topbar"><button class="icon-btn" aria-label="Back"><i data-icon="arrow_back"></i></button><span class="title">Details</span></header>'
            '<div class="screen-error"><i data-icon="cloud_off" style="font-size:48px;color:var(--color-primary)"></i><h2>You\'re offline</h2><p>This title isn\'t downloaded. Connect to your server to see it, or watch something you\'ve downloaded.</p>'
            '<button class="btn">Open downloads</button></div></div></div>')
S.append(state("offline-none", "Offline (not downloaded)", "No connection, nothing cached", off_none, "T5 variant. The action leads to what still works offline."))

S.append(state("source-picker", "Download: pick version", "Tapped Download", movie(after=picker(static=True)),
               "Bottom sheet on phone, dialog on tablet/desktop. Lists the media sources on the server, all original files. The first is preselected; the button repeats the size so the cost is clear. With one source, Download skips the picker."))
S.append(state("queued", "Download: queued", "Confirmed a version", movie("q", dl="queued", after=snackbar("sn-q", "Download queued. Track it in Downloads.", "View", static=True)),
               "T3 confirmation. Tap Queued to continue the demo."))
S.append(state("downloading", "Download: in progress", "Transfer running", movie("dl", dl="downloading"),
               "Ring + percentage, never color alone. Tap it for Pause / Cancel; the menu shows size and time left."))
S.append(state("paused", "Download: paused", "User paused", movie("pa", dl="paused")))
S.append(state("downloaded", "Downloaded", "Transfer finished", movie("dd", dl="done"), "Menu holds Play from server and Remove download (the old ⋮ menu is gone)."))
S.append(state("download-menu", "Downloaded: menu open", "Tapped Downloaded", movie(dl="done-open")))

rm = ('<div class="overlay" data-overlay data-static role="presentation" data-dismissible="false"><div class="dialog" role="alertdialog" aria-labelledby="rm-t" aria-describedby="rm-d">'
      '<h2 id="rm-t">Remove download?</h2><p id="rm-d">The Long Tide (18.4 GB) will be deleted from this device. You can still stream it from your server, but downloading it again takes about 20 minutes.</p>'
      '<div class="actions"><button class="btn text" data-goto="details/movie/downloaded">Keep download</button><button class="btn danger" data-goto="details/movie/resuming">Remove download</button></div></div></div>')
S.append(state("remove-confirm", "Remove download (dialog)", "Chose Remove download", movie(dl="done", after=rm),
               "T6: undo can't bring back an 18 GB file cheaply, so this needs a decision. Not dismissed by tapping outside."))

fail_call = ('<div class="callout error" role="alert"><i data-icon="error"></i><div class="body"><strong>Download stopped at 61%</strong>Your server stopped responding. Check that it\'s reachable, then retry.</div>'
             '<button class="btn text" data-state-to="downloading">Retry</button></div>')
S.append(state("download-failed", "Download failed", "Transfer error", movie("df", dl="failed", callout=fail_call),
               "T4 inline while the failure stands; the button itself also says Retry."))
S.append(state("no-space", "Not enough space", "Picked a version larger than free space", movie(after=picker(static=True, no_space=True)),
               "Shown inside the picker where the choice is made. The Download button stays enabled and points to the reason, never silently disabled."))
S.append(state("watched-undo", "Marked watched (undo)", "Tapped Mark watched while resuming",
               movie(pl="again", watched_on=True, after=snackbar("sn-wu", "Marked as watched. Resume position cleared.", "Undo", static=True)),
               "T3 with Undo: reversible, and Undo restores 1:12:40."))
S.append(state("sync-failed", "Toggle failed", "Favorite request failed",
               movie(fav_on=False, after=snackbar("sn-sf", "Couldn't add to favorites. Your server didn't respond.", "Retry", static=True)),
               "T3: the toggle reverts so the screen never shows a state the server doesn't have."))
pf_call = ('<div class="callout error" role="alert"><i data-icon="error"></i><div class="body"><strong>Couldn\'t start playback</strong>This device couldn\'t open the file. Try again, or pick another version from Download.</div>'
           '<button class="btn text">Try again</button><button class="icon-btn" aria-label="Dismiss"><i data-icon="close"></i></button></div>')
S.append(state("play-failed", "Playback failed", "Returned from the player with an error", movie(callout=pf_call),
               "Judgment call: T4 dismissible callout above Play instead of a snackbar, so there's time to read the reason, and it sits next to the action it's about."))
S.append(state("audio-picker", "Audio track picker", "Tapped the Audio chip",
               movie(tr=tracks(open_menu="audio"), after=track_sheet("audio", static=True)),
               "Anchored menu on tablet/desktop, bottom sheet on phone. Radio items: name + format, tags for Commentary. Arrow keys move, Enter selects, Esc closes."))
S.append(state("subtitle-picker", "Subtitles (24 tracks)", "Tapped the Subtitles chip",
               movie(tr=tracks(open_menu="subs"), after=track_sheet("subs", static=True)),
               "Off first, then alphabetical. SDH and Forced are tags, not colors. List scrolls inside the sheet/menu."))

long_title = "The Extraordinary and Unlikely Voyage of the Northern Lighthouse Keeper's Daughter"
S.append(state("long-title", "Long title, no logo", "No logo image; 84-character title",
               movie(title=long_title, m=meta(year="2019", runtime="3 h 12 min", rating="R", score="6.4", video="1080p"), g=("Adventure", "Drama", "Family", "History", "Romance", "War")),
               "Titles wrap fully (never truncate on the details screen). Genres wrap to a second line."))
S.append(state("no-art", "No backdrop", "Item has a poster but no backdrop", movie(hero_cls="no-art", art_variant="none"),
               "Tinted fallback band + the poster as identity. Same structure, nothing broken."))
sparse_body = facts([("Released", "2021"), ("Video", "720p · H.264"), ("Audio", "1 track · English"), ("File", '<span class="num">MP4 · 1.2 GB</span>')])
S.append(state("sparse", "Sparse metadata", "No overview, no people, one audio track, no rating",
               movie(pl="start", fav_on=False, title="Home Video 2021-08", m=meta(year="2021", runtime="22 min", rating="Not rated", score="", video="720p"), g=(),
                     tr=tracks(single_audio=True, audio="English"), body=sparse_body, art_variant="none", hero_cls="no-art"),
               "Empty sections are removed, not labelled 'None'. A single audio track is a static label, not a dropdown."))
S.append(state("big-text", "Large text (200%)", "System font size at 200%", movie(wrap_cls="big-text"),
               "Actions stack into full-width rows, chips wrap, nothing clips or truncates. Play grows with the text."))

# ---------------------------------------------------------------- episode states
SEASON = [("E1", "The Crossing", "51 min", "w", None), ("E2", "Salt", "47 min", "w", None), ("E3", "Lantern Rock", "49 min", "w", None), ("E4", "Low Water", "52 min", "p", "30%"),
          ("E5", "Harbor Lights", "48 min", "c", None), ("E6", "The Breakwater", "51 min", "", None), ("E7", "Undertow", "50 min", "", None), ("E8", "North Light", "58 min", "", None)]


def ep_card(e, t, d, st, w):
    cur = ' aria-current="true"' if st == "c" else ""
    badge = '<span class="badge-w"><i data-icon="check_circle"></i>Watched</span>' if st == "w" else ('<span class="badge-w">Now viewing</span>' if st == "c" else "")
    bar = f'<span class="bar" aria-hidden="true"><i style="--w:{w}"></i></span>' if st == "p" else ""
    extra = f" · {w} watched" if st == "p" else ""
    variant = "night" if e in ("E2", "E5", "E8") else ("dawn" if e in ("E3", "E6") else "")
    return (f'<li><a class="dx-ep" href="#"{cur} aria-label="Episode {e[1:]}, {t}, {d}{extra}{", watched" if st == "w" else ""}">'
            f'<span class="still">{art(variant)}{badge}{bar}</span><span class="t">{e} · {t}</span><span class="s">{d}</span></a></li>')


def season(kind="ok"):
    hdr = '<header><h2>More in Season 2</h2><button class="btn text">All seasons<i data-icon="chevron_right"></i></button></header>'
    if kind == "loading":
        return f'<section class="dx-section wide dx-skel" aria-busy="true">{hdr}<div class="row" style="gap:var(--space-3);overflow:hidden">' + '<div class="skeleton still"></div>' * 5 + "</div></section>"
    if kind == "failed":
        return (f'<section class="dx-section wide">{hdr}<div class="dx-partial" role="status"><i data-icon="error"></i><div class="body">Couldn\'t load Season 2 episodes. The rest of this page is up to date.</div>'
                '<button class="btn text" data-state-to="season-loading">Retry</button></div></section>')
    return f'<section class="dx-section wide">{hdr}<ul class="dx-rail">' + "".join(ep_card(*x) for x in SEASON) + "</ul></section>"


def epnav(prev=("Previous · S2 E4", "Low Water"), nxt=("Next · S2 E6", "The Breakwater")):
    p = f'<a href="#" class="prev"><i data-icon="skip_previous"></i><span class="text"><span>{prev[0]}</span><span class="truncate">{prev[1]}</span></span></a>' if prev else ""
    n = f'<a href="#" class="next" data-goto="details/episode/loaded"><span class="text"><span>{nxt[0]}</span><span class="truncate">{nxt[1]}</span></span><i data-icon="skip_next"></i></a>' if nxt else ""
    return f'<nav class="dx-epnav" aria-label="Episodes">{p}{n}</nav>'


EP_OVERVIEW = ("A blackout leaves Port Ainsley in the dark on the night the ferry is due. Ines and Callum split up to relight the harbour, and Kai finally tells Ines what he saw on the breakwater.")


def episode(se="S2 · E5", title="Harbor Lights", season_kind="ok", nav=None, dl="idle", finale=False):
    fin = '<span class="tag">Series finale</span>' if finale else ""
    kicker = f'<div class="dx-kicker"><a href="#" aria-label="Open Northbound in your library">Northbound<i data-icon="chevron_right"></i></a><span class="se num">{se}</span>{fin}</div>'
    m = meta(year="2023", runtime="48 min", rating="TV-14", score="8.4", video="1080p")
    nav = nav if nav is not None else epnav()
    body = overview(EP_OVERVIEW, sid="ep") + facts(EP_FACTS) + season(season_kind) + cast(EP_PEOPLE)
    return (f'<div class="app"><div class="dx"><div class="dx-hero"><div class="dx-artbox">{art("night")}</div>{BACK}'
            f'<div class="dx-head">{kicker}<h1 class="dx-logo">{title}</h1>{m}{play("episode")}{actions(dl, fav_on=False)}{tracks(audio="English · Stereo", subs="English")}{nav}</div></div>'
            f'<div class="dx-body">{body}</div></div></div>')


E = []
E.append(state("loaded", "Loaded", "Opened an episode", episode(),
               "Series name links to the library. Prev/Next sit under the actions; the season rail marks watched, in-progress and the current episode with text, not color alone."))
E.append(state("season-loading", "Season row loading", "Season list still loading", episode(season_kind="loading"), "Only the rail shows a skeleton; the rest is usable."))
E.append(state("season-failed", "Season row failed", "Season request failed", episode(season_kind="failed"), "Partial failure stays inside its section, with Retry."))
E.append(state("first", "First episode", "S1 · E1", episode(se="S1 · E1", title="Fog Season", nav=epnav(prev=None, nxt=("Next · S1 E2", "Ballast")))))
E.append(state("season-end", "Last in season", "S2 · E8, Season 3 exists", episode(se="S2 · E8", title="North Light", nav=epnav(prev=("Previous · S2 E7", "Undertow"), nxt=("Next season · S3 E1", "Thaw")))))
E.append(state("finale", "Series finale", "Last episode of the show", episode(se="S3 · E10", title="Slack Water", nav=epnav(prev=("Previous · S3 E9", "Spring Tide"), nxt=None), finale=True),
               "Next disappears; the layout keeps Previous in its column instead of stretching."))

page = f'''<section class="v-page" data-page="details" data-title="Item details" data-group="Features">
<h1>Item details</h1>
<p class="v-lead">Movies and episodes. The one question this screen answers: "Do I want to watch this now, and how?" Play is always the most prominent control; everything else is tonal and labelled.</p>
<article class="v-screen" data-screen="movie" data-title="Movie" data-purpose="Primary action: Play / Resume. Secondary: Favorite, Mark watched, Download, audio and subtitle choice.">
{"".join(S)}
</article>
<article class="v-screen" data-screen="episode" data-title="Episode" data-purpose="Primary action: Play episode. Adds series context, previous/next and the rest of the season.">
{"".join(E)}
</article>
</section>'''
Path("src/pages/details.html").write_text(page)
print("wrote src/pages/details.html", len(page))

# ---------------------------------------------------------------- shell page (global behaviour)
def annotate(html):
    marks = [('<button class="icon-btn" aria-label="Back">', 1), ('<button class="btn dx-play"', 2), ('<button class="btn text" data-goto="shell/player/handoff"><i data-icon="replay">', 3),
             ('<button class="dx-act" aria-pressed="true"', 4), ('<button class="dx-act" aria-pressed="false"', 5), ('<button class="dx-act"><i data-icon="download">', 6),
             ('<div class="menu-anchor"><button class="chip dx-track"', 7), ('<div class="menu-anchor"><button class="chip dx-track"', 8),
             ('<button class="btn text dx-more"', 9), ('<li><a class="dx-person"', 10)]
    for needle, n in marks:
        i = html.find(needle)
        if i < 0:
            continue
        tag_end = html.find(" ", i + 1)
        if needle.startswith("<div") or needle.startswith("<li"):
            # annotate the inner element
            j = html.find("<", i + 1)
            k = html.find(" ", j)
            html = html[:k] + f' data-fo="{n}"' + html[k:]
            html = html.replace(f'data-fo="{n}" class="', f'data-fo="{n}" class="fo ', 1)
        else:
            html = html[:tag_end] + f' data-fo="{n}"' + html[tag_end:]
            html = html.replace(f'data-fo="{n}" class="', f'data-fo="{n}" class="fo ', 1)
    return html.replace('class="fo btn dx-play"', 'class="fo btn dx-play is-focus"', 1)


focus_state = annotate(movie())
handoff = ('<div class="app" data-theme-panel="dark" aria-busy="true"><div class="dx" style="display:grid;place-content:center;justify-items:center;gap:var(--space-4);min-height:100%">'
           '<div class="spinner" role="progressbar" aria-label="Starting playback"></div><p class="t-title-sm" style="margin:0">Starting The Long Tide</p>'
           '<button class="btn text" data-goto="details/movie/resuming">Back to details</button></div></div>')
shell = f'''<section class="v-page" data-page="shell" data-title="Shell &amp; behaviour" data-group="Foundations">
<h1>Shell &amp; behaviour</h1>
<p class="v-lead">How the details screen sits in the app and behaves across input methods, window sizes and system settings.</p>
<div class="v-section"><h2>Window sizes</h2>
<p>Layout follows the window width, never the platform name. A desktop window dragged narrow gets the phone layout; a tablet in landscape gets the desktop layout.</p>
<ul><li><strong>Compact &lt; 700</strong>: artwork on top fading into the page, single column, full-width Play, three equal action tiles.</li>
<li><strong>Medium 700–1099</strong>: taller artwork, head column max 640, Play sized to content, Media info as a side column.</li>
<li><strong>Expanded ≥ 1100</strong>: artwork bleeds right, head column on a left scrim, body in two columns. Text never sits on less than a fully opaque scrim.</li></ul></div>
<div class="v-section"><h2>Keyboard, remote and screen reader</h2>
<ul><li>Opening details puts focus on <strong>Play</strong> (desktop, TV remote) so <span class="kbd">Enter</span> starts watching; screen readers start at the title heading.</li>
<li><span class="kbd">Tab</span> / <span class="kbd">Shift</span>+<span class="kbd">Tab</span> follow the numbered order below. Arrow keys move inside the action row, chip row and rails. <span class="kbd">Esc</span> closes a menu/sheet, otherwise goes back.</li>
<li>Rails scroll the focused card into view with <code>scroll-margin</code> so focus is never hidden at an edge (WCAG 2.4.11).</li>
<li>Toggles announce state (<em>"Favorite, toggle, on"</em>); the download tile announces progress (<em>"Downloading, 42%"</em>) and updates politely, at most every 10%.</li></ul></div>
<div class="v-section"><h2>Motion</h2>
<ul><li>Expressive spring on Play press (shape squish) and on toggles (fill morph). Hero artwork fades in over 300 ms; no parallax.</li>
<li>With reduced motion: springs become 150 ms fades, the artwork appears without fade, progress rings update without animation.</li></ul></div>
<article class="v-screen" data-screen="focus" data-title="Focus order" data-purpose="Numbered Tab order; Play shows the focus ring (2 px focus-ring token, 2 px offset).">
{state("map", "Focus order", "Keyboard or remote navigation", focus_state, "Order follows reading order. Rails count as one stop each; arrow keys move within.")}
</article>
<article class="v-screen" data-screen="player" data-title="Player handoff" data-purpose="Where Play leads. Shown only so the prototype flow has a destination.">
{state("handoff", "Starting playback", "Tapped Play / Resume", handoff)}
</article>
</section>'''
Path("src/pages/shell.html").write_text(shell)
print("wrote src/pages/shell.html")

# ---------------------------------------------------------------- design system page
def fig(inner, cap, w=""):
    st = f' style="width:{w}"' if w else ""
    return f"<figure{st}>{inner}<figcaption>{cap}</figcaption></figure>"


def comp(cid, title, usage, specimen, note):
    return f'<div class="v-section" id="{cid}"><h2>{title}</h2><p>{usage}</p><div class="v-specimen app">{specimen}</div><p class="v-note">{note}</p></div>'


ICONS = ("arrow_back,play_arrow,replay,favorite,check_circle,download,schedule,download_done,pause,close,cloud,delete,error,cloud_off,sync,"
         "graphic_eq,subtitles,arrow_drop_down,expand_more,star,chevron_right,skip_previous,skip_next")
play_btn = lambda cls="", extra="", label="Resume from 1:12:40": f'<button class="btn dx-play {cls}" style="width:auto"{extra}><i data-icon="play_arrow"></i>{label}</button>'
C = []
C.append(comp("c-play", "Play button", "The only filled button on the details screen. Label says exactly what happens: Play, Resume from 1:12:40, Play again, Play episode.",
              fig(play_btn(), "default") + fig(play_btn("is-hover"), "hover") + fig(play_btn("is-focus"), "focus") + fig(play_btn("is-pressed"), "pressed") +
              fig(play_btn("is-loading", ' aria-busy="true"'), "starting") + fig(play_btn("", " disabled", "Play"), "disabled"),
              "56 dp tall, full width on compact. Starting keeps its width. Disabled only while the item is still loading; offline it plays the downloaded copy."))
C.append(comp("c-btn", "Buttons", "Tonal and text buttons for everything that isn't Play. Danger only inside a confirmation.",
              fig('<button class="btn secondary">Tonal</button>', "tonal") + fig('<button class="btn outline">Outlined</button>', "outlined") + fig('<button class="btn text"><i data-icon="replay"></i>Start over</button>', "text") +
              fig('<button class="btn text is-focus">Read more</button>', "text · focus") + fig('<button class="btn danger">Remove download</button>', "danger") +
              fig('<button class="icon-btn" aria-label="Back" style="background:var(--color-surface-container);color:var(--color-on-surface)"><i data-icon="arrow_back"></i></button>', "back over artwork"),
              "Targets ≥ 48 dp. The back button sits on a solid surface circle so it stays visible on any artwork."))
C.append(comp("c-act", "Action tile", "Secondary actions with icon and visible label. Toggles use aria-pressed; the label changes with the state where it helps (Mark watched → Watched).",
              fig(fav(False), "favorite · off", "112px") + fig(fav(True), "favorite · on", "112px") + fig(fav(True, True), "on · waiting to sync", "112px") +
              fig(watched(False).replace('class="dx-act"', 'class="dx-act is-hover"'), "hover", "112px") + fig(watched(False).replace('class="dx-act"', 'class="dx-act is-focus"'), "focus", "112px") +
              fig(watched(True), "watched · on", "112px"),
              "64 dp tall, 8 dp gaps. State shows by fill, filled icon and label, never color alone."))
C.append(comp("c-dl", "Download tile", "The download status lives in the third tile. It opens the version picker, then a menu with Pause/Cancel or Play from server/Remove.",
              "".join(fig(download(k), c, "112px") for k, c in [("idle", "idle"), ("queued", "queued"), ("downloading", "downloading"), ("paused", "paused"), ("done", "downloaded"), ("failed", "failed"), ("offline-none", "offline, not downloadable")]),
              "Progress = ring + percentage text. Screen readers hear the same information as the visual label."))
C.append(comp("c-chip", "Track chip", "Shows the current audio or subtitle choice and opens the picker. One audio track → static label.",
              fig(tracks(), "default") + fig('<button class="chip dx-track is-focus"><i data-icon="subtitles"></i><span class="k">Subtitles</span>Off<i data-icon="arrow_drop_down"></i></button>', "focus") +
              fig(tracks(single_audio=True, audio="English").split('<div class="menu-anchor">')[0] + "</div>", "single track (static)"),
              "48 dp tall. aria-haspopup=menu, aria-expanded while open."))
C.append(comp("c-menu", "Menu and radio items", "Anchored menu on medium and expanded windows; the same items in a bottom sheet on compact.",
              fig(f'<div style="position:relative;width:300px;height:330px">{track_menu("audio", static=True).replace("class=\"menu\"", "class=\"menu\" style=\"position:static\"")}</div>', "audio menu") +
              fig(f'<div style="position:relative;width:320px;height:240px">{dl_menu_done("", static=True).replace("class=\"menu end\"", "class=\"menu\" style=\"position:static\"")}</div>', "download menu"),
              "Rows 48 dp. Radio state = ring + dot. Tags (SDH, Forced, Commentary) explain variants in text."))
C.append(comp("c-source", "Version row", "Choosing what to download. Size is always visible because it's the cost.", f'<div style="width:420px">{source_rows()}</div>', "Rows 64 dp, role=radio inside a radiogroup."))
C.append(comp("c-person", "Cast &amp; crew tile", "3:4 rounded-rectangle photo (portrait headshots crop badly in circles), name and role underneath. Crew roles get a small label.",
              fig('<ul class="dx-rail" style="margin:0;padding:0">' + person("Mara Ellison", "Ines Varga", "photo", None) + "</ul>", "photo") +
              fig('<ul class="dx-rail" style="margin:0;padding:0">' + person("Juno Larsen", "Dr. Hale", "p2", "JL") + "</ul>", "no photo → initials") +
              fig('<ul class="dx-rail" style="margin:0;padding:0">' + person("Hanne Solberg", "Director", "photo", "crew") + "</ul>", "crew") +
              fig('<ul class="dx-rail" style="margin:0;padding:0">' + person("Maximilian Alexander von Habsburg-Lothringen", "Older Ferryman (uncredited)", "p3", "MV") + "</ul>", "long name · 2 lines"),
              "Whole tile is one link. Names clamp to 2 lines; the full name is in the accessible label and on the person page."))
C.append(comp("c-ep", "Episode card", "Season rail cards. Watched, in-progress and current each have a text marker.",
              "".join(fig('<ul class="dx-rail" style="margin:0;padding:0">' + ep_card(*x) + "</ul>", c) for x, c in [(SEASON[0], "watched"), (SEASON[3], "in progress"), (SEASON[4], "current"), (SEASON[5], "not started")]),
              "Current card is outlined 3 dp and labelled Now viewing."))
C.append(comp("c-epnav", "Previous / next", "Neighbouring episodes, under the actions on episode details.", f'<div style="width:420px">{epnav()}</div>' + f'<div style="width:420px">{epnav(prev=None)}</div>',
              "Missing side leaves its column empty so Next never jumps left."))
C.append(comp("c-feedback", "Feedback surfaces", "Callouts for conditions, partial errors inside sections, snackbars for events.",
              f'<div class="stack" style="width:520px">{off_call}{fail_call}{season("failed").replace("<section class=\"dx-section wide\">", "<div>").replace("</section>", "</div>")}</div>' +
              f'<div style="position:relative;width:440px;height:80px;display:flex;align-items:flex-end">{snackbar("x1", "Marked as watched. Resume position cleared.", "Undo", static=True)}</div>',
              "Each severity has its own color pair and icon. Offline callouts are role=status; failures role=alert."))
C.append(comp("c-skel", "Skeleton", "Initial load only, mirroring the loaded layout.", f'<div class="dx-skel stack" style="width:320px"><div class="skeleton title"></div><div class="skeleton line" style="width:60%"></div><div class="skeleton pill"></div><div class="row"><div class="skeleton tile"></div><div class="skeleton tile"></div></div></div>',
              "Shimmer stops with reduced motion."))

FRAME = [("T1", "Field error", '<div class="field is-error" style="width:300px"><label for="t1">Server address</label><div class="control"><input id="t1" value="media.home.lan:80960" aria-invalid="true" aria-describedby="t1e"></div><div class="help" id="t1e"><i data-icon="error"></i>Use a port between 1 and 65535</div></div>', "Sign-in only; the details screen has no inputs."),
         ("T2", "Form summary", '<div class="callout error" style="width:360px"><i data-icon="error"></i><div class="body"><strong>2 things need fixing</strong>Server address, Password</div></div>', "Not used on details."),
         ("T3", "Snackbar", f'<div style="position:relative;width:420px;height:64px;display:flex;align-items:flex-end">{snackbar("x2", "Couldn&#39;t add to favorites. Your server didn&#39;t respond.", "Retry", static=True)}</div>', "Mark watched (Undo), toggle failed (Retry), download queued (View)."),
         ("T4", "Inline callout", f'<div style="width:420px">{off_call}</div>', "Offline, download stopped, playback failed (judgment call), section failed."),
         ("T5", "Screen error", '<div class="screen-error" style="padding:var(--space-4)"><h2>Couldn&#39;t load this title</h2><p>Your server at media.home.lan didn&#39;t respond.</p><button class="btn">Try again</button></div>', "Load failed, offline and not downloaded."),
         ("T6", "Blocking dialog", '<div class="dialog" role="alertdialog" aria-label="Remove download?"><h2>Remove download?</h2><p>The Long Tide (18.4 GB) will be deleted from this device.</p><div class="actions"><button class="btn text">Keep download</button><button class="btn danger">Remove download</button></div></div>', "Remove download only.")]
framework = ('<div class="v-section" id="errors"><h2>Error and feedback framework</h2><p>Pick the tier in this order and stop at the first match: needs a decision or is costly → T6; tied to one input → T1/T2; content can\'t show → T5; lasting condition → T4; otherwise an event → T3.</p>'
             + "".join(f'<h3>{t} · {n}</h3><div class="v-specimen app">{x}</div><p class="v-note">{u}</p>' for t, n, x, u in FRAME) + "</div>")

system = f'''<section class="v-page" data-page="ds" data-title="Design system" data-group="Foundations">
<h1>Design system</h1>
<p class="v-lead">Marquee: the artwork sets the mood, the interface stays quiet. Three rules guide every screen: one filled button (Play), every other action tonal with a visible label, and text only ever on solid surface, never on raw artwork.</p>
<div class="v-section"><h2>Color primitives</h2><p>EmbyEx's existing ink-and-mint palette with a warm amber accent. Screens use only the semantic roles. On Android, wallpaper colors can replace these through dynamic color; the role structure stays the same.</p><div data-render="primitives"></div></div>
<div class="v-section"><h2>Semantic roles</h2><p>art-1…3 only paint the artwork stand-ins in this file; they're identical in both themes because a backdrop image doesn't change with the theme.</p><div data-render="roles"></div></div>
<div class="v-section"><h2>Contrast (WCAG 2.2 AA)</h2><div data-render="contrast"></div></div>
<div class="v-section"><h2>Typography</h2><p>The platform's system font (Roboto on Android, Segoe UI on Windows, the desktop font on Linux), so EmbyEx feels part of the OS. No bundled third-party fonts. Display for the title when there's no logo image; title for section headings; tabular figures for times, sizes and percentages.</p><div class="v-card" data-render="type" data-sample="Resume from 1:12:40 · 66 min left"></div></div>
<div class="v-section"><h2>Spacing</h2><div class="v-card" data-render="space"></div></div>
<div class="v-section"><h2>Shape</h2><p>12 for chips, menus and snackbars, 20 for tiles and photos, 28 for dialogs, 36 for sheets, full for Play.</p><div data-render="radius"></div></div>
<div class="v-section"><h2>Elevation</h2><div data-render="elevation"></div></div>
<div class="v-section"><h2>Icons</h2><p>Material Symbols Rounded, filled variant for active toggles and Play.</p><div data-render="icons" data-names="{ICONS}"></div></div>
{"".join(C)}
{framework}
</section>'''
Path("src/pages/system.html").write_text(system)
print("wrote src/pages/system.html")
