package es.upm.filmrecommender.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import es.upm.filmrecommender.agents.AgenteInterfazUsuario;
import es.upm.filmrecommender.utils.ClimaClient;
import es.upm.filmrecommender.data.*;

public class RecommenderGui extends JFrame {

    private int selectedRatingIndex = -1; 
    private AgenteInterfazUsuario agent;
    private JTextField genreTextField;
    private JButton recommendButton;
    private JTextArea resultsTextArea;
    private JLabel[] stars = new JLabel[5];
    private boolean isRecommendButtonHovering = false; 
    private boolean isbotonClimaHovering = false;
    private boolean isbotonVistasHovering = false;
    private List<Movie> peliculasMostradas = new ArrayList<>();
    private String ultimaFraseClima = null;

    public RecommenderGui(AgenteInterfazUsuario agent) {
        super("Sistema Recomendador de Películas");
        this.agent = agent;

        initComponents();
        setupLayout();
        

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(650, 600); 
        setLocationRelativeTo(null);
        
    }
    
    public void setUltimaFraseClima(String frase) {
        this.ultimaFraseClima = frase;
    }


    private void initComponents() {
        genreTextField = new JTextField(20);
        recommendButton = new JButton("Obtener Recomendaciones") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                Color topColor = isRecommendButtonHovering ? new Color(255, 130, 130) : new Color(255, 210, 210); 
                Color bottomColor = isRecommendButtonHovering ? new Color(255, 115, 115) : new Color(255, 130, 130);
                
                GradientPaint gp = new GradientPaint(0, 5, topColor, 0, getHeight(), bottomColor);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                
                super.paintComponent(g); 
            }
        };

        recommendButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isRecommendButtonHovering = true;
                recommendButton.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isRecommendButtonHovering = false;
                recommendButton.repaint();
            }
        });
        
       
        recommendButton.setOpaque(false); 
        recommendButton.setContentAreaFilled(false); 
        recommendButton.setBorderPainted(true);
        recommendButton.setFocusPainted(false); 
        recommendButton.setForeground(Color.BLACK); 
        recommendButton.setFont(new Font("SansSerif", Font.BOLD, 14));

       
        recommendButton.addActionListener(e -> {
            String genre = genreTextField.getText().trim();
            
            if (!genre.isEmpty()) {
                agent.solicitarRecomendaciones(genre); 
            } else {
                 JOptionPane.showMessageDialog(RecommenderGui.this,
                         "Por favor, introduce o selecciona un género para buscar.",
                         "Entrada Requerida",
                         JOptionPane.WARNING_MESSAGE);
            }
        });

        resultsTextArea = new JTextArea(15, 50); 
        resultsTextArea.setEditable(false);
        resultsTextArea.setLineWrap(true);
        resultsTextArea.setWrapStyleWord(true);
        resultsTextArea.setFont(new Font("Monospaced", Font.BOLD, 12)); 
        
        resultsTextArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    int clickedPosition = resultsTextArea.viewToModel2D(e.getPoint());
                    int clickedLine = resultsTextArea.getLineOfOffset(clickedPosition);
                    int start = resultsTextArea.getLineStartOffset(clickedLine);
                    int end = resultsTextArea.getLineEndOffset(clickedLine);
                    String clickedText = resultsTextArea.getText(start, end - start).trim();

                    if (clickedText.startsWith("- ")) {
                        String tituloExtraido = clickedText.substring(2).split("\\(Rating")[0].trim();
                        Movie seleccionada = peliculasMostradas.stream()
                                .filter(p -> p.getTitle().equalsIgnoreCase(tituloExtraido))
                                .findFirst()
                                .orElse(null);

                        if (seleccionada != null) {
                            HistorialVistosManager.guardarPeliculaVista(seleccionada);
                            JOptionPane.showMessageDialog(RecommenderGui.this,
                                    "Película marcada como vista: " + seleccionada.getTitle(),
                                    "Vista registrada", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }

                } catch (Exception ex) {
                    System.err.println("Error al detectar línea clicada: " + ex.getMessage());
                }
            }
        });


        
        for (int i = 0; i < stars.length; i++) {
            final int index = i;
            stars[i] = new JLabel(" ☆ "); 
            stars[i].setFont(new Font("SansSerif", Font.BOLD, 28)); 
            stars[i].setForeground(Color.LIGHT_GRAY); 
            stars[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            stars[i].addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    
                    highlightStarsOnHover(index);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                   
                    updateStarsDisplay();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (selectedRatingIndex == index) { 
                        selectedRatingIndex = -1; 
                        System.out.println("GUI: Rating deseleccionado.");
                    } else {
                        selectedRatingIndex = index;
                        System.out.println("GUI: Rating seleccionado: " + getSelectedMinimumRating());
                        
                    }
                    updateStarsDisplay(); 
                }
            });
        }
    }
    
    private void highlightStarsOnHover(int hoverIndex) {
        for (int i = 0; i < stars.length; i++) {
            if (i <= hoverIndex) {
                stars[i].setText(" ★ ");
                stars[i].setForeground(Color.ORANGE);
            } else {
                stars[i].setText(" ☆ "); 
                 
                stars[i].setForeground(i <= selectedRatingIndex ? Color.ORANGE : Color.LIGHT_GRAY);

            }
        }
    }

    private void updateStarsDisplay() {
        for (int i = 0; i < stars.length; i++) {
            if (i <= selectedRatingIndex) {
                stars[i].setText(" ★ "); 
                stars[i].setForeground(Color.ORANGE);
            } else {
                stars[i].setText(" ☆ "); 
                stars[i].setForeground(Color.LIGHT_GRAY);
            }
        }
    }


    private void setupLayout() {
        
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5)); 
        inputPanel.add(new JLabel("Buscar:"));
        inputPanel.add(genreTextField);
        inputPanel.add(recommendButton);

        
        String[] genres = {
            "Acción", "Aventura", "Animación", "Comedia", "Crimen",
            "Documental", "Drama", "Familia", "Fantasía", "Historia",
            "Terror", "Música", "Misterio", "Romance", "Ciencia ficción",
            "Película de TV", "Suspense", "Bélica", "Western"
        };

        JPanel genreButtonsPanelContainer = new JPanel();
        genreButtonsPanelContainer.setLayout(new BoxLayout(genreButtonsPanelContainer, BoxLayout.Y_AXIS));
        genreButtonsPanelContainer.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));


        JPanel row1Panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 3));
        for (int i = 0; i < 10 && i < genres.length; i++) {
            addGenreButton(row1Panel, genres[i]);
        }
        genreButtonsPanelContainer.add(row1Panel);

        JPanel row2Panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 3));
        for (int i = 10; i < genres.length; i++) {
            addGenreButton(row2Panel, genres[i]);
        }
        genreButtonsPanelContainer.add(row2Panel);

        // Panel de Estrellas y sus valores
        JPanel starsDisplayPanel = new JPanel();
        starsDisplayPanel.setLayout(new BoxLayout(starsDisplayPanel, BoxLayout.Y_AXIS));
        starsDisplayPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JLabel ratingTitleLabel = new JLabel("Valoración Mínima:");
        ratingTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        ratingTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        starsDisplayPanel.add(ratingTitleLabel);
        starsDisplayPanel.add(Box.createRigidArea(new Dimension(0,2)));

        JPanel starsRowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, -5, 0));
        for (JLabel star : stars) {
            starsRowPanel.add(star);
        }
        starsDisplayPanel.add(starsRowPanel);
        
        JPanel ratingsTextPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, -5, 0));
        String[] ratingValues = { "2.0", "4.0", "6.0", "8.0", "10" };
        for (String ratingValue : ratingValues) {
            JLabel ratingLabel = new JLabel(ratingValue);
            ratingLabel.setPreferredSize(new Dimension(stars[0].getPreferredSize().width, 15));
            ratingLabel.setHorizontalAlignment(SwingConstants.CENTER);
            ratingLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
            ratingsTextPanel.add(ratingLabel);
        }
        starsDisplayPanel.add(ratingsTextPanel);

     // NUEVO botón de "Películas vistas"
        JButton botonVistos = new JButton("Películas vistas") {
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color topColor = isbotonVistasHovering ? new Color(255, 200, 200) : new Color(255, 230, 230); 
                Color bottomColor = isbotonVistasHovering ? new Color(255, 150, 60) : new Color(255, 180, 120);

        GradientPaint gp = new GradientPaint(0, 10, topColor, 1, getHeight(), bottomColor);
       
       
        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }};

    botonVistos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isbotonVistasHovering = true;
                botonVistos.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isbotonVistasHovering = false;
                botonVistos.repaint();
            }
        });
