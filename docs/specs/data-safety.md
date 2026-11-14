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
| Uszkodzenie bazy | `SQLiteDatabaseCorruptException` | Plik zachowany (zmiana nazwy, nie usunięcie). Opcje: eksport, przywrócenie z kopii. Reset dopiero po jawnym potwierdzeniu użytkownika i po zapisaniu kopii. |
| Utrata klucza Keystore | baza otwiera się, lecz odszyfrowanie haseł kont zawodzi (typowane wyjątki kryptograficzne) | **Bazy nie resetujemy.** Konta w stanie „wymaga ponownego wpisania hasła” (serwer i login zostają); biblioteka i profile nietknięte. PIN to hasz PBKDF2 niezależny od Keystore (potwierdzić testem). |
| Baza nowsza niż aplikacja | wyjątek przy downgrade | Odmowa uruchomienia z komunikatem; bez resetu. |

## 4. Migracje

- Testy wszystkich ścieżek `3→14` (`MigrationTestHelper`, schematy w `app/schemas`); decyzja co do `1→2` (P14).
- Każda nowa migracja (`14→15` i dalsze) ma test przed wdrożeniem; migracje addytywne preferowane.
- Test na kopii realnej bazy z urządzenia właściciela (przed zaakceptowaniem E1).

## 5. Integralność profili

- Usunięcie profilu sprząta jego dane (biblioteka, historia, ostatnie wyszukiwania, ustawienia) w jednej transakcji; brak osieroconych wierszy. `library_items` nie ma FK; sprzątanie w warstwie repozytorium (bez przebudowy tabeli), przebudowa tabeli z FK tylko osobną decyzją.
- PIN: limit prób z narastającym opóźnieniem (np. po 5 błędach 30 s, dalej podwajane), licznik trwały, zerowany po sukcesie.

## 6. Testy

Instrumentowane: zepsuta migracja, uszkodzony plik (nagłówek), usunięty wpis Keystore (konta oznaczone, profile i biblioteka nietknięte), baza nowsza od aplikacji. Jednostkowe: polityka limitu prób PIN, klasyfikator błędów otwarcia bazy.

## 7. Kryteria

Wszystkie ścieżki migracji zielone w CI; odzyskiwanie nigdy nie resetuje bazy automatycznie; każda klasa błędu ma udokumentowane i przetestowane zachowanie; brak osieroconych wierszy po usunięciu profilu.
