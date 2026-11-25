package cl.duoc.museo.museos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Prueba de integración del CRUD de museos (usa H2, ver src/test/resources). */
@SpringBootTest
@AutoConfigureMockMvc
class MuseumControllerTest {

    @Autowired MockMvc mvc;

    private static final String BODY = """
            {"name":"Museo de Prueba","country":"Chile","city":"Valparaíso","latitude":-33.04,"longitude":-71.62,
             "description":"Ficha creada desde la prueba automática","classification":"Historia",
             "project":"Red Iberoamericana","virtualTourUrl":"https://ejemplo.cl/tour"}
            """;

    @Test
    void listaLosMuseosDeDemostracion() throws Exception {
        mvc.perform(get("/api/museums")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(12));
    }

    @Test
    void creaYEliminaUnMuseo() throws Exception {
        String json = mvc.perform(post("/api/museums").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.qrCode").exists())
                .andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(json.replaceAll(".*\"id\":(\\d+).*", "$1"));

        mvc.perform(delete("/api/museums/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/museums/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void rechazaUnaFichaInvalida() throws Exception {
        mvc.perform(post("/api/museums").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resuelveQrDeMuseo() throws Exception {
        mvc.perform(get("/api/qr/MUSEO:1")).andExpect(status().isOk()).andExpect(jsonPath("$.type").value("MUSEUM"));
        mvc.perform(get("/api/qr/PROD:1")).andExpect(status().isBadRequest());
    }
}
