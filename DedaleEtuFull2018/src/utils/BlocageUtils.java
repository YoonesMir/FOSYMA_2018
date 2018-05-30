package utils;

//Tous les fonction et méthods que on a besoin pour gestion de déblocage
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.graphstream.algorithm.AStar;

import mas.abstractAgent;
import mas.agents.*;
import mas.agents.AgentExplorateur;

import mas.behaviours.FindNextTreasureBehaviour;

import java.util.Iterator;
import java.util.List;


import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Graph;

import org.graphstream.graph.implementations.Graphs;



import env.Attribute;
import env.Couple;

import java.util.TreeMap;



public class BlocageUtils {

	private abstractAgent agent;
	//nodeblock est les nodes qui nous bloquent en frome en chain string
	private String nodeblock;
	//c'est la meme chose en forme en tab de string
	private ArrayList<String> nodeblocks;
	//position d'agent bloqué
	private String position;
	private String target;
	private Map map;
	private String typeSerach;
	private List<Node> last_chemin;
	
	public BlocageUtils(abstractAgent abstractAgent, String nodeblock, String position, String target,Map map,String typeSerach, List<Node> last_chemin) {
		this.setAgent(abstractAgent);
		this.setNodeblock(nodeblock);
		String[] parts = nodeblock.split("-");
		nodeblocks = new ArrayList<String>();
		for(String s : parts) {
			nodeblocks.add(s);
		}
		nodeblocks.remove(nodeblocks.size()-1);
		this.setPosition(position);
		this.setTarget(target);
		this.setMap(map);
		this.setTypeSerach(typeSerach);
		this.setLast_chemin(last_chemin);
	}
	
	public Map getMap() {
		return map;
	}
	public void setMap(Map map) {
		this.map = map;
	}
	
	public List<Node> getLast_chemin() {
		return last_chemin;
	}
	public void setLast_chemin(List<Node> last_chemin) {
		this.last_chemin = last_chemin;
	}
	
	public String getTypeSerach() {
		return typeSerach;
	}
	public void setTypeSerach(String typeSerach) {
		this.typeSerach = typeSerach;
	}
	

	public abstractAgent getAgent() {
		return agent;
	}
	public void setAgent(abstractAgent agent) {
		this.agent = agent;
	}
	

	public String getNodeblock() {
		return nodeblock;
	}
	public void setNodeblock(String nodeblock) {
		this.nodeblock = nodeblock;
	}
	public ArrayList<String> getNodeblocks() {
		return nodeblocks;
	}
	public void setNodeblocks(ArrayList<String> nodeblocks) {
		this.nodeblocks = nodeblocks;
	}
	

