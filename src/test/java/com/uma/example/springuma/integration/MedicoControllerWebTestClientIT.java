package com.uma.example.springuma.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
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

import com.uma.example.springuma.model.Medico;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MedicoControllerWebTestClientIT {
    
    @LocalServerPort
    private Integer port;
    private WebTestClient client;
    private Medico medico;

    @PostConstruct
    public void init(){
        client = WebTestClient.bindToServer().baseUrl("http://localhost:"+port)
        .responseTimeout(Duration.ofMillis(30000)).build();

        medico = new Medico();
        medico.setId(1);
        medico.setNombre("Clemente");
        medico.setDni("12345678A");
        medico.setEspecialidad("Traumatología");
    }

    @Test
    @DisplayName("Crea un médico, lo guarda y lo obtiene con get correctamente")
    public void createMedicoPost_isObtainedWithGet() {
        // crea una medico
        client.post().uri("/medico")
            .body(Mono.just(medico), Medico.class)
            .exchange()
            .expectStatus().isCreated()
            .expectBody().returnResult();
        
        // obtiene el medico con ID 1
        FluxExchangeResult<Medico> result = client.get().uri("/medico/1")
            .exchange()
            .expectStatus().isOk().returnResult(Medico.class); // comprueba que la respuesta es de tipo medico

        Medico medicoObtained = result.getResponseBody().blockFirst(); // Obtiene el objeto medico en concreto

        assertEquals(medico.toString(), medicoObtained.toString());
    }


    @Test
    @DisplayName("Crea un médico, lo modifica y lo obtiene con get correctamente")
    public void updateMedicoPut_nameChanged_isUpdated() {
        // crea un medico
        client.post().uri("/medico")
            .body(Mono.just(medico), Medico.class)
            .exchange()
            .expectStatus().isCreated()
            .expectBody().returnResult();

        // modifica el medico
        medico.setNombre("Clemente Modificado");
        client.put().uri("/medico")
            .body(Mono.just(medico), Medico.class)
            .exchange()
            .expectStatus().isNoContent()
            .expectBody().returnResult();

        // obtiene el medico con ID 1
        FluxExchangeResult<Medico> result = client.get().uri("/medico/1")
            .exchange()
            .expectStatus().isOk().returnResult(Medico.class); // comprueba que la respuesta es de tipo medico

        Medico medicoObtained = result.getResponseBody().blockFirst(); // Obtiene el objeto medico en concreto

        assertEquals(medico.toString(), medicoObtained.toString());
    }

    @Test
    @DisplayName("Crea un médico y lo elimina correctamente")
    public void deleteMedico_byId_isEliminated(){
        // crea un medico
        client.post().uri("/medico")
            .body(Mono.just(medico), Medico.class)
            .exchange()
            .expectStatus().isCreated()
            .expectBody().returnResult();

        
        FluxExchangeResult<Medico> result = client.get().uri("/medico/1")
            .exchange()
            .expectStatus().isOk().returnResult(Medico.class); // comprueba que la respuesta es de tipo medico
    
        //elimino el medico de ID 1
        client.delete().uri("/medico/1")
            .exchange()
            .expectStatus().isOk();
            
        //Verifico que el medico se ha eliminado -> La pagina devuelve ERROR 5xx
        client.get().uri("/medico/1")
            .exchange()
            .expectStatus().is5xxServerError();
        
    }

    @Test
    @DisplayName("Busca un médico por su dni y lo obtiene con get correctamente")
    public void getMedicoByDNIGet_searchByDni_isObtained(){
        // crea un medico
        client.post().uri("/medico")
            .body(Mono.just(medico), Medico.class)
            .exchange()
            .expectStatus().isCreated()
            .expectBody().returnResult();
        
        // obtiene el medico con DNI 12345678A
        FluxExchangeResult<Medico> result = client.get().uri("/medico/dni/12345678A")
            .exchange()
            .expectStatus().isOk().returnResult(Medico.class); // comprueba que la respuesta es de tipo medico

        Medico medicoObtained = result.getResponseBody().blockFirst(); // Obtiene el objeto medico en concreto

        assertEquals(medico.toString(), medicoObtained.toString());
    }
}
