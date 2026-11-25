package cl.duoc.museo.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {
    @Test fun `correo valido e invalido`() {
        assertTrue(Validators.isEmail("cliente@museo.cl"))
        assertTrue(Validators.isEmail("  admin@museo.cl  "))
        assertFalse(Validators.isEmail("cliente@museo"))
        assertFalse(Validators.isEmail("sin-arroba.cl"))
    }

    @Test fun `contrasena fuerte requiere letras numeros y 6 caracteres`() {
        assertTrue(Validators.isStrongPassword("Admin123"))
        assertFalse(Validators.isStrongPassword("abc12"))
        assertFalse(Validators.isStrongPassword("solotexto"))
        assertFalse(Validators.isStrongPassword("12345678"))
    }

    @Test fun `url debe partir con http o https`() {
        assertTrue(Validators.isUrl("https://museo.cl"))
        assertTrue(Validators.isUrl("http://museo.cl"))
        assertFalse(Validators.isUrl("museo.cl"))
    }
}
