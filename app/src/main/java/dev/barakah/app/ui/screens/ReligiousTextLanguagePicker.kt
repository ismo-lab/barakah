package dev.barakah.app.ui.screens

import dev.barakah.app.data.UiStrings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.barakah.app.data.ReligiousLanguage
import dev.barakah.app.data.ReligiousTextLanguages
import dev.barakah.app.data.UiLanguages

/**
 * Small per-section language control. It changes only the religious text it is attached to, never
 * the interface language, which is what the app-language setting owns.
 */
@Composable
fun ReligiousTextLanguagePicker(
    labelEn: String,
    labelAr: String,
    selectedTag: String,
    options: List<ReligiousLanguage>,
    isAr: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowNone: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }
    val noneLabel = if (isAr) "بدون" else UiStrings.of("None")
    val selectedName = if (selectedTag.isEmpty()) {
        noneLabel
    } else {
        UiLanguages.byTag(selectedTag)?.nativeName
            ?: ReligiousTextLanguages.name(selectedTag, isAr)
    }

    TextButton(onClick = { showDialog = true }, modifier = modifier) {
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${if (isAr) labelAr else UiStrings.of(labelEn)}: $selectedName",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (isAr) labelAr else UiStrings.of(labelEn)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (allowNone) {
                        LanguageOptionRow(
                            label = noneLabel,
                            selected = selectedTag.isEmpty()
                        ) {
                            onSelect("")
                            showDialog = false
                        }
                    }
                    options.forEach { language ->
                        LanguageOptionRow(
                            label = UiLanguages.byTag(language.tag)?.nativeName ?: language.nameEn,
                            selected = selectedTag == language.tag
                        ) {
                            onSelect(language.tag)
                            showDialog = false
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(if (isAr) "إغلاق" else UiStrings.of("Close"))
                }
            }
        )
    }
}

@Composable
private fun LanguageOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}
