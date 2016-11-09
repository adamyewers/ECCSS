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
	Map kvMap = new HashMap();
	int step; //Used for messaging switch statement

	//masterSchedule variable - data type?

	private int currentChargingAgentIndex;

	
	//------------------- Setup -----------------------
	protected void setup() {
		step = 0;
		
		// Printout a start up message
		System.out.println("Master Scheduling Agent is ready.");
		addBehaviour(new ExampleBehaviour(this));
		addBehaviour(new DiscoverCarAgents());
//		addBehaviour(new ReceiveChargeRequest());
		addBehaviour(new ProcessRequest());
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
					kvMap.put(result[i].getName(), false);
				}
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
				System.out.print("Could not add car agents to master agent.");
			}
		}
	}
	
	
	//-------------------------------------------------
	// 			Process Request Behaviour
	//-------------------------------------------------
	public class ProcessRequest extends OneShotBehaviour {
		public void action(){
			
		}
	}

	
//	public class ReceiveChargeRequest extends CyclicBehaviour {
//		public void action() {
//			//check if a schedule has been requested, if so process it
//			ACLMessage msg = receive();
//			if (msg != null) {
//				// Process the message
//				System.out.print(msg.getContent());
//				AID x = msg.getSender();
//				
//				if(x.requireCharge == true)
//				{
//					double newPriority = calcPriority(i);
//					double currentPriority = calcPriority(currentChargingAgentIndex);
//					if(priority > currentChargingAgent.Priority)
//					{
//						Charge(x);
//						currentChargingIndex = i;
//					}
//				}
//				
//			}
//			else
//			{
//				block();
//			}
//		}
//	}
	
	
	//--------------------------------------------
	// Function: CalcPriotity(int)
	// Desc.: Calculate the priority of a car
	// Inputs: i - the index of the car 
	// 	in carAgents AID
	//
	// Outputs: priority - int
	//-------------------------------------------
	private double CalcPriority(int battLevel, float timeToLeave, boolean isHybrid, int waitingTime)
	{
		double priority;
		
		priority = (100-battLevel)*timeToLeave + (waitingTime*10);
		
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
		int wait;
		int total = carAgents.size();
		ACLMessage reply = receive();
		
		for(int i = 0; i < total; i++)
		{
			//MESSAGE: do you require charge, Agent x?
			ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
			
			cfp.addReceiver(carAgents.elementAt(i));
			send(cfp);
		
		}
	}
	
	
	
	
	//---------------- Messaging ------------------

	private class FindHighPriority extends Behaviour 
	{ 
		
		private String title;
		private int maxPrice;
		private MasterSchedulingAgent master;
		private AID highPriorityCar; // The seller agent who provides the best offer
		private int highestPriority; // The best offered price
		private int repliesCount = 0; // The counter of replies from seller
	
	
		private MessageTemplate mt; // The template to receive replies private int step = 0;
		
		public FindHighPriority(String t, int p, MasterSchedulingAgent m) { 
		super(null);
		title = t;
		maxPrice = p;
		master = m; 
		}
		
		public void action() {
			
			//USE MAP TO MAKE dockedCars ARRAY
			
			switch (step) {
			
			case 0:
				// Send the cfp to all sellers
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP); 
				
				for (int i = 0; i < carAgents.size(); ++i) {
				      cfp.addReceiver(dockedCars[i]);
				    }
				cfp.setContent(title);
				cfp.setConversationId("book-trade"); cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				// Prepare the template to get proposals 
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
				           MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
				    
			case 1:
				// Receive all proposals/refusals from seller agents 
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) 
				{
					// Reply received
					if (reply.getPerformative() == ACLMessage.PROPOSE) 
					{
						// This is an offer
						String msgContent = reply.getContent(); 
						
						//BREAK UP STRING, GET VARIABLES, DO CALC
						String[] parts = msgContent.split(", ");
						int battLevel = Integer.parseInt(parts[0]);
						float timeToLeave = Float.parseFloat(parts[1]);
						boolean isHybrid = Boolean.parseBoolean(parts[2]);
						int waitingTime = Integer.parseInt(parts[3]);
						
						double priority = CalcPriority(battLevel, timeToLeave, isHybrid, waitingTime);
						
						if (highPriorityCar == null || priority > highestPriority) 
						{
							// This is the best offer at present bestPrice = price;
							highPriorityCar = reply.getSender();
						} 
					}
					repliesCount++;
					
					if (repliesCount >= carAgents.size()) 
					{
						// We received all replies
						step = 2; 
					}
				} 
				else 
				{
					block(); 
				}
				break;
			
			case 2:
				if (highPriorityCar != null )//&& highestPriority > maxPrice) 
				{
					// Send the charge order to the car with the highest priority
					ACLMessage charge = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
					
			        charge.addReceiver(highPriorityCar);
			        charge.setContent("charge");
			        charge.setConversationId("charge-order");
			        charge.setReplyWith("charge"+System.currentTimeMillis()); //Unique value
			        
			       myAgent.send(charge);
			       // Prepare the template to get the purchase order reply 
			       mt = MessageTemplate.and(
	               MessageTemplate.MatchConversationId("charge-order"),
	               MessageTemplate.MatchInReplyTo(charge.getReplyWith()));
	               step = 3; 
               }
				else 
				{
					// If we received charge requests, terminate 
					step = 4;
				}
			      break;
			      
			case 3:
				// Receive the purchase order reply 
				reply = myAgent.receive(mt);
				if (reply != null) 			//MAY NEED TO REMOVE THIS SECTION
				{
					// Charge order reply received
					if (reply.getPerformative() == ACLMessage.INFORM) 
					{
						String name = reply.getSender().getName();
						System.out.println(name + " is now charging.");
						
						ACLMessage charge = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
						
						for (int i = 0; i < carAgents.size(); i++)
						{
							
							if ( carAgents.elementAt(i) != reply.getSender())
							{
								
						        charge.addReceiver(carAgents.elementAt(i));
							}
						}

				        charge.setContent("stop");
				        charge.setConversationId("charge-order");
				        charge.setReplyWith("charge"+System.currentTimeMillis()); //Unique value
				        
				        myAgent.send(charge);
					}
				step = 4; 
				}
				else 
				{
					block();
				}
				
				break; 
			}
		}
		
		public boolean done() 
		{ 
			return step == 4;
		}
	} // End of FindHighPriority
}