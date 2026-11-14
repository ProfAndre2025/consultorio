package br.senai.consultorio.report;

import br.senai.consultorio.config.PropriedadesClinica;
import br.senai.consultorio.domain.exception.RegraDeNegocioException;
import br.senai.consultorio.dto.view.AtendimentoView;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Receituario em PDF com OpenPDF.
 *
 * Reaproveita o mesmo vocabulario visual do comprovante (cabecalho com logo,
 * cartoes de dados, cores) para que os dois documentos da clinica tenham a
 * mesma identidade. A diferenca central e o corpo: um bloco de prescricao com
 * o texto livre que o dentista escreveu, seguido do espaco de assinatura.
 */
@Component
public class GeradorReceituarioPdf implements GeradorReceituario {

    private static final Logger LOG =
            LoggerFactory.getLogger(GeradorReceituarioPdf.class);

    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'as' HH:mm");

    private static final Color AZUL = new Color(0x0A, 0x84, 0xFF);
    private static final Color CINZA_CLARO = new Color(0xF2, 0xF2, 0xF7);
    private static final Color CINZA_TEXTO = new Color(0x6B, 0x6B, 0x70);
    private static final Color PRETO = new Color(0x1C, 0x1C, 0x1E);

    private static final Font FONTE_TITULO = new Font(Font.HELVETICA, 18, Font.BOLD, PRETO);
    private static final Font FONTE_SUBTITULO = new Font(Font.HELVETICA, 11, Font.BOLD, AZUL);
    private static final Font FONTE_SECAO = new Font(Font.HELVETICA, 9, Font.BOLD, AZUL);
    private static final Font FONTE_ROTULO = new Font(Font.HELVETICA, 10, Font.NORMAL, CINZA_TEXTO);
    private static final Font FONTE_VALOR = new Font(Font.HELVETICA, 10, Font.BOLD, PRETO);
    private static final Font FONTE_CORPO = new Font(Font.HELVETICA, 11, Font.NORMAL, PRETO);
    private static final Font FONTE_RODAPE = new Font(Font.HELVETICA, 8, Font.NORMAL, CINZA_TEXTO);
    private static final Font FONTE_ASSINATURA = new Font(Font.HELVETICA, 10, Font.BOLD, PRETO);

    private final PropriedadesClinica propriedades;

    public GeradorReceituarioPdf(PropriedadesClinica propriedades) {
        this.propriedades = propriedades;
    }

    @Override
    public byte[] gerar(AtendimentoView atendimento, String prescricao) {
        if (prescricao == null || prescricao.isBlank()) {
            throw new RegraDeNegocioException("A prescricao do receituario nao pode ficar vazia.");
        }

        Document documento = new Document(PageSize.A4, 42, 42, 46, 46);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(documento, saida);
            documento.open();

            documento.add(cabecalho());
            documento.add(secaoProfissional(atendimento));
            documento.add(secaoPaciente(atendimento));
            documento.add(blocoPrescricao(prescricao));
            documento.add(assinatura(atendimento));
        } catch (Exception excecao) {
            throw new RegraDeNegocioException(
                    "Nao foi possivel gerar o receituario: " + excecao.getMessage());
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return saida.toByteArray();
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
        Paragraph titulo = new Paragraph("RECEITUARIO", FONTE_SUBTITULO);
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
        cartao.addCell(linhaRotulo("Data", LocalDateTime.now().format(DATA_HORA)));
        return envolver(cartao, 14);
    }

    /**
     * Corpo do receituario: o texto livre da prescricao, dentro de uma moldura
     * alta que lembra o papel de receita. Cada linha digitada pelo dentista
     * vira um paragrafo, preservando as quebras.
     */
    private Paragraph blocoPrescricao(String prescricao) {
        Paragraph titulo = new Paragraph("PRESCRICAO", FONTE_SECAO);
        titulo.setSpacingAfter(8);

        PdfPTable moldura = new PdfPTable(1);
        moldura.setWidthPercentage(100);

        PdfPCell celula = new PdfPCell();
        celula.setBorderColor(CINZA_CLARO);
        celula.setBorderWidth(1);
        celula.setPadding(16);
        celula.setMinimumHeight(320);            // area generosa, cara de receita

        for (String linha : prescricao.strip().split("\\r?\\n")) {
            Paragraph paragrafo = new Paragraph(
                    linha.isBlank() ? " " : linha, FONTE_CORPO);
            paragrafo.setSpacingAfter(4);
            celula.addElement(paragrafo);
        }
        moldura.addCell(celula);

        Paragraph wrapper = new Paragraph();
        wrapper.add(titulo);
        wrapper.add(moldura);
        wrapper.setSpacingAfter(30);
        return wrapper;
    }

