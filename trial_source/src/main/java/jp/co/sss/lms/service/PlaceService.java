package jp.co.sss.lms.service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.co.sss.lms.dto.UserAttendanceDto;
import jp.co.sss.lms.entity.MPlace;
import jp.co.sss.lms.enums.AttendanceStatusEnum;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.form.DailyAttendanceForm;
import jp.co.sss.lms.mapper.MPlaceMapper;
import jp.co.sss.lms.util.AttendanceUtil;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.DateUtil;
import jp.co.sss.lms.util.MessageUtil;
import jp.co.sss.lms.util.PlaceUtil;
import jp.co.sss.lms.util.TrainingTime;

/**
 * 会場情報サービス
 * 
 * @author 馬場成樹  – Task.58
 */
@Service
public class PlaceService {

	@Autowired
	private MPlaceMapper mPlaceMapper;
	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	private DateUtil dateUtil;
	@Autowired
	AttendanceUtil attendanceUtil;
	
	public String getPlaceName(Integer placeId) {

		MPlace mPlace = new MPlace();
		mPlace = mPlaceMapper.findByPlaceId(placeId, Constants.DB_HIDDEN_FLG_FALSE, Constants.DB_FLG_FALSE);
		String placeName = mPlace.getPlaceName() + "(" + PlaceUtil.getPlaceNoteClassRoomName(mPlace.getPlaceNote()) + ")";

		if (placeName.contains("$$")) {
			return null;
		}
		
		return placeName;
	}
	
	public Map<String, String> searchParamCheck(AttendanceForm attendanceForm) {
		Map<String, String> errors = new HashMap<>();
		boolean errFlg = false;

		// 期間(FROM)の未入力チェック
		if (attendanceForm.getSearchPeriodFrom() == null || attendanceForm.getSearchPeriodFrom().isEmpty()) {
			System.out.println("期間(FROM)の未入力チェック in");
			errors.put("searchFromEmptyError", messageUtil.getMessage("required", new String[] {"期間（from）"}));
			errFlg = true;
		}

		// 期間(To)の未入力チェック
		if (attendanceForm.getSearchPeriodTo() == null || attendanceForm.getSearchPeriodTo().isEmpty()) {
			System.out.println("期間(To)の未入力チェック in");
			errors.put("searchToEmptyError", messageUtil.getMessage("required", new String[] {"期間（to）"}));
			errFlg = true;
		}

		if (errFlg) {
			return errors;
		}

		// 現在日を取得
		Date today = new Date();

		// String型をDate型に変換
		Date searchPeriodFrom = dateUtil.stringToSqlDate(attendanceForm.getSearchPeriodFrom());
		Date searchPeriodTo = dateUtil.stringToSqlDate(attendanceForm.getSearchPeriodTo());

		// 期間(To)が現在日付より未来日の場合
		if (searchPeriodTo.after(today)) {
			errors.put("searchToRangeError", messageUtil.getMessage("searchToRangeError",new String[] {attendanceForm.getSearchPeriodTo()}));
		}

		// 期間(To)が期間(From)より過去日の場合
		if (searchPeriodTo.before(searchPeriodFrom)) {
			errors.put("searchPeriodCompareError", messageUtil.getMessage("searchPeriodCompareError",
					new String[] { attendanceForm.getSearchPeriodTo(), attendanceForm.getSearchPeriodFrom() }));
		}

		// 期間(From) ～ 期間(To)の日数 が 30日より大きい場合
		int days = dateUtil.differenceDays(searchPeriodTo, searchPeriodFrom);
		if (days > 30) {
			String searchPeriod = messageUtil.getMessage("searchPeriod");
			errors.put("searchSettingOver", messageUtil.getMessage("searchSettingOver", new String[] {searchPeriod, "30日"}));
		}

		return errors;
	}
	
	public List<UserAttendanceDto> getUserAttendanceDto(AttendanceForm attendanceForm) {
		// String型をDate型に変換
		Date searchPeriodFrom = dateUtil.stringToSqlDate(attendanceForm.getSearchPeriodFrom());
		Date searchPeriodTo = dateUtil.stringToSqlDate(attendanceForm.getSearchPeriodTo());

		return mPlaceMapper.getUserAttendanceDto(attendanceForm.getPlaceId(),searchPeriodFrom, searchPeriodTo, Constants.DB_FLG_FALSE);
	}


