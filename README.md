# RestRedder 

Android-app die voedseloverschotten redt: handelaars bieden verrassingspakketten aan, klanten reserveren ze aan een lage prijs en halen ze op binnen een vast tijdsvenster.

Projectopdracht **Mobile Development** · Toegepaste Informatica · 2026-2027
-
##- Functionaliteit

**Account (beide rollen):** registreren met rolkeuze (klant/handelaar), in- en uitloggen, duidelijke foutmeldingen, wachtwoord vergeten.

**Klant**
- Pakketten in de buurt van de koper zijn gekozen gemeente, als lijst (gesorteerd op afstand) en op een OSMDroid-kaart
- Realtime updates, filter op afstand en categorie
- Pakketdetail, reserveren, ophaalcode van 6 tekens, reservatie annuleren
- Bestellingen (ook offline) en favoriete winkels via Room

**Handelaar**
- Winkel aanmaken met pin op de kaart; adres wordt automatisch ingevuld
- Pakketten aanbieden met foto (Base64), aanpassen of offline halen
- Realtime overzicht van pakketten en reservaties, reservatie als opgehaald markeren

## Tech stack

Kotlin · Jetpack Compose · Navigation Compose · MVVM (`StateFlow<UiState>`) · Coroutines · Firebase Auth + Cloud Firestore · Room · Retrofit (Nominatim) · OSMDroid

## Architectuur

`Composable → ViewModel (StateFlow) → Repository → Firestore / Room / Retrofit`

- Composables bevatten geen Firebase-, Room- of Retrofit-code
- Firestore-lijsten worden realtime gelezen met `snapshots()`
- Bestellingen lezen enkel uit Room, zodat ze offline werken
- Teksten staan in `strings.xml`

## Datamodel

**Firestore:** `users`, `stores`, `bags`, `reservations`
**Room:** `FavoriteStore`, `CachedReservation`, `CachedAddress`

## Installatie

1. Kloon de repo: `git clone [repository-url]`
2. Maak een Firebase-project (Spark-plan) met **Authentication** (e-mail/wachtwoord) en **Cloud Firestore** in productiemodus
3. Registreer de app met package name `[com.example.restredder]` en plaats `google-services.json` in `app/` (staat niet in Git)
TODO:
4. Stel de security rules in:

```
rules_version = '2';
service cloud.firestore {
  match /databases/restredderdb/documents {
    match /users/{uid} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == uid;
    }
    match /{collection}/{doc} {
      allow read, write: if request.auth != null && collection in ['stores', 'bags', 'reservations'];
    }
  }
}
```

5. Open in Android Studio en start op een toestel of emulator (stel op de emulator een locatie in)

## Technische keuzes

- **Reverse geocoding:** Nominatim `/reverse` (zoom 18 voor adres, zoom 10 voor gemeente), eigen User-Agent `RestRedder/[versie] ([contactadres])`, max. 1 aanvraag per seconde, resultaten gecachet in Room
- **Foto's:** verkleind (max. 800 px), JPEG-kwaliteit 70, als Base64 in `photoBase64` (Firestore-limiet 1 MiB), verwerkt op `Dispatchers.Default`
- **Permissies:** locatie (FINE + COARSE) wordt pas gevraagd na uitleg en een actie; bij weigering kiest de gebruiker een locatie met een pin

Dit is een schoolproject en geen officiële Too Good To Go-app.
