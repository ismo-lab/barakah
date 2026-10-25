import java.util.Properties
import java.util.Base64
import java.io.File

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
}

// Load version configuration cleanly from version.properties without configuration-phase side effects
val versionFile = file("version.properties")
val versionProps = Properties().apply {
  if (versionFile.exists()) {
    versionFile.inputStream().use { load(it) }
  }
}

val majorVersion = versionProps.getProperty("MAJOR", "1").toInt()
val minorVersion = versionProps.getProperty("MINOR", "0").toInt()
val patchVersion = versionProps.getProperty("PATCH", "0").toInt()
val buildNumber = versionProps.getProperty("BUILD_NUMBER", "1006").toInt()
// A given commit has to produce the same version every time it is built: F-Droid rebuilds from the
// tag and refuses a version code it has already published, and the tag has to name what it points
// at. So version.properties is the only input a release build reads.
//
// Releasing means bumping those numbers, committing, and tagging that commit with
// v<MAJOR>.<MINOR>.<PATCH>.<BUILD_NUMBER>. The workflows refuse a tag that does not match. The
// -PbarakahVersionName override exists only for the test-release workflow's staging APK.
val signingStorePassword = System.getenv("BARAKAH_KEYSTORE_PASSWORD") ?: "android"
val signingKeyAlias = System.getenv("BARAKAH_KEY_ALIAS") ?: "androiddebugkey"
val signingKeyPassword = System.getenv("BARAKAH_KEY_PASSWORD") ?: "android"

val currentVersionCode = (findProperty("barakahVersionCode") as String?)?.toIntOrNull()
  ?: (majorVersion * 100000 + minorVersion * 10000 + patchVersion * 100 + buildNumber)
val currentVersionName = (findProperty("barakahVersionName") as String?)?.takeIf { it.isNotEmpty() }
  ?: "$majorVersion.$minorVersion.$patchVersion.$buildNumber"

// F-Droid signs the finished APK itself, so a build can opt out with -PsigningDisabled and never
// looks for a key at all. Otherwise the keystore is materialised in the build directory instead of
// the source tree; a checked-in app/keystore.jks still wins, because CI decodes the release key
// there before Gradle runs.
val signingDisabled = providers.gradleProperty("signingDisabled").isPresent
val ciKeystore = file("keystore.jks")
val localKeystore = layout.buildDirectory.file("signing/barakah.keystore").get().asFile
val activeKeystore = if (ciKeystore.exists()) ciKeystore else localKeystore

// Room writes the schema of every database version to app/schemas. Checking those in is what makes
// the next version migratable, and it turns an entity change without a version bump into a build
// failure instead of a silent wipe of the user's bookmarks, counts and reading position.
ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}