botonVistos.setOpaque(false);
botonVistos.setContentAreaFilled(false);
botonVistos.setBorderPainted(true);
botonVistos.setFocusPainted(false);
botonVistos.setForeground(Color.BLACK);
botonVistos.setFont(new Font("SansSerif", Font.BOLD, 14));
botonVistos.setPreferredSize(new Dimension(160, 35));


        botonVistos.addActionListener(e -> {
            List<Movie> vistas = HistorialVistosManager.cargarPeliculasVistas();
            limpiarResultados();
            if (vistas.isEmpty()) {
                mostrarResultados("No has marcado ninguna película como vista todavía.");
            } else {
                mostrarResultados("--- Películas Vistas ---");
                for (Movie peli : vistas) {
                    mostrarResultados("- " + peli.getTitle() + " (Rating: " +
                        String.format("%.2f", peli.getVoteAverage()) + ")");
                }
            }
        });

       JButton botonClima = new JButton("Clima") {
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color topColor = isbotonClimaHovering ? new Color(200, 255, 200) : new Color(230, 255, 230); 
                Color bottomColor = isbotonClimaHovering ? new Color(150, 255, 60) : new Color(120, 255, 120);

        GradientPaint gp = new GradientPaint(0, 10, topColor, 1, getHeight(), bottomColor);

        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }};

    botonClima.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isbotonClimaHovering = true;
                botonClima.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isbotonClimaHovering = false;
                botonClima.repaint();
            }
        });
        
