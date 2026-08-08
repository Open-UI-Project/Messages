package org.openui.sms.sms.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import org.openui.sms.R
import org.openui.sms.sms.ui.SmsGroup
import org.openui.sms.ui.theme.OpenUICardBackground
import org.openui.sms.ui.theme.OpenUIOrangeBadge
import org.openui.sms.ui.theme.OpenUIPillBackground
import org.openui.sms.ui.theme.OpenUITextSecondary

@Composable
fun SmsTopBar(
    groups: List<SmsGroup>,
    selectedGroup: String,
    searchQuery: String,
    onGroupSelect: (String) -> Unit,
    onCreateGroupClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onMarkAllAsReadClick: () -> Unit,
    onMakeDefaultAppClick: () -> Unit,
    onAboutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 12.dp, bottom = 4.dp)
    ) {
        if (isSearchActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_messages),
                            color = OpenUITextSecondary,
                            fontSize = 15.sp
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OpenUIOrangeBadge,
                        unfocusedBorderColor = OpenUICardBackground,
                        focusedContainerColor = OpenUICardBackground,
                        unfocusedContainerColor = OpenUICardBackground
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        isSearchActive = false
                        onSearchQueryChange("")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                ) {
                    val defaultGroupName = stringResource(R.string.all_groups)
                    val allGroups = listOf(defaultGroupName) + groups.map { it.name }
                    allGroups.forEach { groupName ->
                        val isSelected = groupName == selectedGroup
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) OpenUIPillBackground else Color.Transparent)
                                .clickable { onGroupSelect(groupName) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = groupName,
                                color = if (isSelected) Color.White else OpenUITextSecondary,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(OpenUICardBackground)
                            .clickable { onCreateGroupClick() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.add_group),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { isSearchActive = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.search),
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.menu),
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(OpenUICardBackground)
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.read_all), color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    onMarkAllAsReadClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.make_default_app), color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    onMakeDefaultAppClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.about_app), color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    onAboutClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

