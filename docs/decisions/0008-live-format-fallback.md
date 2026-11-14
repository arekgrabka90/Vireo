# 0008 — Fallback LIVE `.m3u8` → `.ts`

**Status:** Zatwierdzona jako część roadmapy. Specyfikacja: `../specs/playback-host.md` (sekcja „Fallback formatu LIVE”).

Fallback jest osobną, czystą polityką z testami tabelarycznymi. Stosujemy go wyłącznie dla kanałów LIVE ze źródeł Xtream z adresem zbudowanym przez providera, tylko przy pierwszej próbie dostrojenia, bez żadnej klatki, i tylko przy błędach klasy formatu (np. 404/403 listy, zły typ treści, uszkodzony manifest). Nie stosujemy go dla błędów sieci, 401, 429 ani odmów z powodu limitu połączeń. Najwyżej jeden fallback na dostrojenie. Zapis trwały formatu per źródło wymaga osobnej decyzji (domyślnie tylko pamięć).
