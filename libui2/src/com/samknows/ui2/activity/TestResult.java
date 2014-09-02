package com.samknows.ui2.activity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class represents a test result.
 * It implements Parcelable to make it possible to pass a class object between activities.
 * 
 * All rights reserved SamKnows
 * @author pablo@samknows.com
 */


public class TestResult implements Parcelable
{
	// *** VARIABLES *** //
	private int networkType;
	private float downloadResult, uploadResult, latencyResult, jitterResult;
	private int packetLossResult;
	private long dtime;	
	private String simOperatorName, simOperatorCode, networkOperatorName, networkOperatorCode, roamingStatus, GSMCellTowerID, GSMLocationAreaCode, GSMSignalStrength,
					manufacturer, bearer, model, OSType, OSVersion, phoneType, latitude, longitude, accuracy, locationProvider;		
	
	public static final Parcelable.Creator<TestResult> CREATOR =
	   new Parcelable.Creator<TestResult>()
	   {
			@Override
		    public TestResult createFromParcel(Parcel pSource)
			{
				return new TestResult(pSource);
		    }
		
		    @Override
		    public TestResult[] newArray(int size)
		    {
		    	return new TestResult[size];
		    }
	   };

	// *** CONSTRUCTORS *** //
	public TestResult()
	{
		
	}
	
	public TestResult(Parcel pIn)
	{
		readFromParcel(pIn);		
	}
	
	@Override
	public int describeContents()
	{
		return 0;
	}

	/**
	 * Write the test result data to Parcel object
	 * 
	 * @param dest
	 * @param flags
	 */
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeLong(dtime);		
		
		dest.writeInt(networkType);		
		
		dest.writeFloat(downloadResult);
		dest.writeFloat(uploadResult);
		dest.writeFloat(latencyResult);
		dest.writeInt(packetLossResult);
		dest.writeFloat(jitterResult);
		
