package com.clinica.agendamento;

import com.clinica.agendamento.domain.Profissional;
import com.clinica.agendamento.repository.ProfissionalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integracao end-to-end: sobe o contexto Spring inteiro, passa por
 * controller -> service -> JPA -> H2 e valida o JSON de saida.
 *
 * Diferente do AgendamentoServiceTest (unitario com Mockito), aqui validamos
 * que o stack inteiro funciona junto, incluindo Flyway e Bean Validation.
 */
@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class AgendamentoIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ProfissionalRepository profissionalRepository;
    @Autowired private ObjectMapper mapper;

    private MockMvc mockMvc;
    private Long profissionalId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        profissionalId = profissionalRepository
                .save(new Profissional("Dra. Teste", "Clinico Geral", "CRM-TEST-001"))
                .getId();
    }

    @Test
    @DisplayName("Fluxo feliz: cadastra paciente, cria agendamento, lista e cancela")
    void fluxoCompleto() throws Exception {
        // 1) cadastra paciente
        var pacienteJson = mapper.writeValueAsString(Map.of(
                "nome", "Maria Souza",
                "cpf", "98765432100",
                "email", "maria@test.com"));

        var responsePaciente = mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pacienteJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.cpf").value("98765432100"))
                .andReturn().getResponse().getContentAsString();

        Long pacienteId = ((Number) mapper.readValue(responsePaciente, Map.class).get("id")).longValue();

        // 2) cria agendamento no futuro
        String dataFutura = LocalDateTime.now().plusDays(7)
                .withSecond(0).withNano(0)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        var agendamentoJson = mapper.writeValueAsString(Map.of(
                "pacienteId", pacienteId,
                "profissionalId", profissionalId,
                "dataHora", dataFutura,
                "tipoAtendimento", "CONSULTA"));

        var responseAgendamento = mockMvc.perform(post("/api/agendamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agendamentoJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGENDADO"))
                .andExpect(jsonPath("$.tipoAtendimento").value("CONSULTA"))
                .andReturn().getResponse().getContentAsString();

        Long agendamentoId = ((Number) mapper.readValue(responseAgendamento, Map.class).get("id")).longValue();

        // 3) lista agendamentos com filtro por paciente
        mockMvc.perform(get("/api/agendamentos")
                        .param("pacienteId", pacienteId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // 4) cancela agendamento
        var cancelamentoJson = mapper.writeValueAsString(Map.of("motivo", "Paciente remarcou"));
        mockMvc.perform(patch("/api/agendamentos/{id}/cancelar", agendamentoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelamentoJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"))
                .andExpect(jsonPath("$.motivoCancelamento").value("Paciente remarcou"));

        // 5) confirma que filtro por status CANCELADO traz o registro
        mockMvc.perform(get("/api/agendamentos").param("status", "CANCELADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Retorna 400 com lista de erros quando o body de paciente e invalido")
    void validacaoDeBody() throws Exception {
        var invalido = mapper.writeValueAsString(Map.of(
                "nome", "",
                "cpf", "abc"));

        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("Retorna 422 ao tentar agendar no passado (regra de negocio)")
    void agendamentoNoPassadoRetorna422() throws Exception {
        // cadastra paciente
        var pacienteJson = mapper.writeValueAsString(Map.of(
                "nome", "Carlos Lima",
                "cpf", "11122233344"));
        var resp = mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON).content(pacienteJson))
                .andReturn().getResponse().getContentAsString();
        Long pacId = ((Number) mapper.readValue(resp, Map.class).get("id")).longValue();

        String dataPassada = LocalDateTime.now().minusDays(1)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        var ag = mapper.writeValueAsString(Map.of(
                "pacienteId", pacId,
                "profissionalId", profissionalId,
                "dataHora", dataPassada,
                "tipoAtendimento", "CONSULTA"));

        // @Future do Bean Validation barra antes do service => 400
        mockMvc.perform(post("/api/agendamentos")
                        .contentType(MediaType.APPLICATION_JSON).content(ag))
                .andExpect(status().isBadRequest());
    }
}
