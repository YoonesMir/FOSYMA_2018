package mas.behaviours;





import mas.agents.AgentExplorateur;
import utils.CommonUtils;
import utils.Map;
import utils.MyCouple;
import utils.TargetWrongException;


public class MovementExplorationBehaviour extends AbstractBehaviour{
	
	private static final long serialVersionUID = 1L;
	private boolean finished = false;
	private AgentExplorateur agent;
	private Long pauseperiod;
	
	public MovementExplorationBehaviour(final mas.abstractAgent myagent,long pauseperiod) {
		super(myagent);
		this.agent = (AgentExplorateur) this.myAgent;
		this.pauseperiod = pauseperiod;
	}
	
	@Override
	public void action() {
		try {
			Thread.sleep(pauseperiod);
			Map map = this.agent.getMap();
			map.setPosition();
			String position = map.getPosition();
			if(!position.equals("")) {
				String move = null;
				if(! this.agent.getExplorationFinished()) {
					String target = this.agent.getNextTarget();
					if(target == null) {
						this.agent.setNextTarget(map.bestmove_with_Distance());
						target = this.agent.getNextTarget();
					}
					if(target.equals("finished")) {
						this.agent.setExplorationFinished(true);
					}else {
						if(target.equals(position)) {
							this.agent.setNextTarget(map.bestmove_with_Distance());
							target = this.agent.getNextTarget();
						}
						if(target.equals("finished")) {
							this.agent.setExplorationFinished(true);
						}else {
							try{ if(target.equals(position)) throw new TargetWrongException("MovementExplorationBehaviour For : "+this.myAgent.getLocalName()+ " target is equal to position");}catch(TargetWrongException ex) {System.out.println(ex.toString());}
							if(this.target_non_valid(target)) {
								this.agent.setNextTarget(null);
							}else {
								MyCouple couple = map.next_move_with_target(target,false,false,false,"slow");
								move  = (String) couple.getLeft();
								int v = couple.getRight();
								//si blocage
								if(v != 0) {
									this.agent.activeProcessUnblocking(move, position,target,v);
									this.finished = true;
									return;
								}//sinon bouger
								else { ((mas.abstractAgent)this.agent).moveTo(move); }
							}
						}
						
					}	
				}//si exploration est fini movment alea
				else {
					MyCouple couple = map.next_move_with_target("Not target",false,true,false,"slow");
					move  = (String) couple.getLeft();
					int v = couple.getRight();
					//si blocage
					if(v != 0) {
						this.agent.activeProcessUnblocking(move, position,map.getLast_move(),v);
						this.finished = true;
						return;
					}//sinon bouger
					else { ((mas.abstractAgent)this.agent).moveTo(move); }
					
				}
				CommonUtils.addNextBehaviour((mas.abstractAgent)this.myAgent,false);
				this.finished = true;
			}
		}
		catch(Exception e) {e.printStackTrace();}	
	}

	private boolean target_non_valid(String target) {
		return (boolean)this.agent.getMap().getAttribute(target,"visite");
	}

	@Override
	public boolean done() {return finished;}
}



