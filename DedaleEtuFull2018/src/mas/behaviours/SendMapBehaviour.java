package mas.behaviours;


import java.util.ArrayList;
import java.util.HashMap;

import jade.core.AID;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import mas.agents.*;
import utils.CommonUtils;
import utils.DfUtils;
import utils.Map;


public class SendMapBehaviour extends AbstractBehaviour{
	
	private static final long serialVersionUID = 1781851554411353328L;
	private DFAgentDescription[] result = null;
	private boolean finished = false;
	private AgentExplorateur myagent;
	//sendAnyModification = true il envoie la carte meme si il a fait une seul modification sur sa carte
	//sendAnyModification = false il envoie la carte que tous les 10 modification effetctué sur la carte
	private boolean sendAnyModification;
	private boolean totanker;
	private boolean addNextNormal;
	public SendMapBehaviour(final mas.abstractAgent myagent,boolean sendAnyModification,boolean totanker,boolean addNextNormal) {
		super(myagent);
		this.myagent = (AgentExplorateur) this.myAgent;
		this.sendAnyModification = sendAnyModification;
		this.addNextNormal = addNextNormal;
		this.totanker = totanker;
	}
	
	
	@Override
	public void action() {
		
		try {
			
			final MessageTemplate msgTemplate;
			Map map = this.myagent.getMap();
			ACLMessage msg;
			Long date = null;
			HashMap<String, HashMap<String, HashMap<String, Object>>>  m = null;
			
			// reception de l'acquittement : mise a jour de l'historique de communication (date du dernier message)
			msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
			do {
				msg = ((mas.abstractAgent) this.myagent).receive(msgTemplate);
				if (msg != null) {	
					date = (Long) msg.getContentObject();
					if(date != null) {
						map.updateLastCommunication(msg.getSender().getLocalName(), date);
					}
				}
			}while(msg != null);

			ArrayList<String> roles = new ArrayList<String>();
			if(totanker) {
				roles.add("AgentTanker");	
			}
			else {
				roles.add("AgentCollect");
				roles.add("AgentTanker");
				roles.add("AgentExplo");
			}
			

			for(String role : roles) {
				// recherche dans les pages jaunes
				this.result = DfUtils.searchExplorer(role,(mas.abstractAgent)this.myAgent);
				if(this.result != null) {
					// envoi de la carte pour tous les reciver potentielle 
					for(DFAgentDescription receiver: this.result){
						// on verifie que l'agent n'envoie pas de message a lui-meme..
						if(! ((mas.abstractAgent) this.myagent).getAID().toString().equals((receiver.getName()).toString())) {
							m = map.shareMap(receiver.getName().getLocalName(),sendAnyModification);
							if(m != null) {
								msg = new ACLMessage(ACLMessage.INFORM);
								msg.setSender(this.myagent.getAID());
								msg.addReceiver(new AID((receiver.getName()).getLocalName().toString(), AID.ISLOCALNAME));
								msg.setContentObject(m);
								((mas.abstractAgent) this.myagent).sendMessage(msg);
							}
						}
					}
				}
			}
			if(addNextNormal)CommonUtils.addNextBehaviour((mas.abstractAgent)this.myAgent,false);
			this.finished = true;
			
		}catch(Exception e) {e.printStackTrace();}
	}

	@Override
	public boolean done() {return finished;}
}