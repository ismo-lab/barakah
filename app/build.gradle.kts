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

    val versionFile = file("version.properties")
    val versionProps = Properties()
    var currentVersionCode = 1
    if (versionFile.exists()) {
      val fis = FileInputStream(versionFile)
      try {
        versionProps.load(fis)
      } finally {
        fis.close()
      }
      currentVersionCode = versionProps.getProperty("VERSION_CODE", "1").toInt()
    }

    val isBuilding = gradle.startParameter.taskNames.any { name ->
      name.contains("assemble", ignoreCase = true) || 
      name.contains("bundle", ignoreCase = true) || 
      name.contains("install", ignoreCase = true) ||
      name.contains("compile", ignoreCase = true)
    }

    val finalVersionCode = if (isBuilding) {
      val next = currentVersionCode + 1
      val major = 1
      val minor = next / 100
      val patch = next % 100
      val formattedVersionName = "$major.${minor.toString().padStart(2, '0')}.${patch.toString().padStart(2, '0')}"
      versionProps.setProperty("VERSION_CODE", next.toString())
      versionProps.setProperty("VERSION_NAME", formattedVersionName)
      val fos = FileOutputStream(versionFile)
      try {
        versionProps.store(fos, "Auto-incremented build version")
      } finally {
        fos.close()
      }
      next
    } else {
      currentVersionCode
    }

    val major = 1
    val minor = finalVersionCode / 100
    val patch = finalVersionCode % 100
    versionCode = major * 10000 + minor * 100 + patch
    versionName = "$major.${minor.toString().padStart(2, '0')}.${patch.toString().padStart(2, '0')}"

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



