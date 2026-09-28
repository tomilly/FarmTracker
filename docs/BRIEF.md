# Rolnicy – brief projektu (handoff do Claude Code)

## 1. Problem
Podczas zbioru kukurydzy na kiszonkę sieczkarnia jeździ po wielu polach (działkach), a kierowcy
transportu (ciągniki z przyczepami / ciężarówki) nie wiedzą, **na którym polu aktualnie jest
sieczkarnia** i gdzie mają podjechać. Baza (silos / pryzma) nie wie, kto jedzie i kiedy dojedzie.
Dziś rozwiązuje się to telefonami – co przy hałasie maszyn i słabym zasięgu jest męczące.

## 2. Cel
Prosta aplikacja na Androida, która:
- pokazuje na mapie (Geoportal lub podobne) oznaczone pola,
- pokazuje na żywo, gdzie jest sieczkarnia i gdzie są kierowcy,
- pozwala adminowi dodać ludzi i przydzielić im role.

## 3. Użytkownicy i role
| Rola | Kto to | Czego potrzebuje |
|---|---|---|
| **Admin** | rolnik / właściciel / organizator zbioru | oznacza pola, zaprasza ludzi, przydziela role, ustawia „aktywne pole" |
| **Sieczkarnia** | operator sieczkarni | jednym przyciskiem mówi „jestem na polu X" / „przejeżdżam", widzi, który kierowca nadjeżdża |
| **Kierowca** | kierowca transportu | widzi, gdzie jest sieczkarnia, nawigacja do wjazdu na pole, status „jadę pusty / pełny / rozładunek" |
| **Baza** | osoba na silosie/pryzmie | widzi, kto jedzie z ładunkiem i kiedy mniej więcej będzie |

Jedna osoba może mieć jedną rolę na dany zbiór (admin może też być w innej roli).

## 4. Zasady UX – „Don't Make Me Think" (S. Krug)
Użytkownicy są **mocno nietechniczni**, często w rękawicach, w kabinie, w słońcu, w pyle.
- **Oczywistość > pomysłowość** – każdy ekran ma być zrozumiały bez zastanawiania się.
- **Jeden ekran = jedno główne zadanie.** Po zalogowaniu od razu ekran roli (nie menu).
- **Duże przyciski** (min. ~64 dp), duży tekst, wysoki kontrast, tryb dzienny czytelny w słońcu.
- **Ikona + podpis** zawsze razem, bez samych ikon.
- **Mało tekstu, zero żargonu** („Jadę po ładunek", nie „Zmień status transportu").
- **Konwencje** – mapa jak w Google Maps, zielony = OK/jedź, czerwony = stop/problem.
- **Brak zbędnych kroków** – logowanie numerem telefonu (SMS), zaproszenie linkiem/kodem.
- **Wybaczanie błędów** – cofnięcie ostatniej akcji, potwierdzenie tylko dla nieodwracalnych.
- **Test „trunk test"** – na każdym ekranie wiadomo: gdzie jestem, kim jestem (rola), co mogę zrobić.
- Menu maksymalnie płaskie: 3–4 pozycje w dolnym pasku, reszta u admina w „Ustawieniach".

