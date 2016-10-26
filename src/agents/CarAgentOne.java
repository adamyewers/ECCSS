package agents;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.AID;
import java.util.Vector;

import java.util.*;

public class CarAgentOne extends Agent {
	//Agent variables go here
	
	protected void setup() {
		// Printout a start up message
		System.out.println("Car Agent One is ready.");
		
		// Register this agent in the yellow pages (df agent) so that it can be found by the master agent
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Car-charging");
		sd.setName(getLocalName()+"-Car-charging");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
			System.out.print("Exception thrown and caught");
		}
	}
	
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Car Agent One terminated.");
		
		// Unregister from the yellow pages (df agent)
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}
	
	//Behaviors go here
}