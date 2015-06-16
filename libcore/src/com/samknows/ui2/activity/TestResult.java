package com.samknows.ui2.activity;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.R;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.SKApplication.eNetworkTypeResults;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * This class represents a test result.
 * It implements Parcelable to make it possible to pass a class object between activities.
 * <p/>
 * All rights reserved SamKnows
 *
 * @author pablo@samknows.com
 */


public class TestResult implements Parcelable {
  // *** VARIABLES *** //
  private eNetworkTypeResults networkType = eNetworkTypeResults.eNetworkTypeResults_WiFi;
  private String downloadResult = "0", uploadResult = "0", latencyResult = "0", jitterResult = "0";
  private String packetLossResult = "0";
  private long dtime;
  private String simOperatorName, simOperatorCode, networkOperatorName, networkOperatorCode, roamingStatus, GSMCellTowerID, GSMLocationAreaCode, GSMSignalStrength,
      manufacturer, bearer, model, OSType, OSVersion, phoneType, latitude, longitude, accuracy, locationProvider;
  private String publicIp = "";
  private String submissionId = "";
  private String targetServerLocation = "";

  public static final Parcelable.Creator<TestResult> CREATOR =
      new Parcelable.Creator<TestResult>() {
        @Override
        public TestResult createFromParcel(Parcel pSource) {
          return new TestResult(pSource);
        }

        @Override
        public TestResult[] newArray(int size) {
          return new TestResult[size];
        }
      };

  // *** CONSTRUCTORS *** //
  public TestResult() {

  }

  public TestResult(Parcel pIn) {
    readFromParcel(pIn);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  /**
   * Write the test result data to Parcel object
   *
   * @param dest
   * @param flags
   */
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(dtime);

    switch (networkType) {
      case eNetworkTypeResults_Any:
        SKLogger.sAssert(getClass(), false);
        break;
      case eNetworkTypeResults_WiFi:
        dest.writeInt(1);
        break;
      case eNetworkTypeResults_Mobile:
        dest.writeInt(2);
        break;
      default:
        break;
    }

    dest.writeString(downloadResult);
    dest.writeString(uploadResult);
    dest.writeString(latencyResult);
    dest.writeString(packetLossResult);
    dest.writeString(jitterResult);

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
    dest.writeString(publicIp);
    dest.writeString(submissionId);
    dest.writeString(targetServerLocation);
  }

  /**
   * Read the test result data from Parcel object
   *
   * @param in
   */
  private void readFromParcel(Parcel in) {
    dtime = in.readLong();

    switch (in.readInt()) {
      case 0:
        SKLogger.sAssert(getClass(), false);
        break;
      case 1:
        networkType = eNetworkTypeResults.eNetworkTypeResults_WiFi;
        break;
      case 2:
        networkType = eNetworkTypeResults.eNetworkTypeResults_Mobile;
        break;
      default:
        break;
    }

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

    downloadResult = in.readString();
    uploadResult = in.readString();
    jitterResult = in.readString();
    latencyResult = in.readString();
    packetLossResult = in.readString();
    publicIp = in.readString();
    submissionId = in.readString();
    targetServerLocation = in.readString();
  }


  // *** GETTERS AND SETTERS *** //

  /**
   * Get the test result time
   *
   * @return dtime
   */
  public long getDtime() {
    return dtime;
  }

  /**
   * Set the test result time
   *
   * @param pDtime
   */
  public void setDtime(long pDtime) {
    this.dtime = pDtime;
  }

  /**
   * Get the test result network type
   *
   * @return networkType
   */
  public eNetworkTypeResults getNetworkType() {
    return networkType;
  }


  /**
   * Get the test result network type
   *
   * @return networkType
   */
  public String getNetworkTypeAsString() {
    Context context = SKApplication.getAppInstance().getApplicationContext();

    switch (networkType) {
      case eNetworkTypeResults_WiFi:
         return context.getString(R.string.network_type_wifi);
      case eNetworkTypeResults_Mobile:
        return context.getString(R.string.network_type_mobile);
      case eNetworkTypeResults_Any:
      default:
        return context.getString(R.string.network_type_all);
    }
  }

