package princ;

/*
 * Pendant collecte compter nombre fois ou on fait mvmt alea consecutivement : si > 10, protocole deblocage 
 * centraliser partage de map autour du tanker pour que les collecteurs aient une map a jour
 * optimiser comportement collecteur solitaire (bestmove_collecteur, machine etats finis de behaviours) : collecte avant ou apres exploration???
 * meilleur mouvement pour tanker ???
 * chercher un bon mouvement pour les explorateurs apres l'exploration
 * 
 * */

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;


import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import mas.agents.AgentCollector;
import mas.agents.AgentExplorateur;
import mas.agents.AgentTanker;
import mas.agents.DummyWumpusShift;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import env.EntityType;
import env.Environment;
import env.Environment.ENVtype;


public class Principal {
	//A modifier 
	final private  static int nbAgentExplorer = 1;
	final private  static int nbAgentCollector =1;
	final private  static int nbAgentSilo =1;
	final private static int nbNode = 10;
	final private static int nbGlum = 0;
	private  static int ntypenv = 3;

	private static String hostname = "127.0.0.1"; 
	private static HashMap<String, ContainerController> containerList=new HashMap<String, ContainerController>();// container's name - container's ref
	private static List<AgentController> agentList;// agents's ref
	private static Runtime rt;	

	private static Environment env;// static ref of the real environment
	

	public static void main(String[] args){

		System.out.println("Hello !");
		//0) Create the real environment and the observed one
		if(ntypenv == 1) {
			env = new Environment(ENVtype.GRID_T, nbNode, null);
		}
		else if (ntypenv == 2) {
			env= new Environment(ENVtype.DOROGOVTSEV_T,nbNode,null);
		}
		else if (ntypenv == 3) {
			env=new Environment("ressources/myMap","ressources/myMap-config");
		}
		else {
			env=new Environment("ressources/map2017-2","ressources/map2017-config");
		}

		//1), create the platform (Main container (DF+AMS) + containers + monitoring agents : RMA and SNIFFER)
		setRt(emptyPlatform(containerList));

		//2) create agents and add them to the platform.
		agentList=createAgents(containerList);

		//3) launch agents
		startAgents(agentList);

	}



	/**********************************************
	 * 
	 * Methods used to create an empty platform
	 * 
	 **********************************************/

	/**
	 * Create an empty platform composed of 1 main container and 3 containers.
	 * 
	 * @return a ref to the platform and update the containerList
	 */
	private static Runtime emptyPlatform(HashMap<String, ContainerController> containerList){

		Runtime rt = Runtime.instance();

		// 1) create a platform (main container+DF+AMS)
		Profile pMain = new ProfileImpl(hostname, 8888, null);
		System.out.println("Launching a main-container..."+pMain);
		AgentContainer mainContainerRef = rt.createMainContainer(pMain); //DF and AMS are include

		// 2) create the containers
		containerList.putAll(createContainers(rt));

		// 3) create monitoring agents : rma agent, used to debug and monitor the platform; sniffer agent, to monitor communications; 
		createMonitoringAgents(mainContainerRef);

		System.out.println("Plaform ok");
		return rt;

	}

	/**
	 * Create the containers used to hold the agents 
	 * @param rt The reference to the main container
	 * @return an Hmap associating the name of a container and its object reference.
	 * 
	 * note: there is a smarter way to find a container with its name, but we go fast to the goal here. Cf jade's doc.
	 */
	private static HashMap<String,ContainerController> createContainers(Runtime rt) {
		String containerName;
		ProfileImpl pContainer;
		ContainerController containerRef;
		HashMap<String, ContainerController> containerList=new HashMap<String, ContainerController>();//bad to do it here.


		System.out.println("Launching containers ...");

		//create the container0	
		containerName="container0";
		pContainer = new ProfileImpl(null, 8888, null);
		System.out.println("Launching container "+pContainer);
		containerRef = rt.createAgentContainer(pContainer); //ContainerController replace AgentContainer in the new versions of Jade.
		containerList.put(containerName, containerRef);

		//create the container1	
		containerName="container1";
		pContainer = new ProfileImpl(null, 8888, null);
		System.out.println("Launching container "+pContainer);
		containerRef = rt.createAgentContainer(pContainer); //ContainerController replace AgentContainer in the new versions of Jade.
		containerList.put(containerName, containerRef);

		//create the container2	
		containerName="container2";
		pContainer = new ProfileImpl(null, 8888, null);
		System.out.println("Launching container "+pContainer);
		containerRef = rt.createAgentContainer(pContainer); //ContainerController replace AgentContainer in the new versions of Jade.
		containerList.put(containerName, containerRef);

		System.out.println("Launching containers done");
		return containerList;
	}


