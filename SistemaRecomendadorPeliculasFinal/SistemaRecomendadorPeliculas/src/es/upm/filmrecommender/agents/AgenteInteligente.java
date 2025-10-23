package es.upm.filmrecommender.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;
import java.util.Map;

public class AgenteInteligente extends Agent {

    private Map<String, Integer> genreRequestCount = new HashMap<>();

    protected void setup() {
        System.out.println(getLocalName() + ": Agente Inteligente iniciado.");

        
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("intelligent-agent-service");
        sd.setName("JADE-intelligent-agent");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            System.out.println(getLocalName() + ": Registrado en el DF como intelligent-agent-service");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        addBehaviour(new CyclicBehaviour() {
            public void action() {
                MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("weather-based-recommendation")
                );
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    System.out.println(getLocalName() + ": Petición recibida para recomendación basada en clima.");

                    String weather = es.upm.filmrecommender.utils.ClimaClient.obtenerTipoClimaMadrid();
                    System.out.println(getLocalName() + ": Clima en Madrid: " + weather);

                    String weatherType = weather.trim();
                    if (weatherType.contains(" ")) {
                        weatherType = weatherType.substring(weatherType.lastIndexOf(" ") + 1);
                    }
                    System.out.println(getLocalName() + ": Tipo de clima detectado: " + weatherType);


                    String generos;
                    switch (weatherType.toLowerCase()) {
                        case "clear":
                            generos = "Comedia, Aventura, Acción";
                            break;
                        case "clouds":
                            generos = "Drama, Documental, Historia";
                            break;
                        case "rain":
                            generos = "Romance, Drama, Ciencia ficción";
                            break;
                        case "drizzle":
                            generos = "Romance, Misterio, Drama";
                            break;
                        case "thunderstorm":
                            generos = "Suspense, Terror, Crimen";
                            break;
                        case "snow":
                            generos = "Animación, Fantasía, Familia";
                            break;
                        case "mist":
                        case "fog":
                        case "haze":
                        case "smoke":
                        case "dust":
                        case "ash":
                        case "sand":
                            generos = "Misterio, Suspense, Crimen";
                            break;
                        case "squall":
                            generos = "Acción, Bélica";
                            break;
                        case "tornado":
                            generos = "Acción, Ciencia ficción, Película de TV";
                            break;
                        default:
                            generos = "Drama";
                            break;
                    }
                    
                    System.out.println(getLocalName() + ": Géneros asociados: " + generos);


                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(generos);
                    send(reply);
                } else {
                    block();
                }
            }
        });


        addBehaviour(new CyclicBehaviour() {
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);

                if (msg != null) {
                	String receivedGenre = (msg.getContent() != null) ? msg.getContent().trim().toLowerCase() : "";


                    if (!receivedGenre.isEmpty()) {
                        genreRequestCount.merge(receivedGenre, 1, Integer::sum);
                        System.out.println(getLocalName() + ": Contador actualizado para género '" + receivedGenre + "': " + genreRequestCount.get(receivedGenre));
                    }

                    String favoriteGenre = genreRequestCount.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("comedia");

                    System.out.println(getLocalName() + ": Género favorito sugerido: " + favoriteGenre);


                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(favoriteGenre);
                    send(reply);
                } else {
                    block();
                }
            }
        });
    }
    


}