  /**
   * Set the test result network type
   *
   * @param pNetworkType
   */
  public void setNetworkType(eNetworkTypeResults pNetworkType) {
    switch (pNetworkType) {
      case eNetworkTypeResults_Any:
        SKLogger.sAssert(getClass(), false);
        break;
      case eNetworkTypeResults_WiFi:
        this.networkType = pNetworkType;
        break;
      case eNetworkTypeResults_Mobile:
        this.networkType = pNetworkType;
        break;
      default:
        SKLogger.sAssert(getClass(), false);
        break;
    }
  }

  /**
   * Get the test result sim operator name
   *
   * @return simOperatorName
   */
  public String getSimOperatorName() {
    return simOperatorName;
  }

  /**
   * Set the test result sim operator name
   *
   * @param pSimOperatorName
   */
  public void setSimOperatorName(String pSimOperatorName) {
    this.simOperatorName = pSimOperatorName;
  }

  /**
   * Get the test result sim operator code
   *
   * @return simOperatorCode
   */
  public String getSimOperatorCode() {
    return simOperatorCode;
  }

  /**
   * Set the test result sim operator code
   *
   * @param pSimOperatorCode
   */
  public void setSimOperatorCode(String pSimOperatorCode) {
    this.simOperatorCode = pSimOperatorCode;
  }

  /**
   * Get the test result network operator name
   *
   * @return networkOperatorName
   */
  public String getNetworkOperatorName() {
    return networkOperatorName;
  }

  /**
   * Set the test result network operator name
   *
   * @param pNetworkOperatorName
   */
  public void setNetworkOperatorName(String pNetworkOperatorName) {
    this.networkOperatorName = pNetworkOperatorName;
  }

  /**
   * Get the test result network operator code
   *
   * @return networkOperatorCode
   */
  public String getNetworkOperatorCode() {
    return networkOperatorCode;
  }

  /**
   * Set the test result network operator code
   *
   * @param pNetworkOperatorCode
   */
  public void setNetworkOperatorCode(String pNetworkOperatorCode) {
    this.networkOperatorCode = pNetworkOperatorCode;
  }

  /**
   * Get the test result roaming status
   *
   * @return roamingStatus
   */
  public String getRoamingStatus() {
    return roamingStatus;
  }

  /**
   * Set the test result roaming status
   *
   * @param pRoamingStatus
   */
  public void setRoamingStatus(String pRoamingStatus) {
    this.roamingStatus = pRoamingStatus;
  }

  /**
   * Get the test result gsm cell tower ID
   *
   * @return GSMCellTowerID
   */
  public String getGSMCellTowerID() {
    return GSMCellTowerID;
  }

  /**
   * Set the test result GSM cell tower ID
   *
   * @param pGSMCellTowerID
   */
  public void setGSMCellTowerID(String pGSMCellTowerID) {
    GSMCellTowerID = pGSMCellTowerID;
  }

  /**
   * Get the test result GSM location area code
   *
   * @return GSMLocationAreaCode
   */
  public String getGSMLocationAreaCode() {
    return GSMLocationAreaCode;
  }

  /**
   * Set the test result GSM location area code
   *
   * @param pGSMLocationAreaCode
   */
  public void setGSMLocationAreaCode(String pGSMLocationAreaCode) {
    GSMLocationAreaCode = pGSMLocationAreaCode;
  }

  /**
   * Get the test result GSM signal strength
   *
   * @return GSMSignalStrength
   */
  public String getGSMSignalStrength() {
    return GSMSignalStrength;
  }

  /**
   * Set the test result GSM signal strength
   *
   * @param gSMSignalStrength
   */
  public void setGSMSignalStrength(String pGSMSignalStrength) {
    this.GSMSignalStrength = pGSMSignalStrength;
  }

  /**
   * Get the test result manufactor
   *
   * @return manufactor
   */
  public String getManufactor() {
    return manufacturer;
  }

  /**
   * Set the test result manufactor
   *
   * @param pManufacturer
   */
  public void setManufactor(String pManufacturer) {
    this.manufacturer = pManufacturer;
  }

  /**
   * Get the test result bearer
   *
   * @return bearer
   */
  public String getBearer() {
    return bearer;
  }

  /**
   * Set the test result bearer
   *
   * @param pBearer
   */
  public void setBearer(String pBearer) {
    this.bearer = pBearer;
  }

  /**
   * Get the test result phone model
   *
   * @return model
   */
  public String getModel() {
    return model;
  }

  /**
   * Set the test result phone model
   *
   * @param pModel
   */
  public void setModel(String pModel) {
    this.model = pModel;
  }

