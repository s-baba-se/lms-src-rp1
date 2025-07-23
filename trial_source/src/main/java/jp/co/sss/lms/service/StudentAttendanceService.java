package jp.co.sss.lms.service;

import java.text.ParseException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.dto.UserAttendanceDto;
import jp.co.sss.lms.entity.TCompanyAttendance;
import jp.co.sss.lms.entity.TStudentAttendance;
import jp.co.sss.lms.enums.AttendanceStatusEnum;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.form.DailyAttendanceForm;
import jp.co.sss.lms.mapper.TCompanyAttendanceMapper;
import jp.co.sss.lms.mapper.TStudentAttendanceMapper;
import jp.co.sss.lms.util.AttendanceUtil;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.DateUtil;
import jp.co.sss.lms.util.LoginUserUtil;
import jp.co.sss.lms.util.MessageUtil;
import jp.co.sss.lms.util.TrainingTime;

/**
 * 勤怠情報（受講生入力）サービス
 * 
 * @author 東京ITスクール
 */
@Service
public class StudentAttendanceService {

	@Autowired
	private DateUtil dateUtil;
	@Autowired
	private AttendanceUtil attendanceUtil;
	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	private LoginUserUtil loginUserUtil;
	@Autowired
	private LoginUserDto loginUserDto;
	@Autowired
	private TStudentAttendanceMapper tStudentAttendanceMapper;
	@Autowired
	private TCompanyAttendanceMapper tCompanyAttendanceMapper;

	/**
	 * 勤怠一覧情報取得
	 * 
	 * @param courseId
	 * @param lmsUserId
	 * @return 勤怠管理画面用DTOリスト
	 */
	public List<AttendanceManagementDto> getAttendanceManagement(Integer courseId,
			Integer lmsUserId) {

		// 勤怠管理リストの取得
		List<AttendanceManagementDto> attendanceManagementDtoList = tStudentAttendanceMapper
				.getAttendanceManagement(courseId, lmsUserId, Constants.DB_FLG_FALSE);
		for (AttendanceManagementDto dto : attendanceManagementDtoList) {
			// 中抜け時間を設定
			if (dto.getBlankTime() != null) {
				TrainingTime blankTime = attendanceUtil.calcBlankTime(dto.getBlankTime());
				dto.setBlankTimeValue(String.valueOf(blankTime));
			}
			// 遅刻早退区分判定
			AttendanceStatusEnum statusEnum = AttendanceStatusEnum.getEnum(dto.getStatus());
			if (statusEnum != null) {
				dto.setStatusDispName(statusEnum.name);
			}
		}

		return attendanceManagementDtoList;
	}

