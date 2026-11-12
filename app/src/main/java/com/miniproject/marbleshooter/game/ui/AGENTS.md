# Game UI and session rules

- Read repository and app-level `AGENTS.md` before changing this package.
- Keep `GameView` a thin Android View adapter. Keep input, navigation, session orchestration and accessibility testable and separated.
- Navigation hit areas must match Canvas controls and virtual accessibility nodes.
- For restart/resume/back/result actions, distinguish campaign, tutorial practice and challenge explicitly.
- Tutorial practice restarts as tutorial, does not create/overwrite campaign save, and does not award campaign progress or lifetime campaign statistics.
- Challenge state and rewards stay isolated from campaign state.
- Keep per-frame methods allocation-light. Never perform repository reads or expensive progress aggregation in `onDraw` hot paths.
- Countdown/input locks and lifecycle pause/resume use the shared game loop; do not bypass fixed-step simulation.
- Test screen transitions/hit targets with Robolectric where possible; preserve TalkBack actions and labels.
