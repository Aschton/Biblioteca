package cl.duoc.museo.catalogo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Prueba de integración del catálogo (usa H2, ver src/test/resources). */
@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void filtraPorCategoria() throws Exception {
        mvc.perform(get("/api/products").param("category", "CURSO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void descuentaStockYRechazaSiNoAlcanza() throws Exception {
        // Producto 8: réplica de cerámica, stock inicial 8
        mvc.perform(post("/api/products/8/decrease-stock").contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(5));
        mvc.perform(post("/api/products/8/decrease-stock").contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":50}"))
                .andExpect(status().isConflict());
    }

    @Test
    void informaDisponibilidad() throws Exception {
        mvc.perform(get("/api/products/1/availability").param("qty", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }
}
