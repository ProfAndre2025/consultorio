package br.senai.consultorio.web.controller;

import br.senai.consultorio.dto.view.AtendimentoView;
import br.senai.consultorio.report.GeradorComprovante;
import br.senai.consultorio.service.AtendimentoService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Download do comprovante em PDF.
 *
 * Repare que o controller depende da INTERFACE GeradorComprovante, nao da
 * implementacao. Se a clinica um dia quiser gerar o comprovante em outro
 * formato, cria-se outra implementacao e nada aqui muda.
 */
@Controller
public class ComprovanteController {

    private final AtendimentoService atendimentoService;
    private final GeradorComprovante geradorComprovante;

    public ComprovanteController(AtendimentoService atendimentoService,
                                 GeradorComprovante geradorComprovante) {
        this.atendimentoService = atendimentoService;
        this.geradorComprovante = geradorComprovante;
    }

    @GetMapping("/atendimentos/{id}/comprovante")
    public ResponseEntity<Resource> baixar(@PathVariable Long id) {
        AtendimentoView atendimento = atendimentoService.buscar(id);

        byte[] pdf = geradorComprovante.gerar(atendimento);
        String nome = geradorComprovante.nomeArquivo(atendimento);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + nome + "\"")
                .contentType(MediaType.parseMediaType(geradorComprovante.tipoConteudo()))
                .body(new ByteArrayResource(pdf));
    }
}
