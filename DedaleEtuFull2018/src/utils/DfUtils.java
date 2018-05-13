package utils;

import java.util.ArrayList;

import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;


public class DfUtils {
	
	//enregistre sur DF
	public static synchronized void registerOnDF(String role, mas.abstractAgent agnet) {
		DFAgentDescription dfd = new DFAgentDescription();
	    dfd.setName(agnet.getAID()); 
	    ServiceDescription sd  = new ServiceDescription();
	    sd.setType(role);
	    sd.setName(agnet.getLocalName());
	    dfd.addServices(sd);
	    try {DFService.register(agnet, dfd);}
	    catch (FIPAException fe) {fe.printStackTrace();}
	}
	
	//supprimmer de DF
	public static synchronized void deletFromDF(mas.abstractAgent thisagent) {
		try { DFService.deregister(thisagent); }
        catch (Exception e) {e.printStackTrace();}
	}
	//chercher sur DF par role
	public static synchronized DFAgentDescription[] searchExplorer(String role,mas.abstractAgent agent){
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType(role); 
		dfd.addServices(sd);
		try {return DFService.search(agent, dfd);
		} catch (FIPAException e) {
			e.printStackTrace();
			return null;
		}
	}

	//return nb agnet en total enregistre sur DF
	
	public static synchronized int  ensmebleAgents(mas.abstractAgent agent,ArrayList<DFAgentDescription[] > resultats) {
		ArrayList<String> roles = new ArrayList<String>();
		roles.add("AgentCollect");
		roles.add("AgentTanker");
		roles.add("AgentExplo");
		int nbAgent = 0;
		DFAgentDescription[] resultat = null;
		for(String role :roles ) {
			resultat = DfUtils.searchExplorer(role,agent);
			if(resultat != null && resultat.length > 0) {
				resultats.add(resultat);
				nbAgent += resultat.length;
			}
			
		}
		return nbAgent;
	}
	
}
