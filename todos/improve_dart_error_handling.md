# TODO : Amélioration de la Gestion d'Erreurs Dart

**Date de création** : 31 octobre 2025
**Priorité** : 🟡 Moyenne (ergonomie, pas critique)
**Estimation** : 2-3 heures
**Type** : Enhancement / Developer Experience

---

## 📋 Contexte

Actuellement, le plugin `another_brother` utilise un pattern où **toutes les méthodes retournent `success()`** même en cas d'erreur, avec l'erreur encodée dans `PrinterStatus.errorCode`.

**Pattern actuel** :
```kotlin
// Kotlin - Native
catch (e: Exception) {
    result.success(PrinterStatus().apply {
        errorCode = ERROR_COMMUNICATION_ERROR
    }.toMap())
}
```

```dart
// Dart - Plugin
Future<PrinterStatus> getPrinterStatus() async {
  final result = await _channel.invokeMethod('getPrinterStatus', ...);
  return PrinterStatus.fromMap(result); // errorCode est dedans
}
```

**Problème** : L'app doit **se souvenir** de vérifier `errorCode` :
```dart
// App - Facile à oublier !
final status = await printer.getPrinterStatus();
if (status.errorCode != ErrorCode.ERROR_NONE) {  // ⚠️ Oubli fréquent
  handleError(status.errorCode);
}
```

---

## 🎯 Objectif

Améliorer l'ergonomie de la gestion d'erreurs **sans breaking change** en ajoutant des helpers optionnels qui rendent le code plus lisible et moins sujet aux erreurs.

---

## 💡 Solution Proposée : Extensions & Exceptions Custom

### 1. Créer des Extensions pour PrinterStatus

**Fichier** : `lib/src/printer_status_extensions.dart` (nouveau)

```dart
/// Extensions utilitaires pour faciliter la gestion d'erreurs PrinterStatus
extension PrinterStatusExtensions on PrinterStatus {
  /// Vérifie s'il y a une erreur
  bool get hasError => errorCode != ErrorCode.ERROR_NONE;

  /// Vérifie si le statut est OK
  bool get isSuccess => errorCode == ErrorCode.ERROR_NONE;

  /// Lance une exception si le statut contient une erreur
  ///
  /// Usage:
  /// ```dart
  /// final status = await printer.getPrinterStatus();
  /// status.throwIfError(); // Lance PrinterException si erreur
  /// ```
  void throwIfError() {
    if (hasError) {
      throw PrinterException(
        errorCode: errorCode,
        message: _getErrorMessage(),
        status: this,
      );
    }
  }

  /// Vérifie si l'erreur est critique (nécessite intervention utilisateur)
  bool get isCriticalError {
    return errorCode == ErrorCode.ERROR_COMMUNICATION_ERROR ||
           errorCode == ErrorCode.ERROR_BROTHER_PRINTER_NOT_FOUND ||
           errorCode == ErrorCode.ERROR_COVER_OPEN ||
           errorCode == ErrorCode.ERROR_WRONG_LABEL;
  }

  /// Vérifie si l'erreur est récupérable (retry possible)
  bool get isRecoverableError {
    return errorCode == ErrorCode.ERROR_TIMEOUT ||
           errorCode == ErrorCode.ERROR_COMMUNICATION_ERROR;
  }

  /// Message d'erreur human-readable
  String _getErrorMessage() {
    switch (errorCode) {
      case ErrorCode.ERROR_COMMUNICATION_ERROR:
        return "Unable to communicate with printer";
      case ErrorCode.ERROR_BROTHER_PRINTER_NOT_FOUND:
        return "Printer not found";
      case ErrorCode.ERROR_WRONG_LABEL:
        return "Wrong label installed in printer";
      case ErrorCode.ERROR_COVER_OPEN:
        return "Printer cover is open";
      case ErrorCode.ERROR_TIMEOUT:
        return "Printer communication timeout";
      // ... autres cas
      default:
        return "Printer error: ${errorCode.toString()}";
    }
  }
}
```

---

### 2. Créer des Exceptions Custom

**Fichier** : `lib/src/printer_exceptions.dart` (nouveau)

```dart
/// Exception de base pour toutes les erreurs d'imprimante
class PrinterException implements Exception {
  final ErrorCode errorCode;
  final String message;
  final PrinterStatus? status;

  const PrinterException({
    required this.errorCode,
    required this.message,
    this.status,
  });

  @override
  String toString() => 'PrinterException: $message ($errorCode)';
}

