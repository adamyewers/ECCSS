/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
 *****************************************************************/

package agents;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;

import jade.wrapper.*;

public class O2AInterfaceExample {
	public static void main(String[] args) throws StaleProxyException, InterruptedException {
		// Get a hold to the JADE runtime
		Runtime rt = Runtime.instance();
		
		// Launch the Main Container (with the administration GUI on top) listening on port 8888
		System.out.println(">>>>>>>>>>>>>>> Launching the platform Main Container...");
		Profile pMain = new ProfileImpl(null, 8888, null);
		pMain.setParameter(Profile.GUI, "true");
		ContainerController mainCtrl = rt.createMainContainer(pMain);

		// Create and start an agent of class CounterAgSent
		System.out.println(">>>>>>>>>>>>>>> Starting Master Scheduling Agent");
		AgentController agentCtrl1 = mainCtrl.createNewAgent("MasterSchedulingAgent", MasterSchedulingAgent.class.getName(), new Object[0]);
		agentCtrl1.start();
		
		System.out.println(">>>>>>>>>>>>>>> Starting Car Agent One");
		AgentController agentCtrl2 = mainCtrl.createNewAgent("CarAgentOne", CarAgentOne.class.getName(), new Object[0]);
		agentCtrl2.start();
		
		System.out.println(">>>>>>>>>>>>>>> Starting Car Agent Two");
		AgentController agentCtrl3 = mainCtrl.createNewAgent("CarAgentTwo", CarAgentTwo.class.getName(), new Object[0]);
		agentCtrl3.start();
	}
}