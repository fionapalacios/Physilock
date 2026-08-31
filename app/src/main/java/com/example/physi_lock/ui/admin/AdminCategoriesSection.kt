package com.example.physi_lock.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.data.AppCategory
import com.example.physi_lock.data.AppCategoryType
import com.example.physi_lock.ui.settings.InstalledAppInfo
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.CardCream
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.DmMono
import com.example.physi_lock.ui.theme.ErrorRed
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SecondarySage

private val ChipUnselectedBg = Color(0xFFF0EEDF)

/** (category constant, short display label) -- the app's real 5-category taxonomy (used for
 *  risk scoring/reports elsewhere), not the mockup's 8-category list. See AppCategoryType. */
private data class CategoryChipSpec(val value: String, val shortLabel: String)
private val CategoryChips = listOf(
    CategoryChipSpec(AppCategoryType.SOCIAL_MEDIA, "Social"),
    CategoryChipSpec(AppCategoryType.ENTERTAINMENT, "Entertainment"),
    CategoryChipSpec(AppCategoryType.PRODUCTIVITY, "Productivity"),
    CategoryChipSpec(AppCategoryType.GAMES, "Gaming"),
    CategoryChipSpec(AppCategoryType.OTHER, "Other")
)

private fun emojiFor(appName: String): String {
    val n = appName.lowercase()
    return when {
        "instagram" in n -> "📸"
        "tiktok" in n -> "🎵"
        "youtube" in n -> "▶️"
        "facebook" in n -> "👤"
        "twitter" in n || n == "x" -> "🐦"
        "reddit" in n -> "🤖"
        "snapchat" in n -> "👻"
        "netflix" in n -> "🎬"
        "spotify" in n -> "🎧"
        "news" in n -> "📰"
        "amazon" in n -> "🛒"
        "duolingo" in n -> "🦉"
        "pubg" in n -> "🎮"
        "roblox" in n -> "🧱"
        "headspace" in n -> "🧘"
        else -> "📱"
    }
}

@Composable
fun AdminCategoriesSection(
    apps: List<InstalledAppInfo>,
    categoryEntries: List<AppCategory>,
    onSetCategory: (InstalledAppInfo, String) -> Unit,
    onRemoveCategory: (InstalledAppInfo) -> Unit,
    onAddManualApp: (String, String) -> Unit
) {
    if (apps.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading installed apps...", fontFamily = Nunito, color = MutedText)
        }
        return
    }

    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val categoryByPackage = remember(categoryEntries) { categoryEntries.associate { it.packageName to it.category } }
    val counts = remember(categoryEntries) { categoryEntries.groupingBy { it.category }.eachCount() }

    // A manually-added app (see onAddManualApp) has no real package -- it only exists because
    // of its AppCategory row -- so it wouldn't otherwise appear in the installed-apps list.
    val installedPackageNames = remember(apps) { apps.map { it.packageName }.toSet() }
    val manualApps = remember(categoryEntries, installedPackageNames) {
        categoryEntries.filter { it.packageName !in installedPackageNames }
            .map { InstalledAppInfo(packageName = it.packageName, appName = it.appName) }
    }
    val allApps = remember(apps, manualApps) { apps + manualApps }

    val filteredApps = remember(allApps, categoryByPackage, query, selectedCategory) {
        allApps.filter { app ->
            val matchesQuery = query.isBlank() || app.appName.contains(query, ignoreCase = true)
            val category = categoryByPackage[app.packageName] ?: AppCategoryType.OTHER
            val matchesCategory = selectedCategory == null || category == selectedCategory
            matchesQuery && matchesCategory
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(7.5.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(CategoryChips) { spec ->
                StatChip(count = counts[spec.value] ?: 0, label = spec.shortLabel)
            }
        }

        Spacer(modifier = Modifier.height(11.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardCream, RoundedCornerShape(15.dp))
                .border(0.8.dp, DeepOlive.copy(alpha = 0.10f), RoundedCornerShape(15.dp))
                .padding(horizontal = 11.25.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = SageAccent, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(7.5.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search apps…",
                        fontFamily = Nunito,
                        fontSize = 13.sp,
                        color = DeepOlive.copy(alpha = 0.5f)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = Nunito, fontSize = 13.sp, color = DeepOlive),
                    cursorBrush = SolidColor(DeepOlive),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(11.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(7.5.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(label = "All", selected = selectedCategory == null, onClick = { selectedCategory = null })
            }
            items(CategoryChips) { spec ->
                FilterChip(
                    label = spec.shortLabel,
                    selected = selectedCategory == spec.value,
                    onClick = { selectedCategory = spec.value }
                )
            }
        }

        Spacer(modifier = Modifier.height(11.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.5.dp)
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                AppCategoryCard(
                    app = app,
                    category = categoryByPackage[app.packageName] ?: AppCategoryType.OTHER,
                    onSetCategory = { category -> onSetCategory(app, category) },
                    onRemove = { onRemoveCategory(app) }
                )
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .border(0.8.dp, DeepOlive.copy(alpha = 0.18f), RoundedCornerShape(15.dp))
                        .clickable { showAddDialog = true }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = SageAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(7.5.dp))
                        Text(
                            text = "Add App Manually",
                            fontFamily = Nunito,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SageAccent
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddManualAppDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, category ->
                onAddManualApp(name, category)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddManualAppDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(AppCategoryType.OTHER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add App Manually") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("App name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Category", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MutedText)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CategoryChips) { spec ->
                        FilterChip(
                            label = spec.shortLabel,
                            selected = category == spec.value,
                            onClick = { category = spec.value }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), category) },
                enabled = name.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun StatChip(count: Int, label: String) {
    Column(
        modifier = Modifier
            .background(CardCream, RoundedCornerShape(15.dp))
            .border(0.8.dp, DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(15.dp))
            .padding(horizontal = 11.25.dp, vertical = 7.5.dp)
    ) {
        Text(text = "$count", fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 18.sp, color = DeepOlive)
        Text(text = label.uppercase(), fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 10.sp, color = SageAccent)
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) DeepOlive else ChipUnselectedBg, RoundedCornerShape(999.dp))
            .border(
                width = 0.8.dp,
                color = if (selected) Color.Transparent else DeepOlive.copy(alpha = 0.12f),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontFamily = Nunito,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (selected) BackgroundLight else MutedText
        )
    }
}

@Composable
private fun AppCategoryCard(
    app: InstalledAppInfo,
    category: String,
    onSetCategory: (String) -> Unit,
    onRemove: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardCream, RoundedCornerShape(15.dp))
            .border(0.8.dp, DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(15.dp))
            .padding(11.25.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emojiFor(app.appName), fontSize = 22.sp)
        Spacer(modifier = Modifier.width(11.dp))
        Text(
            text = app.appName,
            fontFamily = Nunito,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = DeepOlive,
            modifier = Modifier.weight(1f)
        )
        Box {
            Row(
                modifier = Modifier
                    .background(SecondarySage.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                    .border(0.8.dp, DeepOlive.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .clickable { menuExpanded = true }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppCategoryType.label(category),
                    fontFamily = Nunito,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = DeepOlive
                )
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = DeepOlive, modifier = Modifier.size(14.dp))
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                CategoryChips.forEach { spec ->
                    DropdownMenuItem(
                        text = { Text(spec.shortLabel) },
                        onClick = {
                            onSetCategory(spec.value)
                            menuExpanded = false
                        }
                    )
                }
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Remove ${app.appName}'s category",
                tint = ErrorRed,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
