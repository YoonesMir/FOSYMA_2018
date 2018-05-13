package mas.behaviours;



import jade.lang.acl.ACLMessage;
import utils.DfUtils;
import mas.agents.*;




public class emptyMailbox extends AbstractBehaviour{
	
	private static final long serialVersionUID = -7727814212984643827L;
	private boolean finished = false;

	public emptyMailbox(final mas.abstractAgent myagent ) {
		super(myagent);
	}

	@Override
	public void action() {
		DfUtils.deletFromDF((mas.abstractAgent)this.myAgent);
		ACLMessage msg;
		do {msg = ((mas.abstractAgent)this.myAgent).receive();}while(msg != null);
		System.out.println(((mas.abstractAgent)this.myAgent).getLocalName()+" est mort   nb Node : "+((AgentExplorateur)this.myAgent).getMap().nbNodes());
		((mas.abstractAgent)this.myAgent).doDelete();
		finished = true;
	}
	
	@Override
	public boolean done() {return finished;}
}