	/**
	 * 出退勤更新前のチェック
	 * 
	 * @param attendanceType
	 * @return エラーメッセージ
	 */
	public String punchCheck(Short attendanceType) {
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 権限チェック
		if (!loginUserUtil.isStudent()) {
			return messageUtil.getMessage(Constants.VALID_KEY_AUTHORIZATION);
		}
		// 研修日チェック
		if (!attendanceUtil.isWorkDay(loginUserDto.getCourseId(), trainingDate)) {
			return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_NOTWORKDAY);
		}
		// 登録情報チェック
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		switch (attendanceType) {
		case Constants.CODE_VAL_ATWORK:
			if (tStudentAttendance != null
					&& !tStudentAttendance.getTrainingStartTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			break;
		case Constants.CODE_VAL_LEAVING:
			if (tStudentAttendance == null
					|| tStudentAttendance.getTrainingStartTime().equals("")) {
				// 出勤情報がないため退勤情報を入力出来ません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY);
			}
			if (!tStudentAttendance.getTrainingEndTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			TrainingTime trainingStartTime = new TrainingTime(
					tStudentAttendance.getTrainingStartTime());
			TrainingTime trainingEndTime = new TrainingTime();
			if (trainingStartTime.compareTo(trainingEndTime) > 0) {
				// 退勤時刻は出勤時刻より後でなければいけません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE);
			}
			break;
		}
		return null;
	}

	/**
	 * 出勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchIn() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 現在の研修時刻
		TrainingTime trainingStartTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				null);
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		if (tStudentAttendance == null) {
			// 登録処理
			tStudentAttendance = new TStudentAttendance();
			tStudentAttendance.setLmsUserId(loginUserDto.getLmsUserId());
			tStudentAttendance.setTrainingDate(trainingDate);
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setTrainingEndTime("");
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setNote("");
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setFirstCreateDate(date);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendance.setBlankTime(null);
			tStudentAttendanceMapper.insert(tStudentAttendance);
		} else {
			// 更新処理
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendanceMapper.update(tStudentAttendance);
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 退勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchOut() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		// 出退勤時刻
		TrainingTime trainingStartTime = new TrainingTime(
				tStudentAttendance.getTrainingStartTime());
		TrainingTime trainingEndTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				trainingEndTime);
		// 更新処理
		tStudentAttendance.setTrainingEndTime(trainingEndTime.toString());
		tStudentAttendance.setStatus(attendanceStatusEnum.code);
		tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
		tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
		tStudentAttendance.setLastModifiedDate(date);
		tStudentAttendanceMapper.update(tStudentAttendance);
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 勤怠フォームへ設定
	 * 
	 * @param attendanceManagementDtoList
	 * @return 勤怠編集フォーム
	 */
	public AttendanceForm setAttendanceForm(
			List<AttendanceManagementDto> attendanceManagementDtoList) {

		AttendanceForm attendanceForm = new AttendanceForm();
		attendanceForm.setAttendanceList(new ArrayList<DailyAttendanceForm>());
		attendanceForm.setLmsUserId(loginUserDto.getLmsUserId());
		attendanceForm.setUserName(loginUserDto.getUserName());
		attendanceForm.setLeaveFlg(loginUserDto.getLeaveFlg());
		attendanceForm.setBlankTimes(attendanceUtil.setBlankTime());

		// 途中退校している場合のみ設定
		if (loginUserDto.getLeaveDate() != null) {
			attendanceForm
					.setLeaveDate(dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy-MM-dd"));
			attendanceForm.setDispLeaveDate(
					dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy年M月d日"));
		}

		// 勤怠管理リストの件数分、日次の勤怠フォームに移し替え
		for (AttendanceManagementDto attendanceManagementDto : attendanceManagementDtoList) {
			DailyAttendanceForm dailyAttendanceForm = new DailyAttendanceForm();
			dailyAttendanceForm
					.setStudentAttendanceId(attendanceManagementDto.getStudentAttendanceId());
			dailyAttendanceForm
					.setTrainingDate(dateUtil.toString(attendanceManagementDto.getTrainingDate()));
			dailyAttendanceForm
					.setTrainingStartTime(attendanceManagementDto.getTrainingStartTime());
			dailyAttendanceForm.setTrainingEndTime(attendanceManagementDto.getTrainingEndTime());
			if (attendanceManagementDto.getBlankTime() != null) {
				dailyAttendanceForm.setBlankTime(attendanceManagementDto.getBlankTime());
				dailyAttendanceForm.setBlankTimeValue(String.valueOf(
						attendanceUtil.calcBlankTime(attendanceManagementDto.getBlankTime())));
			}
			dailyAttendanceForm.setStatus(String.valueOf(attendanceManagementDto.getStatus()));
			dailyAttendanceForm.setNote(attendanceManagementDto.getNote());
			dailyAttendanceForm.setSectionName(attendanceManagementDto.getSectionName());
			dailyAttendanceForm.setIsToday(attendanceManagementDto.getIsToday());
			dailyAttendanceForm.setDispTrainingDate(dateUtil
					.dateToString(attendanceManagementDto.getTrainingDate(), "yyyy年M月d日(E)"));
			dailyAttendanceForm.setStatusDispName(attendanceManagementDto.getStatusDispName());

			attendanceForm.getAttendanceList().add(dailyAttendanceForm);
		}

		return attendanceForm;
	}

	/**
	 * 勤怠登録・更新処理
	 * 
	 * @param attendanceForm
	 * @return 完了メッセージ
	 * @throws ParseException
	 */
	public String update(AttendanceForm attendanceForm) throws ParseException {

		Integer lmsUserId = loginUserUtil.isStudent() ? loginUserDto.getLmsUserId()
				: attendanceForm.getLmsUserId();

		// 現在の勤怠情報（受講生入力）リストを取得
		List<TStudentAttendance> tStudentAttendanceList = tStudentAttendanceMapper
				.findByLmsUserId(lmsUserId, Constants.DB_FLG_FALSE);

		// 入力された情報を更新用のエンティティに移し替え
		Date date = new Date();
		for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {

			// 更新用エンティティ作成
			TStudentAttendance tStudentAttendance = new TStudentAttendance();
			// 日次勤怠フォームから更新用のエンティティにコピー
			BeanUtils.copyProperties(dailyAttendanceForm, tStudentAttendance);
			// 研修日付
			tStudentAttendance
					.setTrainingDate(dateUtil.parse(dailyAttendanceForm.getTrainingDate()));
			// 現在の勤怠情報リストのうち、研修日が同じものを更新用エンティティで上書き
			for (TStudentAttendance entity : tStudentAttendanceList) {
				if (entity.getTrainingDate().equals(tStudentAttendance.getTrainingDate())) {
					tStudentAttendance = entity;
					break;
				}
			}
			tStudentAttendance.setLmsUserId(lmsUserId);
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			// 出勤時刻整形
			TrainingTime trainingStartTime = null;
			trainingStartTime = new TrainingTime(dailyAttendanceForm.getTrainingStartTime());
			tStudentAttendance.setTrainingStartTime(trainingStartTime.getFormattedString());
			// 退勤時刻整形
			TrainingTime trainingEndTime = null;
			trainingEndTime = new TrainingTime(dailyAttendanceForm.getTrainingEndTime());
			tStudentAttendance.setTrainingEndTime(trainingEndTime.getFormattedString());
			// 中抜け時間
			tStudentAttendance.setBlankTime(dailyAttendanceForm.getBlankTime());
			// 遅刻早退ステータス
			if ((trainingStartTime != null || trainingEndTime != null)
					&& !dailyAttendanceForm.getStatusDispName().equals("欠席")) {
				AttendanceStatusEnum attendanceStatusEnum = attendanceUtil
						.getStatus(trainingStartTime, trainingEndTime);
				tStudentAttendance.setStatus(attendanceStatusEnum.code);
			}
			// 備考
			tStudentAttendance.setNote(dailyAttendanceForm.getNote());
			// 更新者と更新日時
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			// 削除フラグ
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			// 登録用Listへ追加
			tStudentAttendanceList.add(tStudentAttendance);
		}
		// 登録・更新処理
		for (TStudentAttendance tStudentAttendance : tStudentAttendanceList) {
			if (tStudentAttendance.getStudentAttendanceId() == null) {
				tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
				tStudentAttendance.setFirstCreateDate(date);
				tStudentAttendanceMapper.insert(tStudentAttendance);
			} else {
				tStudentAttendanceMapper.update(tStudentAttendance);
			}
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 勤怠の過去日未入力確認
	 * 
	 * @author 馬場成樹  – Task.25
	 * @param lmsUserId
	 * @return 未入力件数結果(ture OR false)
	 */
	public boolean hasMissingAttendance(Integer lmsUserId) {

		Date today = new Date();
		
		// 勤怠未入力件数取得
		int resultCount = tStudentAttendanceMapper.notEnterCount(lmsUserId, Constants.DB_FLG_FALSE,
				dateUtil.stringToSqlDate(dateUtil.toString(today, Constants.DEFAULT_DATE_FORMAT)));

		return resultCount > 0;
	}

	/**
	 * 『検索』ボタン押下時の入力チェック
	 * 
	 * @author 馬場成樹  – Task.58
	 * @param attendanceForm
	 * @return エラーメッセージ
	 */
	public Map<String, String> searchParamCheck(AttendanceForm attendanceForm) {
		Map<String, String> errors = new HashMap<>();
		boolean hasErrorMessage = false;

		// 必須項目：期間(FROM)の未入力チェック
		if (attendanceForm.getSearchPeriodFrom() == null || attendanceForm.getSearchPeriodFrom().isEmpty()) {
			errors.put("searchFromEmptyError", messageUtil.getMessage("required", new String[] {"期間（from）"}));
			hasErrorMessage = true;
		}

		// 必須項目：期間(To)の未入力チェック
		if (attendanceForm.getSearchPeriodTo() == null || attendanceForm.getSearchPeriodTo().isEmpty()) {
			errors.put("searchToEmptyError", messageUtil.getMessage("required", new String[] {"期間（to）"}));
			hasErrorMessage = true;
		}

		// エラーメッセージが設定されている場合
		if (hasErrorMessage) {
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

	/**
	 * 日別勤怠情報フォームリストの設定
	 * 
	 * @author 馬場成樹  – Task.58
	 * @param userAttendanceDtoList
	 * @return 日別勤怠情報フォームリスト
	 */
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

			// キーが存在しない場合は新しいリストを生成してMapに登録
			dailyAttendanceFormMap.computeIfAbsent(dailyAttendanceForm.getDispTrainingDate(), k -> new ArrayList<>()).add(dailyAttendanceForm);

		}
		return dailyAttendanceFormMap;
	}

	/**
	 * 『確定』ボタン押下時の入力チェック
	 * 
	 * @author 馬場成樹  – Task.58
	 * @param dailyAttendanceFormList
	 * @return エラーメッセージ
	 */
	public Map<String, String> completeParamCheck(List<DailyAttendanceForm> dailyAttendanceFormList) {
		Map<String, String> errors = new HashMap<>();
		boolean hasErrorMessage = false;

		for (var var : dailyAttendanceFormList) {

			// 勤怠状態が1
			if (var.getStatus() != null && var.getStatus().equals("1")) {

				// 勤怠の開始・終了時間どちらかに値がある場合
				if ((var.getTrainingStartTime() != null && !var.getTrainingStartTime().isEmpty())
						|| (var.getTrainingEndTime() != null && !var.getTrainingEndTime().isEmpty())) {
	
					// エラーメッセージ設定
					errors.put("absentAndTrainingTimeExistsBulk", messageUtil.getMessage("absentAndTrainingTimeExistsBulk",
									new String[] {var.getTrainingDate()}));
					hasErrorMessage = true;
				}

			// 勤怠状態が1以外
			} else {

				// 勤怠の開始・終了時間どちらかに値がない場合
				if ((var.getTrainingStartTimeCopy() == null || var.getTrainingStartTimeCopy().isEmpty())
						|| (var.getTrainingEndTimeCopy()) == null || var.getTrainingEndTimeCopy().isEmpty()) {

					System.out.println(var.getTrainingStartTimeCopy());
					System.out.println(var.getTrainingEndTimeCopy());
					// エラーメッセージ設定
					errors.put("requiredTrainingTimeBulk", messageUtil.getMessage("requiredTrainingTimeBulk",
									new String[] {var.getTrainingDate()}));
					hasErrorMessage = true;
				}
			}

			// エラーメッセージが設定されている場合
			if (hasErrorMessage) {
				continue;
			}

			// 開始時間・終了時間の値がhh:mm形式以外の場合かつ日付として妥当出ない場合
			if ((!isValidHHmm(var.getTrainingStartTimeCopy()) && !isValidHHmm(var.getTrainingEndTimeCopy()))
				|| dateUtil.stringToDate(var.getTrainingDate(), Constants.DEFAULT_DATE_FORMAT) == null) {

				// エラーメッセージ設定
				errors.put("trainingTimeBulk", messageUtil.getMessage("trainingTimeBulk",
								new String[] {var.getTrainingDate()}));
			}

			// 開始時間の値が24:00を超える場合（24:01～）
			TrainingTime trainingStartTimeCopy = new TrainingTime(var.getTrainingStartTimeCopy());
			if (trainingStartTimeCopy.getHour() >= 24 && trainingStartTimeCopy.getMinute() >= 1) {

				// エラーメッセージ設定
				String trainingStartTimeBulk = messageUtil.getMessage("trainingStartTimeBulk");
				errors.put("maxvalBulk", messageUtil.getMessage("maxvalBulk",
								new String[] {var.getTrainingDate(), trainingStartTimeBulk, "24：00"}));
			}

			// 終了時間の値が24:00を超える場合（24:01～）
			TrainingTime trainingEndTimeCopy = new TrainingTime(var.getTrainingEndTimeCopy());
			if (trainingEndTimeCopy.getHour() >= 24 && trainingEndTimeCopy.getMinute() >= 1) {

				// エラーメッセージ設定
				String trainingEndTimeBulk = messageUtil.getMessage("trainingEndTimeBulk");
				errors.put("maxvalBulk", messageUtil.getMessage("maxvalBulk",
								new String[] {var.getTrainingDate(), trainingEndTimeBulk, "24：00"}));
			}

			// 開始時間が終了時間を超えている場合
			if (trainingStartTimeCopy.getHour() >= trainingEndTimeCopy.getHour()
					&& trainingStartTimeCopy.getMinute() > trainingEndTimeCopy.getMinute()) {

				// エラーメッセージ設定
				errors.put("attendance.trainingTimeRangeBulk",
						messageUtil.getMessage("attendance.trainingTimeRangeBulk",
								new String[] {var.getTrainingDate()}));
			}
		}

		return errors;
	}

	/**
	 * hh:mm形式確認
	 * 
	 * @author 馬場成樹  – Task.58
	 * @param timeStr
	 * @return 判定結果(ture OR false)
	 */
    public static boolean isValidHHmm(String timeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        try {
            LocalTime.parse(timeStr, formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public List<DailyAttendanceForm> setParamMap(Map<String, String> paramMap){
		List<DailyAttendanceForm> dailyAttendanceFormList = new ArrayList<>();

		for (Map.Entry<String, String> var : paramMap.entrySet()) {
			String key = var.getKey();
			String value = var.getValue();

			if (key != null && (key.startsWith("dailyAttendanceList[") && key.contains("]."))) {

				// インデックスを取得
				int idxStart = key.indexOf('[') + 1;
				int idxEnd = key.indexOf(']');
				int index = Integer.parseInt(key.substring(idxStart, idxEnd));

				// keyの文字列からフィールド名を取得
				String field = key.substring(key.indexOf("].") + 2);

				// HTMLから受け取ったリスト数追加する
				while (dailyAttendanceFormList.size() <= index) {
					dailyAttendanceFormList.add(new DailyAttendanceForm());
		        }
				
				DailyAttendanceForm daf = dailyAttendanceFormList.get(index);

				// フィールドごとに値を設定
				switch (field) {
					case "trainingStartTime" -> daf.setTrainingStartTime(value.equals("[未入力]") ? "" : value);
					case "trainingEndTime" -> daf.setTrainingEndTime(value.equals("[未入力]") ? "" : value);
					case "trainingStartTimeCopy" -> daf.setTrainingStartTimeCopy(value);
					case "trainingEndTimeCopy" -> daf.setTrainingEndTimeCopy(value);
					case "status" -> daf.setStatus(value);
					case "companyAttendanceId" -> daf.setCompanyAttendanceId((value != null && !value.isBlank()) ? Integer.parseInt(value) : null);
					case "studentAttendanceId" -> daf.setStudentAttendanceId((value != null && !value.isBlank()) ? Integer.parseInt(value) : null);
					case "lmsUserId" -> daf.setLmsUserId(value);
					case "trainingDate" -> daf.setTrainingDate(value);
				}
			}
		}
		for (var var : dailyAttendanceFormList) {
			System.out.println("【開始：" + var.getTrainingStartTimeCopy() + "】【終了：" + var.getTrainingEndTimeCopy() + "】");
		}
		return dailyAttendanceFormList;
    }

    /**
     * 勤怠情報（企業入力）リストの設定
     * 
	 * @author 馬場成樹  – Task.58
     * @param tCompanyAttendanceList
     * @return 勤怠情報（企業入力）リスト
     */
    public List<TCompanyAttendance> setCompanyAttendanceList(List<DailyAttendanceForm> dailyAttendanceFormList) {
    	List<TCompanyAttendance> tCompanyAttendanceList = new ArrayList<>();
    	
    	for (var var : dailyAttendanceFormList) {
    		
			TCompanyAttendance tCompanyAttendance = new TCompanyAttendance();
			Date today = new Date();

			if (var.getCompanyAttendanceId() != null) {
    			tCompanyAttendance = tCompanyAttendanceMapper.findByCompanyAttendanceId(var.getCompanyAttendanceId(), Constants.DB_FLG_FALSE);

    			tCompanyAttendance.setTrainingStartTime(var.getTrainingStartTimeCopy());
    			tCompanyAttendance.setTrainingEndTime(var.getTrainingEndTimeCopy());

    			if (var.getStatus() != null && !var.getStatus().equals("1")) {
    				AttendanceUtil attendanceUtil = new AttendanceUtil();
    				TrainingTime trainingStartTime = new TrainingTime();
    				TrainingTime trainingEndTime = new TrainingTime();

    				trainingStartTime.isValidTrainingTime(var.getTrainingStartTimeCopy());
    				trainingEndTime.isValidTrainingTime(var.getTrainingEndTimeCopy());

    				tCompanyAttendance.setStatus(attendanceUtil.getStatus(trainingStartTime, trainingEndTime).code);
    			}
    			tCompanyAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
    			tCompanyAttendance.setLastModifiedDate(today);
    		} else {
    			tCompanyAttendance.setLmsUserId(Integer.parseInt(var.getLmsUserId()));
    			tCompanyAttendance.setTrainingDate(dateUtil.stringToSqlDate(var.getTrainingDate()));
    			tCompanyAttendance.setTrainingStartTime(var.getTrainingStartTimeCopy());
    			tCompanyAttendance.setTrainingEndTime(var.getTrainingEndTimeCopy());

    			if (var.getStatus() != null && !var.getStatus().equals("1")) {
    				AttendanceUtil attendanceUtil = new AttendanceUtil();
    				TrainingTime trainingStartTime = new TrainingTime();
    				TrainingTime trainingEndTime = new TrainingTime();

    				trainingStartTime.isValidTrainingTime(var.getTrainingStartTimeCopy());
    				trainingEndTime.isValidTrainingTime(var.getTrainingEndTimeCopy());

    				tCompanyAttendance.setStatus(attendanceUtil.getStatus(trainingStartTime, trainingEndTime).code);
    			}
    			tCompanyAttendance.setAccountId(loginUserDto.getAccountId());

    			tCompanyAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
    			tCompanyAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
    			tCompanyAttendance.setFirstCreateDate(today);
    			tCompanyAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
    			tCompanyAttendance.setLastModifiedDate(today);
    			
    		}
			
			tCompanyAttendanceList.add(tCompanyAttendance);
    	}
    	
    	return tCompanyAttendanceList;
    }
    
    /**
     * 勤怠情報（企業入力）テーブルのデータ登録／更新
     * 
	 * @author 馬場成樹  – Task.58
     * @param tCompanyAttendanceList
     * @return 成功メッセージ
     */
    public String updateCompanyAttendanceDB(List<TCompanyAttendance> tCompanyAttendanceList) {
    	for (var var : tCompanyAttendanceList ) {
    		if (var.getCompanyAttendanceId() != null) {
    			// 勤怠情報（企業入力）更新
    			tCompanyAttendanceMapper.update(var);
    		} else {
    			// 勤怠情報（企業入力）登録
    			tCompanyAttendanceMapper.insert(var);
    		}
    	}
    	
    	return messageUtil.getMessage("regist.complete", new String[] {"勤怠情報"});
    }
}