/// Exception spécifique pour les erreurs de communication
class PrinterCommunicationException extends PrinterException {
  const PrinterCommunicationException({
    required String message,
    PrinterStatus? status,
  }) : super(
    errorCode: ErrorCode.ERROR_COMMUNICATION_ERROR,
    message: message,
    status: status,
  );
}

/// Exception spécifique pour imprimante non trouvée
class PrinterNotFoundException extends PrinterException {
  const PrinterNotFoundException({
    required String message,
    PrinterStatus? status,
  }) : super(
    errorCode: ErrorCode.ERROR_BROTHER_PRINTER_NOT_FOUND,
    message: message,
    status: status,
  );
}

/// Exception spécifique pour mauvaise étiquette
class WrongLabelException extends PrinterException {
  const WrongLabelException({
    required String message,
    PrinterStatus? status,
  }) : super(
    errorCode: ErrorCode.ERROR_WRONG_LABEL,
    message: message,
    status: status,
  );
}

/// Exception spécifique pour timeout
class PrinterTimeoutException extends PrinterException {
  const PrinterTimeoutException({
    required String message,
    PrinterStatus? status,
  }) : super(
    errorCode: ErrorCode.ERROR_TIMEOUT,
    message: message,
    status: status,
  );
}
```

---

### 3. Ajouter des Méthodes de Convenience

**Fichier** : `lib/printer.dart` (modifier)

```dart
// Ajouter ces méthodes à la classe Printer

/// Vérifie le statut et lance une exception si erreur
///
/// Usage:
/// ```dart
/// try {
///   final status = await printer.getPrinterStatusOrThrow();
///   // Status garanti sans erreur ici
/// } catch (e) {
///   // Gérer l'erreur
/// }
/// ```
Future<PrinterStatus> getPrinterStatusOrThrow() async {
  final status = await getPrinterStatus();
  status.throwIfError();
  return status;
}

/// Imprime et lance une exception si erreur
Future<PrinterStatus> printImageOrThrow(Image image, {Duration? timeout}) async {
  final status = await printImage(image, timeout: timeout);
  status.throwIfError();
  return status;
}

// Répéter pour les autres méthodes : printFile, printPdfFile, etc.
```

---

### 4. Exporter les Nouveaux Modules

**Fichier** : `lib/another_brother.dart` (modifier)

```dart
// Ajouter les exports
export 'src/printer_status_extensions.dart';
export 'src/printer_exceptions.dart';
```

---

## 📚 Exemples d'Usage

### Usage 1 : Pattern Actuel (Backward Compatible)

```dart
// Continue de fonctionner comme avant - pas de breaking change
final status = await printer.getPrinterStatus();
if (status.errorCode != ErrorCode.ERROR_NONE) {
  print("Erreur: ${status.errorCode}");
}
```

### Usage 2 : Avec Extensions (Nouveau, Plus Lisible)

```dart
// Plus lisible avec les helpers
final status = await printer.getPrinterStatus();
if (status.hasError) {
  if (status.isCriticalError) {
    showCriticalErrorDialog(status.errorCode);
  } else if (status.isRecoverableError) {
    retryOperation();
  }
}
```

### Usage 3 : Avec Exceptions (Nouveau, Dart-Idiomatique)

```dart
// Force la gestion d'erreur via try-catch
try {
  final status = await printer.getPrinterStatusOrThrow();
  // Status garanti sans erreur
  displayPrinterInfo(status);

} on PrinterCommunicationException catch (e) {
  showError("Impossible de communiquer avec l'imprimante");

} on PrinterNotFoundException catch (e) {
  showError("Imprimante non trouvée");

} on WrongLabelException catch (e) {
  showError("Mauvaise étiquette installée");

} on PrinterException catch (e) {
  showError("Erreur imprimante: ${e.message}");
}
```

### Usage 4 : Hybrid (Check Puis Throw)

```dart
// Vérifier d'abord, puis throw si besoin
final status = await printer.getPrinterStatus();

if (status.isRecoverableError) {
  // Retry logic
  await Future.delayed(Duration(seconds: 2));
  final retryStatus = await printer.getPrinterStatus();
  retryStatus.throwIfError(); // Lance exception si toujours en erreur
}

