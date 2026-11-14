package br.senai.consultorio.config;

/**
 * Dados da clinica que aparecem no cabecalho dos PDFs (comprovante e
 * receituario).
 *
 * Antes este record era preenchido a partir do application.yml. Agora que a
 * configuracao vive em codigo, ele e um simples portador de dados, montado
 * como @Bean em ConfiguracaoAplicacao.
 */
public record PropriedadesClinica(Clinica clinica, Documentos documentos) {

    /**
     * @param logo caminho do arquivo de imagem do logotipo (PNG/JPG). Pode ser
     *             nulo ou apontar para arquivo inexistente: nesse caso os PDFs
     *             simplesmente saem sem o logo, sem quebrar.
     */
    public record Clinica(String nome, String endereco, String telefone,
                          String cnpj, String logo) {
    }

    public record Documentos(String pasta) {
    }
}
