package es.upm.filmrecommender.agents;

import jade.core.Agent;

public class AgenteHolaMundo extends Agent {

	protected void setup() {
		
		System.out.println("Hola Mundo, agente :" + getAID().getName());
		System.out.println("Argumentos: ");
		Object[] args = getArguments();
		
		if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; ++i) {
                System.out.println("- " + args[i]);
            }
        } else {
            System.out.println("No me pasaron argumentos.");
        }
		
        doDelete(); 
		
		
		
	}
	 protected void takeDown() {
	        
	        System.out.println("Agente " + getAID().getName() + " terminando.");
	    }
}
		

