package jp.co.sss.lms.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.co.sss.lms.entity.MPlace;
import jp.co.sss.lms.form.AttendanceBulkRegistSearchForm;
import jp.co.sss.lms.mapper.MPlaceMapper;
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

	
	public String getPlaceName(Integer placeId) {

		MPlace mPlace = new MPlace();
		mPlace = mPlaceMapper.findByPlaceId(placeId, Constants.DB_HIDDEN_FLG_FALSE, Constants.DB_FLG_FALSE);
		String placeName = mPlace.getPlaceName() + "(" + PlaceUtil.getPlaceNoteClassRoomName(mPlace.getPlaceNote()) + ")";

		if (placeName.contains("$$")) {
			return null;
		}
		
		return placeName;
	}
	
	public String searchParamCheck(AttendanceBulkRegistSearchForm abrsForm) {

		// 期間(FROM)の未入力チェック
		if (abrsForm.getSearchPeriodFrom() == null || abrsForm.getSearchPeriodFrom().isEmpty()) {
			return messageUtil.getMessage("required", new String[] {"期間（from）"});
		}

		// 期間(To)の未入力チェック
		if (abrsForm.getSearchPeriodTo() == null || abrsForm.getSearchPeriodTo().isEmpty()) {
			return messageUtil.getMessage("required", new String[] {"期間（to）"});
		}

		// 現在日を取得
		Date today = new Date();
		
		// String型をDate型に変換
		Date searchPeriodFrom = dateUtil.stringToSqlDate(abrsForm.getSearchPeriodFrom());
		Date searchPeriodTo = dateUtil.stringToSqlDate(abrsForm.getSearchPeriodTo());

		// 期間(To)が現在日付より未来日の場合
		if (searchPeriodTo.after(today)) {
			return messageUtil.getMessage("searchToRangeError", new String[] {"0"});
		}
		
		// 期間(To)が期間(From)より過去日の場合
		if (searchPeriodTo.before(searchPeriodFrom)) {
			return messageUtil.getMessage("searchPeriodCompareError",
					new String[] { abrsForm.getSearchPeriodTo(), abrsForm.getSearchPeriodFrom() });
		}
		
		// 期間(From) ～ 期間(To)の日数 が 30日より大きい場合
		int days = dateUtil.differenceDays(searchPeriodTo, searchPeriodFrom);
		if (days > 30) {
			return messageUtil.getMessage("searchSettingOver", new String[] {"0"});
		}
		return "";
	}
}
