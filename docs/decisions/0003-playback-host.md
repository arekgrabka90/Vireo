# 0003 — Host odtwarzania (`PlaybackHost`)

**Status:** Zatwierdzona koncepcja. **Warunek:** spike S1 musi wykazać brak zwalniania i ponownego tworzenia odtwarzacza przy przejściach (`../specs/playback-host.md`). Nie zakładamy, że sam singleton lub `PlayerView.switchTargetView` rozwiązuje problem.

## Problem (stan kodu)

`Media3PlayerEngine` jest `@ViewModelScoped` i tworzy `ExoPlayer` razem z `PlayerViewModel` trasy `player`; zwalnia go razem z ViewModelem. Podgląd w Hub i nakładka nad wideo wymagają jednego odtwarzacza przenoszonego między powierzchniami, a dostawcy IPTV często limitują liczbę połączeń.

## Decyzja

Własność instancji `ExoPlayer` przechodzi do hosta niezależnego od cyklu życia ekranu. Kontrakt `PlayerEngine` pozostaje. Strojenie przez jeden koordynator (tune coordinator), który serializuje operacje. Plan B, gdyby spike nie przeszedł: trwała powierzchnia wideo ponad `NavHost` (opis w specyfikacji).
