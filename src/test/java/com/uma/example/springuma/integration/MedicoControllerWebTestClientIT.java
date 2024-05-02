package com.uma.example.springuma.integration;

import java.time.Duration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.uma.example.springuma.model.Medico;

import jakarta.annotation.PostConstruct;

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
}
