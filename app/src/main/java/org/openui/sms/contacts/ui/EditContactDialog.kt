package org.openui.sms.contacts.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.openui.sms.R
import org.openui.sms.ui.theme.AppTheme
import org.openui.sms.contacts.model.ContactItem
import org.openui.sms.contacts.model.getAvatarColor

private val FieldShape = RoundedCornerShape(16.dp)

@Composable
fun EditContactDialog(
    contact: ContactItem,
    currentSocialLink: String,
    currentBackgroundUri: String? = null,
    onDismiss: () -> Unit,
    onSave: (newName: String, newPhone: String, newEmail: String?, newPhotoUri: Uri?, newSocialLink: String, newBackgroundUri: String?) -> Unit
) {
    val colors = AppTheme.colors
    var name by remember(contact) { mutableStateOf(contact.name) }
    var phone by remember(contact) { mutableStateOf(contact.number) }
    var email by remember(contact) { mutableStateOf(contact.email ?: "") }
    var socialLink by remember(currentSocialLink) { mutableStateOf(currentSocialLink) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBackgroundUri by remember(currentBackgroundUri) { mutableStateOf(currentBackgroundUri) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    val backgroundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedBackgroundUri = uri.toString()
        }
    }

    val avatarColor = remember(contact.name) { getAvatarColor(contact.name) }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                text = stringResource(R.string.contacts_edit_contact),
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(avatarColor)
                            .border(2.dp, colors.dialerGreen.copy(alpha = 0.6f), CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            selectedPhotoUri != null -> {
                                AsyncImage(
                                    model = selectedPhotoUri,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            contact.photoUri != null -> {
                                AsyncImage(
                                    model = contact.photoUri,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Rounded.AddAPhoto,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Аватар контакта",
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(FieldShape)
                        .background(colors.cardElevated)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Image,
                                contentDescription = null,
                                tint = colors.dialerGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Индивидуальный фон профиля",
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (selectedBackgroundUri != null) {
                            IconButton(
                                onClick = { selectedBackgroundUri = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Удалить фон",
                                    tint = colors.actionRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (selectedBackgroundUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    backgroundPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            AsyncImage(
                                model = selectedBackgroundUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                backgroundPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.card),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Image,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.contacts_custom_bg_btn),
                                color = colors.textPrimary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.contacts_name_label), color = colors.textMuted) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Person, contentDescription = null, tint = colors.textMuted)
                    },
                    singleLine = true,
                    shape = FieldShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primaryBrand,
                        unfocusedBorderColor = colors.divider,
                        focusedContainerColor = colors.cardElevated,
                        unfocusedContainerColor = colors.cardElevated,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.contacts_phone_label), color = colors.textMuted) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Phone, contentDescription = null, tint = colors.textMuted)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = FieldShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primaryBrand,
                        unfocusedBorderColor = colors.divider,
                        focusedContainerColor = colors.cardElevated,
                        unfocusedContainerColor = colors.cardElevated,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.contacts_email_label), color = colors.textMuted) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Email, contentDescription = null, tint = colors.textMuted)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = FieldShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primaryBrand,
                        unfocusedBorderColor = colors.divider,
                        focusedContainerColor = colors.cardElevated,
                        unfocusedContainerColor = colors.cardElevated,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = socialLink,
                    onValueChange = { socialLink = it },
                    label = { Text(stringResource(R.string.contacts_social_label), color = colors.textMuted, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Link, contentDescription = null, tint = colors.textMuted)
                    },
                    singleLine = true,
                    shape = FieldShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primaryBrand,
                        unfocusedBorderColor = colors.divider,
                        focusedContainerColor = colors.cardElevated,
                        unfocusedContainerColor = colors.cardElevated,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onSave(
                            name.trim(),
                            phone.trim(),
                            email.trim().ifBlank { null },
                            selectedPhotoUri,
                            socialLink.trim(),
                            selectedBackgroundUri
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.dialerGreen),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.contacts_save),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.contacts_cancel),
                    color = colors.textMuted
                )
            }
        }
    )
}
