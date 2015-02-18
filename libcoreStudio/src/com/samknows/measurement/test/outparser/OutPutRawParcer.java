package com.samknows.measurement.test.outparser;

import java.text.SimpleDateFormat;
import java.util.List;

import com.samknows.libcore.SKConstants;
import com.samknows.measurement.CachingStorage;
import com.samknows.measurement.schedule.ScheduleConfig;
import com.samknows.measurement.test.outparser.ParcerDataType;
import com.samknows.measurement.util.OtherUtils;

public class OutPutRawParcer {
	private List<ParcerDataType> datas;
	private String source[];
	private ScheduleConfig config;
	
	public OutPutRawParcer(List<ParcerDataType> datas) {
		super();
		this.datas = datas;
		config = CachingStorage.getInstance().loadScheduleConfig();
	}

	public void setSource(String source) {
		this.source = source.split(SKConstants.RESULT_LINE_SEPARATOR);
	}

	public String[] parce() {
		String result[] = new String[datas.size()];
		for (int i = 0; i < datas.size(); i++) {
			result[i] = parce(i);
		}
		return result;
	}
	
	public boolean isSuccess() {
		return source[2].equals(SKConstants.RESULT_OK);
	}
	
	public String[] headersArray() {
		String result[] = new String[datas.size()];
		int i = 0;
		for (ParcerDataType type : datas) {
			result[i++] = type.header;
		}
		return result;
	}
	
	private String parce(int idx) {
		Object value = getValue(idx);
		ParcerDataType type = datas.get(idx);
		switch (type.type) {
		case FIELD_TIME:
			return new SimpleDateFormat().format((Long)value);
		case FIELD_SPEED: {
			return OtherUtils.formatToBits((Long)value) + "/s";
		}
		case FIELD_TARGET: {
			return value.toString();
		}
		case FIELD_P_LOSS: {
			return value + "%";
		}
		case FIELD_LATENCY:
		case FIELD_JITTER: {
			if (!isSuccess()) {
				return "";
			}
			return value + "ms";
		}
		default:
			throw new RuntimeException();
		}
	}
	
	//used to compare results
	public Comparable<?> getValue(int rowIdx) {
		ParcerDataType type = datas.get(rowIdx);
		switch (type.type) {
		case FIELD_TIME:
			return Long.valueOf(source[type.idx]) * 1000;
		case FIELD_SPEED: {
			return Long.valueOf(source[type.idx]);
		}
		case FIELD_TARGET: {
			return config.findHostName(source[type.idx]);
		}
		case FIELD_LATENCY: {
			if (!isSuccess()) {
				return "";
			}
			return Long.valueOf(source[type.idx])/1000;
		}
		case FIELD_P_LOSS: {
			if (!isSuccess()) {
				return "";
			}
			long loss = Long.valueOf(source[10]);
			long send = Long.valueOf(source[9]);
			return 100*loss/send;
		}
		case FIELD_JITTER: {
			if (!isSuccess()) { 
				return "";
			}
			return Long.valueOf(source[12])/1000;

		}
		default:
			throw new RuntimeException();
		}
	}
}
