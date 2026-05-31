package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CityData
import com.example.ui.BarakahViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted

@Composable
fun WelcomeScreen(viewModel: BarakahViewModel) {
    var step by remember { mutableIntStateOf(1) } // 1: Language, 2: Location
    val currentLang by viewModel.appLanguage.collectAsState()
    val isAr = currentLang == "ar"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Header
            Text(
                text = if (isAr) "مرحباً بك في بركة" else "Welcome to Barakah",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (isAr) "رفيقك المسلم اليومي" else "Your Daily Muslim Companion",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(48.dp))

            when (step) {
                1 -> LanguageStep(viewModel, onNext = { step = 2 })
                2 -> LocationStep(viewModel, onFinish = { viewModel.setFirstRunComplete() })
            }
        }
    }
}

@Composable
fun LanguageStep(viewModel: BarakahViewModel, onNext: () -> Unit) {
    val currentLang by viewModel.appLanguage.collectAsState()
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "اختر لغة التطبيق / Select Language",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LanguageCard(
                label = "العربية",
                isSelected = currentLang == "ar",
                onClick = { viewModel.setAppLanguage("ar") }
            )
            LanguageCard(
                label = "English",
                isSelected = currentLang == "en",
                onClick = { viewModel.setAppLanguage("en") }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (currentLang == "ar") "التالي" else "Next",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
fun LanguageCard(label: String, isSelected: Boolean, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.size(140.dp, 100.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        ),
        border = CardDefaults.outlinedCardBorder(isSelected).let { border ->
            if (isSelected) {
                border.copy(brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
            } else {
                border
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label, 
                fontWeight = FontWeight.Bold, 
                fontSize = 20.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
 fun LocationStep(viewModel: BarakahViewModel, onFinish: () -> Unit) {
     val currentLang by viewModel.appLanguage.collectAsState()
     val isAr = currentLang == "ar"
     var query by remember { mutableStateOf("") }
     
     val locationPermissionState = rememberPermissionState(
         android.Manifest.permission.ACCESS_FINE_LOCATION
     )

     val filteredCities = if (query.isEmpty()) emptyList() else CityData.cities.filter {
         it.name.contains(query, ignoreCase = true) || it.nameAr.contains(query)
     }.take(5)

     Column(horizontalAlignment = Alignment.CenterHorizontally) {
         Icon(
             imageVector = Icons.Default.LocationOn,
             contentDescription = null,
             modifier = Modifier.size(80.dp),
             tint = MaterialTheme.colorScheme.primary
         )
         Spacer(modifier = Modifier.height(24.dp))
         Text(
             text = if (isAr) "تحديد الموقع" else "Set Your Location",
             style = MaterialTheme.typography.titleLarge,
             fontWeight = FontWeight.Bold
         )
         Text(
             text = if (isAr) "لحساب مواقيت الصلاة واتجاه القبلة بدقة" else "For accurate prayer times and Qibla",
             style = MaterialTheme.typography.bodyMedium,
             color = MaterialTheme.colorScheme.outline
         )
         
         Spacer(modifier = Modifier.height(32.dp))

         // GPS Button
         Button(
             onClick = {
                 if (!locationPermissionState.status.isGranted) {
                     locationPermissionState.launchPermissionRequest()
                 }
                 viewModel.setLocationMethod("auto")
                 onFinish()
             },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.GpsFixed, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = if (isAr) "استخدام الموقع التلقائي (GPS)" else "Use GPS (Recommended)")
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = if (isAr) "أو اختر مدينة يدوياً:" else "Or select a city manually:", style = MaterialTheme.typography.labelMedium)
        
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(if (isAr) "ابحث عن مدينتك..." else "Search for city...") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredCities) { city ->
                ElevatedCard(
                    onClick = {
                        viewModel.updateLocation(city.lat, city.lng, if (isAr) city.nameAr else city.name)
                        viewModel.setLocationMethod("manual")
                        onFinish()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(if (isAr) city.nameAr else city.name, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(if (isAr) city.countryAr else city.country) },
                        leadingContent = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
        
        if (query.isEmpty()) {
            Text(
                text = if (isAr) "يمكنك دائماً تغيير هذه الإعدادات لاحقاً" else "You can always change these later in settings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