botonClima.setOpaque(false);
botonClima.setContentAreaFilled(false);
botonClima.setBorderPainted(true);
botonClima.setFocusPainted(false);
botonClima.setForeground(Color.BLACK);
botonClima.setFont(new Font("SansSerif", Font.BOLD, 14));
botonClima.setPreferredSize(new Dimension(100, 35));

        botonClima.addActionListener(e -> {
            limpiarResultados();
            mostrarResultados("Consultando el clima actual en Madrid...");
            ultimaFraseClima = ClimaClient.obtenerFraseClimaMadrid();
            mostrarResultados(ultimaFraseClima);
            agent.solicitarRecomendacionPorClima();
        });


        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(inputPanel);
        topPanel.add(genreButtonsPanelContainer);

        JPanel panelBotonesSecundarios = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        panelBotonesSecundarios.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        panelBotonesSecundarios.add(botonVistos);
        panelBotonesSecundarios.add(botonClima);
        topPanel.add(panelBotonesSecundarios);

        topPanel.add(starsDisplayPanel);


        getContentPane().setLayout(new BorderLayout(10, 10));
        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(resultsTextArea), BorderLayout.CENTER);
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

    
    }

    private void addGenreButton(JPanel panel, String genreName) {
        JButton genreButton = new JButton(genreName);
        genreButton.setMargin(new Insets(2, 6, 2, 6));
        genreButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        genreButton.setFocusPainted(false);

        Color defaultBg = genreButton.getBackground();
        Color hoverBg = new Color(170, 225, 170);

        genreButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                genreButton.setBackground(hoverBg);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                genreButton.setBackground(defaultBg);
            }
        });
        
        genreButton.addActionListener(e -> {
            genreTextField.setText(genreName);
            recommendButton.doClick();
        });
        panel.add(genreButton);
    }
    
    public void mostrarPeliculas(List<Movie> peliculas) {
        peliculasMostradas = peliculas;

        SwingUtilities.invokeLater(() -> {
            resultsTextArea.setText("");
            if (ultimaFraseClima != null) {
                resultsTextArea.append(ultimaFraseClima + "\n\n");
            }
            resultsTextArea.append("--- Películas Recomendadas ---\n");
            for (Movie peli : peliculas) {
                resultsTextArea.append("- " + peli.getTitle() + " (Rating: " +
                        String.format("%.2f", peli.getVoteAverage()) + ")\n");
            }
        });
    }
    
    public void resetearClima() {
        ultimaFraseClima = null;
    }

    public double getSelectedMinimumRating() {
        if (selectedRatingIndex == -1) {
            return 0.0;
        }
        return (selectedRatingIndex + 1) * 2.0;
    }

    public void mostrarResultados(String texto) {
        SwingUtilities.invokeLater(() -> resultsTextArea.append(texto + "\n"));
    }

    public void limpiarResultados() {
        SwingUtilities.invokeLater(() -> resultsTextArea.setText(""));
    }
}