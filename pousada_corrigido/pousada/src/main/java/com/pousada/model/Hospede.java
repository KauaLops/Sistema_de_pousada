package com.pousada.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hospedes")
public class Hospede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, length = 20)
    private String telefone;

    @Column(nullable = false, length = 20)
    private String documento;

    @OneToMany(mappedBy = "hospede", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reserva> reservas = new ArrayList<>();

    public Hospede() {}

    public Hospede(String nome, String email, String telefone, String documento) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.documento = documento;
    }

    public void adicionarReserva(Reserva reserva) {
        reservas.add(reserva);
        reserva.setHospede(this);
    }

    public void removerReserva(Reserva reserva) {
        reservas.remove(reserva);
        reserva.setHospede(null);
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
    public List<Reserva> getReservas() { return reservas; }

    @Override
    public String toString() {
        return "Hospede{id=" + id + ", nome='" + nome + "', email='" + email + "', telefone='" + telefone + "', documento='" + documento + "'}";
    }
}
