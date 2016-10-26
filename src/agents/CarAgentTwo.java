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

public class CarAgentTwo extends Agent {
	//Agent variables go here
	
	protected void setup() {
		// Printout a start up message
		System.out.println("Car Agent Two is ready.");
	}
	
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Car Agent Two terminated.");
	}
	
	//Behaviors go here
}