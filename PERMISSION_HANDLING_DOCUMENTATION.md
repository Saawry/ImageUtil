# Comprehensive Permission Handling Documentation & Helper Module Guide
**Project:** GadwarePOS (Android)  
**Target SDK:** 36 | **Min SDK:** 24 | **Language:** Kotlin

---

## 1. Executive Summary

This document provides an in-depth architectural and operational breakdown of how runtime and manifest permissions are currently managed across the **GadwarePOS** application. It identifies architectural strengths, inconsistencies, bugs, and OS version edge-cases in the existing codebase, and provides a complete blueprint for designing and implementing a clean, modern, lifecycle-aware **Permission Helper Module**.

---

## 2. Declared Permissions & Capabilities Matrix

The app declares permissions across multiple modules (`app` and `dcadm`). Below is the inventory:

### 2.1 Manifest Declarations (`app/src/main/AndroidManifest.xml`)

| Permission | Protection Level | Purpose in GadwarePOS | Applicable Android Versions |
| :--- | :--- | :--- | :--- |
| `android.permission.INTERNET` | Normal | API networking, Firebase Cloud Messaging, remote sync | All |
| `android.permission.ACCESS_NETWORK_STATE` | Normal | Network connectivity checking | All |
| `android.permission.CAMERA` | Dangerous (Runtime) | Product barcode scanning, profile photo capture, product images | All |
| `android.permission.READ_CONTACTS` | Dangerous (Runtime) | Importing customer/supplier phone numbers and contacts | All |
| `android.permission.CALL_PHONE` | Dangerous (Runtime) | Direct calling customers/suppliers from Dues lists | All |
| `android.permission.POST_NOTIFICATIONS` | Dangerous (Runtime) | Push notifications (FCM), order & stock alerts | API 33+ (Tiramisu) |
| `android.permission.READ_MEDIA_IMAGES` | Dangerous (Runtime) | Picking images from gallery for products and profiles | API 33+ (Tiramisu) |
| `android.permission.READ_EXTERNAL_STORAGE` | Dangerous (Runtime) | Reading photo files from device storage | API 24–32 (`maxSdkVersion="32"`) |
| `android.permission.WRITE_EXTERNAL_STORAGE` | Dangerous (Runtime) | Saving exported reports / images to legacy storage | API 24–28 (`maxSdkVersion="28"`) |

### 2.2 Features Declared
- `android.hardware.camera` (`required="false"`)
- `android.hardware.telephony` (`required="false"`)

---

## 3. Current Architecture & Implementation Analysis

### 3.1 The Current Utility: `PermissionUtil.kt`
Located at: `com.gadware.android.store.posapp.utils.PermissionUtil`

```
┌─────────────────────────────────────────────────────────────┐
│                    PermissionUtil (Object)                  │
├─────────────────────────────────────────────────────────────┤
│ + Config (data class for dialog texts & per-permission map) │
│ + handlePermissionResult(...)                               │
│ + requestImagePermissions(...)                              │
│ + requestContactPermissions(...)                            │
│ + requestCallPermissions(...)                               │
│ + getSharedPreferences(...)                                 │
└─────────────────────────────────────────────────────────────┘
```

