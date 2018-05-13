package mas.behaviours;

import java.io.Serializable;
import java.util.HashMap;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import mas.behaviours.SendAcquittalReciveMap;
import utils.CommonUtils;
import utils.Map;
import mas.agents.AgentExplorateur;

public class ReceiveMapBehaviour extends AbstractBehaviour{

	private static final long serialVersionUID = 8367346506869730211L;
	private boolean finished = false;
	private AgentExplorateur myagent;

	public ReceiveMapBehaviour(final mas.abstractAgent myagent) {
		super(myagent);
		this.myagent = (AgentExplorateur) this.myAgent;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void action() {
		try {
			Long date;
			Map map = this.myagent.getMap();
			HashMap<String, HashMap<String, HashMap<String, Object>>> m = null; // carte serialisee
			Serializable newMap = null;
			MessageTemplate msgTemplate;
			ACLMessage msg = null;
			//vider les message blocages :
			/*msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE);
			do {msg = ((mas.abstractAgent) this.myagent).receive(msgTemplate);}while(msg != null);
			msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF);
			do { msg = ((mas.abstractAgent) this.myagent).receive(msgTemplate);}while(msg != null);*/
			
			msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			
			do {
				msg = ((mas.abstractAgent) this.myagent).receive(msgTemplate);
				if (msg != null) {
					newMap = msg.getContentObject();
					if(newMap != null) {
						m = (HashMap<String, HashMap<String, HashMap<String, Object>>>) newMap;
						if(m != null) {
							date =  (Long) m.get("date").get("date").get("date");
							// envoi d'un acquittement a l'envoyeur
							((mas.abstractAgent) this.myagent).addBehaviour(new SendAcquittalReciveMap((mas.abstractAgent) myagent, msg.getSender().getLocalName(),date ));
							map.unifier(m,msg.getSender().getLocalName(),this.myagent.getLocalName());
						}
					}	
				}
			}while( msg != null );
			CommonUtils.addNextBehaviour((mas.abstractAgent)this.myAgent,false);
			this.finished = true;
		}
		catch(Exception e) {e.printStackTrace();}
	}

	@Override
	public boolean done() {return this.finished;}
}
