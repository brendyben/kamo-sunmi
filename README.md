# KAMO Sunmi — Terrasse Kanisa Moninga

APK Android pour Sunmi V2s — Ticketing buvette 100% offline.

## Build via GitHub Actions

1. Crée le repo `brendyben/kamo-sunmi` sur GitHub  
2. Push tous ces fichiers sur la branche `main`  
3. GitHub Actions lance automatiquement le build  
4. Onglet **Actions** → run en cours → `KAMO-debug` → **Download artifact**  
5. Tu obtiens `app-debug.apk` → transfère via câble USB sur le V2s → installe

## Installer sur Sunmi V2s

```
adb install app-debug.apk
```
ou via le gestionnaire de fichiers Android (sideloading).

## Fonctionnalités

- 9 boissons : Nkoy, Heineken (G/P), Beaufort, Mutzig (G/P), Turbo King, Tembo, Coca, Fanta, Sprite
- Double impression thermique 58 mm (×2 tickets automatiquement)
- Nom du serveur au démarrage
- Dashboard journée + export CSV
- 100% hors ligne — localStorage uniquement
