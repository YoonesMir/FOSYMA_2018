package utils;

import java.util.*;

import mas.agents.*;
import env.Attribute;
import env.Couple;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.util.leap.Serializable;
import mas.abstractAgent;
import utils.MyCouple;
import utils.Sextuple;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.graph.Edge;
import org.graphstream.algorithm.AStar;





public class Map  implements Serializable {
	
	private static final long serialVersionUID = 1933843861223424738L;
	
	private abstractAgent myAgent;
	
	private Graph graph;
	private HashMap<String, Long> communicationHistory;
	private HashMap<String,String> positionOtherAgents;
	private HashMap<String, Long> dateOfPositionOtherAgents;
	private HashMap<String, Object> caps;
	
	//last_Position est le position avant dernier de l'agent : ça nous aide pour trouver les sitaution blocage
	private String last_Position;
	private String myPosition; 
	private String last_move; 
	
	private int treasureValeur= 0;
	private int diamondsValeur = 0;
	//blockingNodes est une liste qui enregistre  l'endroit où on etait bloqué
	private ArrayList<String> blockingNodes;
	//c'est ensemble des dernier movment que on voulait faire mais on n'a pas pu bouger
	private ArrayList<String> setOfLastMove = new ArrayList<String> ();
	

	//nodeRDV est le node que les collectors et tanker ils se retrouvent
	//ce node est trouvé une fois que exploration est términé
	//il peut etre changer dans le cas de blocage
	private String nodeRDV = null;
	//Chemin est :une list de node que agnet doit parcourir pour arriver à son distination 
	private List<Node> chemin;
	
	private boolean explorationFinished;
	private boolean firstTimeExploration;
	
	private HashMap<String,Integer> forcevisite;
    private HashMap<String,Integer> normalvisite;
    private HashMap<String,Integer> repetforcevisite;
    private int nbNodeLast;
    
    private boolean glum;
	
    
   
	

	
	//constructure
	public Map(abstractAgent myAgent){
		
		this.myAgent = myAgent;
		this.graph =  new SingleGraph("Carte");
		this.communicationHistory =  new HashMap<String, Long>();
		this.positionOtherAgents = new HashMap<String,String> ();
		this.dateOfPositionOtherAgents = new HashMap<String,Long> ();
		this.myPosition = null;
		this.last_Position = null;
		this.blockingNodes = new ArrayList<String>();
		this.chemin = new ArrayList<Node>();
		this.setLast_move(null);
		this.setFirstTimeExploration(true);
		this.setExplorationFinished(false);
		this.forcevisite = new HashMap<String,Integer>();
		this.normalvisite = new HashMap<String,Integer>();
		this.repetforcevisite = new HashMap<String,Integer>();
		this.caps = new HashMap<String, Object>();
		setGlum(false);
		setNbNodeLast(0);
		
	}
	public void setCapsDefaul() {
        if(this.myAgent instanceof AgentCollector) {
        	String type = this.myAgent.getMyTreasureType()+"-"+this.myAgent.getLocalName();
        	String val = Integer.toString(((AgentCollector)this.myAgent).getCapMaxBackPack());
        	this.caps.put(type, val);
        }
        
	}
	public int getNbNodeLast() {
		return nbNodeLast;
	}
	public void setNbNodeLast(int nbNodeLast) {
		this.nbNodeLast = nbNodeLast;
	}
	public boolean isGlum() {
		return glum;
	}

	public void setGlum(boolean glum) {
		this.glum = glum;
	}
	
	public int sizeForcevisite() {
    	return this.forcevisite.size();
    }
    
    public void addToForcevisite(String id) {
    	if(!this.normalvisite.containsKey(id) && (!this.repetforcevisite.containsKey(id) || this.repetforcevisite.get(id) < 3)) {
    		forcevisite.put(id,0);
    	}
    }
    
    public void ens_normal_visite() {
		for (Node n : this.getGraph().getNodeSet()) {
			if(!this.forcevisite.containsKey(n.getId())) {
				this.normalvisite.put(n.getId(), 1);
			}
		}
		for(String id : this.forcevisite.keySet()) {
			if(this.repetforcevisite.containsKey(id)) {
				int n = this.repetforcevisite.get(id);
				this.repetforcevisite.put(id, n + 1);
			}else {
				this.repetforcevisite.put(id,  1);
			}
		}
		this.forcevisite.clear();
	}
	
