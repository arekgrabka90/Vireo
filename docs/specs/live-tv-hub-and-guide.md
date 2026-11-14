# Specyfikacja: LIVE TV — Hub, TV Guide Overlay, panel kategorii

**Status:** zatwierdzona (decyzja 0002). Zakres: wydanie 1.0 (etap E4). Wygląd w stylistyce VIREO; TiviMate jest wyłącznie referencją funkcjonalną.

## 1. Trzy elementy

1. **LIVE Hub** — domyślny ekran sekcji LIVE TV (trasa `live` w `MainShell`, zastępuje dzisiejszą listę kategorii).
2. **TV Guide Overlay** — nakładka siatki EPG na pełnoekranowo odtwarzanym kanale (warstwa wewnątrz `PlayerScreen`, bez nowej trasy).
3. **Panel kategorii** — wspólny komponent i stan dla obu.

Referencje wizualne od właściciela: zdjęcie siatki jako overlay na wideo, zdjęcie siatki z podglądem i opisem w nagłówku oraz zdjęcie panelu kategorii (nagłówek grupy ze zwijaniem, lista kategorii, zaznaczona pozycja jako „pigułka”). Nazwy kategorii na zdjęciach są przykładowe; rzeczywiste pochodzą z listy LIVE dostawcy.

## 2. LIVE Hub

**Układ (TV, tablet, telefon w poziomie):**
- Góra: podgląd na żywo (po lewej) i panel informacji o **zaznaczonym** programie: tytuł, godziny, pasek postępu, czas trwania, „źródło • kategoria”, opis, gwiazdka (ulubione).
- Środek: oś czasu (linia „teraz”, znaczniki co 30 min).
- Dół: siatka. Lewa kolumna: logo, nazwa kanału z odznaką jakości, znacznik grającego kanału. Obok bloki programów o szerokości proporcjonalnej do czasu trwania; pierwszy blok to NOW, drugi to NEXT.
- Kategorie nie zajmują ekranu — wysuwają się jako panel.
- Tryb immersyjny: górny pasek aplikacji (TV/tablet) i dolny (telefon) chowają się; wracają po przejściu fokusem w górę z pierwszego wiersza lub przez Back. Wymaga sterowania widocznością paska per trasa w `MainShell`.

**Telefon w pionie (widok kompaktowy):** podgląd 16:9 u góry, przycisk „Kategorie”, lista kanałów z NOW (tytuł i pasek postępu) i NEXT (tytuł i godzina). Te same dane co siatka. Pełna siatka po obrocie do poziomu.

**Zachowanie:**
- Fokus i zaznaczenie nie przełączają kanału; zmieniają tylko panel informacji.
- OK na kanale lub bieżącym programie: jeśli kanał inny niż grający — przełącza **podgląd** (zostajemy w Hub); jeśli to kanał już grający albo OK na podglądzie — otwiera pełny ekran.
- OK na przyszłym programie: szczegóły (bez przełączania). Długie OK: menu (ulubione, pełny ekran, szczegóły).
- Start: podgląd rusza z ostatnim kanałem i kategorią (zastosowanie ustawienia `liveTvStartOnLastChannel`, dziś niekonsumowanego).
- Opuszczenie Hub (inna zakładka) zatrzymuje odtwarzanie i zwalnia strumień. Przejście do pełnego ekranu go nie przerywa.
- Powrót z pełnego ekranu przywraca kanał w podglądzie, kategorię, zaznaczenie, przewinięcie listy i pozycję osi czasu (patrz sekcja 7).
- Stany: brak źródła, ładowanie (najpierw cache, potem odświeżenie), brak EPG („Brak danych programu”, przyciemnione), błąd z „Spróbuj ponownie”. Kategorie zawsze dostępne.

## 3. TV Guide Overlay

**Wejście:** istniejący przycisk TV Guide w HUD odtwarzacza (dziś otwiera panel „teraz/następny”) oraz klawisz Guide pilota.