	public String getPosition() {
		return position;
	}
	public void setPosition(String position) {
		this.position = position;
	}
	
	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}
	
	
	//return ensemble de nodes autour de nous qui sont infecté par Glum
	public ArrayList<String> getStenchNodes(){
		ArrayList<String> res = new ArrayList<String>();
		List<Couple<String,List<Attribute>>> lobs =((mas.abstractAgent)this.agent).observe();
		List<Attribute> att;
		String c;
		for (Couple<String,List<Attribute>> couple: lobs){
			c = couple.getLeft();
			att = couple.getRight();
			for(Attribute a : att){
            	if(a.getName().equals("Stench")) {
            		res.add(c);
            		break;
            	}
            }
		}
		return res;
	}
	
	//checher si il existe un autres chemin pour aller a notre target actuel ou si il existe un autres target 
	//avec le chemin associé a ce target qui passe par les nodes "propre"
	public boolean is_anotherTarget(ArrayList<String> stenchNodes) {
		
		boolean flag = false;
		ArrayList<String> nodes; 
		//trouver les list de target potentielle selon notre type de search soit un node non visité soit un autres T/D pour ramasser
		nodes = this.find_by_type();
		if(nodes.size() == 0) return false;
		int maxTry = 10;
		TreeMap<String,Double> map_apres = new TreeMap<String,Double>();
		HashMap<String ,Double> nodesmap = new HashMap<String, Double>();
		//si on est en mode exploration , triér ensemble des nodes non visité en order rcroissant de leur distance à nous
		if(this.typeSerach.equals("nextForVisite")) {
			Graph g = Graphs.clone(this.map.getGraph());
			Dijkstra dijkstra = new Dijkstra();
			dijkstra.init(g);
			dijkstra.setSource(g.getNode(this.position));
			dijkstra.compute();
	        Comparateur comp =  new Comparateur(nodesmap,true);
	        map_apres = new TreeMap<String,Double>(comp);
			//pour chaque node de la list "nodes" que on viens de créé on calcul le plus 
			//court chemin pour aller à ce node
			for(String id : nodes) {
				double len = dijkstra.getPathLength(g.getNode(id));
				nodesmap.put(id, len);
			}
			//Trieé le hashMAp selon order des distance décroissant
			
		}else if(this.typeSerach.equals("nextTreasure")){
			//si je suis en mode trouver un autres treasure
			//just trie hashmap selon order envoyer par fonction find_by_type
			double val = 1000.0;
			for(int i = 0 ; i< nodes.size();i++) {
				nodesmap.put(nodes.get(i), val);
				val -= 1.0;
			}
		}
		map_apres.putAll(nodesmap);
		ArrayList<String> path;
		
		int Try = 0 ;
		for(String id : map_apres.keySet()) {
			path = new ArrayList<String>();
			//voir si il existe une chemin propre pour aller a ce node
			//et si il exist on le crée
			flag = this.build_path(this.position,id, stenchNodes,path,0,0,0,false);
			if(flag) return this.set_path(path,false);
			Try += 1;
			//on essaye 10 permier (faire plus rapide possible)
			if(Try >= maxTry)break;
		}
		//enlever de list stenchNodes les nodes qui ont pas une voision infecte par Glum
		//et recalculer le chemin propre
		//on fait plus leger notre criter de chemin propre
		if(stenchNodes.size()> 0) {
			int size1 = stenchNodes.size();
			stenchNodes =this.list_Neighbor_non_safe_degre_1(stenchNodes);
			if(size1 > stenchNodes.size()) {
				Try = 0;
				for(String id : map_apres.keySet()) {
					path = new ArrayList<String>();
					flag = this.build_path(this.position,id, stenchNodes,path,0,0,0,false);
					if(flag) return this.set_path(path,false);
					Try += 1;
					if(Try >= maxTry)break;
				}
			}
		}
		return false;
	}

	
	
	
	
	//contruit just une chemin propre pour sortie de posioton blocage 
	public boolean is_anoher_path_clean(ArrayList<String> stenchNodes) {
		
		ArrayList<String> path = new ArrayList<String>();
		boolean flag =false;
		path = new ArrayList<String>();
		//target est null on veut créé un chemin propre de min lenght 10 e max 30
		flag = this.build_path(this.position,null, stenchNodes,path,10,30,0,false);
		if(flag) return this.set_path(path,false);
		//la meme pricipe que dans fonction  plus haut
		if(stenchNodes.size()> 0) {
			int size1 = stenchNodes.size();
			stenchNodes =this.list_Neighbor_non_safe_degre_1(stenchNodes);
			if(size1 > stenchNodes.size()) {
				path = new ArrayList<String>();
				flag = this.build_path(this.position,null, stenchNodes,path,10,30,0,false);
				if(flag) return this.set_path(path,false);
			}
		}
		return flag;
	}

	//cette fonction recursif peut faire 3 chose
	//mode 1 = trouver un path propre vers target passé en param si target != null , dans ce cas comme on veut arriver a notre target minlen = maxlen = len = 0, car lenght de chemin n'a pas importance
	//mode 2 = trouver just un chemin propre pour aller à n'imorte où de lenght minimum = millen et maximum= maxlen dans ce cas taregt en params est null
	// mode 3 = trouver une chemin alea propre de lenght minimum = millen et maximimum lenght = maxlen dans ce cas target = null et alea = true
	public boolean build_path(String myplace,String target, ArrayList<String> stenchNodes, ArrayList<String> path,int minlen,int maxlen,int len,boolean alea) {
		path.add(myplace);
		//3 condition arret pour les 3 cas possibles : 
		//si en mode 1 et arrivé a target return true 
		if(target != null && myplace.equals(target)) return true;
		//si en mode 2  est on a créé un chemin de lenght maxlen return true
		if(target == null && path.size() >= maxlen)return true;
		//si en mode 3  est on a créé un chemin de lenght maxlen return true
		if(alea && path.size() >= maxlen)return true;
		Node courant = this.map.getNode(myplace);
		//normalemnt ce cas arrive jamais c'est just pour etre sûr
		if(courant == null) return false;
		ArrayList<String> list = new ArrayList<String>();
		Iterator<Node> nodeIterator = courant.getNeighborNodeIterator();
		//creé une list de tous les voision de node courant
		while(nodeIterator.hasNext()) list.add(nodeIterator.next().getId());
		//choisir le meilleur parmi les voision pour prochain step dans le chemin
		String idnew = this.choicBestNeighbor(list,stenchNodes,path,target,alea);
		//si il n'y pas un  bonne voision pour avancer :
		//si en mode 1 c'est fini return false , on ne peut pas aller vers target
		if(target != null && idnew == null) return false;
		//si en mode 2 ou 3
		if(idnew == null && target == null) {
			//si en mode 2 et 3 si le chemin déja crée a lenght minumum qu'il faut alors on est bien
			if( path.size() >= minlen) {
				return true;
			}
			//sinon c'est écheque
			return false;
		}
		len += 1;
		return build_path(idnew,target,stenchNodes,path,minlen, maxlen,len,alea);
		
	}
	
	private String choicBestNeighbor(ArrayList<String> list, ArrayList<String> stenchNodes, ArrayList<String> path,String target,boolean alea) {
		ArrayList<String> reslist = new ArrayList<String>();
		for(String id : list) {
			//verifier que on cree pas une cycle
			if(path.contains(id)) continue;
			//verifier on ne passe pas par les nodes ou on etait bloquer si on est en mode alea sinon c'est pas imporant de essaye passer encore par les node où on etait bloqué
			if(!alea && this.nodeblocks.contains(id)) continue;
			//et verifier on ne passe pas par les nodes infecté par GLUM , sauf dans le cas chemin alea
			if(! alea && stenchNodes.contains(id)) continue;
			reslist.add(id);
		}
	
		//ici reslist contien tous les voision propres
		
		if(reslist.size() == 0) {
			return null;
		}
		if(target != null && reslist.contains(target)) {
			return target;
		}
		if(reslist.size() == 1) {
			return reslist.get(0);
		}
		//si on est en mode 1 , meilleur voision propres est c'est lui qui plus proche de notre target
		if(target != null) {
			Path mypath = null;
			AStar astar = new AStar(this.map.getGraph());
			int mini = 1000;
			String idmin = null;
			for(String id : reslist) {
				astar.compute( id,target);
				mypath = astar.getShortestPath();
				if(mypath == null) continue;
				if(mini > mypath.getNodeCount()) {
					mini = mypath.getNodeCount();
					idmin = id;
				}
			}
			if(idmin != null) return idmin;
		}
		//sinon en mode 2 et 3 choisir alea parmi voisions propres (la question de rapidité)
		Collections.shuffle(reslist);
		return reslist.get(0);

	}
	
	//cette fonction return ensemble des node selon notre type de serach
	private ArrayList<String> find_by_type() {
		Graph g = this.map.getGraph();
		ArrayList<String> nodes = new ArrayList<String>();
		//si on est en tarin exploration on return ensmeble nodes non visité
		if(this.typeSerach.equals("nextForVisite")) {
			for(Node n : g.getNodeSet()) {
				String id = n.getId();
				if(!(boolean)n.getAttribute("visite") ){
					nodes.add(id);
				}
			}
		}
		//si on est en tarin collection on return ensemble node avec T/D
		if(this.typeSerach.equals("nextTreasure")) {
			MyFive five = new MyFive();
			int v = FindNextTreasureBehaviour.find_targe(this.position, five, nodes, (AgentCollector)this.agent,this.map);
			while(v != 0) {
				nodes.add(five.getFirst());
				five = new MyFive();
				v = FindNextTreasureBehaviour.find_targe(this.position, five, nodes, (AgentCollector)this.agent,this.map);
				}
			}
		
		return nodes;
	}
	
	//cette fonction return sous ensemble nodes de "voisionsNonSafe" qui n'ont pas une voision non infécté par Glum
	//c'est ensmeble des node non safe de degre 1 
	//en effet si un node a un vosion non infecté par GLUM ca veut dire il a un distance 2 avec Glum sinon il est voision de Glum
	private ArrayList<String> list_Neighbor_non_safe_degre_1(ArrayList<String> voisionsNonSafe) {
		boolean flag = false;
		ArrayList<String> n = new ArrayList<String>();
		Iterator<Node> it;
		Node v;
		ArrayList<Integer> list = new ArrayList<Integer>();
		//cree un sous ensemble de nodes de voisionsNonSafe qui ont un voision qui n'est pas dans voisionsNonSafe
		for(int i = 0; i < voisionsNonSafe.size(); i++) {
			Node courant = this.map.getNode(voisionsNonSafe.get(i));
			//ce cas normalment arrive jamais
			if(courant == null) return voisionsNonSafe;
			flag = false;
			it = courant.getNeighborNodeIterator();
			while(it.hasNext()) {
				v = it.next();
				if(!v.getId().equals(courant.getId()) && !v.getId().equals(this.position)) {
					if(!voisionsNonSafe.contains(v.getId())) {
						flag = true;
						break;
					}
				}
			}
			if(flag)list.add(i);
		}
		//n = voisionsNonSafe privé de list
		for(int i  = 0;i< voisionsNonSafe.size(); i++) {
			if(!list.contains(i)) {
				n.add(voisionsNonSafe.get(i));
			}
		
		}
		return n;
	}
	
	//trasferer une list de string à une list de type Node
	private ArrayList<Node> builPathFinal(ArrayList<String> path) {
		ArrayList<Node> res = new ArrayList<Node>();
		for(String id : path) {
			res.add(this.map.getNode(id));
		}
		return res;
	}
	
	//cette fonction regarde si parmi les nodes infecté par GLUM il existe un des nodes dans le quelle j'etait bloqué ou mon target si oui return true
	public boolean cause_is_Glum(ArrayList<String> stenchNodes ) {
		for (String id : stenchNodes){
            if( id.equals(this.target) )return true;
            for(String n : this.nodeblocks) {
            	if(id.equals(n))return true;
            } 
		}
		return false;
	}
	//initialisé chemin et aussi nextTraget de l'agent qui est etait bloque, une fois que on a trouvé une autre chemin
	//return false si il y a qqch qui se passe mal
	@SuppressWarnings("unchecked")
	public boolean set_path(ArrayList<String> path,boolean alea) {
		if(path == null || path.size() <= 1) return false;
		ArrayList<Node> pathNode = null;
		//remove(0) car elem 0 est notre position actuel
		path.remove(0);
		pathNode = this.builPathFinal(path);
		if(this.typeSerach.equals("nextTreasure") && !alea) {
			List<MyCouple> my = (List<MyCouple>)this.map.getAttribute(path.get(path.size()-1), "contents");
			if(my != null ) {
				for(MyCouple m : my) {
					int val = m.getRight();
					String typeT = (String)m.getLeft();
					if(val > 0 && typeT.equals(this.agent.getMyTreasureType())) {
						this.map.setChemin(pathNode);
						((AgentCollector)this.agent).setNextTarget(path.get(path.size()-1));
						((AgentCollector)this.agent).setNexTreasure(m);
						return true;
					}
				}
				
			}
			
		}

		if(this.typeSerach.equals("nextForVisite") || alea) {
			this.map.setChemin(pathNode);
			((AgentExplorateur)this.agent).setNextTarget(path.get(path.size()-1));
			return true;
			
		}
		return false;
	}
}
