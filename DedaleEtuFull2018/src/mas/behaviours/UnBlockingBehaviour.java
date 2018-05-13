package mas.behaviours;



import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.graphstream.algorithm.AStar;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;

import jade.core.AID;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import mas.agents.AgentCollector;
import mas.agents.AgentExplorateur;
import utils.BlocageUtils;
import utils.Comparateur;
import utils.DfUtils;
import utils.MyCouple;
import utils.TargetWrongException;


//A voir effet de onenode individuel et by groupe


public class UnBlockingBehaviour extends AbstractBehaviour{

	private static final long serialVersionUID = 8304831091790352039L;
	private boolean finished = false;
	private BlocageUtils bu;
	private mas.abstractAgent myagent;
	private String lastBehaviour;
	private HashMap<String,ArrayList<String>> chemins = new HashMap<String,ArrayList<String>>();
	private HashMap<String,ArrayList<String>> glums = new HashMap<String,ArrayList<String>>();
	private HashMap<String,String> names_act = new HashMap<String,String>();
	private HashMap<String,Double> capacitys = new HashMap<String,Double>();
	private HashMap<String,ArrayList<String>> nodeblocks = new HashMap<String,ArrayList<String>>();
	private HashMap<String,String> targets = new HashMap<String,String>();
	private HashMap<String,String> positions = new HashMap<String,String>();
	private HashMap<String,String> firstpositions = new HashMap<String,String>();
	private ArrayList<String> stenchNodes = new ArrayList<String>();
	private HashMap<String,String> ackmessage = new HashMap<String,String>();
	private HashMap<String,HashMap<String,Object>> othermsgs = new HashMap<String,HashMap<String,Object>>();
	private ArrayList<Node> cheminFinalNode = new ArrayList<Node>();
	private TreeMap<String,Double> prioritysdefault;
	private HashMap<String,ArrayList<String>> cheminfinals = new HashMap<String,ArrayList<String>>();
	private HashMap<String,ArrayList<String>> casavoids = new HashMap<String,ArrayList<String>>();
	private TreeMap<String,Double> prioritys;
	private HashMap<String,ArrayList<String>> possible = new HashMap<String,ArrayList<String>>();
	private HashMap<String,Object> mymessage = new HashMap<String,Object> ();
	private int nbagent = 0;


	


	
	


	public UnBlockingBehaviour(final mas.abstractAgent myagent,String lastBehaviour,BlocageUtils bu ) {
		super(myagent);
		this.bu = bu;
		this.myagent = myagent;
		this.lastBehaviour = lastBehaviour;
	}

	
	
