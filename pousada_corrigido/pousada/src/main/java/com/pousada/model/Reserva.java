package com.pousada.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate checkIn;

    @Column(nullable = false)
    private LocalDate checkOut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusReserva status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospede_id", nullable = false)
    private Hospede hospede;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quarto_id", nullable = false)
    private Quarto quarto;

    @OneToOne(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    private Pagamento pagamento;

    public Reserva() {}

    public Reserva(LocalDate checkIn, LocalDate checkOut, Hospede hospede, Quarto quarto) {
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.hospede = hospede;
        this.quarto = quarto;
        this.status = StatusReserva.PENDENTE;
        calcularValorTotal();
    }

    public void calcularValorTotal() {
        if (checkIn != null && checkOut != null && quarto != null) {
            long noites = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
            this.valorTotal = quarto.getDiaria().multiply(BigDecimal.valueOf(Math.max(noites, 0)));
        }
    }

    public Long getId() { return id; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; calcularValorTotal(); }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; calcularValorTotal(); }
    public StatusReserva getStatus() { return status; }
    public void setStatus(StatusReserva status) { this.status = status; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public Hospede getHospede() { return hospede; }
    public void setHospede(Hospede hospede) { this.hospede = hospede; }
    public Quarto getQuarto() { return quarto; }
    public void setQuarto(Quarto quarto) { this.quarto = quarto; calcularValorTotal(); }
    public Pagamento getPagamento() { return pagamento; }
    public void setPagamento(Pagamento pagamento) {
        this.pagamento = pagamento;
        if (pagamento != null) pagamento.setReserva(this);
    }

    @Override
    public String toString() {
        return "Reserva{id=" + id + ", checkIn=" + checkIn + ", checkOut=" + checkOut + ", status=" + status + ", valorTotal=" + valorTotal + ", hospede=" + (hospede != null ? hospede.getNome() : null) + ", quarto=" + (quarto != null ? quarto.getNumero() : null) + "}";
    }
}
