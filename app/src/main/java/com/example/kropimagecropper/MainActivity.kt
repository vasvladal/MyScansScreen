package com.example.kropimagecropper

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

                    // Animation states
                    val headerAlpha by animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = tween(1000),
                        label = "headerAlpha"
                    )
                    val headerScale by animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = tween(800),
                        label = "headerScale"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    ) {
                        Scaffold(
                            containerColor = Color.Transparent,
                            topBar = {
                                TopAppBar(
                                    modifier = Modifier
                                        .alpha(headerAlpha)
                                        .scale(headerScale)
                                        .padding(horizontal = 8.dp)
                                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
                                    title = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        ) {
                                            Card(
                                                modifier = Modifier.size(40.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                                ),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Image(
                                                        painter = painterResource(id = R.mipmap.ic_launcher),
                                                        contentDescription = "App Icon",
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = appName,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "v$appVersion",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                                    ),
                                    actions = {
                                        // Three dots menu button
                                        Card(
                                            modifier = Modifier.size(48.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                            )
                                        ) {
                                            IconButton(
                                                onClick = { showMenu = true },
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = stringResource(R.string.menu),
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        // Enhanced Dropdown menu
                                        DropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false },
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.surface,
                                                    RoundedCornerShape(16.dp)
                                                )
                                        ) {
                                            // Language option
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        stringResource(R.string.language),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                },
                                                onClick = {
                                                    showMenu = false
                                                    showLanguageDialog = true
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Default.Language,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                },
                                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                            )

                                            // About option
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        stringResource(R.string.about),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                },
                                                onClick = {
                                                    showMenu = false
                                                    showAboutDialog = true
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Default.Info,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                },
                                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                            )
                                        }
                                    }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier.padding(innerPadding)
                            ) {
                                Navigation(navController = navController)

                                // Enhanced Language selection dialog
                                if (showLanguageDialog) {
                                    ModernLanguageSelectionDialog(
                                        onDismiss = { showLanguageDialog = false },
                                        onLanguageSelected = { languageCode ->
                                            LanguageUtils.setAppLanguage(context, languageCode)
                                            recreate() // Restart activity to apply language changes
                                        }
                                    )
                                }

                                // Enhanced About dialog
                                if (showAboutDialog) {
                                    ModernAboutDialog(
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
}

@Composable
fun ModernLanguageSelectionDialog(
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val languages = LanguageUtils.getLanguageDisplayNames()
    val flags = mapOf(
        "en" to "🇺🇸",
        "ru" to "🇷🇺",
        "uk" to "🇺🇦",
        "ro" to "🇷🇴"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.select_language),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                languages.forEach { (code, name) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Button(
                            onClick = { onLanguageSelected(code) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            elevation = null,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = flags[code] ?: "🌐",
                                    fontSize = 24.sp,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                                Text(
                                    text = name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    stringResource(R.string.cancel),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
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
fun ModernAboutDialog(
    onDismiss: () -> Unit,
    appVersion: String,
    appName: String
) {
    val context = LocalContext.current
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
    val markwon = remember { Markwon.create(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.about),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                // Enhanced app header in about dialog
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Card(
                            modifier = Modifier.size(64.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.mipmap.ic_launcher),
                                    contentDescription = "App Icon",
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        Column {
                            Text(
                                text = appName,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Version $appVersion",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                }

                // Markdown content
                AndroidView(
                    factory = { context ->
                        TextView(context).apply {
                            setPadding(16, 16, 16, 16)
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        }
                    },
                    update = { textView ->
                        markwon.setMarkdown(textView, aboutText)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text(
                    stringResource(R.string.ok),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp)
    )
}