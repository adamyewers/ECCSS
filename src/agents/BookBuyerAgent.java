package agents;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.AID;
import java.util.Vector;

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
}