package com.example.physi_lock.ui.reflection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.physi_lock.data.entity.ReflectionEntry
import com.example.physi_lock.ui.components.AuthTabsBackground
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.CardCream
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.DmMono
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent
import com.example.physi_lock.ui.theme.SecondarySage
import com.example.physi_lock.ui.theme.TertiaryTan

private enum class ReflectionTab { TODAY, HISTORY }

@Composable
fun ReflectionScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReflectionViewModel = viewModel()
) {
    val todayEntry by viewModel.todayEntry.collectAsState()
    val recentEntries by viewModel.recentEntries.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()

    var tab by remember { mutableStateOf(ReflectionTab.TODAY) }
    var currentStep by remember { mutableIntStateOf(0) }
    var answers by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var justSubmitted by remember { mutableStateOf(false) }

    val alreadyAnsweredToday = todayEntry != null
    val showSubmittedView = justSubmitted || (alreadyAnsweredToday && currentStep == 0 && answers.isEmpty())

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
                color = DeepOlive,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = SageAccent,
                modifier = Modifier.size(18.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp)
                .padding(top = 11.dp)
                .background(CardCream, RoundedCornerShape(13.dp))
                .padding(4.dp)
        ) {
            TabButton(
                label = "Today's Reflection",
                selected = tab == ReflectionTab.TODAY,
                onClick = { tab = ReflectionTab.TODAY },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                label = "History",
                selected = tab == ReflectionTab.HISTORY,
                onClick = { tab = ReflectionTab.HISTORY },
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 15.dp)
                .padding(top = 15.dp, bottom = 30.dp)
        ) {
            when {
                tab == ReflectionTab.HISTORY -> HistoryContent(recentEntries, currentStreak)
                showSubmittedView -> SubmittedContent(
                    entry = todayEntry,
                    justAnsweredNow = answers,
                    currentStreak = currentStreak
                )
                else -> TodayWizard(
                    currentStep = currentStep,
                    answers = answers,
                    onAnswer = { id, value -> answers = answers + (id to value) },
                    onBack = { if (currentStep > 0) currentStep -= 1 },
                    onNext = { if (currentStep < REFLECTION_PROMPTS.size - 1) currentStep += 1 },
                    onSubmit = {
                        viewModel.saveReflection(answers)
                        justSubmitted = true
                    }
                )
            }
        }
    }
}

@Composable
private fun TabButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(if (selected) DeepOlive else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = Nunito,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) BackgroundLight else MutedText
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodayWizard(
    currentStep: Int,
    answers: Map<String, String>,
    onAnswer: (String, String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit
) {
    val prompt = REFLECTION_PROMPTS[currentStep]
    val answered = answers[prompt.id] != null
    val allAnswered = REFLECTION_PROMPTS.all { answers[it.id] != null }

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        REFLECTION_PROMPTS.forEachIndexed { i, _ ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .height(8.dp)
                    .width(if (i == currentStep) 20.dp else 8.dp)
                    .background(
                        when {
                            i < currentStep -> SageAccent
                            i == currentStep -> DeepOlive
                            else -> TertiaryTan
                        },
                        RoundedCornerShape(50)
                    )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepOlive, RoundedCornerShape(22.5.dp))
            .padding(18.75.dp)
    ) {
        Text(
            text = "${currentStep + 1} / ${REFLECTION_PROMPTS.size}",
            fontFamily = DmMono,
            fontSize = 12.sp,
            color = SageAccent
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = prompt.question,
            fontFamily = Nunito,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 22.sp,
            color = BackgroundLight
        )
    }

    Spacer(modifier = Modifier.height(15.dp))

    when (prompt.type) {
        PromptType.SCALE -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            prompt.options.forEach { option -> OptionRow(option, answers[prompt.id] == option) { onAnswer(prompt.id, option) } }
        }
        PromptType.CHOICE -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            prompt.options.forEach { option -> OptionChip(option, answers[prompt.id] == option) { onAnswer(prompt.id, option) } }
        }
        PromptType.TEXT -> TextAnswerField(
            value = answers[prompt.id].orEmpty(),
            placeholder = prompt.placeholder,
            onValueChange = { onAnswer(prompt.id, it) }
        )
    }

    Spacer(modifier = Modifier.height(22.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (currentStep > 0) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(CardCream, RoundedCornerShape(15.dp))
                    .clickable(onClick = onBack)
                    .padding(vertical = 13.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Back", fontFamily = Nunito, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepOlive)
            }
        }
        if (currentStep < REFLECTION_PROMPTS.size - 1) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(if (answered) DeepOlive else TertiaryTan, RoundedCornerShape(15.dp))
                    .clickable(enabled = answered, onClick = onNext)
                    .padding(vertical = 13.5.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Next", fontFamily = Nunito, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BackgroundLight)
                Spacer(modifier = Modifier.width(2.dp))
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = BackgroundLight, modifier = Modifier.size(16.dp))
            }
        } else {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(if (allAnswered) SageAccent else TertiaryTan, RoundedCornerShape(15.dp))
                    .clickable(enabled = allAnswered, onClick = onSubmit)
                    .padding(vertical = 13.5.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = BackgroundLight, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Submit Reflection", fontFamily = Nunito, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BackgroundLight)
            }
        }
    }
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) DeepOlive else CardCream, RoundedCornerShape(15.dp))
            .border(1.dp, if (selected) DeepOlive else DeepOlive.copy(alpha = 0.1f), RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = label,
            fontFamily = Nunito,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) BackgroundLight else DeepOlive
        )
    }
}