**Wygląd:** półprzezroczysta warstwa na pełnym ekranie; wideo i dźwięk grają bez przerwy; pozostały HUD ukryty; overlay jest warstwą modalną (pułapka fokusu, przechwytywanie klawiszy). Brak nagłówka z podglądem; szczegóły zaznaczonego programu w rozwiniętym wierszu pod nim. Przy otwarciu zaznaczenie stoi na grającym kanale i bieżącym programie.

**Niezmienniki „brak przypadkowego zappingu”:**
- I1: zaznaczenie, fokus, przewijanie, skok w czasie i zmiana kategorii nigdy nie przełączają kanału.
- I2: jedyna droga do przełączenia to potwierdzenie na trwającym programie (OK lub dotyk: pierwsze dotknięcie zaznacza i rozwija, drugie dotknięcie zaznaczonego bloku albo przycisk „Oglądaj” potwierdza).
- I3: potwierdzenie zamyka overlay i przełącza kanał jednym wywołaniem; po przełączeniu HUD pokazuje baner kanału.
- I4: zdarzenia powtarzane (`repeatCount > 0`) nie potwierdzają; długie OK otwiera menu; ponowne potwierdzenie w ciągu 500 ms jest ignorowane.
- I5: CH+/CH- w overlay przewijają siatkę o stronę, nie przełączają kanału. Poza overlay (zwykłe oglądanie) CH+/CH- zappują po kolejce aktywnej kategorii.
- I6: Back i gest wstecz zamykają overlay bez zmiany kanału.
- I7: OK na programie przeszłym lub przyszłym pokazuje tylko szczegóły (archiwum i przypomnienia po 1.0).

Błędy odtwarzania i failover działają w tle; komunikat błędu pojawia się po zamknięciu overlay.

## 4. Wspólny panel kategorii

- Pojawia się **nad** widokiem ze ściemnieniem tła, bez przesuwania siatki; nie zamyka EPG i nie przerywa odtwarzania.
- Zawartość: nagłówek grupy (nazwa źródła, zwijany), kategorie wirtualne (Ulubione, Ostatnio oglądane, Wszystkie), potem kategorie z listy LIVE dostawcy (dynamicznie). Zaznaczona kategoria wyróżniona; długie nazwy skracane.
- Wybór kategorii zamyka panel, filtruje kanały i EPG, nie przerywa odtwarzania; fokus wraca do siatki na pierwszy kanał listy (albo na grający, jeśli jest w kategorii).
- Jedna aktywna kategoria dla Hub i Overlay, zapamiętywana per profil. Kolejka zappingu CH+/CH- to kolejka aktywnej kategorii.
- Profil z ograniczeniami: panel pokazuje wyłącznie dozwolone kategorie; filtr egzekwowany w warstwie danych.
- **Otwieranie (dotyk):** przeciągnięcie od lewej krawędzi w prawo (wąska strefa ok. 24 dp, bo poziome przeciąganie w siatce przewija czas). **Przycisk „Kategorie” jest zawsze widoczny i stanowi pełnoprawną alternatywę — poprawność obsługi nie może zależeć wyłącznie od `systemGestureExclusion`** (aktywność ukrywa paski systemowe, ale systemowy gest „wstecz” na krawędzi nadal działa na telefonach z nawigacją gestami).
- **Zamykanie:** dotknięcie ściemnienia, przeciągnięcie w lewo, wybór kategorii, Back.
- **D-pad:** Lewo z kolumny kanałów otwiera panel; w siatce pierwsze Lewo z pierwszego programu przenosi fokus do kolumny kanałów, kolejne otwiera panel. Back lub Prawo zamyka panel (w overlay Back zamyka najpierw panel, potem overlay). Fokus uwięziony w panelu, wraca na poprzedni element po zamknięciu.

## 5. Nawigacja i architektura

