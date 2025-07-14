package jp.co.sss.lms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.sss.lms.entity.MPlace;

/**
 * 会場マスタテーブルマッパー
 * 
 * @author 馬場成樹  – Task.58
 */
@Mapper
public interface MPlaceMapper {

	MPlace findByPlaceId(@Param("placeId") Integer placeId,
			@Param("hiddenFlg") Short hiddenFlg, @Param("deleteFlg") Short deleteFlg);

}
