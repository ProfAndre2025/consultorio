package br.senai.consultorio.dto.view;

import java.time.LocalDate;

/**
 * Objeto de SAIDA. E um record: imutavel e sem setters, porque a tela apenas
 * exibe. Entidade JPA nunca vai para o template -- se fosse, um acesso a uma
 * associacao LAZY dentro do HTML estouraria LazyInitializationException.
 */
public record PacienteView(
        Long id,
        String nome,
        String cpf,
        LocalDate dataNascimento,
        String telefone,
        String email,
        String observacoes,
        boolean ativo) {
}
