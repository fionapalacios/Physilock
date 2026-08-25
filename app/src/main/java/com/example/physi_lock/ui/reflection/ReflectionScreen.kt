package com.example.physi_lock.ui.reflection

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.data.ReflectionEntry
import com.example.physi_lock.ui.components.AuthTabsBackground
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SecondarySage
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private data class MoodOption(val rating: Int, val emoji: String, val label: String)

private val moodOptions = listOf(
    MoodOption(1, "😞", "Struggling"),
    MoodOption(2, "😕", "Meh"),
    MoodOption(3, "😐", "Okay"),
    MoodOption(4, "🙂", "Good"),
    MoodOption(5, "😄", "Great")
)

private val dateDisplayFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

private fun formatDateKey(dateKey: String): String = try {
    LocalDate.parse(dateKey).format(dateDisplayFormatter)
} catch (e: Exception) {
    dateKey
}

@Composable
fun ReflectionScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReflectionViewModel = viewModel()
) {
    val todayEntry by viewModel.todayEntry.collectAsState()
    val recentEntries by viewModel.recentEntries.collectAsState()

    var moodRating by remember { mutableIntStateOf(0) }
    var answerText by remember { mutableStateOf("") }
    var hasLoadedExisting by remember { mutableStateOf(false) }
    var justSaved by remember { mutableStateOf(false) }

    // Pre-fill from today's existing entry (if any) the first time it loads, without
    // clobbering in-progress edits on every recomposition.
    LaunchedEffect(todayEntry) {
        if (!hasLoadedExisting && todayEntry != null) {
            moodRating = todayEntry!!.moodRating
            answerText = todayEntry!!.answerText
            hasLoadedExisting = true
        }
    }

    Column(modifier = modifier.fillMaxSize().background(BackgroundLight)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.79.dp, DeepOlive.copy(alpha = 0.08f))
                .padding(horizontal = 15.dp, vertical = 11.25.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.25.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DeepOlive,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = "Daily Reflection",
                fontFamily = Nunito,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 18.7.sp,
                color = DeepOlive
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 15.dp)
                .padding(top = 15.dp, bottom = 30.dp)
        ) {
            PromptCard(prompt = viewModel.todayPrompt)

            Spacer(modifier = Modifier.height(18.75.dp))

            Text(
                text = "HOW ARE YOU FEELING?",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
                color = SageAccent
            )
            Spacer(modifier = Modifier.height(11.25.dp))
            MoodPicker(selected = moodRating, onSelect = { moodRating = it; justSaved = false })

            Spacer(modifier = Modifier.height(18.75.dp))

            Text(
                text = "YOUR THOUGHTS",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
                color = SageAccent
            )
            Spacer(modifier = Modifier.height(11.25.dp))
            AnswerField(value = answerText, onValueChange = { answerText = it; justSaved = false })

            Spacer(modifier = Modifier.height(18.75.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(
                        if (moodRating > 0) DeepOlive else DeepOlive.copy(alpha = 0.35f),
                        RoundedCornerShape(15.dp)
                    )
                    .clickable(enabled = moodRating > 0) {
                        viewModel.saveReflection(moodRating, answerText)
                        justSaved = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (justSaved) "Saved ✓" else "Save Reflection",
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BackgroundLight
                )
            }

            if (recentEntries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(26.dp))
                Text(
                    text = "PAST REFLECTIONS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp,
                    color = SageAccent
                )
                Spacer(modifier = Modifier.height(11.25.dp))
                Column(verticalArrangement = Arrangement.spacedBy(7.5.dp)) {
                    recentEntries.forEach { entry -> PastReflectionRow(entry) }
                }
            }
        }
    }
}

@Composable
private fun PromptCard(prompt: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepOlive, RoundedCornerShape(22.5.dp))
            .padding(18.75.dp)
    ) {
        Text(
            text = "TODAY'S PROMPT",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp,
            color = SageAccent
        )
        Spacer(modifier = Modifier.height(7.5.dp))
        Text(
            text = "\"$prompt\"",
            fontFamily = Nunito,
            fontSize = 17.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 25.sp,
            color = BackgroundLight
        )
    }
}

@Composable
private fun MoodPicker(selected: Int, onSelect: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        moodOptions.forEach { option ->
            val isSelected = option.rating == selected
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isSelected) SageAccent.copy(alpha = 0.25f) else AuthTabsBackground,
                            CircleShape
                        )
                        .border(
                            if (isSelected) 2.dp else 0.79.dp,
                            if (isSelected) SageAccent else DeepOlive.copy(alpha = 0.08f),
                            CircleShape
                        )
                        .clickable { onSelect(option.rating) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = option.emoji, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = option.label,
                    fontFamily = Nunito,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) DeepOlive else MutedText
                )
            }
        }
    }
}

@Composable
private fun AnswerField(value: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 110.dp)
            .background(AuthTabsBackground, RoundedCornerShape(15.dp))
            .border(0.79.dp, DeepOlive.copy(alpha = 0.12f), RoundedCornerShape(15.dp))
            .padding(15.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                text = "Write a few thoughts...",
                fontFamily = Nunito,
                fontSize = 14.sp,
                color = DeepOlive.copy(alpha = 0.5f)
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontFamily = Nunito, fontSize = 14.sp, lineHeight = 21.sp, color = DeepOlive),
            cursorBrush = SolidColor(DeepOlive),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PastReflectionRow(entry: ReflectionEntry) {
    val mood = moodOptions.firstOrNull { it.rating == entry.moodRating }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AuthTabsBackground, RoundedCornerShape(15.dp))
            .border(0.79.dp, DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(15.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.25.dp)
    ) {
        Text(text = mood?.emoji ?: "🙂", fontSize = 20.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatDateKey(entry.dateKey),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SecondarySage
            )
            if (entry.answerText.isNotBlank()) {
                Text(
                    text = entry.answerText,
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    color = MutedText,
                    maxLines = 2
                )
            }
        }
    }
}
