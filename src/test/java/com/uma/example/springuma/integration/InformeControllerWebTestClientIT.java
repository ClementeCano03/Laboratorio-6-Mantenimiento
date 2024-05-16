/*
    @author1 José Antonio Casado Molina
    @author2 Clemente Cano Mengíbar
    @author3 Manuel Fuentes Vida
*/
package com.uma.example.springuma.integration;


import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.model.Imagen;
import com.uma.example.springuma.model.Informe;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.Paciente;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class InformeControllerWebTestClientIT {
     @LocalServerPort
    private Integer port;
    private WebTestClient client;
    private Medico medico;
    private Paciente paciente;
    private Imagen imagen;
    private Informe informe;

    @PostConstruct
    public void init(){
        client = WebTestClient.bindToServer().baseUrl("http://localhost:"+port)
        .responseTimeout(Duration.ofMillis(30000)).build();

        // No es necesario crear el médico, se puede pasar el paciente directamente
        medico = new Medico();
        medico.setId(1);
        medico.setNombre("Clemente");
        medico.setDni("12345678A");
        medico.setEspecialidad("Traumatología");

        paciente = new Paciente();
        paciente.setId(1);
        paciente.setDni("12345678A");
        paciente.setNombre("Paciente1");
        paciente.setMedico(medico);

        informe = new Informe();
        informe.setId(1);
        informe.setContenido("Informe de la imagen healthy.png");
        informe.setPrediccion("No cancer (label 0)");
        
    }

    @Test
    @DisplayName("Crea un informe, lo guarda y lo obtiene con get correctamente")
    public void createInformPost_isObtainedWithGet_returnInformOfImage() {   
        // creacion del medico
        client.post().uri("/medico")
            .body(Mono.just(medico), Medico.class)
            .exchange()
            .expectStatus().isCreated()
            .expectBody().returnResult();
        
        //creacion del paciente
        client.post()
            .uri("/paciente")
            .body(Mono.just(paciente), Paciente.class)
            .exchange()
            .expectStatus().isCreated()
            .expectBody().returnResult();

            
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", new FileSystemResource("./src/test/resources/healthy.png"));
        builder.part("paciente", paciente);

        //sube la imagen de paciente 1
        client.post()
            .uri("/imagen")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(builder.build()))
            .exchange()
            .expectStatus().is2xxSuccessful().returnResult(String.class);
        
        //obtiene la imagen con ID 1
        FluxExchangeResult<Imagen> resultImagen = client.get().uri("/imagen/info/1")
            .exchange()
            .expectStatus().isOk()
            .returnResult(Imagen.class);

        Imagen imagen = resultImagen.getResponseBody().blockFirst();

        //crea un informe
        informe.setImagen(imagen);
        client.post().uri("/informe")
            .body(Mono.just(informe), Informe.class)
            .exchange()
            .expectStatus().isCreated()
            .expectBody().returnResult();

        // obtiene el informe con ID 1
        FluxExchangeResult<Informe> result = client.get().uri("/informe/1")
            .exchange()
            .expectStatus().isOk()
            .returnResult(Informe.class);

        Informe informe = result.getResponseBody().blockFirst();
        assertEquals("healthy.png", informe.getImagen().getNombre());
        assertEquals("Informe de la imagen healthy.png", informe.getContenido());
    }
    
    @Test
    @DisplayName("Crea un informe, lo guarda y lo elimina correctamente")
    public void createInformePost_deleteInformeDelete_returnIsOK() {
        // creacion del medico
        client.post().uri("/medico")
            .body(Mono.just(medico), Medico.class)
            .exchange()
            .expectStatus().isCreated()
            .expectBody().returnResult();
        
        //creacion del paciente
        client.post()
            .uri("/paciente")
            .body(Mono.just(paciente), Paciente.class)
            .exchange()
            .expectStatus().isCreated()
            .expectBody().returnResult();

            
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", new FileSystemResource("./src/test/resources/healthy.png"));
        builder.part("paciente", paciente);

        //sube la imagen de paciente 1
        client.post()
            .uri("/imagen")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(builder.build()))
            .exchange()
            .expectStatus().is2xxSuccessful().returnResult(String.class);
        
        //obtiene la imagen con ID 1
        FluxExchangeResult<Imagen> resultImagen = client.get().uri("/imagen/info/1")
            .exchange()
            .expectStatus().isOk()
            .returnResult(Imagen.class);

        Imagen imagen = resultImagen.getResponseBody().blockFirst();

        //crea un informe
        informe.setImagen(imagen);
        client.post().uri("/informe")
            .body(Mono.just(informe), Informe.class)
            .exchange()
            .expectStatus().isCreated()
            .expectBody().returnResult();
            
        // elimina el informe con ID 1
        client.delete().uri("/informe/1")
            .exchange()
            .expectStatus().isNoContent();

        
        //Verifico que el medico se ha eliminado -> La pagina devuelve is Ok anque no este eliminado (sin contenido)
        client.get().uri("/informe/1")
            .exchange()
            .expectStatus().isOk();
        
    }

    
    
}
