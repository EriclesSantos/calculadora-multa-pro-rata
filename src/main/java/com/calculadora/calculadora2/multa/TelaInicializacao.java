package com.calculadora.calculadora2.multa;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Feedback visual exibido imediatamente ao abrir o JAR por duplo clique.
 */
public final class TelaInicializacao {

    private static final Color FUNDO = new Color(18, 13, 34);
    private static final Color BORDA = new Color(105, 68, 190);
    private static final Color DESTAQUE = new Color(65, 225, 235);
    private static final Color TEXTO = new Color(242, 239, 252);
    private static final Color TEXTO_SECUNDARIO = new Color(190, 182, 214);

    private static JWindow janela;
    private static JLabel mensagem;
    private static JProgressBar progresso;

    private TelaInicializacao() {
    }

    public static void exibir(String[] argumentos) {
        if (!estaHabilitada(argumentos) || GraphicsEnvironment.isHeadless()) {
            return;
        }

        executarNaInterfaceEEsperar(TelaInicializacao::criarJanela);
    }

    public static void atualizar(String texto) {
        if (janela == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            if (mensagem != null) {
                mensagem.setText(texto);
            }
        });
    }

    public static void concluir() {
        if (janela == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            mensagem.setText("Pronto. Abrindo a calculadora no navegador...");
            progresso.setIndeterminate(false);
            progresso.setValue(100);

            Timer temporizador = new Timer(700, evento -> fechar());
            temporizador.setRepeats(false);
            temporizador.start();
        });
    }

    public static void informarFalha(String detalhe) {
        if (janela == null) {
            System.err.println(detalhe);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            progresso.setIndeterminate(false);
            progresso.setValue(0);
            mensagem.setText("Não foi possível concluir a inicialização.");

            JOptionPane.showMessageDialog(
                    janela,
                    detalhe,
                    "NÖRDHUB - Falha ao iniciar",
                    JOptionPane.ERROR_MESSAGE
            );
            fechar();
        });
    }

    static boolean estaHabilitada(String[] argumentos) {
        String propriedade = System.getProperty(
                "calculadora.splash.enabled",
                "true"
        );
        if ("false".equalsIgnoreCase(propriedade)) {
            return false;
        }

        return argumentos == null || Arrays.stream(argumentos)
                .noneMatch("--calculadora.splash.enabled=false"::equalsIgnoreCase);
    }

    private static void criarJanela() {
        if (janela != null) {
            return;
        }

        janela = new JWindow();
        JPanel conteudo = new JPanel(new BorderLayout(16, 14));
        conteudo.setBackground(FUNDO);
        conteudo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDA, 2),
                BorderFactory.createEmptyBorder(22, 24, 20, 24)
        ));

        JLabel logo = new JLabel(carregarLogo());
        logo.setPreferredSize(new Dimension(58, 58));
        conteudo.add(logo, BorderLayout.WEST);

        JPanel informacoes = new JPanel(new BorderLayout(0, 9));
        informacoes.setOpaque(false);

        JLabel titulo = new JLabel("NÖRDHUB — Calculadora");
        titulo.setForeground(TEXTO);
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        informacoes.add(titulo, BorderLayout.NORTH);

        mensagem = new JLabel("Preparando o servidor local...");
        mensagem.setForeground(TEXTO_SECUNDARIO);
        mensagem.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        informacoes.add(mensagem, BorderLayout.CENTER);

        progresso = new JProgressBar(SwingConstants.HORIZONTAL);
        progresso.setIndeterminate(true);
        progresso.setBorderPainted(false);
        progresso.setForeground(DESTAQUE);
        progresso.setBackground(new Color(43, 32, 69));
        progresso.setPreferredSize(new Dimension(300, 9));
        informacoes.add(progresso, BorderLayout.SOUTH);

        conteudo.add(informacoes, BorderLayout.CENTER);

        janela.setContentPane(conteudo);
        janela.setSize(455, 130);
        janela.setLocationRelativeTo(null);
        janela.setAlwaysOnTop(true);
        janela.setVisible(true);
        janela.toFront();
    }

    private static ImageIcon carregarLogo() {
        try (InputStream arquivo = TelaInicializacao.class.getResourceAsStream(
                "/static/favicon.png"
        )) {
            if (arquivo == null) {
                return new ImageIcon();
            }

            BufferedImage original = ImageIO.read(arquivo);
            Image reduzida = original.getScaledInstance(
                    54,
                    54,
                    Image.SCALE_SMOOTH
            );
            return new ImageIcon(reduzida);
        } catch (Exception ignored) {
            return new ImageIcon();
        }
    }

    private static void fechar() {
        if (janela != null) {
            janela.setVisible(false);
            janela.dispose();
            janela = null;
            mensagem = null;
            progresso = null;
        }
    }

    private static void executarNaInterfaceEEsperar(Runnable acao) {
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                acao.run();
            } else {
                SwingUtilities.invokeAndWait(acao);
            }
        } catch (Exception erro) {
            System.err.println(
                    "Nao foi possivel exibir a tela de inicializacao: "
                            + erro.getMessage()
            );
        }
    }
}
