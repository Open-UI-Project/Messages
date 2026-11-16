package org.openui.sms.sms.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.openui.sms.sms.model.SmsConversation
import org.openui.sms.ui.theme.AppTheme
import org.openui.sms.ui.theme.OpenUIOrangeBadge
import org.openui.sms.ui.theme.OpenUIAvatarGrey

private val BadgeShape = RoundedCornerShape(10.dp)
private val AvatarShape = CircleShape

@Composable
fun SmsConversationItem(
    conversation: SmsConversation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val unreadText = remember(conversation.unreadCount) {
        if (conversation.unreadCount > 999) "999+" else conversation.unreadCount.toString()
    }

    val initial = remember(conversation.title) {
        val trimmed = conversation.title.trim()
        val firstChar = trimmed.firstOrNull()
        if (firstChar != null && (firstChar.isLetter() || firstChar in 'А'..'Я' || firstChar == 'Ё')) {
            firstChar.uppercaseChar().toString()
        } else {
            ""
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val hasPhoto = !conversation.photoUri.isNullOrBlank()
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(AvatarShape)
                .background(
                    when {
                        hasPhoto -> Color.Transparent
                        colors.isExpressive -> MaterialTheme.colorScheme.primaryContainer
                        else -> OpenUIAvatarGrey
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (hasPhoto) {
                AsyncImage(
                    model = conversation.photoUri,
                    contentDescription = conversation.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (colors.isExpressive && initial.isNotBlank()) {
                Text(
                    text = initial,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = if (colors.isExpressive) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        Color.White.copy(alpha = 0.9f)
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = conversation.title,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = conversation.snippet,
                color = colors.textSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Text(
                text = conversation.formattedDate,
                color = if (conversation.unreadCount > 0) colors.primaryBrand else colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = if (conversation.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (conversation.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(if (colors.isExpressive) CircleShape else BadgeShape)
                        .background(if (colors.isExpressive) colors.primaryBrand else OpenUIOrangeBadge)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = unreadText,
                        color = colors.onAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}