## 5. Zakres MVP (wersja 1)
1. **Logowanie** numerem telefonu (SMS OTP).
2. **Gospodarstwo / zbiór** – admin tworzy „zbiór" (np. „Kukurydza 2026"), zaprasza osoby kodem/linkiem (SMS/WhatsApp).
3. **Role** – admin przypisuje: sieczkarnia / kierowca / baza.
4. **Pola**:
   - rysowanie wielokąta na mapie palcem, **lub**
   - wyszukanie działki po numerze ewidencyjnym (GUGiK ULDK) i pobranie granicy,
   - nazwa pola (np. „Za lasem"), kolor, opcjonalnie punkt wjazdu (brama).
5. **Mapa na żywo** – pozycje sieczkarni i kierowców (odświeżanie co ~10–30 s), podświetlone aktywne pole.
6. **Automatyczne wykrywanie pola** – geofencing: „Sieczkarnia jest na polu: Za lasem".
7. **Statusy kierowcy** – 3 duże przyciski: *Jadę na pole* / *Ładuję* / *Wiozę do bazy* (+ *Rozładunek*).
8. **Nawigacja** – przycisk „Prowadź do wjazdu" (intent do Google Maps).
9. **Powiadomienia push** – „Sieczkarnia przejechała na pole: Przy drodze".

## 6. Pomysły na później (backlog)
- **Tryb offline** – buforowanie mapy i pozycji, synchronizacja po odzyskaniu zasięgu (na polach bywa słabo).
- **Kolejka przyczep** – kto jest następny pod sieczkarnią; sieczkarnia widzi „następny: Marek, 3 min".
- **ETA** kierowcy do pola / do bazy.
- **Licznik kursów** na kierowcę i na pole; szacunek ton (waga przyczepy × kursy); eksport CSV/PDF – rozliczenie z usługodawcą.
- **Postęp pola** – % skoszonej powierzchni na podstawie śladu sieczkarni.
- **Historia / ślad GPS** dnia – odtworzenie przejazdów.
- **Szybkie komunikaty** (predefiniowane przyciski): „Awaria", „Przerwa", „Potrzebny kierowca", „Pole skończone" + opcjonalnie push-to-talk.
- **Kolejność pól** – admin ustala plan dnia, kierowcy widzą „następne pole".
- **Punkty na mapie** – brama, mostek, mokre miejsce, słup, „tu nie wjeżdżać".
- **Warstwy mapy** – ortofotomapa Geoportalu, granice działek, OSM.
- **Wiele maszyn** – 2 sieczkarnie, kilka baz (silosów).
- **Inne kampanie** – żniwa zbożowe, rzepak, obornik, sianokiszonka (ta sama logika: maszyna + transport + baza).
- **Tryb dla usługodawcy** – firma usługowa obsługuje wielu rolników.
- **Widżet / ekran blokady** i tryb „zawsze włączony ekran" w kabinie.
- **Tryb pogody** – ostrzeżenie o deszczu.

## 7. Propozycja techniczna (do potwierdzenia)
- **Aplikacja:** Kotlin + Jetpack Compose, Material 3, min SDK 26.
- **Mapa:** MapLibre Native (lub osmdroid) z warstwami:
  - Geoportal WMTS/WMS – ortofotomapa (`mapy.geoportal.gov.pl`),
  - KIEG (Krajowa Integracja Ewidencji Gruntów) – granice działek jako WMS,
  - OSM jako mapa bazowa.
- **Działki po numerze:** usługa GUGiK **ULDK** (`uldk.gugik.gov.pl`) – zwraca geometrię działki po identyfikatorze (TERYT + obręb + nr).
- **Backend:** Firebase (Auth – telefon, Firestore – czas rzeczywisty i offline cache, Cloud Messaging – push, Cloud Functions – geofencing/powiadomienia). Alternatywa: Supabase (Postgres + PostGIS + Realtime).
- **Lokalizacja:** Foreground Service + FusedLocationProvider, interwał adaptacyjny (szybciej w ruchu, rzadziej na postoju) – dbałość o baterię.
- **Architektura:** MVVM / UDF, Hilt, Coroutines/Flow, moduły: `app`, `core-ui`, `data`, `feature-map`, `feature-fields`, `feature-team`.
- **Język UI:** polski (strings.xml), przygotowane pod i18n.

## 8. Szkic modelu danych
```
Farm/Harvest { id, name, ownerId, createdAt, active }
Member       { userId, harvestId, displayName, phone, role: ADMIN|HARVESTER|DRIVER|BASE, vehicleName }
Field        { id, harvestId, name, color, polygon: [LatLng], parcelIds: [String], entryPoint: LatLng?, status: PLANNED|ACTIVE|DONE, order }
Location     { userId, harvestId, lat, lng, speed, heading, accuracy, timestamp, currentFieldId? }
DriverStatus { userId, state: TO_FIELD|LOADING|TO_BASE|UNLOADING|IDLE, updatedAt }
Event        { harvestId, type, userId, fieldId?, message?, timestamp }   // log / historia / kursy
Invite       { code, harvestId, role?, expiresAt }
```

## 9. Kamienie milowe
1. **M0** – szkielet projektu, Compose, nawigacja, motyw „duże przyciski", CI (lint, testy).
2. **M1** – mapa z warstwami Geoportalu + OSM, pokazanie własnej pozycji.
3. **M2** – rysowanie/zapis pól lokalnie; import działki z ULDK.
4. **M3** – Firebase Auth (SMS), zbiór, zaproszenia, role.
5. **M4** – udostępnianie lokalizacji na żywo + geofencing „na którym polu".
6. **M5** – statusy kierowcy, push, nawigacja do wjazdu.
7. **M6** – testy w polu, poprawki UX, tryb offline.

## 10. Otwarte pytania
- Firebase czy Supabase? (koszty, RODO, PostGIS)
- Czy kierowcy mają własne telefony z Androidem, czy potrzebna obsługa iOS później?
- Ile osób na jeden zbiór typowo (3? 10? 30?)
- Czy lokalizacja ma być udostępniana tylko w czasie „zbioru aktywnego" (prywatność) – rekomendacja: TAK, z wyraźnym przełącznikiem „Zaczynam pracę / Kończę pracę".
- Jak rozliczamy kursy/tony – czy to potrzebne już w MVP?
- Nazwa aplikacji.