	public Map<String, List<DailyAttendanceForm>> setDailyAttendanceForm(List<UserAttendanceDto> userAttendanceDtoList) {
		Map<String, List<DailyAttendanceForm>> dailyAttendanceFormMap = new LinkedHashMap<>();
		
		for (var userAttendanceDto : userAttendanceDtoList) {
			DailyAttendanceForm dailyAttendanceForm = new DailyAttendanceForm();

			dailyAttendanceForm.setStudentAttendanceId(userAttendanceDto.getPlaceId());
			dailyAttendanceForm.setLmsUserId(userAttendanceDto.getLmsUserId().toString());
			dailyAttendanceForm.setUserName(userAttendanceDto.getUserName());
			dailyAttendanceForm.setCourseName(userAttendanceDto.getCourseName());
			
			// 日付設定（画面用）
			dailyAttendanceForm.setDispTrainingDate(
					dateUtil.dateToStringJ(userAttendanceDto.getTrainingDate(), "yyyy年M月d日(E)"));
			dailyAttendanceForm.setTrainingDate(
					dateUtil.dateToString(userAttendanceDto.getTrainingDate(), "yyyy-MM-dd"));

			// 出勤時間設定
			if (userAttendanceDto.getTrainingStartTime() != null && !userAttendanceDto.getTrainingStartTime().isEmpty()) {
				// 画面用
				String trainingStartTime = userAttendanceDto.getTrainingStartTime();
				dailyAttendanceForm.setTrainingStartTime(trainingStartTime);
				
				// コピー用(15分単位で切り上げた時刻)
				TrainingTime trainingTime = new TrainingTime(trainingStartTime);
				dailyAttendanceForm.setTrainingStartTimeCopy(trainingTime.roundUp().getFormattedString());

			} else {
				// 画面用([未入力])を設定
				dailyAttendanceForm.setTrainingStartTime(Constants.NOT_ENTERED);
			}

			// 退勤時間設定
			if (userAttendanceDto.getTrainingEndTime() != null && !userAttendanceDto.getTrainingEndTime().isEmpty()) {
				// 画面用
				String trainingEndTime = userAttendanceDto.getTrainingEndTime();
				dailyAttendanceForm.setTrainingEndTime(trainingEndTime);

				// コピー用(15分単位で切り下げた時刻)
				TrainingTime trainingTime = new TrainingTime(trainingEndTime);
				dailyAttendanceForm.setTrainingEndTimeCopy(trainingTime.roundDown().getFormattedString());

			} else {
				// 画面用([未入力])を設定
				dailyAttendanceForm.setTrainingEndTime(Constants.NOT_ENTERED);
			}

			// 中抜け時間設定（hh:mm形式）
			if (userAttendanceDto.getBlankTime() != null) {
				dailyAttendanceForm.setBlankTimeValue(
						attendanceUtil.calcBlankTime(userAttendanceDto.getBlankTime()).getFormattedString());
			}
			
			// 勤怠状態設定
			if (userAttendanceDto.getStatus() != null) {
				dailyAttendanceForm.setStatus(AttendanceStatusEnum.getEnum(userAttendanceDto.getStatus()).code.toString());
				dailyAttendanceForm.setStatusDispName(AttendanceStatusEnum.getEnum(userAttendanceDto.getStatus()).name);
			}
			
			// 備考設定
			if (userAttendanceDto.getNote() != null) {
				dailyAttendanceForm.setNote(userAttendanceDto.getNote());
			}
			
			// 企業入力勤怠情報ID設定
			dailyAttendanceForm.setCompanyAttendanceId(userAttendanceDto.getCompanyAttendanceId());

			dailyAttendanceFormMap.computeIfAbsent(dailyAttendanceForm.getDispTrainingDate(), k -> new ArrayList<>()).add(dailyAttendanceForm);

		}
		return dailyAttendanceFormMap;
	}
	
	public Map<String, String> completeParamCheck(List<DailyAttendanceForm> dailyAttendanceFormList) {
		Map<String, String> errors = new HashMap<>();
		boolean errFlg = false;

		for (var var : dailyAttendanceFormList) {
			// 期間(FROM)の未入力チェック
			if (var.getStatus().equals("1")) {
				if (var.getTrainingStartTime().isEmpty() || var.getTrainingEndTime().isEmpty()) {
	
					errors.put("absentAndTrainingTimeExistsBulk",
							messageUtil.getMessage("absentAndTrainingTimeExistsBulk",
									new String[] {var.getTrainingDate()}));
					errFlg = true;
				}
			} else {
				if (var.getTrainingStartTime().isEmpty() || var.getTrainingEndTime().isEmpty()) {

					errors.put("requiredTrainingTimeBulk",
							messageUtil.getMessage("requiredTrainingTimeBulk",
									new String[] {var.getTrainingDate()}));
					errFlg = true;
				}
			}

			if (errFlg) {
				continue;
			}

			if ((!isValidHHmm(var.getTrainingStartTime()) && !isValidHHmm(var.getTrainingEndTime()))
				|| dateUtil.stringToDate(var.getTrainingDate(), Constants.DEFAULT_DATE_FORMAT) == null) {

				errors.put("trainingTimeBulk",
						messageUtil.getMessage("trainingTimeBulk",
								new String[] {var.getTrainingDate()}));
			}

			TrainingTime trainingStartTime = new TrainingTime(var.getTrainingStartTime());
			if (trainingStartTime.getHour() >= 24 && trainingStartTime.getMinute() >= 1) {

				errors.put("maxvalBulk",
						messageUtil.getMessage("maxvalBulk",
								new String[] {var.getTrainingDate(), "trainingStartTimeBulk", "24：00"}));
			}

			TrainingTime trainingEndTime = new TrainingTime(var.getTrainingEndTime());
			if (trainingEndTime.getHour() >= 24 && trainingEndTime.getMinute() >= 1) {

				errors.put("maxvalBulk",
						messageUtil.getMessage("maxvalBulk",
								new String[] {var.getTrainingDate(), "trainingEndTimeBulk", "24：00"}));
			}

			if (trainingStartTime.getHour() >= trainingEndTime.getHour()
					&& trainingStartTime.getMinute() > trainingEndTime.getMinute()) {

				errors.put("attendance.trainingTimeRangeBulk",
						messageUtil.getMessage("attendance.trainingTimeRangeBulk",
								new String[] {var.getTrainingDate()}));
			}
		}

		return errors;
	}

    public static boolean isValidHHmm(String timeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        try {
            LocalTime.parse(timeStr, formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}

