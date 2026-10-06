package com.pousada;

import com.pousada.dao.HospedeDAO;
import com.pousada.dao.QuartoDAO;
import com.pousada.dao.ReservaDAO;
import com.pousada.model.Hospede;
import com.pousada.model.MetodoPagamento;
import com.pousada.model.Pagamento;
import com.pousada.model.Quarto;
import com.pousada.model.Reserva;
import com.pousada.model.StatusQuarto;
import com.pousada.model.StatusReserva;
import com.pousada.model.TipoQuarto;
import com.pousada.util.HibernateUtil;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.JDBCConnectionException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Main {
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    // Campo static: garante que a configuração do nível de log não seja descartada.
    private static final Logger HIBERNATE_LOG = Logger.getLogger("org.hibernate");

    private static final Pattern EMAIL_VALIDO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    // Tamanhos máximos das colunas (devem coincidir com as entidades)
    private static final int MAX_NOME = 120;
    private static final int MAX_EMAIL = 120;
    private static final int MAX_TELEFONE = 20;
    private static final int MAX_DOCUMENTO = 20;

    public static void main(String[] args) {
        HIBERNATE_LOG.setLevel(Level.SEVERE);

        HospedeDAO hospedeDAO = new HospedeDAO();
        QuartoDAO quartoDAO = new QuartoDAO();
        ReservaDAO reservaDAO = new ReservaDAO();

        try {
            HibernateUtil.getSessionFactory();
        } catch (Throwable e) {
            Throwable causa = e;
            while (causa.getCause() != null) causa = causa.getCause();
            System.out.println("Não foi possível conectar ao banco de dados: " + causa.getMessage());
            System.out.println("Verifique se o MariaDB está rodando e se usuário/senha em hibernate.cfg.xml estão corretos.");
            SCANNER.close();
            return;
        }

        try {
            executarMenu(hospedeDAO, quartoDAO, reservaDAO);
        } catch (NoSuchElementException e) {
            // Entrada encerrada (Ctrl+D / Ctrl+Z ou console fechado)
            System.out.println("\nEntrada encerrada. Finalizando o sistema...");
        } finally {
            SCANNER.close();
            HibernateUtil.shutdown();
        }
    }

    private static void executarMenu(HospedeDAO hospedeDAO, QuartoDAO quartoDAO, ReservaDAO reservaDAO) {
        int opcao;
        do {
            System.out.println("\n========================================");
            System.out.println("     SISTEMA DE GESTÃO DE POUSADA");
            System.out.println("========================================");
            System.out.println("1 - Adicionar hospedagem");
            System.out.println("2 - Listar hospedagens");
            System.out.println("3 - Cadastrar hóspede");
            System.out.println("4 - Listar hóspedes");
            System.out.println("5 - Cadastrar quarto");
            System.out.println("6 - Listar quartos");
            System.out.println("7 - Editar hóspede");
            System.out.println("8 - Excluir hóspede");
            System.out.println("9 - Editar quarto");
            System.out.println("10 - Excluir quarto");
            System.out.println("0 - Sair");
            System.out.println("========================================");

            opcao = lerInteiro("Escolha uma opção: ");

            try {
                switch (opcao) {
                    case 1 -> adicionarHospedagem(hospedeDAO, quartoDAO, reservaDAO);
                    case 2 -> listarHospedagens(reservaDAO);
                    case 3 -> cadastrarHospede(hospedeDAO);
                    case 4 -> listarHospedes(hospedeDAO);
                    case 5 -> cadastrarQuarto(quartoDAO);
                    case 6 -> listarQuartos(quartoDAO);
                    case 7 -> editarHospede(hospedeDAO);
                    case 8 -> excluirHospede(hospedeDAO, reservaDAO);
                    case 9 -> editarQuarto(quartoDAO);
                    case 10 -> excluirQuarto(quartoDAO, reservaDAO);
                    case 0 -> System.out.println("Encerrando o sistema...");
                    default -> System.out.println("Opção inválida.");
                }
            } catch (NoSuchElementException e) {
                throw e; // entrada encerrada: sobe para o main, senão o menu entraria em loop
            } catch (RuntimeException e) {
                System.out.println("\nNão foi possível concluir a operação: " + traduzirErro(e));
            }
        } while (opcao != 0);
    }

    /**
     * Converte exceções técnicas (Hibernate/JDBC) em mensagens compreensíveis.
     * Percorre toda a cadeia de causas, pois o Hibernate costuma encapsular o erro original.
     */
    private static String traduzirErro(RuntimeException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException) {
                return "o registro conflita com outro já existente (dado duplicado) "
                        + "ou está vinculado a outros registros.";
            }
            if (t instanceof DataException) {
                return "algum dado informado é grande demais para o campo do banco.";
            }
            if (t instanceof JDBCConnectionException) {
                return "a conexão com o banco de dados foi perdida. Verifique se o MariaDB está rodando.";
            }
        }

        Throwable causa = e;
        while (causa.getCause() != null) causa = causa.getCause();
        String mensagem = causa.getMessage();
        return (mensagem == null || mensagem.isBlank()) ? causa.getClass().getSimpleName() : mensagem;
    }

    /**
     * Permite criar várias hospedagens, uma após a outra, sem encerrar o programa.
     * Nada é gravado no banco até que todos os dados tenham sido informados e validados,
     * evitando deixar hóspedes ou quartos "soltos" caso a pessoa desista no meio.
     */
    private static void adicionarHospedagem(HospedeDAO hospedeDAO,
                                            QuartoDAO quartoDAO,
                                            ReservaDAO reservaDAO) {
        System.out.println("\n--- NOVA HOSPEDAGEM ---");

        String email = lerEmail("E-mail do hóspede (existente ou novo): ");
        Hospede hospede = hospedeDAO.buscarPorEmail(email);
        boolean hospedeNovo = false;

        if (hospede == null) {
            System.out.println("Hóspede não encontrado. Vamos cadastrá-lo.");
            String nome = lerTexto("Nome: ", MAX_NOME);
            String telefone = lerTexto("Telefone: ", MAX_TELEFONE);
            String documento = lerTexto("Documento: ", MAX_DOCUMENTO);
            hospede = new Hospede(nome, email, telefone, documento);
            hospedeNovo = true;
        } else {
            System.out.println("Hóspede encontrado: " + hospede.getNome() + " (ID " + hospede.getId() + ")");
        }

        listarQuartos(quartoDAO);
        int numeroQuarto = lerInteiroPositivo("Número do quarto: ");
        Quarto quarto = quartoDAO.buscarPorNumero(numeroQuarto);
        boolean quartoNovo = false;

        if (quarto == null) {
            System.out.println("Quarto não encontrado. Vamos cadastrá-lo.");
            TipoQuarto tipo = lerTipoQuarto();
            BigDecimal diaria = lerDecimal("Valor da diária: R$ ");
            quarto = new Quarto(numeroQuarto, tipo, diaria);
            quartoNovo = true;
        }

        if (quarto.getStatus() == StatusQuarto.MANUTENCAO) {
            System.out.println("Este quarto está em manutenção e não pode receber hospedagem.");
            return;
        }

        LocalDate checkIn = lerData("Check-in (dd/MM/yyyy): ");
        LocalDate checkOut = lerData("Check-out (dd/MM/yyyy): ");

        if (!checkOut.isAfter(checkIn)) {
            System.out.println("O check-out deve ser depois do check-in.");
            return;
        }

        // Quarto novo ainda não tem ID nem hospedagens, então não há conflito possível.
        if (!quartoNovo && reservaDAO.existeConflitoDePeriodo(quarto.getId(), checkIn, checkOut)) {
            System.out.println("O quarto " + quarto.getNumero() + " já possui uma hospedagem nesse período.");
            return;
        }

        Reserva reserva = new Reserva(checkIn, checkOut, hospede, quarto);
        reserva.setStatus(StatusReserva.CONFIRMADA);

        System.out.println("Valor total: R$ " + reserva.getValorTotal());
        MetodoPagamento metodo = lerMetodoPagamento();
        Pagamento pagamento = new Pagamento(reserva.getValorTotal(), metodo, reserva);
        reserva.setPagamento(pagamento);

        // Só agora grava tudo, na ordem certa.
        if (hospedeNovo) {
            hospedeDAO.salvar(hospede);
            System.out.println("Hóspede cadastrado com ID " + hospede.getId() + ".");
        }
        if (quartoNovo) {
            quartoDAO.salvar(quarto);
            System.out.println("Quarto cadastrado com ID " + quarto.getId() + ".");
        }
        reservaDAO.salvar(reserva);

        System.out.println("\nHospedagem cadastrada com sucesso!");
        System.out.println("ID da reserva: " + reserva.getId());
        System.out.println("Hóspede: " + hospede.getNome());
        System.out.println("Quarto: " + quarto.getNumero());
        System.out.println("Período: " + checkIn.format(DATE_FORMAT) + " até " + checkOut.format(DATE_FORMAT));
        System.out.println("Valor: R$ " + reserva.getValorTotal());
        System.out.println("Pagamento: " + metodo);
    }

    private static void listarHospedagens(ReservaDAO reservaDAO) {
        System.out.println("\n--- HOSPEDAGENS CADASTRADAS ---");
        List<Reserva> reservas = reservaDAO.listar();

        if (reservas.isEmpty()) {
            System.out.println("Nenhuma hospedagem cadastrada.");
            return;
        }

        reservas.forEach(reserva -> System.out.println(
                "ID " + reserva.getId()
                        + " | Hóspede: " + reserva.getHospede().getNome()
                        + " | Quarto: " + reserva.getQuarto().getNumero()
                        + " | " + reserva.getCheckIn().format(DATE_FORMAT)
                        + " até " + reserva.getCheckOut().format(DATE_FORMAT)
                        + " | Status: " + reserva.getStatus()
                        + " | Total: R$ " + reserva.getValorTotal()));
    }

    private static void cadastrarHospede(HospedeDAO hospedeDAO) {
        System.out.println("\n--- CADASTRAR HÓSPEDE ---");
        String email = lerEmail("E-mail: ");

        if (hospedeDAO.buscarPorEmail(email) != null) {
            System.out.println("Já existe um hóspede com esse e-mail.");
            return;
        }

        String nome = lerTexto("Nome: ", MAX_NOME);
        String telefone = lerTexto("Telefone: ", MAX_TELEFONE);
        String documento = lerTexto("Documento: ", MAX_DOCUMENTO);

        Hospede hospede = new Hospede(nome, email, telefone, documento);
        hospedeDAO.salvar(hospede);
        System.out.println("Hóspede cadastrado com ID " + hospede.getId() + ".");
    }

    private static void listarHospedes(HospedeDAO hospedeDAO) {
        System.out.println("\n--- HÓSPEDES ---");
        List<Hospede> hospedes = hospedeDAO.listar();
        if (hospedes.isEmpty()) {
            System.out.println("Nenhum hóspede cadastrado.");
            return;
        }
        hospedes.forEach(System.out::println);
    }

    private static void cadastrarQuarto(QuartoDAO quartoDAO) {
        System.out.println("\n--- CADASTRAR QUARTO ---");
        int numero = lerInteiroPositivo("Número: ");

        if (quartoDAO.buscarPorNumero(numero) != null) {
            System.out.println("Já existe um quarto com esse número.");
            return;
        }

        TipoQuarto tipo = lerTipoQuarto();
        BigDecimal diaria = lerDecimal("Valor da diária: R$ ");

        Quarto quarto = new Quarto(numero, tipo, diaria);
        quartoDAO.salvar(quarto);
        System.out.println("Quarto cadastrado com ID " + quarto.getId() + ".");
    }

    private static void listarQuartos(QuartoDAO quartoDAO) {
        System.out.println("\n--- QUARTOS ---");
        List<Quarto> quartos = quartoDAO.listar();
        if (quartos.isEmpty()) {
            System.out.println("Nenhum quarto cadastrado.");
            return;
        }
        quartos.forEach(System.out::println);
    }

    private static void editarHospede(HospedeDAO hospedeDAO) {
        System.out.println("\n--- EDITAR HÓSPEDE ---");
        Long id = (long) lerInteiroPositivo("ID do hóspede: ");
        Hospede hospede = hospedeDAO.buscarPorId(id);

        if (hospede == null) {
            System.out.println("Hóspede não encontrado.");
            return;
        }

        System.out.println("Editando: " + hospede);
        String nome = lerTexto("Novo nome: ", MAX_NOME);
        String telefone = lerTexto("Novo telefone: ", MAX_TELEFONE);
        String documento = lerTexto("Novo documento: ", MAX_DOCUMENTO);

        hospede.setNome(nome);
        hospede.setTelefone(telefone);
        hospede.setDocumento(documento);
        hospedeDAO.atualizar(hospede);

        System.out.println("Hóspede atualizado com sucesso.");
    }

    private static void excluirHospede(HospedeDAO hospedeDAO, ReservaDAO reservaDAO) {
        System.out.println("\n--- EXCLUIR HÓSPEDE ---");
        Long id = (long) lerInteiroPositivo("ID do hóspede: ");
        Hospede hospede = hospedeDAO.buscarPorId(id);

        if (hospede == null) {
            System.out.println("Hóspede não encontrado.");
            return;
        }

        long qtdReservas = reservaDAO.contarPorHospede(id);
        if (qtdReservas > 0) {
            System.out.println("Atenção: esse hóspede possui " + qtdReservas
                    + " hospedagem(ns) registrada(s). Excluí-lo também apagará essas hospedagens e pagamentos.");
        }

        if (!confirmar("Confirmar exclusão de \"" + hospede.getNome() + "\"? (s/n): ")) {
            System.out.println("Exclusão cancelada.");
            return;
        }

        hospedeDAO.excluir(id);
        System.out.println("Hóspede excluído com sucesso.");
    }

    private static void editarQuarto(QuartoDAO quartoDAO) {
        System.out.println("\n--- EDITAR QUARTO ---");
        int numero = lerInteiroPositivo("Número do quarto: ");
        Quarto quarto = quartoDAO.buscarPorNumero(numero);

        if (quarto == null) {
            System.out.println("Quarto não encontrado.");
            return;
        }

        System.out.println("Editando: " + quarto);
        TipoQuarto tipo = lerTipoQuarto();
        BigDecimal diaria = lerDecimal("Novo valor da diária: R$ ");
        StatusQuarto status = lerStatusQuarto();

        quarto.setTipo(tipo);
        quarto.setDiaria(diaria);
        quarto.setStatus(status);
        quartoDAO.atualizar(quarto);

        System.out.println("Quarto atualizado com sucesso.");
    }

    private static void excluirQuarto(QuartoDAO quartoDAO, ReservaDAO reservaDAO) {
        System.out.println("\n--- EXCLUIR QUARTO ---");
        int numero = lerInteiroPositivo("Número do quarto: ");
        Quarto quarto = quartoDAO.buscarPorNumero(numero);

        if (quarto == null) {
            System.out.println("Quarto não encontrado.");
            return;
        }

        long qtdReservas = reservaDAO.contarPorQuarto(quarto.getId());
        if (qtdReservas > 0) {
            System.out.println("O quarto " + quarto.getNumero() + " possui " + qtdReservas
                    + " hospedagem(ns) registrada(s) e não pode ser excluído. "
                    + "Se ele não deve mais receber hóspedes, altere o status para MANUTENCAO.");
            return;
        }

        if (!confirmar("Confirmar exclusão do quarto " + quarto.getNumero() + "? (s/n): ")) {
            System.out.println("Exclusão cancelada.");
            return;
        }

        quartoDAO.excluir(quarto.getId());
        System.out.println("Quarto excluído com sucesso.");
    }

    // ------------------------------------------------------------------
    // Leitura e validação de entradas
    // ------------------------------------------------------------------

    private static TipoQuarto lerTipoQuarto() {
        while (true) {
            System.out.println("Tipo do quarto:");
            System.out.println("1 - STANDARD");
            System.out.println("2 - DELUXE");
            System.out.println("3 - FAMILIA");
            int opcao = lerInteiro("Escolha: ");

            switch (opcao) {
                case 1: return TipoQuarto.STANDARD;
                case 2: return TipoQuarto.DELUXE;
                case 3: return TipoQuarto.FAMILIA;
                default: System.out.println("Opção inválida.");
            }
        }
    }

    private static StatusQuarto lerStatusQuarto() {
        while (true) {
            System.out.println("Status do quarto:");
            System.out.println("1 - DISPONIVEL");
            System.out.println("2 - OCUPADO");
            System.out.println("3 - MANUTENCAO");
            int opcao = lerInteiro("Escolha: ");

            switch (opcao) {
                case 1: return StatusQuarto.DISPONIVEL;
                case 2: return StatusQuarto.OCUPADO;
                case 3: return StatusQuarto.MANUTENCAO;
                default: System.out.println("Opção inválida.");
            }
        }
    }

    private static MetodoPagamento lerMetodoPagamento() {
        while (true) {
            System.out.println("Método de pagamento:");
            System.out.println("1 - PIX");
            System.out.println("2 - CARTAO");
            System.out.println("3 - DINHEIRO");
            int opcao = lerInteiro("Escolha: ");

            switch (opcao) {
                case 1: return MetodoPagamento.PIX;
                case 2: return MetodoPagamento.CARTAO;
                case 3: return MetodoPagamento.DINHEIRO;
                default: System.out.println("Opção inválida.");
            }
        }
    }

    private static int lerInteiro(String mensagem) {
        while (true) {
            try {
                System.out.print(mensagem);
                return Integer.parseInt(SCANNER.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Digite um número inteiro válido.");
            }
        }
    }

    /** Lê um inteiro maior que zero (números de quarto, IDs). */
    private static int lerInteiroPositivo(String mensagem) {
        while (true) {
            int valor = lerInteiro(mensagem);
            if (valor > 0) {
                return valor;
            }
            System.out.println("Digite um número maior que zero.");
        }
    }

    private static BigDecimal lerDecimal(String mensagem) {
        while (true) {
            try {
                System.out.print(mensagem);
                String valor = SCANNER.nextLine().trim().replace(',', '.');
                BigDecimal decimal = new BigDecimal(valor);
                if (decimal.signum() <= 0) {
                    System.out.println("O valor deve ser maior que zero.");
                    continue;
                }
                decimal = decimal.setScale(2, RoundingMode.HALF_UP);
                if (decimal.precision() > 10) { // coluna DECIMAL(10,2)
                    System.out.println("Valor alto demais (máximo 99999999.99).");
                    continue;
                }
                return decimal;
            } catch (NumberFormatException e) {
                System.out.println("Digite um valor decimal válido.");
            }
        }
    }

    private static String lerTexto(String mensagem) {
        return lerTexto(mensagem, Integer.MAX_VALUE);
    }

    /** Lê um texto não vazio, respeitando o tamanho máximo da coluna no banco. */
    private static String lerTexto(String mensagem, int tamanhoMaximo) {
        while (true) {
            System.out.print(mensagem);
            String texto = SCANNER.nextLine().trim();
            if (texto.isBlank()) {
                System.out.println("O campo não pode ficar vazio.");
            } else if (texto.length() > tamanhoMaximo) {
                System.out.println("Máximo de " + tamanhoMaximo + " caracteres (você digitou " + texto.length() + ").");
            } else {
                return texto;
            }
        }
    }

    private static String lerEmail(String mensagem) {
        while (true) {
            String email = lerTexto(mensagem, MAX_EMAIL);
            if (EMAIL_VALIDO.matcher(email).matches()) {
                return email;
            }
            System.out.println("E-mail inválido. Exemplo: nome@dominio.com");
        }
    }

    private static LocalDate lerData(String mensagem) {
        while (true) {
            try {
                String texto = lerTexto(mensagem);
                return LocalDate.parse(texto, DATE_FORMAT);
            } catch (DateTimeParseException e) {
                System.out.println("Data inválida. Use o formato dd/MM/yyyy (ex.: 25/12/2026).");
            }
        }
    }

    /** Pergunta s/n até receber uma resposta válida. */
    private static boolean confirmar(String mensagem) {
        while (true) {
            String resposta = lerTexto(mensagem);
            if (resposta.equalsIgnoreCase("s")) return true;
            if (resposta.equalsIgnoreCase("n")) return false;
            System.out.println("Responda com 's' ou 'n'.");
        }
    }
}