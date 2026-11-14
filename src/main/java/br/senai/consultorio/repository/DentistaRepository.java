package br.senai.consultorio.repository;

import br.senai.consultorio.domain.enums.StatusConsulta;
import br.senai.consultorio.domain.model.Dentista;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface DentistaRepository extends JpaRepository<Dentista, Long> {

    Optional<Dentista> findByCro(String cro);

    boolean existsByCro(String cro);

    boolean existsByCroAndIdNot(String cro, Long id);

    @Query("""
            select d
              from Dentista d
             where (:termo is null
                    or lower(d.nome) like lower(concat('%', :termo, '%'))
                    or lower(d.cro)  like lower(concat('%', :termo, '%')))
               and (:ativo is null or d.ativo = :ativo)
            """)
    Page<Dentista> buscarPorTermo(@Param("termo") String termo,
                                  @Param("ativo") Boolean ativo,
                                  Pageable paginacao);

    /**
     * REGRA CENTRAL DO SISTEMA.
     *
     * Um dentista esta disponivel quando:
     *   1. esta ativo;
     *   2. cadastrou atendimento para aquele dia da semana;
     *   3. a janela pedida cabe INTEIRA dentro da faixa cadastrada;
     *   4. nao ha outra consulta AGENDADA que se sobreponha.
     *
     * A sobreposicao usa a regra classica de intervalos:
     *   (inicioA < fimB) e (fimA > inicioB)
     */
    @Query("""
            select distinct d
              from Dentista d
              join d.disponibilidades disp
             where d.ativo = true
               and disp.diaSemana  = :diaSemana
               and disp.horaInicio <= :horaInicio
               and disp.horaFim    >= :horaFim
               and not exists (
                     select 1
                       from Consulta c
                      where c.dentista = d
                        and c.status   = :status
                        and c.dataHora    < :fim
                        and c.dataHoraFim > :inicio)
             order by d.nome
            """)
    List<Dentista> buscarDisponiveis(@Param("diaSemana") DayOfWeek diaSemana,
                                     @Param("horaInicio") LocalTime horaInicio,
                                     @Param("horaFim") LocalTime horaFim,
                                     @Param("inicio") LocalDateTime inicio,
                                     @Param("fim") LocalDateTime fim,
                                     @Param("status") StatusConsulta status);

    /**
     * Sobrecarga de conveniencia: quem chama nao precisa saber que o filtro
     * considera apenas consultas AGENDADAS.
     */
    default List<Dentista> buscarDisponiveisEntre(LocalDateTime inicio, LocalDateTime fim) {
        return buscarDisponiveis(
                inicio.getDayOfWeek(),
                inicio.toLocalTime(),
                fim.toLocalTime(),
                inicio,
                fim,
                StatusConsulta.AGENDADA);
    }

    /**
     * O dentista tem faixa de atendimento cadastrada que cobre esta janela?
     *
     * Separada de buscarDisponiveis porque a remarcacao precisa checar a faixa
     * e o conflito de agenda em passos distintos: a consulta que esta sendo
     * movida nao pode contar como conflito consigo mesma.
     */
    @Query("""
            select count(disp) > 0
              from Disponibilidade disp
             where disp.dentista.id = :dentistaId
               and disp.diaSemana   = :diaSemana
               and disp.horaInicio <= :horaInicio
               and disp.horaFim    >= :horaFim
            """)
    boolean atendeNaFaixa(@Param("dentistaId") Long dentistaId,
                          @Param("diaSemana") DayOfWeek diaSemana,
                          @Param("horaInicio") LocalTime horaInicio,
                          @Param("horaFim") LocalTime horaFim);

    /** Carrega o dentista JUNTO com suas faixas de horario, em uma unica consulta. */
    @Query("""
            select d
              from Dentista d
              left join fetch d.disponibilidades
             where d.id = :id
            """)
    Optional<Dentista> buscarComDisponibilidades(@Param("id") Long id);
}
