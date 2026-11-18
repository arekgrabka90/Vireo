# Raport E3.1 — polityki odtwarzania (niepodpięte)

**Data:** 2026-11-18 · **Commit:** `78e66f8` (CI run #171 SUCCESS) · **Status:** wykonane; oczekuje akceptacji właściciela.

## Zakres

Dodane pliki (kod główny, `app/src/main/java/app/vireo/player/`):

- `BufferingWatchdogPolicy.kt` (24 linie),
- `LiveFormatFallbackPolicy.kt` (61),
- `PlaybackResumePolicy.kt` (26),
- `TuneCoordinator.kt` (109).

Testy (`app/src/test/java/app/vireo/player/`): `BufferingWatchdogPolicyTest.kt` (49), `LiveFormatFallbackPolicyTest.kt` (134), `PlaybackResumePolicyTest.kt` (45), `TuneCoordinatorTest.kt` (140).

## Stan podpięcia

Polityki i koordynator są niepodpięte. Odczyt `PlayerViewModel.kt` w dniu 2026-11-18 nie wykazał ich użycia.

## Propozycje parametrów (do zatwierdzenia)

- watchdog buforowania: 10 s,
- koalescencja żądań dostrojenia: 400 ms / 250 ms.

## Decyzje i sprawy otwarte

- Flaga `refusedForConnectionLimit` — do rozstrzygnięcia w E3.7.
- Wyciąganie statusu HTTP z przyczyny błędu Media3 — E3.7.
- P7 — w E3.5 (nasłuch sieci, `ACCESS_NETWORK_STATE`).
- P8 — domyślnie tylko pamięć (zapamiętany format LIVE nie jest zapisywany trwale).

## Niezweryfikowane

- Zawartość i zachowanie polityk nie były w tej sesji ponownie przeglądane; raport opiera się na liście plików commita, przekazaniu pracy i odczycie `PlayerViewModel.kt`.
- Zachowanie na urządzeniach (polityki nie są podpięte).
