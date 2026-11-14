# Kandydaci do porządków poza `docs/` — wymagają zgody

Żaden z poniższych elementów nie został usunięty. Przebudowa `docs/` objęła wyłącznie katalog dokumentacji (stare `docs/STATE.md` zastąpione, `docs/audits/` usunięty; historia w Git).

| Element | Rozmiar / miejsce | Uzasadnienie | Ryzyko | Weryfikacja przed usunięciem |
|---|---|---|---|---|
| `VIREO_Premium_Player_HUD_Prompt.md` | 80 KB, katalog główny | Prompt dla innego narzędzia opisujący HUD, który jest już wdrożony w `player/hud/`; może zawierać nieaktualne założenia | Utrata notatek projektowych (historia w Git zostaje) | Porównać z `specs/player-invariants.md`; przenieść wartościowe wymagania do specyfikacji |
| `setup.sh` | 396 B, katalog główny | Szkielet Termux z „fazy 0”: `git init`, `git add -A`, commit, push na `main` | Skrypt jest groźny przy przypadkowym uruchomieniu; brak odwołań w CI | Sprawdzić, czy właściciel go używa |
| `.opencode-permission-probe.txt` | lokalnie, poza repozytorium | Plik narzędziowy poprzedniego procesu | Brak (nieśledzony) | Właściciel decyduje lokalnie |
| `AGENTS.md` | lokalnie, poza repozytorium | Opis ról poprzedniego procesu; sprzeczny z `decisions/0001` | Brak (nieśledzony) | Właściciel aktualizuje lokalnie |
| `README.md`, `README.pl.md`, `README.es.md` | katalog główny | Wielojęzyczne README; mogą wymagać aktualizacji opisu produktu | Brak | Przegląd treści w E9 (dokumentacja użytkownika) |
| `PlayerController`, `session/PlaybackQueue`, `PlaybackSessionState`, `TmdbXtreamMatchRepository`, `BrowseScreen`, nieosiągalne placeholdery | kod | Prawdopodobny martwy kod (patrz `STATE.md` 5.7 i `PENDING.md` P10) | Testy referencjonują część klas | Potwierdzić brak użycia produkcyjnego w E0; usunąć razem z testami za zgodą |
