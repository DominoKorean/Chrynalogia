# UI design

Read `DESIGN.md` before changing app UI. It records the user's Figma layouts and One UI kit references.

- Use the user's Chrynalogia Figma file as the layout basis. Use the linked One UI kit for colors, typography, component proportions, and theme variants.
- Keep the Grid, Line, and Settings tabs; use two-column grid cards and a count editor whose space stays reserved when the custom grid card is not selected.
- Use original SVG icons from the One UI kit Icons page for in-app actions. Keep the supplied app logo for the camera menu opener.
- Keep user-facing copy in Android string resources, with Korean and English translations. Respect the app's language and system/light/dark theme selections in activities and camera settings.
- Select a white or black outline to contrast with the line color. Do not reintroduce the retired single-line golden ratio option; the golden ratio grid and spiral remain supported.

The app uses native Android Java views. Preserve the camera detection, touch pass-through, and manual session behavior when changing presentation.
