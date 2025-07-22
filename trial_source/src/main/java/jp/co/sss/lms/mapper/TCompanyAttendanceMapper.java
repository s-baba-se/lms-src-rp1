package jp.co.sss.lms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.sss.lms.entity.TCompanyAttendance;

@Mapper
public interface TCompanyAttendanceMapper {

	TCompanyAttendance findByCompanyAttendanceId(@Param("companyAttendanceId") Integer companyAttendanceId,
			@Param("deleteFlg") Short deleteFlg);

	Boolean insert(@Param("tCompanyAttendance") TCompanyAttendance tCompanyAttendance);
	
	Boolean update(@Param("tCompanyAttendance") TCompanyAttendance tCompanyAttendance);
}
