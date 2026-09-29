package com.inventories.infr.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Permitir solicitudes desde todos los orígenes
        /*config.addAllowedOrigin("http://localhost:5173");
        config.addAllowedOrigin("http://192.168.0.6:5173");
        config.addAllowedOrigin("http://192.168.0.11:5173");*/
        config.addAllowedOrigin("*");
        // Permitir todos los métodos (GET, POST, PUT, DELETE, etc.)
        config.addAllowedMethod("*");

        // Permitir los encabezados que desees (por ejemplo, "Content-Type", "Authorization")
        config.addAllowedHeader("*");

        // Esta es la clave para permitir cookies, por ejemplo, para la autenticación basada en cookies
//        config.setAllowCredentials(false);
        config.setAllowCredentials(false);

        // Especificar los encabezados que pueden ser expuestos al cliente
        config.addExposedHeader("filename");

        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
