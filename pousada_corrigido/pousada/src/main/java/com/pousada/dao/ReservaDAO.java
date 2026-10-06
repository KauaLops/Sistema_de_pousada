package com.pousada.dao;

import com.pousada.model.Reserva;
import com.pousada.model.StatusReserva;
import com.pousada.util.HibernateUtil;
import org.hibernate.Session;

import java.time.LocalDate;
import java.util.List;

public class ReservaDAO {
    public void salvar(Reserva reserva) {
        HibernateUtil.executarEmTransacao(session -> session.persist(reserva));
    }

    public Reserva buscarPorId(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.find(Reserva.class, id);
        }
    }

    public Reserva buscarPorHospedeQuartoPeriodo(Long hospedeId, Long quartoId,
                                                 LocalDate checkIn, LocalDate checkOut) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "select r from Reserva r " +
                            "where r.hospede.id = :hospedeId " +
                            "and r.quarto.id = :quartoId " +
                            "and r.checkIn = :checkIn " +
                            "and r.checkOut = :checkOut", Reserva.class)
                    .setParameter("hospedeId", hospedeId)
                    .setParameter("quartoId", quartoId)
                    .setParameter("checkIn", checkIn)
                    .setParameter("checkOut", checkOut)
                    .uniqueResult();
        }
    }


    public boolean existeConflitoDePeriodo(Long quartoId, LocalDate checkIn, LocalDate checkOut) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long quantidade = session.createQuery(
                            "select count(r) from Reserva r " +
                            "where r.quarto.id = :quartoId " +
                            "and r.status <> :cancelada " +
                            "and r.checkIn < :checkOut " +
                            "and r.checkOut > :checkIn", Long.class)
                    .setParameter("quartoId", quartoId)
                    .setParameter("cancelada", StatusReserva.CANCELADA)
                    .setParameter("checkIn", checkIn)
                    .setParameter("checkOut", checkOut)
                    .uniqueResult();
            return quantidade != null && quantidade > 0;
        }
    }

    public long contarPorHospede(Long hospedeId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long qtd = session.createQuery(
                            "select count(r) from Reserva r where r.hospede.id = :id", Long.class)
                    .setParameter("id", hospedeId)
                    .uniqueResult();
            return qtd == null ? 0 : qtd;
        }
    }

    public long contarPorQuarto(Long quartoId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long qtd = session.createQuery(
                            "select count(r) from Reserva r where r.quarto.id = :id", Long.class)
                    .setParameter("id", quartoId)
                    .uniqueResult();
            return qtd == null ? 0 : qtd;
        }
    }

    public List<Reserva> listar() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "select r from Reserva r join fetch r.hospede join fetch r.quarto " +
                    "order by r.checkIn", Reserva.class).getResultList();
        }
    }
}
