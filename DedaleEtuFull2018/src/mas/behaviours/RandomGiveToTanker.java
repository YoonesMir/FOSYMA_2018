package mas.behaviours;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import mas.agents.AgentCollector;
import utils.DfUtils;
public class RandomGiveToTanker extends TickerBehaviour{

	public RandomGiveToTanker(final mas.abstractAgent myagent, long period) {
		super(myagent, period);
		// TODO Auto-generated constructor stub
	}

	private static final long serialVersionUID = -4139590561983877970L;

	@Override
	protected void onTick() {
		AgentCollector agent = (AgentCollector)this.myAgent;
		int cap = ((mas.abstractAgent)this.myAgent).getBackPackFreeSpace();
		int maxi = agent.getCapMaxBackPack();
		boolean done = false;
		if(cap < maxi) {
			DFAgentDescription[] result = DfUtils.searchExplorer("AgentTanker",(mas.abstractAgent)this.myAgent);
			if(result != null) {
				for(DFAgentDescription ag: result){
					done = ((mas.abstractAgent)this.myAgent).emptyMyBackPack(ag.getName().getLocalName());
					if(done) {
						System.out.println("During visite "+agent.getLocalName()+" gives to Tanker : "+(((mas.abstractAgent) agent).getBackPackFreeSpace() - cap ));
					}
					
				}
			}
		}
		
	}

}
