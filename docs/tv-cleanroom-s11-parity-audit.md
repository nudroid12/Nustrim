# Nustrim TV Clean-room S11 Parity Audit

## Locked inputs

- Nustrim baseline: `0.55.0-tv-cleanroom-s10-global-polish`, commit `0cc82218d91b3311662375637b4be4431032a120`
- Behavioural and visual reference: Nuvio TV `0.8.6-beta`, commit `082af4e29f4629873c185360638940ea42ba988e`
- Product boundary: Nustrim provider, resolver, playback, subtitle, progress, history, updater, local Library and Mobile UI remain authoritative.
- Nuvio-only accounts, profiles, cloud libraries, addons, collections and provider services are outside the Nustrim parity denominator.

## What S11 can prove automatically

The S11 verifier checks 40 source-level contracts across eight TV surfaces. Each surface has five contracts covering structure, product actions, state handling, focus or navigation, and integration with the accepted Nustrim engines.

The automated threshold is 38 of 40 checks, which is 95 percent. This is a static contract score, not a rendered visual score.

| Surface | Static contracts | Reference characteristics |
| --- | ---: | --- |
| Shell | 5 | Collapsed navigation, expanded labels, DPAD hand-off, deterministic Back |
| Home | 5 | Backdrop hero, catalogue rails, focus memory, loading and retry |
| Search | 5 | TV input, Discover content, recent history, result rails, exact restore |
| Library | 5 | Poster grid, selectors, sorting, empty state, exact restore |
| Settings | 5 | Category rail, detail pane, functional controls, focus hand-off |
| Details | 5 | Hero actions, metadata, seasons, episodes, state handling |
| Sources | 5 | Backdrop identity, source filters, stream list, states, Player route |
| Player | 5 | Media3 surface, transport controls, panels, overlays, switching |

## Manual device acceptance

An Actions success does not prove rendered visual parity or remote-control ergonomics. Before claiming the overall 95 percent target, test the signed APK on a TV device or emulator at 1920 by 1080 and record the result for every item below.

- Shell: open the sidebar from each root screen, move through all four destinations, close to content, and verify Back order.
- Home: traverse the hero and at least three rails, open Details, return, and confirm the exact card regains focus.
- Search: test Discover, a one-character query, a valid query, recent history, clear history, empty results, retry, Details and return focus.
- Library: test both sections, every filter and sort option, empty state, Details and exact return focus.
- Settings: move between all six categories and detail controls, confirm values persist, check Sources and signed update status.
- Details: test a movie and a series, Play, Save, season changes, episode focus, error retry and return from Sources.
- Sources: test All and each source chip, empty or failed providers, refresh, stream selection and exact return focus.
- Player: test play or pause, repeated seek, controls auto-hide, audio, subtitles, speed, aspect ratio, Sources, Episodes, playback error and post-play.
- Visual: verify no clipped text, hidden focus, overscan loss, unstable scaling, layout jumps or unreadable contrast on every surface.

## Known deferred work

Player final polish, episode focus edge cases, subtitle overlays, player overlays, post-play behaviour and updater refinements remain explicit backlog items. S11 records them instead of silently treating them as proven.

## Acceptance rule

- Green Actions means the static contract, lint, unit test, signed APK identity and release publication gates passed.
- Overall 95 percent parity may be claimed only after the manual device checklist also passes with evidence.
