package com.pousada.dao;

import com.pousada.model.Hospede;
import com.pousada.util.HibernateUtil;
import org.hibernate.Session;

import java.util.List;

public class HospedeDAO {
    public void salvar(Hospede hospede) {
        HibernateUtil.executarEmTransacao(session -> session.persist(hospede));
    }

    public Hospede buscarPorId(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.find(Hospede.class, id);
        }
    }

    public Hospede buscarPorEmail(String email) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from Hospede where email = :email", Hospede.class)
                    .setParameter("email", email)
                    .uniqueResult();
        }
    }

    public List<Hospede> listar() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Hospede order by nome", Hospede.class).getResultList();
        }
    }

    public void atualizar(Hospede hospede) {
        HibernateUtil.executarEmTransacao(session -> session.merge(hospede));
    }

    public void excluir(Long id) {
        HibernateUtil.executarEmTransacao(session -> {
            Hospede hospede = session.find(Hospede.class, id);
            if (hospede != null) session.remove(hospede);
        });
    }
}
