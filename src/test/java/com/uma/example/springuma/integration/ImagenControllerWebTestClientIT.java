/*
 * @author1 José Antonio Casado Molina
 * @author2 Clemente Cano Mengíbar
 * @author3 Manuel Fuentes Vida
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
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.Paciente;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ImagenControllerWebTestClientIT {

    @LocalServerPort
    private Integer port;
    private WebTestClient client;
    private Medico medico;
    private Paciente paciente;
    //private Imagen imagen;

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

        /*imagen = new Imagen();
        imagen.setId(1);
        imagen.setNombre("healthy.png");
        imagen.setPaciente(paciente);*/
    }

    @Test
    @DisplayName("Subir una imagen de un paciente debe devolver un mensaje de éxito")
	void saveImagePost_imageFromAPacient_shouldRespondValidResponse() throws IOException {
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

        //sube la imagen de paciente 1
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", new FileSystemResource("./src/test/resources/healthy.png"));
        builder.part("paciente", paciente);

        //captura el resultado del request post en /imagen
        FluxExchangeResult<String> responseBody =  client.post()
            .uri("/imagen")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(builder.build()))
            .exchange()
            .expectStatus().is2xxSuccessful().returnResult(String.class);
        
        String result = responseBody.getResponseBody().blockFirst();
        String expected = "{\"response\" : \"file uploaded successfully : healthy.png\"}";

        //comprueba que el resultado es el esperado
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Subir la imagen sana nos devuelve que no tiene cáncer")
	void saveImageHealthy_getPrediction_returnNoCancer() throws IOException {
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
            
        FluxExchangeResult<String> response = client.get()
            .uri("/imagen/predict/1")
            .exchange()
            .expectStatus().isOk()
            .returnResult(String.class);

        String result = response.getResponseBody().blockFirst();
        String expected = "Not cancer (label 0)";
        assertTrue(result.contains(expected));
    }

    @Test
    @DisplayName("Subir la imagen enferma nos devuelve que sí tiene cáncer")
	void saveImageNotHealthy_getPrediction_returnCancer() throws Exception {
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
        builder.part("image", new FileSystemResource("./src/test/resources/no_healthy.png"));
        builder.part("paciente", paciente);

        //sube la imagen de paciente 1
        client.post()
            .uri("/imagen")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(builder.build()))
            .exchange()
            .expectStatus().is2xxSuccessful().returnResult(String.class);
            
        FluxExchangeResult<String> response = client.get()
            .uri("/imagen/predict/1")
            .exchange()
            .expectStatus().isOk()
            .returnResult(String.class);

        String result = response.getResponseBody().blockFirst();
        String expected = "Cancer (label 1)";
        assertTrue(result.contains(expected));
    }
    
}
