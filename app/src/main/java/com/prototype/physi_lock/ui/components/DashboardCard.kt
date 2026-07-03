package com.prototype.physi_lock.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.prototype.physi_lock.ui.theme.PrimaryDark

private val CoreModuleCardBackground = Color(0xFFF5F3EB)

@Composable
fun DashboardCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = CoreModuleCardBackground
        ),
        border = BorderStroke(0.792.dp, PrimaryDark.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryDark
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = PrimaryDark.copy(alpha = 0.7f)
            )
        }
    }
}
