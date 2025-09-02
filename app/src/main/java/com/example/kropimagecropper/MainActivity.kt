package com.example.kropimagecropper

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.compose.rememberNavController
import com.example.kropimagecropper.navigation.Navigation
import com.example.kropimagecropper.ui.theme.KropImageCropperTheme
import com.example.kropimagecropper.utils.AppInfo
import com.example.kropimagecropper.utils.LanguageUtils
import io.noties.markwon.Markwon
import kotlinx.io.IOException
import android.widget.TextView

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply saved language when activity starts
        LanguageUtils.applySavedLanguage(this)

        setContent {
            KropImageCropperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    val appVersion = remember { AppInfo.getFormattedVersion(context) }

                    // Get the app name from resources and format it with the version
                    val appName = context.getString(R.string.app_name)
                    val title = "$appName: v$appVersion"

                    // State for dropdown menu
                    var showMenu by remember { mutableStateOf(false) }
                    // State for language selection dialog
                    var showLanguageDialog by remember { mutableStateOf(false) }
                    // State for about dialog
                    var showAboutDialog by remember { mutableStateOf(false) }

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(title)
                                },
                                actions = {
                                    // Three dots menu button
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = stringResource(R.string.menu)
                                        )
                                    }

                                    // Dropdown menu
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        // Language option
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.language)) },
                                            onClick = {
                                                showMenu = false
                                                showLanguageDialog = true
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Language,
                                                    contentDescription = null
                                                )
                                            }
                                        )

                                        // About option
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.about)) },
                                            onClick = {
                                                showMenu = false
                                                showAboutDialog = true
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Info,
                                                    contentDescription = null
                                                )
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            Navigation(navController = navController)

                            // Language selection dialog
                            if (showLanguageDialog) {
                                LanguageSelectionDialog(
                                    onDismiss = { showLanguageDialog = false },
                                    onLanguageSelected = { languageCode ->
                                        LanguageUtils.setAppLanguage(context, languageCode)
                                        recreate() // Restart activity to apply language changes
                                    }
                                )
                            }

                            // About dialog
                            if (showAboutDialog) {
                                AboutDialog(
                                    onDismiss = { showAboutDialog = false },
                                    appVersion = appVersion,
                                    appName = appName
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val languages = LanguageUtils.getLanguageDisplayNames()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_language)) },
        text = {
            Column {
                languages.forEach { (code, name) ->
                    Button(
                        onClick = {
                            onLanguageSelected(code)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(name)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

// Function to read markdown files from assets
private fun readMarkdownFile(context: Context, fileName: String): String {
    return try {
        context.assets.open(fileName).bufferedReader().use { it.readText() }
    } catch (e: IOException) {
        // Fallback to English if specific language file not found
        try {
            context.assets.open("about_en.md").bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            // Final fallback to string resource if English file not found
            context.getString(R.string.about_description)
        }
    }
}

@Composable
fun AboutDialog(
    onDismiss: () -> Unit,
    appVersion: String,
    appName: String
) {
    val context = LocalContext.current
    // Use the same method that's used for applying language to get the current language
    val currentLanguage = LanguageUtils.getCurrentLanguage(context)

    // Map language codes to file names
    val markdownFileName = when (currentLanguage) {
        "en" -> "about_en.md"
        "ru" -> "about_ru.md"
        "uk" -> "about_uk.md"
        "ro" -> "about_ro.md"
        else -> "about_en.md" // Default to English
    }

    val aboutText = remember { readMarkdownFile(context, markdownFileName) }

    // Create Markwon instance
    val markwon = remember { Markwon.create(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about)) },
        text = {
            Column {
                Text("$appName v$appVersion", style = MaterialTheme.typography.titleMedium)
                AndroidView(
                    factory = { context ->
                        TextView(context).apply {
                            // Set any TextView properties you want here
                        }
                    },
                    update = { textView ->
                        markwon.setMarkdown(textView, aboutText)
                    },
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}