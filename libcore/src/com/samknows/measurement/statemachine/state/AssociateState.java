package com.samknows.measurement.statemachine.state;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.MainService;
import com.samknows.measurement.environment.PhoneIdentityDataCollector;
import com.samknows.measurement.net.AssociateAction;
import com.samknows.measurement.statemachine.StateResponseCode;
import com.samknows.measurement.util.OtherUtils;

public class AssociateState extends BaseState{

	public AssociateState(MainService c) {
		super(c);
	}

	@Override
	public StateResponseCode executeState() {
		if (!OtherUtils.isPhoneAssosiated(ctx)) {
			String unitId = SK2AppSettings.getInstance().getUnitId();
			String imei = new PhoneIdentityDataCollector(ctx).collect().imei;
			AssociateAction action = new AssociateAction(unitId, imei); 
			action.execute();
			return action.isSuccess() ? StateResponseCode.OK : StateResponseCode.FAIL;
		} else {
			SKLogger.d(this, "already associated...skipping");
			return StateResponseCode.OK;
		}
	}

}
