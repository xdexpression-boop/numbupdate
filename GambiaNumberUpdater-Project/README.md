# Gambia Number Updater

Scans your phone contacts for old-format **7-digit** Gambia numbers and adds
your chosen 2-digit prefix, skipping anything already 9+ digits or already
international. Backs up original numbers to a CSV before changing anything.

I can't compile the APK myself (this environment has no Android build tools
and no internet access to Google's Android servers). But this folder is a
complete, ready-to-build project two ways — pick whichever is easier for you.

## Option A — Let GitHub build it for you (no software install)

1. Go to https://github.com and make a free account if you don't have one.
2. Click **New repository** (name it anything, e.g. `gambia-number-updater`),
   keep it Public or Private, don't add a README, click Create.
3. On the new repo page, click **uploading an existing file**, then drag in
   **every file and folder from this zip** (keep the folder structure —
   `.github/workflows/build-apk.yml` must stay at that exact path). Commit.
4. Click the **Actions** tab at the top of the repo. You should see a
   "Build APK" run start automatically (takes ~2-3 minutes).
5. When it finishes (green checkmark), click into that run, scroll to
   **Artifacts**, and download **gambia-number-updater-apk** — it's a zip
   containing `app-debug.apk`.
6. Unzip it, transfer `app-debug.apk` to your phone (email it to yourself,
   Google Drive, or USB), open it on your phone, and tap Install (allow
   "install unknown apps" for whichever app you opened it with, if asked).

That's it — no Android Studio, nothing installed on your computer.

## Option B — Build locally with Android Studio

1. Install Android Studio (free): https://developer.android.com/studio
2. **Open** this folder directly as a project (File → Open).
3. Let it sync (first time takes a few minutes, downloads its own build tools).
4. Menu: `Build` → `Build Bundle(s)/APK(s)` → `Build APK(s)`.
5. Find the file at `app/build/outputs/apk/debug/app-debug.apk`, transfer it
   to your phone, and install it the same way as in Option A step 6.

## Using the app once installed

1. Open it, tap **Allow** when it asks for contacts permission.
2. Type the 2-digit prefix you're adding (e.g. `20`).
3. Tap **Scan Contacts** — lists every contact with a plain 7-digit number,
   old → new.
4. Uncheck any you don't want touched (everything is checked by default).
5. Tap **Update Selected Contacts**. It saves a backup CSV first, then
   applies the change, and tells you how many succeeded.

Backup file location on your phone:
`Android/data/com.example.gambianumberupdater/files/gambia_number_backup_<date>.csv`

## Before you run it on real contacts
I found solid evidence PURA (Gambia's regulator) is moving from 7-digit to
9-digit numbers, but couldn't confirm the exact two digits being assigned —
please verify that with your operator (Africell/QCell/Gamtel/Comium) or PURA
directly before applying it to your contact list, since there's no in-app
undo — only the backup CSV.
