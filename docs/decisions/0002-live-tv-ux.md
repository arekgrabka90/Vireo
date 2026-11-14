# 0002 — Model UX LIVE TV

**Status:** Zatwierdzona (2026-11-14), wraz z sześcioma założeniami. Specyfikacja: `../specs/live-tv-hub-and-guide.md`.

## Decyzja

LIVE TV składa się z trzech elementów o odrębnych rolach:

1. **LIVE Hub** — domyślny ekran sekcji LIVE TV: lista kanałów i ramówka EPG widoczne od razu, bez pełnoekranowego odtwarzacza; podgląd na żywo i informacje o zaznaczonym programie; kategorie jako wysuwany panel.
2. **TV Guide Overlay** — półprzezroczysta pełnoekranowa nakładka siatki EPG na pełnoekranowo odtwarzanym kanale; wideo gra bez przerwy; zaznaczenie nie zmienia kanału, zmiana następuje po zatwierdzeniu.
3. **Wspólny panel kategorii** — ten sam komponent i stan w obu kontekstach, wysuwany nad widokiem; przycisk „Kategorie” jest zawsze dostępną alternatywą gestu.

## Zatwierdzone założenia

1. W Hub OK na innym kanale przełącza podgląd; OK na odtwarzanym kanale lub na podglądzie otwiera pełny ekran.
2. Aktywna kategoria jest wspólna dla Hub i Overlay i zapamiętywana per profil.
3. Telefon w pionie: widok kompaktowy; w poziomie: pełna siatka.
4. Hub działa w trybie immersyjnym.
5. Panel kategorii wyświetla się nad widokiem, bez przesuwania siatki.
6. Opuszczenie Hub zatrzymuje odtwarzanie i zwalnia strumień.

## Konsekwencje

Wymaga hosta odtwarzania (0003), magazynu EPG (0004) i zestawu fokusu (E7.1). Wygląd pozostaje autorski (TiviMate to wyłącznie referencja funkcjonalna).
