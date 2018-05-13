package mas.behaviours;




import jade.domain.FIPAAgentManagement.DFAgentDescription;

//A voir completment , voie glum , nombre fois essaye délivrer

import mas.agents.AgentCollector;
import utils.CentralUnit;
import utils.CommonUtils;
import utils.DfUtils;
import utils.Map;
import utils.MyCouple;


public class LivrerBehaviour extends AbstractBehaviour {

	private static final long serialVersionUID = 9214290581051729970L;
	private Map map;
    private boolean finished = false;
    private AgentCollector agent;
    private Long pauseCollect;

	public LivrerBehaviour(final mas.abstractAgent myagent ,Long pauseCollect) {
		super(myagent);
		this.agent =(AgentCollector) this.myAgent;
		this.pauseCollect = pauseCollect;
	}


	@Override
	public void action() {
		try {
			Thread.sleep(pauseCollect);
			this.map = agent.getMap();
			this.map.setPosition();
			boolean flag = false;
			String target = null;
			String position = this.map.getPosition();
			if(! position.equals("")) {
				this.map.visiter(false);
				DFAgentDescription[] result = DfUtils.searchExplorer("AgentTanker",(mas.abstractAgent)this.myAgent);
				boolean done = false;
				if(result != null) {
					for(DFAgentDescription ag: result){
						done = ((mas.abstractAgent)this.myAgent).emptyMyBackPack(ag.getName().getLocalName());
					}
				}
				int cap = ((mas.abstractAgent)this.myAgent).getBackPackFreeSpace();
				if(done || cap == this.agent.getCapMaxBackPack()) {
					CentralUnit.setTankerrecived(true);
					CommonUtils.addNextBehaviour((mas.abstractAgent)this.myAgent,false);
					finished = true;
				}else {
					target = this.map.getPostionTanker(result);
					boolean tankerfinished = CentralUnit.isTankerFinished();
					if(target == null && tankerfinished) {
						flag = true;
						target = this.agent.getNodeRDV();
						if(target == null) this.map.setNodeRDV(map.nodeRDV(null));
						target = this.agent.getNodeRDV();
					}
					MyCouple couple = null;
					if(target == null) {
						couple = map.next_move_with_target("Not target",false,true,false,"speed");
						target = this.map.getLast_move();
					}
					else {
						if(flag) {
							couple = this.map.next_move_with_target(target,true,false,false,"speed");
						}else {
							couple =  this.map.next_move_with_target(target,false,false,false,"speed");
						}
					}
					int v = couple.getRight();
					String move = (String) couple.getLeft();
					//si on est bloqué
					if(v != 0) {
						this.agent.activeProcessUnblocking(move, position,target,v);
						finished = true;
						return;
					}
					//si pas de blocage : movment
					else { ((mas.abstractAgent)this.myAgent).moveTo(move); }
					}
				}
		}
		catch(Exception e) {e.printStackTrace();}
	}

	@Override
	public boolean done() { return finished; }
}
