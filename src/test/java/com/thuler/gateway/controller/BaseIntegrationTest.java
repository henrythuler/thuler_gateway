package com.thuler.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thuler.gateway.domain.repository.CobrancaRepository;
import com.thuler.gateway.domain.repository.ContaRepository;
import com.thuler.gateway.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UsuarioRepository usuarioRepository;

    @Autowired
    protected ContaRepository contaRepository;

    @Autowired
    protected CobrancaRepository cobrancaRepository;

    @BeforeEach
    void cleanDatabase() {
        cobrancaRepository.deleteAll();
        contaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }
}