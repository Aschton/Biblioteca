package cl.duoc.museo.data.repository

import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.local.FavoriteDao
import cl.duoc.museo.data.local.FavoriteEntity
import cl.duoc.museo.data.local.HistoryDao
import cl.duoc.museo.data.local.MuseumDao
import cl.duoc.museo.data.local.MuseumEntity
import cl.duoc.museo.data.local.MuseumViewEntity
import cl.duoc.museo.data.remote.MuseosApi
import cl.duoc.museo.testutil.Fixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class MuseumRepositoryTest {

    private val museumDao = mockk<MuseumDao>(relaxUnitFun = true)
    private val favoriteDao = mockk<FavoriteDao>(relaxUnitFun = true)
    private val historyDao = mockk<HistoryDao>(relaxUnitFun = true)
    private val db = mockk<AppDatabase>()
    private val api = mockk<MuseosApi>()
    private val session = mockk<SessionManager>()
    private val userId = MutableStateFlow<Long?>(1L)
    private lateinit var repo: MuseumRepository

    @Before fun setUp() {
        every { db.museumDao() } returns museumDao
        every { db.favoriteDao() } returns favoriteDao
        every { db.historyDao() } returns historyDao
        every { session.userId } returns userId
        every { session.currentId() } answers { userId.value }
        repo = MuseumRepository(db, api, session)
    }

    @Test fun `refresh sube pendientes y guarda los museos del microservicio`() = runTest {
        val pendiente = Fixtures.museum(id = 5000, pendingSync = true, createdBy = 3)
        coEvery { museumDao.pendingSync() } returns listOf(pendiente)
        coEvery { api.createMuseum(any()) } returns Fixtures.museumDto(id = 20)
        coEvery { api.museums(any(), any()) } returns listOf(Fixtures.museumDto(id = 1))

        assertTrue(repo.refresh())

        coVerify { museumDao.deleteById(5000) }
        coVerify { museumDao.upsert(match { it.id == 20L && it.createdBy == 3L }) }
        coVerify { museumDao.upsertAll(match { it.size == 1 && it[0].id == 1L }) }
    }

    @Test fun `refresh sin conexion devuelve false`() = runTest {
        coEvery { museumDao.pendingSync() } returns emptyList()
        coEvery { api.museums(any(), any()) } throws IOException()

        assertFalse(repo.refresh())
    }

    @Test fun `guardar nueva ficha en linea`() = runTest {
        coEvery { api.createMuseum(any()) } returns Fixtures.museumDto(id = 30)
        val nueva = Fixtures.museum(id = 0).copy(imageUrl = "/foto.jpg")

        assertTrue(repo.save(nueva, isNew = true))

        coVerify { api.createMuseum(match { it.id == null }) }
        coVerify { museumDao.upsert(match { it.id == 30L && it.createdBy == 1L && it.imageUrl == "/foto.jpg" }) }
    }

    @Test fun `actualizar ficha en linea`() = runTest {
        coEvery { api.updateMuseum(4, any()) } returns Fixtures.museumDto(id = 4)

        assertTrue(repo.save(Fixtures.museum(id = 4, createdBy = 9), isNew = false))

        coVerify { museumDao.upsert(match { it.id == 4L && it.createdBy == 9L }) }
    }

    @Test fun `sin conexion la ficha nueva queda pendiente de sincronizar`() = runTest {
        coEvery { api.createMuseum(any()) } throws IOException()
        val saved = slot<MuseumEntity>()
        coEvery { museumDao.upsert(capture(saved)) } returns Unit

        assertFalse(repo.save(Fixtures.museum(id = 0), isNew = true))

        assertTrue(saved.captured.pendingSync)
        assertTrue(saved.captured.qrCode.startsWith("MUSEO:"))
        assertEquals(1L, saved.captured.createdBy)
    }

    @Test fun `sin conexion la edicion se guarda tal cual`() = runTest {
        val m = Fixtures.museum(id = 4)
        coEvery { api.updateMuseum(any(), any()) } throws IOException()

        assertFalse(repo.save(m, isNew = false))

        coVerify { museumDao.upsert(m) }
    }

    @Test fun `publicar usa la actualizacion`() = runTest {
        coEvery { api.updateMuseum(2, match { !it.published }) } returns Fixtures.museumDto(id = 2).copy(published = false)

        assertTrue(repo.setPublished(Fixtures.museum(id = 2), false))
    }

    @Test fun `eliminar ficha en el microservicio y en la cache`() = runTest {
        coEvery { api.deleteMuseum(3) } returns Response.success(Unit)

        assertTrue(repo.delete(Fixtures.museum(id = 3)))

        coVerify { museumDao.deleteById(3) }
    }

    @Test fun `eliminar ficha pendiente solo la borra localmente`() = runTest {
        assertTrue(repo.delete(Fixtures.museum(id = 9000, pendingSync = true)))

        coVerify { museumDao.deleteById(9000) }
        coVerify(exactly = 0) { api.deleteMuseum(any()) }
    }

    @Test fun `si el servidor rechaza o no responde no se elimina`() = runTest {
        coEvery { api.deleteMuseum(3) } returns Response.error(500, "".toResponseBody(null))
        assertFalse(repo.delete(Fixtures.museum(id = 3)))

        coEvery { api.deleteMuseum(3) } throws IOException()
        assertFalse(repo.delete(Fixtures.museum(id = 3)))

        coVerify(exactly = 0) { museumDao.deleteById(3) }
    }

    @Test fun `lecturas delegan en la cache`() = runTest {
        val m = Fixtures.museum()
        every { museumDao.observeAll(true) } returns flowOf(listOf(m))
        every { museumDao.observeById(1) } returns flowOf(m)
        coEvery { museumDao.getById(1) } returns m

        assertEquals(listOf(m), repo.observeMuseums(true).first())
        assertEquals(m, repo.observeMuseum(1).first())
        assertEquals(m, repo.getMuseum(1))
    }

    @Test fun `favoritos y consultados por usuario`() = runTest {
        val m = Fixtures.museum()
        every { favoriteDao.observeMuseums(1) } returns flowOf(listOf(m))
        every { historyDao.observeViewedMuseums(1) } returns flowOf(listOf(m))
        every { favoriteDao.observeIsFavorite(1, MuseumRepository.TYPE, 1) } returns flowOf(true)

        assertEquals(listOf(m), repo.observeFavorites().first())
        assertEquals(listOf(m), repo.observeViewed().first())
        assertTrue(repo.observeIsFavorite(1).first())

        repo.setFavorite(1, true)
        repo.setFavorite(1, false)
        repo.recordView(1)
        coVerify { favoriteDao.insert(match<FavoriteEntity> { it.userId == 1L && it.itemId == 1L && it.itemType == "MUSEUM" }) }
        coVerify { favoriteDao.delete(1, "MUSEUM", 1) }
        coVerify { historyDao.upsertView(match<MuseumViewEntity> { it.userId == 1L && it.museumId == 1L }) }
    }

    @Test fun `sin sesion no hay favoritos ni historial`() = runTest {
        userId.value = null

        assertTrue(repo.observeFavorites().first().isEmpty())
        assertTrue(repo.observeViewed().first().isEmpty())
        assertFalse(repo.observeIsFavorite(1).first())
        repo.setFavorite(1, true)
        repo.recordView(1)

        coVerify(exactly = 0) { favoriteDao.insert(any()) }
        coVerify(exactly = 0) { historyDao.upsertView(any()) }
    }
}
