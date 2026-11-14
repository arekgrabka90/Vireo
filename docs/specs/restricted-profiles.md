# Specyfikacja: profil z ograniczeniami

**Status:** wymagania zatwierdzone (decyzja 0007); model danych czeka na akceptację (P13). Etap E6.

## 1. Stan obecny [K]

Flaga profilu dziecięcego jest zapisywana i przekazywana do fabryki ustawień, która jej nie używa; brak filtrowania treści. PIN chroni tylko wybór profilu (bez limitu prób). Ustawienia i źródła nie mają dodatkowej bramki.

## 2. Nazwa i granice

W interfejsie: **„Profil z ograniczeniami”**. VIREO nie weryfikuje treści dostawcy IPTV; ograniczenia opierają się na kategoriach wybranych przez właściciela oraz klasyfikacji TMDB i **nie gwarantują**, że treść jest odpowiednia dla dzieci. Komunikat jest widoczny przy włączaniu profilu.

## 3. Egzekwowanie w logice (nie tylko UI)

- **Polityka profilu** (obiekt domenowy) rozstrzygana w repozytoriach i przypadkach użycia, nie w widokach.
- **Model uprawnień (rekomendacja):** allowlista kategorii wybierana przez właściciela; domyślnie profil z ograniczeniami nie widzi kategorii dostawcy. Bezpieczniejsze niż lista zakazanych słów.
- **Warstwa danych:** repozytorium katalogu filtruje kategorie i tytuły po dozwolonych kategoriach; zapytania TMDB dostają ograniczenia po stronie zapytania (certyfikacja, gatunki, `include_adult=false`); wyniki wyszukiwania filtrowane po fladze dla dorosłych i gatunkach.
- **Bramka odtwarzania:** rozwiązanie źródła i start sesji sprawdzają politykę; „Kontynuuj”, historia i głębokie linki nie obchodzą jej.
- **Operacje właścicielskie** (ustawienia, źródła, profile): wymóg PIN-u właściciela egzekwowany w ViewModelu i repozytorium (nie samo ukrycie przycisku).
- **Biblioteka i historia:** zapytania filtrowane dla profilu z ograniczeniami.
- **Panel kategorii LIVE:** tylko dozwolone kategorie (filtr w warstwie danych).

## 4. Dane (propozycja, do akceptacji)

Pole ograniczeń w profilu i tabela dozwolonych kategorii `(profileId, sourceId, contentType, categoryId)`; migracja addytywna z testem. Istniejąca flaga profilu dziecięcego zostaje źródłem prawdy lub jest zastąpiona — decyzja przy akceptacji modelu.

## 5. Testy

Jednostkowe: polityka. Repozytoria: filtry. **Testy obejść:** bezpośrednie wywołania repozytoriów i ViewModeli, „Kontynuuj”, historia, głęboki link do odtwarzania, próba wejścia w ustawienia bez PIN-u właściciela.

## 6. Kryteria

Testy polityki i obejść zielone; operacje właścicielskie wymagają PIN-u na poziomie ViewModelu i repozytorium; granice opisane w UI.
