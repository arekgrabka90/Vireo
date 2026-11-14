# Dokumentacja VIREO

Ten katalog jest jedynym aktualnym źródłem prawdy o projekcie. Poprzednie dokumenty (stary `STATE.md`, katalog `audits/`) zostały zastąpione; ich treść pozostaje w historii Git (ostatni stan sprzed przebudowy: commit `2db5db3`).

## Struktura

| Ścieżka | Zawartość |
|---|---|
| `STATE.md` | Aktualny stan projektu, architektura, matryca funkcji, znane problemy, punkt wejścia dla kolejnych sesji |
| `ROADMAP.md` | Etapy E0–E10 do wydania 1.0, zależności, kryteria ukończenia, weryfikacja, ryzyka |
| `specs/` | Specyfikacje funkcjonalne i techniczne (LIVE TV, EPG, host odtwarzania, synchronizacja, bezpieczeństwo danych, profile, metryki, niezmienniki odtwarzacza) |
| `decisions/` | Decyzje produktowe i architektoniczne ze statusem oraz lista decyzji otwartych (`PENDING.md`) |
| `reports/` | Raporty z audytów i zakończonych etapów |

## Kolejność czytania na początku sesji

1. `STATE.md` (sekcja 11: punkt wejścia).
2. `ROADMAP.md` (aktualny etap).
3. Specyfikacja i decyzje aktualnego etapu.
4. Sprawdzenie gałęzi, HEAD i stanu repozytorium; potem tylko pliki kodu potrzebne do zadania.

## Zasady

- **Kod wygrywa nad dokumentacją.** Rozbieżność = popraw dokument w tym samym etapie.
- **Znaczniki dowodów** (w `STATE.md` i raportach): **[K]** potwierdzone w kodzie przez odczyt źródeł; **[A2]** ustalenie z wcześniejszego audytu (`beec446`), niezweryfikowane ponownie; **[H]** hipoteza; **[U]** wymaga testu na urządzeniu.
- Nie zapisujemy w dokumentacji sekretów, haseł, kluczy ani danych logowania.
- Nie deklarujemy, że testy przeszły, jeśli nie zostały uruchomione. Podajemy numer uruchomienia CI albo piszemy „niezweryfikowane”.
- Każdy etap ma trzy niezależne statusy: **W kodzie / Testy przeszły / Zaakceptowany**. Sukces CI nie oznacza akceptacji.
- Po zakończeniu etapu aktualizujemy `STATE.md`, `ROADMAP.md` i dodajemy raport w `reports/`.
