package com.samknows.measurement.schedule;

import android.util.Log;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKConstants;
import com.samknows.measurement.schedule.TestDescription.*;
import com.samknows.measurement.TestRunner.ManualTestRunner;
import com.samknows.measurement.schedule.condition.ConditionGroup;
import com.samknows.measurement.schedule.datacollection.BaseDataCollector;
import com.samknows.measurement.schedule.failaction.RetryFailAction;
import com.samknows.measurement.util.OtherUtils;
import com.samknows.measurement.util.XmlUtils;

public class ScheduleConfig implements Serializable {
  //strings found in the schedule config
  public static final String CONFIG = "config";
  public static final String GLOBAL = "global";
  public static final String BACKGROUND_TEST = "background-test";
  public static final String SCHEDULE_VERSION = "schedule-version";
  public static final String SUBMIT_DCS = "submit-dcs";
  public static final String TESTS_ALARM_TYPE = "tests-alarm-type";
  public static final String LOCATION_SERVICE = "location-service";
  public static final String ONFAIL_TEST_ACTION = "onfail-test-action";
  public static final String INIT = "init";
  public static final String TYPE = "type";
  public static final String HOSTS = "hosts";
  public static final String HOST = "host";
  public static final String DNSNAME = "dnsName";
  public static final String DISPLAYNAME = "displayName";
  public static final String DATA_CAP_DEFAULT = "data-cap-default";
  public static final String VALUE = "value";
  public static final String COMMUNICATIONS = "communications";
  public static final String COMMUNICATION = "communication";
  public static final String DATA_COLLECTOR = "data-collector";
  public static final String TIME = "time";
  public static final String RANDOM_INTERVAL = "random-interval";
  public static final String LISTENERDELAY = "listenerDelay";
  public static final String ENABLED = "enabled";
  public static final String DISABLED = "disabled";
  public static final String CONDITIONS = "conditions";
  public static final String CONDITION = "condition";
  public static final String CONDITION_GROUP = "condition-group";
  public static final String CONDITION_GROUP_ID = "condition-group-id";
  public static final String FAIL_QUIET = "fail-quiet";
  public static final String ID = "id";
  public static final String TESTS = "tests";
  public static final String TEST = "test";
  public static final String SCHEDULED_TESTS = "scheduled-tests";
  public static final String CONTINUOUS_TESTS = "continuous-tests";
  public static final String BATCH = "batch";
  public static final String MANUAL_TESTS = "manual-tests";

  private static final long serialVersionUID = 1L;

  public String version = "";
  public String submitHost; // submitHost/mobile/submit
  public long downloadedTime;
  public long dataCapDefault;
  public TestAlarmType testAlamType;
  public LocationType locationType;  //location type for data collectors
  public RetryFailAction retryFailAction;
  private boolean backgroundTest = true;
  public List<ConditionGroup> conditionGroups = new ArrayList<ConditionGroup>();
  public List<TestDescription> tests = new ArrayList<TestDescription>();
  public List<TestGroup> backgroundTestGroups = new ArrayList<TestGroup>();
  public List<TestDescription> manual_tests = new ArrayList<TestDescription>();
  public List<TestDescription> continuous_tests = new ArrayList<TestDescription>();
  public String manual_test_condition_group_id;
  public List<BaseDataCollector> dataCollectors = new ArrayList<BaseDataCollector>();
  public HashMap<String, String> hosts = new HashMap<String, String>();
  public HashMap<String, Communication> communications = new HashMap<String, Communication>();
  public long maximumTestUsage = 0;

  public enum TestAlarmType {
    WAKEUP, NO_WAKEUP
  }

  public boolean getBackgroundTest() {
    return backgroundTest;
  }

  public enum LocationType {
    gps, network
  }

  public ConditionGroup getConditionGroup(String conditionGroupId) {
    for (ConditionGroup cg : conditionGroups) {
      if (cg.id.equals(conditionGroupId)) {
        return cg;
      }
    }
    SKLogger.e(this, "condition group not found for id: " + conditionGroupId);
    return new ConditionGroup();
  }

  public TestDescription findTest(long testId) {
    for (TestDescription td : tests) {
      if (td.id == testId) return td;
    }
    return null;
  }

  public TestGroup findBackgroundTestGroup(long id) {
    for (TestGroup tg : backgroundTestGroups) {
      if (tg.id == id) {
        return tg;
      }
    }

    // Nothing yet found, for some reason!
    Log.w("ScheduleConfig", "WARNING: no schedule test group found, returning first item");
    if (backgroundTestGroups.size() > 0) {
      return backgroundTestGroups.get(0);
    }

    SKLogger.sAssert(false);

    return null;
  }

  public TestDescription findTestById(SCHEDULE_TEST_ID id) {
    for (TestDescription td : tests) {
      if (td.testId == id) {
        return td;
      }
    }

    // Unrecognized test id!
    SKLogger.sAssert(getClass(), false);

    return null;
  }

