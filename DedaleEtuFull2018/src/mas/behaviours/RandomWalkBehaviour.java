package mas.behaviours;


	


import mas.agents.AgentCollector;
import utils.Map;
import utils.MyCouple;


//juste marhcé alea
//c'est que les collector qui utilise cette Behaviour une fois que un agent collector
//terminé sont travaille il commence de marche alea tant que tous les
//collector terminent leur travaille
public class RandomWalkBehaviour extends AbstractBehaviour{

	private static final long serialVersionUID = 9088209402507795289L;
	private boolean finished = false;
	private Long pauseperiod;

	public RandomWalkBehaviour (final mas.abstractAgent myagent,Long pauseperiod) {
		super(myagent);
		this.pauseperiod = pauseperiod;
	}

	@Override
	public void action() {
		try {
			AgentCollector agent = (AgentCollector) this.myAgent;
			Map map = agent.getMap();
			map.setPosition();
			if(!map.getPosition().equals("")) {
				map.visiter(false);
				if(agent.game_over()) {
					((mas.abstractAgent) agent).addBehaviour(new emptyMailbox((mas.abstractAgent) agent ));
					this.finished = true;
					return;
				}
				
				//movment alea 
				MyCouple m = map.next_move_with_target("Not target",false,true,false,"slow");
				String move = (String)m.getLeft();
				int v = m.getRight();
				if(v != 0) {
					agent.activeProcessUnblocking(move, map.getPosition(),map.getLast_move(),v);
					this.finished = true;
					return;
				}//sinon bouger
				else { 
					Thread.sleep(pauseperiod);
					((mas.abstractAgent)agent).moveTo(move);
					}
			}	
		}
		catch(Exception e) {e.printStackTrace();}
	}
	
	@Override
	public boolean done() {return this.finished;}
}