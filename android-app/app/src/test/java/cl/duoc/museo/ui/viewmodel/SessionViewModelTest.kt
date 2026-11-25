package cl.duoc.museo.ui.viewmodel

import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.repository.AuthRepository
import cl.duoc.museo.data.repository.CatalogRepository
import cl.duoc.museo.data.repository.NotificationRepository
import cl.duoc.museo.testutil.Fixtures
import cl.duoc.museo.testutil.MainDispatcherRule
import cl.duoc.museo.testutil.keepCollecting
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SessionViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun `expone usuario, avisos sin leer y carrito`() = runTest {
        val session = mockk<SessionManager>()
        val auth = mockk<AuthRepository>(relaxUnitFun = true)
        val notifications = mockk<NotificationRepository>()
        val catalog = mockk<CatalogRepository>()
        every { session.currentUser } returns flowOf(Fixtures.user())
        every { session.currentId() } returns 1L
        every { notifications.observeUnread() } returns flowOf(3)
        every { catalog.observeCartCount() } returns flowOf(2)

        val vm = SessionViewModel(session, auth, notifications, catalog)
        keepCollecting(vm.unread)
        keepCollecting(vm.cartCount)

        assertEquals(1L, vm.userState.value.user?.id)
        assertFalse(vm.userState.value.loading)
        assertEquals(3, vm.unread.value)
        assertEquals(2, vm.cartCount.value)
        assertTrue(vm.hasSession())
        vm.logout()
        verify { auth.logout() }
    }
}