	@Override
	public void action() {
		boolean b = true;
		boolean affiche = false;
		//trouver les nodes infecté par GLUM dans notre autour
		this.stenchNodes = this.bu.getStenchNodes();
		//si on est pas en train aller vers nodeRDV , on essaye trouver un autre target (soit un autre node pour vister
		//en mode exploration soir un autre T/D pour rammaser en mode collection)
		if(!bu.getTypeSerach().equals("nodeRDV") && !bu.getTypeSerach().equals("other") && bu.is_anotherTarget(stenchNodes) ) {
			if(affiche)System.out.println(this.myagent.getLocalName()+" Find new target"+this.bu.getMap().getChemin()+" "+((AgentExplorateur)this.myagent).getNextTarget()+" "+this.bu.getPosition());
			//si in a trouvé ub autre target on relance dernier Behaviour qui etait avtive avec nouvelle target et nouvelle chemin pour aller a ce node
			((AgentExplorateur)this.myagent).setUnBlockingBehaviourOFF(null);
		}else {
			boolean cause = bu.cause_is_Glum(stenchNodes);
			/*boolean onenodenonviste = this.verificationForOneAgent();
			if(cause && onenodenonviste) {
				System.out.println("just un node non visité pour  "+this.myagent.getLocalName()+" node est "+this.bu.getTarget());
				((AgentExplorateur)this.myagent).setUnBlockingBehaviourOFF(null);
				this.finished = true;
				return;
			}*/
			
			
			//si glum est ici essaye de trouver un chemin just propre sans avoir target
			if(bu.is_anoher_path_clean(stenchNodes)) {
				if(affiche)System.out.println(this.myagent.getLocalName()+" Find clean path"+this.bu.getMap().getChemin()+" "+((AgentExplorateur)this.myagent).getNextTarget()+" "+this.bu.getPosition());
				((AgentExplorateur)this.myagent).setUnBlockingBehaviourOFF("JustWalkBehaviour");
				
			}else  {
				ArrayList<DFAgentDescription[] > resultats = new ArrayList<DFAgentDescription[] >();
				this.nbagent  = DfUtils.ensmebleAgents(this.myagent,resultats);
				//si deux permier cas n'a pas reussit et on a plus que un agent passer par communication
				if(this.nbagent > 1) {
					//créé mon message
					this.mymessage= this.build_message();
					//communication
					this.communication(this.mymessage,resultats,this.nbagent -1);
					if(affiche)System.out.println("4 "+this.myagent.getLocalName()+" "+(new Date()).getTime());
					if(affiche)System.out.println(this.myagent.getLocalName()+" a recu message "+this.othermsgs.keySet().size());
					if(affiche)System.out.println(this.myagent.getLocalName()+" a envoye proprement "+this.ackmessage.keySet().size());
					//vider la boit aux lettres
					this.removeMailBox();
				}
				
				b = true;
				if(othermsgs.keySet().size() > 0 ) {
					
					if(cause) {
						boolean one = this.verificationForAllAgent();
						if(one) {
							System.out.println("just one node non visite pour  "+this.myagent.getLocalName()+" node est "+this.bu.getTarget());
							((AgentExplorateur)this.myagent).setUnBlockingBehaviourOFF(null);
							this.finished = true;
							return;
						}
						
					}
					//creé les chemin pour sorti de position blocage 
					this.buildUNBlockingWays(cause);
					if(this.cheminFinalNode.size() > 0) {
						b = false;
						this.bu.getMap().setChemin(this.cheminFinalNode);
						((AgentExplorateur)this.myagent).setNextTarget(this.cheminFinalNode.get(this.cheminFinalNode.size()-1).getId());
						if(this.bu.getTypeSerach().equals("nextTreasure")) {
							((AgentCollector)this.myagent).setNexTreasure(null);
						}
						((AgentExplorateur)this.myagent).setUnBlockingBehaviourOFF("JustWalkBehaviour");
						if(affiche)System.out.println("contruit path by communication "+this.myagent.getLocalName()+"  "+this.bu.getMap().getChemin()+" "+((AgentExplorateur)this.myagent).getNextTarget()+" "+this.bu.getPosition());
					}
					
				}else if(b){
					b = false;
					ArrayList<String> path = new ArrayList<String>();
					//path alea, dans le cas que on a pas crée un chemin en communication ou dans le cas que on a pas recu les messages des autres
					b = this.bu.build_path(this.bu.getPosition(),null, null,path,3,30,0,true);
					boolean b1 = true;
					if(b) {
						b1 = this.bu.set_path(path,true);
						if(b1) {
							b1 = false;
							if(affiche)System.out.println("6 just walk alea  "+this.myagent.getLocalName()+" "+this.bu.getMap().getChemin() +" "+((AgentExplorateur)this.myagent).getNextTarget()+" "+this.bu.getPosition());
							((AgentExplorateur)this.myagent).setUnBlockingBehaviourOFF("JustWalkBehaviour");
							
						}
						
					}else if(b1) {
						//dans pire de cas quand il y a pas aussi chemin alea , relacer dernier Behaviour sans rien chengé (ce cas arrive rarment)
						if(affiche)System.out.println("Non Walk "+this.myagent.getLocalName() +" "+(new Date()).getTime()+" "+this.bu.getMap().getChemin()+" "+this.bu.getMap().getPosition());
						((AgentExplorateur)this.myagent).setUnBlockingBehaviourOFF(null);
					}
					
				}
				
			}
			this.finished = true;
		}

		this.finished = true;
	}
	



