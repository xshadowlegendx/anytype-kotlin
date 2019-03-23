package com.agileburo.anytype.feature_editor.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.agileburo.anytype.feature_editor.domain.EditorInteractor

class EditorViewModelFactory(
    private val editorInteractor: EditorInteractor
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T =
        EditorViewModel(interactor = editorInteractor) as T
}