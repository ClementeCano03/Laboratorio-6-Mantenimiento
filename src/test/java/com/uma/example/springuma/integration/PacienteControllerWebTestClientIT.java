package com.uma.example.springuma.integration;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.uma.example.springuma.model.Paciente;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PacienteControllerWebTestClientIT {
    
    @LocalServerPort
    private Integer port;
    private WebTestClient client;
    private Medico medico;
    private Paciente paciente;

    @PostConstruct
    public void init(){
        client = WebTestClient.bindToServer().baseUrl("http://localhost:"+port)
        .responseTimeout(Duration.ofMillis(30000)).build();

        medico = new Medico();
        medico.setId(1);
        medico.setNombre("Clemente");
        medico.setDni("12345678A");
        medico.setEspecialidad("Traumatología");

        paciente = new Paciente();
        paciente.setId(1);
        paciente.setNombre("Manuel");
        paciente.setDni("741852963X");
        paciente.setEdad(20);
        paciente.setMedico(medico);
        paciente.setCita("30/06/2026");
    }

    @Test
    @DisplayName("Creamos un paciente, lo guardamos y lo asociamos a un médico previamente guardado")
    public void createPacientPost_AsociatedWithMedico(){
        // crea un medico
        client.post().uri("/medico")
            .body(Mono.just(medico), Medico.class)
            .exchange()
            .expectStatus().isCreated()
            .expectBody().returnResult();
            
    	// crea un paciente
        client.post().uri("/paciente")
            .body(Mono.just(paciente), Paciente.class)
            .exchange()
            .expectStatus().isCreated()
            .expectBody().returnResult();

            //Comprobamos que el paciente y el medico estan vinculados
        FluxExchangeResult<List> result = client.get().uri("/paciente/medico/1")
            .exchange()
            .expectStatus().isOk().returnResult(List.class); // comprueba que la reffspuesta es de tipo List

        List pacientesObtained = result.getResponseBody().blockFirst(); // Obtiene el objeto List<Paciente> en concreto

        assertTrue(pacientesObtained.contains(paciente));
        
    }

    @Test
    @DisplayName("Crea un paciente, lo modifica y lo obtiene con get correctamente")
    public void createPacientPost_UpdatePacientGet(){
        // crea un paciente
        client.post().uri("/paciente")
            .body(Mono.just(paciente), Paciente.class)
            .exchange()
            .expectStatus().isCreated()
            .expectBody().returnResult();

        // modifica el paciente
        paciente.setNombre("Manuelito");
        paciente.setEdad(21);
        paciente.setCita("30/06/2027");
        paciente.setId(2);

        client.put().uri("/paciente")
            .body(Mono.just(paciente), Paciente.class)
            .exchange()
            .expectStatus().isOk()
            .expectBody().returnResult();

        // obtiene el paciente
        FluxExchangeResult<Paciente> result = client.get().uri("/paciente/2")
            .exchange()
            .expectStatus().isOk().returnResult(Paciente.class); // comprueba que la respuesta es de tipo paciente

        Paciente pacienteResult = result.getResponseBody().blockFirst(); // Obtiene el objeto paciente en concreto
        
        assertEquals(paciente.toString(), pacienteResult.toString());
    }

}