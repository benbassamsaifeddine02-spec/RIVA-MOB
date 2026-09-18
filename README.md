# RIVA-MOB

RIVA Industries mobile application for creating and managing industrial work instructions (modes opératoires).

## Android MVP
Expo / React Native, offline-first.

### Features
- Home / Documents / Settings
- New work-instruction wizard
- Complexe / Zone-Machine / Serial Number
- Automatic code: `Complexe-ZoneIndex-SerialNumber`
- Version / status
- Safety
- 5M tables
- Materials: Code d'article / Pièce / Quantité / Code magasin
- Steps: title / action / duration / photo
- Local persistence with AsyncStorage
- PDF generation and Android sharing

## Local development

```bash
npm install
npx expo start
```

## Android APK

A GitHub Actions workflow is included at `.github/workflows/build-android.yml`.
It generates an installable APK as a workflow artifact.