  public TestDescription findTestForType(String type) {
    for (TestDescription td : tests) {
      if (td.type.equals(type)) return td;
    }
    return null;
  }

  /*
   * Returns the test batch to be run in the RunTestActivity
   */
  public List<TestDescription> testGroup() {
    List<TestDescription> ret = new ArrayList<TestDescription>();
    //Closest Target
    TestDescription td = findTestForType(SKConstants.TEST_TYPE_CLOSEST_TARGET);
    if (td != null) {
      ret.add(td);
    }
    td = findTestForType(SKConstants.TEST_TYPE_DOWNLOAD);
    if (td != null) {
      ret.add(td);
    }
    td = findTestForType(SKConstants.TEST_TYPE_UPLOAD);
    if (td != null) {
      ret.add(td);
    }
    td = findTestForType(SKConstants.TEST_TYPE_LATENCY);
    if (td != null) {
      ret.add(td);
    }
    return ret;
  }


  public Communication findCommunication(String id) {
    return communications.get(id);
  }

  public String findHostName(String dnsName) {
    String result = hosts.get(dnsName);
    if (result == null) {
      return dnsName;
    }
    return result;
  }

  //------------------------------------------------------------------------
  //parsing from xml
  public static ScheduleConfig parseXml(InputStream is) {


    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    try {
      Element root = factory.newDocumentBuilder().parse(is).getDocumentElement();
      return parseXml(root);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static ScheduleConfig parseXml(Element node) {

    ScheduleConfig c = new ScheduleConfig();

    c.downloadedTime = System.currentTimeMillis();

    //version
    c.version = XmlUtils.getNodeAttrValue(node, SCHEDULE_VERSION, VALUE);

    //base properties
    c.submitHost = XmlUtils.getNodeAttrValue(node, SUBMIT_DCS, HOST);
    String dataCapValue = XmlUtils.getNodeAttrValue(node, DATA_CAP_DEFAULT, VALUE);
    if (dataCapValue != null && !dataCapValue.equals("")) {
      c.dataCapDefault = Long.parseLong(dataCapValue);
    } else {
      c.dataCapDefault = -1;
    }

    String type = XmlUtils.getNodeAttrValue(node, TESTS_ALARM_TYPE, TYPE);
    c.testAlamType = TestAlarmType.valueOf(type);

    c.locationType = LocationType.valueOf(XmlUtils.getNodeAttrValue(node, LOCATION_SERVICE, TYPE));
    c.retryFailAction = RetryFailAction.parseXml((Element) node.getElementsByTagName(ONFAIL_TEST_ACTION).item(0));

    //conditions
    c.conditionGroups = new ArrayList<ConditionGroup>();
    NodeList conditionGroups = node.getElementsByTagName(CONDITION_GROUP);
    for (int i = 0; i < conditionGroups.getLength(); i++) {
      Element conditionGroupNode = (Element) conditionGroups.item(i);
      ConditionGroup cg = ConditionGroup.parseXml(conditionGroupNode);
      if (cg != null) {
        c.conditionGroups.add(cg);
      }
    }

    //tests
    c.tests = new ArrayList<TestDescription>();
    NodeList tests = null;
    for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element && child.getNodeName().equals(TESTS)) {
        tests = ((Element) child).getElementsByTagName(TEST);
        break;
      }
    }

    if (tests != null) {
      for (int i = 0; i < tests.getLength(); i++) {
        Element e = (Element) tests.item(i);
        c.tests.add(TestDescription.parseXml(e));
      }
    }

    //tests groups
    c.backgroundTestGroups = new ArrayList<TestGroup>();
    NodeList tests_groups = node.getElementsByTagName(SCHEDULED_TESTS);
    if (tests_groups.getLength() == 1) {
      tests_groups = ((Element) tests_groups.item(0)).getElementsByTagName(BATCH);
      for (int i = 0; i < tests_groups.getLength(); i++) {
        TestGroup curr = TestGroup.parseXml((Element) tests_groups.item(i));
        curr.setUsage(c.tests);
        c.maximumTestUsage = Math.max(c.maximumTestUsage, curr.netUsage);
        c.backgroundTestGroups.add(curr);
      }
    }

    //continuous tests
    NodeList continuous_tests = node.getElementsByTagName(CONTINUOUS_TESTS);
    if (continuous_tests != null && continuous_tests.getLength() == 1) {
      NodeList c_tests = ((Element) continuous_tests.item(0)).getElementsByTagName(TEST);
      for (int i = 0; i < c_tests.getLength(); i++) {
        int testIdAsInt = Integer.parseInt(((Element) c_tests.item(i)).getAttribute(ID));
        SCHEDULE_TEST_ID testId = SCHEDULE_TEST_ID.sGetTestIdForInt(testIdAsInt);

        boolean bMatched = false;
        for (TestDescription td : c.tests) {
          if (td.testId == testId) {
            bMatched = true;
            c.continuous_tests.add(td);
          }
        }
        SKLogger.sAssert(ScheduleConfig.class, bMatched);
      }
    }

    //tests run manually
    NodeList manual_tests = node.getElementsByTagName(MANUAL_TESTS);
    if (manual_tests.getLength() == 1) {
      //Get condition group for manual test
      NodeList condition_manual_test = ((Element) manual_tests.item(0)).getElementsByTagName(CONDITION_GROUP_ID);
      if (condition_manual_test.getLength() == 1) {
        c.manual_test_condition_group_id = ((Element) manual_tests.item(0)).getAttribute(CONDITION_GROUP_ID);
      }
      //Get the test ids for the manual test

      manual_tests = ((Element) manual_tests.item(0)).getElementsByTagName(TEST);
      for (int i = 0; i < manual_tests.getLength(); i++) {
        int testIdAsInt = Integer.parseInt(((Element) manual_tests.item(i)).getAttribute(ID));
        SCHEDULE_TEST_ID testId = SCHEDULE_TEST_ID.sGetTestIdForInt(testIdAsInt);

        boolean bMatched = false;
        for (TestDescription td : c.tests) {
          if (td.testId == testId) {
            bMatched = true;
            c.manual_tests.add(td);
          }
        }
        SKLogger.sAssert(ScheduleConfig.class, bMatched);
      }
    }

    //data-collectors
    c.dataCollectors = new ArrayList<BaseDataCollector>();
    NodeList dataCollectors = node.getElementsByTagName(DATA_COLLECTOR);
    for (int i = 0; i < dataCollectors.getLength(); i++) {
      Element e = (Element) dataCollectors.item(i);
      c.dataCollectors.add(BaseDataCollector.parseXml(e));
    }

    NodeList list = null;

    //init tests
//		NodeList list = ((Element)node.getElementsByTagName(GLOBAL).item(0)).getElementsByTagName(INIT);
//		if (list.getLength() == 1) {
//			NodeList initTests = ((Element)list.item(0)).getElementsByTagName(TEST);
//			for (int i = 0; i < initTests.getLength(); i++) {
//				Element e = (Element) initTests.item(i);
//				c.initTestTypes.add(e.getAttribute(TYPE));
//			}
//		} else {
//			throw new RuntimeException("more than one init section or none");
//		}

    //hosts
    list = ((Element) node.getElementsByTagName(GLOBAL).item(0)).getElementsByTagName(HOSTS);
    if (list.getLength() == 1) {
      NodeList initTests = ((Element) list.item(0)).getElementsByTagName(HOST);
      for (int i = 0; i < initTests.getLength(); i++) {
        Element e = (Element) initTests.item(i);
        c.hosts.put(e.getAttribute(DNSNAME), OtherUtils.stringEncoding(e.getAttribute(DISPLAYNAME)));
      }
    } else {
      throw new RuntimeException("more than one hosts section or none");
    }

    //background test
    list = ((Element) node.getElementsByTagName(GLOBAL).item(0)).getElementsByTagName(BACKGROUND_TEST);
    if (list != null && list.getLength() == 1) {
      Element e = (Element) list.item(0);
      String value = e.getAttribute(VALUE);
      if (value != null && value.equalsIgnoreCase(DISABLED)) {
        c.backgroundTest = false;
      }
    }
    //Communications
    list = ((Element) node.getElementsByTagName(GLOBAL).item(0)).getElementsByTagName(COMMUNICATIONS);
    if (list.getLength() == 1) {
      NodeList communicationList = ((Element) list.item(0)).getElementsByTagName(COMMUNICATION);
      for (int i = 0; i < communicationList.getLength(); i++) {
        Communication comm = Communication.parseXml((Element) communicationList.item(i));
        c.communications.put(comm.id, comm);
      }
    }

    return c;
  }

