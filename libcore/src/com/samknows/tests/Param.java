package com.samknows.tests;

import java.io.Serializable;
import java.util.Locale;

public class Param implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public Param(String name, String value){
		this.name = name.toLowerCase(Locale.ENGLISH);
		this.value = value;
	}
	public String getName(){
		return name;
	}
	public boolean isName(String n){
		return name.equalsIgnoreCase(n);
	}
	
	public boolean isValue(String v){
		return value.equalsIgnoreCase(v);
	}
	
	public String getValue(){
		return value;
	}
	
	
	private String name = "";
	private String value ="";
}
