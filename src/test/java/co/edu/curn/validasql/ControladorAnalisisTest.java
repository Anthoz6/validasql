package co.edu.curn.validasql;

import co.edu.curn.validasql.web.ValidaSqlAplicacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** La API REST: las dos rutas del diseño y la regla de cuándo responde 400. */
@SpringBootTest(classes = ValidaSqlAplicacion.class)
@AutoConfigureMockMvc
class ControladorAnalisisTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("GET /api/esquema-ejemplo devuelve el esquema de la universidad y las consultas de prueba")
    void esquemaEjemplo() throws Exception {
        mvc.perform(get("/api/esquema-ejemplo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esquema").value(org.hamcrest.Matchers.containsString("CREATE TABLE matricula")))
                .andExpect(jsonPath("$.consultas").value(org.hamcrest.Matchers.containsString("SELECT")));
    }

    @Test
    @DisplayName("POST /api/analizar con el ejemplo 5 del anexo: 200 con M02 y M05 en el formato del anexo")
    void analizarEjemplo5() throws Exception {
        String cuerpo = """
                {"esquema": "CREATE TABLE estudiante (id INT PRIMARY KEY, nombre VARCHAR(60) NOT NULL, semestre INT, id_programa INT);",
                 "consultas": "SELECT nombr FROM estudiante WHERE semestre = 'cuarto';"}""";

        mvc.perform(post("/api/analizar").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen.tablas").value(1))
                .andExpect(jsonPath("$.resumen.sentencias").value(1))
                .andExpect(jsonPath("$.resumen.conErrores").value(1))
                .andExpect(jsonPath("$.consultas[0].estado").value("CON_ERRORES"))
                .andExpect(jsonPath("$.consultas[0].tokens.length()").value(9))
                .andExpect(jsonPath("$.consultas[0].traza.length()").value(22))
                .andExpect(jsonPath("$.consultas[0].diagnosticos[0].codigo").value("M02"))
                .andExpect(jsonPath("$.consultas[0].diagnosticos[0].columna").value(8))
                .andExpect(jsonPath("$.consultas[0].diagnosticos[0].texto").value(
                        "[M02] Error semántico (línea 1, columna 8). La columna «nombr» no existe en la tabla «estudiante». ¿Quisiste decir «nombre»?"))
                .andExpect(jsonPath("$.consultas[0].diagnosticos[1].codigo").value("M05"));
    }

    @Test
    @DisplayName("Un SQL con errores léxicos sigue siendo 200, con diagnósticos y sin árbol")
    void sqlInvalidoEs200() throws Exception {
        mvc.perform(post("/api/analizar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"esquema\": \"\", \"consultas\": \"SELECT # FROM t;\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consultas[0].diagnosticos[0].codigo").value("L01"))
                .andExpect(jsonPath("$.consultas[0].arbol").value(""))
                .andExpect(jsonPath("$.consultas[0].traza.length()").value(0));
    }

    @Test
    @DisplayName("Un cuerpo vacío o sin campos no es un error de la API")
    void camposAusentes() throws Exception {
        mvc.perform(post("/api/analizar").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen.sentencias").value(0));
    }

    @Test
    @DisplayName("Sin cuerpo la API responde 400")
    void sinCuerpo() throws Exception {
        mvc.perform(post("/api/analizar").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("La página principal se sirve como recurso estático")
    void paginaPrincipal() throws Exception {
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ValidaSQL")));
    }
}
