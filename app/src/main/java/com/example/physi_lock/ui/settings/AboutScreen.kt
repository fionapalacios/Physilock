package com.example.physi_lock.ui.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physi_lock.ui.theme.BackgroundLight
import com.example.physi_lock.ui.theme.CardCream
import com.example.physi_lock.ui.theme.DeepOlive
import com.example.physi_lock.ui.theme.MutedText
import com.example.physi_lock.ui.theme.Nunito
import com.example.physi_lock.ui.theme.SageAccent

private data class HelpItem(val question: String, val answer: String)

/** Real answers, not the mockup's verbatim copy -- corrected against what this app
 *  actually does (2026-09-04 fact-check):
 *  - Dropped "no internet, no servers, no third parties, never leaves your phone": false
 *    -- account/login data syncs through Firebase (Firestore + Auth). Usage logs and
 *    behavioral data (the actual AI inputs) do stay device-local, so the answer below
 *    says that instead.
 *  - "GPS anchors ... Home/School/Work/Bedtime auto-switching": Location doesn't do
 *    automatic profile switching -- it's two named watched anchors (School, Work),
 *    passive alert only (see LocationScreen.kt). Rewritten to match.
 *  - Module count (7) and on-device AI both check out against MODULE_PROGRESS.md / the
 *    real ml/ pipeline, left as-is.
 */
private val helpItems = listOf(
    HelpItem(
        "How does Move-to-Unlock work?",
        "When an app is locked, Physi-Lock shows a Motion Challenge. Complete a physical movement (shake, rotate, walk) to unlock the app. Challenge difficulty scales with your AI Risk Score."
    ),
    HelpItem(
        "What is the AI Risk Score?",
        "A score calculated from your screen time, doomscrolling sessions, and predicted overuse. LOW = normal, MODERATE = caution, HIGH = intervention triggered."
    ),
    HelpItem(
        "Is my data private?",
        "Your usage logs and behavioral data (what the AI models run on) stay on this device only. Account and login info syncs through Firebase so you can sign in across devices — that part isn't local-only."
    ),
    HelpItem(
        "How does Location work?",
        "Pin your School and/or Work location in Settings → Location (tap it on the map), optionally limiting alerts to set hours. Physi-Lock sends a passive alert when you're near a pinned spot and open a distracting app — it doesn't auto-switch modes or lock anything on its own."
    ),
    HelpItem(
        "How do I set app limits?",
        "Go to Settings → App Lock Rules to gate specific apps behind a Motion Challenge, or Home → Usage Goals for a daily/weekly time cap."
    )
)

@Composable
fun AboutScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    var expandedQuestion by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = DeepOlive,
                modifier = Modifier.size(20.dp).clickable(onClick = onBackClick)
            )
            Text(
                text = "About Physi-Lock",
                fontFamily = Nunito,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepOlive
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 15.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(DeepOlive),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🌿", fontSize = 30.sp)
            }
            Spacer(modifier = Modifier.height(11.dp))
            Text(
                text = "Physi-Lock",
                fontFamily = Nunito,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepOlive
            )
            Text(
                text = "Version ${appVersionName(context)}",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = SageAccent,
                modifier = Modifier.padding(top = 2.dp, bottom = 18.dp)
            )
            Text(
                text = "Physi-Lock uses AI and motion detection to help you build healthier screen habits — one movement at a time.",
                fontFamily = Nunito,
                fontSize = 14.sp,
                color = MutedText,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            AboutStatRow("Modules", "7")
            AboutStatRow("User Functions", "21")
            AboutStatRow("AI Models", "On-device")
            AboutStatRow("Usage Data", "Stored locally")

            Text(
                text = "Made with 🌿 for your mental wellness",
                fontFamily = Nunito,
                fontSize = 13.sp,
                color = SageAccent,
                modifier = Modifier.padding(top = 24.dp, bottom = 24.dp)
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "HELP GUIDE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SageAccent,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardCream)
                ) {
                    helpItems.forEachIndexed { index, item ->
                        val isOpen = expandedQuestion == item.question
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(DeepOlive.copy(alpha = 0.07f))
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedQuestion = if (isOpen) null else item.question }
                                .padding(15.dp)
                                .animateContentSize()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.question,
                                    fontFamily = Nunito,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepOlive,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = SageAccent,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .rotate(if (isOpen) 90f else 0f)
                                )
                            }
                            if (isOpen) {
                                Text(
                                    text = item.answer,
                                    fontFamily = Nunito,
                                    fontSize = 13.sp,
                                    color = MutedText,
                                    lineHeight = 19.sp,
                                    modifier = Modifier.padding(top = 8.dp)
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
private fun AboutStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontFamily = Nunito, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepOlive)
        Text(text = value, fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = MutedText)
    }
}
