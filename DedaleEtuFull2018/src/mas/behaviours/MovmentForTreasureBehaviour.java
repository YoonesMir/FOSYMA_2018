package mas.behaviours;



import mas.agents.AgentCollector;
import utils.CommonUtils;
import utils.DfUtils;
import utils.Map;
import utils.MyCouple;
import org.graphstream.graph.Node;

import jade.domain.FIPAAgentManagement.DFAgentDescription;

import java.util.List;


//A voir : est-ce que c'est bien si on prends un T/D en danger ou pas
//sans regrader le perts potentielle et aussi le fait que peut etre y a qqn qui viens pour le checher et il est proche
//et aussi le points négative que on change completment notre plain de checher les T/D

public class MovmentForTreasureBehaviour extends AbstractBehaviour {

	private static final long serialVersionUID = -700727734500971996L;
	private boolean finished = false;
	private Map map;
	private AgentCollector agent;
	private Long pauseCollect;
	public MovmentForTreasureBehaviour(final mas.abstractAgent myagent,Long pauseCollect ) {
		super(myagent);
		this.agent = (AgentCollector) this.myAgent;;
		this.map = this.agent.getMap();
		this.pauseCollect = pauseCollect;
	}

	@Override
	public void action() {
		try {
			Thread.sleep(pauseCollect);
			this.map.setPosition();
			String position = this.map.getPosition();
			boolean done = false;
			if(! position.equals("")) {
				int cap = ((mas.abstractAgent)this.myAgent).getBackPackFreeSpace();
				if(cap < agent.getCapMaxBackPack()) {
					DFAgentDescription[] result = DfUtils.searchExplorer("AgentTanker",(mas.abstractAgent)this.myAgent);
					if(result != null) {
						for(DFAgentDescription ag: result){
							done = ((mas.abstractAgent)this.myAgent).emptyMyBackPack(ag.getName().getLocalName());
						}
					}
				}
				if(done) {
					agent.setDone(true);
					CommonUtils.addNextBehaviour((mas.abstractAgent)this.myAgent,false);
					this.finished = true;
					return;
					
				}
				//visite local le node , danger = true si le Glum est la
				boolean danger = this.map.visiter(false);
				String nextDist = this.agent.getNextTarget();
				MyCouple my = this.agent.getNexTreasure();
				//si on voit un T/D en danger de mon type avec valeur plus grand que zero
				//ou si on est arrive à notre distination, on essaye  picker ce T/D
				if((danger && this.treasureEnDanger(position)) || (nextDist != null && my != null && position.equals(nextDist) ) ) {
					int pickup = ((mas.abstractAgent) agent).pick();
					if(position.equals(nextDist)) {
						System.out.println(agent.getLocalName()+" pickup "+pickup+" in position "+position+" ce T/D n'est pas en danger ");
					}
					else {
						System.out.println(agent.getLocalName()+" pickup "+pickup+" in position "+position+" ce T/D est  en danger donc on a changé le plain ");
					}
					this.map.setCollectorOfNode(position, "");
					cap = ((mas.abstractAgent)this.myAgent).getBackPackFreeSpace();
					//revister ce node
					this.map.visiter(false);
					//si pickup > 0 
					if(pickup > 0) {
						agent.setDone(false);
						agent.setTotalCollect(pickup+this.agent.getTotalCollect());
						//si j'ai plus capacite je commence livre
						if(cap <= 0) {
							agent.setRestartFindNextT(false);
						}//sinon je chercher un autre T/D
						else {
							agent.setRestartFindNextT(true);
						}
						CommonUtils.addNextBehaviour((mas.abstractAgent)this.myAgent,false);
					}
					//si pickup == 0 , je chercher directement un autre T/T
					else {
						agent.setDone(true);
						CommonUtils.addNextBehaviour((mas.abstractAgent)this.myAgent,false);
					}
					finished = true;
				}//sinon
				else {
					//si j'ai recu un message ou avec autre manier je sait que mon direction n'est pas valide
					//je recommence chercher un autre T/D
					if(not_valid_Destination()) {
						if(nextDist != null)this.map.setCollectorOfNode(nextDist, "");
						agent.setDone(true);
						CommonUtils.addNextBehaviour((mas.abstractAgent)this.myAgent,false);
						finished = true;
					}//sinon je bouge vers mon dist si je ne suis pas blouqé
					else {
						MyCouple couple = this.map.next_move_with_target(nextDist,false,false,false,"speed");
						int v = couple.getRight();
						//si je suis bloqué 
						if(v != 0 ) {
							this.agent.activeProcessUnblocking((String)couple.getLeft(), this.map.getPosition(),nextDist,v);
							this.finished = true;
							return;
						}
						else {
							((mas.abstractAgent) agent).moveTo((String) couple.getLeft());
						}
						
					}
				}
			}
		}
		catch(Exception e) {e.printStackTrace();}
	}
	
	@SuppressWarnings("unchecked")
	private boolean treasureEnDanger(String position) {
		Node n = this.map.getNode(position);
		if(n == null) return false;
		List<MyCouple> attr = (List<MyCouple>) n.getAttribute("contents");
		for(MyCouple c : attr) {
			//si ce T/D est de mon type est son valeur > 0 return true
			if(((String)c.getLeft()).equals(((mas.abstractAgent)this.myAgent).getMyTreasureType()) && c.getRight() > 0 )return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private boolean not_valid_Destination() {
		AgentCollector agent = (AgentCollector) this.myAgent;
		if(agent.getNextTarget() == null || agent.getNexTreasure() == null ) return true;
		List<MyCouple> att = (List<MyCouple>) agent.getMap().getNode(agent.getNextTarget() ).getAttribute("contents");
		for(MyCouple at : att){
			//si sur ma carte ce T/D est encore enregistre avec meme valeur que avant return false
			if(((String)at.getLeft()).equals((String)agent.getNexTreasure().getLeft()) && at.getRight() == agent.getNexTreasure().getRight()) {
				return false;}
		}
		return true;
	}
	
	@Override
	public boolean done() {return this.finished;}
}
