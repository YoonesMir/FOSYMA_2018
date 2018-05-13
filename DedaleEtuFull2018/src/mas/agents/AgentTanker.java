package mas.agents;



import mas.behaviours.MovementExplorationBehaviour;
import mas.behaviours.MovmentTankerBehaviour;
import mas.behaviours.ReceiveMapBehaviour;
import mas.behaviours.SendMapBehaviour;
import mas.behaviours.VisiteBehaviour;
import mas.behaviours.emptyMailbox;
import utils.CentralUnit;

public class AgentTanker  extends AgentExplorateur{

	private static final long serialVersionUID = 949759335270719676L;

	
	public  void addNextBehaviour(boolean activeSameBehaviour) {
		this.removeCurrentBehavior();
		if( CentralUnit.getTankerrecived() ) {
			CentralUnit.setTankerrecived(false);
			this.setCurrentBehavior("ReceiveMapBehaviour");
			this.setBehaviour(new ReceiveMapBehaviour((mas.abstractAgent) this ));
			((mas.abstractAgent) this).addBehaviour(this.getBehaviour());

		}
		else if(this.game_over()) {
			this.setBehaviour(new emptyMailbox((mas.abstractAgent) this ));
			((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
		}else {
			if(!this.getExplorationFinished()) {
				if(this.getCurrentBehavior().equals("VisiteBehaviour")) {
					if(this.cptMove % this.cptMoveCycle > 0 ) {
						this.cptMove  = this.cptMove+1;
						this.setCurrentBehavior("MovementExplorationBehaviour");
						this.setBehaviour(new MovementExplorationBehaviour((mas.abstractAgent) this,this.pauseperiod ));
						((mas.abstractAgent) this).addBehaviour(this.getBehaviour());	
					}else {
						this.setCurrentBehavior("ReceiveMapBehaviour");
						this.setBehaviour(new ReceiveMapBehaviour((mas.abstractAgent) this));
						((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
					}
				}
				else if(this.getCurrentBehavior().equals("ReceiveMapBehaviour")) {
					this.setCurrentBehavior("SendMapBehaviour");
					this.setBehaviour(new SendMapBehaviour((mas.abstractAgent) this,false ));
					((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
				}
				else if(this.getCurrentBehavior().equals("SendMapBehaviour")) {
					this.cptMove  = this.cptMove+1;
					this.setCurrentBehavior("MovementExplorationBehaviour");
					this.setBehaviour(new MovementExplorationBehaviour((mas.abstractAgent) this,this.pauseperiod ));
					((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
				}
				else if(this.getCurrentBehavior().equals("MovementExplorationBehaviour")){
					if(activeSameBehaviour) {
						this.cptMove  = this.cptMove+1;
						this.setBehaviour(new MovementExplorationBehaviour((mas.abstractAgent) this,this.pauseperiod));
						((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
					}else {
						this.setCurrentBehavior("VisiteBehaviour");
						this.setBehaviour(new VisiteBehaviour((mas.abstractAgent) this ));
						((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
					}
				}
				else {
					System.out.println("Not recognizable behaviour for "+this.getLocalName() );
				}
			}
			else {
				if(this.getCurrentBehavior().equals("ReceiveMapBehaviour")) {
					this.setCurrentBehavior("SendMapBehaviour");
					this.setBehaviour(new SendMapBehaviour((mas.abstractAgent) this,false ));
					((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
					
				}
				else if(this.getCurrentBehavior().equals("MovmentTankerBehaviour")) {
					this.setCurrentBehavior("ReceiveMapBehaviour");
					this.setBehaviour(new ReceiveMapBehaviour((mas.abstractAgent) this ));
					((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
				}
				else {
					this.setCurrentBehavior("MovmentTankerBehaviour");
					this.setBehaviour(new MovmentTankerBehaviour((mas.abstractAgent) this,this.pauseperiod ));
					((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
				}
			}
		}
	}
}
