# Raport: infrastruktura schematu i testów migracji Room (C1.1, C1.2)

**Status:** zrealizowane w kodzie, potwierdzone w CI, **niezaakceptowane** przez właściciela. Podsumowanie historyczne wg dotychczasowej dokumentacji i commitów; wyniki CI przepisane, nie uruchamiane ponownie w tym audycie.

## C1.1 — eksport schematu i artefakt CI

- Commity `6c4d2a5` (eksport schematu: `exportSchema = true`, `ksp { arg("room.schemaLocation", ...) }`, `room-testing:2.8.5`), `597d887` (publikacja artefaktu `vireo-room-schemas`), `8d5a0a5` (poprawka ścieżki `app/schemas/**/*.json`).
- Runy CI: #156 SUCCESS (bez publikacji schematu), #157 FAILURE (zła ścieżka artefaktu), #158 SUCCESS (artefakt opublikowany).
- Schemat v14: `app/schemas/app.vireo.data.local.VireoDatabase/14.json`; 14 encji; `identityHash 410131225083a186b43291867168bf6e`.

## C1.2 — infrastruktura testów migracji

- Commity `fecb0df` (konfiguracja `androidTest`, runner, `VireoDatabaseMigrationTest`, job CI `android-test` na emulatorze API 34) i `7d3ab08` (`kotlinx-serialization-json` 1.6.3 → 1.8.1, wymóg `room-migration:2.8.5`).
- Runy CI: #159 FAILURE (`AbstractMethodError` z powodu rozjazdu wersji serializacji), #160 SUCCESS (oba joby; 2/2 testy migracji na emulatorze API 34).
- Zakres testów: tylko wersja 14 (utworzenie bazy, ponowne otwarcie z walidacją `identityHash`). Ścieżki `2→3` … `13→14` nie są pokryte (zadanie E1).

## Historyczny plan `14→15` (źródła per profil) — nierozpoczęty

Dotychczasowa dokumentacja planowała migrację `14→15` dla przypisywania źródeł do profili. Macierz testów z tego planu (zachowana jako materiał dla decyzji P12):

- istniejące profile i ich dane pozostają nienaruszone;
- aktywne konto Xtream przypisane do każdego istniejącego profilu i ustawione jako źródło domyślne; pozostałe konta nieprzypisane;
- brak aktywnego konta: profile bez przypisań; brak profili: migracja bez nieprawidłowych odwołań;
- wiele profili dostaje niezależne przypisania; źródło domyślne zawsze należy do zbioru przypisanych źródeł;
- duplikat przypisania profil/źródło odrzucony; profil nie może mieć więcej niż jednego źródła domyślnego;
- integralność kluczy obcych zachowana; biblioteka, historia, katalog i EPG nienaruszone;
- nieudana migracja nie zostawia częściowo opublikowanych danych.

W nowej roadmapie numer `14→15` zajmuje najpierw migracja addytywna LIVE/EPG (ulubione, magazyn EPG); źródła per profil są decyzją otwartą (P12).
