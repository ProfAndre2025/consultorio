package br.senai.consultorio.domain.model;

import br.senai.consultorio.domain.exception.RegraDeNegocioException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Procedimento efetivamente realizado.
 *
 * Ponto importante de modelagem: preco_mao_obra e uma COPIA do valor vigente
 * no dia. No sistema anterior o total era recalculado a partir da tabela
 * procedimento, entao um reajuste de preco alterava retroativamente
 * atendimentos ja fechados.
 */
@Entity
@Table(name = "atendimento")
public class Atendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consulta_id", nullable = false, unique = true)
    private Consulta consulta;

    @Column(name = "realizado_em", nullable = false)
    private LocalDateTime realizadoEm;

    @Column(name = "preco_mao_obra", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoMaoObra = BigDecimal.ZERO;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean finalizado = false;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Version
    @Column(nullable = false)
    private Long versao;

    @OneToMany(mappedBy = "atendimento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AtendimentoMaterial> materiais = new ArrayList<>();

    protected Atendimento() {
    }

    public Atendimento(Consulta consulta) {
        consulta.garantirQueNaoEstaCancelada();
        this.consulta = consulta;
        this.realizadoEm = LocalDateTime.now();
        this.precoMaoObra = consulta.getProcedimento().getPrecoMaoObra();
        this.valorTotal = this.precoMaoObra;
    }

    /**
     * Registra o uso de um material. Se o mesmo produto ja tiver sido
     * lancado, SOMA a quantidade em vez de criar uma segunda linha.
     */
    public void registrarUso(Produto produto, BigDecimal quantidade) {
        garantirQueEstaAberto();
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new RegraDeNegocioException("A quantidade deve ser maior que zero.");
        }
        materiais.stream()
                .filter(m -> m.getProduto().equals(produto))
                .findFirst()
                .ifPresentOrElse(
                        m -> m.somarQuantidade(quantidade),
                        () -> {
                            boolean extra = !produtoEstaPrevisto(produto);
                            AtendimentoMaterial novo =
                                    new AtendimentoMaterial(produto, quantidade, extra);
                            materiais.add(novo);
                            novo.setAtendimento(this);
                        });
        recalcularTotal();
    }

    public void removerMaterial(Long materialId) {
        garantirQueEstaAberto();
        AtendimentoMaterial alvo = materiais.stream()
                .filter(m -> Objects.equals(m.getId(), materialId))
                .findFirst()
                .orElseThrow(() -> new RegraDeNegocioException(
                        "Este material nao pertence ao atendimento."));
        materiais.remove(alvo);
        alvo.setAtendimento(null);
        recalcularTotal();
    }

    public void recalcularTotal() {
        BigDecimal totalMateriais = materiais.stream()
                .map(AtendimentoMaterial::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.valorTotal = precoMaoObra.add(totalMateriais).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularTotalMateriais() {
        return materiais.stream()
                .map(AtendimentoMaterial::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** So depois de finalizado o estoque e baixado e o comprovante pode ser emitido. */
    public void finalizar(String observacoesClinicas) {
        garantirQueEstaAberto();
        this.observacoes = observacoesClinicas;
        this.realizadoEm = LocalDateTime.now();
        this.finalizado = true;
        recalcularTotal();
        consulta.marcarComoRealizada();
    }

    public void garantirQueEstaAberto() {
        if (finalizado) {
            throw new RegraDeNegocioException(
                    "Este atendimento ja foi finalizado e nao pode mais ser alterado.");
        }
    }

    public void garantirQueEstaFinalizado() {
        if (!finalizado) {
            throw new RegraDeNegocioException(
                    "O comprovante so pode ser emitido depois de finalizar o atendimento.");
        }
    }

    /** Um produto e "extra" quando nao constava na lista padrao do procedimento. */
    private boolean produtoEstaPrevisto(Produto produto) {
        return consulta.getProcedimento().getMateriais().stream()
                .anyMatch(padrao -> padrao.getProduto().equals(produto));
    }

    public Long getId() {
        return id;
    }

    public Consulta getConsulta() {
        return consulta;
    }

    public LocalDateTime getRealizadoEm() {
        return realizadoEm;
    }

    public BigDecimal getPrecoMaoObra() {
        return precoMaoObra;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public boolean isFinalizado() {
        return finalizado;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public List<AtendimentoMaterial> getMateriais() {
        return Collections.unmodifiableList(materiais);
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Atendimento atendimento)) {
            return false;
        }
        return id != null && id.equals(atendimento.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
