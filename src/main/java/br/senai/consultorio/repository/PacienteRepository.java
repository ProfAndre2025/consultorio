package br.senai.consultorio.repository;

import br.senai.consultorio.domain.model.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repositorio = a "ponte" com o banco.
 *
 * Nao existe nenhuma classe implementando esta interface: o Spring Data gera a
 * implementacao em tempo de execucao. O que voce escreve e apenas o CONTRATO --
 * e e isso que permite trocar a persistencia sem tocar no service.
 */
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    /** Todos os ATIVOS, ordenados -- para selects de formularios. */
    java.util.List<Paciente> findByAtivoTrue(org.springframework.data.domain.Sort ordenacao);

    Optional<Paciente> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    /** Usado na edicao: o CPF pode se repetir, desde que seja do proprio registro. */
    boolean existsByCpfAndIdNot(String cpf, Long id);

    @Query("""
            select p
              from Paciente p
             where (:termo is null
                 or lower(p.nome) like lower(concat('%', :termo, '%'))
                 or p.cpf like concat('%', :termo, '%'))
               and (:ativo is null or p.ativo = :ativo)
            """)
    Page<Paciente> buscarPorTermo(@Param("termo") String termo,
                                  @Param("ativo") Boolean ativo,
                                  Pageable paginacao);
}
