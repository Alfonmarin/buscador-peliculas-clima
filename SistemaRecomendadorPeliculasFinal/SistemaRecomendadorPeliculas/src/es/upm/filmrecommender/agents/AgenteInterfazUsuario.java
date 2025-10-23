package es.upm.filmrecommender.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import jade.core.behaviours.TickerBehaviour;

import javax.swing.SwingUtilities;
import es.upm.filmrecommender.gui.RecommenderGui;
import es.upm.filmrecommender.data.Movie;
import es.upm.filmrecommender.data.UserPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonSyntaxException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class AgenteInterfazUsuario extends Agent {

    private RecommenderGui myGui;
    private AID recommenderAgent;
    private Gson gson;

    public static final String CONVERSATION_ID_RECOMMENDATION_QUERY = "recommendation-request-conversation";

    protected void setup() {
        System.out.println("Agente InterfazUsuario " + getAID().getLocalName() + " iniciado.");
        gson = new Gson();

        SwingUtilities.invokeLater(() -> {
            myGui = new RecommenderGui(AgenteInterfazUsuario.this);
            myGui.setVisible(true);
        });

        addBehaviour(new TickerBehaviour(this, 5000) {
            protected void onTick() {
                if (recommenderAgent == null) {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType(AgenteRecomendador.SERVICE_TYPE_RECOMMENDER);
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        if (result.length > 0) {
                            recommenderAgent = result[0].getName();
                            System.out.println(getLocalName() + ": Encontrado AgenteRecomendador: " + recommenderAgent.getLocalName());
                            if(myGui != null) myGui.mostrarResultados("INFO: Conectado al servicio de recomendación.");
                            stop(); 
                        } else {
                            System.out.println(getLocalName() + ": No se encontró AgenteRecomendador. Reintentando...");
                            if(myGui != null) myGui.mostrarResultados("INFO: Buscando servicio de recomendación...");
                        }
                    } catch (FIPAException fe) { fe.printStackTrace(); }
                }
            }
        });
        

    }
    
    public void solicitarRecomendacionPorClima() {
        if (recommenderAgent == null) {
            myGui.mostrarResultados("ERROR: Servicio de recomendación no disponible.");
            return;
        }

        double minRating = myGui.getSelectedMinimumRating();
        UserPreferences prefs = new UserPreferences("", minRating);
        String json = gson.toJson(prefs);

        myGui.limpiarResultados();
        myGui.mostrarResultados("Consultando el clima actual en Madrid...");
        String clima = es.upm.filmrecommender.utils.ClimaClient.obtenerFraseClimaMadrid();
        myGui.setUltimaFraseClima(clima);
        myGui.mostrarResultados(clima + "\n");

        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.addReceiver(recommenderAgent);
        request.setOntology("recommender-ontology");
        request.setConversationId("weather-based-recommendation");
        request.setContent(json);
        request.setReplyWith("req_clima_" + System.currentTimeMillis());

        addBehaviour(new AchieveREInitiator(this, request) {
            @Override
            protected void handleInform(ACLMessage inform) {
                if ("weather-based-recommendation".equals(inform.getConversationId())) {
                    String jsonRecommendedMovies = inform.getContent();
                    try {
                        Type movieListType = new TypeToken<ArrayList<Movie>>() {}.getType();
                        List<Movie> recommendedMovies = gson.fromJson(jsonRecommendedMovies, movieListType);

                        if (recommendedMovies != null && !recommendedMovies.isEmpty()) {
                            myGui.mostrarPeliculas(recommendedMovies);
                        } else {
                            myGui.mostrarResultados("No se encontraron recomendaciones para el clima actual.");
                        }
                    } catch (Exception e) {
                        myGui.mostrarResultados("Error procesando películas por clima: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }

            @Override
            protected void handleFailure(ACLMessage failure) {
                myGui.mostrarResultados("ERROR: No se pudieron obtener recomendaciones por clima.");
            }
        });

        send(request);
        System.out.println(getLocalName() + ": Solicitud enviada al recomendador para recomendación por clima.");
    }



    protected void takeDown() {
        if (myGui != null) {
            SwingUtilities.invokeLater(() -> myGui.dispose());
        }
        System.out.println("Agente InterfazUsuario " + getAID().getLocalName() + " terminando.");
    }

    public void solicitarRecomendaciones(String genero) {
        if (myGui == null) {
            System.err.println(getLocalName() + ": GUI no inicializada. No se puede enviar la solicitud.");
            return;
        }
        if (recommenderAgent == null) {
            myGui.mostrarResultados("ERROR: El servicio de recomendación no está disponible aún. Inténtalo más tarde.");
            System.err.println(getLocalName() + ": AgenteRecomendador no encontrado. No se puede enviar la solicitud.");
            return;
        }

        double minRating = myGui.getSelectedMinimumRating();

        myGui.limpiarResultados();
        myGui.resetearClima();

        String infoMsg = "INFO: Solicitando recomendaciones para el género: \"" + (genero.isEmpty() ? "Cualquiera (Populares)" : genero) + "\"";
        if (minRating > 0) {
            infoMsg += " con valoración mínima de: " + String.format("%.1f", minRating);
        }
        myGui.mostrarResultados(infoMsg + "...");


        UserPreferences preferences = new UserPreferences(genero, minRating);
        String jsonPreferences = gson.toJson(preferences);

        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.addReceiver(recommenderAgent);
        request.setOntology(AgenteRecomendador.ONTOLOGY_RECOMMENDATION_QUERY);
        request.setContent(jsonPreferences);
        request.setConversationId(CONVERSATION_ID_RECOMMENDATION_QUERY);
        request.setReplyWith("req_reco_" + System.currentTimeMillis());

        System.out.println(getLocalName() + ": Enviando solicitud de recomendación al RA. Contenido JSON: " + jsonPreferences);

        addBehaviour(new AchieveREInitiator(this, request) {
            @Override
            protected void handleInform(ACLMessage inform) {
                System.out.println(myAgent.getLocalName() + ": Recomendaciones recibidas de " + inform.getSender().getLocalName());
                String jsonRecommendedMovies = inform.getContent();
           

                try {
                    Type movieListType = new TypeToken<ArrayList<Movie>>() {}.getType();
                    List<Movie> recommendedMovies = gson.fromJson(jsonRecommendedMovies, movieListType);

                    if (recommendedMovies != null && !recommendedMovies.isEmpty()) {
                        myGui.mostrarPeliculas(recommendedMovies);
                    } else {
                        myGui.mostrarResultados("No se encontraron recomendaciones que cumplan los criterios.");
                    }
                } catch (JsonSyntaxException e) {
                    myGui.mostrarResultados("Error: Respuesta de recomendaciones malformada.");
                    System.err.println(myAgent.getLocalName() + ": Error parseando JSON de recomendaciones: " + e.getMessage());
                    System.err.println(myAgent.getLocalName() + ": JSON Recibido: " + jsonRecommendedMovies);
                } catch (Exception e) {
                    myGui.mostrarResultados("Error inesperado al procesar las recomendaciones: " + e.getMessage());
                    e.printStackTrace();
                }

            }

            @Override
            protected void handleFailure(ACLMessage failure) {
                System.err.println(myAgent.getLocalName() + ": El AgenteRecomendador reportó FAILURE: " + failure.getContent());
                myGui.mostrarResultados("ERROR: No se pudieron obtener recomendaciones: " + failure.getContent());
            }

            @Override
            protected void handleAllResultNotifications(Vector resultNotifications) {
                if (resultNotifications.isEmpty()) {
                    System.err.println(myAgent.getLocalName() + ": No se recibió ninguna respuesta del servicio de recomendación (posible timeout).");
                    myGui.mostrarResultados("ADVERTENCIA: No se recibió respuesta del servicio de recomendación (posible timeout).");
                }
            }
        });
    }
}