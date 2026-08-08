package org.openui.sms.sms.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import org.openui.sms.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.openui.sms.sms.model.SmsConversation
import org.openui.sms.ui.theme.OpenUICardBackground
import org.openui.sms.ui.theme.OpenUIOrangeBadge
import org.openui.sms.ui.theme.OpenUISurface
import org.openui.sms.ui.theme.OpenUITextSecondary

@Composable
fun CreateGroupDialog(
    conversations: List<SmsConversation>,
    onDismiss: () -> Unit,
    onCreateGroup: (String, List<Long>) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    val selectedThreadIds = remember { mutableStateListOf<Long>() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = OpenUISurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.group_create_header),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text(stringResource(R.string.group_create_name_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OpenUIOrangeBadge,
                        unfocusedBorderColor = OpenUICardBackground,
                        focusedLabelColor = OpenUIOrangeBadge,
                        unfocusedLabelColor = OpenUITextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.group_create_select_dialogs),
                    color = OpenUITextSecondary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(conversations) { conversation ->
                        val isChecked = conversation.threadId in selectedThreadIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) {
                                        selectedThreadIds.remove(conversation.threadId)
                                    } else {
                                        selectedThreadIds.add(conversation.threadId)
                                    }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked == true) {
                                        selectedThreadIds.add(conversation.threadId)
                                    } else {
                                        selectedThreadIds.remove(conversation.threadId)
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = OpenUIOrangeBadge,
                                    uncheckedColor = OpenUITextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = conversation.title,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = conversation.snippet,
                                    color = OpenUITextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.group_cancel),
                            color = OpenUITextSecondary,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            if (groupName.isNotBlank()) {
                                onCreateGroup(groupName.trim(), selectedThreadIds.toList())
                                onDismiss()
                            }
                        },
                        enabled = groupName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OpenUIOrangeBadge,
                            disabledContainerColor = OpenUICardBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.group_create_btn),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