	public Object getAttribute(String idNode, String att) {
		Node n = this.getNode(idNode);
		if(n== null)return null;
		return n.getAttribute(att);
	}
	
	 public boolean isFirstTimeExploration() {
			return firstTimeExploration;
	}

	public void setFirstTimeExploration(boolean firstTimeExploration) {
			this.firstTimeExploration = firstTimeExploration;
	}


	// verifier est-ce que les n derniére element d'une liste sont la meme choses
	private boolean seemNodes(int n) {
		int size = this.blockingNodes.size();
		if(size < n) return false;
		for(int j = size-1 ; j > (size-n); j--) {
			if(!this.blockingNodes.get(j).equals(this.blockingNodes.get(j-1)))return false;
		}
		return true;
	}

	//changer etat visite de tous les nodes 
	public void setAllNodes(boolean b) {
		Long date = (new Date()).getTime();
		for(Node n : this.graph.getNodeSet()) {
			n.setAttribute("visite", b);
			n.setAttribute("date", date);
		}
	}
	
	
	
	
	public String getLast_move() {
		return last_move;
	}
	private void setLast_move(String last_move) {
		this.last_move = last_move;
	}
	

	private void setBlockingNodes(ArrayList<String> nodesBlocage) {
		this.blockingNodes = nodesBlocage;
	}
	
	private void addToBlockingNodes(String node) {
		this.blockingNodes.add(node);
	}
	
	//Si exploration est fini on initialise aussi node RDV
	public void setExplorationFinished(boolean b) {
		this.explorationFinished = b;
		if(b)this.setNodeRDV(this.nodeRDV(null));
		
	}
	
	public boolean getExplorationFinished() {
		return this.explorationFinished;
	}
	
	public int getTreasureValeur() {
		return treasureValeur;
	}
	public void setTreasureValeur(int treasureValeur) {
		this.treasureValeur = treasureValeur;
	}
	
	public int getDiamondsValeur() {
		return diamondsValeur;
	}
	public void setDiamondsValeur(int diamondsValeur) {
		this.diamondsValeur = diamondsValeur;
	}
	
	public String getNodeRDV() {
		return this.nodeRDV;
	}
	public void setNodeRDV(String nodeRDV) {
		this.nodeRDV = nodeRDV;
	}
	

	
	public Graph getGraph(){
		return this.graph;
	}

