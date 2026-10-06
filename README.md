# Chicky Spells

A spelling game for young kids. Chicky the white rooster says each word, and the child spells it back by voice or with letter blocks. Grown-ups add the weekly word lists in the parent area (tap "Grown-ups", answer the times-table question).

- `app/` – Android app (WebView + Android speech recognition and text-to-speech)
- `app/src/main/assets/index.html` – the game
- `docs/index.html` – same game for GitHub Pages (main branch, /docs folder)

Every push to `main` builds a debug APK in **Actions → Build Android app → Artifacts → chicky-spells-debug-apk**.
