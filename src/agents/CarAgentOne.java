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

//import agents.MasterSchedulingAgent.ExampleBehaviour;

import java.util.*;

public class CarAgentOne extends Agent {
	//Agent variables go here
	String carState;
	int currentBattery, maxBattery, minBattery, chargeRate, drainRate;
	boolean isHybrid, isWaiting;
	int waitTime, currentTime, dayOfWeek;
	int[] leaveTime = new int[7];
	int[] homeTime = new int[7];
	private AID masterAgent;
	
	protected void setup() {
		// Printout a start up message
		System.out.println("Car Agent One is ready.");
		
		//Initialize variables
		carState = "idle";
		
		currentBattery = 100;
		maxBattery = 100;
		minBattery = 20;
		chargeRate = 20;
		drainRate = 5;
		isHybrid = false;
		
		currentTime = 0;
		dayOfWeek = 0;
		waitTime = 0;
		isWaiting = false;
		
		//The times that this agent leaves the house each day with a random variation to simulate real life
		leaveTime[0] = 7 + Randomise(1, 2);
		leaveTime[1] = 7 + Randomise(1, 2);
		leaveTime[2] = 7 + Randomise(1, 2);
		leaveTime[3] = 7 + Randomise(1, 2);
		leaveTime[4] = 10 + Randomise(1, 2);
		leaveTime[5] = 0;
		leaveTime[6] = 0;
		
		homeTime[0] = 17 + Randomise(1, 2);
		homeTime[1] = 17 + Randomise(1, 2);
		homeTime[2] = 20 + Randomise(1, 2);
		homeTime[3] = 17 + Randomise(1, 2);
		homeTime[4] = 14 + Randomise(1, 2);
		homeTime[5] = 0;
		homeTime[6] = 0;
	
		//Add behaviors
		addBehaviour(new DiscoverMasterAgent());
		addBehaviour(new ChangeDay(this));
		addBehaviour(new Idle(this));
		addBehaviour(new Driving(this));
		addBehaviour(new Charging(this));
		addBehaviour(new ReceiveRequestResult());
		addBehaviour(new StopCharge());
		addBehaviour(new SendInformation());
		addBehaviour(new BatteryDead());
		
		//Post the car agent to the blackboard for discovery
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
		
		System.out.print("Day: " + Integer.toString((dayOfWeek + 1)) + "\n");
	}
		
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Car Agent One terminated.\n");
		
