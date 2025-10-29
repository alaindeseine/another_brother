# Upgrade Brother SDK 4.13.0 - Résumé des Modifications

**Date** : 29 octobre 2025
**Branche** : `fix-upgrade-brother-sdk-local-413-16ko`
**Objectif** : Résoudre le problème d'alignement 16KB pour Google Play Store Android 15+

---

## ✅ MODIFICATIONS EFFECTUÉES

### 1. Brother SDK 4.12.0 → 4.13.0

**Android** :
- ✅ Fichier : `android/libs/BrotherPrintLibrary-4.13.0.aar` (4.9M)
- ✅ Supprimé : `android/libs/BrotherPrintLibrary-4.12.0.aar` (4.0M)
- ✅ Configuration : `android/build.gradle` ligne 62

**iOS** :
- ✅ Version : `BRLMPrinterKit ~> 4.13.0`
- ✅ Configuration : `ios/another_brother.podspec` ligne 29

**Vérification alignement** :
```
✅ armeabi-v7a/libcreatedata.so: 16KB (0x04000)
✅ arm64-v8a/libcreatedata.so: 16KB (0x04000)
✅ x86/libcreatedata.so: 16KB (0x04000)
✅ x86_64/libcreatedata.so: 16KB (0x04000)
```

### 2. Android Gradle Plugin 8.5.0 → 8.5.2

- ✅ Fichier : `android/build.gradle` ligne 13
- ✅ Amélioration : Support NDK et stabilité de build
- ✅ Impact : Faible, corrections de bugs

### 3. flutter_blue_plus 1.12.13 → 1.36.8

- ✅ Fichier : `pubspec.yaml` ligne 15
- ✅ Version installée : 1.36.8 (plus récente que 1.35.10 demandée)
- ✅ Amélioration : Bluetooth Android 14+, stabilité BLE

### 4. Documentation

- ✅ `CHANGELOG.md` : Nouvelle entrée `0.0.1+upgrade-sdk-4.13.0-16kb`
- ✅ Documentation complète des changements

---

## 📦 RÉSUMÉ DES FICHIERS MODIFIÉS

```
M  CHANGELOG.md
M  android/build.gradle
D  android/libs/BrotherPrintLibrary-4.12.0.aar
A  android/libs/BrotherPrintLibrary-4.13.0.aar
M  ios/another_brother.podspec
M  pubspec.yaml
M  pubspec.lock (auto-généré)
```

---

## 🧪 PLAN DE TESTS OBLIGATOIRES

### Phase 1 : Tests de Non-Régression (CRITIQUE)

#### Test 1.1 : Impression WiFi ⚠️ PRIORITÉ MAX
**Objectif** : Vérifier que le bug `WifiConnection.closeConnection()` ne se reproduit pas

**Procédure** :
1. Connecter une imprimante Brother via WiFi
2. Lancer une impression (image ou PDF)
3. **Observer attentivement** la fermeture de connexion
4. Répéter **5 fois** pour confirmer la stabilité
5. Tester une déconnexion/reconnexion WiFi

**Résultat attendu** :
- ✅ Pas de `NullPointerException`
- ✅ Impression réussie à chaque fois
- ✅ Connexion/déconnexion propre

**Si échec** : Noter le message d'erreur exact et revenir sur le code pour implémenter le fix.

---

#### Test 1.2 : Impression Bluetooth Classic
**Procédure** :
1. Connecter imprimante via Bluetooth Classic
2. Imprimer 3 documents différents
3. Vérifier la stabilité de la connexion

**Résultat attendu** : ✅ Impression réussie, pas de crash

---

#### Test 1.3 : Impression Bluetooth BLE
**Procédure** :
1. Scanner et connecter via BLE
2. Tester impression avec `flutter_blue_plus` 1.36.8
3. Vérifier amélioration de la stabilité

**Résultat attendu** : ✅ Scan plus rapide, connexion plus stable

---

#### Test 1.4 : Impression USB (Android uniquement)
**Procédure** :
1. Connecter imprimante USB
2. Vérifier détection automatique
3. Imprimer un document

**Résultat attendu** : ✅ Détection et impression OK

---

### Phase 2 : Tests Spécifiques TD-2350D

#### Test 2.1 : Impression TD-2350D
**Objectif** : Confirmer le support complet TD-2350D avec SDK 4.13.0

**Procédure** :
1. Connecter une TD-2350D (WiFi, BT ou USB)
2. Utiliser TypeB commands :
   - `typeB-setup`
   - `typeB-barcode`
   - `typeB-printerFont`
   - `typeB-printLabel`
3. Imprimer 5 étiquettes différentes

**Résultat attendu** : ✅ Support complet maintenu

---

### Phase 3 : Tests de Stress

#### Test 3.1 : Impression en Boucle
**Procédure** :
1. Imprimer 50 étiquettes consécutives via WiFi
2. Observer la stabilité de la connexion
3. Vérifier absence de fuites mémoire

**Résultat attendu** : ✅ Pas de crash, pas de ralentissement

---

