package agents;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.AID;
import java.util.Vector;

import java.util.*;

public class MasterSchedulingAgent extends Agent {
	//Agent variables go here
	
	protected void setup() {
		// Printout a start up message
		System.out.println("Master Scheduling Agent is ready.");
	}
	
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Master Scheduling Agent terminated.");
	}
	
	//Behaviors go here
}