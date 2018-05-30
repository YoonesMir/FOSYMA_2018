package mas.behaviours;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;



import org.graphstream.algorithm.AStar;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;

import mas.agents.AgentCollector;
import utils.CommonUtils;
import utils.Map;
import utils.MyCouple;
import utils.MyFive;


public class FindNextTreasureBehaviour extends AbstractBehaviour {

	private static final long serialVersionUID = 9214290581051729970L;
	private Map map;
	private boolean finished = false;
    
	
	public FindNextTreasureBehaviour(final mas.abstractAgent myagent ) {
		super(myagent);
		this.map = ((AgentCollector)myagent).getMap() ;
	}


	@Override
	public void action() {
	
		AgentCollector agent = (AgentCollector) this.myAgent;
		int cap = Integer.valueOf(((mas.abstractAgent) agent).getBackPackFreeSpace());
		//si la capacite restant est zéro commence directement livrer
		if( cap == 0) {
			agent.setStartlivrer(true);
			CommonUtils.addNextBehaviour((mas.abstractAgent)this.myAgent,false);
			finished = true;
		}
		//si encore place libre
		else {
			this.map.setPosition();
			String position = this.map.getPosition();
			if(!position.equals("")) {
				this.map.setChemin(null);
				agent.setNextTarget(null);
				agent.setNexTreasure(null);
				MyFive five = new MyFive();
				int v = find_targe(position,five,null,agent,this.map);
				//si il n'y a plus Treasure/Diamond
				if(v == 0) {
					//si deja qqch dans pack commencer livrer
					if(cap < agent.getCapMaxBackPack()) {
						agent.setStartlivrer(true);
						CommonUtils.addNextBehaviour((mas.abstractAgent)this.myAgent,false);
					}//sinon finish
					else {agent.setCollectionFinished(true);}
				}
				//si encore Treasure/Diamond sur la carte 
				else {
					boolean b = fixe_next_Treasure(five);
					//b = true , je veux aller chercher ce Treasure/Diamonds donc activer MovmentForTreasureBehaviour
					if(b) {
						agent.setStartlivrer(false);
					}else {
						//sinon commecncer livrer , car je suis sûr que si je veux pas aller chercher ce Treasure/Diamonds 
						//ça veut dire que j'ai deja qqch dans mon backpack et 
						//la veleur de ce Treasure/Diamonds trouvé est bcp plus grand que capacité restant donc 
						//je le touche pas pour pas perdre le valuer de ce Treasure/Diamonds
						agent.setStartlivrer(true);
					}
					CommonUtils.addNextBehaviour((mas.abstractAgent)this.myAgent,false);
				}
				finished = true;
			}
		}
	}
		
