package es.upm.filmrecommender.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonSyntaxException;
import es.upm.filmrecommender.data.Movie;
import es.upm.filmrecommender.data.UserPreferences;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class AgenteRecomendador extends Agent {

    private Gson gson;
    private AID mdaaAgent;
    private AID agenteInteligente;


    public static final String SERVICE_TYPE_RECOMMENDER = "recommendation-service";
    public static final String ONTOLOGY_RECOMMENDATION_QUERY = "recommender-ontology";
    public static final String CONVERSATION_ID_MOVIE_QUERY = "movie-query-conversation";

    protected void setup() {
        System.out.println("Agente Recomendador " + getAID().getLocalName() + " iniciado.");
        gson = new Gson();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(SERVICE_TYPE_RECOMMENDER);
        sd.setName("JADE-movie-recommender");
        sd.addOntologies(ONTOLOGY_RECOMMENDATION_QUERY);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            System.out.println("Agente " + getLocalName() + " registró el servicio: " + SERVICE_TYPE_RECOMMENDER);
        } catch (FIPAException fe) { fe.printStackTrace(); }

        addBehaviour(new TickerBehaviour(this, 10000) {
        	protected void onTick() {
        	    if (mdaaAgent == null) {
        	        DFAgentDescription template = new DFAgentDescription();
        	        ServiceDescription sd_mdaa = new ServiceDescription();
        	        sd_mdaa.setType(AgenteAdquisicionDatosPeliculas.SERVICE_TYPE);
        	        template.addServices(sd_mdaa);
        	        try {
        	            DFAgentDescription[] result = DFService.search(myAgent, template);
        	            if (result.length > 0) {
        	                mdaaAgent = result[0].getName();
        	                System.out.println(getLocalName() + ": Encontrado AgenteAdquisicionDatosPeliculas: " + mdaaAgent.getLocalName());
        	            }
        	        } catch (FIPAException fe) { fe.printStackTrace(); }
        	    }

        	    if (agenteInteligente == null) {
        	        DFAgentDescription template = new DFAgentDescription();
        	        ServiceDescription sd_ai = new ServiceDescription();
        	        sd_ai.setType("intelligent-agent-service");
        	        template.addServices(sd_ai);
        	        try {
        	            DFAgentDescription[] result = DFService.search(myAgent, template);
        	            if (result.length > 0) {
        	                agenteInteligente = result[0].getName();
        	                System.out.println(getLocalName() + ": Encontrado AgenteInteligente: " + agenteInteligente.getLocalName());
        	            }
        	        } catch (FIPAException fe) { fe.printStackTrace(); }
        	    }

        	    if (mdaaAgent != null && agenteInteligente != null) {
        	        stop();
        	    }
        	}

        });
        addBehaviour(new RecommendationServerBehaviour());
    }

    protected void takeDown() {
        try { DFService.deregister(this); } catch (FIPAException fe) { fe.printStackTrace(); }
        System.out.println("Agente Recomendador " + getAID().getLocalName() + " terminando.");
    }

    private class RecommendationServerBehaviour extends CyclicBehaviour {
    	public void action() {
    	    MessageTemplate mt = MessageTemplate.and(
    	        MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
    	        MessageTemplate.MatchOntology(ONTOLOGY_RECOMMENDATION_QUERY)
    	    );
    	    ACLMessage msgFromUIA = myAgent.receive(mt);

    	    if (msgFromUIA != null) {
    	        String jsonRequestFromUIA = msgFromUIA.getContent();
    	        
    	        if ("weather-based-recommendation".equals(msgFromUIA.getConversationId())) {
    	            System.out.println(getLocalName() + ": Solicitud de recomendación por clima recibida.");

    	            UserPreferences climaPrefs;
    	            try {
    	                climaPrefs = gson.fromJson(msgFromUIA.getContent(), UserPreferences.class);
    	            } catch (JsonSyntaxException e) {
    	                System.err.println(getLocalName() + ": Error parseando UserPreferences para clima: " + e.getMessage());
    	                return;
    	            }

    	            double minRating = climaPrefs.getMinimumRating();

    	            ACLMessage reqToAI = new ACLMessage(ACLMessage.REQUEST);
    	            reqToAI.addReceiver(agenteInteligente);
    	            reqToAI.setConversationId("weather-based-recommendation");

    	            addBehaviour(new AchieveREInitiator(myAgent, reqToAI) {
    	                @Override
    	                protected void handleInform(ACLMessage inform) {
    	                    String genreList = inform.getContent();
    	                    System.out.println(getLocalName() + ": Géneros recomendados por clima: " + genreList);

    	                    UserPreferences prefs = new UserPreferences(genreList, minRating);
    	                    continuarConRecomendacion(prefs, msgFromUIA);
    	                }

    	                @Override
    	                protected void handleFailure(ACLMessage failure) {
    	                    ACLMessage reply = msgFromUIA.createReply();
    	                    reply.setPerformative(ACLMessage.FAILURE);
    	                    reply.setContent("Error al obtener recomendación basada en clima.");
    	                    send(reply);
    	                }
    	            });

    	            return;
    	        }


    	        
    	        System.out.println(getLocalName() + ": Solicitud de recomendación recibida de " + msgFromUIA.getSender().getLocalName() +
    	                           ". Contenido JSON: " + jsonRequestFromUIA);

    	        UserPreferences preferencesForMDAA;
    	        try {
    	            preferencesForMDAA = gson.fromJson(jsonRequestFromUIA, UserPreferences.class);
    	            if (preferencesForMDAA == null) {
    	                throw new JsonSyntaxException("Contenido JSON inválido o nulo.");
    	            }
    	        } catch (JsonSyntaxException e) {
    	            System.err.println(getLocalName() + ": Error parseando UserPreferences del UIA: " + e.getMessage());
    	            ACLMessage errorReply = msgFromUIA.createReply();
    	            errorReply.setPerformative(ACLMessage.FAILURE);
    	            errorReply.setContent("Formato de solicitud incorrecto desde UIA.");
    	            myAgent.send(errorReply);
    	            return;
    	        }

    	        String receivedGenre = preferencesForMDAA.getGenre();
    	        ACLMessage requestToAI = new ACLMessage(ACLMessage.REQUEST);
    	        requestToAI.addReceiver(agenteInteligente);
    	        requestToAI.setContent(receivedGenre == null ? "" : receivedGenre);
    	        requestToAI.setConversationId("genre-analysis");

    	        myAgent.addBehaviour(new AchieveREInitiator(myAgent, requestToAI) {
    	            @Override
    	            protected void handleInform(ACLMessage inform) {
    	                String suggestedGenre = inform.getContent();
    	                System.out.println(getLocalName() + ": AgenteInteligente sugirió el género: " + suggestedGenre);

    	                UserPreferences updatedPreferences = new UserPreferences(suggestedGenre, preferencesForMDAA.getMinimumRating());

    	                continuarConRecomendacion(updatedPreferences, msgFromUIA);
    	            }
    	        });
    	        
    	        

    	    } else {
    	        block();
    	    }
    	}
    	
    	private void continuarConRecomendacion(UserPreferences preferencesForMDAA, ACLMessage originalRequestFromUIA) {
    	    if (mdaaAgent == null) {
    	        System.out.println(getLocalName() + ": MDAA no disponible. No se puede procesar la solicitud.");
    	        ACLMessage reply = originalRequestFromUIA.createReply();
    	        reply.setPerformative(ACLMessage.FAILURE);
    	        reply.setContent("Servicio de datos de películas no disponible temporalmente.");
    	        myAgent.send(reply);
    	        return;
    	    }

    	    String updatedJsonRequest = gson.toJson(preferencesForMDAA);
    	    System.out.println(getLocalName() + ": Solicitando películas al MDAA con preferencias JSON: " + updatedJsonRequest);

    	    ACLMessage requestToMDAA = new ACLMessage(ACLMessage.REQUEST);
    	    requestToMDAA.addReceiver(mdaaAgent);
    	    requestToMDAA.setOntology(AgenteAdquisicionDatosPeliculas.ONTOLOGY_MOVIE_QUERY);
    	    requestToMDAA.setContent(updatedJsonRequest);
    	    requestToMDAA.setConversationId(CONVERSATION_ID_MOVIE_QUERY);
    	    requestToMDAA.setReplyWith("req_mdaa_" + System.currentTimeMillis());

    	    myAgent.addBehaviour(new AchieveREInitiator(myAgent, requestToMDAA) {
    	        @Override
    	        protected void handleInform(ACLMessage inform) {
    	            System.out.println(myAgent.getLocalName() + ": Respuesta INFORM recibida del MDAA: " + inform.getSender().getLocalName());
    	            String jsonMoviesFromMDAA = inform.getContent();
    	            System.out.println(getLocalName() + ": JSON completo recibido del MDAA:\n" + jsonMoviesFromMDAA);

    	            List<Movie> moviesFromMDAA = new ArrayList<>();
    	            try {
    	                Type movieListType = new TypeToken<ArrayList<Movie>>() {}.getType();
    	                moviesFromMDAA = gson.fromJson(jsonMoviesFromMDAA, movieListType);
    	                if (moviesFromMDAA == null) moviesFromMDAA = new ArrayList<>();
    	            } catch (JsonSyntaxException e) {
    	                ACLMessage replyToUIA = originalRequestFromUIA.createReply();
    	                replyToUIA.setPerformative(ACLMessage.FAILURE);
    	                replyToUIA.setContent("Error interno: Datos de películas malformados.");
    	                myAgent.send(replyToUIA);
    	                return;
    	            }

    	            ACLMessage replyToUIA = originalRequestFromUIA.createReply();
    	            if (!moviesFromMDAA.isEmpty()) {
    	            	
    	            	if (moviesFromMDAA.size() > 5) {
    	            	    moviesFromMDAA = moviesFromMDAA.subList(0, 5);
    	            	}

    	                replyToUIA.setPerformative(ACLMessage.INFORM);
    	                String jsonRecommendedMovies = gson.toJson(moviesFromMDAA);
    	                replyToUIA.setContent(jsonRecommendedMovies);
    	            } else {
    	                replyToUIA.setPerformative(ACLMessage.FAILURE);
    	                replyToUIA.setContent("No se encontraron recomendaciones para el género: " + preferencesForMDAA.getGenre());
    	            }
    	            myAgent.send(replyToUIA);
    	        }

    	        @Override
    	        protected void handleFailure(ACLMessage failure) {
    	            ACLMessage replyToUIA = originalRequestFromUIA.createReply();
    	            replyToUIA.setPerformative(ACLMessage.FAILURE);
    	            replyToUIA.setContent("Error al obtener datos de películas: " + failure.getContent());
    	            myAgent.send(replyToUIA);
    	        }

    	        @Override
    	        protected void handleAllResultNotifications(Vector resultNotifications) {
    	            if (resultNotifications.isEmpty()) {
    	                ACLMessage replyToUIA = originalRequestFromUIA.createReply();
    	                replyToUIA.setPerformative(ACLMessage.FAILURE);
    	                replyToUIA.setContent("El servicio de datos de películas no respondió a tiempo.");
    	                myAgent.send(replyToUIA);
    	            }
    	        }
    	    });
    	}


    }
}