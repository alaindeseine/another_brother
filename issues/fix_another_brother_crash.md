# Fix crash another_brother - getPrinterStatus timeout

## 📋 Résumé du problème

**Crash SIGABRT récurrent** dans l'application Flutter lors de l'utilisation du plugin `another_brother` pour vérifier le statut des imprimantes Brother Bluetooth.

### Signature du crash
```
F/flutter: [FATAL:flutter/lib/ui/window/platform_message_response_dart_port.cc(53)] Check failed: did_send.
Fatal signal 6 (SIGABRT), code -1 (SI_QUEUE)
Stack trace: com.rouninlabs.another_brother.method.GetPrinterStatusMethodCall$execute$1$2.invokeSuspend
```

### Logs Brother SDK associés
```
E/Brother Print SDK: IOException is caught in connect method.
E/Brother Print SDK: read failed, socket might closed or timeout, read ret: -1
D/BluetoothSocket: close() - BluetoothSocket fermé
```

---

## 🔍 Analyse technique de la cause racine

### Séquence problématique actuelle

1. **Flutter Dart** : Appel `printer.getPrinterStatus()`
2. **another_brother** : `_channel.invokeMethod("getPrinterStatus", params)` **SANS TIMEOUT**
3. **Platform channel** : Transmission vers code natif Android
4. **Brother SDK natif** : Tentative de connexion Bluetooth (peut prendre 15+ secondes)
5. **Flutter engine** : Ferme le platform channel (app lifecycle, timeout, memory pressure)
6. **Brother SDK** : Termine l'opération et essaie de répondre
7. **another_brother natif** : Tentative de réponse sur channel fermé
8. **Flutter engine** : `did_send` check fails → **CRASH SIGABRT**

### Code problématique dans another_brother

**Fichier :** `lib/src/printer.dart` (ligne ~XXX)

```dart
/// Retrieves the printer status.
Future<PrinterStatus> getPrinterStatus() async {
  var params = {
    "printerId": mPrinterId,
    "printInfo": mPrinterInfo.toMap(),
  };

  // ❌ PROBLÈME: Pas de timeout sur invokeMethod
  final Map resultMap = await _channel.invokeMethod("getPrinterStatus", params);
  PrinterStatus status = PrinterStatus.fromMap(resultMap);

  return status;
}
```

### Pourquoi les try-catch Flutter ne fonctionnent pas

Le crash se produit **au niveau natif** lors de la tentative de réponse sur un channel fermé. Ce n'est **pas une exception Dart** mais un crash du platform channel lui-même.

```dart
try {
  // ✅ Cette ligne s'exécute correctement
  PrinterStatus status = await printer.getPrinterStatus();
  // ❌ Le crash arrive APRÈS, quand le natif essaie de répondre
} catch (e) {
  // ❌ Cette ligne n'est JAMAIS atteinte car ce n'est pas une exception Dart
}
```

---

## ✅ Solution proposée

### Code corrigé pour another_brother

**Fichier :** `lib/src/printer.dart`

```dart
/// Retrieves the printer status.
Future<PrinterStatus> getPrinterStatus() async {
  var params = {
    "printerId": mPrinterId,
    "printInfo": mPrinterInfo.toMap(),
  };

  try {
    // ✅ Fix crash: Timeout court pour éviter channel fermé
    final Map resultMap = await _channel.invokeMethod("getPrinterStatus", params)
      .timeout(
        Duration(seconds: 3), // Timeout agressif pour Bluetooth
        onTimeout: () {
          // Retourner un Map d'erreur au lieu de crash
          return {
            "errorCode": {"id": 0, "name": "ERROR_COMMUNICATION_ERROR"}
          };
        }
      );
    
    PrinterStatus status = PrinterStatus.fromMap(resultMap);
    return status;
  } catch (e) {
    // Protection supplémentaire pour autres erreurs
    PrinterStatus errorStatus = PrinterStatus();
    errorStatus.errorCode = ErrorCode.ERROR_COMMUNICATION_ERROR;
    return errorStatus;
  }
}
```

