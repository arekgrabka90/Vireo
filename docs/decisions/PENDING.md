# Decyzje otwarte

Do czasu decyzji nie zmieniamy powiązanych elementów.

| # | Decyzja | Rekomendacja | Najpóźniej |
|---|---|---|---|
| P1 | **Klucz TMDB i licencja**: czy VIREO będzie komercyjne (wg warunków TMDB wymaga umowy); opcje: A klucz w aplikacji z nagłówkiem tokena, B własny klucz użytkownika, C własne proxy, D umowa komercyjna | Beta: A. Publicznie: C (lub D przy monetyzacji), B jako opcja. Klient TMDB z konfigurowalnym adresem bazowym (E5) | przed E9 |
| P2 | **Gradle Wrapper** (brak `gradlew`): opcja A — dodać przypięty do 8.13 z sumą kontrolną; zmiana CI na `./gradlew` i walidację wrappera | A. Wymaga zgody na zmianę workflow; generowanie bez lokalnego Gradle przez jednorazowe zadanie CI | E0 |
| P3 | **Zmiany CI**: build release (R8), lint, wrapper-validation | Jedna zgoda na pakiet | E1 |
| P4 | **Dystrybucja i `targetSdk`** (34): kanał publikacji i aktualne wymagania sklepu | Sprawdzić wymagania przed decyzją | przed E9 |
| P5 | **Urządzenia referencyjne** (słaby TV ≤2 GB RAM, średni TV, 2 telefony) | Wskazać konkretne modele | E0 |
| P6 | **Diagnostyka**: lokalny eksport logów w 1.0; zewnętrzny crash reporting po ocenie prywatności | Lokalny eksport | E8 |
| P7 | **Manifest**: `ACCESS_NETWORK_STATE` (odzyskiwanie po utracie sieci) | Zgoda | E3 |
| P8 | **Zapis trwały formatu LIVE** per źródło (kolumna, migracja) | Tylko pamięć w 1.0 | E3 |
| P9 | **Dwie ścieżki przeglądania VOD** (TMDB i kategorie dostawcy) | TMDB główna, kategorie dostawcy dodatkowe, bez dalszych inwestycji | E5 |
| P10 | **Martwy kod**: `PlayerController`, `session/*`, `TmdbXtreamMatchRepository`, `BrowseScreen`, placeholdery | Usunąć po potwierdzeniu w E0 | E0 |
| P11 | **Porządki poza `docs/`** (`VIREO_Premium_Player_HUD_Prompt.md`, `setup.sh`, lokalne pliki) | Zatwierdzić listę z `reports/repo-cleanup-candidates.md` | E0 |
| P12 | **Źródła per profil** (plan `14→15` z dotychczasowej dokumentacji: przypisanie źródeł i źródło domyślne profilu; macierz testów w `reports/2026-11-room-migration-infra-c1.md`) | Odłożyć na 1.1; jeśli zostaje w 1.0 — osobny etap i migracja z testami | E6 |
| P13 | **Model danych profilu z ograniczeniami** (allowlista kategorii, pole właściciela/PIN) i zmiana schematu | Allowlista, migracja addytywna | E6 |
| P14 | **Minimalna wspierana wersja bazy** | **Zatwierdzone 2026-11-15:** v14; starsze bazy trafiają na ekran odzyskiwania bez automatycznego resetu | zamknięte |
| P15 | **Cleartext i poświadczenia w URL**: polityka (wyjątki per źródło, `network_security_config`, ukrywanie w logach) | Nie wyłączać globalnie bez analizy skutków | E1 |
| P16 | **Uprawnienia konektora GitHub** (zapis zawartości na `work/vireo-development`), aby commity mogły być wykonywane bez skryptu lokalnego | Nadać uprawnienie zapisu zawartości dla repozytorium albo kontynuować paczką + skrypt Termux | teraz |
| P17 | Odroczone: migracja Hilt `kapt` → KSP; modularyzacja Gradle | Po 1.0 | po 1.0 |
| P18 | **Czyszczenie katalogów `aside-*`** (odsunięte bazy nie są usuwane automatycznie): ręczna opcja w ustawieniach | Osobne zadanie po E1; do czasu — bez automatycznego usuwania | przed E9 |
| P19 | **Utrata klucza Keystore** (konta wymagają ponownego wpisania hasła, baza nietknięta) | E1.3: zmiany przygotowane lokalnie, bez commita i CI; testy i weryfikacja urządzenia oczekują; akceptacja właściciela nieudzielona | po CI |
