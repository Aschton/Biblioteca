package cl.duoc.museo.data.remote

import cl.duoc.museo.testutil.Fixtures
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response

class MappersTest {

    @Test fun `museo dto a entidad y de vuelta`() {
        val e = Fixtures.museumDto(id = 7).toEntity()
        assertEquals(7L, e.id)
        assertEquals("MUSEO:7", e.qrCode)
        assertEquals("https://img.cl/a.jpg", e.imageUrl)

        val sinId = Fixtures.museumDto(id = null).copy(imageUrl = null, rating = null).toEntity()
        assertEquals(0L, sinId.id)
        assertEquals("MUSEO:0", sinId.qrCode)
        assertEquals("", sinId.imageUrl)
        assertEquals(0.0, sinId.rating, 0.0)

        assertEquals(7L, e.toDto().id)
        assertNull(e.toDto(sendId = false).id)
    }

    @Test fun `producto dto a entidad y de vuelta`() {
        val e = Fixtures.productDto(id = 3).toEntity()
        assertEquals("PROD:3", e.qrCode)
        assertEquals(3, e.stock)

        val sinId = Fixtures.productDto(id = null).copy(imageUrl = null, rating = null).toEntity()
        assertEquals("PROD:0", sinId.qrCode)
        assertEquals("", sinId.imageUrl)

        assertEquals(3L, e.toDto().id)
        assertNull(e.toDto(sendId = false).id)
    }

    @Test fun `ensureSuccess acepta 2xx`() {
        Response.success(Unit).ensureSuccess()
    }

    @Test(expected = IllegalStateException::class)
    fun `ensureSuccess lanza error si no es 2xx`() {
        Response.error<Unit>(404, "".toResponseBody(null)).ensureSuccess()
    }
}