android {
  namespace = "dev.barakah.app"
  compileSdk = 37

  defaultConfig {
    applicationId = "dev.barakah.app"
    minSdk = 24
    targetSdk = 36

    versionCode = currentVersionCode
    versionName = currentVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Null when something else will sign the APK, which is the case on F-Droid's buildserver.
  val sharedSigning = if (signingDisabled) {
    null
  } else {
    signingConfigs.create("sharedConfig") {
      // The keystore file is prepared before packaging rather than here: CI decodes the
      // secret in a workflow step and the prepareSigningKeystore task below covers local
      // builds. Doing it during configuration meant writing files and shelling out to
      // keytool on every sync.
      storeFile = activeKeystore
      storePassword = signingStorePassword
      keyAlias = signingKeyAlias
      keyPassword = signingKeyPassword
      enableV1Signing = true
      enableV2Signing = true
      enableV3Signing = true
      enableV4Signing = true
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      sharedSigning?.let { signingConfig = it }
    }
    debug {
      sharedSigning?.let { signingConfig = it }
    }
    // Test-only variant: installs beside production, keeps the same signing key so it can
    // update itself, and never ships from main.
    create("staging") {
      initWith(getByName("release"))
      applicationIdSuffix = ".test"
      versionNameSuffix = "-test"
      sharedSigning?.let { signingConfig = it }
    }
  }
  lint {
    // These four only ever say that a newer version exists somewhere, which is true every week
    // and is not something a build should fail or nag about: Dependabot belongs on the first
    // three, and Play says the same thing about the target API once a year.
    disable += setOf(
      "GradleDependency",
      "NewerVersionAvailable",
      "AndroidGradlePluginVersion",
      "OldTargetApi",
    )
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

/**
 * Materialises the keystore both build variants are signed with.
 *
 * In CI the release key is decoded to app/keystore.jks by the workflow before Gradle runs, so this
 * task finds the file already there and does nothing. A local build falls back to
 * debug.keystore.base64, then debug.keystore, and only generates a throwaway key when neither
 * exists - such an APK will not install over a build from another machine, which is why it says so
 * loudly. Anything generated goes to build/signing/ so a checkout stays clean.
 *
 * Skipped entirely under -PsigningDisabled, which is how F-Droid builds it: fdroidserver signs the
 * finished APK with its own key and does not use this project's keystore.
 */
val prepareSigningKeystore = tasks.register("prepareSigningKeystore") {
  group = "signing"
  description = "Writes the signing keystore unless the build is signed elsewhere."

  val keystoreFile = activeKeystore
  val base64File = rootProject.file("debug.keystore.base64")
  val rootKeystore = rootProject.file("debug.keystore")
  outputs.file(keystoreFile)
  onlyIf { !signingDisabled }

  doLast {
    keystoreFile.parentFile?.mkdirs()
    if (keystoreFile.exists() && keystoreFile.length() > 0L) {
      println("Signing keystore already present at ${keystoreFile.absolutePath}")
      return@doLast
    }

    if (base64File.exists() && base64File.length() > 0L) {
      try {
        keystoreFile.writeBytes(Base64.getMimeDecoder().decode(base64File.readText().trim()))
        println("Signing keystore written from debug.keystore.base64 (${keystoreFile.length()} bytes)")
        return@doLast
      } catch (e: Exception) {
        println("Could not decode debug.keystore.base64: ${e.message}")
      }
    }

    if (rootKeystore.exists() && rootKeystore.length() > 0L) {
      rootKeystore.copyTo(keystoreFile, overwrite = true)
      println("Signing keystore copied from debug.keystore (${keystoreFile.length()} bytes)")
      return@doLast
    }

    println(
      """
      WARNING: neither debug.keystore.base64 nor debug.keystore is present, so a throwaway
      key is being generated at ${keystoreFile.absolutePath}. This APK will NOT install over a
      build made anywhere else, because Android rejects an update whose signing key changed.
      """.trimIndent()
    )
    val exitCode = ProcessBuilder(
      "keytool", "-genkey", "-v",
      "-keystore", keystoreFile.absolutePath,
      "-storepass", "android",
      "-alias", "androiddebugkey",
      "-keypass", "android",
      "-keyalg", "RSA",
      "-keysize", "2048",
      "-validity", "10000",
      "-dname", "CN=Android Debug,O=Android,C=US"
    ).inheritIO().start().waitFor()

    if (exitCode != 0) {
      throw GradleException("keytool could not generate a fallback keystore (exit code $exitCode)")
    }
  }
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.glance.appwidget)
  implementation(libs.androidx.glance.material3)
  // The widgets read Glance's Preferences type directly, so declare what they use
  // instead of leaning on Glance's transitive copy.
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
}

tasks.named("preBuild") {
  dependsOn(prepareSigningKeystore)
}

// Tasks for professional Version Management as per official Android documentation
tasks.register("incrementBuildNumber") {
  group = "versioning"
  description = "Increments the BUILD_NUMBER in version.properties and updates VERSION_CODE/VERSION_NAME."
  doLast {
    val props = Properties()
    if (versionFile.exists()) {
      versionFile.inputStream().use { props.load(it) }
    }
    val maj = props.getProperty("MAJOR", "1").toInt()
    val min = props.getProperty("MINOR", "0").toInt()
    val pat = props.getProperty("PATCH", "0").toInt()
    val bld = props.getProperty("BUILD_NUMBER", "1004").toInt() + 1

    val newCode = maj * 100000 + min * 10000 + pat * 100 + bld
    val newName = "$maj.$min.$pat.$bld"

    props.setProperty("MAJOR", maj.toString())
    props.setProperty("MINOR", min.toString())
    props.setProperty("PATCH", pat.toString())
    props.setProperty("BUILD_NUMBER", bld.toString())
    props.setProperty("VERSION_CODE", newCode.toString())
    props.setProperty("VERSION_NAME", newName)

    versionFile.outputStream().use { 
      props.store(it, "Updated by incrementBuildNumber task") 
    }
    println("Updated Version: Code=$newCode ($newName Build #$bld)")
  }
}

tasks.register("incrementMinorVersion") {
  group = "versioning"
  description = "Bumps MINOR version, resets PATCH to 0 and increments BUILD_NUMBER."
  doLast {
    val props = Properties()
    if (versionFile.exists()) {
      versionFile.inputStream().use { props.load(it) }
    }
    val maj = props.getProperty("MAJOR", "1").toInt()
    val min = props.getProperty("MINOR", "0").toInt() + 1
    val pat = 0
    val bld = props.getProperty("BUILD_NUMBER", "1004").toInt() + 1

    val newCode = maj * 100000 + min * 10000 + pat * 100 + bld
    val newName = "$maj.$min.$pat.$bld"

    props.setProperty("MAJOR", maj.toString())
    props.setProperty("MINOR", min.toString())
    props.setProperty("PATCH", pat.toString())
    props.setProperty("BUILD_NUMBER", bld.toString())
    props.setProperty("VERSION_CODE", newCode.toString())
    props.setProperty("VERSION_NAME", newName)

    versionFile.outputStream().use { 
      props.store(it, "Updated by incrementMinorVersion task") 
    }
    println("Updated Minor Version: Code=$newCode ($newName Build #$bld)")
  }
}

tasks.register("incrementMajorVersion") {
  group = "versioning"
  description = "Bumps MAJOR version, resets MINOR and PATCH to 0, and increments BUILD_NUMBER."
  doLast {
    val props = Properties()
    if (versionFile.exists()) {
      versionFile.inputStream().use { props.load(it) }
    }
    val maj = props.getProperty("MAJOR", "1").toInt() + 1
    val min = 0
    val pat = 0
    val bld = props.getProperty("BUILD_NUMBER", "1004").toInt() + 1

    val newCode = maj * 100000 + min * 10000 + pat * 100 + bld
    val newName = "$maj.$min.$pat.$bld"

    props.setProperty("MAJOR", maj.toString())
    props.setProperty("MINOR", min.toString())
    props.setProperty("PATCH", pat.toString())
    props.setProperty("BUILD_NUMBER", bld.toString())
    props.setProperty("VERSION_CODE", newCode.toString())
    props.setProperty("VERSION_NAME", newName)

    versionFile.outputStream().use { 
      props.store(it, "Updated by incrementMajorVersion task") 
    }
    println("Updated Major Version: Code=$newCode ($newName Build #$bld)")
  }
}

tasks.register("printVersion") {
  group = "versioning"
  description = "Prints current version info"
  doLast {
    println("================================================")
    println("Application ID: dev.barakah.app")
    println("Version Name:   $currentVersionName")
    println("Version Code:   $currentVersionCode")
    println("Major: $majorVersion, Minor: $minorVersion, Patch: $patchVersion, Build: $buildNumber")
    println("Signing: ${if (signingDisabled) "disabled (signed elsewhere)" else activeKeystore.absolutePath}")
    println("================================================")
  }
}