### Paramètres de timeout recommandés

- **Bluetooth** : 3 secondes (connexions souvent instables)
- **WiFi/Ethernet** : 5 secondes (plus fiable mais peut être lent)
- **USB** : 2 secondes (connexion directe, rapide)

### Autres méthodes à protéger

Appliquer le même pattern à toutes les méthodes qui utilisent `_channel.invokeMethod` :

- `printPdfFile()`
- `printImage()`
- `printText()`
- `getLabelInfo()`
- `getPrinterSettings()`

---

## 🧪 Tests de validation

### Test 1 : Imprimante éteinte
```dart
// Éteindre l'imprimante Brother
final status = await printer.getPrinterStatus();
// Résultat attendu : ERROR_COMMUNICATION_ERROR (pas de crash)
```

### Test 2 : Imprimante hors portée Bluetooth
```dart
// Éloigner l'imprimante (>10m)
final status = await printer.getPrinterStatus();
// Résultat attendu : Timeout après 3s, ERROR_COMMUNICATION_ERROR
```

### Test 3 : App en background
```dart
// Mettre l'app en background pendant getPrinterStatus
// Résultat attendu : Pas de crash, timeout graceful
```

### Test 4 : Monitoring périodique
```dart
// Laisser le monitoring automatique tourner avec imprimante éteinte
// Résultat attendu : Statuts offline réguliers, pas de crash
```

---

## 📊 Impact de la solution

### ✅ Bénéfices
- **Élimination des crashes SIGABRT** liés à another_brother
- **Graceful degradation** : Imprimantes offline au lieu de crash
- **Stabilité app** : Monitoring peut continuer sans interruption
- **UX améliorée** : Pas de fermeture brutale de l'app

### ⚠️ Considérations
- **Timeout plus court** : Détection d'erreur plus rapide mais moins de tolérance
- **Status offline** : Imprimantes lentes peuvent apparaître offline temporairement
- **Logs supplémentaires** : Plus de logs d'erreur (normal)

### 🔄 Compatibilité
- **Rétrocompatible** : Aucun changement d'API
- **Comportement** : Identique sauf en cas d'erreur/timeout
- **Performance** : Légèrement améliorée (timeouts plus courts)

---

## 🚀 Implémentation

### Étapes recommandées

1. **Fork another_brother** depuis GitHub
2. **Appliquer le fix** dans `lib/src/printer.dart`
3. **Tester** avec les cas de test ci-dessus
4. **Publier** version fixée sur pub.dev ou utiliser dependency override
5. **Mettre à jour** pubspec.yaml pour utiliser la version fixée

### Dependency override temporaire

En attendant la publication officielle :

```yaml
dependency_overrides:
  another_brother:
    git:
      url: https://github.com/votre-username/another_brother.git
      ref: fix-timeout-crash
```

---

## 📝 Notes techniques

### Pourquoi 3 secondes pour Bluetooth ?

- **Connexion Bluetooth** : Généralement < 2s si l'imprimante est disponible
- **Échec rapide** : Si > 3s, probablement éteinte/hors portée
- **Channel safety** : Évite la fermeture du channel par Flutter
- **UX** : Feedback rapide à l'utilisateur

### Alternative : Timeout configurable

```dart
Future<PrinterStatus> getPrinterStatus({Duration? timeout}) async {
  final timeoutDuration = timeout ?? Duration(seconds: 3);
  // ... reste du code avec timeoutDuration
}
```

### Monitoring en production

Ajouter des métriques pour surveiller :
- **Fréquence des timeouts** par type d'imprimante
- **Temps de réponse moyen** des opérations Brother
- **Taux de succès** des connexions Bluetooth

---

**Date :** 16 octobre 2025  
**Analysé par :** Équipe technique  
**Priorité :** CRITIQUE - Fix nécessaire pour stabilité production  
**Status :** Prêt pour implémentation
