package br.senai.consultorio.report;

import br.senai.consultorio.dto.view.AtendimentoView;

/**
 * PORTA (contrato) para emissao do receituario em PDF.
 *
 * Segue exatamente a mesma ideia do {@link GeradorComprovante}: o controller
 * pede "gere o receituario deste atendimento com este texto de prescricao" e
 * nao sabe qual tecnologia produz o documento. A implementacao atual (OpenPDF)
 * fica em GeradorReceituarioPdf.
 *
 * Diferente do comprovante, o receituario carrega um texto livre -- a
 * prescricao que o dentista escreve na hora. Por isso o metodo recebe esse
 * texto alem do AtendimentoView (dados de clinica, dentista e paciente).
 */
public interface GeradorReceituario {

    /** Produz o receituario e devolve os bytes prontos para download. */
    byte[] gerar(AtendimentoView atendimento, String prescricao);

    /** Nome sugerido do arquivo, ex.: "receituario_0007_Marina_Souza.pdf". */
    String nomeArquivo(AtendimentoView atendimento);

    /** Tipo de midia para o cabecalho HTTP (ex.: application/pdf). */
    String tipoConteudo();
}
