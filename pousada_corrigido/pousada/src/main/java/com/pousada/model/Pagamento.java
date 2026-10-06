package com.pousada.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagamentos")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPagamento metodo;

    @Column(nullable = false)
    private LocalDateTime dataPagamento;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false, unique = true)
    private Reserva reserva;

    public Pagamento() {}

    public Pagamento(BigDecimal valor, MetodoPagamento metodo, Reserva reserva) {
        this.valor = valor;
        this.metodo = metodo;
        this.dataPagamento = LocalDateTime.now();
        this.reserva = reserva;
    }

    public Long getId() { return id; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public MetodoPagamento getMetodo() { return metodo; }
    public void setMetodo(MetodoPagamento metodo) { this.metodo = metodo; }
    public LocalDateTime getDataPagamento() { return dataPagamento; }
    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva reserva) { this.reserva = reserva; }

    @Override
    public String toString() {
        return "Pagamento{id=" + id + ", valor=" + valor + ", metodo=" + metodo + ", dataPagamento=" + dataPagamento + "}";
    }
}
