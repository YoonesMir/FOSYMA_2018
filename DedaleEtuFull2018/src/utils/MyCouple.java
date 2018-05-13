package utils;

import jade.util.leap.Serializable;


public class MyCouple implements Serializable {
	
	private static final long serialVersionUID = -1204566044048182797L;
	Object left;
	int right;

	public MyCouple(Object left,int right) {
		this.left = left;
		this.right = right;
	}
	
	public MyCouple clone() {
		return new MyCouple(this.left, this.right);
	}
	
	public Object getLeft() {
		return this.left;
	}
	
	public int getRight() {
		return this.right;
	}
	
	public void setRight(int right) {
		this.right = right;
	}
	
	public void setLeft(Object left) {
		this.left = left;
	}
	
	public String toString(){
		String s= "";
		try {
			if(this.left instanceof Integer){
				s += String.valueOf((int)this.left)+" ";
			}else{
				s += (String) this.left+" ";
			}
			s += String.valueOf(this.right)+" ";
		}catch(Exception e) {e.printStackTrace();}
		return s;
	}
}
