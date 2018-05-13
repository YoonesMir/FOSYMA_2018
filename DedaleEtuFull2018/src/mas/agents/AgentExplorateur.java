package mas.agents;






import env.EntityType;
import env.Environment;


import java.util.Date;

import jade.domain.FIPAAgentManagement.DFAgentDescription;
import mas.abstractAgent;
import mas.behaviours.*;
import utils.BlocageUtils;
import utils.CentralUnit;
import utils.CommonUtils;
import utils.DfUtils;
import utils.Map;







public class AgentExplorateur extends abstractAgent {


	private static final long serialVersionUID = 2617609839649870623L;
	private boolean blockingAcitve = false;
	private String nextTarget = null;
	private String role = null;
	private String currentBehavior = null;
	private AbstractBehaviour behaviour = null;
	private Map map = null;
    private BlocageUtils bu = null;
    protected int cptMove = 0;
    final protected int  cptMoveCycle = 25;
    protected Long pauseperiod = 100L;
    
    
    public int sizeForcevisite() {
    	return this.map.sizeForcevisite();
    }
    
    public void addToForcevisite(String id) {
    	this.map.addToForcevisite(id);
    }
    
  
    
    public String getNodeRDV() {
		return this.map.getNodeRDV();
	}
    
	public void setNodeRDV(String nodeRDV) {
		this.map.setNodeRDV(nodeRDV);
	}
	
	public String getNextTarget() {
		return this.nextTarget;
	}
	public void setNextTarget(String nextTarget) {
		this.nextTarget = nextTarget;
	}
	
	public boolean getExplorationFinished() {
        return this.map.getExplorationFinished();
    }
	
    public void setExplorationFinished(boolean b) {
    	this.map.setExplorationFinished(b);
		if(b) {
			this.setNextTarget(null);
			this.map.setFirstTimeExploration(false);
			if(this.sizeForcevisite() > 0 && ! ( this instanceof AgentCollector)) {
				
				System.out.println("Relancer expolaration pour "+this.getLocalName()+" nb force visite "+this.sizeForcevisite() );
				this.map.setAllNodes(false);
				this.map.setExplorationFinished(false);
				this.map.ens_normal_visite();
				return;
			}
			//information partagé entre tous les agents
			if (this instanceof AgentTanker) CentralUnit.setTankerFinished(true);

			if(this.map.isFirstTimeExploration()) {
				if(this instanceof AgentCollector) {
					System.out.println(((mas.abstractAgent)this).getLocalName()+" stop exploration :  nb Node : "+this.map.nbNodes()+" avec carte: "+this.map.toString(false));
				}else {
					System.out.println(((mas.abstractAgent)this).getLocalName()+" stop exploration :  nb Node : "+this.map.nbNodes());
				}
				
			}
		}
    }

	private void setRole(String role) {
		this.role = role;
	}
    
	public String getRole() {
		return this.role;
	}

	
	private void setMap(Map map) {
		this.map = map;
	}
	public Map getMap() {
		return this.map;
	}
	
	
	//le permeir Behaviour 
	private void setDefaultBehaviour() {
		this.setCurrentBehavior("VisiteBehaviour");
		this.setBehaviour(new VisiteBehaviour((mas.abstractAgent)this));
		((mas.abstractAgent)this).addBehaviour(this.behaviour);
	}
	
	
	protected void setCurrentBehavior(String currentBehavior) {
		this.currentBehavior = currentBehavior;
	}
	
	protected String getCurrentBehavior() {
		return this.currentBehavior;
	}

	protected void removeCurrentBehavior() {
		try {this.removeBehaviour(this.behaviour);}
		catch(Exception e) { e.printStackTrace();}
	}
	
	public AbstractBehaviour getBehaviour() {
		return this.behaviour;
	}
	
	public void setBehaviour(AbstractBehaviour behaviour) {
		this.behaviour = behaviour;
	}
	
	public boolean game_over() {
		DFAgentDescription[] resultat = DfUtils.searchExplorer("AgentCollect",(mas.abstractAgent)this);
		if((resultat == null ||resultat.length == 0) ) return true;
		return false;
	}
	
	protected boolean isBlocageAvitve() {
		return blockingAcitve;
	}
	
