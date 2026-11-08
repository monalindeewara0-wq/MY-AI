package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AboutDialog
import com.example.ui.components.AppThemeMode
import com.example.ui.components.ChatInputDock
import com.example.ui.components.ChatTopBar
import com.example.ui.components.ConversationDrawer
import com.example.ui.components.MessageBubble
import com.example.ui.components.SettingsDialog
import com.example.ui.components.WelcomeHero
import com.example.ui.theme.MyAiTheme
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val selectedImageUri by viewModel.selectedImageUri.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Resolve system vs manual theme
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> systemInDark
    }

    // Auto-scroll to bottom as messages change or new streaming chunks arrive
    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Surface error message in snackbar if present
    LaunchedEffect(errorMessage) {
        errorMessage?.let { err ->
            snackbarHostState.showSnackbar(
                message = err,
                duration = SnackbarDuration.Long
            )
            viewModel.dismissError()
        }
    }

    // BackHandler closes drawer if opened
    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    MyAiTheme(darkTheme = isDark) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface
                ) {
                    ConversationDrawer(
                        conversations = conversations,
                        activeConversationId = activeConversationId,
                        onSelectConversation = { id ->
                            viewModel.selectConversation(id)
                            coroutineScope.launch { drawerState.close() }
                        },
                        onNewChat = {
                            viewModel.startNewChat()
                            coroutineScope.launch { drawerState.close() }
                        },
                        onRenameConversation = { id, newTitle ->
                            viewModel.renameConversation(id, newTitle)
                        },
                        onDeleteConversation = { id ->
                            viewModel.deleteConversation(id)
                        },
                        onClearAllHistory = {
                            viewModel.clearAllHistory()
                            coroutineScope.launch { drawerState.close() }
                        }
                    )
                }
            }
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    ChatTopBar(
                        onMenuClick = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        onNewChatClick = {
                            viewModel.startNewChat()
                        },
                        onSettingsClick = { showSettingsDialog = true },
                        onAboutClick = { showAboutDialog = true },
                        onClearChatClick = { viewModel.clearCurrentChat() }
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    ChatInputDock(
                        text = inputText,
                        onTextChange = { viewModel.updateInputText(it) },
                        selectedImageUri = selectedImageUri,
                        onImageSelected = { viewModel.setSelectedImageUri(it) },
                        isGenerating = isGenerating,
                        onSendMessage = { viewModel.sendMessage() }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (messages.isEmpty()) {
                        // Empty State: Welcome Hero with Sinhala + English Greetings & Prompt Starters
                        WelcomeHero(
                            onSelectPrompt = { prompt ->
                                viewModel.sendMessage(prompt)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Active Chat Conversation
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("messages_list")
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                val isLastModel = msg.role == "model" && msg.id == messages.lastOrNull { it.role == "model" }?.id
                                MessageBubble(
                                    message = msg,
                                    isLastModelMessage = isLastModel,
                                    onRegenerate = { viewModel.regenerateLastAnswer() }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dialogs
        if (showAboutDialog) {
            AboutDialog(onDismiss = { showAboutDialog = false })
        }

        if (showSettingsDialog) {
            SettingsDialog(
                currentThemeMode = themeMode,
                onThemeModeChange = { viewModel.setThemeMode(it) },
                onClearAllHistory = {
                    viewModel.clearAllHistory()
                    showSettingsDialog = false
                },
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}