	//si mon target est un node non visté est ce node est le seul non visité je change etat de ce node
	/*private boolean verificationForOneAgent() {
		int nb = this.bu.getMap().getNbNodeNonVisite();
		if(nb > 1) return false;
		Node n = this.bu.getMap().getNode(this.bu.getTarget());
		if(n == null ) return false;
		boolean visit = (boolean)n.getAttribute( "visite");
		if(!visit && this.lastBehaviour.equals("MovementExplorationBehaviour")) {
			n.setAttribute("visite", true);
			((AgentExplorateur)this.myagent).addToForcevisite(n.getId());
			return true;
		}
		return false;
	}*/


	//si tous les agent ont meme target et Behaviour est MovementExplorationBehaviour est ce node n'est pas visté on just change l'eta viste de ce node
	private boolean verificationForAllAgent() {
		int nb = this.bu.getMap().getNbNodeNonVisite();
		if(nb > 1 ) return false;
		ArrayList<String> list = new ArrayList<String>();
		for(String id : this.targets.keySet()) {
			if(!list.contains(this.targets.get(id))) list.add(this.targets.get(id));
		}
		if(list.size() == 1 ) {
			Node n = this.bu.getMap().getNode(list.get(0));
			if(n == null ) return false;
			boolean visit = (boolean)n.getAttribute( "visite");
			if(!visit ) {
				boolean flag = true;
				for(String id : this.names_act.keySet()) {
					if(!this.names_act.get(id).equals("MovementExplorationBehaviour")) {
						flag = false;
					}
				}
				if(flag) {
					if(!(boolean)n.getAttribute("visite")) {
						n.setAttribute("visite", true);
						((AgentExplorateur)this.myagent).addToForcevisite(n.getId());
						return true;
					}
					
				}
				
			}
		}
		return false;
	}



	private void buildUNBlockingWays( boolean cause) {
		//lire les message des autres qui sont enregistre dans othermsgs
		this.initialise(this.othermsgs);
		this.raffinerPositionGlum();
		this.raffinerNodesBlocks();
		this.priorityOfAgent(cause);
		this.whoBlockedWho(cause);
		this.buildPathFinal();
		
	}
	

	private void raffinerPositionGlum() {
		boolean flag;
		for(String id : this.glums.keySet()) {
			ArrayList<String> list = new ArrayList<String>();
			ArrayList<String> myglum = this.glums.get(id);
			for(String n: myglum) {
				flag = false;
				for(String id1 : this.glums.keySet()) {
					if(!id.equals(id1)) {
						ArrayList<String> otherglum = this.glums.get(id1);
						if(otherglum.contains(n)) {
							flag = true;
							break;
						}
					}
				}
				if(!flag)list.add(n);
			}
			myglum.removeAll(list);
			this.glums.put(id, myglum);
		}
	
	}



	private void raffinerNodesBlocks() {
		for(String id : this.nodeblocks.keySet()) {
			ArrayList<String> nb = this.nodeblocks.get(id);
			ArrayList<String> list = new ArrayList<String>();
			for(String n : nb) {
				if(!this.positions.values().contains(n) && !this.glums.get(id).contains(n)) {
					list.add(n);
				}
			}
			nb.removeAll(list);
			this.nodeblocks.put(id, nb);
		}
		
	}



