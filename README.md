# Keto365 (Android)

App Android sencilla que:

- Muestra **1 receta keto por cada día del año** (día 1..365/366).
- Tiene **login con Google** (Firebase Auth).
- Guarda el **correo del usuario** en **Room (SQLite local)** para usarlo después.
- El login está pensado para ejecutarse **una sola vez** (bandera persistida con DataStore).

## Requisitos

- Android Studio Iguana o superior
- JDK 17
- Cuenta de Firebase

## Configuración rápida

1. Crea un proyecto en Firebase y registra la app Android con package:
   `com.healthguidelab.keto365`.
2. Descarga `google-services.json` y cópialo en `app/google-services.json`.
3. En `app/src/main/res/values/strings.xml`, reemplaza:
   `REEMPLAZA_CON_WEB_CLIENT_ID_DE_FIREBASE`
   por el valor real de `default_web_client_id` que te da Firebase.
4. Sincroniza Gradle y ejecuta la app.

## Cómo funciona el login de una sola vez

- Al iniciar, se revisa una bandera `has_logged_once` en DataStore.
- También se revisa si existe email en Room.
- Si ambos existen, salta directo al Home.
- Si no, pide login con Google.
- Tras login exitoso, guarda el email en Room y marca la bandera.

## Estructura principal

- `MainActivity.kt`: flujo UI + login + guardado de email.
- `data/AppDatabase.kt`: base Room.
- `data/UserEmailDao.kt` y `data/UserEmailEntity.kt`: tabla para email.
- `data/SessionStore.kt`: bandera de primer login.
- `data/KetoRecipes.kt`: receta diaria por día del año.
