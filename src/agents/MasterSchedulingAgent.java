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
	Map<String, Boolean> kvMap = new HashMap<String,Boolean>();
	int step; //Used for messaging switch statement

	//masterSchedule variable - data type?

	private int currentChargingAgentIndex;

	
	//------------------- Setup -----------------------
	protected void setup() {
		step = 0;
		
		// Printout a start up message
		System.out.println("Master Scheduling Agent is ready.");
		addBehaviour(new DiscoverCarAgents());
		addBehaviour(new FindHighPriority(this));
		addBehaviour(new ReceiveChargeRequest());
		addBehaviour(new ReceiveStopRequest());
		addBehaviour(new ReceiveInfo());
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
					//kvMap.put(result[i].getName().getName(), false);
				}
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
				System.out.print("Could not add car agents to master agent.");
			}
		}
	}
	
	//----------------Functions---------------------------
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
	
	
	
	//---------------- Messaging -------------------------

	private class FindHighPriority extends TickerBehaviour 
	{ 
		private String bCast;
		private AID highPriorityCar; // The seller agent who provides the best offer
		private int highestPriority; // The best offered price
		private int repliesCount = 0; // The counter of replies from seller
	
	
		private MessageTemplate mt; // The template to receive replies private int step = 0;
		
		public FindHighPriority(Agent a) { 
		super(a, 500);
		}
		
		public void onTick() {
			
			//USE MAP TO MAKE dockedCars ARRAY
			AID[] dockedCars = new AID[kvMap.size()];
			for(int i = 0; i<kvMap.size(); i++)
			{
				if(kvMap.get(carAgents.elementAt(i).getName()) == true)
				{
					dockedCars[i] = carAgents.elementAt(i);
				}
			}
			switch (step) {
			
			case 0:
				// Send the cfp to all cars
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < dockedCars.length; ++i) {
					System.out.println(dockedCars[0].getName());
				      cfp.addReceiver(dockedCars[0]);
				      step = 1;
				    }
				cfp.setContent(bCast);
				cfp.setConversationId("info-collect"); 
				cfp.setOntology("info-collect");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				send(cfp);
				// Prepare the template to get proposals 
				//mt = MessageTemplate.and(MessageTemplate.MatchConversationId("info-collect"),
				           //MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
//				step = 1;
				break;
				    
			case 1:
				// Receive all proposals/refusals from seller agents 

				System.out.println("CASE 1\n");
				ACLMessage reply = receive();//mt
				if (reply != null) 
				{
					System.out.println("WE HAVE INFO");
					// Reply received
					if (reply.getPerformative() == ACLMessage.PROPOSE) 
					{
						System.out.println("CORRECT MSG TYPE");
						
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
					
					if (repliesCount >= dockedCars.length) 
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
			        
			       send(charge);
			       // Prepare the template to get the purchase order reply 
			       mt = MessageTemplate.and(
	               MessageTemplate.MatchConversationId("charge-order"),
	               MessageTemplate.MatchInReplyTo(charge.getReplyWith()));
	               step = 3; 
               }
				else 
				{
					// If we received charge requests, terminate 
					step = 0;
				}
			      break;
			      
			case 3:
				// Receive the purchase order reply 
				reply = receive(mt);
				if (reply != null)
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
				        
				        send(charge);
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
	} // End of FindHighPriority
	
	private class ReceiveInfo extends CyclicBehaviour 
	{ 
		public void action() 
		{
			  ACLMessage msg = myAgent.receive();
			  if (msg != null && msg.getOntology() == "info-collect") 
			  {
				  
			  }
			  else 
			  {
				  block();
			  }
		}
	}
	
	private class ReceiveChargeRequest extends CyclicBehaviour 
	{ 
		public void action() 
		{
			  ACLMessage msg = receive();
			  if (msg != null && msg.getOntology() == "request-charge") 
			  {
				  System.out.println("Charge Request received from: " + msg.getSender().getName());
				  kvMap.put(msg.getSender().getName(), true);
			  }
			  else 
			  {
				  block();
			  }
		}
	}
	
	private class ReceiveStopRequest extends CyclicBehaviour 
	{ 
		public void action() 
		{
			  ACLMessage msg = receive();
			  if (msg != null && msg.getOntology() == "stop-charge") 
			  {
				  kvMap.put(msg.getSender().getName(), false);
			  }
			  else 
			  {
				  block();
			  }
		}
	}
}