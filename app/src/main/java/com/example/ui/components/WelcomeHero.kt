package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanGlow
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary

data class QuickPrompt(
    val category: String,
    val prompt: String,
    val icon: ImageVector,
    val isSinhala: Boolean = false
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WelcomeHero(
    onSelectPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "halo")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val quickPrompts = listOf(
        QuickPrompt(
            category = "Sinhala",
            prompt = "ඔයා කවුද? ඔයාව නිර්මාණය කළේ කවුද?",
            icon = Icons.Default.AutoAwesome,
            isSinhala = true
        ),
        QuickPrompt(
            category = "Singlish & GTA",
            prompt = "Mata GTA game ekak kiyanna.",
            icon = Icons.Default.VideogameAsset,
            isSinhala = true
        ),
        QuickPrompt(
            category = "WWE & Sports",
            prompt = "What are the latest WWE championships and storylines?",
            icon = Icons.Default.SportsMma
        ),
        QuickPrompt(
            category = "Tech & PC",
            prompt = "Explain Windows 11 vs Windows 10 performance for gaming.",
            icon = Icons.Default.Computer
        ),
        QuickPrompt(
            category = "Cricket & Football",
            prompt = "Tell me about the latest cricket world records and upcoming matches.",
            icon = Icons.Default.SportsKabaddi
        ),
        QuickPrompt(
            category = "Photo Vision",
            prompt = "Attach a photo and ask MY AI to explain objects, read text, or identify devices.",
            icon = Icons.Default.CameraAlt
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Glowing Animated AI Emblem
        Box(
            modifier = Modifier
                .size(110.dp)
                .scale(pulseScale),
            contentAlignment = Alignment.Center
        ) {
            // Ambient Aura Halo
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeonCyan.copy(alpha = 0.35f),
                                ElectricBlue.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Outer ring
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            listOf(NeonCyan, ElectricBlue, NeonCyanGlow, NeonCyan)
                        ),
                        shape = CircleShape
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_my_ai_logo),
                    contentDescription = "MY AI Animated Emblem",
                    modifier = Modifier.size(80.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main English Greeting
        Text(
            text = "Hey! I'm MY AI.",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Sinhala Greeting
        Text(
            text = "ආයුබෝවන්! මම MY AI. ඔයාට අද මගෙන් මොනවද දැනගන්න ඕනේ?",
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 24.sp
            ),
            color = NeonCyan,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Created by Monal from Sri Lanka • Powered by Gemini AI",
            style = MaterialTheme.typography.labelSmall,
            color = SlateTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "SUGGESTED TOPICS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            ),
            color = SlateTextSecondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Suggestion Cards in a responsive FlowRow
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickPrompts.forEach { item ->
                Card(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectPrompt(item.prompt) }
                        .testTag("prompt_chip_${item.category.lowercase().replace(" ", "_")}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                NeonCyan.copy(alpha = 0.3f)
                            )
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = ElectricBlue
                            )
                            Text(
                                text = item.prompt,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
