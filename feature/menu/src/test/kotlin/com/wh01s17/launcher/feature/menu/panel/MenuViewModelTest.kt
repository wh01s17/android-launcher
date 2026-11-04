package com.wh01s17.launcher.feature.menu.panel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MenuViewModelTest {
    @Test
    fun `back returns to root then closes, and reopen starts fresh`() {
        val vm = MenuViewModel()
        vm.open()
        vm.navigate(MenuPage.Trigger)
        assertEquals(MenuPage.Trigger, vm.page.value)
        vm.back()
        assertEquals(MenuPage.Root, vm.page.value)
        vm.back()
        assertNull(vm.page.value)
        vm.open()
        assertEquals(MenuPage.Root, vm.page.value)
        vm.navigate(MenuPage.System)
        vm.close()
        vm.navigate(MenuPage.Trigger)
        assertNull(vm.page.value)
        vm.back()
        assertNull(vm.page.value)
    }
}
