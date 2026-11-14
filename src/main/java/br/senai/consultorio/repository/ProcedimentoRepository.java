package br.senai.consultorio.repository;

import br.senai.consultorio.domain.model.Procedimento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProcedimentoRepository extends JpaRepository<Procedimento, Long> {

    /** Todos os ATIVOS, ordenados -- para selects de formularios. */
    java.util.List<Procedimento> findByAtivoTrue(org.springframework.data.domain.Sort ordenacao);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    @Query("""
            select p
              from Procedimento p
             where (:termo is null
                 or lower(p.nome) like lower(concat('%', :termo, '%')))
               and (:ativo is null or p.ativo = :ativo)
            """)
    Page<Procedimento> buscarPorTermo(@Param("termo") String termo,
                                      @Param("ativo") Boolean ativo,
                                      Pageable paginacao);

    /**
     * Carrega o procedimento com os materiais e os produtos em UMA consulta.
     * Sem este "join fetch" o calculo do preco sugerido dispararia uma consulta
     * por material -- o classico problema N+1.
     */
    @Query("""
            select distinct p
              from Procedimento p
              left join fetch p.materiais m
              left join fetch m.produto
             where p.id = :id
            """)
    Optional<Procedimento> buscarComMateriais(@Param("id") Long id);

    @Query("""
            select count(c) > 0
              from Consulta c
             where c.procedimento.id = :procedimentoId
            """)
    boolean possuiConsultas(@Param("procedimentoId") Long procedimentoId);
}
