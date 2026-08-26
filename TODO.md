# 📝 GDL-Android Roadmap & Ideas

This file is a living document to drop ideas, track upcoming features, and plan out the future of GDL-Android. 
Whenever you get a cool idea, just drop it in here so it stays with the code!

## 🚀 Upcoming Features / Ideas
- [x] **Auto-Install Updates:** After downloading the APK via the new update checker, automatically prompt the user to install it.
- [ ] **Per-Site Authentication:** Create a dedicated UI to manage logins and cookies for specific sites (Twitter, Pixiv, etc.) individually.
- [ ] **Download History:** A visual history log showing past downloads, separate from the active queue.
- [ ] **Scheduled Downloads:** Allow users to queue up downloads that only trigger when on Wi-Fi or charging.
- [ ] **Bulk Import:** A way to paste a massive list of URLs and have the app queue them all up automatically.
- [ ] **Advanced gallery-dl Config:** A dedicated text field in settings for power users to pass raw, custom command-line arguments (e.g., `--write-metadata`, `--range`, `--chapter-filter`).
- [ ] **In-App Fullscreen Gallery:** Upgrade the current gallery grid so tapping an image opens a beautiful fullscreen viewer with pinch-to-zoom and swipe gestures.
- [ ] **Notification Quick Actions:** Add "Pause", "Resume", and "Cancel" buttons directly inside the Android foreground service download notification.
- [ ] **Media Scanner Trigger:** A button to manually force Android to scan the download folder so new images immediately show up in external gallery apps.
- [ ] **Quick Settings Tile:** Add an Android notification shade drop-down tile to quickly open the app or start a download directly from your clipboard.
- [ ] **Settings Backup & Restore:** Allow users to export their settings and cookies to a file, making it easy to migrate to a new phone.
- [ ] **WebP Support:** Add Cwebp Module in app to convert images to webp format to save space.
- [ ] **Smart Duplicate Detection:** Use lightweight perceptual hashing (pHash) to detect if you've already downloaded an identical image to save storage and keep the gallery clean.

## 🛠️ Refactoring & Tech Debt
- [ ] Move hardcoded Neobrutalist colors into a centralized theme configuration file.
- [x] Setup a proper local Room Database for caching download history so it persists across app restarts.

## 🐛 Known Bugs
- [ ] *(List any bugs you find here)*

---

