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

public class MasterSchedulingAgent extends Agent {
	//Agent variables go here
	private Vector<AID> carAgents = new Vector<AID>();
	
	protected void setup() {
		// Printout a start up message
		System.out.println("Master Scheduling Agent is ready.");
		addBehaviour(new ExampleBehaviour(this));
		addBehaviour(new DiscoverCarAgents());
	}
	
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Master Scheduling Agent terminated.");
	}
	
	
	//Behaviors go here
	public class ExampleBehaviour extends TickerBehaviour {
		int tick = 1;
		
		private ExampleBehaviour(Agent a) {
			super(a, 2000);
		}
		
		public void onTick() {
			System.out.println("Tick #" + tick);
			tick++;
		}
	}
	
	//This behavior discovers car agents in the yellow pages (df agent) and adds them to its list of known car agents
	public class DiscoverCarAgents extends OneShotBehaviour {
		public void action() {
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("Car-charging");
			template.addServices(sd);
			try {
				DFAgentDescription[] result = DFService.search(myAgent, template);
				carAgents.clear();
				for (int i = 0; i < result.length; ++i) {
					carAgents.addElement(result[i].getName());
					System.out.println("Adding " + carAgents.elementAt(i).getName() + " to Master Scheduling Agent");
				}
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
				System.out.print("Exception thrown and caught");
			}
		}
	}
}