@Composable
private fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) DeepOlive else CardCream, RoundedCornerShape(13.dp))
            .border(1.dp, if (selected) DeepOlive else DeepOlive.copy(alpha = 0.1f), RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            fontFamily = Nunito,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) BackgroundLight else DeepOlive
        )
    }
}

@Composable
private fun TextAnswerField(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 110.dp)
            .background(AuthTabsBackground, RoundedCornerShape(15.dp))
            .border(0.79.dp, DeepOlive.copy(alpha = 0.12f), RoundedCornerShape(15.dp))
            .padding(15.dp)
    ) {
        if (value.isEmpty()) {
            Text(text = placeholder, fontFamily = Nunito, fontSize = 14.sp, color = DeepOlive.copy(alpha = 0.5f))
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
private fun SubmittedContent(entry: ReflectionEntry?, justAnsweredNow: Map<String, String>, currentStreak: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 20.dp)) {
        Box(
            modifier = Modifier.size(72.dp).background(SageAccent.copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = SageAccent, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(text = "Reflection saved", fontFamily = Nunito, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = DeepOlive)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Taking a moment to reflect is an act of self-care. See you tomorrow.",
            fontFamily = Nunito,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MutedText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        val answersToShow: Map<String, String> = when {
            justAnsweredNow.isNotEmpty() -> justAnsweredNow
            entry != null -> mapOf(
                "mood" to entry.moodImpact,
                "control" to entry.controlLevel,
                "trigger" to entry.trigger,
                "highlight" to entry.highlight,
                "tomorrow" to entry.tomorrowPlan
            )
            else -> emptyMap()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardCream, RoundedCornerShape(18.dp))
                .border(0.79.dp, DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                .padding(15.dp)
        ) {
            REFLECTION_PROMPTS.forEach { prompt ->
                val answer = answersToShow[prompt.id]
                if (!answer.isNullOrBlank()) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(text = prompt.question, fontFamily = DmMono, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SageAccent)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = answer, fontFamily = Nunito, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DeepOlive)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (currentStreak > 1) "+10 XP · $currentStreak-day streak" else "+10 XP · Reflection logged",
            fontFamily = DmMono,
            fontSize = 13.sp,
            color = SageAccent
        )
    }
}

@Composable
private fun HistoryContent(recentEntries: List<ReflectionEntry>, currentStreak: Int) {
    if (currentStreak > 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SecondarySage.copy(alpha = 0.13f), RoundedCornerShape(15.dp))
                .border(1.dp, SecondarySage.copy(alpha = 0.4f), RoundedCornerShape(15.dp))
                .padding(horizontal = 15.dp, vertical = 12.dp)
        ) {
            Text(
                text = "$currentStreak-day streak",
                fontFamily = Nunito,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DeepOlive
            )
        }
        Spacer(modifier = Modifier.height(15.dp))
    }

    if (recentEntries.isEmpty()) {
        Text(
            text = "No reflections logged yet -- answer today's prompts to start your history.",
            fontFamily = Nunito,
            fontSize = 14.sp,
            color = MutedText
        )
        return
    }

    Text(
        text = "RECENT REFLECTIONS",
        fontFamily = DmMono,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = SageAccent
    )
    Spacer(modifier = Modifier.height(11.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        recentEntries.forEach { entry -> HistoryRow(entry) }
    }
}

@Composable
private fun HistoryRow(entry: ReflectionEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardCream, RoundedCornerShape(15.dp))
            .border(0.79.dp, DeepOlive.copy(alpha = 0.08f), RoundedCornerShape(15.dp))
            .padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = formatReflectionDateKey(entry.dateKey),
                fontFamily = DmMono,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = SecondarySage
            )
            Text(text = entry.moodImpact, fontFamily = Nunito, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepOlive)
        }
        if (entry.highlight.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = entry.highlight, fontFamily = Nunito, fontSize = 13.sp, color = MutedText, maxLines = 2)
        }
    }
}
