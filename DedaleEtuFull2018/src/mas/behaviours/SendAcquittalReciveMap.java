package mas.behaviours;


import jade.lang.acl.ACLMessage;


import jade.core.AID;

public class SendAcquittalReciveMap extends AbstractBehaviour{
	
	private static final long serialVersionUID = 9088209402507795289L;
	private boolean finished = false;
	private String receiverName;
	private Long date;

	public SendAcquittalReciveMap(final mas.abstractAgent myagent, String receiverName, Long date) {
		super(myagent);
		this.receiverName = receiverName;
		this.date = date;
	}
	@Override
	public void action() {
		try {
			// envoi du message d'acquittement, avec la date d'envoi du message que l'on acquitte
			final ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM);
			msg.setSender(((mas.abstractAgent)this.myAgent).getAID());
			msg.addReceiver(new AID(this.receiverName, AID.ISLOCALNAME));  
			msg.setContentObject(date);
			((mas.abstractAgent)this.myAgent).send(msg);
			this.finished = true;
		}
		catch(Exception e) {e.printStackTrace();}
	}
	@Override
	public boolean done() {return finished;}
}
