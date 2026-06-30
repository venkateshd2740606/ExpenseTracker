package com.expensetracker.presentation.ui.screens.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.expensetracker.presentation.viewmodel.GroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(viewModel: GroupViewModel, onGroupClick: (String) -> Unit) {
    val groups by viewModel.groups.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("👥") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Groups") }) },
        floatingActionButton = { FloatingActionButton(onClick = { showDialog = true }) { Icon(Icons.Default.GroupAdd, null) } }
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(groups) { group ->
                ListItem(
                    headlineContent = { Text("${group.iconEmoji} ${group.name}") },
                    supportingContent = { Text(group.description.ifBlank { "${group.memberIds.size} members" }) },
                    modifier = Modifier,
                    leadingContent = { Text(group.iconEmoji, style = MaterialTheme.typography.headlineMedium) },
                    trailingContent = { IconButton(onClick = { viewModel.archiveGroup(group.id) }) { Icon(Icons.Default.Archive, null) } }
                )
                HorizontalDivider()
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Create Group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") })
                    OutlinedTextField(desc, { desc = it }, label = { Text("Description") })
                    OutlinedTextField(emoji, { emoji = it }, label = { Text("Icon") })
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.createGroup(name, desc, emoji); showDialog = false }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(viewModel: com.expensetracker.presentation.viewmodel.FriendViewModel) {
    val friends by viewModel.friends.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends") },
                actions = { IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.PersonAdd, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(query, { query = it; viewModel.search(query) }, label = { Text("Search") }, modifier = Modifier.fillMaxWidth().padding(16.dp))
            LazyColumn {
                items(friends) { friend ->
                    ListItem(
                        headlineContent = { Text(friend.name) },
                        supportingContent = { Text(friend.email.ifBlank { friend.mobile }) },
                        leadingContent = { Icon(Icons.Default.Person, null) }
                    )
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add Friend") },
            text = {
                Column {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") })
                    OutlinedTextField(email, { email = it }, label = { Text("Email") })
                    OutlinedTextField(mobile, { mobile = it }, label = { Text("Mobile") })
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.addFriend(name, email, mobile); showAdd = false }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementsScreen(viewModel: com.expensetracker.presentation.viewmodel.SettlementViewModel) {
    val settlements by viewModel.settlements.collectAsState()
    val balances by viewModel.balances.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadBalances("USD") }

    Scaffold(topBar = { TopAppBar(title = { Text("Settle Up") }) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Suggested Settlements", style = MaterialTheme.typography.titleMedium) }
            items(balances) { b ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${b.fromUserName} → ${b.toUserName}")
                        Text("${"%.2f".format(b.amount)} ${b.currencyCode}")
                    }
                }
            }
            item { Text("History", style = MaterialTheme.typography.titleMedium) }
            items(settlements) { s ->
                ListItem(
                    headlineContent = { Text("${s.fromUserName} paid ${s.toUserName}") },
                    supportingContent = { Text(s.method.name) },
                    trailingContent = { Text("${"%.2f".format(s.amount)}") }
                )
            }
        }
    }
}
