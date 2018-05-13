package utils;

import mas.agents.AgentCollector;
import mas.agents.AgentExplorateur;
import mas.agents.AgentTanker;

public class CommonUtils {
	//cette méthod est ecrit ici just pour pas le répeter par tous 
		public static void addNextBehaviour(mas.abstractAgent myAgent,boolean sameBehaviour) {
			if (myAgent instanceof AgentExplorateur && ! (myAgent instanceof AgentTanker) &&  ! (myAgent instanceof AgentCollector)) {
				((AgentExplorateur) myAgent).addNextBehaviour(sameBehaviour);
			}else if(myAgent instanceof AgentTanker) {
				((AgentTanker) myAgent).addNextBehaviour(sameBehaviour);	
			}else {
				((AgentCollector)myAgent).addNextBehaviour(sameBehaviour);
			}
			
		}
}
