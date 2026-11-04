package com.wh01s17.launcher.feature.menu.panel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Una página abierta o null. La actividad conserva el ViewModel durante la rotación. */
class MenuViewModel : ViewModel() {
    private val currentPage = MutableStateFlow<MenuPage?>(null)
    val page = currentPage.asStateFlow()

    fun open() {
        currentPage.value = MenuPage.Root
    }

    fun navigate(page: MenuPage) {
        if (currentPage.value != null) currentPage.value = page
    }

    fun back() {
        currentPage.value = if (currentPage.value == MenuPage.Root) null else currentPage.value?.let { MenuPage.Root }
    }

    fun close() {
        currentPage.value = null
    }
}
