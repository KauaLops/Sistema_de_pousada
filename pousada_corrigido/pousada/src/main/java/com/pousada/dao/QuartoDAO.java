package com.pousada.dao;

import com.pousada.model.Quarto;
import com.pousada.model.StatusQuarto;
import com.pousada.util.HibernateUtil;
import org.hibernate.Session;

import java.util.List;

public class QuartoDAO {
    public void salvar(Quarto quarto) {
        HibernateUtil.executarEmTransacao(session -> session.persist(quarto));
    }

    public Quarto buscarPorId(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.find(Quarto.class, id);
        }
    }

    public Quarto buscarPorNumero(Integer numero) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from Quarto where numero = :numero", Quarto.class)
                    .setParameter("numero", numero)
                    .uniqueResult();
        }
    }

    public List<Quarto> listar() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Quarto order by numero", Quarto.class).getResultList();
        }
    }

    public List<Quarto> listarDisponiveis() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from Quarto where status = :status order by numero", Quarto.class)
                    .setParameter("status", StatusQuarto.DISPONIVEL)
                    .getResultList();
        }
    }

    public void atualizar(Quarto quarto) {
        HibernateUtil.executarEmTransacao(session -> session.merge(quarto));
    }

    public void excluir(Long id) {
        HibernateUtil.executarEmTransacao(session -> {
            Quarto quarto = session.find(Quarto.class, id);
            if (quarto != null) session.remove(quarto);
        });
    }
}
