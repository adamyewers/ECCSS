package agents;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.AID;
import java.util.Vector;

import java.util.*;

public class BookSellerAgent extends Agent {
	// The catalogue of books available for sale
	private Map catalogue = new HashMap();

	// Agent initializations
	protected void setup() {
		// Add the behaviour serving calls for price from buyer agents
		addBehaviour(new CallForOfferServer());

		// Add the behaviour serving purchase requests from buyer agents
		addBehaviour(new PurchaseOrderServer());
	}

	// Agent clean-up
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Seller-agent " + getAID().getName() + "terminating.");
	}

	/**
	 * This method is called by the GUI when the user inserts a new
	 * book for sale
	 * @param title The title of the book for sale
	 * @param initialPrice The initial price
	 * @param minPrice The minimum price
	 * @param deadline The deadline by which to sell the book
	 */

	public void putForSale(String title, int initPrice, int minPrice,Date deadline) {
		addBehaviour(new PriceManager(this, title, initPrice, minPrice, deadline));
	}

	public class PriceManager extends TickerBehaviour {
		private String title;
		private int initPrice, minPrice, currentPrice, deltaP;
		private long initTime, deadline, deltaT;		
		
		private PriceManager(Agent a, String t, int ip, int mp, Date d) {
			super(a, 60000);
			title = t;
			initPrice = ip;
			currentPrice = initPrice;
			deltaP = initPrice - mp;
			deadline = d.getTime();
			initTime = System.currentTimeMillis();
		}

		public void onStart() {
			// Insert the book in the catalogue of books available for sale
			catalogue.put(title, this);
			super.onStart();
		}

		public void onTick() {
			long currentTime = System.currentTimeMillis();
			if (currentTime > deadline) {
				// Deadline expired
				System.out.println("Cannot sell book " + title);
				catalogue.remove(title);
				stop();
			} 
			else {
				// Compute the current price
				long elapsedTime = currentTime - initTime;
				currentPrice = (int) (initPrice - deltaP * (elapsedTime / deltaT));
			}
		}

		public int getCurrentPrice() {
			return currentPrice;
		}
	}	
}