    private Paragraph assinatura(AtendimentoView atendimento) {
        PdfPTable tabela = tabelaSemBorda(new float[]{1});
        tabela.setWidthPercentage(60);
        tabela.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell celula = celulaSemBorda();
        celula.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph linha = new Paragraph("_______________________________", FONTE_ROTULO);
        linha.setAlignment(Element.ALIGN_CENTER);
        celula.addElement(linha);

        Paragraph nome = new Paragraph(atendimento.dentistaNome(), FONTE_ASSINATURA);
        nome.setAlignment(Element.ALIGN_CENTER);
        celula.addElement(nome);

        Paragraph cro = new Paragraph(
                "Assinatura do dentista - " + atendimento.dentistaCro(), FONTE_RODAPE);
        cro.setAlignment(Element.ALIGN_CENTER);
        celula.addElement(cro);

        tabela.addCell(celula);

        Paragraph wrapper = new Paragraph();
        wrapper.add(tabela);
        return wrapper;
    }

    // -------------------------------------------------------------- auxiliares

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
            logo.scaleToFit(140, 60);
            Paragraph linha = new Paragraph();
            linha.add(new Chunk(logo, 0, 0, true));
            linha.setSpacingAfter(6);
            celula.addElement(linha);
        } catch (Exception excecao) {
            LOG.warn("Nao foi possivel carregar o logo em {}: {}",
                    caminhoLogo, excecao.getMessage());
        }
    }

    private PdfPTable cartao() {
        PdfPTable cartao = new PdfPTable(1);
        cartao.setWidthPercentage(100);
        return cartao;
    }

    private PdfPCell celulaSecao(String texto) {
        PdfPCell celula = new PdfPCell(new Paragraph(texto, FONTE_SECAO));
        celula.setBackgroundColor(CINZA_CLARO);
        celula.setBorder(PdfPCell.NO_BORDER);
        celula.setPadding(8);
        celula.setPaddingLeft(10);
        return celula;
    }

    private PdfPCell linhaRotulo(String rotulo, String valor) {
        PdfPTable interna = tabelaSemBorda(new float[]{3, 7});

        PdfPCell celulaRotulo = new PdfPCell(new Paragraph(rotulo, FONTE_ROTULO));
        celulaRotulo.setBorder(PdfPCell.NO_BORDER);
        celulaRotulo.setPadding(3);
        celulaRotulo.setPaddingLeft(10);
        interna.addCell(celulaRotulo);

        PdfPCell celulaValor = new PdfPCell(new Paragraph(valor, FONTE_VALOR));
        celulaValor.setBorder(PdfPCell.NO_BORDER);
        celulaValor.setPadding(3);
        interna.addCell(celulaValor);

        PdfPCell envolvente = new PdfPCell(interna);
        envolvente.setBackgroundColor(CINZA_CLARO);
        envolvente.setBorder(PdfPCell.NO_BORDER);
        envolvente.setPadding(0);
        return envolvente;
    }

    private Paragraph envolver(PdfPTable tabela, float espacoDepois) {
        Paragraph wrapper = new Paragraph();
        wrapper.add(tabela);
        wrapper.setSpacingAfter(espacoDepois);
        return wrapper;
    }

    private PdfPTable tabelaSemBorda(float[] larguras) {
        PdfPTable tabela = new PdfPTable(larguras);
        tabela.setWidthPercentage(100);
        return tabela;
    }

    private PdfPCell celulaSemBorda() {
        PdfPCell celula = new PdfPCell();
        celula.setBorder(PdfPCell.NO_BORDER);
        celula.setPadding(0);
        return celula;
    }

    // ------------------------------------------------------------ contrato

    @Override
    public String nomeArquivo(AtendimentoView atendimento) {
        String paciente = Normalizer.normalize(atendimento.pacienteNome(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9]+", "_");
        return "receituario_%04d_%s.pdf".formatted(atendimento.id(), paciente);
    }

    @Override
    public String tipoConteudo() {
        return "application/pdf";
    }
}
