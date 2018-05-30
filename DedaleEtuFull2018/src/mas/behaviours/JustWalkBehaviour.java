package mas.behaviours;


import mas.agents.AgentCollector;
import mas.agents.AgentExplorateur;
import utils.CommonUtils;
import utils.Map;
import utils.MyCouple;

public class JustWalkBehaviour   extends AbstractBehaviour{

	private static final long serialVersionUID = 2971613752958169836L;
	private boolean finished = false;
	private Long pauseperiod;

	public JustWalkBehaviour(final mas.abstractAgent abstractAgent,Long pauseperiod) {
		super(abstractAgent);
		this.pauseperiod = pauseperiod;
	}


	@Override
	public void action() {
		try {
			
			AgentExplorateur agent = (AgentExplorateur) this.myAgent;
			Map map = agent.getMap();
			map.setPosition();
			
			String position = map.getPosition();
			
			if(!position.equals("")) {
				//visiter ce node et autour
				map.visiter(false);
				MyCouple couple = map.next_move_with_target(agent.getNextTarget(),false,false,true,"slow");
				String move  = (String) couple.getLeft();
				int v = couple.getRight();
				//si c'est fini just walk ou si on est bloqué
				if(  v != 0 ||  move.equals("finished")) {
					agent.setNextTarget(null);
					map.setChemin(null);
					if(this.myAgent instanceof AgentCollector){
						AgentCollector agentCollector = (AgentCollector) this.myAgent;
						agentCollector.setNexTreasure(null);
					}
					//activer dernier Behaviour qui etait active avant 
					CommonUtils.addNextBehaviour(agent, true);
					finished = true;
				}
				//sinon bouger
				else {
					Thread.sleep(pauseperiod);
					((mas.abstractAgent)agent).moveTo(move);}
			}	
		}catch(Exception e) {e.printStackTrace();}
		
	}

	@Override
	public boolean done() { return finished; }	
}