  /**
   * Get the test result phone OS type
   *
   * @return OSType
   */
  public String getOSType() {
    return OSType;
  }

  /**
   * Set the test result phone OS type
   *
   * @param pOSType
   */
  public void setOSType(String pOSType) {
    OSType = pOSType;
  }

  /**
   * Get the test result phone OS version
   *
   * @return OSVersion
   */
  public String getOSVersion() {
    return OSVersion;
  }

  /**
   * Set the test result phone OS version
   *
   * @param pOSVersion
   */
  public void setOSVersion(String pOSVersion) {
    OSVersion = pOSVersion;
  }

  /**
   * Get the test result phone type
   *
   * @return phoneType
   */
  public String getPhoneType() {
    return phoneType;
  }

  /**
   * Set the test result phone type
   *
   * @param pPhoneType
   */
  public void setPhoneType(String pPhoneType) {
    this.phoneType = pPhoneType;
  }

  /**
   * Get the test result location latitude
   *
   * @return latitude
   */
  public String getLatitude() {
    return latitude;
  }

  /**
   * Set the test result location latitude
   *
   * @param pLatitude
   */
  public void setLatitude(String pLatitude) {
    this.latitude = pLatitude;
  }

  /**
   * Get the test result location longitude
   *
   * @return longitude
   */
  public String getLongitude() {
    return longitude;
  }

  /**
   * Set the test result location latitude
   *
   * @param pLongitude
   */
  public void setLongitude(String pLongitude) {
    this.longitude = pLongitude;
  }

  /**
   * Get the test result locationaccuracy
   *
   * @return accuracy
   */
  public String getAccuracy() {
    return accuracy;
  }

  /**
   * Set the test result location accuracy
   *
   * @param pAccuracy
   */
  public void setAccuracy(String pAccuracy) {
    this.accuracy = pAccuracy;
  }

  /**
   * Get the test result location provider
   *
   * @return locationProvider
   */
  public String getLocationProvider() {
    return locationProvider;
  }

  public String getPublicIp() {
    return publicIp;
  }

  public String getSubmissionId() {
    return submissionId;
  }
  public String getTargetServerLocation() {
    return targetServerLocation;
  }

  /**
   * Set the test result location provider
   *
   * @param pLocationProvider
   */
  public void setLocationProvider(String pLocationProvider) {
    this.locationProvider = pLocationProvider;
  }

  public void setPublicIp(String value) {
    this.publicIp = value;
  }

  public void setSubmissionId(String value) {
    this.submissionId = value;
  }

  public void setTargetServerLocation(String value) {
    this.targetServerLocation = value;
  }

  /**
   * Get the test result download result
   *
   * @return downloadResult
   */
  public String getDownloadResult() {
    return downloadResult;
  }

  /**
   * Set the test result download
   *
   * @param pDownloadResult
   */
  public void setDownloadResult(String pDownloadResult) {
    this.downloadResult = pDownloadResult;
  }

  /**
   * Get the test result upload
   *
   * @return uploadResult
   */
  public String getUploadResult() {
    return uploadResult;
  }

  /**
   * Set the test result upload
   *
   * @param pUploadResult
   */
  public void setUploadResult(String pUploadResult) {
    this.uploadResult = pUploadResult;
  }

  /**
   * Get the test result latency
   *
   * @return latencyResult
   */
  public String getLatencyResult() {
    return latencyResult;
  }

  /**
   * Set the test result latency
   *
   * @param pLatencyResult
   */
  public void setLatencyResult(String pLatencyResult) {
    this.latencyResult = pLatencyResult;
  }

  /**
   * Get the test result packet loss
   *
   * @return packetLossResult
   */
  public String getPacketLossResult() {
    return packetLossResult;
  }

  /**
   * Set the test result packet loss
   *
   * @param pPacketLossResult
   */
  public void setPacketLossResult(String pPacketLossResult) {
    this.packetLossResult = pPacketLossResult;
  }

  /**
   * Get the test result jitter
   *
   * @return jitterResult
   */
  public String getJitterResult() {
    return jitterResult;
  }

  /**
   * Set the test result
   *
   * @param pJitterResult
   */
  public void setJitterResult(String pJitterResult) {
    Log.d("MPC TESTRESULT", "JITTER pJitterResult=" + pJitterResult);
    this.jitterResult = pJitterResult;
  }
}
