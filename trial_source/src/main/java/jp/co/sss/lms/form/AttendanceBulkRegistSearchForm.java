package jp.co.sss.lms.form;

import lombok.Data;

@Data
public class AttendanceBulkRegistSearchForm {

	/** 期間(FROM) */
	private String searchPeriodFrom;
	/** 期間(To) */
	private String searchPeriodTo;
	/** 会場ID */
	private Integer placeId;

}
