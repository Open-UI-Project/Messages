package org.openui.sms.sms.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openui.sms.sms.ui.model.SmsTab
import org.openui.sms.ui.theme.OpenUIOrangeBadge
import org.openui.sms.ui.theme.OpenUIPillBackground

@Composable
fun SmsIslandNavigationBar(
    selectedTab: SmsTab,
    onTabSelected: (SmsTab) -> Unit,
    totalUnreadCount: Int,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(30.dp)

    Box(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .shadow(elevation = 16.dp, shape = shape, clip = false)
                .widthIn(min = 200.dp, max = 220.dp)
                .height(58.dp)
                .clip(shape)
                .background(color = Color(0x9926262B))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmsIslandTabItem(
                title = stringResource(id = SmsTab.CONVERSATIONS.titleRes),
                icon = Icons.Rounded.ChatBubble,
                isSelected = selectedTab == SmsTab.CONVERSATIONS,
                onClick = { onTabSelected(SmsTab.CONVERSATIONS) },
                badgeCount = totalUnreadCount,
                modifier = Modifier.weight(1f)
            )

            SmsIslandTabItem(
                title = stringResource(id = SmsTab.CONTACTS.titleRes),
                icon = Icons.Rounded.Person,
                isSelected = selectedTab == SmsTab.CONTACTS,
                onClick = { onTabSelected(SmsTab.CONTACTS) },
                badgeCount = 0,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SmsIslandTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    badgeCount: Int,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    val pillColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF383840) else Color.Transparent,
        label = "pillColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF9E9E9E),
        label = "contentColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.06f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tabScale"
    )

    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(color = pillColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .testTag("sms_tab_$title"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.scale(scale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = 14.dp, y = (-6).dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(OpenUIOrangeBadge)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (badgeCount > 999) "999+" else badgeCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                lineHeight = 13.sp
            )
        }
    }
}
