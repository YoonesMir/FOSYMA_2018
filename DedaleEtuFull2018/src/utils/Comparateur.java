package utils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;


//pour ordoner une HashMap selon valuer 
public class Comparateur implements Comparator<String> {

	Map<String, Double> base;
	//reversed = true trieé dans order décroissant
	//reversed = false trieé dans order croissant
	private boolean reversed;
    public Comparateur(HashMap<String , Double> map,boolean b) {
        this.base = map;
        this.reversed = b;
    }

    //ce comparateur ordonne les éléments dans l'ordre   
    @Override
    public int compare(String a, String b) {
    	if (base.get(a) > base.get(b)) {
    		if(!this.reversed)return 1;
    		return -1;
    	} else  if (base.get(a) < base.get(b)){
    		if(! this.reversed)return -1;
    		return 1;
    	}
    	return 0;
    }
}