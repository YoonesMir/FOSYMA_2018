package mas.behaviours;

import jade.core.behaviours.SimpleBehaviour;

//cette class nous servie à  facilité action de basculer entre behaviours
public abstract class AbstractBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 993079098525085696L;
	public AbstractBehaviour(final mas.abstractAgent myagent) {
		super(myagent);
	}
}