#### How `PermissionUtil.handlePermissionResult` Works:
1. Receives the `grantStates: Map<String, Boolean>` from the `ActivityResultLauncher`.
2. Marks `KEY_FIRST_LAUNCH = true` in SharedPreferences (`permission_prefs`).
3. If all requested permissions are granted, calls `onResult(true)`.
4. If any are denied, checks `activity.shouldShowRequestPermissionRationale(perm)`:
   - If `shouldShowRequestPermissionRationale` returns **false** (and it's not the initial ask), the permission is considered **permanently denied** ("Don't ask again").
   - Shows an `AlertDialog` with a "Settings" button (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`) directing the user to system settings.
5. Invokes `onResult(false)`.

---

## 4. Current Usage Inventory Across the Codebase

### 4.1 Activity & Fragment Implementations

| Screen / Component | File Path | Permissions Handled | Mechanism Used |
| :--- | :--- | :--- | :--- |
| **MainActivity** | `activities/MainActivity.kt` | `POST_NOTIFICATIONS` | **Legacy:** `checkSelfPermission()` + `requestPermissions(101)` + `onRequestPermissionsResult()` |
| **AddNewProduct** | `activities/AddNewProduct.kt` | `CAMERA`, `READ_EXTERNAL_STORAGE` | `ActivityResultContracts.RequestMultiplePermissions()` + `PermissionUtil` |
| **ProfileActivity** | `activities/ProfileActivity.kt` | `CAMERA`, `READ_EXTERNAL_STORAGE` | `ActivityResultContracts.RequestMultiplePermissions()` + `PermissionUtil` |
| **AddNewUserActivity** | `activities/AddNewUserActivity.kt` | `CAMERA`, `READ_EXTERNAL_STORAGE`, `READ_CONTACTS` | Two separate `RequestMultiplePermissions()` launchers + `PermissionUtil` |
| **SellActivity** | `activities/SellActivity.kt` | `READ_CONTACTS` | `RequestMultiplePermissions()` + `PermissionUtil` |
| **OrderActivity** | `activities/OrderActivity.kt` | `READ_CONTACTS` | `RequestMultiplePermissions()` + `PermissionUtil` |
| **ProfileInfoFragment** | `fragments/ProfileInfoFragment.kt` | `CAMERA`, `READ_EXTERNAL_STORAGE` | `RequestMultiplePermissions()` + `PermissionUtil` |
| **SupplierProfileInfoFragment** | `fragments/SupplierProfileInfoFragment.kt` | `CAMERA`, `READ_EXTERNAL_STORAGE` | `RequestMultiplePermissions()` + `PermissionUtil` |
| **NewExpenseFragment** | `fragments/NewExpenseFragment.kt` | `READ_CONTACTS` | `RequestMultiplePermissions()` + `PermissionUtil` |
| **DuesFragment** | `fragments/DuesFragment.kt` | `CALL_PHONE` | `RequestMultiplePermissions()` + `PermissionUtil` |

---

## 5. Critical Issues & Inconsistencies in the Current Codebase

### 🔴 1. Broken Callback Signature in `PermissionUtil`
In `PermissionUtil.kt`:
```kotlin
fun requestImagePermissions(
    launcher: ActivityResultLauncher<Array<String>>,
    context: Context,
    onResult: (Boolean) -> Unit // ❌ BUG: This parameter is completely ignored!
) {
    val PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )
    // ...
    launcher.launch(PERMISSIONS) // Callback was already fixed when registering launcher!
}
```
*Why this is a problem:* Callers pass a lambda `{ granted -> ... }` to `requestImagePermissions()`, but that lambda is never executed. The result is only delivered to the launcher's callback defined during `registerForActivityResult`.

### 🔴 2. Missing Android 13+ (API 33) Media Permission Branching
`PermissionUtil.requestImagePermissions` hardcodes:
```kotlin
Manifest.permission.READ_EXTERNAL_STORAGE,
Manifest.permission.CAMERA
```
On **Android 13+ (API 33–36)**:
- `READ_EXTERNAL_STORAGE` has no effect (and manifest sets `maxSdkVersion="32"`).
- The app requires `Manifest.permission.READ_MEDIA_IMAGES` (or Photo Picker / Android 14 `READ_MEDIA_VISUAL_USER_SELECTED`).
- Current code fails to request `READ_MEDIA_IMAGES` at runtime on Android 13+.

### 🔴 3. Mixed / Deprecated APIs
`MainActivity.kt` uses the deprecated `requestPermissions(arrayOf(permission), 101)` and `onRequestPermissionsResult()`, whereas all other screens use `androidx.activity.result.ActivityResultContracts`.

### 🔴 4. Mismatched Helper Calls (Copy-Paste Bugs)
In `OrderActivity.kt` (line 197) and `SellActivity.kt` (line 184):
```kotlin
// In OrderActivity.kt / SellActivity.kt:
binding.bottomSheetLay.selectBtn.setOnClickListener {
    // ❌ Calls requestImagePermissions for a CONTACTS action!
    PermissionUtil.requestImagePermissions(requestPermissionLauncher, this) { granted ->
        if (granted) ShowContactsDialog()
    }
}
```

### 🔴 5. Massive Boilerplate Duplication
Every single activity and fragment duplicates 25–40 lines of boilerplate initializing `registerForActivityResult`, building `PermissionUtil.Config`, obtaining `SharedPreferences`, and invoking `handlePermissionResult`.

---

## 6. Target Architecture for the New Permission Helper Module

To solve these issues, the new Permission Helper module should follow these design principles:

### Core Requirements
1. **Lifecycle Safe**: Automatically bind to `ComponentActivity` or `Fragment` before `STARTED` state (using `ActivityResultRegistry` or `LifecycleObserver`).
2. **Version-Aware Resolution**:
   - `Storage / Media`: Automatically selects `READ_MEDIA_IMAGES` (API 33+) vs `READ_EXTERNAL_STORAGE` (API < 33).
   - `Notifications`: Automatically handles `POST_NOTIFICATIONS` for API 33+ and treats as granted on API < 33.
3. **Unified Single & Multi-Permission API**:
   - Simple callback / coroutine DSL: `requestPermissions(permissions) { onGranted, onDenied, onPermanentlyDenied }`
   - Pre-check helper: `hasPermission(permission): Boolean`
4. **Customizable Rationale & Settings Redirection**:
   - Built-in customizable Material dialog or UI delegate for rationales and app settings navigation.
5. **Decoupled Architecture**: Can be extracted into a standalone `:core:permission` or `:common:permission` module.

---

## 7. Recommended Implementation Blueprint

Below is the recommended clean architectural blueprint for the new module:

### 7.1 Architecture Layers

```
:permission-helper (Module)
├── data/
│   └── PermissionPrefs.kt             // Persisting first-request flags
├── model/
│   ├── PermissionType.kt              // High-level enums (IMAGE_PICKER, CONTACTS, CALL, CAMERA, NOTIFICATIONS)
│   ├── PermissionResult.kt            // Granted, Denied, PermanentlyDenied
│   └── PermissionDialogConfig.kt      // Customizable rationale & settings dialog texts
├── resolver/
│   └── PermissionResolver.kt          // Maps PermissionType to OS Manifest strings based on Build.VERSION.SDK_INT
├── core/
│   ├── PermissionManager.kt           // Core controller attached to Activity/Fragment
│   └── PermissionLauncherHolder.kt    // Handles registerForActivityResult registration
└── extensions/
    ├── ActivityExt.kt                 // Context/Activity extension functions
    └── FragmentExt.kt                 // Fragment extension functions
```

### 7.2 Example Code Structure

#### `PermissionType.kt`
```kotlin
package com.gadware.core.permission.model

import android.Manifest
import android.os.Build

sealed class PermissionGroup {
    object ImagePicker : PermissionGroup()
    object Camera : PermissionGroup()
    object Contacts : PermissionGroup()
    object PhoneCall : PermissionGroup()
    object Notifications : PermissionGroup()
    data class Custom(val permissions: List<String>) : PermissionGroup()

    fun getManifestPermissions(): Array<String> = when (this) {
        is ImagePicker -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.CAMERA)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
            }
        }
        is Camera -> arrayOf(Manifest.permission.CAMERA)
        is Contacts -> arrayOf(Manifest.permission.READ_CONTACTS)
        is PhoneCall -> arrayOf(Manifest.permission.CALL_PHONE)
        is Notifications -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                emptyArray()
            }
        }
        is Custom -> permissions.toTypedArray()
    }
}
```

#### `PermissionHelper.kt` (Lifecycle-Aware Component)
```kotlin
package com.gadware.core.permission

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