		// Unregister from the yellow pages (df agent)
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}
	
	//Agent Functions
	public int Randomise(int min, int max) {
		Random rand = new Random();
		int randInt = rand.nextInt(max) + min;	
		return randInt;
	}
	
	//Behaviors go here	
	//Adds 1 hour to the time every second to simulate a 24 hour day and a 7 day week
	public class ChangeDay extends TickerBehaviour {
		public ChangeDay(Agent a) {
			super(a, 1000);
		}
		
		public void onTick() {
			currentTime++;
			if (currentTime == 24)	{
				if (dayOfWeek < 7) {
					dayOfWeek++;
					System.out.print("Day: " + Integer.toString((dayOfWeek + 1)) + "\n");
				}
				else {
					dayOfWeek = 0;
					System.out.print("Day: " + Integer.toString((dayOfWeek + 1)) + "\n");
				}
				currentTime = 0;
			}
			System.out.print(currentTime + ":00, " + currentBattery + "% Battery\n");
			if (waitTime != 0) {
				System.out.println("Total waiting: " + waitTime + " hours\n");
			}
		}
	}
	
	//Finds the master agent and stores it in local variable
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
	
	//The agent is in this state when it waiting to charge and when it is sitting at home not charging
	public class Idle extends TickerBehaviour {
		public Idle(Agent a) {
			super(a, 1000);
		}
		
		public void onTick() {
			if (carState == "idle") {
				//If it is time to leave, start driving
				if (currentTime >= leaveTime[dayOfWeek] && currentTime < homeTime[dayOfWeek]) {
					carState = "driving";
					System.out.println(myAgent.getLocalName() + " has started driving.\n");
				}
				//If car is at home waiting to charge, increment wait time for priority calculation
				else if (isWaiting)	{
					waitTime++;
				}
			}
		}
	}
	
	//The car is in this state when it is driving
	public class Driving extends TickerBehaviour {
		public Driving(Agent a) {
			super(a, 1000);
		}
		
		public void onTick() {
			if (carState == "driving") {				
				//Drain random amount of charge each hour to simulate random driving amounts
				Random rand = new Random();
				double travelling = rand.nextInt(10) + 1;	
				if (travelling > 3)	{
					
					currentBattery = currentBattery - Randomise(8, 16);
				}
				
				//Request charge when it is time to go home or when the battery is below 20%
				if (currentTime == homeTime[dayOfWeek] || currentBattery < minBattery) {
					addBehaviour(new RequestCharge());

				}
			}			
		}
	}
	
	
	//The agent is in this state when it is charging
	public class Charging extends TickerBehaviour {
		public Charging(Agent a) {
			super(a, 1000);
		}
	
		
		public void onTick() {
			if (carState == "charging") {
				if (currentBattery < maxBattery)
				{
					currentBattery = currentBattery + chargeRate;
					//System.out.println("Charging " + myAgent.getLocalName() + ": " + currentBattery + "%");
				}
				else
				{
					isWaiting = false;
					currentBattery = 100;
					carState = "idle";
					System.out.println("Battery is fully charged.\n");
				}
			}			
		}
	}
	
	//Sends a message to the master agent asking to charge
	public class RequestCharge extends OneShotBehaviour  {
		public void action() {
			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			msg.addReceiver(masterAgent);
			msg.setLanguage("English");
			msg.setOntology("request-charge");
			msg.setContent("true");
			send(msg);
		}
	}
	
	//Receives the reply from the master agent to check whether it can charge or not
	public class ReceiveRequestResult extends OneShotBehaviour  {
		public void action() {
			ACLMessage msg = receive();
			System.out.println("");
			if(msg != null)
			{
				System.out.println(msg.getContent());
				if (msg.getContent() == "charge" && msg.getOntology() == "charge-order")	{
					System.out.println("CO received");
					carState = "charging";
					System.out.println(myAgent.getLocalName() + " has requested to charge.\n");		
					ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
					reply.addReceiver(masterAgent);
					reply.setOntology("charge-order");
					reply.setContent("");
					send(reply);
				}
				else if (msg.getContent() == "stop")	{
					carState = "idle";
					isWaiting = true;
				}
			}
		}
	}
	
	//Create and send a message to the master agent to say that this agent has stopped charging
	public class StopCharge extends OneShotBehaviour  {
		public void action() {
			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			msg.addReceiver(masterAgent);
			msg.setLanguage("English");
			msg.setOntology("stop-charge");
			msg.setContent("false");
			send(msg);
		}
	}
	
	//Create and send a message containing all of the priority calculation information
	public class SendInformation extends CyclicBehaviour {
		public void action()	{
			ACLMessage msg = receive();
			if (msg != null && msg.getOntology() == "info-collect") {
				System.out.println("info-collect message received");
				String replyCode = msg.getReplyWith();
				ACLMessage reply = msg.createReply();//new ACLMessage(ACLMessage.INFORM);							
				reply.setContent(Integer.toString(currentBattery) + ", " + Integer.toString(Math.abs(leaveTime[dayOfWeek] - currentTime)) + ", " + Boolean.toString(isHybrid) + ", " + Integer.toString(waitTime));
				reply.setConversationId("info-collect");
				reply.setOntology("info-collect");
//				reply.addReceiver(masterAgent);
				reply.setLanguage("English");
				reply.setReplyWith(replyCode);
				reply.addReplyTo(masterAgent);
				reply.setPerformative(ACLMessage.INFORM);
				System.out.println(reply.getContent());
				//send(reply);
			}
		}
	}
	
	//Prints a message if a car battery dies
	public class BatteryDead extends CyclicBehaviour {
		public void action() {
			if (currentBattery <= 0) {
				currentBattery = 0;
				//System.out.println(myAgent.getLocalName() + "battery has died, simulation failed.\n");
			}
		}
	}
}