// Continue avec status OK
```

---

## ✅ Avantages de Cette Approche

1. **✅ Pas de Breaking Change**
   - Le code existant continue de fonctionner
   - Les apps n'ont pas besoin de modification

2. **✅ Amélioration Progressive**
   - Les devs peuvent adopter les nouveaux helpers progressivement
   - Choix entre pattern actuel ou exceptions

3. **✅ Plus Difficile d'Oublier**
   - `throwIfError()` force la gestion d'erreur
   - Méthodes `*OrThrow()` rendent l'intention claire

4. **✅ Code Plus Lisible**
   - `status.hasError` vs `status.errorCode != ErrorCode.ERROR_NONE`
   - `status.isCriticalError` explicite l'intention

5. **✅ Meilleur Tooling**
   - L'IDE suggère les méthodes via autocomplétion
   - Documentation inline sur chaque helper

6. **✅ Gestion d'Erreur Typée**
   - Catch spécifique par type d'exception
   - Meilleure granularité

---

## 🔄 Plan d'Implémentation

### Phase 1 : Extensions (1 heure)
- [ ] Créer `lib/src/printer_status_extensions.dart`
- [ ] Implémenter `hasError`, `isSuccess`, `throwIfError()`
- [ ] Implémenter `isCriticalError`, `isRecoverableError`
- [ ] Implémenter `_getErrorMessage()` pour tous les ErrorCode
- [ ] Ajouter tests unitaires

### Phase 2 : Exceptions (30 min)
- [ ] Créer `lib/src/printer_exceptions.dart`
- [ ] Implémenter `PrinterException` et sous-classes
- [ ] Ajouter tests unitaires

### Phase 3 : Méthodes *OrThrow (30 min)
- [ ] Ajouter `getPrinterStatusOrThrow()` dans `Printer`
- [ ] Ajouter `printImageOrThrow()` dans `Printer`
- [ ] Ajouter autres méthodes `*OrThrow` selon besoin
- [ ] Ajouter tests unitaires

### Phase 4 : Documentation (30 min)
- [ ] Mettre à jour README.md avec exemples
- [ ] Ajouter section "Error Handling" dans la doc
- [ ] Ajouter exemples d'usage dans example/
- [ ] Mettre à jour CHANGELOG.md

### Phase 5 : Tests & Validation (30 min)
- [ ] Valider backward compatibility
- [ ] Tester les 3 patterns d'usage
- [ ] Code review
- [ ] Merge

---

## 📊 Impact Estimation

**Code à modifier** :
- 2 nouveaux fichiers (~200 lignes)
- 1 fichier existant modifié (~30 lignes)
- Tests (~100 lignes)
- Documentation (~50 lignes)

**Total** : ~380 lignes

**Bénéfices** :
- ✅ Meilleure DX (Developer Experience)
- ✅ Réduction des bugs dans les apps
- ✅ Code plus maintenable
- ✅ Alignement avec les best practices Dart/Flutter

---

## 🎯 Critères d'Acceptation

- [ ] Backward compatibility maintenue (code existant fonctionne)
- [ ] Extensions disponibles sur tous les `PrinterStatus`
- [ ] Exceptions custom créées et documentées
- [ ] Méthodes `*OrThrow()` ajoutées
- [ ] Tests unitaires à 100% coverage
- [ ] Documentation complète
- [ ] Example app mise à jour avec les 3 patterns
- [ ] CHANGELOG.md mis à jour

---

## 📝 Notes

### Pourquoi Pas Plus Tôt ?

Cette amélioration n'a pas été faite immédiatement car :
1. Le fix crash WiFi était **urgent** (crash app)
2. Cette amélioration est **ergonomique**, pas fonctionnelle
3. Pas de breaking change souhaité
4. Pattern actuel est cohérent avec le reste du plugin

### Alternative Considérée

**Option rejetée** : Changer toutes les méthodes pour lancer des exceptions
- ❌ Breaking change massif
- ❌ Force tous les utilisateurs à modifier leur code
- ❌ Perte d'infos partielles (ex: batteryLevel en cas d'erreur)

**Option choisie** : Extensions optionnelles + méthodes `*OrThrow()`
- ✅ Pas de breaking change
- ✅ Adoption progressive possible
- ✅ Flexibilité maximale

---

## 🔗 Références

- Issue Sentry : HACCP-SYNC-1S (crash WiFi résolu)
- Commit fix NPE : `d638693`
- Discussion : Session Claude 2025-10-31

---

**Créé par** : Claude (Anthropic)
**Date** : 31 octobre 2025
**Statut** : 📋 TODO - Non urgent, amélioration ergonomique