- Hub zastępuje `LiveTvScreen` w trasie `live`. Pełny ekran = nawigacja do trasy `player` (jak dziś). Overlay = warstwa w `PlayerScreen`; obsługa Back o priorytetach: panel kategorii → overlay → HUD → wyjście.
- **Host odtwarzania** (`specs/playback-host.md`): podgląd w Hub i pełny ekran używają tego samego odtwarzacza; jedno połączenie z dostawcą.
- **Jeden przypadek użycia „dostrój kanał”** wspólny dla Hub i Overlay (historia, failover, EPG spójne).
- **`LiveBrowseState`** (singleton, resetowany przy zmianie profilu): aktywna kategoria, lista kanałów, kolejka zappingu. Zaznaczenie w widoku jest lokalne i niezależne od hosta; host dostaje wyłącznie jawne „dostrój”.
- Komponenty wspólne: siatka, panel informacji o programie (wariant nagłówka w Hub, wariant rozwinięty w overlay), `CategoryDrawer`, podgląd, kompaktowy wiersz kanału (telefon w pionie).
- Dane: kanały z katalogu LIVE (zapytania po kategorii z `LIMIT`), EPG z magazynu (`specs/epg-store.md`).
- Ulubione per profil: nowa encja (migracja addytywna z testem). Zapamiętanie kategorii i ostatniego kanału: istniejące magazyny kluczy (`app_settings` lub ustawienia profilu), bez nowych zależności.

## 6. Mapa klawiszy i gestów

**Hub (D-pad):** góra/dół — kanały (z pierwszego wiersza w górę: górny pasek); lewo/prawo — poprzedni/następny program; OK — wg sekcji 2; długie OK — menu; CH+/CH- i PageUp/PageDown — strona kanałów; przewijanie klawiszami — ±3 h; przycisk „Teraz” — powrót do bieżącej godziny; klawisze numeryczne — numer kanału; Back — wyjście.

**Overlay:** jak Hub z modyfikacjami z sekcji 3 (OK = potwierdzenie na trwającym programie, CH+/CH- = strona siatki, Back zamyka).

**Dotyk:** przewijanie pionowe (kanały) i poziome (jedna wspólna oś czasu); pierwszy dotyk zaznacza, drugi na zaznaczonym potwierdza (Hub: podgląd lub pełny ekran; overlay: dostrojenie); dotknięcie podglądu w Hub otwiera pełny ekran; długie dotknięcie — menu; gest od lewej krawędzi i przycisk „Kategorie” — panel.

## 7. Stan i odtwarzanie po powrocie

- Po powrocie z pełnego ekranu do Hub muszą zostać odtworzone: grający kanał (z hosta), aktywna kategoria, zaznaczony kanał i program, przewinięcie listy oraz pozycja osi czasu.
- Mechanizm: `ViewModel` wpisu `main` (żyje na stosie), `rememberSaveable` dla przewinięcia i osi czasu, trwały zapis kategorii i ostatniego kanału per profil. Po zabiciu procesu odtwarzane są kategoria i kanał; przewinięcie i oś czasu best-effort.
- Zachowanie stanu wewnętrznego `NavHost` przy przejściu na trasę zewnętrzną musi zostać zweryfikowane na urządzeniu [U].

## 8. Rozróżnienie wizualne stanów

Trzy odrębne stany, rozróżnialne nie tylko kolorem:
- **Gra** — znacznik odtwarzania (ikona) i akcent w nazwie kanału.
- **Zaznaczony** — trwałe wyróżnienie wiersza/bloku (pigułka lub obrys); panel informacji pokazuje ten element.
- **Fokus D-pad** — pierścień fokusu z podniesieniem.
Ten sam element może być w kilku stanach naraz; kombinacje muszą być czytelne.

## 9. Koordynacja strojenia

