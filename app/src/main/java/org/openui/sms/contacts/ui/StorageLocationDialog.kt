package org.openui.sms.contacts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openui.sms.R
import org.openui.sms.ui.theme.AppTheme
import org.openui.sms.contacts.data.SimAccountInfo

private val ItemCardShape = RoundedCornerShape(14.dp)

@Composable
fun StorageLocationDialog(
    currentTarget: String,
    simAccounts: List<SimAccountInfo>,
    onDismiss: () -> Unit,
    onSelectTarget: (String) -> Unit
) {
    val colors = AppTheme.colors
    val dialerGreen = colors.dialerGreen

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                text = stringResource(R.string.contacts_storage_dialog_title),
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.contacts_storage_dialog_desc),
                    color = colors.textMuted,
                    fontSize = 13.sp
                )

                val isPhoneSelected = currentTarget == "PHONE"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ItemCardShape)
                        .background(if (isPhoneSelected) dialerGreen.copy(alpha = 0.15f) else colors.cardElevated)
                        .clickable { onSelectTarget("PHONE") }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhoneAndroid,
                        contentDescription = null,
                        tint = if (isPhoneSelected) dialerGreen else colors.textMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.contacts_storage_phone_title),
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.contacts_storage_phone_subtitle),
                            color = colors.textMuted,
                            fontSize = 12.sp
                        )
                    }
                    RadioButton(
                        selected = isPhoneSelected,
                        onClick = { onSelectTarget("PHONE") },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = dialerGreen,
                            unselectedColor = colors.textMuted
                        )
                    )
                }

                if (simAccounts.isEmpty()) {
                    val isSim1Selected = currentTarget == "SIM" || currentTarget == "SIM 1"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ItemCardShape)
                            .background(if (isSim1Selected) dialerGreen.copy(alpha = 0.15f) else colors.cardElevated)
                            .clickable { onSelectTarget("SIM 1") }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SimCard,
                            contentDescription = null,
                            tint = if (isSim1Selected) dialerGreen else colors.textMuted,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SIM 1",
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.contacts_storage_sim_subtitle, 1),
                                color = colors.textMuted,
                                fontSize = 12.sp
                            )
                        }
                        RadioButton(
                            selected = isSim1Selected,
                            onClick = { onSelectTarget("SIM 1") },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = dialerGreen,
                                unselectedColor = colors.textMuted
                            )
                        )
                    }
                } else {
                    simAccounts.forEach { sim ->
                        val targetKey = "SIM ${sim.slotIndex + 1}"
                        val isSimSelected = currentTarget == targetKey || (currentTarget == "SIM" && sim.slotIndex == 0)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ItemCardShape)
                                .background(if (isSimSelected) dialerGreen.copy(alpha = 0.15f) else colors.cardElevated)
                                .clickable { onSelectTarget(targetKey) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SimCard,
                                contentDescription = null,
                                tint = if (isSimSelected) dialerGreen else colors.textMuted,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sim.displayName,
                                    color = colors.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (sim.carrierName.isNotBlank() && sim.carrierName != sim.displayName)
                                        "${sim.carrierName} • SIM ${sim.slotIndex + 1}"
                                    else
                                        "SIM ${sim.slotIndex + 1}",
                                    color = colors.textMuted,
                                    fontSize = 12.sp
                                )
                            }
                            RadioButton(
                                selected = isSimSelected,
                                onClick = { onSelectTarget(targetKey) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = dialerGreen,
                                    unselectedColor = colors.textMuted
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.contacts_close), color = colors.textMuted)
            }
        }
    )
}
