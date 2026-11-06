package co.edu.curn.validasql.web;

import co.edu.curn.validasql.servicio.EsquemaEjemplo;
import co.edu.curn.validasql.servicio.ServicioAnalisis;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * API REST de ValidaSQL. Un SQL con errores no es un error de la API: sus diagnósticos van en una
 * respuesta 200. Solo un cuerpo ausente o ilegible responde 400.
 */
@RestController
@RequestMapping("/api")
public class ControladorAnalisis {

    private final ServicioAnalisis servicio;

    public ControladorAnalisis(ServicioAnalisis servicio) {
        this.servicio = servicio;
    }

    /** Esquema de la universidad y unas consultas de prueba, para el botón «Cargar ejemplo». */
    @GetMapping("/esquema-ejemplo")
    public Map<String, String> esquemaEjemplo() {
        return Map.of("esquema", EsquemaEjemplo.esquema(), "consultas", EsquemaEjemplo.consultas());
    }

    @PostMapping("/analizar")
    public RespuestaAnalisis analizar(@RequestBody PeticionAnalisis peticion) {
        return RespuestaAnalisis.de(servicio.analizar(peticion.esquema(), peticion.consultas()));
    }
}