	private void buildPathFinal() {
		ArrayList<String> c = this.cheminfinals.get(this.myagent.getLocalName());
		for(String id : c) {
			this.cheminFinalNode.add(this.bu.getMap().getNode(id));
		}
		
	}
	private void whoBlockedWho(boolean cause) {
		for(String name : this.names_act.keySet()) {
			this.casavoids.put(name, new ArrayList<String>());
			this.cheminfinals.put(name, new ArrayList<String>());
		}
		this.cheminFinalNode = new ArrayList<Node>();
		this.calcul_chemins(cause);
	}
	
	private boolean calcul_chemins(boolean cause) {

		if(this.is_terminate()) return true;
		this.order_by_cases_possible(cause);
		ArrayList<String> poi;
		ArrayList<String> thispath;
		ArrayList<String> thiscasavoid;
		String lastpos;
		String tmp = null;
		boolean flag = false;
		for(String thisname : this.prioritys.keySet()) {
			thispath = this.cheminfinals.get(thisname);
			thiscasavoid = this.casavoids.get(thisname);
			try{if(this.prioritys.get(thisname) == -1.0) {throw new TargetWrongException( " il y a un node non voision");}
    		}catch(TargetWrongException ex){System.out.println(ex.toString());}
			
			if(this.prioritys.get(thisname) != -1.0) {
				poi = this.possible.get(thisname);
				if(poi.size() > 0) {
					flag = true;
					tmp = this.best(poi,thisname);
					lastpos = this.positions.get(thisname);
					thispath.add(tmp);
					this.cheminfinals.put(thisname, thispath);
					this.positions.put(thisname, tmp);
					thiscasavoid.add(lastpos);
					this.casavoids.put(thisname, thiscasavoid);
				}
				
			}
		}
		if(flag)return calcul_chemins(cause);
		return flag;
	}
	
	
	private String best(ArrayList<String> poi, String thisname) {

		if(poi.size() == 1) return poi.get(0);
		ArrayList<String> mynode = new ArrayList<String>();
		for(String p: poi) {
			if(! this.glums.get(thisname).contains(p) && !this.nodeblocks.get(thisname).contains(p)) {
				mynode.add(p);
			}
		}
		if(mynode.size() == 0) {
			for(String p: poi) {
				if(!this.nodeblocks.get(thisname).contains(p) ) {
					mynode.add(p);
				}
			}
			
			if(mynode.size() == 0) {
				for(String p: poi) {
					if(! this.glums.get(thisname).contains(p) ) {
						mynode.add(p);
					}
				}
			}
			if(mynode.size() == 0) {
				mynode = poi;
			}
		}
		HashMap<String,Double> p = new HashMap<String,Double>();
		
		double nb;
		for(String id : mynode) {
			nb = this.count_nb_way(id);
			p.put(id, nb);
		}
		double min = 100000.0;
		for(String id : p.keySet()) {
			if(p.get(id)< min) {
				min = p.get(id);
			}
		}
		ArrayList<String> list = new ArrayList<String>();
		for(String id : p.keySet()) {
			if(p.get(id).compareTo(min) == 0) {
				list.add(id);
			}
		}
		if(this.bu.getTarget() != null) {
			min = 10000.0;
			String idmini = null;
			AStar astar = new AStar(this.bu.getMap().getGraph());
			Path path = null;
			List<Node> chemin = null;
			for (int i = 0; i < list.size(); i++) {
				String n = list.get(i);
				try {
					astar.compute(n,this.bu.getTarget());
					path = astar.getShortestPath();
					if(path != null) {
						chemin = path.getNodePath();
						if(min > (double)chemin.size()) {
							min = (double)chemin.size();
							idmini = n;
						}
						
					}
				}catch(Exception e) {
					e.printStackTrace();
					continue;
				}
			}
			if(idmini != null) return idmini;
		}
		
		Collections.shuffle(list);
		return list.get(0);
		
	}
	

