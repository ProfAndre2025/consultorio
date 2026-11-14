package br.senai.consultorio.repository;

import br.senai.consultorio.domain.enums.StatusConsulta;
import br.senai.consultorio.domain.model.Consulta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    /**
     * Filtro unico da agenda. Todos os parametros aceitam null, e o "is null"
     * dentro do JPQL desliga o criterio correspondente -- assim evitamos montar
     * SQL concatenando strings.
     *
     * Aqui NAO usamos "join fetch". Numa consulta paginada, o join fetch obriga
     * o Hibernate a trazer TODAS as linhas e paginar em memoria (aviso
     * HHH000104), perdendo o limit/offset do banco. A solucao correta e o
     * @EntityGraph: ele carrega paciente, dentista e procedimento junto (matando
     * o N+1 que o ConsultaMapper causaria), mas por left join que respeita a
     * paginacao no banco. Por isso tambem nao ha mais countQuery manual: sem
     * join na propria @Query, o count derivado pelo Spring Data ja fica correto.
     */
    @EntityGraph(attributePaths = {"paciente", "dentista", "procedimento"})
    @Query("""
            select c
              from Consulta c
             where (:inicio is null or c.dataHora >= :inicio)
               and (:fim    is null or c.dataHora <= :fim)
               and (:status is null or c.status = :status)
               and (:termo  is null
                    or lower(c.paciente.nome)     like lower(concat('%', :termo, '%'))
                    or lower(c.dentista.nome)     like lower(concat('%', :termo, '%'))
                    or lower(c.procedimento.nome) like lower(concat('%', :termo, '%')))
             order by c.dataHora
            """)
    Page<Consulta> buscarAgenda(@Param("inicio") LocalDateTime inicio,
                                @Param("fim") LocalDateTime fim,
                                @Param("status") StatusConsulta status,
                                @Param("termo") String termo,
                                Pageable paginacao);

    @Query("""
            select c
              from Consulta c
              join fetch c.paciente
              join fetch c.dentista
              join fetch c.procedimento
             where c.id = :id
            """)
    Optional<Consulta> buscarCompleta(@Param("id") Long id);

    /**
     * Segunda barreira contra agendamento duplicado. A primeira e a consulta de
     * disponibilidade; esta e verificada logo antes de gravar, para reduzir a
     * janela de corrida entre dois atendentes simultaneos.
     */
    @Query("""
            select count(c) > 0
              from Consulta c
             where c.dentista.id = :dentistaId
               and c.status      = br.senai.consultorio.domain.enums.StatusConsulta.AGENDADA
               and c.dataHora    < :fim
               and c.dataHoraFim > :inicio
            """)
    boolean existeConflito(@Param("dentistaId") Long dentistaId,
                           @Param("inicio") LocalDateTime inicio,
                           @Param("fim") LocalDateTime fim);

    boolean existsByPacienteId(Long pacienteId);

    boolean existsByDentistaId(Long dentistaId);

    /**
     * Mesma checagem de conflito, porem IGNORANDO uma consulta especifica.
     *
     * Necessaria ao remarcar: sem o "c.id <> :consultaId", a propria consulta
     * que esta sendo movida contaria como conflito consigo mesma, e trocar
     * apenas o dentista (mantendo o horario) seria sempre recusado.
     */
    @Query("""
            select count(c) > 0
              from Consulta c
             where c.dentista.id = :dentistaId
               and c.status      = br.senai.consultorio.domain.enums.StatusConsulta.AGENDADA
               and c.id         <> :consultaId
               and c.dataHora    < :fim
               and c.dataHoraFim > :inicio
            """)
    boolean existeConflitoExcluindo(@Param("dentistaId") Long dentistaId,
                                    @Param("inicio") LocalDateTime inicio,
                                    @Param("fim") LocalDateTime fim,
                                    @Param("consultaId") Long consultaId);
}
