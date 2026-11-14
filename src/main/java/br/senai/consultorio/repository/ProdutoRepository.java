package br.senai.consultorio.repository;

import br.senai.consultorio.domain.model.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    /** Todos os ATIVOS, ordenados -- para selects de formularios. */
    java.util.List<Produto> findByAtivoTrue(org.springframework.data.domain.Sort ordenacao);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    @Query("""
            select p
              from Produto p
             where (:termo is null
                 or lower(p.nome) like lower(concat('%', :termo, '%')))
               and (:ativo is null or p.ativo = :ativo)
            """)
    Page<Produto> buscarPorTermo(@Param("termo") String termo,
                                 @Param("ativo") Boolean ativo,
                                 Pageable paginacao);

    /** Impede excluir um material que ja participou de algum atendimento. */
    @Query("""
            select count(am) > 0
              from AtendimentoMaterial am
             where am.produto.id = :produtoId
            """)
    boolean foiUtilizadoEmAtendimento(@Param("produtoId") Long produtoId);

    /** Impede excluir um material que ainda consta na lista padrao de algum procedimento. */
    @Query("""
            select count(pp) > 0
              from ProcedimentoProduto pp
             where pp.produto.id = :produtoId
            """)
    boolean estaVinculadoAProcedimento(@Param("produtoId") Long produtoId);
}
