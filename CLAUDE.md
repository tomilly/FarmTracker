# CLAUDE.md – Rolnicy (Android)

Aplikacja Android dla rolników: koordynacja sieczkarni, kierowców transportu i bazy podczas zbioru kukurydzy.
Pełny opis produktu, zakres MVP, backlog i model danych: **docs/BRIEF.md** – przeczytaj przed pracą.

## Zasady produktu (nie łamać)
- Użytkownicy są nietechniczni → „Don't Make Me Think" (S. Krug): jeden ekran = jedno zadanie,
  duże przyciski (≥64 dp), ikona + podpis, krótkie polskie teksty bez żargonu, płaskie menu.
- Po zalogowaniu użytkownik trafia od razu na ekran swojej roli (Sieczkarnia / Kierowca / Baza / Admin).
- Lokalizacja udostępniana tylko gdy użytkownik kliknie „Zaczynam pracę"; zawsze widoczne, że jest włączona.
- Zakładaj słaby zasięg: UI nie może się blokować przy braku sieci.

## Stack
- Kotlin, Jetpack Compose, Material 3, min SDK 26, Gradle KTS + version catalog (`libs.versions.toml`).
- MVVM/UDF, Hilt, Coroutines/Flow.
- Mapa: MapLibre Native; warstwy Geoportal (WMTS orto), KIEG (WMS działki), OSM.
- Działki po numerze: GUGiK ULDK.
- Backend: Firebase (Auth phone, Firestore, FCM, Functions) – potwierdzone; region UE (`europe-central2`).
  Geofencing liczony na telefonie (brak PostGIS). Skala: do ~10 osób na zbiór.
- Tylko Android, ale „KMP-ready": modele i reguły domenowe w `core-domain` (czysty Kotlin/JVM, bez Androida i Hilt).
- Nazwa: FarmTracker, `applicationId = pl.farmtracker.app`, pakiety `pl.farmtracker.*`.

## Konwencje
- Wszystkie teksty UI w `strings.xml` (PL). Liczebniki przez `pluralStringPl` (core-ui), nie `plurals` –
  Android dobiera formę `plurals` wg języka telefonu („4 rogu" na telefonie po angielsku).
- Moduły: `app`, `core-domain` (JVM), `core-ui` (motyw + komponenty), `core-map` (MapLibre: `MapScaffold`,
  `MapChromeController`, warstwy), `core-testing` (fake'i, reguły testowe),
  `data`, `feature-*`. Konfiguracja buildów w pluginach konwencji `build-logic/` (`farmtracker.android.library`,
  `farmtracker.android.compose`, `farmtracker.hilt`, `farmtracker.jvm.library`, …).
- UI buduj z komponentów `core-ui`: `BigActionButton`, `RoleScaffold`/`FarmTrackerScaffold`, `StatusPill`, `Tone`.
- Każda nowa funkcja: ViewModel + testy jednostkowe; komponenty UI z `@Preview`.
- Małe commity, opisowe wiadomości (EN lub PL, spójnie).

## Komendy
- Build: `./gradlew assembleDebug`
- Testy: `./gradlew test`
- Lint: `./gradlew lint`

## Stan
M0 gotowe (szkielet, motyw, nawigacja z 4 ekranami ról, CI). Wybór roli jest tymczasowy (do M3).
M1 gotowe (`feature-map`): mapa bazowa OpenFreeMap (polskie nazwy), ortofotomapa Geoportalu (WMTS 3857),
granice działek KIEG (WMS w 3× rozdzielczości, od zoomu 13,5; na zdjęciu białe), własna pozycja,
zaznaczanie działki dotknięciem (ULDK GetParcelByXY → nakładka + numer, obręb, ha).
Adresy usług: `feature-map/.../MapSources.kt`, ULDK: `data/.../parcel/UldkParcelRepository.kt`.
Geometria (WKT, powierzchnia) w `core-domain/.../geo`.
M2 gotowe (`feature-fields`, zapis lokalny w DataStore JSON – `data/.../field`): lista pól (admin → „Pola"),
nowe pole z działek (dotyk / wieś „sulmow" przez Nominatim OSM / numer „Otusz 125" przez ULDK) albo narysowane
po rogach → nazwa, kolor, wjazdy (kilka; dotknięcie wjazdu usuwa) → zapis; działka może należeć tylko do jednego pola;
dotknięcie pola na liście → pole na mapie (`FieldScreen`, dotknięcie innego pola przełącza) → „Edytuj pole";
na głównej mapie dotknięcie pola pokazuje kartę pola (admin: „Edytuj pole"); usuwanie z „Cofnij" (`DeletedFieldBin`);
pola i wjazdy widoczne na mapie każdej roli. Baza (silos/pryzma): admin → „Baza" (`feature-fields/.../base`,
zapis `data/.../base`), czarna kropka „Baza" na mapach.
M3 w toku: logowanie numerem (`feature-auth`: login, pierwsze wejście – imię, kod zaproszenia / nowy zbiór),
admin „Ludzie" (`feature-team`: zaproszenia na rolę – 6 cyfr, 7 dni, wysyłka przez udostępnianie; rola, usuwanie
z „Cofnij"). Firebase: `data/.../auth/FirebaseAuthRepository`, `data/.../harvest/FirestoreHarvestRepository`
(`users/{uid}`, `harvests/{id}` + `members`, `fields`, `invites/{kod}`), pola/baza `Firestore*Repository`;
reguły w `firestore.rules` (wkleja się w konsoli). Tryb zależy od `app/google-services.json` (nie w repo – repo
publiczne): jest → `BuildConfig.SHARED_HARVEST` i wspólny zbiór; brak → jak przed M3 (wybór roli, dane lokalne).
Wybór w `app/.../di/StorageModule`. W debug „Zmień rolę" przykrywa rolę ze zbioru tylko na tym telefonie.
`-Pfarmtracker.localOnly` buduje wersję lokalną mimo pliku (próby na emulatorze bez logowania).
M4 w toku: „Zaczynam pracę" (sieczkarnia, kierowca) pyta o zgodę na lokalizację i powiadomienie, ustawia
`WorkRepository.isWorking`; `MainActivity` (gdy na wierzchu) włącza `feature-work/WorkService` (usługa pierwszoplanowa
„location", stałe powiadomienie z polem i „Kończę pracę"; kończy się sama po `isWorking = false`).
`LocationPublisher`: GPS → pole (`fieldWith`, 30 m zapasu przy wyjeździe) → wysyłka tylko gdy trzeba
(`PositionReport.needsUpdate`). Pozycje: `LiveLocationRepository` – Firestore `harvests/{id}/locations/{uid}`
albo lokalnie (tylko własna). Ekrany ról: „Jesteś na polu: …", gdzie sieczkarnia/kierowcy (`common/CrewWatch`);
mapa: kropki innych w kolorze roli, szare po 3 min, znikają po 12 h.
Kamienie milowe: docs/BRIEF.md §9.

## Emulator (uwagi)
- Obraz API 37 Google Play: GPS emulatora bywa „martwy" (0 pozycji) – zimny start (`-no-snapshot-load`)
  i ustawienie lokalizacji w Extended controls → Location. `adb emu geo fix` bywa ignorowane.
