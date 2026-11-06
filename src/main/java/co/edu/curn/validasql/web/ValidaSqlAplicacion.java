package co.edu.curn.validasql.web;

import co.edu.curn.validasql.servicio.ServicioAnalisis;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Arranque de la interfaz web. Este es el único paquete que conoce Spring: el núcleo de análisis
 * es Java puro y aquí solo se registra como un bean.
 */
@SpringBootApplication
public class ValidaSqlAplicacion {

    public static void main(String[] args) {
        SpringApplication.run(ValidaSqlAplicacion.class, args);
    }

    /** {@link ServicioAnalisis} no guarda estado entre análisis, así que una sola instancia sirve para todos. */
    @Bean
    ServicioAnalisis servicioAnalisis() {
        return new ServicioAnalisis();
    }
}
