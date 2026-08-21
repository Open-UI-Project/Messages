package org.openui.sms.sms.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openui.sms.sms.model.SmsConversation

private val AvatarBgColor = Color(0xFF393939)
private val SubtextColor = Color(0xFFA8A8A8)
private val UnreadBadgeColor = Color(0xFFDD5928)
private val AvatarIconTint = Color(0xCCFFFFFF)
private val BadgeShape = RoundedCornerShape(10.dp)
private val AvatarShape = CircleShape

@Composable
fun SmsConversationItem(
    conversation: SmsConversation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unreadText = remember(conversation.unreadCount) {
        if (conversation.unreadCount > 999) "999+" else conversation.unreadCount.toString()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(AvatarShape)
                .background(AvatarBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = AvatarIconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = conversation.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = conversation.snippet,
                color = SubtextColor,
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
                color = SubtextColor,
                fontSize = 12.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (conversation.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(BadgeShape)
                        .background(UnreadBadgeColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = unreadText,
                        color = Color.White,
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