	//return : node de l'id passé en params, Dan le cas Exception : return null
	public Node getNode(String nodeID){
		try {
			return this.graph.getNode(nodeID);
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	//return nombre des nodes
	public int nbNodes() {
		try{
			return this.graph.getNodeSet().size();
		}
		catch(Exception e) {
			e.printStackTrace();
			return 0;
		}
	}
	//return nombre nodes non visté sur la carte
	public int getNbNodeNonVisite() {
		int nb = 0;
		for(Node n : this.graph.getNodeSet()) {
			if(!(boolean) n.getAttribute("visite")) nb+=1 ;
		}
		return nb;
	}
	
	//trouve position courant sur la carte : Dans le cas exception qui vients  souvent fixer le position par ""
	public void setPosition(){
        try {
        	this.myPosition = this.myAgent.getCurrentPosition();
        }catch(Exception e) {
        	this.myPosition = "";
        }
	}
	
	
	//return position courant
	public String getPosition() {
		return this.myPosition;
	}
	
	
	public void setChemin(List<Node> chemin){
		this.chemin = chemin;
	}
	
	public List<Node> getChemin() {
		return this.chemin;
	}
	
	//
	public void setLastPosition(String pos) {
		this.last_Position = pos;
		
	}
	public String getLastPosition() {
		return this.last_Position;
		
	}
	
	//return dernier position de tanker si on le sait par les message que en 200 milisecods dernieron a récu sinon null , 
	public String getPostionTanker(DFAgentDescription[] result) {
		Long date = (new Date()).getTime();
		if(result != null) {
			for(DFAgentDescription ag: result){
				if(this.positionOtherAgents.containsKey(ag.getName().getLocalName())  && (date - this.dateOfPositionOtherAgents.get(ag.getName().getLocalName()) )<= 1000) {
					return this.positionOtherAgents.get(ag.getName().getLocalName());
				}
			}
		}
		
		return null;
	}
	//Entress : deux list attribut
	//return true si les deux list ne sont pas exactment similiare sinon false
	private boolean new_information_about_attributes(List<MyCouple> newattribute, List<MyCouple> oldattribute) {
		
		if(newattribute.size() != oldattribute.size()) return true;
		boolean flag = false;
		//trouve si il y a qqch dans le oldattribute  qui n'existe pas dans newattribute
		for(MyCouple a : oldattribute) {
			flag = false;
			for(int i = 0; i < newattribute.size(); i++) {
				if(((String)newattribute.get(i).getLeft()).equals((String)a.getLeft())) {
					if(newattribute.get(i).getRight() == a.getRight()) {
						flag = true;
						break;
						
					}
				}
			}
			if(!flag) return true;
		}
		//Et l'inverse
		for(MyCouple a : newattribute) {
			flag = false;
			for(int i = 0; i < oldattribute.size(); i++) {
				if(((String)oldattribute.get(i).getLeft()).equals((String)a.getLeft())) {
					if(oldattribute.get(i).getRight() == a.getRight()) {
						flag = true;
						break;
						
					}
				}
			}
			if(!flag) return true;
		}
		return false;
	}
	
	
	
	// met a jour la carte lors de la visite d'un noeud
	//local = true on visite que node de position 
	//local = false , on visite node de position est les voision
	//retur true si on observe GLUM sinon false
	@SuppressWarnings("unchecked")
	public  boolean visiter(boolean local){
		this.setPosition();
		boolean danger = false;
		List<Couple<String,List<Attribute>>> lobs;
		try {
			lobs =((mas.abstractAgent)this.myAgent).observe();
		}catch(Exception e) {e.printStackTrace(); return false;}
		
		Long d = (new Date()).getTime();
		List<Attribute> att;
		Node n;
		String c;
		for (Couple<String,List<Attribute>> couple: lobs){
			c = (String)couple.getLeft();
            n = null;
            try {
            	n  = this.graph.getNode(c);
            }catch(Exception e) {
    			e.printStackTrace();
    			continue;
    		}
            att = (List<Attribute>)couple.getRight();
            List<MyCouple> attr = new ArrayList<MyCouple>();
            for(Attribute a : att){
            	if(a.getName().equals("Stench")) {
              		danger = true;
            	}
            	else {
            		attr.add(new MyCouple((String)a.getName(),(int)a.getValue()));
            	}
            }
            //so on ne connait pas ce node
            if(n == null) {
        		n = this.graph.addNode(c);
        		n.setAttribute("date", d);
        		n.setAttribute("contents", attr);
            	if(n.getId().equals(this.myPosition)){
            		n.setAttribute("visite", true);
            	}else {
            		n.setAttribute("visite", false);
            	}
            }
            // si le noeud est deja cree
            else { 
            	//si ce n'est pas un voisin
	    		if( n.getId().equals(this.myPosition)) {
	    			//si on n'a pas déja visité
	    			if(! (boolean) n.getAttribute("visite")) {
	    				n.setAttribute("visite", true);
	        			n.setAttribute("contents", attr);
	        			n.setAttribute("date", d);
	    			}else { //si j'ai deja visite ce noeud on modifie information de node si il y a nouvelle information sur attributs
	    				List<MyCouple> myattribute =(List<MyCouple>) n.getAttribute("contents");
	    				if(new_information_about_attributes(attr,myattribute)) {
	    					n.setAttribute("contents", attr);
	            			n.setAttribute("date", d);
	    				}
	    			}
	    			
	    		}
            }       
		}
		n = null;
		Node courant = this.graph.getNode(this.myPosition);
		
		// ajout des arcs
		//si c'est un viste local , forcement on a deja fait viste golbal de ce node et donc on a cree les arcs
		if(!local) {
			for(Couple<String,List<Attribute>> couple: lobs) {
				c = couple.getLeft();
				n = this.graph.getNode(c);
				if(!n.getId().equals(courant.getId()) ) {
		        	try {
		        		if(this.graph.getEdge(n.getId()+courant.getId()) == null && this.graph.getEdge(courant.getId()+n.getId()) == null) {
		        			this.graph.addEdge( courant.getId() + n.getId(), courant.getId(), n.getId()).setAttribute("date", d);
		        		}
		            }catch (Exception e) {
		    			e.printStackTrace();
		                continue;
		            }
		        }
			}
		}
		if(danger) {this.setGlum(true);}
		return danger;
	}
	
	
	//b = true envoyer une version  de la cart meme si on fait que une seul midification , sinon envoyer carte tous les 10 modification
	public  HashMap<String, HashMap<String, HashMap<String, Object>>> shareMap(String agentID, boolean b) {
		int nbmodif = 0;
        Long d = (new Date()).getTime();
        HashMap<String, HashMap<String, HashMap<String, Object>>> mapprim = new HashMap<String, HashMap<String, HashMap<String, Object>>>();
        HashMap<String, HashMap<String, Object>> noeuds = new HashMap<String, HashMap<String, Object>>();
        HashMap<String, HashMap<String, Object>> aretes = new HashMap<String, HashMap<String, Object>>();
        
      //ADD nodes 
        for (Node n: this.graph.getNodeSet()) {
    		if(!this.communicationHistory.containsKey(agentID) ||  (Long) n.getAttribute("date") > this.communicationHistory.get(agentID)){
    			nbmodif += 1;
    			HashMap<String, Object> node_car = new HashMap<String, Object>();
    			for (String att: n.getAttributeKeySet()) {
    				node_car.put(att, n.getAttribute(att));
                }
                noeuds.put(n.getId(), node_car);
        	}
        }
        
        //ADD aretes
        for (Edge a: this.graph.getEdgeSet()) {
        	if(!this.communicationHistory.containsKey(agentID) || (Long) a.getAttribute("date") > this.communicationHistory.get(agentID)) {
        		nbmodif += 1;
        		HashMap<String, Object> arret_car = new HashMap<String, Object>();
        		arret_car.put("node0", a.getNode0().getId());
        		arret_car.put("node1", a.getNode1().getId());
        		arret_car.put("date", a.getAttribute("date"));
        		aretes.put(a.getId(), arret_car);
        	}
        }
        if(!b && nbmodif < 10) return null;
        
        mapprim.put("nodes", noeuds);
        mapprim.put("aretes", aretes);
        HashMap<String, HashMap<String, Object>> capsHashHash = new HashMap<>();
        capsHashHash.put("caps", this.caps);
        mapprim.put("caps", capsHashHash);
        //ADD date
        HashMap<String, HashMap<String, Object>> dateHashHash = new HashMap<>();
        HashMap<String, Object> dateHash = new HashMap<>();
        dateHash.put("date", d);
        dateHashHash.put("date", dateHash);
        mapprim.put("date", dateHashHash);
 
        //Add position
        HashMap<String, HashMap<String, Object>> posHashHash = new  HashMap<>();
        HashMap<String, Object> posHash = new HashMap<>();
        posHash.put("position", getPosition());
        posHashHash.put("position", posHash);
        mapprim.put("position", posHashHash);
        return mapprim;
    }
	
	
	
	@SuppressWarnings("unchecked")
	public  void unifier(HashMap<String, HashMap<String, HashMap<String, Object>>> newMap,String sender,String agentname) {
		boolean done = false;
		Long d = (Long)newMap.get("date").get("date").get("date");
		
		//Si le message recu est plus ancien que dernier message recu on fait rien
		if(this.dateOfPositionOtherAgents.containsKey(sender)  && this.dateOfPositionOtherAgents.get(sender) >= d) return;
		
		//sinon mis à jour
		for(String node : newMap.get("nodes").keySet()) {
			Node n = null;
			try {
				n = this.graph.getNode(node);
			}catch(Exception e) {
    			e.printStackTrace();
				continue;
			}
			
			//si j'ai pas ce node dans ma carte je le ajoute
			if(n == null ) {
				n = this.graph.addNode(node);
				for (String att: newMap.get("nodes").get(node).keySet()) {
	                n.setAttribute(att, newMap.get("nodes").get(node).get(att));
	            }
				//n.setAttribute("date", d);
			}
			//si j'ai deja ce node dans ma carte
			else {
				Long datenode = (Long)newMap.get("nodes").get(node).get("date");
				boolean othervisit = (boolean)newMap.get("nodes").get(node).get("visite");
				//si j'ai deja visité ce node
				if( (boolean)n.getAttribute("visite") ) {
					//si sender a visité ce node aussi
					if(othervisit) {
						//si son visite est plus recent que la mine
						if(datenode.compareTo((Long) n.getAttribute("date")) > 0 ) {
							List<MyCouple> myAttribute = (List<MyCouple>)n.getAttribute("contents");
							List<MyCouple> otherAttribute = (List<MyCouple>) newMap.get("nodes").get(node).get("contents");
							done = new_information_about_attributes(otherAttribute,myAttribute);
							//si il y a nouvelles information
							if(done) {
								n.setAttribute("contents", otherAttribute);
								//n.setAttribute("date", d);
								n.setAttribute("date", datenode);
							}
						}
					}

					
				}
				//si j'ai pas  visité  ce node
				else {
					//si sender a visité ce node
					if(othervisit) {
						//si c'est permier fois que Exploration de la carte
						if(isFirstTimeExploration()) {
							for (String att: newMap.get("nodes").get(node).keySet()) {
								n.setAttribute(att, newMap.get("nodes").get(node).get(att));
				            }
							//n.setAttribute("date", d);
						}
						///si j'ai deja fait une Exploration complet de lacarte et c'est le duxime fois
						else {
							if( datenode.compareTo((Long) n.getAttribute("date")) > 0 ) {
								for (String att: newMap.get("nodes").get(node).keySet()) {
					                n.setAttribute(att, newMap.get("nodes").get(node).get(att));
					                //n.setAttribute("date", d);
					                n.setAttribute("date", datenode);
					            }
							}
						}
					}
				}
			}
		}
		//add des arcs si ils sont pas deja enregistre
		for (String arete: newMap.get("aretes").keySet()) {
				try {
					Node n0 = this.graph.getNode((String) newMap.get("aretes").get(arete).get("node0"));
					Node n1 = this.graph.getNode((String) newMap.get("aretes").get(arete).get("node1"));
					if(n0 != null && n1 != null) {
						if(this.graph.getEdge(n0.getId()+n1.getId()) == null && this.graph.getEdge(n1.getId()+n0.getId()) == null) {
							this.graph.addEdge(arete, n0.getId(),n1.getId()).setAttribute("date",newMap.get("aretes").get(arete).get("date") );
						}
					}
	            }catch (Exception e) {
	    			e.printStackTrace();
	                continue;
	            }
			}
        
		//ajoute date et position d'agent sender dans la base de donne
		dateOfPositionOtherAgents.put(sender,d );
		positionOtherAgents.put(sender,(String) newMap.get("position").get("position").get("position") );
		HashMap<String, Object> capscopy = newMap.get("caps").get("caps");
	
		for(String id: capscopy.keySet()) {
			this.caps.put(id, capscopy.get(id));
			
		}

	}
	
	// renvoie le nombre d'agents situes a une distance a un noeud donné inferieure ou egale a la distance  de l'agent
	// prend uniquement en compte les agents dont la position a ete enregistree apres une date donné en params
	private MyCouple find_nb_Agnet(String nodeID, Long datelimite, int mydistance, ArrayList<String> list_shorter, ArrayList<String> list_equal){
		Long date = (new Date()).getTime();
		AStar astar = new AStar(this.graph);
		int nb_equal = 0;
		int nb_shorter = 0;
		Path otherpath = null;
		
		for (String mapKey : this.positionOtherAgents.keySet()) {
			if(this.dateOfPositionOtherAgents.containsKey(mapKey) && (date - this.dateOfPositionOtherAgents.get(mapKey)) <= datelimite && !this.positionOtherAgents.get(mapKey).equals("") ) {
				otherpath = null;
				try {
					astar.compute( this.positionOtherAgents.get(mapKey), nodeID);
					otherpath = astar.getShortestPath();
				}catch(Exception e){
	    			e.printStackTrace();
					continue;
				}
				if(otherpath != null) {
					if(mydistance == otherpath.size()) {
						nb_equal += 1;
						list_equal.add(mapKey);
					}
					if(mydistance > otherpath.size()) {
						nb_shorter += 1;
						list_shorter.add(mapKey);
					}
				}
			}	
		}
		return new MyCouple(nb_shorter, nb_equal);
	}
	
	
	
	// prend en compte les positions des autres agents pour calculer la meilleure DESTINATION
	public  String bestmove_with_Distance(){
		String mypostion = this.getPosition();
		Path mypath = null;
		AStar astar = new AStar(this.graph);
		ArrayList<Sextuple> list0 = new ArrayList<Sextuple>();
		//Cree la liste pour tous les nodes non visité , chaque element de liste est un Sixtuple
		//un Sixtuple contient , (Idnode, mon distance jusqua ce node , nombre des agents qui sont plus proche
		//à ce node que moi , nombre des agent qui ont meme distance à ce node que moi, list trie (ordre alphabetique des
		// id des agent qui sont plus proche à ce node que moi , liste trié (alphabetique ) des id des agents qui ont 
		//meme distance que moi jusqua ce node))
		ArrayList<Node> listNode = new ArrayList<Node>();
		for(Node n : this.graph.getNodeSet()){
			if(!(boolean)n.getAttribute("visite")){
				listNode.add(n);
			}
		}
		Collections.shuffle(listNode);
		for(Node n : listNode){
			try {
				astar.compute( mypostion,n.getId());
				mypath = astar.getShortestPath();
				if(mypath == null) continue;
			}catch(Exception e){
    			e.printStackTrace();
				continue;
			}
			ArrayList<String> list_equal = new ArrayList<String>();
			list_equal.add(this.myAgent.getLocalName());
			ArrayList<String> list_shorter = new ArrayList<String>();
			list_shorter.add(this.myAgent.getLocalName());
			int mydistance = mypath.size();
			MyCouple a = find_nb_Agnet(n.getId(),1000L,mydistance,list_shorter,list_equal);
			Collections.sort(list_equal);
			Collections.sort(list_shorter);
			list0.add(new Sextuple(n.getId(), mydistance, (int)a.getLeft(), a.getRight(), list_shorter, list_equal));
		}
		
		// s'il n'y a pas de noeuds Exploration est fini
		if(list0.size() == 0) return "finished";
		//Si il y a que une seul node non visite on n'a pas besoin faire calcule
		if(list0.size() == 1) return list0.get(0).getFirst();
		
		
		//trier la liste selon ordre l'exographie
		//Regarde method comparTo de la class Sextuple
		Collections.sort(list0);
		ArrayList<Sextuple> list1 = new ArrayList<Sextuple>();
		//return premier node tq il n'y a pas agent plus proche à  ce node que moi , si il exsite
		for(int i = 0 ; i < list0.size();i++) {
			Sextuple elem = list0.get(i);
			if(elem.getThird() > 0) {list1.add(elem);}
			else {
				return elem.getFirst();
			}
		}
		
		//si on est Ici : on sait que pout tous node non visite il y a des agens plus proche que moi a ces nodes
		if(list1.size() == 0) return list0.get(0).getFirst();
		ArrayList<Sextuple> list2 = new ArrayList<Sextuple>();
		
		//return premier node tq il n'y a pas agent avec meme distnace à ce node que moi
		for(int i = 0 ; i < list1.size();i++) {
			Sextuple elem = list1.get(i);
			if(elem.getFour() > 0) {list2.add(elem);}
			else {
				return elem.getFirst();
			}
		}
		//  ici : tous neouds non visites ont agents plus proches et a meme distance
		// on regarde les noeuds dans l'ordre et on leur associe l'agent le plus proche et on ne considere plus cet agent pour les autres noeuds
		// et ce jusqu'a tomber sur notre propre agent
		if(list2.size() == 0) return list0.get(0).getFirst();
		while(list2.size() > 0) {
			Sextuple elem = list2.get(0);
			String name = elem.getSix().get(0);
			if(this.myAgent.getLocalName().equals(name)) {
				return elem.getFirst();
			}else {
				list2.remove(0);
				for(Sextuple elem1 : list2){
					ArrayList<String> l = elem1.getSix();
					if(l.contains(name)) {
						l.remove(name);
					}
					elem1.setSix(l);
				}
			}
		}
		// dans le pire des cas on va vers le noeud le plus proches
		return list0.get(0).getFirst();	
	}
	
	
	private MyCouple next_move() {
		if(this.chemin == null) {
			this.setLast_move(this.moveAlea());
			return new  MyCouple(this.getLast_move(),0);
		}
		if(this.chemin.size() == 0) {
			this.setLast_move(this.myPosition);
			return new  MyCouple(this.myPosition,0);
		}
		// on renvoie notre prochain mouvement
		String move  = this.chemin.get(0).getId();
		this.chemin.remove(0);
		this.setLast_move(move);
		return new MyCouple(move,0);
	}
	public MyCouple next_move_with_target(String target,boolean tankermove,boolean move_alea,boolean justwalk,String mode) {
		Path path = null;
		try{
			if(target == null) {throw new TargetWrongException("For : "+this.myAgent.getLocalName()+ " target is null");
			}
		}catch(TargetWrongException ex){
			ex.printStackTrace();
			this.setLast_move(this.myPosition);
			return new MyCouple(this.myPosition,0);
		}
		if(justwalk && this.chemin.size() == 0) return new MyCouple("finished",0); 
		int nbrepet = 10;
		if(justwalk || mode.equals("speed")) nbrepet = 20;
		if(last_Position != null && myPosition.equals(last_Position)) {
			this.addToBlockingNodes(last_Position);
			if(this.last_move != null) this.setOfLastMove.add(this.last_move);
			if(this.seemNodes(nbrepet)) {
				ArrayList<String> returnNode = new ArrayList<String>();
				for(String s: this.setOfLastMove) {
					if(! returnNode.contains(s)) {
						returnNode.add(s);
					}
				}
				this.setOfLastMove = new ArrayList<String>();
				this.setBlockingNodes(new ArrayList<String>());
				String retrunstring = "";
				for(String s : returnNode) {
					retrunstring += s+"-";
				}
				retrunstring += "end";
				return new MyCouple(retrunstring,1);
			}
			else if(!justwalk) {
				this.setChemin(null);
				this.setLast_move(this.moveAlea());
				return new MyCouple(this.getLast_move(),0);
			}
			else {
				return new MyCouple(this.last_move,0);
			}
			
		}
		if(move_alea) {
			this.last_Position = this.myPosition;
			this.setLast_move(this.moveAlea());
			this.setChemin(null);
			return new MyCouple(this.getLast_move(),0);
			
		}
		if(tankermove) {
			this.last_Position = this.myPosition;
			if( this.myPosition.equals(target) ) {
				this.setLast_move(this.moveAlea());
				return new MyCouple(this.getLast_move(),0);
			}
			if(around_Target(target)) {
				this.setLast_move(target);
				return new MyCouple(this.getLast_move(),0);
				
			}
		}
		
		if(!this.test_Chemin(target)) {
			
			try {
				this.chemin = null;
				AStar astar = new AStar(this.graph);
				astar.compute(myPosition,target);
				path = astar.getShortestPath();
				if(path != null) {
					this.chemin = path.getNodePath();
					if(this.chemin != null && this.chemin.size() > 0)this.chemin.remove(0);
				}
				
			}
			catch (Exception e) {
				e.printStackTrace();
				this.setChemin(null);
				this.last_Position = this.myPosition;
				this.setLast_move(this.moveAlea());
				return new MyCouple(this.getLast_move(),0);
	        }
		}
		this.last_Position = myPosition;
		return this.next_move();
		
		
	}
	
	private boolean test_Chemin(String target) {
		List<Node> c  = this.getChemin();
		if(c== null) return  false;
		if(c.size() == 0 ) return false;
		if(!target.equals(c.get(c.size()-1).getId())) return false;
		String pos = this.getPosition();
		try{ if(pos.equals("") || pos.equals(c.get(0).getId())) throw new TargetWrongException("Map Test chemin For : "+this.myAgent.getLocalName()+ " target is equal to position or position wrong");}
		catch(TargetWrongException ex) {System.out.println(ex.toString()); }
		if( pos.equals(c.get(0).getId())) {
			if(c.size() <= 1) return false;
			c.remove(0);
		}
		Node courant = this.graph.getNode(pos);
		if(courant == null) return false;
		Iterator<Node> it= courant .getNeighborNodeIterator();
		while(it.hasNext()) {
			Node m = it.next();
			if(m.getId().equals(c.get(0).getId())) return true;
		}
		return false;
	}

	
	private String moveAlea() {
		String move = null;
        try {
        	ArrayList<String> list = new ArrayList<String>();
    		Node courant = this.graph.getNode(this.myPosition);
    		if(courant == null) return this.myPosition ;
    		Iterator<Node> nodeIterator = courant.getNeighborNodeIterator();
    		while(nodeIterator.hasNext()) {
    			Node n = nodeIterator.next();
    			if(!n.getId().equals(this.myPosition) )list.add(n.getId());
    		}
			Collections.shuffle(list);
    		move = list.get(0);
    	}
		catch(Exception e) {
			e.printStackTrace();
			return this.myPosition;
		}
		return move;
	}
	

	public boolean around_Target(String node_final) {
		try {
			String position = this.getPosition();
			Node courant = this.getNode(position);
			if(courant == null) return false;
			if(courant.getId().equals(node_final)) return true;
			Iterator<Node> nodeIterator = courant.getNeighborNodeIterator();
			while(nodeIterator.hasNext()){
				Node n =  nodeIterator.next();
				if(n.getId().equals(node_final)){
					return true;
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		return false;	
	}

	public  void updateLastCommunication(String agentID, Long date) {
		this.communicationHistory.put(agentID, date);
    }
	
	
	
	// retourne le noeud de degre gradn qui est plus central avec l'indice le plus grand, sans tresor dessus
	public String nodeRDV(String centerActuel) {
		
		List<Node> chemin = null;
		Path path = null;
		int degremax = -1;
		String nodeMaxDefault = null;
		//trouve degre max
		for(Node n : this.graph.getNodeSet()) {
			if(degremax < n.getDegree() ) {
				degremax = n.getDegree();
				nodeMaxDefault = n.getId();
			}
		}
		//trouves les nodes tq ils ont degre >= 80% de degre max
		List<String> nodes = new ArrayList<String>();
		for(Node n : this.graph.getNodeSet()) {
			if(n.getDegree() > (0.8 * degremax) &&  (centerActuel == null || !n.getId().equals(centerActuel))) {
				nodes.add(n.getId());
			}
		}
		if(nodes.size() == 1) return nodes.get(0);
		Collections.sort(nodes);
		
		double size = 0;
		double maxsize = 0d;
		
		//pour tous les nodes qui ont degre 80% plus grand que degremax :
		//calculer somme des longeur de chemin à tous les autres node
		//return node qui est plus central : c-a-d en moyenne il a plus proche à tous les autres nodes
		AStar astar = new AStar(this.graph);
		Node n;
		for(int i = 0; i < nodes.size(); i++) {
			n = this.getNode(nodes.get(i));
			if(n == null) continue;
			size = 0d;
			for(Node m : this.graph.getNodeSet()) {
				chemin = null;
				try {
					astar.compute(n.getId(),m.getId());
					path = astar.getShortestPath();
					if(path != null) {
						chemin = path.getNodePath();
						size += (double)chemin.size();
					}
				}catch(Exception e) {
					e.printStackTrace();
					continue;
				}
			}
			if(size > 0 && maxsize < (1.0 / size)) {
				maxsize = 1.0 / size;
				nodeMaxDefault = n.getId();
			}
		}

		return nodeMaxDefault;
    }

	public String getTreseurvaleur() {
		try {
			for(Node n : this.graph.getNodeSet()){
				List<MyCouple> attr = n.getAttribute("contents");
				for(MyCouple a : attr) {
					if(((String)a.getLeft()).equals("Diamonds")) {
						this.diamondsValeur += (int)a.getRight();
					}
					if(((String)a.getLeft()).equals("Treasure")) {
						this.treasureValeur += (int)a.getRight();
					}
				}
				
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			return "Diamonds : 0 treasure : 0";
		}
		return "Diamonds : "+String.valueOf(this.diamondsValeur)+" "+"Treasure : "+String.valueOf(this.treasureValeur);
	}
	
	@SuppressWarnings("unchecked")
	public String toString(boolean afficheComplet){
		String res = "";
		try {
			int i = 1;
			for(Node n : this.graph.getNodeSet()){
				if(afficheComplet || ((List<MyCouple>)n.getAttribute("contents")).size() > 0) {
					i += 1;
					res += "Node : "+n.getId()+" ";
					for (String att: n.getAttributeKeySet()) {
	            		res += n.getAttribute(att)+" ";
					}
					Iterator<Node>nodeIterator = n.getNeighborNodeIterator();
					while( nodeIterator.hasNext()){
						Node voision = nodeIterator.next();
						res += voision.getId()+",";
					}
					if((i % 2) != 0) {
						res += "\t";
					}else {
						res += "\n";
					}
				}
			}
			
			res += "\n";
		}catch(Exception e) {
			e.printStackTrace();
			return "";
		}
		
		return res;
	}

	//b = true il return valeur si il existe un truc sur ce node
	//b = false il return valeur si il existe un truc sur ce node de mon type
	public int is_Treasure_on_this_node(String typeOfBack, boolean b) {
		int valeur = 0;
		List<MyCouple> attr = null;
		try {
			attr  = this.graph.getNode(this.getPosition()).getAttribute("contents");
		}catch(Exception e) {
			e.printStackTrace();
			return 0;
		}
		for (int i = 0; i < attr.size(); i++) {
			MyCouple c = attr.get(i);
			if(b || ((String)c.getLeft()).equals(typeOfBack)) {
				valeur = (Integer)c.getRight();
				if(valeur > 0) return valeur;
				
			}
		}
		return 0;
	}
	public HashMap<String, Object> getCaps() {
		return caps;
	}
	public void setCaps(HashMap<String, Object> caps) {
		this.caps = caps;
	}

	

}
