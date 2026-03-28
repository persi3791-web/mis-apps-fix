package com.example.cornell.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cornell.ui.screens.ExportModal
import com.example.cornell.ui.screens.FolderDetailScreen
import com.example.cornell.ui.screens.HomeScreen
import com.example.cornell.ui.screens.ManageScreen
import com.example.cornell.ui.screens.ResultScreen
import com.example.cornell.ui.screens.TabChatContent
import com.example.cornell.ui.screens.TabExplicacionContent
import com.example.cornell.ui.screens.TabFlashcardsContent
import com.example.cornell.ui.screens.TabQuizContent
import com.example.cornell.ui.screens.TabResumenContent
import com.example.cornell.ui.screens.TabTranscContent
import com.example.cornell.data.Note

@Composable
fun AppNavigation(
    viewModel: CornellViewModel = viewModel(),
    onShowToast: (String) -> Unit
) {
    val navController = rememberNavController()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { msg ->
            onShowToast(msg)
            viewModel.consumeToast()
        }
    }
    LaunchedEffect(state.navigateToNoteId) {
        state.navigateToNoteId?.let { id ->
            navController.navigate(Routes.result(id)) {
                popUpTo(Routes.HOME) { inclusive = false }
                launchSingleTop = true
            }
            viewModel.clearNavigateToNoteId()
        }
    }

    var showExportModal by remember { mutableStateOf(false) }
    var pendingPdfNote by remember { mutableStateOf<Note?>(null) }
    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { pendingPdfNote?.let { note -> viewModel.exportNoteToPdf(uri, note) } }
        pendingPdfNote = null
    }
    var quizQuantity by remember { mutableStateOf("5") }
    var flashQuantity by remember { mutableStateOf("5") }
    var editTitle by remember { mutableStateOf("") }
    var editIdeas by remember { mutableStateOf("") }
    var editNotas by remember { mutableStateOf("") }
    var editResumen by remember { mutableStateOf("") }

    val currentNote = state.currentNote
    LaunchedEffect(currentNote?.id) {
        editTitle = currentNote?.title ?: ""
        editIdeas = currentNote?.cornellData?.ideasClave ?: ""
        editNotas = currentNote?.cornellData?.notasClase ?: ""
        editResumen = currentNote?.cornellData?.resumen ?: ""
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            if (showExportModal) {
                ExportModal(
                    onDismiss = { showExportModal = false },
                    viewModel = viewModel
                )
            }
            HomeScreen(
                notes = state.notes,
                folders = state.folders,
                onNoteClick = { id ->
                    viewModel.openNote(id)
                    navController.navigate(Routes.result(id))
                },
                onFolderClick = { name -> navController.navigate(Routes.folder(name)) },
                onCreateFolder = { },
                onConfirmCreateFolder = { viewModel.createFolder(it) },
                onRenameNote = { id, title -> viewModel.renameNote(id, title) },
                onDeleteNote = { id -> viewModel.deleteNote(id) },
                onMoveNoteToFolder = { id, folder -> viewModel.moveNoteToFolder(id, folder) },
                onCopyNoteToFolder = { id, folder -> viewModel.copyNoteToFolder(id, folder) },
                onExportNoteToPdf = { note ->
                    pendingPdfNote = note
                    pdfExportLauncher.launch("${note.title}.pdf")
                },
                onNewNote = { navController.navigate(Routes.MANAGE) },
                onSettingsClick = { showExportModal = true }
            )
        }
        composable(Routes.MANAGE) {
            ManageScreen(
                isLoading = state.isLoading,
                onBack = { navController.popBackStack() },
                onGenerate = { title, folder, text ->
                    viewModel.generateCornell(text, title, folder)
                }
            )
        }
        composable(
            Routes.RESULT,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: return@composable
            if (state.currentNote?.id != noteId) viewModel.openNote(noteId)
            val note = state.currentNote
            if (note == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
                return@composable
            }
            ResultScreen(
                note = note,
                isEditing = state.isNoteEditing,
                onBack = {
                    viewModel.clearCurrentNote()
                    navController.popBackStack()
                },
                onToggleEdit = {
                    if (state.isNoteEditing) {
                        viewModel.saveCurrentNoteEdits(editTitle, editIdeas, editNotas, editResumen)
                    } else {
                        viewModel.setNoteEditing(true)
                    }
                },
                onSaveEdits = { t, i, n, r ->
                    viewModel.saveCurrentNoteEdits(t, i, n, r)
                    editTitle = t
                    editIdeas = i
                    editNotas = n
                    editResumen = r
                },
                tabResumen = {
                    TabResumenContent(
                        note = note,
                        isEditing = state.isNoteEditing,
                        titleValue = editTitle,
                        ideasValue = editIdeas,
                        notasValue = editNotas,
                        resumenValue = editResumen,
                        onTitleChange = { editTitle = it },
                        onIdeasChange = { editIdeas = it },
                        onNotasChange = { editNotas = it },
                        onResumenChange = { editResumen = it }
                    )
                },
                tabCuestionario = {
                    TabQuizContent(
                        questions = state.quizQuestions,
                        currentIndex = state.quizCurrentIndex,
                        answered = state.quizAnswered,
                        selectedOption = state.quizSelectedOption,
                        isLoading = state.isLoading,
                        statusMessage = state.quizStatusMessage,
                        quantityHint = quizQuantity,
                        onQuantityChange = { quizQuantity = it },
                        onGenerate = { viewModel.generateQuiz(quizQuantity.toIntOrNull() ?: 5) },
                        onClear = { viewModel.clearQuiz() },
                        onPrev = { viewModel.setQuizCurrentIndex(state.quizCurrentIndex - 1) },
                        onNext = { viewModel.setQuizCurrentIndex(state.quizCurrentIndex + 1) },
                        onSelectOption = { optIdx ->
                            viewModel.setQuizAnswer(state.quizCurrentIndex, optIdx)
                        }
                    )
                },
                tabFlashcards = {
                    TabFlashcardsContent(
                        cards = state.flashcards,
                        flipped = state.flashcardFlipped,
                        isLoading = state.isLoading,
                        quantityHint = flashQuantity,
                        onQuantityChange = { flashQuantity = it },
                        onGenerate = { viewModel.generateFlashcards(flashQuantity.toIntOrNull() ?: 5) },
                        onClear = { viewModel.clearFlashcards() },
                        onFlip = { viewModel.setFlashcardFlipped(it, !(state.flashcardFlipped[it] == true)) }
                    )
                },
                tabChat = {
                    TabChatContent(
                        messages = state.chatMessages,
                        isLoading = state.isChatLoading,
                        onSend = { viewModel.sendChatMessage(it) }
                    )
                },
                tabExplicacion = {
                    TabExplicacionContent(
                        explanationText = state.explanationText,
                        explanationPartial = state.explanationPartial,
                        onExplanationChange = { viewModel.setExplanationText(it) },
                        score = state.explanationScore,
                        scoreLabelText = state.explanationScoreLabel,
                        feedbackFound = state.explanationFeedbackFound,
                        feedbackMissing = state.explanationFeedbackMissing,
                        feedbackTip = state.explanationFeedbackTip,
                        isEvaluating = state.isExplanationEvaluating,
                        voiceStatus = state.explanationVoiceStatus,
                        onVoiceStatusChange = { viewModel.setExplanationVoiceStatus(it) },
                        micLevel = state.explanationMicLevel,
                        onMicLevelChange = { viewModel.setExplanationMicLevel(it) },
                        onEvaluate = { viewModel.evaluateExplanation() },
                        onClear = { viewModel.clearExplanation() },
                        onVoicePartial = { viewModel.setExplanationVoicePartial(it) },
                        onVoiceResult = { viewModel.appendExplanationFromVoice(it) }
                    )
                },
                tabTransc = {
                    TabTranscContent(originalText = note?.originalText ?: "")
                }
            )
        }
        composable(
            Routes.FOLDER,
            arguments = listOf(navArgument("folderName") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderName = backStackEntry.arguments?.getString("folderName") ?: return@composable
            FolderDetailScreen(
                folderName = folderName,
                notes = state.notes.filter { it.folder == folderName || folderName in it.folders }.reversed(),
                folders = state.folders,
                onBack = { navController.popBackStack() },
                onNoteClick = { id ->
                    viewModel.openNote(id)
                    navController.navigate(Routes.result(id))
                },
                onRenameNote = { id, title -> viewModel.renameNote(id, title) },
                onDeleteNote = { id -> viewModel.deleteNote(id) },
                onMoveNoteToFolder = { id, folder -> viewModel.moveNoteToFolder(id, folder) },
                    onCopyNoteToFolder = { id, folder -> viewModel.copyNoteToFolder(id, folder) },
                onExportNoteToPdf = { note ->
                    pendingPdfNote = note
                    pdfExportLauncher.launch("${note.title}.pdf")
                },
                onRenameFolder = { old, new ->
                    if (viewModel.renameFolder(old, new)) {
                        navController.popBackStack()
                        navController.navigate(Routes.folder(new))
                    }
                },
                onDeleteFolder = { name -> viewModel.deleteFolder(name) }
            )
        }
    }
}