  public int getNumberOfBackgroundTestGroups() {
    int ret = 0;
    for (TestGroup tg : backgroundTestGroups) {
      ret += tg.times.size();
    }
    return ret;
  }

  public boolean toUpdate(ScheduleConfig config) {
    if (version.equals("")) {
      return true;
    }
    if (config.version.equals("")) {
      return true;
    }
    return !version.equals(config.version);
  }

  public String getConfigVersion() {
    return version;
  }

  public void forManualOrContinuousTestEnsureClosestTargetIsRunAtStart(List<TestDescription> theTests) {
    if (!(continuous_tests == theTests || theTests == manual_tests)) {
      // Must be run EITHER for continuous or manual test.
      SKLogger.sAssert(getClass(), false);
      return;
    }

    if (theTests.get(0).type.equals(SKConstants.TEST_TYPE_CLOSEST_TARGET)) {
      // All is OK!
    } else {
      // Need to add a closest target test to the start!
      int i;
      for (i = 0; i < tests.size(); i++) {
        TestDescription theTest = tests.get(i);
        if (theTest.type.equals(SKConstants.TEST_TYPE_CLOSEST_TARGET)) {
          theTests.add(0, theTest);
          SKLogger.sAssert(ManualTestRunner.class, theTests.get(0).type.equals(SKConstants.TEST_TYPE_CLOSEST_TARGET));
          break;
        }
      }

    }

  }
}
