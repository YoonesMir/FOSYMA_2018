package mas.behaviours;


import jade.domain.FIPAAgentManagement.DFAgentDescription;
import mas.agents.AgentCollector;
import mas.agents.AgentExplorateur;
import utils.CommonUtils;
import utils.DfUtils;
import utils.Map;



public class VisiteBehaviour extends AbstractBehaviour{
	
	private static final long serialVersionUID = -7727814212984643827L;
	private boolean finished = false;
	private AgentExplorateur agent;

	public VisiteBehaviour(final mas.abstractAgent myagent ) {
		super(myagent);
		this.agent = (AgentExplorateur) this.myAgent;
	}

	//visite permanat de la carte tant que le proces total n'est pas términé
	public void action() {
		try {
			Map map = this.agent.getMap();
			map.setPosition();
			String position = map.getPosition();
			if(!position.equals("")) {
				//visiter ce node et les nodes voisions
				map.visiter(false);
				//si c'est agnet Collector est on essaye de picker si il y a qqch sur la route avec taille plus petit de notre capacite on le prendre
				if(this.myAgent instanceof AgentCollector) {
					AgentCollector collectorAgent = (AgentCollector) this.myAgent;
					//on fait appele à cette méthod mais il intilalise que une seul fois (permier fois d'appele) valeur de capMaxBackPack
					
					int mybackpack = ((mas.abstractAgent) collectorAgent).getBackPackFreeSpace();
					if(mybackpack < collectorAgent.getCapMaxBackPack()) {
						DFAgentDescription[] result = DfUtils.searchExplorer("AgentTanker",(mas.abstractAgent)collectorAgent);
						boolean done = false;
						if(result != null) {
							for(DFAgentDescription ag: result){
								done = ((mas.abstractAgent)collectorAgent).emptyMyBackPack(ag.getName().getLocalName());
								if(done) {
									System.out.println("During visite "+collectorAgent.getLocalName()+" gives to Tanker : "+(((mas.abstractAgent) collectorAgent).getBackPackFreeSpace() - mybackpack ));
								}
							}
						}
					}
					mybackpack = ((mas.abstractAgent) collectorAgent).getBackPackFreeSpace();
					if(mybackpack > 0) {
						//verfier si il ya qqch sur ce node de type de mon sac
						//params false ca veit dire que je chercher que les trucs de typede mon sac
						int valeur = collectorAgent.getMap().is_Treasure_on_this_node(collectorAgent.getMyTreasureType(),false);
						// v = valuer de Diamonds/Treasure sur ce node
						if(valeur > 0 && mybackpack >= valeur ) {
							int pickup = ((mas.abstractAgent) collectorAgent).pick();
							System.out.println(collectorAgent.getLocalName()+" During visite :  pickup "+pickup+" in position "+collectorAgent.getMap().getPosition()+" type : "+collectorAgent.getMyTreasureType());
							collectorAgent.setTotalCollect(pickup+collectorAgent.getTotalCollect());
							if(collectorAgent.getMyTreasureType().equals("Diamonds")) {
								map.setDiamondsValeur(map.getDiamondsValeur()+pickup);
							}else {
								map.setTreasureValeur(map.getTreasureValeur()+pickup);
							}
							map.visiter(true);
						}
					}
				}
				CommonUtils.addNextBehaviour((mas.abstractAgent)this.myAgent,false);
				this.finished = true;
			}
		}catch(Exception e) {e.printStackTrace();}
	}
	
	@Override
	public boolean done() {return finished;}
}