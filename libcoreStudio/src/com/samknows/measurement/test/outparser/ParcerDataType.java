package com.samknows.measurement.test.outparser;

import java.io.Serializable;

public class ParcerDataType implements Serializable{
	private static final long serialVersionUID = 1L;
	public int idx;
	public ParcerFieldType type;
	public String header;
	public ParcerDataType(int idx, ParcerFieldType type, String header) {
		super();
		this.idx = idx;
		this.type = type;
		this.header = header;
	}
	
}

