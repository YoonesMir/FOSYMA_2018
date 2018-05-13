package utils;




public class MyFive  implements Comparable<MyFive> {
	private String first;
	private double second;
	private double third;
	private MyCouple at;
	private String five;

	//constructeur
	public MyFive(String  first, double  second,double  third ,MyCouple at2,String five) {
		this.first = first;
		this.second = second;
		this.third = (int) third;
		this.at = at2;
		this.five = five;
	
	}
	//constructeur
	public MyFive() {
		this.first = "nothing";
		this.second = 0;
		this.third = 0;
		this.at = null;
		this.five = "";
	}
	//copier tous les donner de elem passé en param dans this
	public void copy(MyFive f) {
		this.setFirst(f.getFirst());
		this.setSecond(f.getSecond());
		this.setThird(f.getThird());
		this.setAttribute(f.getAttribute().clone());
		this.setFive(this.getFive());
	}
	
	public String getFirst() {
		return this.first;
	}
	
	public double getSecond() {
		return this.second;
	}
	
	public double getThird() {
		return this.third;
	}
	
	public MyCouple getAttribute() {
		return this.at;
	}
	public String getFive() {
		return this.five;
	}
	public void setFirst(String first) {
		this.first = first;
	}
	
	public void setSecond(double second) {
		this.second = second;
	}
	
	public void setThird(double third) {
		this.third = third;
	}
	public void setAttribute(MyCouple at) {
		this.at = at;
	}
	public void setFive(String five) {
		this.five = five;
	}
	
	public String toString() {
		return "Node :"+this.first.toString()+" rapport : "+this.second+" perte "+this.third+" Attribute: "+this.at.toString()+"\n";	
	}
	
	
	//trieé selon ordre l'exo
	@Override
	public int compareTo(MyFive arg0) {
		try {
			if(arg0 == null) return 0;
			if(!( arg0 instanceof MyFive) ) return 0;
			if(this.third < arg0.getThird()) return -1;
			if(this.third > arg0.getThird()) return 1;
			if(this.second < arg0.getSecond()) return 1;
			if(this.second > arg0.getSecond()) return -1;
			String[] l1 = this.first.split("_");
			String[] l2 = arg0.getFirst().split("_");
			if(Integer.parseInt(l1[0]) > Integer.parseInt(l2[0])) return 1;
			if(Integer.parseInt(l1[0]) < Integer.parseInt(l2[0]) ) return -1;
			if(l1.length > 1 && l2.length > 1) {
				if(Integer.parseInt(l1[1]) > Integer.parseInt(l2[1])) return 1;
				if(Integer.parseInt(l1[1]) < Integer.parseInt(l2[1]) ) return -1;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			return 0;
		}
		return 0;
	}
	
}