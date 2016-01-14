package com.samknows.measurement.environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.os.Build;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.SignalStrength;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.storage.PassiveMetric;
import com.samknows.measurement.util.DCSConvertorUtil;
import com.samknows.measurement.util.DCSStringBuilder;
import com.samknows.measurement.util.SKDateFormat;
import com.samknows.measurement.util.SKGsmSignalStrength;

public class CellTowersData implements DCSData{
	private static final String ID = "CELLTOWER";
	private static final String ID_NEIGHBOR = "CELLTOWERNEIGHBOR";
	private static final String GSM = "GSM";
	private static final String CDMA = "CDMA";
	private static final String VERSION = ".2";
	

	//JSONOutput for the neighbour cell tower
	public static final String JSON_TYPE_CELL_TOWER_NEIGHBOUR= "cell_neighbour_tower_data";
	public static final String JSON_CELL_TOWER_ID= "cell_tower_id";
	public static final String JSON_LOCATION_AREA_CODE = "location_area_code";
	public static final String JSON_UMTS_PSC = "umts_psc";
	public static final String JSON_RSSI = "rssi";
	public static final String JSON_NETWORK_TYPE = "network_type";
	public static final String JSON_NETWORK_TYPE_CODE = "network_type_code";
	
	public static final String JSON_TYPE_GSM_CELL_LOCATION = "gsm_cell_location";
	public static final String JSON_TYPE_CDMA_CELL_LOCATION = "cdma_cell_location";
	public static final String JSON_BASE_STATION_ID = "base_station_id";
	public static final String JSON_BASE_STATION_LATITUDE = "base_station_latitude";
	public static final String JSON_BASE_STATION_LONGITUDE = "base_station_longitude";
	public static final String JSON_NETWORK_ID = "netwok_id";
	public static final String JSON_SYSTEM_ID = "system_id";
	public static final String JSON_SIGNAL_STRENGTH = "signal_strength";
	public static final String JSON_BIT_ERROR_RATE = "bit_error_rate";
	public static final String JSON_DBM = "dbm";
	public static final String JSON_ECIO = "ecio";
	
	
	private CellLocation cellLocation = null;
	private SignalStrength signal = null;
	// time in millseconds
	private long time = 0L;
	
	public CellLocation getCellLocation() {
		return cellLocation;
	}
	public SignalStrength getSignal() {
		return signal;
	}
	// time in millseconds
	public long getTime() {
		return time;
	}

	public void setCellLocation(CellLocation value) {
		// We might be passed null, if the location is not available currently.
		// This is perfectly valid!
		// SKLogger.sAssert(getClass(), value != null);
		cellLocation = value;
	}
	public void setSignal(SignalStrength value) {
		// We might be passed null, if the signal is not available currently.
		// This is perfectly valid!
		// SKLogger.sAssert(getClass(), value != null);
		signal = value;
	}
	// time in millseconds
	public void setTime(long value) {
		time = value;
	}

	// Note that this value is NEVER allowed to be set null.
	private List<NeighboringCellInfo> neighbors = new ArrayList<>(); // TODO - remove me, for testing only!
	
	public void setNeighbors(List<NeighboringCellInfo> inNeighbors) {
		
		// We NEVER allow the neighbors member to be set null.
		// Otherwise, tests might be mis-reported as failing.
		// If we're supplied a null value, set to use an empty list.
		if (inNeighbors == null) {
			// ... we should trap this where possible in the debugger...
			SKLogger.sAssert(getClass(), false);
            neighbors = new ArrayList<>();
		} else {
			neighbors = inNeighbors;
		}
	}
	public List<NeighboringCellInfo> getNeighbors() {
		return neighbors;
	}
	
