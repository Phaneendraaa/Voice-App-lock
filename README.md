# VoiceLock

An Android app locker you open with your voice. Pick the apps to protect; when one opens, VoiceLock covers it with a lock screen that asks you to say a phrase plus a few random digits. Everything runs on the phone. There is no backend and no account.

## How unlocking works

Both checks must pass:

1. **Right words.** Vosk (offline speech-to-text) must hear your phrase, and the random digits **exactly**. The digits change every time, so a replayed recording of an earlier unlock won't match.
2. **Right voice.** sherpa-onnx turns your speech into a voiceprint (WeSpeaker ResNet34) and compares it with the one saved at enrollment (cosine similarity, default threshold 0.60).

Fallbacks: a PIN (4-8 digits, PBKDF2) and fingerprint. After 3 failed voice tries the lock screen switches to PIN; 5 wrong PINs lock it for 30 seconds. Failed attempts are logged in Settings.

## Stack

- Kotlin, Jetpack Compose, Material 3
- Detection: `UsageStatsManager` polling in a foreground service (400 ms), lock screen as a full-screen `LockActivity`
- Voice: Vosk (`vosk-android`) and sherpa-onnx (AAR)
- Voiceprint stored encrypted with the Android Keystore (AES-GCM)

## Build

Requirements: Android Studio (or JDK 17 + Android SDK 34) and Gradle 8.7.

```bash
bash scripts/fetch_sherpa.sh        # one-time: downloads the sherpa-onnx AAR into app/libs
gradle :app:assembleDebug           # or open the folder in Android Studio and press Run
```

CI (`.github/workflows/build.yml`) does the same and uploads `app-debug.apk` as a build artifact. Open the latest run under the **Actions** tab to download it.

First launch: grant the permissions, set a PIN, then tap **Download models** (about 60 MB, once). After that the app works offline.

## Setup on the phone

1. Allow **Microphone**, **Display over other apps**, **Usage access** (and Notifications on Android 13+).
2. On Xiaomi, Oppo, Vivo, Realme, Samsung and similar: allow **autostart** and set battery to **unrestricted** for VoiceLock, or the system may stop the lock service.
3. Record your phrase 3 times in a quiet place.
4. Choose apps under **Protected apps**, then switch protection on from the home screen.

## Known limits (please read)

- **Voice is not bank-grade security.** A voiceprint can be fooled by a high-quality recording or an AI clone of your voice. The random digits stop simple replays only. Keep the PIN strong.
- **Tuning is needed.** Speaker matching depends on your voice, mic and room. If it rejects you, lower *Voice strictness* in Settings; if it lets others in, raise it. The failed-attempt log shows the match score.
- **Short flash.** Detection polls every 400 ms, so a protected app can be visible for a moment before the lock screen covers it.
- **Phone makers differ.** Background limits vary a lot by brand; test on your own phone.
- **Can be bypassed by** force-stopping or uninstalling the app, safe mode, or revoking permissions. VoiceLock does not use Device Admin.
- **VoiceLock locks itself** when protection is on, so nobody can switch it off without unlocking.
- **Play Store:** apps that cover other apps are reviewed closely. This project is meant for sideloading.
- **Demo mode** (Settings > Developer) fakes the voice check for emulators. It is not secure.

## Project layout

```
app/src/main/java/com/phani/voicelock/
  data/    Prefs, SecureStore (Keystore), AppRepository
  voice/   AudioRecorder, VoskAsr, SherpaSpeaker, RealVoiceAuthenticator,
           MockVoiceAuthenticator, ModelManager, PhraseMatcher, Challenge, VectorMath
  lock/    LockService, LockActivity, LockViewModel, LockScreen, LockState, BootReceiver
  ui/      Onboarding, Enroll, Home, Apps, Settings screens + navigation
```

## Models and licenses

Models are downloaded at runtime from their official sources, not bundled:
Vosk `vosk-model-small-en-in-0.4` (alphacephei.com) and WeSpeaker `wespeaker_en_voxceleb_resnet34.onnx` (k2-fsa/sherpa-onnx releases). Check each project's license before distributing the app commercially.
