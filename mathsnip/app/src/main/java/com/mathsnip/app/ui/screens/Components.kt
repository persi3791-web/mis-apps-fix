package com.mathsnip.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathsnip.app.ui.theme.*

@Composable
fun MathTopBar(
    title: String,
    subtitle: String = "",
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface3)
                .padding(horizontal = 4.dp)
                .height(60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                if (title.isNotEmpty()) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPri
                    )
                }
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Muted
                    )
                }
            }
            actions()
        }
        HorizontalDivider(color = Divider, thickness = 1.dp)
    }
}

@Composable
fun MathTopBarBack(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface3)
                .height(60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text("←", fontSize = 22.sp, color = Accent)
            }
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPri,
                modifier = Modifier.weight(1f)
            )
            actions()
        }
        HorizontalDivider(color = Divider, thickness = 1.dp)
    }
}

@Composable
fun SearchBar(hint: String, onSearch: (String) -> Unit = {}) {
    var text by remember { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface2)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("🔍", fontSize = 16.sp)
        Text(
            text = if (text.isEmpty()) hint else text,
            color = if (text.isEmpty()) Muted else TextPri,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SectionHeader(text: String, color: Color = TextPri) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
        letterSpacing = 0.5.sp
    )
}

@Composable
fun SettingsRow(
    icon: String,
    label: String,
    subtitle: String = "",
    iconBg: Color = Surface2,
    iconColor: Color = TextSec,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 16.sp, color = iconColor)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, color = TextPri)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 12.sp, color = Muted)
            }
        }
        Text("›", fontSize = 20.sp, color = Muted)
    }
    HorizontalDivider(color = Divider, thickness = 0.5.dp, modifier = Modifier.padding(start = 66.dp))
}

@Composable
fun ToggleRow(
    label: String,
    subLabel: String = "",
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, color = TextPri)
            if (subLabel.isNotEmpty())
                Text(subLabel, fontSize = 12.sp, color = Muted, lineHeight = 16.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Accent,
                uncheckedThumbColor = Color(0xFF666680),
                uncheckedTrackColor = Surface2,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
    HorizontalDivider(color = Divider, thickness = 0.5.dp)
}

@Composable
fun DropdownRow(label: String, value: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, color = TextPri, modifier = Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(value, fontSize = 14.sp, color = Muted)
            Text("⌄", fontSize = 14.sp, color = Muted)
        }
    }
    HorizontalDivider(color = Divider, thickness = 0.5.dp)
}

@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    color: Color = Accent,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) color.copy(alpha = 0.2f) else Color.Transparent
            )
            .then(
                if (!selected) Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Surface2)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = if (selected) color else Muted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun ModalBottomSheetContent(
    title: String,
    items: List<Triple<String, String, () -> Unit>>,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Surface)
                .clickable(enabled = false) {}
        ) {
            // Handle bar
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Muted.copy(alpha = 0.4f))
            )

            if (title.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(52.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPri,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Surface2),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", fontSize = 13.sp, color = Muted)
                        }
                    }
                }
                HorizontalDivider(color = Divider)
            } else {
                Spacer(Modifier.height(8.dp))
            }

            items.forEachIndexed { index, (icon, label, action) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = action)
                        .padding(horizontal = 20.dp)
                        .height(58.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Surface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(icon, fontSize = 18.sp, color = TextSec)
                    }
                    Text(label, fontSize = 15.sp, color = TextPri, modifier = Modifier.weight(1f))
                    Text("›", fontSize = 18.sp, color = Muted)
                }
                if (index < items.size - 1) {
                    HorizontalDivider(
                        color = Divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 76.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    colors: List<Color>,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(colors))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun StatChip(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = color.copy(alpha = 0.7f))
    }
}
