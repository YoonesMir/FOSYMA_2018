package utils;



public class CentralUnit {
	//c'est ne pas un information partagé just on a besoin déclarer ici a cause le proble suivant:
	//on ne peut pas savoir est-ce que agent tanker a réçu certin truc ou pas , c'est pas trop malin :-)
	//pour les agnet collector on est capable de savoir est-ce que ils ont donné qqch au tanker ou pas
	private static boolean tankerrecived = false;
	//c'est le seul information partagé entres les agents
	private static boolean tankerFinished = false;

	
	
	public synchronized static void setTankerrecived(boolean b) {
		tankerrecived = b;

	}
	
	public synchronized static boolean getTankerrecived() {
		return tankerrecived;
	}
	
	public static synchronized boolean isTankerFinished() {
		return tankerFinished;
	}

	public static synchronized void setTankerFinished(boolean tankerFinished) {
		CentralUnit.tankerFinished = tankerFinished;
	}

	

}
