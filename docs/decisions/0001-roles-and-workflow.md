# 0001 — Role i zasady pracy

**Status:** Zatwierdzona (instrukcja stałej współpracy; upoważnienie do commitów i push z 2026-11-14).

## Role

- **Właściciel:** zatwierdza kierunek i istotne decyzje, uruchamia i kontroluje GitHub Actions, akceptuje etapy.
- **Claude (główny architekt i wykonawca):** analizuje, projektuje, implementuje, testuje w zakresie dostępnych narzędzi, prowadzi dokumentację; commity i push na `work/vireo-development` są mu upoważnione.

Dotychczasowy opis ról (OpenCode jako wykonawca, Claude poza workflow) jest zastąpiony. Lokalny `AGENTS.md` (poza repozytorium) wymaga uzgodnienia przez właściciela.

## Zasady

- Commity logiczne, komunikat `typ(zakres): opis`; push wyłącznie na `work/vireo-development`. Bez merge do `main`, tagów, force push, `reset`, `clean`, amend i zmian w `.git` bez osobnej zgody.
- **GitHub Actions:** nie uruchamiamy workflowów ręcznie. Workflow `build.yml` ma wyzwalacz `push`, więc push na gałąź roboczą uruchamia go automatycznie; wyniki sprawdza właściciel.
- Nie uruchamiamy lokalnego Gradle. Nie deklarujemy, że testy przeszły, jeśli nie zostały uruchomione; podajemy numer runu CI albo „niezweryfikowane”.
- Bez osobnej zgody nie zmieniamy: `applicationId`/package name, podpisywania i keystore, workflowów CI, wersji Gradle/AGP/Kotlin/compileSdk, Media3 1.11.1; nie zmieniamy ani nie dodajemy zależności; nie wykonujemy migracji grożących utratą danych; nie zmieniamy architektury i zakresu zatwierdzonego etapu.
- Nie czytamy, nie ujawniamy i nie zapisujemy sekretów ani danych uwierzytelniających.
- Pliki poza `docs/` nie są usuwane bez zatwierdzonej listy kandydatów z uzasadnieniem.
- Statusy etapu: W kodzie / Testy przeszły / Zaakceptowany (niezależne).
- Po każdym etapie: krótkie podsumowanie (zmienione pliki, wykonane prace, commity, wyniki dostępnych testów, pozostałe zadania) i aktualizacja dokumentacji.
- Uwaga operacyjna (2026-11-14): konektor GitHub używany przez Claude'a miał uprawnienia tylko do odczytu (odpowiedź 403 na zapis). Zmiany trafiają do repozytorium przez paczkę i skrypt Termux wykonywane lokalnie przez właściciela, do czasu nadania konektorowi uprawnienia zapisu zawartości.
