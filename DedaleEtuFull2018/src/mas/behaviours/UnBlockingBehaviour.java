package mas.behaviours;



import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

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
	private boolean modeMaitreSlave;
	private boolean prioritysdefaultDone = false;


	


	
	


	public UnBlockingBehaviour(final mas.abstractAgent myagent,String lastBehaviour,BlocageUtils bu,boolean modeMaitreSlave ) {
		super(myagent);
		this.bu = bu;
		this.myagent = myagent;
		this.lastBehaviour = lastBehaviour;
		this.modeMaitreSlave = modeMaitreSlave;
	}

	
	
	@Override
	public void action() {
		boolean b = true;
		boolean affiche = false;
		this.readNormalMessage();
		this.verificationForTanker();
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
				System.out.println("just un node non visité pour solo "+this.myagent.getLocalName()+" node est "+this.bu.getTarget());
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
					if(affiche)System.out.println(this.myagent.getLocalName()+" a recu message "+this.othermsgs.keySet().size());
					if(affiche)System.out.println(this.myagent.getLocalName()+" a envoye proprement "+this.ackmessage.keySet().size());
				}
				
				b = true;
				
				if(othermsgs.keySet().size() > 0 ) {
					this.initialise(this.othermsgs);
					if(this.casMasterSlave() && this.isExplorationPhase()) {
						((AgentExplorateur)this.myagent).setUnBlockingBehaviourOFF(null);
						this.finished = true;
						return;
					}
					if(cause) {
						boolean one = this.verificationForAllAgent();
						if(one) {
							System.out.println("just one node non visite pour  "+this.myagent.getLocalName()+" node est "+this.bu.getTarget());
							((AgentExplorateur)this.myagent).setUnBlockingBehaviourOFF(null);
							this.finished = true;
							return;
						}
						
					}
					if(!cause && this.modeMaitreSlave  ) {
						if( this.casMasterSlave() ) {
							this.buildWayMasterSlave();
							if(this.cheminFinalNode.size() > 0) {
								this.bu.getMap().setChemin(this.cheminFinalNode);
								((AgentExplorateur)this.myagent).setNextTarget(this.cheminFinalNode.get(this.cheminFinalNode.size()-1).getId());
								if(this.bu.getTypeSerach().equals("nextTreasure")) {
									((AgentCollector)this.myagent).setNexTreasure(null);
								}
								((AgentExplorateur)this.myagent).setUnBlockingBehaviourOFF("JustWalkBehaviour");
								if(affiche)System.out.println("contruit path by communication master slave "+this.myagent.getLocalName()+"  "+this.bu.getMap().getChemin()+" "+((AgentExplorateur)this.myagent).getNextTarget()+" "+this.bu.getPosition());
								if(affiche)System.out.println(this.cheminfinals.toString());
								this.finished = true;
								return;
							}
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
							if(affiche)System.out.println("just walk alea  "+this.myagent.getLocalName()+" "+this.bu.getMap().getChemin() +" "+((AgentExplorateur)this.myagent).getNextTarget()+" "+this.bu.getPosition());
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


	



	private boolean isExplorationPhase() {
		for(String id : this.names_act.keySet()) {
			if(!this.names_act.get(id).equals("MovementExplorationBehaviour")) return false;
		}
		for(String id : this.targets.keySet()) {
			String nID = this.targets.get(id);
			Node n = this.bu.getMap().getNode(nID);
			if(n == null || !(boolean)n.getAttribute("visite")) return false;
			
		}
		return true;
	}



	private boolean casMasterSlave() {
		if(this.ackmessage.keySet().size() != this.othermsgs.keySet().size()) return false;
		//if( this.othermsgs.keySet().size() > 1) return false;
		boolean flag;
		for(String id: this.othermsgs.keySet()) {
			flag = false;
			for(String id1 : this.ackmessage.keySet()) {
				if(id.equals(id1)) {
					flag = true;
					break;
				}
			}
			if(!flag) return false;
			
		}
		for(String id: this.ackmessage.keySet()) {
			flag = false;
			for(String id1 : this.othermsgs.keySet()) {
				if(id.equals(id1)) {
					flag = true;
					break;
				}
			}
			if(!flag) return false;
			
		}
		for(String id : this.glums.keySet()) {
			if(this.glums.get(id).size() > 0) return false;
		}
		return true;
	}



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
					n.setAttribute("visite", true);
					((AgentExplorateur)this.myagent).addToForcevisite(n.getId());
					return true;
				}
				
			}
		}
		return false;
	}
	
	private boolean buildWayMasterSlave() {
	
		this.priorityOfAgent(false);
		this.prioritysdefaultDone = true;
		Entry<String,Double> ent = this.prioritysdefault.firstEntry();
		String idMaster = ent.getKey();
		for(String id : this.prioritysdefault.keySet()) {
			ArrayList<String> way = new ArrayList<String>();
			way.add(this.positions.get(id));
			this.cheminfinals.put(id, way);
		}
		Graph G = this.bu.getMap().getGraph();
		AStar astar = new AStar(G);
		HashMap<String,ArrayList<String>> pathOrginals = new HashMap<String,ArrayList<String>>();
		Path path = null;
		for(String id : this.prioritysdefault.keySet()) {
			if(! id.equals(idMaster)) {
				String pos = this.positions.get(id);
				astar.compute(pos,this.targets.get(idMaster));
				path = astar.getShortestPath();
				if(path == null) return false;
				ArrayList<String> pathOrginal = new ArrayList<String>();
				for(Node n : path.getNodePath()) {
					pathOrginal.add(n.getId());
				}
				pathOrginals.put(id, pathOrginal);
			}
			else {
				pathOrginals.put(id,  new ArrayList<String>());
			}

		}
		return this.buildWayMasterSlaverec(pathOrginals,idMaster,this.prioritysdefault.keySet().size()-1,false,0,0);
	}
	
	private boolean buildWayMasterSlaverec(HashMap<String,ArrayList<String>> pathOrginals ,String idMaster ,int start,boolean b,int nbrepaet,int nbTrue) {
		ArrayList<String> masterWay = this.cheminfinals.get(idMaster);
		if(masterWay.get(masterWay.size()-1).equals(this.targets.get(idMaster)) || nbrepaet>= 100 ) {
			return this.finalBuild();
		}
		ArrayList<String> possibleSlave = new ArrayList<String>();
		ArrayList<String> avoidSlave = new ArrayList<String>();
		String idSlave = null;
		int k = 0;
		if(start > 0) {
			for(String id : this.prioritysdefault.keySet()) {
				if(k == start) {
					idSlave = id;
					break;
				}
				k += 1;
			}
			k = 0;
			int s = start;
			while(possibleSlave.size() <= 0) {
				avoidSlave = new ArrayList<String>();
				possibleSlave = new ArrayList<String>();
				for(String id : this.cheminfinals.keySet()) {
					if(k < s) {
						avoidSlave.addAll(this.cheminfinals.get(id));
					}
					k += 1;
				}
				
				boolean b1 = this.casespossibleSlave(pathOrginals.get(idSlave),avoidSlave,this.cheminfinals.get(idSlave),possibleSlave,false,idSlave);
				if(b1) nbTrue+=1;
				if(!b && nbTrue >= this.names_act.keySet().size()-1) b = true;
				if(possibleSlave.size() <= 0) {
					boolean aut = this.is_autorise(pathOrginals.get(idSlave));
					if(aut ) {
						 b1 = this.casespossibleSlave(pathOrginals.get(idSlave),avoidSlave,this.cheminfinals.get(idSlave),possibleSlave,true,idSlave);
						if(b1) nbTrue+=1;
						if(!b && nbTrue >= this.names_act.keySet().size()-1) b = true;
					}
				}
				
				s -= 1;
				if(s < 0)break;
				
			}
			if(possibleSlave.size() > 0) {
				String nextSlave = this.bestForSlave_Master(possibleSlave,idMaster,false);
				if(nextSlave != null) {
					ArrayList<String> slaveWay = this.cheminfinals.get(idSlave);
					slaveWay.add(nextSlave);
					this.cheminfinals.put(idSlave, slaveWay);
				}
				
			}
		}
		else {
			ArrayList<String> possibleMaster = new ArrayList<String>();
			this.casespossibleMaster(masterWay,possibleMaster,b);
			if(0 < possibleMaster.size() ) {
				String nextMaster =  this.bestForSlave_Master(possibleMaster,idMaster,true);
				if(nextMaster != null) {
					masterWay.add(nextMaster);
					this.cheminfinals.put(idMaster, masterWay);
				}
				
				
			}
		}
		
		start -= 1;
		if(start < 0) start = this.names_act.size()-1;

		return buildWayMasterSlaverec(pathOrginals,idMaster,start,b,nbrepaet+1,nbTrue);

	}
	


	private boolean is_autorise(ArrayList<String> list) {
		Graph G = this.bu.getMap().getGraph();
		ArrayList<String> pos = new ArrayList<String>();
		for(String id : this.cheminfinals.keySet()) {
			ArrayList<String> way = this.cheminfinals.get(id);
			pos.add(way.get(way.size()-1));
		}
		for(String id : list) {
			Node n = G.getNode(id);
			if(n == null) return false;
			Iterator<Node> it = n.getNeighborNodeIterator();
			while(it.hasNext()) {
				String v = it.next().getId();
				if(!list.contains(v) && ! pos.contains(v)  ) return true;
			}
		}
		return false;
	}



	private boolean finalBuild() {
		for(String id : this.cheminfinals.keySet()) {
			ArrayList<String> way = this.cheminfinals.get(id);
			if(way.size() > 0) {
				way.remove(0);
				this.cheminfinals.put(id, way);
			}
		}
	
		this.buildPathFinal();
		return true;
		
	}

	private void casespossibleMaster(ArrayList<String> masterWay,
			ArrayList<String> possible,boolean aut) {
		Graph G = this.bu.getMap().getGraph();
	
		String posMaster = masterWay.get(masterWay.size()-1);
		Node cou = G.getNode(posMaster);
		ArrayList<String> pos = new ArrayList<String>();
		for(String id : this.cheminfinals.keySet()) {
			ArrayList<String> w = this.cheminfinals.get(id);
			pos.add(w.get(w.size()-1));
		}
		Iterator<Node> it= cou.getNeighborNodeIterator();
		while(it.hasNext()) {
			String n = it.next().getId();
			if(!pos.contains(n) ) {
				if(aut || !masterWay.contains(n)) {
					possible.add(n);
				}
			}
			
		}
	}

	private boolean casespossibleSlave(ArrayList<String> pathOrginal,ArrayList<String> masterWay, ArrayList<String> slaveWay,
			ArrayList<String> possible,boolean aut,String idslave) {
		Graph G = this.bu.getMap().getGraph();
		boolean flag  = false;
		String posSlave = null;
		ArrayList<String> pos = new ArrayList<String>();
		for(String id : this.cheminfinals.keySet()) {
			ArrayList<String> way = this.cheminfinals.get(id);
			String p = way.get(way.size()-1);
			pos.add(p);
			if(id.equals(idslave))posSlave= p;
		}
	
		Node cou = G.getNode(posSlave);
		if(cou == null) return false;
		Iterator<Node> it= cou.getNeighborNodeIterator();
		while(it.hasNext()) {
			String n = it.next().getId();
			if(! pos.contains(n)  && !slaveWay.contains(n) ) {
				if(aut ||  !pathOrginal.contains(n) ) {
					if(!masterWay.contains(n)  ) {
						possible.add(n);
						flag = true;
					}
				}
				
			}
			
		}
		if(!flag) {
			it= cou.getNeighborNodeIterator();
			while(it.hasNext()) {
				String n = it.next().getId();
				if(! pos.contains(n) && !slaveWay.contains(n) ) {
					if(aut ||  ! pathOrginal.contains(n) ) {
						possible.add(n);
					}
					
				}
				
			}
		}
		if(possible.size() > 1) {
			ArrayList<String> list = new ArrayList<String>();
			ArrayList<String> ids = new ArrayList<String>();
			for(String id : this.prioritysdefault.keySet()) {ids.add(id);}
			boolean flag1 = true;
			boolean flag2 = true;
			while(list.size() <= 0 && flag1 ) {
				for(String c : possible) {
					flag2 = true;
					for(String id : ids) {
						if(this.cheminfinals.get(id).contains(c)) {
							flag2 = false;
							break;
						}
					}
					if(flag2) list.add(c);
				}
				int size = ids.size();
				if(size > 0) {
					ids.remove(size -1);
				}
				else {
					flag1 = false;
				}
			}
			if(list.size() > 0 && list.size() < possible.size() ) {
				possible = list;
			}
		}
		return flag;
	}

	private String bestForSlave_Master(ArrayList<String> possible,String idMaster,boolean master) {
		if(possible.size() == 1) return possible.get(0);
		int s= 0;
		int maxi = -1;
		if(master) {
			maxi = 10000;
		}
		HashMap<String,Integer> ids_val = new HashMap<String,Integer> ();
		Graph G = this.bu.getMap().getGraph();
		Path path = null;
		for(String n : possible) {
			if(n.equals(this.targets.get(idMaster))) return n;
			AStar astar = new AStar(G);
			astar.compute(n,this.targets.get(idMaster));
			path = astar.getShortestPath();
			if(path != null) {
				s = path.getNodeCount();
				ids_val.put(n, s);
			}
			
		}
		if(ids_val.keySet().size() <= 0) return null;
		for(String id : ids_val.keySet()) {
			int v = ids_val.get(id);
			if(master) {
				if(v <= maxi) {
					maxi = v;
				}
			}else {
				if(v >= maxi) {
					maxi = v;
				}
			}
		}
		ArrayList<String> p = new ArrayList<String>();
		for(String id: ids_val.keySet()) {
			int v = ids_val.get(id);
			if(v == maxi) {
				p.add(id);
			}
		}
		Collections.shuffle(p);
		return p.get(0);
	}



	private void buildUNBlockingWays( boolean cause) {
		//lire les message des autres qui sont enregistre dans othermsgs
		this.raffinerPositionGlum();
		this.raffinerNodesBlocks();
		if(! this.prioritysdefaultDone) {
			this.priorityOfAgent(cause);
		}
		this.whoBlockedWho(cause);
		this.buildPathFinal();
		
	}
	

	private void verificationForTanker() {
		if(this.myagent.getLocalName().startsWith("AgentCollector") && this.myagent.getBackPackFreeSpace() < ((AgentCollector)this.myagent).getCapMaxBackPack()) {
			DFAgentDescription[] result = DfUtils.searchExplorer("AgentTanker",(mas.abstractAgent)this.myAgent);
			if(result != null) {
				for(DFAgentDescription ag: result){
					((mas.abstractAgent)this.myAgent).emptyMyBackPack(ag.getName().getLocalName());
				}
			}
		}
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
	private void order_default_priority(HashMap<String, Double> p, HashMap<String, ArrayList<String>> doubles,
			boolean cause) {
		if(cause) {
			this.order_default_priority_by_glum(doubles,p);
		}else {
			this.order_default_priority_without_glum(doubles,p);
		}
		
	}


	



	private void order_default_priority_without_glum(HashMap<String, ArrayList<String>> doubles,
			HashMap<String, Double> p) {
		ArrayList<String> d = null;
		boolean stable = false;
		while(!stable) {
			stable = true;
			for(String id : doubles.keySet()) {
				d = doubles.get(id);
				for(String v : d) {
					if(p.get(id).equals(p.get(v))) {
						stable = false;
						if(Math.random() > 0.5) {
							p.put(id, (double)p.get(v)+ 0.1);
						}else {
							p.put(v,(double) p.get(id)+ 0.1);
						}
					}
					
				}
			}
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
							if(Math.random() > 0.5) {
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

	private void order_default_priority_by_glum(HashMap<String, ArrayList<String>> doubles, HashMap<String, Double> p) {
		boolean stable = false;
		ArrayList<String> d = null;
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
							
							if(Math.random() > 0.5) {
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
								if(Math.random() > 0.5) {
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
		HashMap<String, ArrayList<String>> doubles = this.find_doubles(p);
		this.order_default_priority(p,doubles,cause);
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
			double cap = Double.parseDouble((String) msg.get("cap"));
			if(cap > 0.0)this.capacitys.put(name, cap);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void removeMailBox() {
		MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE);
		HashMap<String,HashMap<String, HashMap<String, HashMap<String, Object>>>> onemsg = null;
		HashMap<String, HashMap<String, HashMap<String, Object>>> mapseri = null;
		HashMap<String, Object> info = null;
		ACLMessage msg = null;
		do {
			msg = ((mas.abstractAgent) this.myagent).receive(msgTemplate);
			if(msg != null) {
				if(msg != null) {
					onemsg = null;
					try {
						onemsg = (HashMap<String,HashMap<String, HashMap<String, HashMap<String, Object>>>>) msg.getContentObject();
						if(onemsg != null) {
							info =(HashMap<String, Object>) onemsg.get("info").get("info").get("info");
							othermsgs.put(msg.getSender().getLocalName(), info);
							Long d = (Long)info.get("date");
							this.sendAcquittal(msg.getSender().getLocalName(),d);
							mapseri = (HashMap<String, HashMap<String, HashMap<String, Object>>>)onemsg.get("map");
							if(mapseri != null) {
								this.bu.getMap().unifier(mapseri, msg.getSender().getLocalName(),this.myagent.getLocalName());
							}else {
								System.out.println("Map recu est null");
							}
							
						}
						else {
							System.out.println("onemsg null");
						}
					} catch (UnreadableException e) {
						e.printStackTrace();
						continue;
					}
				}
			}
			
		}while(msg != null);
		msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF);
		do {
			msg = ((mas.abstractAgent) this.myagent).receive(msgTemplate);
			if(msg != null) {
				this.ackmessage.put(msg.getSender().getLocalName(), msg.getSender().getLocalName());
			}
		}while(msg != null);	
	}
	
	private void communication(HashMap<String, Object> m,ArrayList<DFAgentDescription[] > resultats,int nbAgent) {
		this.removeOldMessage();
		int nbTry = 2;
		while(nbTry > 0) {
			nbTry -= 1;
			this.sendMessage(resultats,m);
			this.reciveMessage(nbAgent);
		}
		this.removeMailBox();
	}
	private void removeOldMessage() {
		MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF);
		ACLMessage msg = null;
		do {msg = ((mas.abstractAgent) this.myagent).receive(msgTemplate);}while(msg != null);
		
	}



	@SuppressWarnings("unchecked")
	private void readNormalMessage() {
		MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
		ACLMessage msg = null;
		Serializable newMap = null;
		HashMap<String, HashMap<String, HashMap<String, Object>>> m = null;
		Long date;
		
		do {
			msg = ((mas.abstractAgent) this.myagent).receive(msgTemplate);
			if (msg != null) {
				try {
					newMap = msg.getContentObject();
				} catch (UnreadableException e) {
					e.printStackTrace();
					continue;
				}
				if(newMap != null) {
					m = (HashMap<String, HashMap<String, HashMap<String, Object>>>) newMap;
					if(m != null) {
						date =  (Long) m.get("date").get("date").get("date");
						// envoi d'un acquittement a l'envoyeur
						((mas.abstractAgent) this.myagent).addBehaviour(new SendAcquittalReciveMap((mas.abstractAgent) myagent, msg.getSender().getLocalName(),date ));
						this.bu.getMap().unifier(m,msg.getSender().getLocalName(),this.myagent.getLocalName());
					}
				}	
			}
		}while( msg != null );
	}
	
	@SuppressWarnings("unchecked")
	private void reciveMessage(int nbAgent) {
		
		
		MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE);
		HashMap<String,HashMap<String, HashMap<String, HashMap<String, Object>>>> onemsg = null;
		HashMap<String, HashMap<String, HashMap<String, Object>>> mapseri = null;
		HashMap<String, Object> info = null;
		ACLMessage msg = null;
		for(int i = 0 ; i< nbAgent; i++) {
			msg = ((mas.abstractAgent) this.myagent).receive(msgTemplate);
			if(msg != null) {
				onemsg = null;
				try {
					onemsg = (HashMap<String,HashMap<String, HashMap<String, HashMap<String, Object>>>>) msg.getContentObject();
					if(onemsg != null) {
						
						mapseri = (HashMap<String, HashMap<String, HashMap<String, Object>>>)onemsg.get("map");
						if(mapseri != null) {
							Long d =(Long) mapseri.get("date").get("date").get("date");
							this.sendAcquittal(msg.getSender().getLocalName(),d);
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
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			this.reviceAcquittal();
		}
		
	}
	private void sendAcquittal(String receiverName,Long date) {
		try {
			final ACLMessage msg = new ACLMessage(ACLMessage.INFORM_REF);
			msg.setSender(((mas.abstractAgent)this.myAgent).getAID());
			msg.addReceiver(new AID(receiverName, AID.ISLOCALNAME));
			msg.setContentObject(date);
			((mas.abstractAgent)this.myAgent).sendMessage(msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		
		}
	}
	private void reviceAcquittal() {
		final MessageTemplate msgTemplate =  MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF);
		ACLMessage msg = null;
		msg = ((mas.abstractAgent) this.myagent).receive(msgTemplate);
		if(msg != null) {
			Long date = null;
			try {
				date = (Long) msg.getContentObject();
				this.ackmessage.put(msg.getSender().getLocalName(), msg.getSender().getLocalName());
				this.bu.getMap().updateLastCommunication(msg.getSender().getLocalName(), date);
			} catch (UnreadableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private HashMap<String,Object> build_message() {
		HashMap<String,Object> msg = new HashMap<String,Object>();
		Long date = (new Date()).getTime();
		msg.put("date", date);
		String name =  this.myagent.getLocalName();
		msg.put("name",name );
		msg.put("behaviour", this.lastBehaviour);
		msg.put("target", this.bu.getTarget());
		this.targets.put(name, this.bu.getTarget());
		this.names_act.put(name,this.lastBehaviour);
		msg.put("nodeblock", this.bu.getNodeblock());
		this.nodeblocks.put(name, this.bu.getNodeblocks());
		msg.put("posiotion", this.bu.getMap().getPosition());
		this.positions.put(name, this.bu.getMap().getPosition());
		this.firstpositions.put(name, this.bu.getMap().getPosition());
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
		msg.put("cap", String.valueOf(cap));
		if(cap > 0.0) {
			this.capacitys.put(name, cap);
		}
		return msg;
	}
	
	@Override
	public boolean done() {return this.finished;}

}
