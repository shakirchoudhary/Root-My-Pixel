# App Assets

This directory contains bundled payload files shipped inside the APK.

## Required files

### profiles.json
The device profile feed. Maps each supported Pixel firmware to its asset paths.

### exploits/*.so
Exploit payloads (CVE-2026-43499 APP_PAYLOAD variant), one per target. These
are build outputs, not sources: `build-all.sh` regenerates every entry in its
TARGETS list from the payloads submodule, and CI runs it before the Gradle
build, so a new target does not need a binary committed here.

To build one by hand:
```
make TARGET=tegu-CP2A.260705.006 ANDROID_NDK_HOME=... release
```
The resulting `cve-2026-43499-app.release.so` goes to `exploits/<profileId>.so`.

### ksud/ksud
The ReSukiSU late-load binary, downloaded from official ReSukiSU releases.

One binary covers every KMI. It embeds a `kernelsu.ko` per KMI
(`android12-5.10` through `android16-6.12`) and selects between them from the
`--kmi` it is passed at late-load, which is what a profile's `kmi` field
supplies. This used to be shipped as one file per KMI; those copies were
byte-identical, so they were merged into this one.

## Adding a new target

1. Add the target profile to `profiles.json`
2. Add the target to `TARGETS` in `build-all.sh` — CI builds the .so from there
3. Set the profile's `kmi` to the target's kernel KMI, so late-load picks the
   right `kernelsu.ko` out of `ksud/ksud`
