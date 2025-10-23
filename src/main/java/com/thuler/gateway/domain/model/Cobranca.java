package com.thuler.gateway.domain.model;

import com.thuler.gateway.domain.enums.CobrancaStatus;
import com.thuler.gateway.domain.enums.TipoPagamento;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cobrancas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Cobranca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "originador_id", nullable = false)
    private Usuario originador;

    @ManyToOne
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Usuario destinatario;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CobrancaStatus status = CobrancaStatus.PENDENTE;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TipoPagamento tipoPagamento;

    @Column(length = 4)
    private String numeroCartao;

    @Column(columnDefinition = "TEXT")
    private String autorizadorResponse;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime paidAt;

    private LocalDateTime cancelledAt;

    public void marcarComoPaga(TipoPagamento tipoPagamento, String numeroCartao, String autorizadorResponse) {
        if (this.status != CobrancaStatus.PENDENTE) {
            throw new IllegalStateException("Apenas cobranças pendentes podem ser pagas");
        }
        this.status = CobrancaStatus.PAGA;
        this.tipoPagamento = tipoPagamento;
        this.numeroCartao = numeroCartao;
        this.autorizadorResponse = autorizadorResponse;
        this.paidAt = LocalDateTime.now();
    }

    public void cancelar(String autorizadorResponse) {
        if (this.status == CobrancaStatus.CANCELADA) {
            throw new IllegalStateException("Cobrança já está cancelada");
        }
        this.status = CobrancaStatus.CANCELADA;
        if (autorizadorResponse != null) {
            this.autorizadorResponse = autorizadorResponse;
        }
        this.cancelledAt = LocalDateTime.now();
    }

    public boolean isPendente() {
        return this.status == CobrancaStatus.PENDENTE;
    }

    public boolean isPaga() {
        return this.status == CobrancaStatus.PAGA;
    }

    public boolean isCancelada() {
        return this.status == CobrancaStatus.CANCELADA;
    }

    public boolean foiPagaComSaldo() {
        return isPaga() && this.tipoPagamento == TipoPagamento.SALDO;
    }

    public boolean foiPagaComCartao() {
        return isPaga() && this.tipoPagamento == TipoPagamento.CARTAO_CREDITO;
    }
}
