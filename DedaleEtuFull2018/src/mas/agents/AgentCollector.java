package mas.agents;






import mas.behaviours.FindNextTreasureBehaviour;
import mas.behaviours.LivrerBehaviour;
import mas.behaviours.MovementExplorationBehaviour;
import mas.behaviours.MovmentForTreasureBehaviour;
import mas.behaviours.RandomWalkBehaviour;
import mas.behaviours.ReceiveMapBehaviour;
import mas.behaviours.SendMapBehaviour;
import mas.behaviours.VisiteBehaviour;
import utils.DfUtils;
import utils.MyCouple;


public class AgentCollector extends AgentExplorateur{

	private static final long serialVersionUID = 949759335270719676L;
	private boolean collectionFinished = false;
	private boolean done = false;
	private MyCouple nexTreasure = null;
    private int totalCollect = 0; 
    final private int  nb_max_finished_collection = 1;
    private int  cpt_finished_collection = 0;
    private int capMaxBackPack = -1;
    private boolean startlivrer = false;
    private boolean restartFindNextTreasure = false;
    private Long pauseCollect = 50L;
   
    
    public MyCouple getNexTreasure() {
		return this.nexTreasure;
	}

	public void setNexTreasure(MyCouple nexTreasure) {
		this.nexTreasure = nexTreasure;
	}

	
	public int getTotalCollect() {
		return this.totalCollect;
	}

	public void setTotalCollect(int totalCollect) {
		this.totalCollect = totalCollect;
	}



	public boolean getDone() {
		return done;
	}

	public void setDone(boolean done) {
		this.done = done;
	}

   public boolean isRestartFindNextT() {
		return restartFindNextTreasure;
	}

	public void setRestartFindNextT(boolean restartFindNextT) {
		this.restartFindNextTreasure = restartFindNextT;
	}

	public boolean isStartlivrer() {
		return startlivrer;
	}

	public void setStartlivrer(boolean startlivrer) {
		this.startlivrer = startlivrer;
	}

	public int getCapMaxBackPack() {
	   return this.capMaxBackPack;
    }
   
   //On initialise une seule fois valeur de max back pack
   public void setCapMaxBackPack() {
	   if(this.capMaxBackPack == -1) {
		   this.capMaxBackPack = Integer.valueOf(((mas.abstractAgent) this).getBackPackFreeSpace());
	   }
   }
    

	public boolean getCollectionFinished() {
		return this.collectionFinished;
	}
	
