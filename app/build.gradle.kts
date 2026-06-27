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
      val formattedVersionName = "$major.$minor.${patch.toString().padStart(2, '0')}"
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
    versionCode = finalVersionCode
    versionName = "$major.$minor.${patch.toString().padStart(2, '0')}"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("sharedConfig") {
      val keystoreFile = file("keystore.jks")
      if (!keystoreFile.exists()) {
        try {
          val embeddedBase64 = """
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
          """.trimIndent().replace("\n", "").replace("\r", "").replace(" ", "")
          val decodedBytes = Base64.getDecoder().decode(embeddedBase64)
          keystoreFile.writeBytes(decodedBytes)
          println("Dynamic embedded keystore extraction successful: ${keystoreFile.length()} bytes")
        } catch (e: Exception) {
          println("Dynamic embedded keystore extraction failed: ${e.message}")
        }
      }
      
      storeFile = keystoreFile
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
  dependsOn("downloadAdhan", "downloadArabicTafseer", "downloadEnglishTafseer")
}



