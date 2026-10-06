package com.pousada.util;

import com.pousada.model.Hospede;
import com.pousada.model.Pagamento;
import com.pousada.model.Quarto;
import com.pousada.model.Reserva;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.util.function.Consumer;

public final class HibernateUtil {
    private static final SessionFactory SESSION_FACTORY;

    static {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure() // lê /hibernate.cfg.xml do classpath (src/main/resources)
                .build();
        try {
            SESSION_FACTORY = new MetadataSources(registry)
                    .addAnnotatedClass(Hospede.class)
                    .addAnnotatedClass(Quarto.class)
                    .addAnnotatedClass(Reserva.class)
                    .addAnnotatedClass(Pagamento.class)
                    .buildMetadata()
                    .buildSessionFactory();
        } catch (Exception e) {
            StandardServiceRegistryBuilder.destroy(registry);
            throw new ExceptionInInitializerError(e);
        }
    }

    private HibernateUtil() {}

    public static SessionFactory getSessionFactory() {
        return SESSION_FACTORY;
    }


    public static void executarEmTransacao(Consumer<Session> acao) {
        try (Session session = SESSION_FACTORY.openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                acao.accept(session);
                tx.commit();
            } catch (RuntimeException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            }
        }
    }

    public static void shutdown() {
        SESSION_FACTORY.close();
    }
}
