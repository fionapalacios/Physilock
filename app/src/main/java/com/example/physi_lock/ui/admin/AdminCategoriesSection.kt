package com.example.physi_lock.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.physi_lock.data.AppCategoryType
import com.example.physi_lock.ui.settings.InstalledAppInfo
import com.example.physi_lock.ui.theme.DeepOlive

@Composable
fun AdminCategoriesSection(
    apps: List<InstalledAppInfo>,
    categories: Map<String, String>,
    onSetCategory: (InstalledAppInfo, String) -> Unit
) {
    if (apps.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading installed apps...", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(apps, key = { it.packageName }) { app ->
            val currentCategory = categories[app.packageName] ?: AppCategoryType.OTHER
            var menuExpanded by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.appName, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    Row(
                        modifier = Modifier.clickable { menuExpanded = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppCategoryType.label(currentCategory),
                            color = DeepOlive,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = DeepOlive)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        AppCategoryType.all.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(AppCategoryType.label(category)) },
                                onClick = {
                                    onSetCategory(app, category)
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            HorizontalDivider()
        }
    }
}