	private void order_by_cases_possible(boolean cause ) {

		Graph g = this.bu.getMap().getGraph();
		HashMap<String,Double> p = new HashMap<String,Double>();
		Comparateur comp =  new Comparateur(p,true);
        this.prioritys = new TreeMap<String,Double>(comp);
        int nb = 0;
		String thispos;
		Node courant;
		String tmp;
		boolean flag = false;
		Iterator<Node> it; 
		for(String name : this.names_act.keySet()) {
			ArrayList<String> poi = new ArrayList<String>();
			nb = 0;
			flag = true;
			thispos = this.positions.get(name);
			courant = g.getNode(thispos);
			if(courant == null) {
				flag = false;
				
			}else {
				it = courant.getNeighborNodeIterator();
				while(it.hasNext()) {
					tmp = it.next().getId();
					if(this.is_valide_choice(tmp,name)){
						nb += 1;
						poi.add(tmp);
					}
				}
			}
			
			if(flag) {
				this.possible.put(name, poi);
				p.put(name, (double)nb);
			}else {
				p.put(name,-1.0);
			}
			
		}
		HashMap<String,ArrayList<String>> doubles = this.find_doubles(p);
		this.order_by_priority(doubles,p,cause);
		this.prioritys.putAll(p);
	}
	
	private void order_by_priority(HashMap<String, ArrayList<String>> doubles, HashMap<String, Double> p,boolean cause) {
		if(cause) {
			this.order_by_priority_by_glum(doubles,p);
		}else {
			this.order_by_priority_without_glum(doubles,p);
		}
		
	}



	private void order_by_priority_without_glum(HashMap<String, ArrayList<String>> doubles, HashMap<String, Double> p) {
		ArrayList<String> d = null;
		boolean stable = false;
		while(!stable) {
			stable = true;
			for(String id : doubles.keySet()) {
				d = doubles.get(id);
				for(String v : d) {
					if(p.get(id).equals(p.get(v))) {
						stable = false;
						if(this.prioritysdefault.get(id).compareTo((double)this.prioritysdefault.get(v)) <0  ) {
							p.put(id, (double)p.get(v)+ 0.1);
						}
						else if(this.prioritysdefault.get(id).compareTo((double)this.prioritysdefault.get(v)) >0){
							p.put(v, (double)p.get(id)+ 0.1);
						}
						else {
							if(id.compareTo(v) > 0) {
								p.put(id, (double)p.get(v)+ 0.1);
							}else {
								p.put(v,(double) p.get(id)+ 0.1);
							}
						}
					}
					
				}
			}
		}

	}



	private void order_by_priority_by_glum(HashMap<String, ArrayList<String>> doubles, HashMap<String, Double> p) {
		ArrayList<String> d = null;
		boolean stable = false;
		while(!stable) {
			stable = true;
			for(String id : doubles.keySet()) {
				d = doubles.get(id);
				for(String v : d) {
					if(p.get(id).equals(p.get(v))) {
						stable = false;
						if(this.glums.get(id).size() > this.glums.get(v).size()) {
							p.put(id,(double) p.get(v)+ 0.1);
						}
						else if(this.glums.get(id).size() < this.glums.get(v).size()){
							p.put(v,(double) p.get(id)+ 0.1);
						}
						else {
							if(this.prioritysdefault.get(id).compareTo((double)this.prioritysdefault.get(v)) <0) {
								p.put(id, (double)p.get(v)+ 0.1);
							}
							else if(this.prioritysdefault.get(id).compareTo((double)this.prioritysdefault.get(v)) >0){
								
								p.put(v,(double) p.get(id)+ 0.1);
							}
							else {
								if(id.compareTo(v) > 0) {
									p.put(id,(double) p.get(v)+ 0.1);
								}else {
									p.put(v,(double) p.get(id)+ 0.1);
								}
							}
						}
					}
					
				}
			}
		}
	}



