package com.prototype.physi_lock.ui.screens.onboarding

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prototype.physi_lock.R
import com.prototype.physi_lock.ui.theme.AccentLavender
import com.prototype.physi_lock.ui.theme.BackgroundLight
import com.prototype.physi_lock.ui.theme.DeepOlive
import com.prototype.physi_lock.ui.theme.NunitoFontFamily
import com.prototype.physi_lock.ui.theme.PhysiLockTheme
import com.prototype.physi_lock.ui.theme.PrimaryDark
import com.prototype.physi_lock.ui.theme.PrimaryGreen
import com.prototype.physi_lock.ui.theme.SecondarySage
import com.prototype.physi_lock.ui.theme.TertiaryTan

private val OliveAccent = Color(0xFF6B7A4E)

private data class PermissionRowSpec(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconBackground: Color,
    val iconTint: Color
)

private val permissionRows = listOf(
    PermissionRowSpec(
        id = "accessibility_service",
        title = "Accessibility Service",
        description = "Detects when a locked app opens and shows the Move-to-Unlock screen.",
        icon = Icons.Default.Accessibility,
        iconBackground = PrimaryDark,
        iconTint = SecondarySage
    ),
    PermissionRowSpec(
        id = "usage_access",
        title = "Usage Access",
        description = "Tracks which apps you use and for how long to power your screen time reports.",
        icon = Icons.Default.Check,
        iconBackground = PrimaryGreen,
        iconTint = BackgroundLight
    ),
    PermissionRowSpec(
        id = "display_over_apps",
        title = "Display over other apps",
        description = "Lets Physi-Lock draw the lock challenge screen on top of any app.",
        icon = Icons.Default.Layers,
        iconBackground = OliveAccent,
        iconTint = BackgroundLight
    ),
    PermissionRowSpec(
        id = "notifications",
        title = "Notifications",
        description = "Sends break reminders, doomscrolling alerts, and bedtime nudges.",
        icon = Icons.Default.Notifications,
        iconBackground = AccentLavender,
        iconTint = BackgroundLight
    ),
    PermissionRowSpec(
        id = "battery_optimization",
        title = "Battery Optimization Exemption",
        description = "Keeps background monitoring stable so locks always trigger reliably.",
        icon = Icons.Default.BatteryChargingFull,
        iconBackground = TertiaryTan,
        iconTint = DeepOlive
    )
)

@Composable
fun GrantPermissionsScreen(
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var grantedIds by remember { mutableStateOf(emptySet<String>()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 480.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.75.dp)
                    .padding(top = 24.dp, bottom = 15.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Physi-Lock logo",
                    modifier = Modifier
                        .size(80.dp)
                        .padding(bottom = 18.dp)
                )
                Text(
                    text = "Welcome,",
                    fontFamily = NunitoFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 33.sp,
                    color = PrimaryDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Just grant a few permissions and Physi-Lock is ready to go!",
                    fontFamily = NunitoFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 20.15.sp,
                    color = DeepOlive
                )
            }

            HorizontalDivider(thickness = 1.25.dp, color = PrimaryDark.copy(alpha = 0.08f))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 18.75.dp)
                    .padding(vertical = 15.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundLight, RoundedCornerShape(15.dp))
                        .border(1.25.dp, PrimaryDark.copy(alpha = 0.16f), RoundedCornerShape(15.dp))
                ) {
                    permissionRows.forEachIndexed { index, spec ->
                        PermissionRow(
                            spec = spec,
                            isGranted = spec.id in grantedIds,
                            onToggle = {
                                grantedIds = if (spec.id in grantedIds) {
                                    grantedIds - spec.id
                                } else {
                                    grantedIds + spec.id
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (index != permissionRows.lastIndex) {
                            HorizontalDivider(thickness = 1.25.dp, color = PrimaryDark.copy(alpha = 0.08f))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.5.dp)
                    .padding(bottom = 11.25.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.63.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "Your activity stays on your device, only anonymous summaries are ever shared.",
                    fontFamily = NunitoFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 19.5.sp,
                    color = PrimaryGreen
                )
            }

            HorizontalDivider(thickness = 1.25.dp, color = PrimaryDark.copy(alpha = 0.08f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.75.dp)
                    .padding(top = 15.dp, bottom = 30.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.5.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(15.dp),
                            ambientColor = PrimaryDark.copy(alpha = 0.25f),
                            spotColor = PrimaryDark.copy(alpha = 0.25f)
                        )
                        .background(PrimaryDark, RoundedCornerShape(15.dp))
                        .clickable {
                            if (grantedIds.size < permissionRows.size) {
                                grantedIds = permissionRows.map { it.id }.toSet()
                            } else {
                                onContinueClick()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Continue (${grantedIds.size}/${permissionRows.size})",
                        textAlign = TextAlign.Center,
                        fontFamily = NunitoFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 22.5.sp,
                        color = BackgroundLight
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    spec: PermissionRowSpec,
    isGranted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 15.dp, vertical = 13.13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.25.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(spec.iconBackground, RoundedCornerShape(19.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = spec.icon,
                contentDescription = null,
                tint = spec.iconTint,
                modifier = Modifier.size(19.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = spec.title,
                fontFamily = NunitoFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 19.5.sp,
                color = PrimaryDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = spec.description,
                fontFamily = NunitoFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 18.2.sp,
                color = DeepOlive
            )
        }
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(
                    color = if (isGranted) PrimaryGreen else TertiaryTan,
                    shape = CircleShape
                )
        )
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun GrantPermissionsScreenPreview() {
    PhysiLockTheme {
        GrantPermissionsScreen(onContinueClick = {})
    }
}
