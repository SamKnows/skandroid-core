package com.samknows.libcore;

public class SKConstants {
	
	public static final int CACHE_EXPIRATION = 12 * 60 * 60 * 1000;// 12 hours in millis, HOURS * MINUTES * SECONDS * MILLIS
	public static final String RESULT_LINE_SEPARATOR = ";";
	
	public static boolean LOG_TO_FILE = true;
	
	public static final String ENABLE_MULTIPLE_TABS = "enable_multiple_tabs";

	// These must all be set by the application at start-up.
	public static int RStringQuit = 0;
	public static int RStringReallyQuit = 0;
	public static int RStringYes = 0;
	public static int RStringNoDialog = 0;
	
	public static String PREF_KEY_USED_BYTES = null; // "usedBytes";
	public static String PREF_DATA_CAP = null; // "data_cap"; "data_cap_pref"
	public static String PROP_TEST_START_WINDOW_RTC = null; //  "test_start_window_in_millis"; "test_start_window_in_millis_rtc"
	
	// If set to true, the following flag is used in some UI screens, to show
	// some debug-only UI elements.
	// The value must NOT be shipped set to true!
	public static final boolean DEBUG = false;
	
	// If set to true, the following flag forces BACKGROUND tests to use a built-in
	// test schedule file.
	// The value must NOT be shipped set to true!
	public static final boolean USE_LOCAL_CONFIG = false;
	
	public static long TEST_QUEUE_MAX_SIZE_IN_DAYS = 3; //populate execution queue with task for next *number* of days
	public static long TEST_QUEUE_NORMAL_SIZE_IN_DAYS = 1; //populate execution queue with task for next *number* of days
	public static long NO_NEXT_RUN_TIME = -1;
	
	public static long WAIT_TEST_BEFORE_ABORT = 600000; //Wait time before kill a test being executing in millis currently 10 min
	
	public static final int NOTIFICATION_ID = 1;
	
	public static final long SERVICE_RESCHEDULE_IF_ROAMING_OR_DATACAP = 24 * 60 * 60 * 1000; //1 day in millis, HOURS * MINUTES * SECONDS * MILLIS
	
	public static final long NET_ACTIVITY_CONDITION_WAIT_TIME = 5000;

	public static final int CONNECTION_TIMEOUT_20_SECONDS_IN_MILLIS = 20 * 1000;//20sec, SECONDS * MILLIS	
	public static final int CONNECTION_TIMEOUT_30_SECONDS_IN_MILLIS = 30 * 1000;//30sec
	
	public static final long SUBMITED_LOGS_MAX_SIZE = 5L * 1024L * 1024L; //in bytes
	
	public static int RC_SCHEDULE_TEST = 0;
	public static int RC_NOTIFICATION = 1;
	
	public static final String RESULT_OK = "OK";
	public static final String RESULT_FAIL = "FAIL";
	
	public static final String PARAM_PREFIX = "$";
	
	public static final String TEST_RESULTS_TO_SUBMIT_FILE_NAME = "test_results_to_submit";
	public static final String TEST_RESULTS_SUBMITTED_FILE_NAME = "test_results_submited";
	public static final String SCHEDULE_CONFIG_FILE_NAME 		= "schedule_config";
	public static final String EXECUTION_QUEUE_FILE_NAME 		= "execution_queue";
	public static final String TEST_PARAMS_MANAGER_FILE_NAME 	= "test_params";
	public static final String KEYS_FILE_NAME 					= "keys";
	public static final String NETUSAGE_STORAGE 				= "netusage_storage";
	
	
	//Entries in the properties file
	public static final String PROP_ANONYMOUS 								= "anonymous";
	public static final String PROP_BRAND 									= "brand";

