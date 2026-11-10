# Release checklist — Marble Frog 0.4.0

## Prepared in the repository

- [x] App launcher icon (legacy vector and Android 8+ adaptive icon).
- [x] App label set to Marble Frog.
- [x] Store listing metadata draft in Russian and English.
- [x] Privacy policy in Markdown and HTML.
- [x] Privacy policy readable inside the app (Settings → Privacy Policy).
- [x] Android cloud backup and device-transfer of saved SharedPreferences disabled.
- [x] targetSdk is Android 16 / API 36.
- [x] Release build uses R8 minification.
- [x] CI builds debug APK, tests and an unsigned release AAB.

## Still requires the developer / Play Console

- [ ] Add a public support email to the privacy-policy files and Play Console contact fields.
- [ ] In repository Settings → Pages select GitHub Actions as the source; the deploy-privacy-policy workflow will publish docs/privacy-policy.html as the page root. Verify the generated HTTPS URL is globally accessible without sign-in or geofencing.
- [ ] Export the final 512 × 512 PNG store icon and 1024 × 500 feature graphic; confirm current requirements in Play Console.
- [ ] Capture genuine screenshots from the actual release build on supported phones/tablets.
- [ ] Test on physical low/mid-range and newer Android devices; check frame stability, pause/resume, background/foreground and small displays.
- [ ] Create and back up a private upload keystore. Never commit the keystore or passwords to Git.
- [ ] Sign the AAB with the upload key. The CI AAB is unsigned and cannot be uploaded as-is.
- [ ] Complete Play Console app content, Data safety, content rating, target audience and ads declaration. The Data safety form and privacy-policy URL are required even when the app does not collect user data.
- [ ] Create a closed test and invite testers if the developer account is subject to that requirement. For personal developer accounts created after 13 November 2023, Google currently requires at least 12 testers opted in continuously for 14 days before applying for production access.
- [ ] Submit the signed build for review and publish only after approval.

## Signing the AAB locally

1. Download the CI artifact miniproject-release-aab.
2. In Android Studio select **Build → Generate Signed Bundle / APK**.
3. Choose **Android App Bundle** and create or select a private upload key.
4. Store the keystore and passwords outside the repository; keep a secure backup.
5. Generate the signed release bundle and upload that file to Play Console.

The repository does not contain signing keys or passwords. The CI AAB is intentionally unsigned.

## Screenshot capture guide

Capture gameplay before a match, the rollback animation, menu and settings in both languages, pause and level-complete screens. Use a device screenshot or Android Studio emulator screenshot. Ensure images reflect the real build and contain no private notifications or account details.
