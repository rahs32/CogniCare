# CogniCare

**AI-powered assistive Android app concept for autistic individuals**  
Team: **AIRA** | Theme: **Swasth & Samavesh Bharat**

## Overview
CogniCare explores multilingual AAC communication, structured routines, sensory awareness, medical report organization, and caregiver-oriented assistance. This repository is an **Android prototype source project**, not a medically validated or production-ready product.

## What's in this repository?
The supplied Kotlin / Jetpack Compose project includes source files for:
- Onboarding and personalized user preferences
- AAC interface (`AacTab.kt`)
- Sensor monitoring (`MonitorTab.kt`, `SensorEnvironmentMonitor.kt`)
- Reports (`ReportsTab.kt`)
- SOS interface (`SosTab.kt`)
- Room-based local data storage
- Gemini API integration with a demonstration fallback

The existence of these modules does **not** mean all proposed features are complete or tested. The original pitch also describes future capabilities, including healthcare-professional connectivity and wearable integration, which should not be assumed to be implemented.

## Setup
1. Install **Android Studio**, the Android SDK, and a compatible JDK (project targets Java 11 bytecode).
2. Open the `CogniCare-main` directory as an Android Studio project.
3. Let Android Studio sync the Gradle files and install any requested SDK packages.
4. The Gradle wrapper configuration specifies **Gradle 9.3.1**. The uploaded archive does not include `gradlew`, `gradlew.bat` or `gradle-wrapper.jar`; use Android Studio's configured Gradle installation or generate a wrapper locally with a compatible Gradle installation.
5. Copy `.env.example` to `.env` if you need to configure the AI integration. Replace the placeholder with your own key. **Never commit `.env`.**
6. Build the `app` module and launch it on an Android emulator or device (Android 7.0 / API 24 or newer).

> **Security:** The prototype reads a Gemini key through `BuildConfig`. Keys shipped inside an Android app can be extracted. A production deployment should use a secure backend instead of embedding a privileged API key.

## Tests
Example unit, Robolectric, screenshot and instrumented test sources are included under `app/src/test/` and `app/src/androidTest/`. After Gradle setup, run the relevant test tasks from Android Studio. **No test execution or successful build is claimed here.**

## Documentation
- [Technical architecture](docs/architecture.md)
- [Hackathon submission checklist](docs/hackathon-submission.md)
- [Fictional sample routine](sample-data/demo_routine.json)

## Privacy and accessibility
Do not upload real medical data or secrets. Sensor readings are not a diagnosis, AI outputs require human review for health-related use, and SOS features are not a replacement for emergency services.

## License
See [LICENSE](LICENSE). The team must confirm it has rights to license all included code and assets under MIT.
