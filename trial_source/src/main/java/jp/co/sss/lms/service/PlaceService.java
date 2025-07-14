package jp.co.sss.lms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.co.sss.lms.entity.MPlace;
import jp.co.sss.lms.mapper.MPlaceMapper;
import jp.co.sss.lms.util.Constants;

/**
 * 会場情報サービス
 * 
 * @author 馬場成樹  – Task.58
 */
@Service
public class PlaceService {

	@Autowired
	private MPlaceMapper mPlaceMapper;

	public String getPlaceName(Integer placeId) {

		MPlace mPlace = new MPlace();
		mPlace = mPlaceMapper.findByPlaceId(placeId, Constants.DB_HIDDEN_FLG_FALSE, Constants.DB_FLG_FALSE);
		String placeName = mPlace.getPlaceName() + "(" + mPlace.getPlaceNote() + ")";

		if (placeName.contains("$$")) {
			return null;
		}
		
		return placeName;
	}
}
