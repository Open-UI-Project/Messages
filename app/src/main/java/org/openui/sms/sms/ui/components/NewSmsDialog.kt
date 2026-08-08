package org.openui.sms.sms.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import org.openui.sms.R
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.openui.sms.ui.theme.OpenUICardBackground
import org.openui.sms.ui.theme.OpenUIOrangeBadge
import org.openui.sms.ui.theme.OpenUISurface
import org.openui.sms.ui.theme.OpenUITextSecondary

@Composable
fun NewSmsDialog(
    initialRecipient: String = "",
    onDismiss: () -> Unit,
    onSend: (String, String) -> Unit
) {
    var recipient by remember { mutableStateOf(initialRecipient) }
    var messageText by remember { mutableStateOf("") }

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
                    text = stringResource(R.string.new_sms_title),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text(stringResource(R.string.new_sms_to)) },
                    placeholder = { Text("+79991234567") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
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

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text(stringResource(R.string.new_sms_text_label)) },
                    placeholder = { Text(stringResource(R.string.new_sms_text_placeholder)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OpenUIOrangeBadge,
                        unfocusedBorderColor = OpenUICardBackground,
                        focusedLabelColor = OpenUIOrangeBadge,
                        unfocusedLabelColor = OpenUITextSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.group_cancel),
                            color = OpenUITextSecondary,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (recipient.isNotBlank() && messageText.isNotBlank()) {
                                onSend(recipient, messageText)
                                onDismiss()
                            }
                        },
                        enabled = recipient.isNotBlank() && messageText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OpenUIOrangeBadge,
                            disabledContainerColor = OpenUICardBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.new_sms_send),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

