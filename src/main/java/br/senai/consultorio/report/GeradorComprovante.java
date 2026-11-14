package br.senai.consultorio.report;

import br.senai.consultorio.dto.view.AtendimentoView;

/**
 * PORTA (contrato) para emissao do comprovante do atendimento.
 *
 * Este e o desacoplamento que realmente importa no projeto: o controller pede
 * "gere o comprovante deste atendimento" sem saber se sai um PDF, um HTML ou
 * outro formato. Trocar a tecnologia de geracao nao toca em mais nenhuma linha
 * fora da implementacao.
 *
 * Recebe um AtendimentoView (record ja montado pela camada de servico), e nao
 * a entidade JPA. Assim o gerador nao depende de Hibernate e nao corre o risco
 * de acessar uma associacao LAZY fora da transacao.
 *
 * A implementacao atual (OpenPDF) fica em GeradorComprovantePdf.
 */
public interface GeradorComprovante {

    /** Produz o comprovante e devolve os bytes prontos para download. */
    byte[] gerar(AtendimentoView atendimento);

    /** Nome sugerido do arquivo, ex.: "comprovante_0007_Marina_Souza.pdf". */
    String nomeArquivo(AtendimentoView atendimento);

    /** Tipo de midia para o cabecalho HTTP (ex.: application/pdf). */
    String tipoConteudo();
}