class PermissionHelper private constructor(
    private val activityProvider: () -> ComponentActivity?,
    private val launcherProvider: (ActivityResultContracts.RequestMultiplePermissions, (Map<String, Boolean>) -> Unit) -> ActivityResultLauncher<Array<String>>
) {
    private var onResultAction: ((PermissionResult) -> Unit)? = null
    private lateinit var launcher: ActivityResultLauncher<Array<String>>

    init {
        launcher = launcherProvider(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            handleResult(results)
        }
    }

    constructor(activity: ComponentActivity) : this(
        activityProvider = { activity },
        launcherProvider = { contract, callback -> activity.registerForActivityResult(contract, callback) }
    )

    constructor(fragment: Fragment) : this(
        activityProvider = { fragment.activity as? ComponentActivity },
        launcherProvider = { contract, callback -> fragment.registerForActivityResult(contract, callback) }
    )

    fun request(group: PermissionGroup, onResult: (PermissionResult) -> Unit) {
        val permissions = group.getManifestPermissions()
        val activity = activityProvider() ?: return

        // If no runtime permissions needed (e.g. notifications on Android < 13)
        if (permissions.isEmpty() || permissions.all { PermissionChecker.isGranted(activity, it) }) {
            onResult(PermissionResult.AllGranted)
            return
        }

        this.onResultAction = onResult
        launcher.launch(permissions)
    }

    private fun handleResult(results: Map<String, Boolean>) {
        val activity = activityProvider() ?: return
        val denied = results.filter { !it.value }.keys

        if (denied.isEmpty()) {
            onResultAction?.invoke(PermissionResult.AllGranted)
            return
        }

        val permanentlyDenied = denied.filter { perm ->
            !activity.shouldShowRequestPermissionRationale(perm)
        }

        if (permanentlyDenied.isNotEmpty()) {
            onResultAction?.invoke(PermissionResult.PermanentlyDenied(permanentlyDenied.toList()))
        } else {
            onResultAction?.invoke(PermissionResult.Denied(denied.toList()))
        }
    }
}
```

---

## 8. Migration Checklist for Refactoring

When building and integrating the new permission module into GadwarePOS:

1. [ ] Create permission module/package (`com.gadware.android.store.posapp.permission` or `:core:permission`).
2. [ ] Support OS Version branching (`READ_MEDIA_IMAGES` for API 33+, `POST_NOTIFICATIONS` for API 33+).
3. [ ] Replace deprecated `requestPermissions` in `MainActivity.kt`.
4. [ ] Fix copy-paste permission bug in `OrderActivity.kt` and `SellActivity.kt`.
5. [ ] Replace repetitive boilerplate in:
   - `AddNewProduct.kt`
   - `AddNewUserActivity.kt`
   - `ProfileActivity.kt`
   - `SupplierProfileInfoFragment.kt`
   - `ProfileInfoFragment.kt`
   - `NewExpenseFragment.kt`
   - `DuesFragment.kt`
6. [ ] Delete obsolete commented-out code in `PermissionUtil.kt`.
