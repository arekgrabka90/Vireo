# Specyfikacja: niezmienniki odtwarzacza i HUD

**Status:** obowiązujące (przeniesione z dotychczasowej dokumentacji jako aktualne wymagania właściciela). Dotyczą każdej zmiany HUD i odtwarzacza.

1. Warstwa wideo znajduje się pod HUD.
2. HUD wyświetla logo albo tytuł/tekst.
3. Plakat nigdy nie zastępuje logo.
4. Nie wolno fabrykować metadanych, których nie dostarcza źródło.
5. LIVE ma dwa oddzielne paski informacji.
6. HUD filmu nie może wyświetlać miniaturek odcinków.
7. Autoplay ma działać bez drugiego, konkurencyjnego mechanizmu uruchamiania.

## Reguła logo

- Jeśli logo jest dostępne, wyświetl logo.
- Jeśli logo nie jest dostępne, wyświetl zwykły tekst.
- Nigdy nie używaj plakatu jako zastępstwa logo.
- Zachowuj proporcje i czytelność oryginalnego logo.

## Uzupełnienie (2026-11-14)

TV Guide Overlay i host odtwarzania muszą zachować te niezmienniki (m.in. warstwa wideo pod interfejsem; overlay jest warstwą ponad HUD i ukrywa go na czas działania).
