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
- Wszystkie teksty UI w `strings.xml` (PL).
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
Następny: M2 – pola (rysowanie, ULDK). Kamienie milowe: docs/BRIEF.md §9.

## Emulator (uwagi)
- Obraz API 37 Google Play: GPS emulatora bywa „martwy" (0 pozycji) – zimny start (`-no-snapshot-load`)
  i ustawienie lokalizacji w Extended controls → Location. `adb emu geo fix` bywa ignorowane.
