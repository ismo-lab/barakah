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

// Automatically extract keystore from base64 string on build to prevent signing corruption across devices and GitHub actions
val keystoreFile = file("keystore.jks")
val base64File = rootProject.file("debug.keystore.base64")
if (base64File.exists()) {
  try {
    // Strip any possible whitespace, newlines, carriage returns, or non-base64 characters
    val rawContent = base64File.readText().trim()
    if (rawContent.isNotEmpty()) {
      // Use MimeDecoder to handle any line breaks (CRLF/LF) added during git/checkout operations
      val decodedBytes = Base64.getMimeDecoder().decode(rawContent)
      keystoreFile.writeBytes(decodedBytes)
      println("Dynamic keystore extraction successful: ${keystoreFile.length()} bytes")
    }
  } catch (e: Exception) {
    println("Dynamic keystore extraction failed: ${e.message}")
  }
}

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
      versionProps.setProperty("VERSION_CODE", next.toString())
      versionProps.setProperty("VERSION_NAME", "1.0.$next")
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

    versionCode = finalVersionCode
    versionName = "1.0.$finalVersionCode"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("sharedConfig") {
      storeFile = file("keystore.jks")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
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

tasks.register("downloadAdhan") {
  notCompatibleWithConfigurationCache("Uses network connection and local file references")
  doLast {
    val rawDir = file("src/main/res/raw")
    rawDir.mkdirs()
    val adhanFajrOptions = listOf(
      "https://github.com/Kiwifu/adhan-mp3/raw/refs/heads/main/Adhan_Fajr_Al_Haram_Al_Maki_(%D8%A3%D8%B0%D8%A7%D9%86_%D8%A7%D9%84%D9%81%D8%AC%D8%B1_%D8%A7%D9%84%D8%AD%D8%B1%D9%85_%D8%A7%D9%84%D9%85%D9%83%D9%8I).mp3",
      "https://github.com/Kiwifu/adhan-mp3/raw/refs/heads/main/Adhan_Fajr_Al_Haram_Al_Maki_(%D8%A3%D8%B0%D8%A7%D9%86_%D8%A7%D9%84%D9%81%D8%AC%D8%B1_%D8%A7%D9%84%D8%AD%D8%B1%D9%85_%D8%A7%D9%84%D9%85%D9%83%D9%8I).mp3".replace("%D9%8I", "%D9%8E"),
      "https://github.com/Kiwifu/adhan-mp3/raw/refs/heads/main/Adhan_Fajr_Al_Haram_Al_Maki_(%D8%A3%D8%B0%D8%A7%D9%86_%D8%A7%D9%84%D9%81%D8%AC%D8%B1_%D8%A7%D9%84%D8%AD%D8%B1%D9%85_%D8%A7%D9%84%D9%85%D9%83%D9%8I).mp3".replace("%D9%8I", "%D9%8Y"),
      "https://github.com/Kiwifu/adhan-mp3/raw/refs/heads/main/Adhan_Fajr_Al_Haram_Al_Maki_(%D8%A3%D8%B0%D8%A7%D9%86_%D8%A7%D9%84%D9%81%D8%AC%D8%B1_%D8%A7%D9%84%D8%AD%D8%B1%D9%85_%D8%A7%D9%84%D9%85%D9%83%D9%8I).mp3".replace("%D9%8I", "%D9%8A"),
      "https://github.com/Kiwifu/adhan-mp3/raw/refs/heads/main/Adhan_Fajr_Al_Haram_Al_Maki_(%D8%A3%D8%B0%D8%A7%D9%86_%D8%A7%D9%84%D9%81%D8%AC%D8%B1_%D8%A7%D9%84%D8%AD%D8%B1%D9%85_%D8%A7%D9%84%D9%85%D9%83%D9%8A).mp3"
    )
    val adhanRegularOptions = listOf(
      "https://github.com/Kiwifu/adhan-mp3/raw/refs/heads/main/Nayf_Fedah_-_Al_Haram_Al_Maki_(%D9%86%D8%A7%D9%8A%D9%81_%D9%81%D8%AF%D8%A7%D8%AD_-_%D8%A7%D9%84%D8%AD%D8%B1%D9%85_%D8%A7%D9%84%D9%85%D9%83%D9%8A).mp3",
      "https://github.com/Kiwifu/adhan-mp3/raw/refs/heads/main/Nayf_Fedah_-_Al_Haram_Al_Maki_(%D9%86%D8%A7%D9%8A%D9%81_%D9%81%D8%AF%D8%A7%D8%AD_-_%D8%A7%D9%84%D8%AD%D8%B1%D9%85_%D8%A7%D9%84%D9%85%D9%83%D9%8I).mp3",
      "https://github.com/Kiwifu/adhan-mp3/raw/refs/heads/main/Nayf_Fedah_-_Al_Haram_Al_Maki_(%D9%86%D8%A7%D9%8A%D9%81_%D9%81%D8%AF%D8%A7%D8%AD_-_%D8%A7%D9%84%D8%AD%D8%B1%D9%85_%D8%A7%D9%84%D9%85%D9%83%D9%8I).mp3".replace("%D9%8I", "%D9%8E"),
      "https://github.com/Kiwifu/adhan-mp3/raw/refs/heads/main/Nayf_Fedah_-_Al_Haram_Al_Maki_(%D9%86%D8%A7%D9%8A%D9%81_%D9%81%D8%AF%D8%A7%D8%AD_-_%D8%A7%D9%84%D8%AD%D8%B1%D9%85_%D8%A7%D9%84%D9%85%D9%83%D9%8I).mp3".replace("%D9%8I", "%D9%8A")
    )
    
    val downloads = mapOf(
      "adhan_fajr.mp3" to adhanFajrOptions,
      "adhan_regular.mp3" to adhanRegularOptions
    )
    
    for ((filename, urlOptions) in downloads) {
      val targetFile = File(rawDir, filename)
      if (!targetFile.exists() || targetFile.length() < 100000) {
        var success = false
        for (urlStr in urlOptions) {
          println("Trying to download $filename from $urlStr...")
          try {
            var currentUrlStr = urlStr
            var conn: HttpURLConnection
            var attempts = 0
            while (attempts < 5) {
              val url = URI(currentUrlStr).toURL()
              conn = url.openConnection() as HttpURLConnection
              conn.instanceFollowRedirects = true
              conn.requestMethod = "GET"
              conn.connectTimeout = 30000
              conn.readTimeout = 30000
              val status = conn.responseCode
              if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER || status == 307 || status == 308) {
                val newUrl = conn.getHeaderField("Location")
                currentUrlStr = newUrl
                attempts++
                continue
              }
              if (status == 200) {
                conn.inputStream.use { input ->
                  targetFile.outputStream().use { output ->
                    input.copyTo(output)
                  }
                }
                println("Downloaded $filename successfully! Size: ${targetFile.length()} bytes")
                success = true
              } else {
                println("HTTP status $status for $filename attempt")
              }
              break
            }
          } catch (e: Exception) {
            println("Exception while downloading $filename from $urlStr: ${e.message}")
          }
          if (success) break
        }
        if (!success) {
          println("Warning: Could not download $filename from any of the URLs.")
        }
      } else {
        println("$filename already exists with valid size (${targetFile.length()} bytes).")
      }
    }
  }
}

tasks.register("downloadTafseer") {
  notCompatibleWithConfigurationCache("Uses network connection and local file references")
  doLast {
    val dir = file("src/main/assets/quran/ar_tafseer")
    dir.mkdirs()
    for (surah in 1..20) {
      val target = File(dir, "$surah.json")
      if (!target.exists() || target.length() < 10) {
        try {
          val url = URI("https://cdn.jsdelivr.net/gh/spa5k/tafsir_api@main/tafsir/ar-tafsir-al-muyassar/$surah.json").toURL()
          val conn = url.openConnection() as HttpURLConnection
          conn.connectTimeout = 3000
          conn.readTimeout = 3000
          if (conn.responseCode in 200..299) {
            target.writeBytes(conn.inputStream.readBytes())
          }
        } catch(e: Exception) {
        }
      }
    }
  }
}

tasks.register("compressAndCacheTafseer") {
  doLast {
    println("Using offline cached or on-demand loading of Tafseer files. Skipping download during build.")
  }
}

tasks.named("preBuild") {
  dependsOn("downloadAdhan", "compressAndCacheTafseer")
}



