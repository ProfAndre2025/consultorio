package br.senai.consultorio.report;

import br.senai.consultorio.config.PropriedadesClinica;
import br.senai.consultorio.domain.exception.RegraDeNegocioException;
import br.senai.consultorio.dto.view.AtendimentoView;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Implementacao do comprovante em PDF usando OpenPDF.
 *
 * Toda a "sujeira" da biblioteca (tabelas, celulas, fontes) fica confinada
 * aqui. O resto do sistema conhece apenas a interface GeradorComprovante.
 */
@Component
public class GeradorComprovantePdf implements GeradorComprovante {

    private static final Logger LOG =
            LoggerFactory.getLogger(GeradorComprovantePdf.class);

    private static final Locale BRASIL = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'as' HH:mm", BRASIL);

    // Paleta em tons claros, coerente com o padrao "iOS" pedido no projeto.
    private static final Color AZUL = new Color(0x0A, 0x84, 0xFF);
    private static final Color CINZA_CLARO = new Color(0xF2, 0xF2, 0xF7);
    private static final Color CINZA_TEXTO = new Color(0x6B, 0x6B, 0x70);
    private static final Color PRETO = new Color(0x1C, 0x1C, 0x1E);

    private static final Font FONTE_TITULO = new Font(Font.HELVETICA, 18, Font.BOLD, PRETO);
    private static final Font FONTE_SUBTITULO = new Font(Font.HELVETICA, 11, Font.BOLD, AZUL);
    private static final Font FONTE_SECAO = new Font(Font.HELVETICA, 9, Font.BOLD, AZUL);
    private static final Font FONTE_ROTULO = new Font(Font.HELVETICA, 10, Font.NORMAL, CINZA_TEXTO);
    private static final Font FONTE_VALOR = new Font(Font.HELVETICA, 10, Font.BOLD, PRETO);
    private static final Font FONTE_TABELA_CAB = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font FONTE_TABELA = new Font(Font.HELVETICA, 9, Font.NORMAL, PRETO);
    private static final Font FONTE_TOTAL = new Font(Font.HELVETICA, 13, Font.BOLD, AZUL);
    private static final Font FONTE_RODAPE = new Font(Font.HELVETICA, 8, Font.NORMAL, CINZA_TEXTO);

    private final PropriedadesClinica propriedades;

    public GeradorComprovantePdf(PropriedadesClinica propriedades) {
        this.propriedades = propriedades;
    }

