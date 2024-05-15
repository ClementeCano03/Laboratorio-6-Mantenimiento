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
        paciente.setDni("87654321A");
        paciente.setNombre("Paciente1");
        paciente.setMedico(medico);
    }

    @Test
    @DisplayName("Crea un paciente, lo guarda, lo modifica y lo obtiene con get correctamente")
    public void createPacientPost_modifyThePacientPut_returnTheModifiedPacient(){
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

        // modifica el paciente
        paciente.setNombre("Manuelito");
        paciente.setEdad(21);
        paciente.setCita("30/06/2027");
        
        client.put().uri("/paciente")
            .body(Mono.just(paciente), Paciente.class)
            .exchange()
            .expectStatus().isNoContent()
            .expectBody().returnResult();

        // obtiene el paciente
        FluxExchangeResult<Paciente> result = client.get().uri("/paciente/1")
            .exchange()
            .expectStatus().isOk().returnResult(Paciente.class); // comprueba que la respuesta es de tipo paciente

        Paciente pacienteResult = result.getResponseBody().blockFirst(); // Obtiene el objeto paciente en concreto
        
        assertEquals(paciente.toString(), pacienteResult.toString());
    }

    @Test
    @DisplayName("Creamos un paciente, lo guardamos, se asocia a un médico y obtenemos la lista de pacientes del médico correctamente")
    public void createPacientPost_asociatedWithMedico_returnListOfPacients(){
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
        client.get().uri("/paciente/medico/1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueEquals("Content-Type", "application/json")  // comprueba que la respuesta es de tipo json
            .expectBody().jsonPath("$", hasSize(1));// comprueba que la respuesta tenga un array con tamanyo 1
        
    }

    @Test
    @DisplayName("Creamos un paciente, lo guardamos y lo asociamos a un médico previamente guardado. Le cambiamos el médico y comprobamos que se ha cambiado correctamente")
    public void createPacientPost_changeMedico_returnThePacient(){
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
        client.get().uri("/paciente/medico/1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueEquals("Content-Type", "application/json")  // comprueba que la respuesta es de tipo json
            .expectBody().jsonPath("$", hasSize(1));// comprueba que la respuesta tenga un array con tamanyo 1
        
        // cambiamos el medico. Primero lo creamos
        Medico medico2 = new Medico();
        medico2.setId(2);
        medico2.setNombre("Jose");
        medico2.setDni("87654321B");
        medico2.setEspecialidad("Cardiología");
        paciente.setMedico(medico2);

        // guardamos el médico nuevo
        client.post().uri("/medico")
            .body(Mono.just(medico2), Medico.class)
            .exchange()
            .expectStatus().isCreated()
            .expectBody().returnResult();
        
       
        //Comprobamos que el paciente y el medico estan vinculados
        client.get().uri("/paciente/medico/2")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueEquals("Content-Type", "application/json")  // comprueba que la respuesta es de tipo json
            .expectBody().jsonPath("$", hasSize(1));// comprueba que la respuesta tenga un array con tamanyo 1
        
    }

    @Test
    @DisplayName("Crea un paciente y lo elimina correctamente")
    public void createPacientPost_deletePacient(){
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

        // obtiene el paciente
        FluxExchangeResult<Paciente> result = client.get().uri("/paciente/1")
            .exchange()
            .expectStatus().isOk().returnResult(Paciente.class); // comprueba que la respuesta es de tipo paciente
        
        //Eliminamos el paciente
        client.delete().uri("/paciente/1")
            .exchange()
            .expectStatus().isOk();
            
        //Verifico que el medico se ha eliminado -> La pagina devuelve ERROR 5xx
        client.get().uri("/paciente/1")
            .exchange()
            .expectStatus().is5xxServerError();
    }
}