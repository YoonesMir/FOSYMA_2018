package mas.behaviours;

//A voir //ajoutes si on voit glum changer nodeRDv

import mas.agents.AgentTanker;
import utils.CommonUtils;
import utils.Map;
import utils.MyCouple;

public class MovmentTankerBehaviour  extends AbstractBehaviour{

	private static final long serialVersionUID = -6356828261551712267L;
	private boolean finished = false;
	private String dist;
	AgentTanker agent;
	private Long pauseperiod;

	public MovmentTankerBehaviour(final mas.abstractAgent myagent,Long pauseperiod) {
		super(myagent);
		 this.agent = (AgentTanker) this.myAgent;
		 this.pauseperiod = pauseperiod;
	}
	
	@Override
	public void action() {
		try {
			Thread.sleep(pauseperiod);
			Map map = this.agent.getMap();
			map.setPosition();
			if(!map.getPosition().equals("")) {
				map.visiter(false);
				this.dist = this.agent.getNodeRDV();
				if(this.dist == null) {
					map.setNodeRDV(map.nodeRDV(null));
					this.dist = this.agent.getNodeRDV();
				}
				//movment spécial vers node RDV
				MyCouple couple = agent.getMap().next_move_with_target(this.dist,true,false,false,"slow");
				String move = (String)couple.getLeft();
				int v = couple.getRight();
				//blocage en allant vers nodeRDv
				if(v != 0 ) {
					this.agent.activeProcessUnblocking(move, map.getPosition(),this.dist,v);
					this.finished = true;
					return;
				}
				else {((mas.abstractAgent) this.agent).moveTo(move);}
				CommonUtils.addNextBehaviour((mas.abstractAgent)this.myAgent,false);
				this.finished = true;
			}
		}
		catch(Exception e) {e.printStackTrace();}
	}

	@Override
	public boolean done() {return finished;}
}
