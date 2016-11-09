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

import agents.MasterSchedulingAgent.ExampleBehaviour;

import java.util.*;

public class CarAgentOne extends Agent {
	//Agent variables go here
	boolean carCharging;
	int currentBattery, maxBattery, minBattery, chargeRate;
	private AID masterAgent;
	
	protected void setup() {
		// Printout a start up message
		System.out.println("Car Agent One is ready.");
		
		//Initialize variables
		currentBattery = 100;
		
		//Add behaviors
		addBehaviour(new Driving(this));
		addBehaviour(new RequestCharge());
		addBehaviour(new DiscoverMasterAgent());
		
		// Register this agent in the yellow pages (df agent) so that it can be found by the master agent
		DFAgentDescription postCarAgent = new DFAgentDescription();
		postCarAgent.setName(getAID());
		ServiceDescription postCarService = new ServiceDescription();
		postCarService.setType("discover-car-agents");
		postCarService.setName(getLocalName()+"-discover-car-agents");
		postCarAgent.addServices(postCarService);
		try {
			DFService.register(this, postCarAgent);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
			System.out.print("Could not make " + this.getLocalName() + " discoverable");
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
	public class DiscoverMasterAgent extends OneShotBehaviour {
		public void action() {
			//Discover and add the master scheduling agent
			DFAgentDescription findMasterAgent = new DFAgentDescription();
			ServiceDescription findMasterService = new ServiceDescription();
			findMasterService.setType("discover-master-agent");
			findMasterAgent.addServices(findMasterService);
			
			try {
				DFAgentDescription[] result = DFService.search(myAgent, findMasterAgent);
				for (int i = 0; i < result.length; ++i) {
					masterAgent = result[0].getName();
					System.out.println(myAgent.getName() + " has found " + masterAgent.getName());
				}
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
				System.out.print(myAgent.getName()+ ": Could not find Master Agent");
			}		
		}
	}
	
	public class Driving extends TickerBehaviour {
		public Driving(Agent a) {
			super(a, 10000);
		}
		
		public void onTick() {
			if (!carCharging) {
				currentBattery--;
				System.out.println("Battery  level: " + currentBattery);
			}			
		}
	}
	
	public class Charging extends TickerBehaviour {
		public Charging(Agent a) {
			super(a, 10000);
		}
		
		public void onTick() {
			if (carCharging) {
				if (currentBattery < maxBattery)
				{
					currentBattery++;
					System.out.println("Battery level: " + currentBattery);
				}
				else
				{
					carCharging = false;
					System.out.println("Battery is fully charged");
				}
			}			
		}
	}
		
	public class RequestCharge extends CyclicBehaviour {
		public void action() {
			//Create new message
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(masterAgent);
			msg.setLanguage("English");
			msg.setOntology("test-ontology");
			msg.setContent("Test message.");		
			//Send message
			send(msg);
		}
	}
	
	//Add check if charge requirec cyclice which will call request when condition is met
	//if battery < threshold then send message to master agent
}