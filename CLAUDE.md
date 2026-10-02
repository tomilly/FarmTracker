# CLAUDE.md – Rolnicy (Android)

Aplikacja Android dla rolników: koordynacja sieczkarni, kierowców transportu i bazy podczas zbioru kukurydzy.
Pełny opis produktu, zakres MVP, backlog i model danych: **docs/BRIEF.md** – przeczytaj przed pracą.

## Zasady produktu (nie łamać)
- Użytkownicy są nietechniczni → „Don't Make Me Think" (S. Krug): jeden ekran = jedno zadanie,
  duże przyciski (≥64 dp), ikona + podpis, krótkie polskie teksty bez żargonu, płaskie menu.
- Po zalogowaniu użytkownik trafia od razu na ekran swojej roli (Sieczkarnia / Kierowca / Baza / Admin).
- Lokalizacja udostępniana tylko gdy użytkownik kliknie „Zaczynam pracę"; zawsze widoczne, że jest włączona.
- Zakładaj słaby zasięg: UI nie może się blokować przy braku sieci.
- Kierowca i sieczkarnia prowadzą – w czasie pracy nic nie klikają. Bez statusów kursu („Jadę na pole", „Ładuję"…);
  co się da, wykrywa telefon (pole z lokalizacji). Aplikacja ma być super prosta.
- „Kończę pracę" – mały przycisk na dole ekranu, z potwierdzeniem (wyjątek od „cofnij zamiast potwierdzeń").

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
- Paczka na telefon (instalacja z pliku, ~16 MB): `./gradlew assembleRelease -Pfarmtracker.abi=arm64-v8a` →
  `app/build/outputs/apk/release/app-release.apk` (podpis kluczem debug tego komputera; bez „Zmień rolę").

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
M3 gotowe (sprawdzone na 2 emulatorach z prawdziwym Firebase, numery testowe +48 600 000 001 admin / …002 kierowca):
pierwszy ekran: „Mam kod zaproszenia" (konto anonimowe Firebase na tym telefonie, bez SMS-a – kierowcy, sieczkarnia,
baza; potem tylko kod, imię i rola z zaproszenia) albo logowanie numerem (admin; SMS wymaga planu Blaze, na Spark –
numery testowe); `feature-auth`: login, pierwsze wejście – imię, kod zaproszenia / nowy zbiór.
Admin „Ludzie" (`feature-team`): zaproszenie konkretnej osoby – imię, numer → 6 cyfr, działa raz (zużywa się w zapisie
dołączenia), 7 dni, „Wyślij SMS" na ten numer; lista „Czekają na kod" z wycofaniem; rola, usuwanie z „Cofnij". Firebase: `data/.../auth/FirebaseAuthRepository`, `data/.../harvest/FirestoreHarvestRepository`
(`users/{uid}`, `harvests/{id}` + `members`, `fields`, `invites/{kod}`), pola/baza `Firestore*Repository`;
reguły w `firestore.rules` (wkleja się w konsoli – po każdej zmianie pliku przypomnij o publikacji).
Nasłuch Firestore tylko przez `data/.../firebase/Listen.kt` `changes()`, nie `snapshots()` (ten przy odmowie serwera
kończy się CancellationException i ekran po cichu przestaje się odświeżać); dane zbioru z `retryWhenNotYetMember()`. Tryb zależy od `app/google-services.json` (nie w repo – repo
publiczne): jest → `BuildConfig.SHARED_HARVEST` i wspólny zbiór; brak → jak przed M3 (wybór roli, dane lokalne).
Wybór w `app/.../di/StorageModule`. W debug „Zmień rolę" przykrywa rolę ze zbioru tylko na tym telefonie.
`-Pfarmtracker.localOnly` buduje wersję lokalną mimo pliku (próby na emulatorze bez logowania); `-Pfarmtracker.demo` – lokalna
z udawaną sieczkarnią „Rysiek" (`app/.../demo`). Admin → „Ustawienia" (`feature-auth/settings`): konto i „Wyloguj się"
(kończy pracę, zdejmuje pozycję, potem wylogowuje).
M4 w toku: „Zaczynam pracę" (sieczkarnia, kierowca) pyta o zgodę na lokalizację i powiadomienie, ustawia
`WorkRepository.isWorking`; `MainActivity` (gdy na wierzchu) włącza `feature-work/WorkService` (usługa pierwszoplanowa
„location", stałe powiadomienie z polem i „Kończę pracę" – otwiera aplikację z pytaniem „Skończyć pracę?" (`AppViewModel`); kończy się sama po `isWorking = false`).
`LocationPublisher`: GPS → pole (`fieldWith`, 30 m zapasu przy wyjeździe) → wysyłka tylko gdy trzeba
(`PositionReport.needsUpdate`). Pozycje: `LiveLocationRepository` – Firestore `harvests/{id}/locations/{uid}`
albo lokalnie (tylko własna). Ekrany ról: „Jesteś na polu: …", gdzie sieczkarnia/kierowcy (`common/CrewWatch`);
mapa: kropki innych w kolorze roli, szare po 3 min, znikają po 2 h.
Kierowca i sieczkarnia w pracy widzą mapę na cały ekran (`common/WorkMap`): jedzie za nimi i oddala się tyle, by
było widać sieczkarnię / nadjeżdżające przyczepy (`MapOverlays.keepInView`); pole z sieczkarnią podświetlone na mapach
(`activeFieldIds`). Status kierowcy bez klikania: `core-domain/Trip.kt`
(`TripTracker`: przy sieczkarni 2 pozycje ≤ 40 m → ładuje; przy bazie ≤ 150 m → w bazie (przed polem); zjechał z pola →
wraca do bazy; odjechał z bazy → jedzie na pole; w drodze kierunek względem bazy po 300 m), liczony w `LocationPublisher`,
wysyłany w `PositionReport.trip`. „Stoi od N min" (≥ 5 min w promieniu 50 m): `core-domain/Still.kt` → `stillSince`.
Admin: na górze podgląd pracy (`admin/AdminViewModel`: kto gdzie i co robi, kto stoi, kto nie pracuje), „Mapa"
startuje od wszystkich pól i bazy. Rolę i ludzi zbioru ekrany biorą z `data/.../session/PeopleRepository`
(wspólny zbiór albo telefon), nie z `SessionRepository` – ta ma tylko rolę wybraną na telefonie.
Kamienie milowe: docs/BRIEF.md §9.

## Emulator (uwagi)
- Obraz API 37 Google Play: GPS emulatora bywa „martwy" (0 pozycji) – zimny start (`-no-snapshot-load`)
  i ustawienie lokalizacji w Extended controls → Location. `adb emu geo fix` bywa ignorowane.
