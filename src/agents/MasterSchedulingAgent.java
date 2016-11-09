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
	//------------------ Master Agent encapsulated variables ------------------
	private Vector<AID> carAgents = new Vector<AID>();

	//masterSchedule variable - data type?

	private int currentChargingAgentIndex;

	
	//------------------- Setup -----------------------
	protected void setup() {
		// Printout a start up message
		System.out.println("Master Scheduling Agent is ready.");
		addBehaviour(new ExampleBehaviour(this));
		addBehaviour(new DiscoverCarAgents());
		addBehaviour(new ReceiveChargeRequest());
		takeDown();
		
		//Register the master agent in the yellow pages (df agent) so that it can be found by the car agents
		DFAgentDescription postMasterAgent = new DFAgentDescription();
		postMasterAgent.setName(getAID());
		ServiceDescription postMasterService = new ServiceDescription();
		postMasterService.setType("discover-master-agent");
		postMasterService.setName(getLocalName()+"-discover-master-agent");
		postMasterAgent.addServices(postMasterService);
		try {
			DFService.register(this, postMasterAgent);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
			System.out.print("Could not make " + this.getLocalName() + " discoverable.");
		}
	}
	
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Master Scheduling Agent terminated.");
	}

	//------------------ Behaviors ---------------------
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
			DFAgentDescription findCarAgents = new DFAgentDescription();
			ServiceDescription findCarService = new ServiceDescription();
			findCarService.setType("discover-car-agents");
			findCarAgents.addServices(findCarService);
			try {
				DFAgentDescription[] result = DFService.search(myAgent, findCarAgents);
				carAgents.clear();
				for (int i = 0; i < result.length; ++i) {
					carAgents.addElement(result[i].getName());
					System.out.println("Adding " + carAgents.elementAt(i).getName() + " to Master Scheduling Agent");
				}
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
				System.out.print("Could not add car agents to master agent.");
			}
		}
	}

	
	public class ReceiveChargeRequest extends CyclicBehaviour {
		public void action() {
			//check if a schedule has been requested, if so process it
			ACLMessage msg = receive();
			if (msg != null) {
				// Process the message
				System.out.print(msg.getContent());
			}
		}
	}
	
	
	//--------------------------------------------
	// Function: CalcPriotity(int)
	// Desc.: Calculate the priority of a car
	// Inputs: i - the index of the car 
	// 	in carAgents AID
	//
	// Outputs: priority - int
	//-------------------------------------------
	private double CalcPriority(int i)
	{
		double priority;
		int battLevel; // A number between 0 and 100
		float timeToLeave;
		bool isHybrid;
		
		battLevel = carAgents.elementAt(i).getBatteryLevel();
		
		timeToLeave = carAgents.elementAt(i).getHoursToLeave();
		
		isHybrid = carAgents.elementAt(i).IsHybrid;
		
		priority = (100-battLevel)*timeToLeave;
		
		if(isHybrid)
			priority = priority * 0.9; // Priority factor reduced for hybrid cars
		
		return priority;
	}
	
	//--------------------------------------------
	// Function: ProcessRequest
	// Desc.: Calculate the priority of a car
	// Inputs: i - the index of the car 
	// 	in carAgents AID
	//
	// Outputs: priority - int
	//-------------------------------------------
	private void ProcessRequest()
	{
		int total = carAgents.size();
		for(int i = 0; i < total; i++)
		{
			AID x = carAgents.elementAt(i);
			if(x.requireCharge == true)
			{
				double newPriority = calcPriority(i);
				double currentPriority = calcPriority(currentChargingAgentIndex);
				if(priority > currentChargingAgent.Priority)
				{
					Charge(x);
					currentChargingIndex = i;
				}
			}
		}
	}
}