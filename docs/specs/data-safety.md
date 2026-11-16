# Specyfikacja: bezpieczeństwo danych i odzyskiwanie bazy

**Status:** wymagania zatwierdzone (decyzja 0006). Etap E1.

## 1. Zasady

- Nigdy automatyczny reset bazy. Nigdy `fallbackToDestructiveMigration`.
- Przed uruchomieniem migracji — kopia bezpieczeństwa.
- Rozróżniamy cztery klasy: błąd migracji, uszkodzenie bazy, utrata klucza Android Keystore, baza nowsza od aplikacji.

## 2. Kopia przed migracją

- Przy starcie odczyt `PRAGMA user_version`. Jeśli niższa niż docelowa — kopia `vireo.db` (wraz z `-wal` i `-shm`) do katalogu wewnętrznego aplikacji, np. `vireo-v{stara}-{czas}.db`; przechowywane 2 ostatnie.
- Eksport poza urządzenie tylko na życzenie użytkownika (SAF). Kopia zawiera hasła zaszyfrowane kluczem Keystore, więc nie jest przenośna na inne urządzenie (komunikat w UI).
- `allowBackup=false` (zachowane).

## 3. Obsługa klas błędów

| Klasa | Wykrycie | Obsługa |
|---|---|---|
| Błąd migracji | wyjątek przy otwarciu (brak ścieżki, błąd SQL w migracji) | Baza nietknięta (transakcja migracji). Ekran odzyskiwania: spróbuj ponownie, eksportuj kopię, przywróć z kopii przedmigracyjnej. Bez resetu. |
| Uszkodzenie bazy | `SQLiteDatabaseCorruptException`, uszkodzony nagłówek (odczyt `user_version`) lub błąd `PRAGMA quick_check` kopii | Plik zachowany: Room nie usuwa go (`NonDestructiveOpenHelperFactory`, sekcja 8). Ekran odzyskiwania: eksport, przywrócenie z kopii, odsunięcie (nie usunięcie). Odsunięcie wymaga potwierdzenia. |
| Utrata klucza Keystore | baza otwiera się, lecz odszyfrowanie haseł kont zawodzi (typowane wyjątki kryptograficzne) | **Bazy nie resetujemy.** Konta w stanie „wymaga ponownego wpisania hasła” (serwer i login zostają); biblioteka i profile nietknięte. PIN to hasz PBKDF2 niezależny od Keystore (potwierdzić testem). |
| Baza nowsza niż aplikacja | wyjątek przy downgrade | Odmowa uruchomienia z komunikatem; bez resetu. |

## 4. Migracje

- Od decyzji P14 (2026-11-15) minimalną wspieraną wersją jest 14. W `app/schemas` jest tylko `14.json`, więc ścieżki `3→14` nie są testowalne przez `MigrationTestHelper`; testujemy migracje od wersji 14 w górę (pierwsza: `14→15`).
- Każda nowa migracja (`14→15` i dalsze) ma test przed wdrożeniem; migracje addytywne preferowane.
- Test na kopii realnej bazy z urządzenia właściciela (przed zaakceptowaniem E1).

## 5. Integralność profili

- Usunięcie profilu sprząta jego dane (biblioteka, historia, ostatnie wyszukiwania, ustawienia) w jednej transakcji; brak osieroconych wierszy. `library_items` nie ma FK; sprzątanie w warstwie repozytorium (bez przebudowy tabeli), przebudowa tabeli z FK tylko osobną decyzją.
- PIN: limit prób z narastającym opóźnieniem (np. po 5 błędach 30 s, dalej podwajane), licznik trwały, zerowany po sukcesie.

## 6. Testy

Instrumentowane: zepsuta migracja, uszkodzony plik (nagłówek), usunięty wpis Keystore (konta oznaczone, profile i biblioteka nietknięte), baza nowsza od aplikacji. Jednostkowe: polityka limitu prób PIN, klasyfikator błędów otwarcia bazy.

## 7. Kryteria

Wszystkie ścieżki migracji zielone w CI; odzyskiwanie nigdy nie resetuje bazy automatycznie; każda klasa błędu ma udokumentowane i przetestowane zachowanie; brak osieroconych wierszy po usunięciu profilu.

## 8. Ochrona przed automatycznym usunięciem (E1.2c)

Domyślne `SupportSQLiteOpenHelper.Callback.onCorruption` usuwa plik bazy. `NonDestructiveOpenHelperFactory` deleguje całe otwieranie do domyślnej fabryki, ale podmienia `onCorruption` na wpis w logu. Test instrumentowany: (1) uszkodzony plik zostaje na miejscu z tymi samymi bajtami; (2) test kontrolny: domyślna fabryka Room usuwa plik w tym samym scenariuszu; (3) zdrowa baza działa bez zmian; (4) bezpośrednie wywołanie `onCorruption` opakowanego callbacku nie usuwa pliku.

## 9. Pętla awarii i kontrola kopii (E1.2d)

- `StartupCrashGuard`: start jest „oczekujący” do czasu `ON_STOP` lub 15 s pracy. Trzy kolejne niestabilne starty powodują ekran odzyskiwania przy czwartym. „Spróbuj ponownie” zeruje licznik.
- Kopia przedmigracyjna jest sprawdzana `PRAGMA quick_check` po utworzeniu (niepoprawna kopia jest usuwana, a stare kopie nie są przycinane) i przed przywróceniem (wybierana jest najnowsza kopia, która przechodzi kontrolę).
- Ograniczenia: licznik może dać fałszywy alarm po trzech awariach z innych powodów (ekran ma tylko bezpieczne akcje); `quick_check` nie wykrywa uszkodzenia samych danych w poprawnych stronach.