	public static final String PROP_RUN_TEST_POPUP 							= "run_test_pop_up";
	public static final String PROP_DCS_URL 								= "DCS_Init_url";
	public static final String PROP_REPORTING_PATH 							= "Reporting_path";
	public static final String PROP_RESCHEDULE_TIME 						= "fail_request_reschedule_time_in_millis";
	public static final String PROP_TEST_START_WINDOW_RTC_WAKEUP 			= "test_start_window_in_millis_rtc_wakeup";
	public static final String PROP_KILLED_SERVICE_RESTART_TIME_IN_MILLIS	= "killed_service_restart_time_in_millis";
	public static final String PROP_PROTOCOL_SCHEME						 	= "protocol_scheme";
	public static final String PROP_SUBMIT_PATH 							= "submit_path";
	public static final String PROP_DOWNLOAD_CONFIG_PATH 					= "download_config_path";
	public static final String INTENT_EXTRA_TD 								= "test_description";
	public static final String INTENT_EXTRA_USERNAME 						= "username";
	public static final String INTENT_EXTRA_DEVICE 							= "device";
	public static final String INTENT_EXTRA_IS_CURRENT_DEVICE 				= "isCurrentDevice";
	public static final String PROP_ENTERPRISE_ID 							= "enterprise_id";
	public static final String PROP_USER_SELF_IDENTIFIER					= "user_self_identifier";
	public static final String PROP_DATA_CAP_WELCOME						= "data_cap_welcome";
	public static final String PROP_COLLECT_TRAFFIC_DATA					= "collect_traffic_data";
	public static final String PROP_RUN_IN_ROAMING							= "run_in_roaming";
	//Preferences
	public static final String PREF_FILE_NAME = "SK_PREFS";
	public static final String PREF_KEY_STATE = "InitState";
	public static final String PREF_NEXT_RUN_TIME = "next_test_run";
	public static final String PREF_KEY_SERVER_BASE_URL = "ServerBaseUrl";
	public static final String PREF_KEY_UNIT_ID = "UnitId";
	public static final String PREF_KEY_USED_BYTES_MONTH = "usedBytes_month";	
	public static final String PREF_KEY_USED_BYTES_LAST_TIME = "usedBytesLastTime";
	public static final String PREF_KEY_CONFIG_VERSION = "config_version";
	public static final String PREF_KEY_CONFIG_PATH = "config_path";
	public static final String PREF_WAS_INTRO_SHOWN = "was_intro_shown";
	public static final String PREF_KEY_USERNAME = "username";
	public static final String PREF_KEY_PASSWORD = "password";
	public static final String PREF_KEY_SERVICE_ACTIVATED = "service_activated";
	public static final String PREF_KEY_DEVICES = "devices";
	
	public static final String PREF_SERVICE_ENABLED = "enable_testing";
	public static final String PREF_KEY_DATA_CAP_DAY_IN_MONTH_RESET = "data_cap_reset_day";
	public static final String PREF_DATA_CAP_ENABLED = "data_cap_enabled";
	public static final String PREF_DATA_CAP_USER_DEFINED = "data_cap_user_defined";
	public static final String PREF_ENABLE_WAKEUP = "enable_wakeup";
	public static final String PREF_LOCATION_TYPE = "location_type";
	public static final String PREF_USER_SELF_ID = "user_self_id";
	public static String PREF_DATA_CAP_RESET_DAY = "data_cap_reset_day";
	
	public static String STATE_MACHINE_STATUS = "state_machine_status";
	
	public static final String TEST_TYPE_CLOSEST_TARGET = "closestTarget";
	public static final String TEST_TYPE_DOWNLOAD = "downstreamthroughput";
	public static final String TEST_TYPE_UPLOAD = "upstreamthroughput";
	public static final String TEST_TYPE_LATENCY = "latency";
	
	public static final String TEST_PARAM_CLOSEST = "closest";
	//dialogs
	public static final String RUN_TEST_DIALOG_ID = "1";
	
	//data cap limits
	public static final int DATA_CAP_DEFAULT_VALUE=100;
	public static final int DATA_CAP_MAX_VALUE=5000;
	public static final int DATA_CAP_MIN_VALUE=1;
	
}