	public int network_type;
	@Override
	public List<String> convert() {
		List<String> list = new ArrayList<>();
		addCellData(list);
		
		SKLogger.sAssert(getClass(), (neighbors != null));
		if (neighbors != null) {
			for (NeighboringCellInfo cellInfo : neighbors) {
				addCellData(list, cellInfo);
			}
		}
		return list;
	}
	
	
	private void addCellData(List<String> list, NeighboringCellInfo cellInfo) {
		DCSStringBuilder builder = new DCSStringBuilder();
		builder.append(ID_NEIGHBOR);
		builder.append(time/1000);
		builder.append(cellInfo.getCid());
		builder.append(cellInfo.getLac());
		builder.append(cellInfo.getPsc() );
		builder.append(cellInfo.getRssi());
		builder.append(DCSConvertorUtil.convertNetworkType(cellInfo.getNetworkType()));
		list.add(builder.build());
	}

	@SuppressLint("NewApi")
	private void addCellData(List<String> list) {
		DCSStringBuilder builder = new DCSStringBuilder();
	
		if (cellLocation == null) {
			// No location information currently available!
		} else if (cellLocation instanceof GsmCellLocation) {
			GsmCellLocation gsmLocation = (GsmCellLocation) cellLocation;
			builder.append(ID + GSM + VERSION);
			builder.append(time/1000);
			builder.append(GSM);
			builder.append(gsmLocation.getCid());
			builder.append(gsmLocation.getLac());
			builder.append(Build.VERSION.SDK_INT >= 9 ? gsmLocation.getPsc() : -1 );
			
		} else if (cellLocation instanceof CdmaCellLocation) {
			CdmaCellLocation cdmaLocation = (CdmaCellLocation) cellLocation;
			builder.append(ID + CDMA);
			builder.append(time/1000);
			builder.append(CDMA);
			builder.append(cdmaLocation.getBaseStationId());
			builder.append(cdmaLocation.getBaseStationLatitude());
			builder.append(cdmaLocation.getBaseStationLongitude());
			builder.append(cdmaLocation.getNetworkId());
			builder.append(cdmaLocation.getSystemId());
		}
		
		if (signal == null) {
			// No signal information currently available!
		} else if (signal.isGsm()) {
			builder.append(SKGsmSignalStrength.getGsmSignalStrength(signal));
			builder.append(signal.getGsmBitErrorRate());
		} else {
			builder.append(signal.getCdmaDbm());
			builder.append(signal.getCdmaEcio());
		}
		list.add(builder.build());
	}

	
	
	
	@Override
	public List<JSONObject> getPassiveMetric() {
		List<JSONObject> ret = new ArrayList<>();
		
		if (cellLocation == null) {
			// No location information currently available!
		} else if(cellLocation instanceof GsmCellLocation){
			ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.GSMLAC,time,((GsmCellLocation) cellLocation).getLac()+""));
			ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.GSMCID,time,((GsmCellLocation) cellLocation).getCid()+""));
		}else if(cellLocation instanceof CdmaCellLocation){
			CdmaCellLocation cdmaLocation = (CdmaCellLocation) cellLocation;
			ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.CDMABSID, time, cdmaLocation.getBaseStationId()+""));
			if (cdmaLocation.getBaseStationLatitude() != Integer.MAX_VALUE) {
				ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.CDMABSLAT, time,cdmaLocation.getBaseStationLatitude()+""));
			} 
			if (cdmaLocation.getBaseStationLongitude() != Integer.MAX_VALUE) {
				ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.CDMABSLNG, time, cdmaLocation.getBaseStationLongitude()+""));
			}
			ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.CDMANETWORKID,time,cdmaLocation.getNetworkId()+""));
			ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.CDMASYSTEMID,time, cdmaLocation.getSystemId()+""));
		}
		
		if (signal == null) {
			// No signal information currently available!
		} else if (signal.isGsm()) {
			ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.GSMSIGNALSTRENGTH,time, DCSConvertorUtil.convertGsmSignalStrength(SKGsmSignalStrength.getGsmSignalStrength(signal))));
		//	ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.GSMBER, time, DCSConvertorUtil.convertGsmBitErroRate(signal.getGsmBitErrorRate())));
		} else {
			ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.CDMADBM,time, signal.getCdmaDbm()+""));
			ret.add(PassiveMetric.create(PassiveMetric.METRIC_TYPE.CDMAECIO,time, signal.getCdmaEcio()+""));
		}
		return ret;
	}


	@SuppressLint("NewApi")
	@Override
	public List<JSONObject> convertToJSON() {
		List<JSONObject> ret = new ArrayList<>();

		if (cellLocation == null) {
			// No location information currently available!
		} else if(cellLocation instanceof GsmCellLocation){
			GsmCellLocation l = (GsmCellLocation) cellLocation;
			Map<String, Object> gsm = new HashMap<>();
			gsm.put(JSON_TYPE, JSON_TYPE_GSM_CELL_LOCATION);
			gsm.put(JSON_TIMESTAMP, time/1000);
			gsm.put(JSON_DATETIME, SKDateFormat.sGetDateAsIso8601String(new java.util.Date(time)));
			gsm.put(JSON_CELL_TOWER_ID, l.getCid());
			gsm.put(JSON_LOCATION_AREA_CODE, l.getLac());
			gsm.put(JSON_UMTS_PSC, Build.VERSION.SDK_INT >= 9 ? l.getPsc() : -1);
			
			if (signal == null) {
				SKLogger.sAssert(getClass(), false);
			} else {
				if(signal.isGsm()){
					gsm.put(JSON_SIGNAL_STRENGTH, SKGsmSignalStrength.getGsmSignalStrength(signal));
				}
			}
			ret.add(new JSONObject(gsm));

		} else if(cellLocation instanceof CdmaCellLocation){
			CdmaCellLocation l = (CdmaCellLocation) cellLocation;
			Map<String, Object> cdma = new HashMap<>();
			cdma.put(JSON_TYPE,JSON_TYPE_CDMA_CELL_LOCATION);
			cdma.put(JSON_TIMESTAMP, time/1000);
			cdma.put(JSON_DATETIME, SKDateFormat.sGetDateAsIso8601String(new java.util.Date(time)));
			cdma.put(JSON_BASE_STATION_ID, l.getBaseStationId());
			cdma.put(JSON_BASE_STATION_LATITUDE, l.getBaseStationLatitude());
			cdma.put(JSON_BASE_STATION_LONGITUDE, l.getBaseStationLongitude());
			cdma.put(JSON_SYSTEM_ID, l.getSystemId());
			cdma.put(JSON_NETWORK_ID, l.getNetworkId());
			
			if (signal == null) {
				// No signal information currently available!
			} else if(!signal.isGsm()) {
				cdma.put(JSON_DBM, signal.getCdmaDbm());
				cdma.put(JSON_ECIO, signal.getCdmaEcio());
			}
			
			ret.add(new JSONObject(cdma));
		}

		SKLogger.sAssert(getClass(), (neighbors != null));
		if (neighbors != null) {
			for (NeighboringCellInfo cellInfo : neighbors) {
				Map<String, Object> neighbour = new HashMap<>();
				neighbour.put(JSON_TYPE, JSON_TYPE_CELL_TOWER_NEIGHBOUR);
				neighbour.put(JSON_TIMESTAMP, time/1000);
				neighbour.put(JSON_DATETIME, SKDateFormat.sGetDateAsIso8601String(new java.util.Date(time)));
				neighbour.put(JSON_NETWORK_TYPE_CODE,cellInfo.getNetworkType());
				neighbour.put(JSON_NETWORK_TYPE, DCSConvertorUtil.convertNetworkType(cellInfo.getNetworkType()));
				neighbour.put(JSON_RSSI, cellInfo.getRssi());
				neighbour.put(JSON_UMTS_PSC, cellInfo.getPsc());
				neighbour.put(JSON_CELL_TOWER_ID, cellInfo.getCid());
				neighbour.put(JSON_LOCATION_AREA_CODE, cellInfo.getLac());
				ret.add(new JSONObject(neighbour));
			}
		}

		return ret;
	}
}
