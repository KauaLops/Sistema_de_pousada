package com.pousada.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quartos")
public class Quarto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoQuarto tipo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal diaria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusQuarto status;

    @OneToMany(mappedBy = "quarto")
    private List<Reserva> reservas = new ArrayList<>();

    public Quarto() {}

    public Quarto(Integer numero, TipoQuarto tipo, BigDecimal diaria) {
        this.numero = numero;
        this.tipo = tipo;
        this.diaria = diaria;
        this.status = StatusQuarto.DISPONIVEL;
    }

    public Long getId() { return id; }
    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public TipoQuarto getTipo() { return tipo; }
    public void setTipo(TipoQuarto tipo) { this.tipo = tipo; }
    public BigDecimal getDiaria() { return diaria; }
    public void setDiaria(BigDecimal diaria) { this.diaria = diaria; }
    public StatusQuarto getStatus() { return status; }
    public void setStatus(StatusQuarto status) { this.status = status; }
    public List<Reserva> getReservas() { return reservas; }

    @Override
    public String toString() {
        return "Quarto{id=" + id + ", numero=" + numero + ", tipo=" + tipo + ", diaria=" + diaria + ", status=" + status + "}";
    }
}
