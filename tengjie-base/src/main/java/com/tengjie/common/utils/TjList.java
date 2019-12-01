package com.tengjie.common.utils;

import java.io.Serializable;
import java.util.ArrayList;

public class TjList<E> extends ArrayList<E> implements Serializable {
	private static final long serialVersionUID = 1L;
	public boolean add(E ...args) {
		 for (int i = 0; i < args.length; i++) {   
			 super.add(args[i]);
		    } 
		return true;
	}
	public TjList<E> addCon(E args) {
		
			 super.add(args);
		  
		return this;
	}
	
	public static <E>TjList<E> newInstance(){
		return new TjList();
	}
}
