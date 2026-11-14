# Specyfikacja: host odtwarzania, stabilność, fallback formatu LIVE

**Status:** zatwierdzona koncepcja (decyzja 0003); implementacja w E3 po spike'u S1.

## 1. Stan zweryfikowany [K]

- `Media3PlayerEngine` jest `@ViewModelScoped`; `ExoPlayer` powstaje w inicjalizatorze silnika (`ExoPlayer.Builder(context)` z krokami seek ±10 s), bez `AudioAttributes`, bez `LoadControl`, bez konfiguracji user-agenta. Zwolnienie razem z ViewModelem.
- `PlayerScreen` (trasa `player`): `PlayerView` tworzony w `AndroidView.factory` z `setPlayer(player)`, `useController = false`, `keepScreenOn = true`; `onDispose` pauzuje i zapisuje postęp. Zewnętrzny `NavHost` nie komponuje wpisów poniżej szczytu stosu, więc po wejściu na trasę `player` ekran Hub jest rozkomponowany.
- Cykl życia: `ON_STOP` zapisuje postęp i pauzuje; `ON_START` wznawia, jeśli użytkownik chciał odtwarzać. Start w `LaunchedEffect` (klucze: `settingsReady`, `url`, `contentKey`, `retryToken`) — po odtworzeniu aktywności możliwy ponowny `startPlayback` [H][U].
- `PlayerViewModel` używa singletona `PlaybackSession` (kolejka, alternatywy źródeł) i silnika bezpośrednio.

## 2. Wymagania

1. Przejście Hub ↔ pełny ekran ↔ Overlay nie zwalnia ani nie odtwarza na nowo odtwarzacza i nie powoduje ponownego zestawiania połączenia z dostawcą.
2. Jeden odtwarzacz = jedno połączenie z dostawcą.
3. Opuszczenie Hub zatrzymuje odtwarzanie i zwalnia strumień (zatwierdzone założenie 6).
4. Stabilność przy przechodzeniu aplikacji do tła i powrocie (także gdy MediaSession i odtwarzanie w tle są odłożone na 1.1).

## 3. Projekt hosta

- `PlaybackHost` (singleton, zakres aplikacji) jest właścicielem instancji `ExoPlayer` i `PlayerEngine`; kontrakt `PlayerEngine` pozostaje niezależny od Media3.
- Stany hosta: `NoPlayer` → `Idle` (instancja bez mediów) → `Playing/Paused` (źródło aktywne). Po bezczynności (np. 60 s bez mediów i bez widocznej powierzchni) instancja jest zwalniana i tworzona ponownie na żądanie.
- Powierzchnia wideo jest dołączana i odłączana przez widoki (podgląd w Hub, `PlayerScreen`); host nie zależy od cyklu życia widoku. `PlayerViewModel` przestaje być właścicielem silnika (zachowuje logikę sesji, recovery i postępu).
- Operacje strojenia przechodzą przez `TuneCoordinator` (patrz `live-tv-hub-and-guide.md`, sekcja 9).

## 4. Spike S1 (warunek decyzji 0003)

Nie zakładamy, że sam singleton ani `PlayerView.switchTargetView` rozwiązuje problem. Weryfikujemy na urządzeniach (TV i telefon) protokołem:

1. 20 cykli Hub → pełny ekran → Hub na kanale LIVE (lokalny strumień testowy).
2. Asercje na każdy cykl: ta sama instancja `ExoPlayer` (identyfikator obiektu w logu); `playbackState` nie przechodzi przez `IDLE`/`ENDED`; brak `onPlayerError`; czas do `onRenderedFirstFrame` po przeniesieniu powierzchni ≤700 ms; liczba `onAudioUnderrun` = 0; serwer testowy widzi dokładnie 1 połączenie; brak wzrostu liczby kodeków w `dumpsys media.player`.
3. Warianty do porównania: `PlayerView.switchTargetView`; ręczne `setVideoSurface` z przeniesieniem `SurfaceView`/`TextureView`; kolejność komponowania nowego i starego widoku w przejściu `NavHost`.
4. Test zmiany konfiguracji (motyw, język, tryb wielu okien) podczas odtwarzania: brak restartu transmisji.
5. Wynik: raport w `reports/`. Jeśli S1 nie przejdzie, **plan B**: trwała powierzchnia wideo ponad `NavHost` (jedna warstwa wideo na poziomie aktywności, interfejs Hub/Player/Overlay rysowany nad nią i pozycjonowany względem prostokąta wideo). Plan B wymaga osobnej decyzji.

