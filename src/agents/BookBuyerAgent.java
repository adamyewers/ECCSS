package agents;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.AID;
import java.util.Vector;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Date;

public class BookBuyerAgent extends Agent {
	// The list of known seller agents
	private Vector<AID> sellerAgents = new Vector<AID>();

	// Agent initializations
	protected void setup() {
		// Printout a welcome message
		System.out.println("Buyer-agent " + getAID().getName() + " is ready.");

		// Get names of seller agents as arguments
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			for (int i = 0; i < args.length; ++i) {
				AID seller = new AID((String) args[i], AID.ISLOCALNAME);
				sellerAgents.addElement(seller);
			}
		}
	}

	// Agent clean-up
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Buyer-agent " + getAID().getName() + "terminated.");
	}

	public void purchase(String title, int maxPrice, Date deadline) {
		addBehaviour(new PurchaseManager(this, title, maxPrice, deadline));
	}

	private class PurchaseManager extends TickerBehaviour {
		private String title;
		private int maxPrice;
		private long deadline, initTime, deltaT;

		private PurchaseManager(Agent a, String t, int mp, Date d) {
			super(a, 60000); // tick every minute
			title = t;
			maxPrice = mp;
			deadline = d.getTime();
			initTime = System.currentTimeMillis();
			deltaT = deadline - initTime;
		}

		public void onTick() {
			long currentTime = System.currentTimeMillis();
			if (currentTime > deadline) {
				// Deadline expired
				System.out.println("Cannot buy book " + title);
				stop();
			} else {
				// Compute the currently acceptable price and start negotiation
				long elapsedTime = currentTime - initTime;
				int acceptablePrice = (int) (maxPrice * (elapsedTime / deltaT));
				myAgent.addBehaviour(new BookNegotiator(title, acceptablePrice, this));
			}
		}
	}
	
	/**
	Inner class BookNegotiator.
	This is the behaviour used by Book-buyer agents to actually negotiate with seller agents the purchase of a book.
	*/
	private class BookNegotiator extends Behaviour {
		private String title;
		private int maxPrice;
		private PurchaseManager manager;
		private AID bestSeller; // The seller agent who provides the best offer
		private int bestPrice; // The best offered price
		private int repliesCnt = 0; // The counter of replies from seller agents
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;
		
		public BookNegotiator(String t, int p, PurchaseManager m) {
			super(null);
			title = t;
			maxPrice = p;
			manager = m;
		}
		public void action() {
			switch (step) {
			case 0:
				// Send the cfp to all sellers
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < sellerAgents.size(); ++i) {
					cfp.addReceiver(sellerAgents.elementAt(i));
				}
				cfp.setContent(title);
				cfp.setConversationId("book-trade");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"), MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				// Receive all proposals/refusals from seller agents
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						// This is an offer
						int price = Integer.parseInt(reply.getContent());
						if (bestSeller == null || price < bestPrice) {
							// This is the best offer at present
							bestPrice = price;
							bestSeller = reply.getSender();
						}
					}
					repliesCnt++;
					if (repliesCnt >= sellerAgents.size()) {
						// We received all replies
						step = 2;
					}
				}
				else {
					block();
				}
				break;
			case 2:
				if (bestSeller != null && bestPrice <= maxPrice) {
					// Send the purchase order to the seller that provided the best offer
					ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
					order.addReceiver(bestSeller);
					order.setContent(title);
					order.setConversationId("book-trade");
					order.setReplyWith("order"+System.currentTimeMillis());
					myAgent.send(order);
					// Prepare the template to get the purchase order reply
					mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"), MessageTemplate.MatchInReplyTo(order.getReplyWith()));
					step = 3;
				}
				else {
					// If we received no acceptable proposals, terminate
					step = 4;
				}
				break;
			case 3:
				// Receive the purchase order reply
				reply = myAgent.receive(mt);
		if (reply != null) {
			// Purchase order reply received
			if (reply.getPerformative() == ACLMessage.INFORM) {
				// Purchase successful. We can terminate
				System.out.println("Book "+title+" successfully purchased.Price = " + bestPrice);
				manager.stop();
			}
			step = 4;
		}
		else {
			block();
		}
		break;
			}
		}
		public boolean done() {
			return step == 4;
		}
	} // End of inner class BookNegotiator
}