	private HashMap<String, ArrayList<String>>  find_doubles(HashMap<String, Double> p) {

		HashMap<String, ArrayList<String>> doubles = new HashMap<String, ArrayList<String>>();
		ArrayList<String> d;
		for(String name: this.names_act.keySet()) {
			doubles.put(name, new ArrayList<String>());
		}
		for(String name : p.keySet()) {
			for(String name1 : p.keySet()) {
				if(! name.equals(name1) && p.get(name).equals((double)p.get(name1)) && ((double)p.get(name))> 0.0) {
					d = doubles.get(name);
					d.add(name1);
				}
			}
		}
		return doubles;
	}
	
	private boolean is_valide_choice(String tmp,String me) {
		
		ArrayList<String> mycaseavoid = this.casavoids.get(me);
		for(String cas : mycaseavoid) {
			if(cas.equals(tmp)) return false;
		}
		for(String cas : this.positions.keySet()) {
			if(this.positions.get(cas).equals(tmp)) return false;
		}
		return true;
	}

	private double count_nb_way(String id) {
		ArrayList<String> path;
		double nb = 0.0;
		for(String name : this.cheminfinals.keySet()) {
			path = this.cheminfinals.get(name);
			for(String node : path) {
				if(node.equals(id)) {
					nb+= 10.0;
				}
			}
		}
		return nb;
	}
	
	private boolean is_terminate() {
		for(String name : this.targets.keySet()) {
			if(!this.targets.get(name).equals(this.positions.get(name))) return false;
		}
		return true;
	}
	
	private void priorityOfAgent(boolean cause) {
		HashMap<String,Double> p = new HashMap<String,Double>();
		Comparateur comp =  new Comparateur(p,true);
        this.prioritysdefault = new TreeMap<String,Double>(comp);
		int prioritybyrole;
		double prioritybyact ;
		int prioritybyglum ;
		double prioritbychemin;
		double priority;
		for(String agentname :names_act.keySet()) {
			prioritybyrole = this.getprioritybyrole(agentname);
			prioritybyact = this.getprioritybyact(names_act.get(agentname),agentname);
			prioritybyglum =  this.getprioritybyglum(this.glums.get(agentname));
			prioritbychemin = this.getprioritybypath(this.chemins.get(agentname));
			priority =  (double ) (prioritybyrole *  prioritybyact * prioritybyglum * prioritbychemin);
			p.put(agentname,priority);
		}
	
        this.prioritysdefault.putAll(p);
	}
	




	private double getprioritybypath(ArrayList<String> path) {
		int size = path.size();
		if(size == 0) return 1;
		return (double) (1.0/ ((double)size));
	}

	private int getprioritybyglum(ArrayList<String> glum) {
		int size = glum.size();
		if(size > 0) return size;
		return 1;
	}

	private double getprioritybyact(String act,String agengtName) {
		if(act.equals("MovmentForTreasureBehaviour")) {
			if(this.capacitys.containsKey(agengtName) && this.capacitys.get(agengtName) < 1.0) {
				return 5.5;
			}
			return 6.0;
		}
		if(act.equals("MovmentTankerBehaviour")) return 3.0;
		if(act.equals("LivrerBehaviour")) return 5.0;
		if(act.equals("MovementExplorationBehaviour")) return 4.0;
		if(act.equals("RandomWalkBehaviour")) return 2.0;
		return 1;
	}

	private int getprioritybyrole(String role) {
		if(role.startsWith("AgentTanker")) return 2;
		if(role.startsWith("AgentCollect")) return 3;
		return 1;
		
	}
	
