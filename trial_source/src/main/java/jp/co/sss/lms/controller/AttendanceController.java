package jp.co.sss.lms.controller;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.form.DailyAttendanceForm;
import jp.co.sss.lms.service.PlaceService;
import jp.co.sss.lms.service.StudentAttendanceService;
import jp.co.sss.lms.util.Constants;

/**
 * 勤怠管理コントローラ
 * 
 * @author 東京ITスクール
 */
@Controller
@RequestMapping("/attendance")
public class AttendanceController {

	@Autowired
	private StudentAttendanceService studentAttendanceService;
	@Autowired
	private PlaceService placeService;
	@Autowired
	private LoginUserDto loginUserDto;

	/**
	 * 勤怠管理画面 初期表示
	 * 
	 * @param lmsUserId
	 * @param courseId
	 * @param model
	 * @return 勤怠管理画面
	 * @throws ParseException
	 */
	@RequestMapping(path = "/detail", method = RequestMethod.GET)
	public String index(Model model) {
		
		// 勤怠一覧の取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		// 馬場成樹 – Task.25
		// 勤怠未入力判定結果の取得
		model.addAttribute("notEnterFlg", studentAttendanceService.hasMissingAttendance(loginUserDto.getLmsUserId()));

		return "attendance/detail";
	}

	/**
	 * 勤怠管理画面 『出勤』ボタン押下
	 * 
	 * @param model
	 * @return 勤怠管理画面
	 */
	@RequestMapping(path = "/detail", params = "punchIn", method = RequestMethod.POST)
	public String punchIn(Model model) {

		// 更新前のチェック
		String error = studentAttendanceService.punchCheck(Constants.CODE_VAL_ATWORK);
		model.addAttribute("error", error);
		// 勤怠登録
		if (error == null) {
			String message = studentAttendanceService.setPunchIn();
			model.addAttribute("message", message);
		}
		// 一覧の再取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		return "attendance/detail";
	}

	/**
	 * 勤怠管理画面 『退勤』ボタン押下
	 * 
	 * @param model
	 * @return 勤怠管理画面
	 */
	@RequestMapping(path = "/detail", params = "punchOut", method = RequestMethod.POST)
	public String punchOut(Model model) {

		// 更新前のチェック
		String error = studentAttendanceService.punchCheck(Constants.CODE_VAL_LEAVING);
		model.addAttribute("error", error);
		// 勤怠登録
		if (error == null) {
			String message = studentAttendanceService.setPunchOut();
			model.addAttribute("message", message);
		}
		// 一覧の再取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		return "attendance/detail";
	}

	/**
	 * 勤怠管理画面 『勤怠情報を直接編集する』リンク押下
	 * 
	 * @param model
	 * @return 勤怠情報直接変更画面
	 */
	@RequestMapping(path = "/update")
	public String update(Model model) {

		// 勤怠管理リストの取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		// 勤怠フォームの生成
		AttendanceForm attendanceForm = studentAttendanceService
				.setAttendanceForm(attendanceManagementDtoList);
		model.addAttribute("attendanceForm", attendanceForm);

		return "attendance/update";
	}

	/**
	 * 勤怠情報直接変更画面 『更新』ボタン押下
	 * 
	 * @param attendanceForm
	 * @param model
	 * @param result
	 * @return 勤怠管理画面
	 * @throws ParseException
	 */
	@RequestMapping(path = "/update", params = "complete", method = RequestMethod.POST)
	public String complete(AttendanceForm attendanceForm, Model model, BindingResult result)
			throws ParseException {

		// 更新
		String message = studentAttendanceService.update(attendanceForm);
		model.addAttribute("message", message);
		// 一覧の再取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		return "attendance/detail";
	}

	/**
	 * 勤怠一括登録画面 初期表示
	 * 
	 * @param model
	 * @return 勤怠一括登録画面
	 */
	@RequestMapping(path = "/bulkRegist", method = RequestMethod.GET)
	public String bulkRegist(Model model) {
		
		model.addAttribute("placeId",loginUserDto.getPlaceId());
		model.addAttribute("placeName", placeService.getPlaceName(loginUserDto.getPlaceId()));
		model.addAttribute("isSearch", false);

		return "attendance/bulkRegist";
	}

	/**
	 * 勤怠一括登録画面 『検索』ボタン押下
	 * 
	 * @param model
	 * @return 勤怠一括登録画面
	 */
	@RequestMapping(path = "/bulkRegist/search", method = RequestMethod.POST)
	public String search(Model model, @ModelAttribute("attendanceForm") AttendanceForm attendanceForm) {

		// 入力チェック
		Map<String, String> errors = placeService.searchParamCheck(attendanceForm);
		
		if (!errors.isEmpty()) {
			model.addAttribute("errors", errors);
			return "attendance/bulkRegist";
		}

		model.addAttribute("dailyAttendanceFormMap",
				placeService.setDailyAttendanceForm(placeService.getUserAttendanceDto(attendanceForm)));
		model.addAttribute("isSearch", true);

		return "attendance/bulkRegist";
	}

	/**
	 * 『確定』ボタン押下
	 * 
	 * @param model
	 * @return 勤怠一括登録画面
	 */
	@RequestMapping(path = "/bulkRegist/complete", method = RequestMethod.POST)
	public String complete(Model model, @RequestParam Map<String, String> paramMap) {
		List<DailyAttendanceForm> list = new ArrayList<>();

		// HTMLのパラメータを設定
		for (Map.Entry<String, String> var : paramMap.entrySet()) {
			String key = var.getKey();
			String value = var.getValue();

			if (key != null && (key.startsWith("dailyAttendanceList[") && key.contains("]."))) {
				DailyAttendanceForm daf = new DailyAttendanceForm();

				int startIndexPos = key.indexOf('[') + 1;
				int endIndexPos = key.indexOf(']') + 1;
//				int index = Integer.parseInt(key.substring(startIndexPos, endIndexPos));
				String field = key.substring(key.indexOf("].") + 2);

				System.out.println("★：" + field + "→" + value);
				switch (field) {
					case "setTrainingStartTime" -> daf.setTrainingStartTime(value.equals("[未入力]") ? "" : value);
					case "setTrainingEndTime" -> daf.setTrainingEndTime(value.equals("[未入力]") ? "" : value);
					case "setStatus" -> daf.setStatus(value);
					case "setCompanyAttendanceId" -> daf.setCompanyAttendanceId(Integer.parseInt(value));
					case "setStudentAttendanceId" -> daf.setStudentAttendanceId(Integer.parseInt(value));
					case "setLmsUserId" -> daf.setLmsUserId(value);
					case "setTrainingDate" -> daf.setTrainingDate(value);
				}

				list.add(daf);

			}
		}
		
		for (var var : list) {
			System.out.println(var);
		}

/*
		// 入力チェック
		Map<String, String> errors = placeService.completeParamCheck(list);
		
		if (!errors.isEmpty()) {
			model.addAttribute("errors", errors);
			return "attendance/bulkRegist";
		}
*/
		System.out.println("*************************");
		System.out.println(list);
		model.addAttribute("testList", list);
		return "attendance/bulkRegist";
	}
}