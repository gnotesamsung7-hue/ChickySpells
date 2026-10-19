# Chicky School

A learning app for young kids with Chicky, the school helper chicken: spelling practice (voice or letter blocks) and skip counting (2s, 5s, 10s, 100s, backwards, from any number) with voice, where Chicky listens and corrects mistakes. Lessons go from Kinder to Grade 2, and a quick level-check game guesses where to start. Grown-ups add the weekly word lists in the parent area (tap "Grown-ups", answer the times-table question).

- `app/` – Android app (WebView + Android speech recognition and text-to-speech)
- `app/src/main/assets/index.html` – the game
- `docs/index.html` – same game for GitHub Pages (main branch, /docs folder)

Every push to `main` builds a debug APK in **Actions → Build Android app → Artifacts → chicky-spells-debug-apk**.