## 5. Stabilność tła i powrotu

- **LIVE po przerwie:** po pauzie dłuższej niż 2 min ponowne ustawienie źródła (skok do krawędzi na żywo) zamiast wznawiania ze starego miejsca; VOD zachowuje pozycję.
- **Zwolnienie połączenia:** dla LIVE w tle po 60 s zatrzymanie i wyczyszczenie strumienia (limit połączeń dostawcy), powrót przez ponowne przygotowanie.
- **Zmiana konfiguracji:** `startPlayback` nie może restartować transmisji, gdy źródło i pozycja się nie zmieniły (idempotencja startu).
- **Śmierć procesu:** zapis minimalnego stanu ostatniego odtwarzania (klucz, źródło, pozycja, kanał LIVE) i przywrócenie.
- **Audio focus** i `handleAudioBecomingNoisy` ustawione w buildzie `ExoPlayer`; utrata i odzyskanie focusu obsługiwane (pauza/wznowienie).
- **Sieć:** watchdog zawieszonego buforowania; nasłuch sieci (wymaga `ACCESS_NETWORK_STATE`, decyzja P7); ponowienia z opóźnieniem po przywróceniu łączności.
- **Macierz testów (TV i telefon):** powrót po 5 s, 2 min, 20 min dla LIVE i VOD; blokada ekranu; przełączenie aplikacji; zabicie procesu; zmiana Wi-Fi/dane; brak pamięci. Kryteria: wznowienie bez błędu w ≤3 s po przerwie do 2 min; dostrojenie LIVE w ≤5 s po dłuższej; brak wycieku instancji odtwarzacza i połączeń.

## 6. Fallback formatu LIVE (`.m3u8` → `.ts`)

Dziś adres LIVE jest zawsze `.m3u8`. Osobna, czysta polityka (`LiveFormatFallbackPolicy`) z testami tabelarycznymi. Zastosowanie wyłącznie, gdy spełnione **łącznie**:

1. Element LIVE ze źródła Xtream, adres zbudowany przez providera.
2. Pierwsza próba tego dostrojenia, bez żadnej klatki (transport nie osiągnął `READY`).
3. Błąd klasy formatu: HTTP 404 lub 403 listy odtwarzania, zły typ treści, uszkodzony lub nieobsługiwany manifest.
4. **Nie** dla błędów sieci (brak połączenia, limit czasu, DNS), ani dla 401, 429 czy odmowy z powodu limitu połączeń.
5. Najwyżej jeden fallback na dostrojenie; jeśli `.ts` też zawiedzie — zwykły failover do alternatywnego źródła, potem błąd.
6. Po sukcesie `.ts` (stan `READY` w limicie czasu) format jest zapamiętany w pamięci dla źródła; kolejne dostrojenia startują od niego. Zapis trwały dopiero po decyzji P8.
7. Symetrycznie: zapamiętane `.ts` z błędem klasy formatu — jedna próba `.m3u8`.
8. Brak zmiany formatu po osiągnięciu `READY`.

Kod statusu HTTP pobieramy z przyczyny błędu Media3. Testy: tabela `(kod błędu, status HTTP, faza, format)` → akcja oraz lokalny serwer testowy.

## 7. Testy

Jednostkowe: polityki (fallback, recovery), `TuneCoordinator` (wyścigi, ostatnie żądanie wygrywa, anulowanie). Urządzenia: spike S1, macierz tła, soak 8 h LIVE, utrata sieci 30 s (odzyskanie ≤15 s od przywrócenia, failover ≤10 s).
