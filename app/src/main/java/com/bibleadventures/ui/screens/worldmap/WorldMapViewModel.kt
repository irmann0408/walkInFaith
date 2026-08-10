package com.bibleadventures.ui.screens.worldmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibleadventures.domain.model.Chapter
import com.bibleadventures.domain.model.ChapterStatus
import com.bibleadventures.domain.repository.PlayerProfileRepository
import com.bibleadventures.game.stories.ChapterCatalog
import com.bibleadventures.progress.ProgressionService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class WorldMapNodeUiState(
    val chapter: Chapter,
    val status: ChapterStatus,
    val stars: Int,
)

data class WorldMapUiState(val nodes: List<WorldMapNodeUiState> = emptyList())

class WorldMapViewModel(
    progressionService: ProgressionService,
    repository: PlayerProfileRepository,
    catalog: List<Chapter> = ChapterCatalog.all,
) : ViewModel() {

    val uiState: StateFlow<WorldMapUiState> = combine(
        progressionService.chapterStatuses,
        repository.profile,
    ) { statuses, profile ->
        WorldMapUiState(
            nodes = catalog.map { chapter ->
                WorldMapNodeUiState(
                    chapter = chapter,
                    status = statuses.getValue(chapter.id),
                    stars = profile.progressByChapter[chapter.id]?.stars ?: 0,
                )
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = WorldMapUiState(),
    )
}
