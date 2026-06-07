package dev.barakah.app.util

fun String.localize(isAr: Boolean, useWesternInArabic: Boolean): String {
    val westernDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    val easternDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    
    // Check system preference
    val systemZeroDigit = java.text.DecimalFormatSymbols.getInstance().zeroDigit
    val systemPrefersEastern = systemZeroDigit == '٠'
    
    // App should show eastern if:
    // 1. The app is in Arabic
    // 2. The user hasn't opted for Western digits in app settings
    // 3. AND the system itself is configured to use Eastern digits
    val shouldShowEastern = isAr && !useWesternInArabic && systemPrefersEastern
    
    var result = this
    if (shouldShowEastern) {
        // Force Arabic/Eastern
        for (i in 0..9) {
            result = result.replace(westernDigits[i], easternDigits[i])
        }
    } else {
        // Force Western
        for (i in 0..9) {
            result = result.replace(easternDigits[i], westernDigits[i])
        }
    }
    return result
}

fun Int.localize(isAr: Boolean, useWesternInArabic: Boolean): String {
    return this.toString().localize(isAr, useWesternInArabic)
}

fun Long.localize(isAr: Boolean, useWesternInArabic: Boolean): String {
    return this.toString().localize(isAr, useWesternInArabic)
}
