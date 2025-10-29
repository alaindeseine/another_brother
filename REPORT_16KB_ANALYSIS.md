# Rapport d'Analyse - Problème 16KB Page Size Google Play Store

**Date** : 29 octobre 2025
**Projet** : KalyApp Print Service (print_hub_pro)
**Version** : 4.0.16+4016
**Problème** : Rejet Google Play Store - Bibliothèques natives non alignées à 16KB

---

## 📋 TABLE DES MATIÈRES

1. [Contexte du Problème](#1-contexte-du-problème)
2. [Diagnostic Initial](#2-diagnostic-initial)
3. [Modifications Apportées au Projet Principal](#3-modifications-apportées-au-projet-principal)
4. [Résultats des Tests](#4-résultats-des-tests)
5. [Analyse du Package Officiel another_brother v2.2.4](#5-analyse-du-package-officiel-another_brother-v224)
6. [Analyse Comparative : Officiel vs Fork](#6-analyse-comparative--officiel-vs-fork)
7. [Pourquoi les Bibliothèques Natives Restent à 4KB](#7-pourquoi-les-bibliothèques-natives-restent-à-4kb)
8. [Solutions Possibles pour le Fork](#8-solutions-possibles-pour-le-fork)
9. [Commandes de Vérification](#9-commandes-de-vérification)
10. [Prochaines Étapes](#10-prochaines-étapes)

---

## 1. CONTEXTE DU PROBLÈME

### Message d'Erreur Google Play Store

```
Recompilez votre appli avec un alignement des bibliothèques natives pour 16 ko

Votre appli utilise des bibliothèques natives qui ne sont pas alignées pour prendre
en charge les appareils avec des tailles de page de mémoire de 16 ko. Il est possible
que votre appli ne puisse pas être installée ou lancée sur ces appareils, ou qu'elle
plante après avoir démarré.

Cette version comporte de nouveaux app bundles qui ne sont pas compatibles avec les
tailles de page de mémoire de 16 ko.

Codes de version : 4016
```

### Exigence Google Play

- **Date limite** : 1er novembre 2025 (nouvelles soumissions)
- **Date limite stricte** : 1er mai 2026 (toutes les mises à jour)
- **Plateforme** : Android 15+ (API 35), appareils 64-bit
- **Bénéfices** : Lancement 30% plus rapide, batterie -5% au démarrage

---

## 2. DIAGNOSTIC INITIAL

### Configuration Projet Avant Modifications

| Composant | Version | Statut |
|-----------|---------|--------|
| Flutter SDK | 3.35.6 | ✅ Compatible |
| Gradle | 8.10.2 | ✅ Compatible |
| Android Gradle Plugin | 8.7.0 | ✅ Compatible |
| NDK | 27.0.12077973 (R27) | ⚠️ Ancien |
| Kotlin | 2.1.0 | ✅ Compatible |

### Analyse des Bibliothèques Natives (Bundle Initial)

#### Résultats arm64-v8a (64-bit)

```
✓ libapp.so                      64KB (0x10000)
✓ libbarhopper_v3.so             16KB (0x04000)
✗ libcreatedata.so                4KB (0x01000)  ← PROBLÈME
✓ libdartjni.so                  16KB (0x04000)
✓ libdatastore_shared_counter.so 16KB (0x04000)
✓ libflutter.so                  64KB (0x10000)
✓ libimage_processing_util_jni.so 16KB (0x04000)
✓ libsentry-android.so           16KB (0x04000)
✓ libsentry.so                   16KB (0x04000)
✓ libsurface_util_jni.so         16KB (0x04000)
```

#### Résultats armeabi-v7a (32-bit)

```
✓ libapp.so                      16KB (0x04000)
✗ libbarhopper_v3.so              4KB (0x01000)  ← PROBLÈME
✗ libcreatedata.so                4KB (0x01000)  ← PROBLÈME
✓ libdatastore_shared_counter.so 16KB (0x04000)
✓ libflutter.so                  64KB (0x10000)
✓ libimage_processing_util_jni.so 16KB (0x04000)
✓ libsentry-android.so           16KB (0x04000)
✓ libsentry.so                   16KB (0x04000)
✓ libsurface_util_jni.so         16KB (0x04000)
```

### Identification de l'Origine

**Bibliothèques problématiques** :
- `libcreatedata.so` : Brother SDK (2 architectures)
- `libbarhopper_v3.so` : Brother SDK (armeabi-v7a uniquement)

**Source** : Fork custom `another_brother`
- Repository : `https://github.com/alaindeseine/another_brother.git`
- Branche : `ios-td2350-final`
- Localisation : `bpsdkaall4130/bpsdka4130/libs/BrotherPrintLibrary/jni/`

---

## 3. MODIFICATIONS APPORTÉES AU PROJET PRINCIPAL

### 3.1 Mise à Jour NDK (build.gradle.kts)

**Fichier** : `android/app/build.gradle.kts`

```kotlin
android {
    namespace = "com.kalyapp.print_service"
    compileSdk = flutter.compileSdkVersion
-   ndkVersion = "27.0.12077973" // R27
+   ndkVersion = "28.0.12674087" // R28 pour support natif 16KB
```

**Raison** : NDK R28+ a un meilleur support natif pour l'alignement 16KB des bibliothèques.

---

### 3.2 Configuration Packaging (build.gradle.kts)

```kotlin
defaultConfig {
    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols += listOf("**/*.so")
        }
        resources {
            excludes += listOf("META-INF/*.kotlin_module")
        }
    }
}
```

**Raison** : Tentative d'empêcher Gradle de réaligner les bibliothèques lors du packaging.

---

### 3.3 Optimisation gradle.properties

**Fichier** : `android/gradle.properties`

```properties
android.injected.build.abi=arm64-v8a,armeabi-v7a
android.enableR8.fullMode=true
android.experimental.enableArtProfiles=true
```

---

### 3.4 AndroidManifest.xml

```xml
<application
    android:extractNativeLibs="false">
```

**Raison** : Force Android à utiliser les bibliothèques natives sans extraction.

---

## 4. RÉSULTATS DES TESTS

### Build Après Modifications

```bash
flutter clean
flutter pub get
flutter build appbundle --release --target-platform=android-arm,android-arm64 \
  --obfuscate --split-debug-info=build/app/outputs/symbols
```

**Durée** : ~9 minutes (NDK R28 téléchargé et installé)

### Vérification Alignement (Nouveau Bundle)

#### ❌ RÉSULTATS

**arm64-v8a** :
- `libcreatedata.so` : **TOUJOURS 4KB** ❌

**armeabi-v7a** :
- `libbarhopper_v3.so` : **TOUJOURS 4KB** ❌
- `libcreatedata.so` : **TOUJOURS 4KB** ❌

### ❌ CONCLUSION DES TESTS

**Malgré toutes les modifications** (NDK R28, configuration packaging, propriétés Gradle, AndroidManifest), **les bibliothèques Brother SDK restent à 4KB** ❌

---

## 5. ANALYSE DU PACKAGE OFFICIEL another_brother v2.2.4

### Commits du Support 16KB (10 septembre 2025)

**Auteur** : swapnilparmar-git
**Releasé par** : PoachMe (Frank Hernandez) le 12 septembre 2025

#### Commit 1 : android/build.gradle

```gradle
- classpath 'com.android.tools.build:gradle:8.5.0'
+ classpath 'com.android.tools.build:gradle:8.5.2'
```

#### Commit 2 : android/gradle/wrapper/gradle-wrapper.properties

```properties
- distributionUrl=gradle-8.3-all.zip
+ distributionUrl=gradle-8.13.0-all.zip
```

#### Commit 3 : pubspec.yaml

```yaml
- flutter_blue_plus: ^1.12.13
+ flutter_blue_plus: ^1.35.10
```

### Architecture du Package Officiel

**Fichier** : `android/build.gradle`

```gradle
dependencies {
    // Brother SDK téléchargé depuis Maven
    implementation 'com.brother.sdk:printer:4.6.1@aar'
    implementation 'com.brother.typeb:print:1.0.0'
}

rootProject.allprojects {
    repositories {
        maven {
            url "https://artifacts.rouninlabs.com/rounin-libs-external/"
        }
    }
}
```

**Point clé** : Les bibliothèques natives sont **téléchargées depuis Maven**, pas embarquées dans le Git.

---

## 6. ANALYSE COMPARATIVE : OFFICIEL vs FORK

| Aspect | Package Officiel v2.2.4 | Fork (ios-td2350-final) | Impact |
|--------|-------------------------|-------------------------|--------|
| **AGP** | 8.5.2 | 8.5.0 | Mineur |
| **Gradle** | 8.13.0 | 8.7 | Mineur |
| **Brother SDK** | 4.6.1 (Maven) | 4.12.0 (AAR local) | **CRITIQUE** |
| **Source .so** | Maven (16KB OK) | AAR local (4KB) | **CRITIQUE** |
| **flutter_blue_plus** | 1.35.10 | ? | Mineur |

### Note Critique dans le Fork

```gradle
// Note: Using local 4.12.0 instead of 4.13.0 due to WifiConnection crash bug
implementation files('libs/BrotherPrintLibrary-4.12.0.aar')
```

### Raison du Fork

**CRITIQUE** : Le package officiel utilise Brother SDK 4.6.1 qui **NE SUPPORTE PAS** l'imprimante **TD-2350**.

Le fork existe spécifiquement pour :
- ✅ Support de l'imprimante TD-2350
- ✅ Fix du bug WiFi crash (version 4.12.0 vs 4.13.0)
- ✅ Modifications iOS spécifiques

**Conclusion** : Retourner au package officiel n'est **PAS UNE OPTION**.

---

## 7. POURQUOI LES BIBLIOTHÈQUES NATIVES RESTENT À 4KB

### Explication Technique

1. **Brother SDK précompilé** : Les `.so` dans `BrotherPrintLibrary-4.12.0.aar` sont compilées avec un ancien NDK (probablement R23 ou antérieur)

2. **Gradle ne réaligne PAS** : Les configurations `packaging`, `useLegacyPackaging`, `externalNativeBuild` sont **inutiles** car elles ne s'appliquent QUE :
   - Aux bibliothèques qu'on compile nous-mêmes via CMake/ndk-build
   - PAS aux bibliothèques précompilées des plugins

3. **Vérité** : Les bibliothèques Brother dans l'AAR locale ont été compilées AVANT que le support 16KB n'existe.

---

## 8. SOLUTIONS POSSIBLES POUR LE FORK

### ❌ Solution A : Passer au SDK Maven (NON VIABLE)

**Pourquoi NON** :
- Brother SDK 4.6.1 ne supporte PAS la TD-2350
- Raison d'être du fork compromise

**Verdict** : IMPOSSIBLE

---

### ✅ Solution B : Recompiler le Brother SDK avec NDK R28+ (RECOMMANDÉ)

#### Prérequis

1. **Sources Brother SDK** :
   - Option 1 : Contacter Brother pour obtenir les sources
   - Option 2 : Décompiler l'AAR (légalité à vérifier)
   - Option 3 : Obtenir le SDK 4.12.0 depuis Brother Developer Network

2. **Outils** :
   - Android NDK R28+ (`28.0.12674087`)
   - Android Studio / Gradle 8.13.0
   - AGP 8.5.2+

#### Étapes Détaillées

##### 1. Obtenir le Brother SDK Source

```bash
# Si vous avez accès aux sources Brother
git clone <brother-sdk-source-repo>
cd brother-sdk-source

# OU extraire depuis l'AAR existante
unzip BrotherPrintLibrary-4.12.0.aar -d brother-sdk-extracted
```

##### 2. Configurer le Build Native

**Android.mk** (si le projet utilise ndk-build) :

```makefile
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := createdata
LOCAL_SRC_FILES := createdata.cpp
# CRITIQUE: Force l'alignement 16KB
LOCAL_LDFLAGS := -Wl,-z,max-page-size=0x4000
include $(BUILD_SHARED_LIBRARY)
```

**CMakeLists.txt** (si le projet utilise CMake) :

```cmake
cmake_minimum_required(VERSION 3.18.1)

project("brothersdk")

# CRITIQUE: Support 16KB page size
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wl,-z,max-page-size=0x4000")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wl,-z,max-page-size=0x4000")

add_library(createdata SHARED createdata.cpp)
add_library(barhopper_v3 SHARED barhopper_v3.cpp)
```

##### 3. Compiler avec NDK R28

```bash
# Configurer le NDK
export ANDROID_NDK_HOME=/path/to/ndk/28.0.12674087

# Option A: ndk-build
cd jni
$ANDROID_NDK_HOME/ndk-build \
  APP_ABI="arm64-v8a armeabi-v7a" \
  APP_PLATFORM=android-21

# Option B: CMake
mkdir build && cd build
cmake .. \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-21
make
```

##### 4. Vérifier l'Alignement

```bash
python3 check_alignment.py libs/arm64-v8a/libcreatedata.so
# Attendu: 16KB (0x4000) ou plus
```

##### 5. Recréer l'AAR

```bash
# Structure AAR
mkdir -p brother-sdk-16kb/{jni/arm64-v8a,jni/armeabi-v7a}

# Copier les nouvelles bibliothèques
cp libs/arm64-v8a/*.so brother-sdk-16kb/jni/arm64-v8a/
cp libs/armeabi-v7a/*.so brother-sdk-16kb/jni/armeabi-v7a/

# Copier les ressources existantes
unzip BrotherPrintLibrary-4.12.0.aar -d old_aar
cp -r old_aar/res brother-sdk-16kb/
cp old_aar/AndroidManifest.xml brother-sdk-16kb/
cp old_aar/classes.jar brother-sdk-16kb/

# Créer la nouvelle AAR
cd brother-sdk-16kb
zip -r ../BrotherPrintLibrary-4.12.0-16kb.aar .
```

##### 6. Remplacer dans le Fork

```bash
cd /path/to/another_brother_fork
cp BrotherPrintLibrary-4.12.0-16kb.aar libs/BrotherPrintLibrary-4.12.0.aar

git add libs/BrotherPrintLibrary-4.12.0.aar
git commit -m "Update Brother SDK with 16KB alignment support

- Recompiled native libraries with NDK R28
- Force 16KB page alignment (0x4000)
- Fixes Google Play Store rejection for Android 15+ devices"
git push origin ios-td2350-final
```

#### Avantages Solution B

- ✅ Garde le support TD-2350
- ✅ Conserve les fixes WiFi
- ✅ Solution pérenne
- ✅ Contrôle total sur la version

#### Inconvénients Solution B

- ⏱️ Complexe (nécessite sources Brother SDK)
- 🔧 Nécessite compétences NDK/JNI
- ⚖️ Questions de licence à vérifier avec Brother

---

### ⚡ Solution C : Patcher les Bibliothèques avec objcopy (WORKAROUND)

#### Principe

Utiliser `llvm-objcopy` pour modifier l'alignement des sections dans les `.so` existantes.

#### Étapes

```bash
# Localiser objcopy du NDK R28
OBJCOPY=/path/to/ndk/28.0.12674087/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-objcopy

# Extraire l'AAR
mkdir /tmp/brother_aar && cd /tmp/brother_aar
unzip BrotherPrintLibrary-4.12.0.aar

# Patcher chaque .so
for arch in arm64-v8a armeabi-v7a; do
  for lib in jni/$arch/*.so; do
    echo "Patching $lib..."
    $OBJCOPY \
      --set-section-alignment .text=0x4000 \
      --set-section-alignment .data=0x4000 \
      $lib $lib.new
    mv $lib.new $lib
  done
done

# Recréer l'AAR
zip -r BrotherPrintLibrary-4.12.0-patched.aar .
```

#### Avertissement Solution C

- ⚠️ **Non testé** - peut causer des crashs
- ⚠️ **Risqué** - modifier des binaires peut introduire des bugs
- ⚠️ **Temporaire** - perdu à chaque mise à jour du SDK
- ⚠️ **Légalité douteuse** - modification de binaires propriétaires

**Utiliser uniquement pour tests/proof-of-concept**

---

### 🔄 Solution D : Contacter Brother / RounInLabs (DIPLOMATIQUE)

#### Actions

1. **Issue GitHub** sur le repo officiel
2. **Contacter Brother Developer Support**
3. **Contacter RounInLabs**

#### Avantages

- ✅ Officiel et supporté
- ✅ Pas de manipulation binaire
- ✅ Légal et propre

#### Inconvénients

- ⏱️ Délai de réponse incertain
- ❓ Pas de garantie de réponse positive

---

### 📊 Comparaison des Solutions

| Solution | Complexité | Délai | Viabilité | Pérennité | Recommandation |
|----------|------------|-------|-----------|-----------|----------------|
| A - Maven SDK | ❌ Bloqué | N/A | **0%** | N/A | ❌ NON |
| B - Recompiler SDK | 🔴 Haute | 2-5 jours | **80%** | ✅ Excellente | ✅ **OUI** |
| C - Patcher binaires | 🟡 Moyenne | 2-4 heures | **30%** | ❌ Nulle | ⚠️ Test uniquement |
| D - Contacter Brother | 🟢 Faible | 2-8 semaines | **50%** | ✅ Excellente | ✅ En parallèle |

---

## 9. COMMANDES DE VÉRIFICATION

### Script Python - Vérification Alignement

Sauvegarder en tant que `check_16kb.py` :

```python
#!/usr/bin/env python3
import struct, os, sys

def check_elf_alignment(filepath):
    with open(filepath, 'rb') as f:
        elf_header = f.read(64)
        if elf_header[:4] != b'\x7fELF':
            return None
        ei_class = elf_header[4]
        is_64bit = (ei_class == 2)

        if is_64bit:
            f.seek(32)
            e_phoff = struct.unpack('<Q', f.read(8))[0]
            f.seek(54)
            e_phentsize = struct.unpack('<H', f.read(2))[0]
            e_phnum = struct.unpack('<H', f.read(2))[0]
        else:
            f.seek(28)
            e_phoff = struct.unpack('<I', f.read(4))[0]
            f.seek(42)
            e_phentsize = struct.unpack('<H', f.read(2))[0]
            e_phnum = struct.unpack('<H', f.read(2))[0]

        alignments = []
        for i in range(e_phnum):
            f.seek(e_phoff + i * e_phentsize)
            ph = f.read(e_phentsize)
            p_type = struct.unpack('<I', ph[0:4])[0]
            if p_type == 1:
                if is_64bit:
                    p_align = struct.unpack('<Q', ph[48:56])[0]
                else:
                    p_align = struct.unpack('<I', ph[28:32])[0]
                alignments.append(p_align)

        return max(alignments) if alignments else 0

# Utilisation
if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python3 check_16kb.py <file.so>")
        sys.exit(1)

    align = check_elf_alignment(sys.argv[1])
    align_kb = align // 1024
    print(f"Alignment: {align_kb}KB (0x{align:05x})")
    print("✅ OK" if align >= 16384 else "❌ FAILED")
```

### Utilisation

```bash
python3 check_16kb.py path/to/libcreatedata.so
```

---

## 10. PROCHAINES ÉTAPES

### Plan d'Action Recommandé

#### Phase 1 : Investigation (Immédiat)

- [ ] Vérifier accès Brother Developer Network SDK 4.12.0 source
- [ ] Contacter Brother Support (SDK 16KB compatible TD-2350)
- [ ] Créer issue sur repo officiel another_brother
- [ ] Contacter RounInLabs (Maven version SDK 4.12.0)

**Délai** : 1 semaine

---

#### Phase 2 : Solution Technique (Parallèle)

**Option 2A : Si sources disponibles**

- [ ] Obtenir Brother SDK 4.12.0 sources
- [ ] Configurer build avec NDK R28
- [ ] Ajouter flags 16KB alignment
- [ ] Compiler pour arm64-v8a et armeabi-v7a
- [ ] Vérifier alignement
- [ ] Créer nouvelle AAR
- [ ] Tester impression TD-2350
- [ ] Mettre à jour le fork

**Délai** : 2-5 jours

**Option 2B : Si sources indisponibles**

- [ ] Tenter solution C (patch objcopy) pour proof-of-concept
- [ ] Tester stabilité
- [ ] Attendre réponse Brother/RounInLabs

**Délai** : 2-4 heures (test), puis attente

---

#### Phase 3 : Validation

- [ ] Build complet avec nouveau SDK
- [ ] Vérifier alignement avec `check_16kb.py`
- [ ] Tests fonctionnels TD-2350
- [ ] Tests sur Android 15 émulateur 16KB
- [ ] Soumission test interne Play Store
- [ ] Vérification Play Console

**Délai** : 1-2 jours

---

#### Phase 4 : Déploiement

- [ ] Mettre à jour version dans pubspec.yaml
- [ ] Commit et push sur fork
- [ ] Mettre à jour projet principal
- [ ] Build production
- [ ] Soumission Play Store

**Délai** : 1 jour

---

### Délais Play Store

- **Deadline nouvelles soumissions** : 1er novembre 2025
- **Deadline toutes mises à jour** : 1er mai 2026
- **Temps restant** : ~6 mois pour mises à jour

**Recommandation** : Poursuivre en mises à jour (deadline mai 2026).

---

## CONCLUSION

Le problème d'alignement 16KB provient des **bibliothèques natives précompilées du Brother SDK** dans le fork `another_brother`. Ces bibliothèques ont été compilées avec un ancien NDK et ne peuvent pas être réalignées par de simples configurations Gradle.

La **solution recommandée** est de **recompiler le Brother SDK** avec NDK R28+ et les flags d'alignement appropriés. Cela nécessite l'accès aux sources du SDK, qu'il faut obtenir auprès de Brother.

En parallèle, il est recommandé de **contacter Brother et RounInLabs** pour demander une version officielle du SDK compatible 16KB avec support TD-2350.

Le fork doit être maintenu car il est **essentiel** pour le support de l'imprimante TD-2350, qui n'est pas disponible dans les versions Maven du package officiel.

---

**Date du rapport** : 29 octobre 2025
**Auteur** : Claude (Anthropic)
**Projet** : KalyApp Print Service
**Version analysée** : 4.0.16+4016

---

*Fin du rapport*
