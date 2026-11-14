package br.senai.consultorio.domain.model;

import br.senai.consultorio.domain.exception.RegraDeNegocioException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "procedimento")
public class Procedimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * LGPD: registros nao sao apagados. Ao "excluir", o cadastro fica
     * inativo (ativo = false) -- some das listagens, mas continua no banco
     * para consulta e pode ser reativado.
     */
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean ativo = true;

    @Column(nullable = false, length = 120, unique = true)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "preco_mao_obra", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoMaoObra = BigDecimal.ZERO;

    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoMinutos = 30;

    /** Lista PADRAO de materiais, usada para sugerir o preco. */
    @OneToMany(mappedBy = "procedimento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProcedimentoProduto> materiais = new ArrayList<>();

    protected Procedimento() {
    }

    public Procedimento(String nome, BigDecimal precoMaoObra, Integer duracaoMinutos) {
        this.nome = nome;
        this.precoMaoObra = precoMaoObra;
        this.duracaoMinutos = duracaoMinutos;
    }

    /** Vincula um material. Se o produto ja estiver na lista, apenas ajusta a quantidade. */
    public void vincularMaterial(Produto produto, BigDecimal quantidade) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new RegraDeNegocioException("A quantidade deve ser maior que zero.");
        }
        materiais.stream()
                .filter(m -> m.getProduto().equals(produto))
                .findFirst()
                .ifPresentOrElse(
                        m -> m.setQuantidade(quantidade),
                        () -> {
                            ProcedimentoProduto novo = new ProcedimentoProduto(produto, quantidade);
                            materiais.add(novo);
                            novo.setProcedimento(this);
                        });
    }

    public void desvincularMaterial(Long vinculoId) {
        ProcedimentoProduto alvo = materiais.stream()
                .filter(m -> Objects.equals(m.getId(), vinculoId))
                .findFirst()
                .orElseThrow(() -> new RegraDeNegocioException(
                        "Este material nao esta vinculado ao procedimento."));
        materiais.remove(alvo);
        alvo.setProcedimento(null);
    }

    /** Mao de obra + soma dos materiais padrao. */
    public BigDecimal calcularPrecoSugerido() {
        BigDecimal totalMateriais = materiais.stream()
                .map(ProcedimentoProduto::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return precoMaoObra.add(totalMateriais).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getPrecoMaoObra() {
        return precoMaoObra;
    }

    public void setPrecoMaoObra(BigDecimal precoMaoObra) {
        this.precoMaoObra = precoMaoObra;
    }

    public Integer getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public void setDuracaoMinutos(Integer duracaoMinutos) {
        this.duracaoMinutos = duracaoMinutos;
    }

    public List<ProcedimentoProduto> getMateriais() {
        return Collections.unmodifiableList(materiais);
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    /** LGPD: "exclusao" na verdade inativa o registro. */
    public void inativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Procedimento procedimento)) {
            return false;
        }
        return id != null && id.equals(procedimento.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
