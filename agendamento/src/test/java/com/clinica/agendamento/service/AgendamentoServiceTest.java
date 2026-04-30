package com.clinica.agendamento.service;

import com.clinica.agendamento.domain.Agendamento;
import com.clinica.agendamento.domain.Paciente;
import com.clinica.agendamento.domain.Profissional;
import com.clinica.agendamento.domain.enums.StatusAgendamento;
import com.clinica.agendamento.domain.enums.TipoAtendimento;
import com.clinica.agendamento.dto.AgendamentoRequest;
import com.clinica.agendamento.dto.AgendamentoResponse;
import com.clinica.agendamento.dto.CancelamentoRequest;
import com.clinica.agendamento.exception.BusinessException;
import com.clinica.agendamento.exception.ConflictException;
import com.clinica.agendamento.exception.NotFoundException;
import com.clinica.agendamento.repository.AgendamentoRepository;
import com.clinica.agendamento.repository.PacienteRepository;
import com.clinica.agendamento.repository.ProfissionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Testes unitarios das regras de negocio do AgendamentoService.
 * Cobre todos os requisitos do PRD: data no futuro, profissional sem conflito,
 * cancelamento com motivo, idempotencia do cancelamento e race condition.
 */
@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private ProfissionalRepository profissionalRepository;

    private AgendamentoService service;

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 5, 1, 10, 0);

    @BeforeEach
    void setUp() {
        Clock clockFixo = Clock.fixed(
                AGORA.atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault());
        service = new AgendamentoService(
                agendamentoRepository, pacienteRepository, profissionalRepository, clockFixo);
    }

    // ---------- Regra: nao permitir data/hora no passado ----------

    @Test
    @DisplayName("Deve recusar agendamento com data no passado")
    void recusaAgendamentoNoPassado() {
        AgendamentoRequest req = new AgendamentoRequest(
                1L, 1L, AGORA.minusHours(1), TipoAtendimento.CONSULTA);

        assertThatThrownBy(() -> service.criar(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    @DisplayName("Deve recusar agendamento na data/hora exata de agora")
    void recusaAgendamentoAgoraMesmo() {
        AgendamentoRequest req = new AgendamentoRequest(
                1L, 1L, AGORA, TipoAtendimento.CONSULTA);

        assertThatThrownBy(() -> service.criar(req))
                .isInstanceOf(BusinessException.class);
    }

    // ---------- Regra: profissional/paciente devem existir ----------

    @Test
    @DisplayName("Deve devolver 404 quando paciente nao existe")
    void recusaQuandoPacienteNaoExiste() {
        when(pacienteRepository.findById(99L)).thenReturn(Optional.empty());
        AgendamentoRequest req = new AgendamentoRequest(
                99L, 1L, AGORA.plusDays(1), TipoAtendimento.CONSULTA);

        assertThatThrownBy(() -> service.criar(req))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Paciente");
    }

    @Test
    @DisplayName("Deve devolver 404 quando profissional nao existe")
    void recusaQuandoProfissionalNaoExiste() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(pacienteFake()));
        when(profissionalRepository.findById(99L)).thenReturn(Optional.empty());
        AgendamentoRequest req = new AgendamentoRequest(
                1L, 99L, AGORA.plusDays(1), TipoAtendimento.CONSULTA);

        assertThatThrownBy(() -> service.criar(req))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Profissional");
    }

    // ---------- Regra: profissional sem dois agendamentos no mesmo horario ----------

    @Test
    @DisplayName("Deve recusar quando profissional ja tem agendamento no horario")
    void recusaConflitoDeHorario() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(pacienteFake()));
        when(profissionalRepository.findById(1L)).thenReturn(Optional.of(profissionalFake()));
        when(agendamentoRepository.existsByProfissionalIdAndDataHoraAndStatusNot(
                any(), any(), any())).thenReturn(true);

        AgendamentoRequest req = new AgendamentoRequest(
                1L, 1L, AGORA.plusDays(1), TipoAtendimento.CONSULTA);

        assertThatThrownBy(() -> service.criar(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("agendamento neste horario");
    }

    @Test
    @DisplayName("Deve mapear violacao de constraint unica para ConflictException (race condition)")
    void mapeiaConstraintViolationParaConflict() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(pacienteFake()));
        when(profissionalRepository.findById(1L)).thenReturn(Optional.of(profissionalFake()));
        when(agendamentoRepository.existsByProfissionalIdAndDataHoraAndStatusNot(
                any(), any(), any())).thenReturn(false);
        when(agendamentoRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("uk_agendamento_profissional_horario"));

        AgendamentoRequest req = new AgendamentoRequest(
                1L, 1L, AGORA.plusDays(1), TipoAtendimento.CONSULTA);

        assertThatThrownBy(() -> service.criar(req))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Deve criar agendamento com sucesso quando tudo esta valido")
    void criaAgendamentoComSucesso() {
        Paciente p = pacienteFake();
        Profissional pr = profissionalFake();
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(p));
        when(profissionalRepository.findById(1L)).thenReturn(Optional.of(pr));
        when(agendamentoRepository.existsByProfissionalIdAndDataHoraAndStatusNot(
                any(), any(), any())).thenReturn(false);
        when(agendamentoRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        AgendamentoRequest req = new AgendamentoRequest(
                1L, 1L, AGORA.plusDays(1), TipoAtendimento.CONSULTA);

        AgendamentoResponse resp = service.criar(req);

        assertThat(resp.status()).isEqualTo(StatusAgendamento.AGENDADO);
        assertThat(resp.tipoAtendimento()).isEqualTo(TipoAtendimento.CONSULTA);
        assertThat(resp.dataHora()).isEqualTo(AGORA.plusDays(1));
    }

    // ---------- Regra: cancelamento muda status, mantem registro, exige motivo ----------

    @Test
    @DisplayName("Cancelar deve mudar status para CANCELADO e registrar motivo")
    void cancelamentoMudaStatusEGuardaMotivo() {
        Agendamento existente = new Agendamento(
                pacienteFake(), profissionalFake(),
                AGORA.plusDays(1), TipoAtendimento.CONSULTA);
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(existente));

        AgendamentoResponse resp = service.cancelar(10L, new CancelamentoRequest("Paciente desistiu"));

        assertThat(resp.status()).isEqualTo(StatusAgendamento.CANCELADO);
        assertThat(resp.motivoCancelamento()).isEqualTo("Paciente desistiu");
        assertThat(resp.canceladoEm()).isNotNull();
    }

    @Test
    @DisplayName("Nao deve permitir cancelar agendamento ja cancelado")
    void naoPermiteRecancelar() {
        Agendamento existente = new Agendamento(
                pacienteFake(), profissionalFake(),
                AGORA.plusDays(1), TipoAtendimento.CONSULTA);
        existente.cancelar("primeiro cancelamento");
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.cancelar(10L, new CancelamentoRequest("tentando de novo")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Deve devolver 404 ao cancelar agendamento inexistente")
    void cancelarInexistenteRetornaNotFound() {
        lenient().when(agendamentoRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelar(99L, new CancelamentoRequest("motivo")))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- helpers ----------

    private static Paciente pacienteFake() {
        return new Paciente("Joao Silva", "12345678901", "joao@test.com", "11999999999", null);
    }

    private static Profissional profissionalFake() {
        return new Profissional("Dra. Ana", "Cardiologia", "CRM-SP-12345");
    }
}