	private void initialise(HashMap<String, HashMap<String, Object>> other) {
		for(HashMap<String, Object> msg : other.values()) {
			String name =(String) msg.get("name");
			String act = (String)msg.get("behaviour");
			this.names_act.put(name,act);
			String chemin = (String)msg.get("chemin");
			String[] cheminlist = chemin.split("-");
			ArrayList<String> chmeinarray = new ArrayList<String>();
			for(String c : cheminlist) {
				chmeinarray.add(c);
			}
			chmeinarray.remove(chmeinarray.size()-1);
			this.chemins.put(name,chmeinarray);
			String glum = (String)msg.get("Stench");
			String[] glumlist = glum.split("-");
			ArrayList<String> glumarray = new ArrayList<String>();
			for(String c : glumlist) {
				glumarray.add(c);
			}
			glumarray.remove(glumarray.size()-1);
			this.glums.put(name,glumarray);
			String nodeblock = (String)msg.get("nodeblock");
			String[] parts = nodeblock.split("-");
			ArrayList<String> nodes = new ArrayList<String>();
			for(String s : parts) {
				nodes.add(s);
			}
			nodes.remove(nodes.size()-1);
			this.nodeblocks.put(name,nodes);
			String target = (String)msg.get("target");
			this.targets.put(name,target);
			String position =(String) msg.get("posiotion");
			this.positions.put(name, position);
			this.firstpositions.put(name, position);
			double cap = (double)msg.get("cap");
			if(cap > 0.0)this.capacitys.put(name, cap);
		}
	}
	
