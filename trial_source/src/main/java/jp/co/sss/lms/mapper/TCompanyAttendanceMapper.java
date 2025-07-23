package jp.co.sss.lms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.sss.lms.entity.TCompanyAttendance;

@Mapper
public interface TCompanyAttendanceMapper {

	/**
	 * 勤怠情報（企業入力）取得（企業入力勤怠情報ID）
	 * 
	 * @author 馬場成樹  – Task.58
	 * @param companyAttendanceId
	 * @param deleteFlg
	 * @return 勤怠情報（企業入力）情報
	 */
	TCompanyAttendance findByCompanyAttendanceId(@Param("companyAttendanceId") Integer companyAttendanceId,
			@Param("deleteFlg") Short deleteFlg);

	/**
	 * 勤怠情報（企業入力）登録
	 * 
	 * @author 馬場成樹  – Task.58
	 * @param tCompanyAttendance
	 * @return 登録結果（true/false）
	 */
	Boolean insert(TCompanyAttendance tCompanyAttendance);
	
	/**
	 * 勤怠情報（企業入力）更新
	 * 
	 * @author 馬場成樹  – Task.58
	 * @param tCompanyAttendance
	 * @return 更新結果（true/false）
	 */
	Boolean update(TCompanyAttendance tCompanyAttendance);
}