    @Override
    public byte[] gerar(AtendimentoView atendimento) {
        if (!atendimento.finalizado()) {
            throw new RegraDeNegocioException(
                    "O comprovante so pode ser emitido depois de finalizar o atendimento.");
        }

        Document documento = new Document(PageSize.A4, 42, 42, 46, 46);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(documento, saida);
            documento.open();

            documento.add(cabecalho());
            documento.add(secaoProfissional(atendimento));
            documento.add(secaoPaciente(atendimento));
            documento.add(secaoProcedimento(atendimento));
            documento.add(tabelaMateriais(atendimento));
            documento.add(blocoTotais(atendimento));
            documento.add(observacoes(atendimento));
            documento.add(assinaturas(atendimento));
            documento.add(rodape());

            documento.close();
            return saida.toByteArray();

        } catch (Exception excecao) {
            // Empacota como erro de negocio para o ControleGlobalDeErros tratar.
            throw new RegraDeNegocioException(
                    "Nao foi possivel gerar o comprovante em PDF: " + excecao.getMessage());
        }
    }

    @Override
    public String nomeArquivo(AtendimentoView atendimento) {
        String paciente = removerAcentos(atendimento.pacienteNome())
                .replaceAll("[^A-Za-z0-9]+", "_");
        return "comprovante_%04d_%s.pdf".formatted(atendimento.id(), paciente);
    }

    @Override
    public String tipoConteudo() {
        return "application/pdf";
    }

    // ---------------------------------------------------------------- secoes

    private Paragraph cabecalho() {
        PropriedadesClinica.Clinica clinica = propriedades.clinica();

        PdfPTable tabela = tabelaSemBorda(new float[]{7, 3});

        PdfPCell esquerda = celulaSemBorda();
        adicionarLogoSeExistir(esquerda, clinica.logo());
        esquerda.addElement(new Paragraph(clinica.nome(), FONTE_TITULO));
        esquerda.addElement(new Paragraph(clinica.endereco(), FONTE_RODAPE));
        esquerda.addElement(new Paragraph(
                "Tel: %s   -   CNPJ: %s".formatted(clinica.telefone(), clinica.cnpj()),
                FONTE_RODAPE));
        tabela.addCell(esquerda);

        PdfPCell direita = celulaSemBorda();
        direita.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph titulo = new Paragraph("COMPROVANTE DE\nPROCEDIMENTO", FONTE_SUBTITULO);
        titulo.setAlignment(Element.ALIGN_RIGHT);
        direita.addElement(titulo);
        tabela.addCell(direita);

        Paragraph wrapper = new Paragraph();
        wrapper.add(tabela);
        wrapper.setSpacingAfter(14);
        return wrapper;
    }

    private Paragraph secaoProfissional(AtendimentoView atendimento) {
        PdfPTable cartao = cartao();
        cartao.addCell(celulaSecao("PROFISSIONAL RESPONSAVEL"));
        cartao.addCell(linhaRotulo("Dentista", atendimento.dentistaNome()));
        cartao.addCell(linhaRotulo("CRO", atendimento.dentistaCro()));
        // Especialidade e opcional no cadastro; so aparece quando preenchida.
        if (atendimento.dentistaEspecialidade() != null
                && !atendimento.dentistaEspecialidade().isBlank()) {
            cartao.addCell(linhaRotulo("Especialidade", atendimento.dentistaEspecialidade()));
        }
        return envolver(cartao, 10);
    }

    private Paragraph secaoPaciente(AtendimentoView atendimento) {
        PdfPTable cartao = cartao();
        cartao.addCell(celulaSecao("PACIENTE"));
        cartao.addCell(linhaRotulo("Nome", atendimento.pacienteNome()));
        cartao.addCell(linhaRotulo("CPF", atendimento.pacienteCpf()));
        return envolver(cartao, 10);
    }

    private Paragraph secaoProcedimento(AtendimentoView atendimento) {
        PdfPTable cartao = cartao();
        cartao.addCell(celulaSecao("PROCEDIMENTO"));
        cartao.addCell(linhaRotulo("Procedimento", atendimento.procedimentoNome()));
        cartao.addCell(linhaRotulo("Data", atendimento.realizadoEm().format(DATA_HORA)));
        return envolver(cartao, 12);
    }

    /**
     * Discriminacao do comprovante.
     *
     * Mostra SOMENTE o procedimento realizado e o valor total. Os materiais,
     * suas quantidades e precos ficam de fora de proposito: sao a estrutura de
     * custo da clinica e nao devem circular no documento entregue ao paciente.
     * O detalhamento completo continua disponivel na tela do atendimento.
     */
    private Paragraph tabelaMateriais(AtendimentoView atendimento) {
        Paragraph titulo = new Paragraph("Discriminacao", FONTE_VALOR);
        titulo.setSpacingAfter(6);

        PdfPTable tabela = new PdfPTable(new float[]{7, 3});
        tabela.setWidthPercentage(100);
        tabela.setHeaderRows(1);

        PdfPCell cabItem = new PdfPCell(new Phrase("PROCEDIMENTO", FONTE_TABELA_CAB));
        cabItem.setBackgroundColor(AZUL);
        cabItem.setPadding(6);
        cabItem.setBorder(PdfPCell.NO_BORDER);
        tabela.addCell(cabItem);

        PdfPCell cabValor = new PdfPCell(new Phrase("VALOR", FONTE_TABELA_CAB));
        cabValor.setBackgroundColor(AZUL);
        cabValor.setPadding(6);
        cabValor.setBorder(PdfPCell.NO_BORDER);
        cabValor.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabela.addCell(cabValor);

        tabela.addCell(celulaTabela(
                atendimento.procedimentoNome(), Color.WHITE, Element.ALIGN_LEFT));
        tabela.addCell(celulaTabela(
                reais(atendimento.valorTotal()), Color.WHITE, Element.ALIGN_RIGHT));

        Paragraph wrapper = new Paragraph();
        wrapper.add(titulo);
        wrapper.add(tabela);
        wrapper.setSpacingAfter(10);
        return wrapper;
    }

    /** Apenas o total. Honorarios e materiais nao sao abertos ao paciente. */
    private Paragraph blocoTotais(AtendimentoView atendimento) {
        PdfPTable tabela = tabelaSemBorda(new float[]{6, 4});

        tabela.addCell(celulaSemBorda());
        tabela.addCell(linhaTotal("TOTAL:", reais(atendimento.valorTotal()), true));

        return envolver(tabela, 16);
    }

    private Paragraph observacoes(AtendimentoView atendimento) {
        Paragraph bloco = new Paragraph();
        bloco.add(new Paragraph("Observacoes:", FONTE_VALOR));
        String texto = atendimento.observacoes();
        bloco.add(new Paragraph(
                (texto == null || texto.isBlank()) ? "-" : texto, FONTE_TABELA));
        bloco.setSpacingAfter(34);
        return bloco;
    }

    private Paragraph assinaturas(AtendimentoView atendimento) {
        PdfPTable tabela = tabelaSemBorda(new float[]{1, 1});

        tabela.addCell(assinatura(
                atendimento.dentistaNome(),
                "Assinatura do dentista - " + atendimento.dentistaCro()));
        tabela.addCell(assinatura(
                atendimento.pacienteNome(),
                "Assinatura do paciente"));

        return envolver(tabela, 0);
    }

    private Paragraph rodape() {
        Paragraph rodape = new Paragraph(
                "Documento emitido pelo sistema em "
                        + java.time.LocalDateTime.now().format(DATA_HORA),
                FONTE_RODAPE);
        rodape.setAlignment(Element.ALIGN_CENTER);
        rodape.setSpacingBefore(28);
        return rodape;
    }

    // ---------------------------------------------------------------- apoio

    private PdfPTable cartao() {
        PdfPTable tabela = new PdfPTable(1);
        tabela.setWidthPercentage(100);
        return tabela;
    }

    private PdfPCell celulaSecao(String titulo) {
        PdfPCell celula = new PdfPCell(new Phrase(titulo, FONTE_SECAO));
        celula.setBackgroundColor(CINZA_CLARO);
        celula.setBorder(PdfPCell.NO_BORDER);
        celula.setPaddingTop(8);
        celula.setPaddingLeft(10);
        celula.setPaddingRight(10);
        celula.setPaddingBottom(4);
        return celula;
    }

    private PdfPCell linhaRotulo(String rotulo, String valor) {
        PdfPTable interna = tabelaSemBorda(new float[]{3, 7});
        PdfPCell r = new PdfPCell(new Phrase(rotulo, FONTE_ROTULO));
        r.setBorder(PdfPCell.NO_BORDER);
        r.setPaddingLeft(10);
        r.setPaddingBottom(4);
        PdfPCell v = new PdfPCell(new Phrase(valor != null ? valor : "-", FONTE_VALOR));
        v.setBorder(PdfPCell.NO_BORDER);
        v.setPaddingBottom(4);
        interna.addCell(r);
        interna.addCell(v);

        PdfPCell externa = new PdfPCell(interna);
        externa.setBackgroundColor(CINZA_CLARO);
        externa.setBorder(PdfPCell.NO_BORDER);
        return externa;
    }

    private PdfPCell celulaTabela(String texto, Color fundo, int alinhamento) {
        PdfPCell celula = new PdfPCell(new Phrase(texto, FONTE_TABELA));
        celula.setBackgroundColor(fundo);
        celula.setBorder(PdfPCell.NO_BORDER);
        celula.setPadding(6);
        celula.setHorizontalAlignment(alinhamento);
        return celula;
    }

    private PdfPCell linhaTotal(String rotulo, String valor, boolean destaque) {
        // 3:2 -> mais espaco para o rotulo, mantendo o valor junto a margem direita.
        PdfPTable interna = tabelaSemBorda(new float[]{3, 2});
        Font fonteRotulo = destaque ? FONTE_TOTAL : FONTE_ROTULO;
        Font fonteValor = destaque ? FONTE_TOTAL : FONTE_VALOR;

        PdfPCell r = new PdfPCell(new Phrase(rotulo, fonteRotulo));
        r.setBorder(PdfPCell.NO_BORDER);
        r.setHorizontalAlignment(Element.ALIGN_RIGHT);
        r.setPadding(3);
        PdfPCell v = new PdfPCell(new Phrase(valor, fonteValor));
        v.setBorder(PdfPCell.NO_BORDER);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setPadding(3);
        interna.addCell(r);
        interna.addCell(v);

        PdfPCell externa = new PdfPCell(interna);
        externa.setBorder(PdfPCell.NO_BORDER);
        return externa;
    }

    private PdfPCell assinatura(String nome, String legenda) {
        PdfPCell celula = celulaSemBorda();
        celula.setPaddingLeft(14);
        celula.setPaddingRight(14);

        Paragraph linha = new Paragraph("_____________________________", FONTE_ROTULO);
        linha.setAlignment(Element.ALIGN_CENTER);
        Paragraph nomeP = new Paragraph(nome, FONTE_VALOR);
        nomeP.setAlignment(Element.ALIGN_CENTER);
        Paragraph legendaP = new Paragraph(legenda, FONTE_RODAPE);
        legendaP.setAlignment(Element.ALIGN_CENTER);

        celula.addElement(linha);
        celula.addElement(nomeP);
        celula.addElement(legendaP);
        return celula;
    }

    private PdfPTable tabelaSemBorda(float[] larguras) {
        PdfPTable tabela = new PdfPTable(larguras);
        tabela.setWidthPercentage(100);
        return tabela;
    }

    private PdfPCell celulaSemBorda() {
        PdfPCell celula = new PdfPCell();
        celula.setBorder(PdfPCell.NO_BORDER);
        return celula;
    }

    private Paragraph envolver(PdfPTable tabela, float espacoDepois) {
        Paragraph wrapper = new Paragraph();
        wrapper.add(tabela);
        wrapper.setSpacingAfter(espacoDepois);
        return wrapper;
    }

    private String reais(BigDecimal valor) {
        return "R$ " + numero(valor);
    }

    private String numero(BigDecimal valor) {
        return String.format(BRASIL, "%,.2f", valor);
    }

    private String removerAcentos(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    /**
     * Coloca o logo no topo da celula, SE o arquivo existir e for lido sem erro.
     *
     * O logo e um "enfeite" do documento: nunca deve derrubar a emissao do PDF.
     * Por isso qualquer falha (caminho nulo, arquivo ausente, imagem invalida)
     * apenas registra um aviso e segue sem o logo.
     */
    private void adicionarLogoSeExistir(PdfPCell celula, String caminhoLogo) {
        if (caminhoLogo == null || caminhoLogo.isBlank()) {
            return;
        }
        File arquivo = new File(caminhoLogo);
        if (!arquivo.isFile()) {
            return;
        }
        try {
            Image logo = Image.getInstance(caminhoLogo);
            logo.scaleToFit(140, 60);        // cabe no cabecalho sem dominar
            Paragraph linha = new Paragraph();
            linha.add(new com.lowagie.text.Chunk(logo, 0, 0, true));
            linha.setSpacingAfter(6);
            celula.addElement(linha);
        } catch (Exception excecao) {
            LOG.warn("Nao foi possivel carregar o logo em {}: {}",
                    caminhoLogo, excecao.getMessage());
        }
    }

}
