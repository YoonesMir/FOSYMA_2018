package utils;

import java.util.ArrayList;

public class Sextuple  implements Comparable<Sextuple> {
	private String first;
	private int second;
	private int third;
	private int four;
	private ArrayList<String> five;
	private ArrayList<String> six;
	
	public Sextuple(String  first, int  second,int  third,int  four,ArrayList<String >five,ArrayList<String> six) {
		this.first = first;
		this.second = second;
		this.third = third;
		this.four = four;
		this.five  = five;
		this.six  = six;
	}
	
	public String getFirst() {
		return this.first;
	}
	
	public int getSecond() {
		return this.second;
	}
	
	public int getThird() {
		return this.third;
	}
	
	public int getFour() {
		return this.four;
	}
	
	public ArrayList<String> getFive() {
		return this.five;
	}
	
	public ArrayList<String> getSix() {
		return this.six;
	}
	
	public void setFirst(String first) {
		this.first = first;
	}
	
	public void setSecond(int second) {
		this.second = second;
	}
	
	public void setThird(int third) {
		this.third = third;
	}
	
	public void setFour(int four) {
		this.four = four;
	}
	
	public void setFive(ArrayList<String> l) {
		this.five = l;
	}
	
	public void setSix(ArrayList<String> l) {
		this.six = l;
	}
	
	public String toString() {
		return this.first+" "+this.second+" "+this.third+" "+this.four+" "+this.five.toString()+" "+this.six.toString()+"\n";	
	}
	//trié selon ordre lexo graphie, 
	//permier element plus imporant et puis dans le cas equalité c'est les deuximem elem et etc
	@Override
	public int compareTo(Sextuple arg0) {
		try {
			if(arg0 == null) return 0;
			if(!( arg0 instanceof Sextuple) ) return 0;
			if(this.second > arg0.getSecond()) return 1;
			if(this.second < arg0.getSecond()) return -1;
			if(this.third > arg0.getThird()) return 1;
			if(this.third < arg0.getThird()) return -1;
			if(this.four > arg0.getFour()) return 1;
			if(this.four < arg0.getFour()) return -1;
		}catch(Exception e) {
			e.printStackTrace();
			return 0;
		}
		return 0;
	}
}