	/**
	 * create the monitoring agents (rma+sniffer) on the main-container given in parameter and launch them.
	 *  - RMA agent's is used to debug and monitor the platform;
	 *  - Sniffer agent is used to monitor communications
	 * @param mc the main-container's reference
	 * @return a ref to the sniffeur agent
	 */
	private static void createMonitoringAgents(ContainerController mc) {

		System.out.println("Launching the rma agent on the main container ...");
		AgentController rma;

		try {
			rma = mc.createNewAgent("rma", "jade.tools.rma.rma", new Object[0]);
			rma.start();
		} catch (StaleProxyException e) {
			e.printStackTrace();
			System.out.println("Launching of rma agent failed");
		}

		System.out.println("Launching  Sniffer agent on the main container...");
		AgentController snif=null;

		try {
			snif= mc.createNewAgent("sniffeur", "jade.tools.sniffer.Sniffer",new Object[0]);
			snif.start();

		} catch (StaleProxyException e) {
			e.printStackTrace();
			System.out.println("launching of sniffer agent failed");

		}		


	}



	/**********************************************
	 * 
	 * Methods used to create the agents and to start them
	 * 
	 **********************************************/


	/**
	 *  Creates the agents and add them to the agentList.  agents are NOT started.
	 *@param containerList :Name and container's ref
	 *@param sniff : a ref to the sniffeur agent
	 *@return the agentList
	 */
	private static List<AgentController> createAgents(HashMap<String, ContainerController> containerList) {
		System.out.println("Launching agents...");
		ContainerController c;
		String agentName;
		List<AgentController> agentList=new ArrayList<AgentController>();
		c = containerList.get("container0");
		//Collector (backPack)
		for(int i = 1; i <= nbAgentCollector;++i) {
			c = containerList.get("container0");
			agentName="AgentCollector"+String.valueOf(i);
			try {

				Object[] objtab=new Object[]{env,EntityType.AGENT_COLLECTOR};//used to give informations to the agent
				AgentController	ag=c.createNewAgent(agentName,AgentCollector.class.getName(),objtab);
				agentList.add(ag);
				System.out.println(agentName+" launched");
			} catch (StaleProxyException e) {
				e.printStackTrace();
			}
		}
		for(int i = 1; i <= nbAgentSilo;++i) {
			c = containerList.get("container0");
			agentName="AgentTanker"+String.valueOf(i);
			try {
				Object[] objtab=new Object[]{env,EntityType.AGENT_TANKER};//used to give informations to the agent
				AgentController	ag=c.createNewAgent(agentName,AgentTanker.class.getName(),objtab);
				agentList.add(ag);
				System.out.println(agentName+" launched");
			} catch (StaleProxyException e) {
				e.printStackTrace();
			}
		}
		for(int i = 1; i <= nbAgentExplorer;++i) {
			c = containerList.get("container0");
			agentName="AgentExplorer"+String.valueOf(i);
			try {
		
				Object[] objtab=new Object[]{env,EntityType.AGENT_EXPLORER};//used to give informations to the agent
				AgentController	ag=c.createNewAgent(agentName,AgentExplorateur.class.getName(),objtab);
				agentList.add(ag);
				System.out.println(agentName+" launched");
			} catch (StaleProxyException e) {
				e.printStackTrace();
			}
		}
		for(int i = 1 ; i<= nbGlum ; ++i) {
			agentName="Golem"+String.valueOf(i);
			try {
				Object[] objtab=new Object[]{env};//used to give informations to the agent
				AgentController	ag=c.createNewAgent(agentName,DummyWumpusShift.class.getName(),objtab);
				agentList.add(ag);
				System.out.println(agentName+" launched");
			} catch (StaleProxyException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Agents launched...");
		return agentList;
	}

	/**
	 * Start the agents
	 * @param agentList
	 */
	private static void startAgents(List<AgentController> agentList){

		System.out.println("Starting agents...");


		for(final AgentController ac: agentList){
			try {
				ac.start();
			} catch (StaleProxyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		System.out.println("Agents started...");
	}



	public static Runtime getRt() {
		return rt;
	}



	public static void setRt(Runtime rt) {
		Principal.rt = rt;
	}

}