#### Test 3.2 : Polling Status
**Procédure** :
1. Appeler `getPrinterStatus()` toutes les 30 secondes
2. Laisser tourner pendant 5 minutes
3. Observer absence de crash SIGABRT

**Résultat attendu** : ✅ Pas de crash (protection timeout en place)

---

### Phase 4 : Build & Vérification Finale

#### Test 4.1 : Build Release
**Commandes** :
```bash
flutter clean
flutter pub get
flutter build appbundle --release \
  --target-platform=android-arm,android-arm64 \
  --obfuscate --split-debug-info=build/app/outputs/symbols
```

**Résultat attendu** : ✅ Build réussi sans erreur

---

#### Test 4.2 : Vérification Alignement 16KB
**Commande** :
```bash
unzip -l build/app/outputs/bundle/release/app-release.aab | grep libcreatedata.so
python3 check_alignment.py <chemin_vers_libcreatedata.so>
```

**Résultat attendu** :
```
✅ armeabi-v7a/libcreatedata.so: 16KB (0x04000)
✅ arm64-v8a/libcreatedata.so: 16KB (0x04000)
```

---

#### Test 4.3 : Google Play Console
**Procédure** :
1. Uploader l'AAB sur Play Console (track interne)
2. Vérifier absence du warning 16KB
3. Tester l'installation sur un appareil Android 15

**Résultat attendu** :
- ✅ Pas d'avertissement 16KB
- ✅ Installation réussie sur Android 15
- ✅ Lancement sans crash

---

## 🎯 CHECKLIST DE VALIDATION

Avant de merger dans `main` :

- [ ] **Test 1.1** : WiFi impression × 5 (CRITIQUE)
- [ ] **Test 1.2** : Bluetooth Classic
- [ ] **Test 1.3** : BLE avec flutter_blue_plus 1.36.8
- [ ] **Test 1.4** : USB (si disponible)
- [ ] **Test 2.1** : TD-2350D support complet
- [ ] **Test 3.1** : 50 impressions consécutives
- [ ] **Test 3.2** : Polling status 5 min
- [ ] **Test 4.1** : Build release OK
- [ ] **Test 4.2** : Vérification alignement 16KB
- [ ] **Test 4.3** : Upload Play Console test

---

## 🚨 POINTS DE VIGILANCE

### Bug WiFi Potentiel

**Symptôme précédent** (SDK 4.13.0 initial) :
```
NullPointerException in WifiConnection.closeConnection()
Crash during: printPdfFile() → printer.finish() → closeConnection()
```

**Action si bug reproduit** :
1. Noter le stack trace complet
2. Identifier le fichier source exact
3. Implémenter try-catch avec vérification nullité
4. Ajouter logs de debug
5. Retester

**Fichiers probables à modifier** :
- `android/src/main/kotlin/com/rouninlabs/another_brother/method/EndCommunicationMethodCall.kt`
- `android/src/main/kotlin/com/rouninlabs/another_brother/method/PrintPdfFileMethodCall.kt`
- Tout fichier appelant `printer.finish()`

---

## 📊 BÉNÉFICES ATTENDUS

### 1. Conformité Google Play
- ✅ Compatible Android 15+ (16KB page size)
- ✅ Respect deadline 1er novembre 2025 (nouvelles soumissions)
- ✅ Respect deadline 1er mai 2026 (toutes mises à jour)

### 2. Améliorations Techniques
- ✅ SDK Brother plus récent (bug fixes)
- ✅ Bluetooth plus stable (flutter_blue_plus 1.36.8)
- ✅ Build tools à jour (AGP 8.5.2)

### 3. Performances
- ✅ Lancement 30% plus rapide (Android 15+)
- ✅ Batterie -5% au démarrage (Android 15+)

---

## 📝 COMMANDES UTILES

### Vérifier version SDK
```bash
ls -lh android/libs/
# Doit afficher: BrotherPrintLibrary-4.13.0.aar (4.9M)
```

### Vérifier alignement
```bash
python3 check_alignment.py android/libs/BrotherPrintLibrary-4.13.0.aar
```

### Rebuild complet
```bash
flutter clean
flutter pub get
cd android && ./gradlew clean
cd .. && flutter build apk --release
```

### Git status
```bash
git status
git diff android/build.gradle
git diff ios/another_brother.podspec
git diff pubspec.yaml
```

---

## 🔄 ROLLBACK SI NÉCESSAIRE

Si les tests échouent et qu'un rollback est nécessaire :

```bash
# Annuler toutes les modifications
git checkout .
git clean -fd

# Ou revenir au commit précédent
git reset --hard HEAD~1

# Reconstruire
flutter clean && flutter pub get
```

---

## 📞 SUPPORT

**En cas de problème** :
1. Consulter `REPORT_16KB_ANALYSIS.md` pour contexte complet
2. Vérifier logs Android Studio / Xcode
3. Tester sur plusieurs modèles d'imprimantes
4. Contacter Brother Developer Support si bug SDK

---

**Auteur** : Claude (Anthropic)
**Date** : 29 octobre 2025
**Statut** : ✅ Modifications appliquées, en attente de tests
