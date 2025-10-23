package es.upm.filmrecommender;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class MainLauncher {
    public static void main(String[] args) {
        Runtime rt = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "true");
        AgentContainer mainContainer = rt.createMainContainer(profile);

        try {
            AgentController adquisicion = mainContainer.createNewAgent(
                "adquisicion",
                es.upm.filmrecommender.agents.AgenteAdquisicionDatosPeliculas.class.getName(),
                null
            );
            adquisicion.start();

            AgentController inteligente = mainContainer.createNewAgent(
                "inteligente",
                es.upm.filmrecommender.agents.AgenteInteligente.class.getName(),
                null
            );
            inteligente.start();

            AgentController recomendador = mainContainer.createNewAgent(
                "recomendador",
                es.upm.filmrecommender.agents.AgenteRecomendador.class.getName(),
                null
            );
            recomendador.start();

            AgentController interfaz = mainContainer.createNewAgent(
                "interfaz",
                es.upm.filmrecommender.agents.AgenteInterfazUsuario.class.getName(),
                null
            );
            interfaz.start();

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}