		dest.writeString(simOperatorName);
		dest.writeString(simOperatorCode);
		dest.writeString(networkOperatorName);
		dest.writeString(networkOperatorCode);
		dest.writeString(roamingStatus);
		dest.writeString(GSMCellTowerID);
		dest.writeString(GSMLocationAreaCode);
		dest.writeString(GSMSignalStrength);
		dest.writeString(manufacturer);
		dest.writeString(bearer);
		dest.writeString(model);
		dest.writeString(OSType);
		dest.writeString(OSVersion);
		dest.writeString(phoneType);
		dest.writeString(latitude);
		dest.writeString(longitude);
		dest.writeString(accuracy);
		dest.writeString(locationProvider);
	}	
	
	/**
	 * Read the test result data from Parcel object
	 * 
	 * @param in
	 */
	private void readFromParcel(Parcel in)
	{
		dtime = in.readLong();
		
		networkType = in.readInt();		
		
		simOperatorName = in.readString();
		simOperatorCode = in.readString();
		networkOperatorName = in.readString();
		networkOperatorCode = in.readString();
		roamingStatus = in.readString();
		GSMCellTowerID = in.readString();
		GSMLocationAreaCode = in.readString();
		GSMSignalStrength = in.readString();
		manufacturer = in.readString();
		bearer = in.readString();
		model = in.readString();
		OSType = in.readString();
		OSVersion = in.readString();
		phoneType = in.readString();
		latitude = in.readString();
		longitude = in.readString();
		accuracy = in.readString();
		locationProvider = in.readString();
		
		downloadResult = in.readFloat();
		uploadResult = in.readFloat();
		jitterResult = in.readFloat();
		latencyResult = in.readFloat();
		packetLossResult = in.readInt();
	}
	
	
	// *** GETTERS AND SETTERS *** //

	/**
	 * Get the test result time
	 * 
	 * @return dtime
	 */
	public long getDtime()
	{
		return dtime;
	}

	/**
	 * Set the test result time
	 * 
	 * @param pDtime
	 */
	public void setDtime(long pDtime)
	{
		this.dtime = pDtime;
	}

	/**
	 * Get the test result network type
	 * 
	 * @return networkType
	 */
	public int getNetworkType()
	{
		return networkType;
	}

	/**
	 * Set the test result network type
	 * 
	 * @param pNetworkType
	 */
	public void setNetworkType(int pNetworkType)
	{
		this.networkType = pNetworkType;
	}

	/**
	 * Get the test result sim operator name
	 * 
	 * @return simOperatorName
	 */
	public String getSimOperatorName()
	{
		return simOperatorName;
	}

	/**
	 * Set the test result sim operator name
	 * 
	 * @param pSimOperatorName
	 */
	public void setSimOperatorName(String pSimOperatorName)
	{
		this.simOperatorName = pSimOperatorName;
	}

	/**
	 * Get the test result sim operator code
	 * 
	 * @return simOperatorCode
	 */
	public String getSimOperatorCode()
	{
		return simOperatorCode;
	}

	/**
	 * Set the test result sim operator code
	 * 
	 * @param pSimOperatorCode
	 */
	public void setSimOperatorCode(String pSimOperatorCode)
	{
		this.simOperatorCode = pSimOperatorCode;
	}

	/**
	 * Get the test result network operator name
	 * 
	 * @return networkOperatorName
	 */
	public String getNetworkOperatorName()
	{
		return networkOperatorName;
	}

	/**
	 * Set the test result network operator name
	 * 
	 * @param pNetworkOperatorName
	 */
	public void setNetworkOperatorName(String pNetworkOperatorName)
	{
		this.networkOperatorName = pNetworkOperatorName;
	}

	/**
	 * Get the test result network operator code
	 * 
	 * @return networkOperatorCode
	 */
	public String getNetworkOperatorCode()
	{
		return networkOperatorCode;
	}

	/**
	 * Set the test result network operator code
	 * 
	 * @param pNetworkOperatorCode
	 */
	public void setNetworkOperatorCode(String pNetworkOperatorCode)
	{
		this.networkOperatorCode = pNetworkOperatorCode;
	}

	/**
	 * Get the test result roaming status
	 * 
	 * @return roamingStatus
	 */
	public String getRoamingStatus()
	{
		return roamingStatus;
	}

	/**
	 * Set the test result roaming status
	 * 
	 * @param pRoamingStatus
	 */
	public void setRoamingStatus(String pRoamingStatus)
	{
		this.roamingStatus = pRoamingStatus;
	}

	/**
	 * Get the test result gsm cell tower ID
	 * 
	 * @return GSMCellTowerID
	 */
	public String getGSMCellTowerID()
	{
		return GSMCellTowerID;
	}

	/**
	 * Set the test result GSM cell tower ID
	 * 
	 * @param pGSMCellTowerID
	 */
	public void setGSMCellTowerID(String pGSMCellTowerID)
	{
		GSMCellTowerID = pGSMCellTowerID;
	}

	/**
	 * Get the test result GSM location area code
	 * 
	 * @return GSMLocationAreaCode
	 */
	public String getGSMLocationAreaCode()
	{
		return GSMLocationAreaCode;
	}

	/**
	 * Set the test result GSM location area code
	 * 
	 * @param pGSMLocationAreaCode
	 */
	public void setGSMLocationAreaCode(String pGSMLocationAreaCode)
	{
		GSMLocationAreaCode = pGSMLocationAreaCode;
	}

	/**
	 * Get the test result GSM signal strength
	 * 
	 * @return GSMSignalStrength
	 */
	public String getGSMSignalStrength()
	{
		return GSMSignalStrength;
	}

	/**
	 * Set the test result GSM signal strength
	 * 
	 * @param gSMSignalStrength
	 */
	public void setGSMSignalStrength(String pGSMSignalStrength)
	{
		this.GSMSignalStrength = pGSMSignalStrength;
	}

	/**
	 * Get the test result manufactor
	 * 
	 * @return manufactor
	 */
	public String getManufactor()
	{
		return manufacturer;
	}

	/**
	 * Set the test result manufactor
	 * 
	 * @param pManufacturer
	 */
	public void setManufactor(String pManufacturer)
	{
		this.manufacturer = pManufacturer;
	}
	
	/**
	 * Get the test result bearer
	 * 
	 * @return bearer
	 */
	public String getBearer()
	{
		return bearer;
	}

	/**
	 * Set the test result bearer
	 * 
	 * @param pBearer
	 */
	public void setBearer(String pBearer)
	{
		this.bearer = pBearer;
	}
	
	/**
	 * Get the test result phone model
	 * 
	 * @return model
	 */
	public String getModel()
	{
		return model;
	}

	/**
	 * Set the test result phone model
	 * 
	 * @param pModel
	 */
	public void setModel(String pModel)
	{
		this.model = pModel;
	}

	/**
	 * Get the test result phone OS type
	 * 
	 * @return OSType
	 */
	public String getOSType()
	{
		return OSType;
	}

	/**
	 * Set the test result phone OS type
	 * 
	 * @param pOSType
	 */
	public void setOSType(String pOSType)
	{
		OSType = pOSType;
	}

	/**
	 * Get the test result phone OS version
	 * 
	 * @return OSVersion
	 */
	public String getOSVersion()
	{
		return OSVersion;
	}

	/**
	 * Set the test result phone OS version
	 * 
	 * @param pOSVersion
	 */
	public void setOSVersion(String pOSVersion)
	{
		OSVersion = pOSVersion;
	}

	/**
	 * Get the test result phone type
	 * 
	 * @return phoneType
	 */
	public String getPhoneType()
	{
		return phoneType;
	}

	/**
	 * Set the test result phone type
	 * 
	 * @param pPhoneType
	 */
	public void setPhoneType(String pPhoneType)
	{
		this.phoneType = pPhoneType;
	}

	/**
	 * Get the test result location latitude
	 * 
	 * @return latitude
	 */
	public String getLatitude()
	{
		return latitude;
	}

	/**
	 * Set the test result location latitude
	 * 
	 * @param pLatitude
	 */
	public void setLatitude(String pLatitude)
	{
		this.latitude = pLatitude;
	}

	/**
	 * Get the test result location longitude
	 * 
	 * @return longitude
	 */
	public String getLongitude()
	{
		return longitude;
	}

	/**
	 * Set the test result location latitude
	 * 
	 * @param pLongitude
	 */
	public void setLongitude(String pLongitude)
	{
		this.longitude = pLongitude;
	}

	/**
	 * Get the test result locationaccuracy
	 * 
	 * @return accuracy
	 */
	public String getAccuracy()
	{
		return accuracy;
	}

	/**
	 * Set the test result location accuracy
	 * 
	 * @param pAccuracy
	 */
	public void setAccuracy(String pAccuracy)
	{
		this.accuracy = pAccuracy;
	}

	/**
	 * Get the test result location provider
	 * 
	 * @return locationProvider
	 */
	public String getLocationProvider()
	{
		return locationProvider;
	}

	/**
	 * Set the test result location provider
	 * 
	 * @param pLocationProvider
	 */
	public void setLocationProvider(String pLocationProvider)
	{
		this.locationProvider = pLocationProvider;
	}

	/**
	 * Get the test result download result
	 * 
	 * @return downloadResult
	 */
	public float getDownloadResult()
	{
		return downloadResult;
	}

	/**
	 * Set the test result download
	 * 
	 * @param pDownloadResult
	 */
	public void setDownloadResult(float pDownloadResult)
	{
		this.downloadResult = pDownloadResult;
	}

	/**
	 * Get the test result upload
	 * 
	 * @return uploadResult
	 */
	public float getUploadResult()
	{
		return uploadResult;
	}

	/**
	 * Set the test result upload
	 * 
	 * @param pUploadResult
	 */
	public void setUploadResult(float pUploadResult)
	{
		this.uploadResult = pUploadResult;
	}

	/**
	 * Get the test result latency
	 * 
	 * @return latencyResult
	 */
	public float getLatencyResult()
	{
		return latencyResult;
	}

	/**
	 * Set the test result latency
	 * 
	 * @param pLatencyResult
	 */
	public void setLatencyResult(float pLatencyResult)
	{
		this.latencyResult = pLatencyResult;
	}

	/**
	 * Get the test result packet loss
	 * 
	 * @return packetLossResult
	 */
	public int getPacketLossResult()
	{
		return packetLossResult;
	}

	/**
	 * Set the test result packet loss
	 * 
	 * @param pPacketLossResult
	 */
	public void setPacketLossResult(int pPacketLossResult)
	{
		this.packetLossResult = pPacketLossResult;
	}
	
	/**
	 * Get the test result jitter
	 * 
	 * @return jitterResult
	 */
	public float getJitterResult()
	{
		return jitterResult;
	}

	/**
	 * Set the test result
	 * 
	 * @param pJitterResult
	 */
	public void setJitterResult(int pJitterResult)
	{
		this.jitterResult = pJitterResult;
	}
}