	//méthod static car on le utilise dans BlocageUtils aussi
	//idTreasureList est une list de Treasure/Diamonds que on veux pas aller chercher
	//ce list peut etre null dans ce cas on trouve meilleur Treasure/Diamonds 
	@SuppressWarnings("unchecked")
	public static int find_targe(String mypostion,MyFive mine,ArrayList<String> idTreasureList,AgentCollector agentCollect,Map map0) {
		
		//construit une list de tous les Treasure/Diamonds disponibles sur la carte
		//avec valeur plus grand que zero et de type de mon backpack
		//qui ne son pas dans list idTreasureList
		ArrayList<MyFive> list = new ArrayList<MyFive>();
		Graph graph = map0.getGraph();
		Path mypath = null;
		int dist = 0;
		int mybackfree = Integer.valueOf(((mas.abstractAgent) agentCollect).getBackPackFreeSpace());
		ArrayList<Node> listNode = new ArrayList<Node>();
		for(Node n : graph.getNodeSet()){
			if(idTreasureList != null &&  idTreasureList.contains(n.getId()) ) {continue;}
			listNode.add(n);
				
		}
		Collections.shuffle(listNode);
		for(Node n : listNode) {
			dist = 0;
			mypath = null;
			List<MyCouple>  att = (List<MyCouple> )n.getAttribute("contents");
			for(MyCouple at : att){
				if(((String)at.getLeft()).equals(((mas.abstractAgent) agentCollect).getMyTreasureType() ) && at.getRight() > 0){
					AStar astar = new AStar(graph);
					try {
						astar.compute(mypostion,n.getId());
						mypath = astar.getShortestPath();
						if(mypath != null) {
							dist += mypath.size();
						}else {
							continue;
						}
					}catch(Exception e) {
						e.printStackTrace();
						continue;
					}
					int valeur = at.getRight();
					//chaque fois que on touche un T/D si son valuer est plus grand que mon cap , je perds environ 30% de valeur restant
					double perte = 0.0;
					//si le valeru de T/D est plus grand que mon capicité on pénalise son valuer 
					//idée est aller chercher tout d'abord les T/D qui sont fit à mon capacité
					if(valeur > mybackfree ) {
						perte = (valeur -mybackfree ) * 0.3;
						valeur = mybackfree;
					}
					list.add(new MyFive(n.getId(),((double)(valeur * 1.0))/((double)dist),perte,at));
				}
			}
		}
		//on vas chercher le T/D qui minimise le perte et maximise de rapport entre valeur de T/D et distace de ce node à moi
		//list est une liste de object MyFive , 
		//chaque MyFive contient 5 information
		//1: id de node , 2, rapport de valeur-distance 
		//3,perte de valeur de T/D apres le touché 0 si valeur de T/D est plus petit ou égale à mon cap
		//, 4 attribut sur node , 5 identifient de agent collector qui a decider aller checher T/D sur ce node
		if(list.size() > 0){
			if(list.size() == 1) {
				mine.copy(list.get(0));
				return 1;
			}
			
			//ordonée list selon ordre lexo , minimiser perte et maximise rapport
			Collections.sort(list);
			String smaller = null;
			HashMap<String, Object> caps = map0.getCaps();
			HashMap<String, Object> capscopy = new HashMap<String, Object>();
			for(String id: caps.keySet()) {
				capscopy.put(id, caps.get(id));
			}
			map0.setCaps(capscopy);
			ArrayList<String> listcopy = new ArrayList<String>();
			int mycap = Integer.parseInt((String)caps.get(agentCollect.getMyTreasureType()+"-"+(agentCollect).getLocalName()));
			int othercap = 0;
			ArrayList<String> listequal = null;
			for(MyFive five : list) {
				if(! listcopy.contains(five.getFirst())) {
					smaller = null;
					listequal = new ArrayList<String>();
					for(String id : caps.keySet()) {
						if(id.split("-")[0].equals((String)five.getAttribute().getLeft()) && ! id.split("-")[1].equals(agentCollect.getLocalName())) {
							othercap = Integer.parseInt((String)caps.get(id));
							if(othercap < mycap) {
								smaller = id;
								break;
							}
							else if(othercap == mycap ) {
								listequal.add(id);
							}
						}
					}
					if(smaller == null) {
						if(listequal.size() == 0) {
							mine.copy(list.get(0));
							return 1;
						}
						else {
							String idmin = listequal.get(0);
							for(int i = 1 ; i < listequal.size(); ++i) {
								if(listequal.get(i).compareTo(idmin) < 0) {
									idmin = listequal.get(i);
								}
							}
							if(agentCollect.getLocalName().equals(idmin)) {
								mine.copy(list.get(0));
								return 1;
							}
							else {
								smaller =  idmin;
							}
						}
						
					}else {
						listcopy.add(five.getFirst());
						caps.remove(smaller);
					}
				}
				
				
			}
			//Si en je sort de boucle en haut ca veut dire que tous les T/D trouvé sont marqué par qqn autre donc je choisi le meilleur
			mine.copy( list.get(list.size()-1));
			return 1;
		}
		//si il n'y pas T/D à chercher 
		else {return 0;}
	}

	private boolean fixe_next_Treasure(MyFive myFive) {
		try {
			AgentCollector agent = (AgentCollector) this.myAgent;
			int cap =((mas.abstractAgent) agent).getBackPackFreeSpace();
			MyCouple my = myFive.getAttribute();
			int val = my.getRight();
			//si jai déja qqch dans mon backpack et mon cap restant est plus petit que 80% de le velur de truc trouvé je vais pas chercher ce truc
			if(cap < agent.getCapMaxBackPack() && cap < (0.8 * val)) return false;
			agent.setNextTarget(myFive.getFirst());
			agent.setNexTreasure(myFive.getAttribute());
			System.out.println(agent.getLocalName()+" chose : "+myFive.toString());
			return true;
		}catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	@Override
	public boolean done() {return finished;}
}