- Jeden `TuneCoordinator`: pojedynczy lot, zasada „ostatnie żądanie wygrywa”, numer sekwencji; wyniki spóźnione są ignorowane; poprzednie rozwiązywanie źródła jest anulowane.
- Szybkie powtarzane akcje (np. przytrzymane CH+/CH-) są zlewane z krótkim opóźnieniem; pojedyncze naciśnięcie działa natychmiast.
- Źródło nowego kanału jest rozwiązywane **przed** zatrzymaniem starego strumienia; błąd rozwiązania pozostawia stary kanał grający.
- Stan interfejsu wynika ze stanu hosta (nie optymistyczny): znacznik „gra” odzwierciedla faktyczny stan odtwarzacza. Błąd strojenia nie może zostawić rozjazdu: selekcja zostaje, znacznik „gra” wskazuje to, co faktycznie gra (lub nic), cel pokazuje błąd.
- Brak EPG nigdy nie blokuje strojenia ani odtwarzania.

## 10. Kryteria akceptacji E4

**Hub**
1. Po wejściu w LIVE TV od razu widoczne: lista kanałów z NOW/NEXT oraz ramówka (poziomo siatka, pionowo lista kompaktowa), bez pełnoekranowego odtwarzacza i bez ekranu samych kategorii. Zimny stan z cache ≤700 ms (p90).
2. Fokus i przewijanie nie przełączają kanału (test z fałszywym hostem: 0 wywołań „dostrój”).
3. OK na innym kanale przełącza podgląd; OK na grającym lub na podglądzie otwiera pełny ekran; powrót zachowuje kanał, kategorię, zaznaczenie, przewinięcie i oś czasu.
4. Wyjście z Hub zwalnia strumień (brak aktywnego połączenia po ≤2 s).
5. Trzy stany wizualne (gra / zaznaczony / fokus) są rozróżnialne.

**Overlay**
6. Wideo gra podczas 60 s przeglądania (przewijanie, zmiana kategorii) bez `onPlayerError` i bez rebufferingu wywołanego przez UI (lokalny strumień testowy).
7. Niezmienniki I1–I7: w 100 losowych sekwencjach zdarzeń zaznaczenia, przewijania i zmiany kategorii liczba przełączeń = 0; potwierdzenie = dokładnie 1 przełączenie; `repeatCount > 0` i duplikaty w 500 ms nie potwierdzają.
8. Back zamyka bez zmiany kanału; reszta HUD nie reaguje na klawisze przy otwartym overlay.
9. Szybkie powtarzane akcje nie uruchamiają równoległych strojeń; po błędzie strojenia stan UI i hosta jest spójny.

**Panel kategorii**
10. Ten sam komponent i zachowanie w Hub i overlay; dotyk: gest od krawędzi i przycisk; D-pad: Lewo. Otwarcie ≤200 ms (p90).
11. Wybór zamyka panel, filtruje kanały i EPG, nie przerywa odtwarzania, ustawia kolejkę zappingu; kategorie dynamiczne.
12. Przycisk „Kategorie” działa niezależnie od gestu; na urządzeniach z nawigacją gestami potwierdzone działanie przycisku.

**Siatka i wydajność**
13. Kanały z logo i nazwą, bloki proporcjonalne do czasu, linia „teraz”, dane z jednego magazynu EPG w Hub i overlay.
14. Przewijanie ≤5% klatek janky (`dumpsys gfxinfo`) przy 300 kanałach i oknie 3 h na urządzeniu referencyjnym.

**Weryfikacja:** testy jednostkowe `LiveBrowseState`, `TuneCoordinator` i niezmienników; testy UI z symulowanymi klawiszami D-pad i gestami; testy na pilocie TV i telefonie; pomiary wg `quality-metrics.md`.

## 11. Poza zakresem 1.0

Przypomnienia, archiwum (catch-up) i jego ikona, nagrywanie, wyszukiwanie w całym EPG, kolorowanie po gatunkach, ręczne mapowanie kanał↔EPG, zewnętrzny XMLTV, wiele list ulubionych, osobne okno podglądu w overlay.
