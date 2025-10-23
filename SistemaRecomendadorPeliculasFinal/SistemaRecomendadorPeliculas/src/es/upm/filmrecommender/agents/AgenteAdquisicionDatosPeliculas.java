package es.upm.filmrecommender.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException; 
import es.upm.filmrecommender.data.Movie;
import es.upm.filmrecommender.data.UserPreferences; 
import es.upm.filmrecommender.utils.TMDBApiClient;

import java.util.List;
import java.util.ArrayList;

public class AgenteAdquisicionDatosPeliculas extends Agent {

    private TMDBApiClient apiClient;
    private Gson gson;

    public static final String SERVICE_TYPE = "movie-data-acquisition";
    public static final String ONTOLOGY_MOVIE_QUERY = "movie-query-ontology";

    protected void setup() {
        System.out.println("Agente " + getAID().getLocalName() + " iniciado.");
        apiClient = new TMDBApiClient();
        gson = new Gson();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(SERVICE_TYPE);
        sd.setName("JADE-movie-database-service");
        sd.addOntologies(ONTOLOGY_MOVIE_QUERY);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            System.out.println("Agente " + getLocalName() + " registró el servicio: " + SERVICE_TYPE);
        } catch (FIPAException fe) {
            System.err.println("Agente " + getLocalName() + " falló al registrar el servicio: " + fe.getMessage());
            fe.printStackTrace();
        }
        addBehaviour(new MovieDataServerBehaviour());
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println("Agente " + getLocalName() + " desregistrado del DF.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Agente " + getAID().getLocalName() + " terminando.");
    }

    private class MovieDataServerBehaviour extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchOntology(ONTOLOGY_MOVIE_QUERY)
            );
            ACLMessage msg = myAgent.blockingReceive(mt);

            if (msg != null) {
                String jsonRequestContent = msg.getContent();
                System.out.println("Agente " + myAgent.getLocalName() + " recibió de " + msg.getSender().getLocalName() +
                                   ". Contenido JSON: \"" + jsonRequestContent + "\", Ontología: " + msg.getOntology());
                
                ACLMessage reply = msg.createReply();
                UserPreferences preferences;
                List<Movie> movies = new ArrayList<>();
                int maxResults = 20; 

                try {
                    preferences = gson.fromJson(jsonRequestContent, UserPreferences.class);
                    if (preferences == null) {
                        throw new JsonSyntaxException("El contenido JSON no pudo ser parseado a UserPreferences o es nulo.");
                    }

                    String genre = preferences.getGenre();
                    double minRating = preferences.getMinimumRating();

                    if (genre != null && genre.trim().equalsIgnoreCase("populares")) {
                        movies = apiClient.getPopularMovies(minRating, maxResults);
                    } else if (genre != null && !genre.trim().isEmpty()) {
                        String[] generosSeparados = genre.split(",");
                        List<Movie> acumuladas = new ArrayList<>();
                        for (String g : generosSeparados) {
                            String trimmed = g.trim();
                            if (!trimmed.isEmpty()) {
                                List<Movie> encontradas = apiClient.searchMoviesByGenre(trimmed, minRating, maxResults);
                                for (Movie m : encontradas) {
                                    if (acumuladas.size() < 5 && !acumuladas.contains(m)) {
                                        acumuladas.add(m);
                                    }
                                }
                            }
                            if (acumuladas.size() >= 5) break;
                        }
                        movies = acumuladas;
                    }
                    else { 
                         System.out.println("Agente " + myAgent.getLocalName() + ": Género no especificado en preferencias, obteniendo populares con rating >= " + minRating);
                        movies = apiClient.getPopularMovies(minRating, maxResults);
                    }

                    if (movies != null && !movies.isEmpty()) {
                        reply.setPerformative(ACLMessage.INFORM);
                        String jsonMovies = gson.toJson(movies);
                        System.out.println("DEBUG MDAA: Enviando JSON: " + jsonMovies.substring(0, Math.min(jsonMovies.length(), 200)) + "...");
                        reply.setContent(jsonMovies);
                        System.out.println("Agente " + myAgent.getLocalName() + " enviando JSON (" + movies.size() + " películas) a " + msg.getSender().getLocalName());
                    } else {
                        reply.setPerformative(ACLMessage.FAILURE);
                        String failureMsg = "No se encontraron películas para los criterios: Género='" + genre + "', Rating >=" + minRating;
                        reply.setContent(failureMsg);
                        System.out.println("Agente " + myAgent.getLocalName() + ": " + failureMsg);
                    }
                } catch (JsonSyntaxException jsonEx) {
                    System.err.println("Agente " + myAgent.getLocalName() + ": Error parseando JSON de preferencias: " + jsonRequestContent + ". Error: " + jsonEx.getMessage());
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("Error: Formato de solicitud de preferencias incorrecto.");
                } catch (Exception e) {
                    System.err.println("Agente " + myAgent.getLocalName() + ": Error crítico procesando solicitud: " + e.getMessage());
                    e.printStackTrace();
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("Error interno del servidor de datos: " + e.getMessage());
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }
}