	public void setCollectionFinished(boolean collectionFinished) {
		this.collectionFinished = collectionFinished;
		boolean flag = false;
		//si on a rien trouvé sur la carte
		if(collectionFinished) {
			this.removeCurrentBehavior();
			if(! this.getMap().isGlum() && this.sizeForcevisite() <= 0) flag = true;
			//si on a déja relacer exploration est on a rien trouvé , on fait un movment alea tantque tous les collector finis leur travaille
			if(flag || ( this.cpt_finished_collection >= this.nb_max_finished_collection && this.sizeForcevisite() <= 0) ) {
				System.out.println(((mas.abstractAgent)this).getLocalName()+" Total collect : "+ ((AgentCollector) this).getMyTreasureType()+" "+((AgentCollector) this).getTotalCollect()+" Avect carte :\n "+((AgentExplorateur) this).getMap().toString(false));
				DfUtils.deletFromDF((mas.abstractAgent) this);
				DfUtils.registerOnDF("CollectFinished", this);
				this.setCurrentBehavior("RandomWalkBehaviour");
				this.setBehaviour(new RandomWalkBehaviour((mas.abstractAgent) this,this.pauseperiod ));
				((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
				
			}
			//on relance une fois exploaration pour etre sûr que il reste vraimet rien 
			else {
				System.out.println("Relancer exploration pour "+this.getLocalName()+" "+this.cpt_finished_collection);
				this.setExplorationFinished(false);
				this.getMap().setAllNodes(false);
				this.cpt_finished_collection += 1;
				this.setNextTarget(null);
				this.getMap().ens_normal_visite();
				this.setCurrentBehavior("VisiteBehaviour");
				this.setBehaviour(new VisiteBehaviour((mas.abstractAgent) this ));
				((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
			}
		}
	}

	
	public void addNextBehaviour(boolean activeSameBehaviour) {
		this.removeCurrentBehavior();
    	if(!this.getExplorationFinished()) {
    		if(this.getCurrentBehavior().equals("VisiteBehaviour")) {
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
			else if(this.getCurrentBehavior().equals("SendMapBehaviour")) {
				this.setCurrentBehavior("ReceiveMapBehaviour");
				this.setBehaviour(new ReceiveMapBehaviour((mas.abstractAgent) this ));
				((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
			}
			else if(this.getCurrentBehavior().equals("ReceiveMapBehaviour")) {
				this.cptMove  = this.cptMove+1;
				this.setCurrentBehavior("MovementExplorationBehaviour");
				this.setBehaviour(new MovementExplorationBehaviour((mas.abstractAgent) this,pauseperiod ));
				((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
			}
			else if(this.getCurrentBehavior().equals("MovementExplorationBehaviour")){
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
    	else {
    		if(this.getCurrentBehavior().equals("FindNextTreasureBehaviour")) {
    			if(this.isStartlivrer()) {
    				this.setCurrentBehavior("LivrerBehaviour");
	    			this.setBehaviour(new LivrerBehaviour((mas.abstractAgent) this,pauseCollect ));
					((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
    			}else {
					this.setCurrentBehavior("MovmentForTreasureBehaviour");
	    			this.setBehaviour(new MovmentForTreasureBehaviour((mas.abstractAgent) this,pauseCollect ));
					((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
    			}
    		}
    		else if(this.getCurrentBehavior().equals("MovmentForTreasureBehaviour") ) {
    			if(activeSameBehaviour) {
    				this.setCurrentBehavior("MovmentForTreasureBehaviour");
	    			this.setBehaviour(new MovmentForTreasureBehaviour((mas.abstractAgent) this,this.pauseCollect ));
					((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
    			}else {
    				if(this.getDone()) {
	    				this.setCurrentBehavior("FindNextTreasureBehaviour");
	    				this.setBehaviour(new FindNextTreasureBehaviour((mas.abstractAgent) this ));
						((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
	    			}
	    			
    					if(this.isRestartFindNextT()) {
    						this.setCurrentBehavior("FindNextTreasureBehaviour");
		    				this.setBehaviour(new FindNextTreasureBehaviour((mas.abstractAgent) this ));
							((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
    					}
    					else {
    						this.setCurrentBehavior("LivrerBehaviour");
		    				this.setBehaviour(new LivrerBehaviour((mas.abstractAgent) this,pauseCollect ));
							((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
    					}
	    			
    			}
    		}
    		else if(this.getCurrentBehavior().equals("LivrerBehaviour")) {
    			if(activeSameBehaviour) {
    				this.setCurrentBehavior("LivrerBehaviour");
    				this.setBehaviour(new LivrerBehaviour((mas.abstractAgent) this ,this.pauseCollect));
					((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
    			}else {
    				this.setCurrentBehavior("SendMapBehaviour");
	    			this.setBehaviour(new SendMapBehaviour((mas.abstractAgent) this,true));
					((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
    			}
    		}
    		else if(this.getCurrentBehavior().equals("SendMapBehaviour")) {
    			this.setCurrentBehavior("ReceiveMapBehaviour");
    			this.setBehaviour(new ReceiveMapBehaviour((mas.abstractAgent) this ));
				((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
    		}
  
    		else{
				this.setCurrentBehavior("FindNextTreasureBehaviour");
    			this.setBehaviour(new FindNextTreasureBehaviour((mas.abstractAgent) this ));
				((mas.abstractAgent) this).addBehaviour(this.getBehaviour());
    		}
    	}	
	}
}
