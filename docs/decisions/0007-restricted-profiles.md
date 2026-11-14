# 0007 — Profil z ograniczeniami

**Status:** Wymagania zatwierdzone. Model danych (allowlista kategorii, pola profilu) czeka na akceptację przed implementacją. Specyfikacja: `../specs/restricted-profiles.md`.

- Nazwa w interfejsie: **„Profil z ograniczeniami”**. Nie używamy nazwy sugerującej gwarancję bezpieczeństwa treści.
- Ograniczenia są egzekwowane w logice danych i biznesowej, a nie tylko przez ukrywanie elementów UI.
- Gdy nie można zagwarantować filtrowania treści IPTV (VIREO nie weryfikuje zawartości dostawcy), UI mówi to jawnie.