	public void setBlocageAvitve(boolean blockingAcitve) {
		this.blockingAcitve = blockingAcitve;
		if(blockingAcitve) {
			String beh = this.getCurrentBehavior();
			//activer UnBlockingBehaviour
			this.setBehaviour(new UnBlockingBehaviour((mas.abstractAgent) this,beh,this.bu));
 			((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
		}
	}
	//chque fois que on est bloqué on fait appele à cette méthod
	public void activeProcessUnblocking(String nodeblock,String position,String target,int mode) {
		this.removeCurrentBehavior();
		this.map.setPosition();
		String typeSearch = null;
		if(this.getCurrentBehavior().equals("MovementExplorationBehaviour") ) {
			typeSearch = "nextForVisite";
		}
		else if(this.currentBehavior.equals("MovmentTankerBehaviour") || this.currentBehavior.equals("LivrerBehaviour") ) {
			typeSearch = "nodeRDV";
		}
		else if(this.getCurrentBehavior().equals("MovmentForTreasureBehaviour")) {
			typeSearch = "nextTreasure";
		}
		else{
			typeSearch = "other";
		}
		this.bu = new BlocageUtils((mas.abstractAgent)this,nodeblock,position,target,this.getMap(),typeSearch,this.map.getChemin());
		this.map.setChemin(null);
		this.map.setLastPosition(null);
		setBlocageAvitve(true);
	}
	
	//chaque fois que on sort de UnBlockingBehaviour on fair appele à cette méthod
	public void setUnBlockingBehaviourOFF( String next_Behaviour) {
		this.setBlocageAvitve(false);
		this.removeCurrentBehavior();
		this.bu = null;
		if(next_Behaviour == null) {
			CommonUtils.addNextBehaviour((mas.abstractAgent) this,true);
		}
		else if(next_Behaviour.equals("JustWalkBehaviour")){
			 ((mas.abstractAgent) this).addBehaviour(new JustWalkBehaviour((mas.abstractAgent) this,pauseperiod));
		}
	}
	
	
	
	public  void addNextBehaviour(boolean activeSameBehaviour) {
		this.removeCurrentBehavior();
		if(this.game_over()) {
			this.setBehaviour(new emptyMailbox((mas.abstractAgent) this ));
			 ((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
		}
		else {
			if(this.currentBehavior.equals("VisiteBehaviour")) {
				if(this.cptMove % this.cptMoveCycle > 0 ) {
					this.cptMove  = this.cptMove+1;
					this.setCurrentBehavior("MovementExplorationBehaviour");
					this.setBehaviour(new MovementExplorationBehaviour((mas.abstractAgent) this,pauseperiod ));
					((mas.abstractAgent) this).addBehaviour(this.getBehaviour());	
				}else {
					this.setCurrentBehavior("SendMapBehaviour");
					this.setBehaviour(new SendMapBehaviour((mas.abstractAgent) this,false ));
					((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
				}
			}
			else if(this.currentBehavior.equals("SendMapBehaviour")) {
				this.setCurrentBehavior("ReceiveMapBehaviour");
				this.setBehaviour(new ReceiveMapBehaviour((mas.abstractAgent) this ));
				((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
			}
			else if(this.currentBehavior.equals("ReceiveMapBehaviour")) {
				this.cptMove  = this.cptMove+1;
				this.setCurrentBehavior("MovementExplorationBehaviour");
				this.setBehaviour(new MovementExplorationBehaviour((mas.abstractAgent) this,pauseperiod ));
				((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
			}
			else if(this.currentBehavior.equals("MovementExplorationBehaviour")){
				if(activeSameBehaviour) {
					this.cptMove  = this.cptMove+1;
					this.setBehaviour(new MovementExplorationBehaviour((mas.abstractAgent) this,pauseperiod));
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
    }
    
	protected void setupArguments() {
    	final Object[] args = getArguments();
		if(args!=null && args[0]!=null && args[1]!=null){
			this.setMap(new Map(this));
			this.setRole(args[1].toString());
			DfUtils.registerOnDF(this.getRole(), this);
			deployAgent((Environment) args[0],(EntityType)args[1]);
		}else{
			System.err.println("Malfunction during parameter's loading of agent"+ this.getClass().getName()+" "+(new Date()).getTime());
			System.exit(-1);
		}
	}

    protected void setup(){
        super.setup();
        this.setupArguments();
        try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        this.setDefaultBehaviour();
    }
    
    protected void takeDown(){ }
}
