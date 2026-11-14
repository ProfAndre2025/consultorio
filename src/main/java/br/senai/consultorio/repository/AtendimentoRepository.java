package br.senai.consultorio.repository;

import br.senai.consultorio.domain.model.Atendimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

    Optional<Atendimento> findByConsultaId(Long consultaId);

    /**
     * Busca EM LOTE os atendimentos de varias consultas.
     * Sem isto, montar uma pagina de 20 consultas dispararia 20 consultas
     * extras ao banco -- o classico problema N+1.
     */
    List<Atendimento> findByConsultaIdIn(Collection<Long> consultaIds);

    /** Carrega tudo o que o comprovante em PDF precisa, em uma unica consulta. */
    @Query("""
            select distinct a
              from Atendimento a
              join fetch a.consulta c
              join fetch c.paciente
              join fetch c.dentista
              join fetch c.procedimento
              left join fetch a.materiais
             where a.id = :id
            """)
    Optional<Atendimento> buscarCompleto(@Param("id") Long id);

    /**
     * Historico clinico de um paciente: os atendimentos JA FINALIZADOS, do mais
     * recente para o mais antigo.
     *
     * Traz procedimento, dentista e materiais na mesma consulta (join fetch)
     * para o dentista abrir o historico sem disparar uma consulta por linha.
     * So finalizados entram -- atendimento em curso ainda nao e historico.
     */
    @Query("""
            select distinct a
              from Atendimento a
              join fetch a.consulta c
              join fetch c.dentista
              join fetch c.procedimento
              left join fetch a.materiais
             where c.paciente.id = :pacienteId
               and a.finalizado  = true
             order by a.realizadoEm desc
            """)
    List<Atendimento> buscarHistoricoDoPaciente(@Param("pacienteId") Long pacienteId);
}
