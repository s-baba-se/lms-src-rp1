package jp.co.sss.lms.service;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.co.sss.lms.dto.UserAttendanceDto;
import jp.co.sss.lms.entity.MPlace;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.mapper.MPlaceMapper;
import jp.co.sss.lms.util.AttendanceUtil;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.DateUtil;
import jp.co.sss.lms.util.MessageUtil;
import jp.co.sss.lms.util.PlaceUtil;

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

	/**
	 * 会場DTOリストの取得
	 * 
	 * @author 馬場成樹  – Task.58
	 * @param placeId
	 * @return 会場名
	 */
	public String getPlaceName(Integer placeId) {

		MPlace mPlace = new MPlace();

		// 会場DTOリストの取得
		mPlace = mPlaceMapper.findByPlaceId(placeId, Constants.DB_HIDDEN_FLG_FALSE, Constants.DB_FLG_FALSE);
		
		// 会場名を生成
		String placeName = mPlace.getPlaceName() + "(" + PlaceUtil.getPlaceNoteClassRoomName(mPlace.getPlaceNote()) + ")";

		// 備考に「$$」「タイトル$$」のように教室名がない場合は、nullを設定
		if (placeName.contains("$$")) {
			return null;
		}
		
		return placeName;
	}

	/**
	 * ユーザー勤怠情報DTOリストの取得
	 * 
	 * @author 馬場成樹  – Task.58
	 * @param attendanceForm
	 * @return ユーザー勤怠情報DTOリスト
	 */
	public List<UserAttendanceDto> getUserAttendanceDto(AttendanceForm attendanceForm) {
		// String型をDate型に変換
		Date searchPeriodFrom = dateUtil.stringToSqlDate(attendanceForm.getSearchPeriodFrom());
		Date searchPeriodTo = dateUtil.stringToSqlDate(attendanceForm.getSearchPeriodTo());

		return mPlaceMapper.getUserAttendanceDto(attendanceForm.getPlaceId(),searchPeriodFrom, searchPeriodTo, Constants.DB_FLG_FALSE);
	}

}

