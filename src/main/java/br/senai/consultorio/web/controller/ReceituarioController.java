package br.senai.consultorio.web.controller;

import br.senai.consultorio.dto.view.AtendimentoView;
import br.senai.consultorio.report.GeradorReceituario;
import br.senai.consultorio.service.AtendimentoService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Geracao do receituario em PDF.
 *
 * Igual ao ComprovanteController, depende da INTERFACE GeradorReceituario, e
 * nao da implementacao. A prescricao vem por POST (texto que o dentista
 * escreveu na tela), por isso aqui e POST e nao GET como o comprovante.
 */
@Controller
public class ReceituarioController {

    private final AtendimentoService atendimentoService;
    private final GeradorReceituario geradorReceituario;

    public ReceituarioController(AtendimentoService atendimentoService,
                                 GeradorReceituario geradorReceituario) {
        this.atendimentoService = atendimentoService;
        this.geradorReceituario = geradorReceituario;
    }

    @PostMapping("/atendimentos/{id}/receituario")
    public ResponseEntity<Resource> baixar(@PathVariable Long id,
                                           @RequestParam(required = false) String prescricao) {
        AtendimentoView atendimento = atendimentoService.buscar(id);

        byte[] pdf = geradorReceituario.gerar(atendimento, prescricao);
        String nome = geradorReceituario.nomeArquivo(atendimento);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + nome + "\"")
                .contentType(MediaType.parseMediaType(geradorReceituario.tipoConteudo()))
                .body(new ByteArrayResource(pdf));
    }
}
