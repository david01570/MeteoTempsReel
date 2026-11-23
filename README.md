# Météo Temps Réel

Application Android de météo en temps réel avec géolocalisation.

## Fonctionnalités

- 📍 Géolocalisation automatique de la position
- 🌡️ Température actuelle, ressentie, min/max du jour
- 💧 Humidité, 🌧️ précipitations, 💨 vent
- ☀️ Condition météo avec description en français (codes WMO)
- 🔄 Actualisation automatique toutes les 5 minutes + bouton manuel
- 🎨 Interface Jetpack Compose moderne

## Technique

- Kotlin 2.0 + Jetpack Compose (Material 3)
- API [Open-Meteo](https://open-meteo.com/) (gratuite, sans clé)
- Localisation via `LocationManager` natif (aucun service Google requis)
- `minSdk` 26 (Android 8.0+), `targetSdk` 34 (Android 14)

## Compiler

```bash
./gradlew assembleDebug
```

L'APK est généré dans `app/build/outputs/apk/debug/app-debug.apk`.

## Télécharger l'APK

Un APK prêt à installer est disponible dans les [releases](../../releases).