	private void removeMailBox() {
		MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE);
		ACLMessage msg = null;
		do {msg = ((mas.abstractAgent) this.myagent).receive(msgTemplate);}while(msg != null);
		msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF);
		do {msg = ((mas.abstractAgent) this.myagent).receive(msgTemplate);}while(msg != null);	
	}
	
	private void communication(HashMap<String, Object> m,ArrayList<DFAgentDescription[] > resultats,int nbAgent) {
		int nbTry = 2;
		while(nbTry > 0) {
			nbTry -= 1;
			this.sendMessage(resultats,m);
			this.reciveMessage(nbAgent);
			if(this.othermsgs.keySet().size() == nbAgent && this.ackmessage.keySet().size()== nbAgent) break;
		}
	}
	
	@SuppressWarnings("unchecked")
	private void reciveMessage(int nbAgent) {
		MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE); 
		HashMap<String,HashMap<String, HashMap<String, HashMap<String, Object>>>> onemsg = null;
		HashMap<String, HashMap<String, HashMap<String, Object>>> mapseri = null;
		HashMap<String, Object> info = null;
		ACLMessage msg = null;
		for(int i = 0 ; i< nbAgent; i++) {
			msg = ((mas.abstractAgent) this.myagent).blockingReceive(msgTemplate, 150);
			if(msg != null) {
				onemsg = null;
				try {
					onemsg = (HashMap<String,HashMap<String, HashMap<String, HashMap<String, Object>>>>) msg.getContentObject();
					if(onemsg != null) {
						this.sendAcquittal(msg.getSender().getLocalName());
						mapseri = (HashMap<String, HashMap<String, HashMap<String, Object>>>)onemsg.get("map");
						if(mapseri != null) {
							this.bu.getMap().unifier(mapseri, msg.getSender().getLocalName(),this.myagent.getLocalName());
						}else {
							System.out.println("Map recu est null");
						}
						info =(HashMap<String, Object>) onemsg.get("info").get("info").get("info");
						othermsgs.put(msg.getSender().getLocalName(), info);
					}
					else {
						System.out.println("onemsg null");
					}
				} catch (UnreadableException e) {
					e.printStackTrace();
					continue;
				}
			}
			
			this.reviceAcquittal();
		}
		
	}
	private void sendAcquittal(String receiverName) {
		final ACLMessage msg = new ACLMessage(ACLMessage.INFORM_REF);
		msg.setSender(((mas.abstractAgent)this.myAgent).getAID());
		msg.addReceiver(new AID(receiverName, AID.ISLOCALNAME));
		msg.setContent("recived");
		((mas.abstractAgent)this.myAgent).sendMessage(msg);
	}
	
	private void sendMessage(ArrayList<DFAgentDescription[]> resultats, HashMap<String, Object> m) {
		ACLMessage msg = null;
		HashMap<String,HashMap<String, HashMap<String, HashMap<String, Object>>>> messag = new HashMap<String,HashMap<String, HashMap<String, HashMap<String, Object>>>>();
		HashMap<String, HashMap<String, HashMap<String, Object>>> mapseri = null;
		for(DFAgentDescription[] res : resultats) {
			for(DFAgentDescription receiver: res){
				if(! ((mas.abstractAgent) this.myagent).getAID().toString().equals((receiver.getName()).toString()) && !this.ackmessage.containsKey(receiver.getName().getLocalName())) {
					msg = new ACLMessage(ACLMessage.PROPAGATE);
					msg.setSender(this.myagent.getAID());
					msg.addReceiver(new AID((receiver.getName()).getLocalName().toString(), AID.ISLOCALNAME));
					mapseri = this.bu.getMap().shareMap(receiver.getName().getLocalName(),true);
					if(mapseri== null) {
						System.out.println("map null");
					}
					messag.put("map", mapseri);
					HashMap<String, HashMap<String, HashMap<String, Object>>> info = new  HashMap<String, HashMap<String, HashMap<String, Object>>>();
					HashMap<String, HashMap<String, Object>> minfo = new  HashMap<String, HashMap<String, Object>>();
					minfo.put("info", m);
					info.put("info", minfo);
					messag.put("info", info);
					try {
						msg.setContentObject(messag);
					} catch (IOException e) {
						e.printStackTrace();
						continue;
					}
					((mas.abstractAgent) this.myagent).sendMessage(msg);
				}
			}
		
		}
	}
	private void reviceAcquittal() {
		final MessageTemplate msgTemplate =  MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF);
		ACLMessage msg = null;
		msg = ((mas.abstractAgent) this.myagent).blockingReceive(msgTemplate, 50);
		if(msg != null)this.ackmessage.put(msg.getSender().getLocalName(), msg.getSender().getLocalName());
	}

	private HashMap<String,Object> build_message() {
		HashMap<String,Object> msg = new HashMap<String,Object>();
		String name =  this.myagent.getLocalName();
		msg.put("name",name );
		msg.put("behaviour", this.lastBehaviour);
		msg.put("target", this.bu.getTarget());
		this.targets.put(name, this.bu.getTarget());
		this.names_act.put(name,this.lastBehaviour);
		msg.put("nodeblock", this.bu.getNodeblock());
		this.nodeblocks.put(name, this.bu.getNodeblocks());
		msg.put("posiotion", this.bu.getPosition());
		this.positions.put(name, this.bu.getPosition());
		this.firstpositions.put(name, this.bu.getPosition());
		String chemin ="";
		ArrayList<String> mychemin = new ArrayList<String>();
		if(this.bu.getLast_chemin() != null) {
			for(Node n : this.bu.getLast_chemin()) {
				chemin += n.getId()+"-";
				mychemin.add(n.getId());
			}
		}
		this.chemins.put(name, mychemin);
		chemin +="end";
		msg.put("chemin", chemin);
		String s ="";
		for(String id : this.stenchNodes) {
			s+= id+"-";
		}
		s +="end";
		this.glums.put(name, this.stenchNodes);
		msg.put("Stench", s);
		double cap = 0.0;
		if(name.startsWith("AgentCollector") && this.lastBehaviour.equals("MovmentForTreasureBehaviour") ) {
			MyCouple my = ((AgentCollector)this.myagent).getNexTreasure();
			if(my != null) {
				int v = my.getRight();
				int c = this.myagent.getBackPackFreeSpace();
				if(v > c ) {
					cap = (v -c )/ v;
				}
				else {
					cap = 1.0;
				}
			}
		}
		msg.put("cap", cap);
		if(cap > 0.0) {
			this.capacitys.put(name, cap);
		}
		return msg;
	}
	
	@Override
	public boolean done() {return this.finished;}

}
