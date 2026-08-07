import java.net.URL
import java.net.HttpURLConnection
import java.net.URI
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Properties
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Base64
import java.io.File
import java.util.zip.GZIPOutputStream

// Dynamic keystore extraction happens on-demand during configuration phase in signingConfigs below

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
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
val buildNumber = versionProps.getProperty("BUILD_NUMBER", "1004").toInt()

// Standard Android Version Code calculation formula:
// MAJOR * 100000 + MINOR * 10000 + PATCH * 100 + BUILD_NUMBER
// Guarantees strictly monotonically increasing integer versionCode (>= 101004)
val currentVersionCode = majorVersion * 100000 + minorVersion * 10000 + patchVersion * 100 + buildNumber
val currentVersionName = "$majorVersion.$minorVersion.$patchVersion"

android {
  namespace = "dev.barakah.app"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  androidResources {
    noCompress += "json"
  }

  defaultConfig {
    applicationId = "dev.barakah.app"
    minSdk = 24
    targetSdk = 36

    versionCode = currentVersionCode
    versionName = currentVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("sharedConfig") {
      val keystoreFile = file("keystore.jks")
      val rootKeystore = rootProject.file("debug.keystore")
      val base64File = rootProject.file("debug.keystore.base64")
      
      if (rootKeystore.exists()) {
        try {
          rootKeystore.copyTo(keystoreFile, overwrite = true)
          println("Dynamic keystore copy successful from debug.keystore: ${keystoreFile.length()} bytes")
        } catch (e: Exception) {
          println("Dynamic keystore copy from debug.keystore failed: ${e.message}")
        }
      } else if (base64File.exists()) {
        try {
          val rawContent = base64File.readText().trim()
          if (rawContent.isNotEmpty()) {
            val decodedBytes = Base64.getMimeDecoder().decode(rawContent)
            keystoreFile.writeBytes(decodedBytes)
            println("Dynamic keystore extraction successful from debug.keystore.base64: ${keystoreFile.length()} bytes")
          }
        } catch (e: Exception) {
          println("Dynamic keystore extraction from debug.keystore.base64 failed: ${e.message}")
        }
      }

      if (!keystoreFile.exists()) {
        try {
          println("WARNING: debug.keystore.base64 not found or extraction failed. Generating fallback debug keystore...")
          val process = ProcessBuilder(
            "keytool", "-genkey", "-v",
            "-keystore", keystoreFile.absolutePath,
            "-storepass", "android",
            "-alias", "androiddebugkey",
            "-keypass", "android",
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "10000",
            "-dname", "CN=Android Debug,O=Android,C=US"
          ).start()
          val exitCode = process.waitFor()
          if (exitCode == 0) {
            println("Fallback debug keystore generated successfully: ${keystoreFile.length()} bytes")
          } else {
            println("Fallback debug keystore generation failed with exit code $exitCode")
          }
        } catch (e: Exception) {
          println("Failed to generate fallback debug keystore: ${e.message}")
        }
      }
      
      storeFile = keystoreFile
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
      isV1SigningEnabled = true
      isV2SigningEnabled = true
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("sharedConfig")
    }
    debug {
      signingConfig = signingConfigs.getByName("sharedConfig")
    }
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

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
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
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.play.services.location)
  implementation(libs.retrofit)
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
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register("downloadHisn") {
  notCompatibleWithConfigurationCache("Uses network connection and local file references")
  doLast {
    val dest = file("src/main/res/raw/hisn_en.json")
    dest.parentFile.mkdirs()
    println("Downloading Hisn data to ${dest.absolutePath}...")
    val url = URI("https://raw.githubusercontent.com/4thel00z/hadith.json/refs/heads/master/hisnulmuslim/hisnulmuslim.json").toURL()
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 30000
    conn.readTimeout = 30000
    if (conn.responseCode == 200) {
      conn.inputStream.use { input ->
        dest.outputStream().use { output ->
          input.copyTo(output)
        }
      }
      println("Hisn data downloaded successfully! Size: ${dest.length()} bytes")
      println("FIRST 1000 CHARS:")
      val firstChars = dest.readText().take(1500)
      println(firstChars)
    } else {
      throw GradleException("Failed to download Hisn data. HTTP Response Code: ${conn.responseCode}")
    }
  }
}

tasks.register("downloadArabicTafseer") {
  notCompatibleWithConfigurationCache("Uses network connection and local file references")
  doLast {
    val dest = file("src/main/assets/quran/tafseer_ar.json")
    if (dest.exists() && dest.length() > 100000) {
      println("Arabic Tafseer data already exists, skipping download.")
      return@doLast
    }
    dest.parentFile.mkdirs()
    println("Downloading Arabic Tafseer data to ${dest.absolutePath}...")
    val url = URI("https://github.com/00AhmedMokhtar00/QuranTafseer-ar-json/raw/refs/heads/master/tafseer.json").toURL()
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 30000
    conn.readTimeout = 30000
    if (conn.responseCode == 200) {
      conn.inputStream.use { input ->
        dest.outputStream().use { output ->
          input.copyTo(output)
        }
      }
      println("Arabic Tafseer data downloaded successfully! Size: ${dest.length()} bytes")
    } else {
      println("Failed to download Arabic Tafseer data. HTTP Response Code: ${conn.responseCode}")
    }
  }
}

tasks.register("downloadEnglishTafseer") {
  notCompatibleWithConfigurationCache("Uses network connection and local file references")
  doLast {
    val dir = file("src/main/assets/quran/en_tafseer")
    dir.mkdirs()
    println("Checking English Tafseer files (1..114) in ${dir.absolutePath}...")
    var localCount = 0
    for (surah in 1..114) {
      val target = File(dir, "$surah.json")
      if (target.exists() && target.length() > 100) {
        localCount++
        continue
      }
      try {
        println("Downloading English Tafseer for surah $surah...")
        val url = URI("https://cdn.jsdelivr.net/gh/spa5k/tafsir_api@main/tafsir/en-al-jalalayn/$surah.json").toURL()
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        if (conn.responseCode == 200) {
          conn.inputStream.use { input ->
            target.outputStream().use { output ->
              input.copyTo(output)
            }
          }
          localCount++
        } else {
          println("Failed to download English Tafseer for surah $surah. HTTP Code: ${conn.responseCode}")
        }
      } catch (e: Exception) {
        println("Error downloading English Tafseer for surah $surah: ${e.message}")
      }
    }
    println("English Tafseer files update complete. Total of $localCount/114 files downloaded and ready offline.")
  }
}

tasks.named("preBuild") {
  dependsOn("downloadArabicTafseer", "downloadEnglishTafseer")
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
    val newName = "$maj.$min.$pat"

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
    val newName = "$maj.$min.$pat"

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
    val newName = "$maj.$min.$pat"

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
    println("Signing Config: sharedConfig (Keystore: app/keystore.jks)")
    println("================================================")
  }
}




