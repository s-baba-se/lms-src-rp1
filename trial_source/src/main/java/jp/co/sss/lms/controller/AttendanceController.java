package jp.co.sss.lms.controller;

import java.text.ParseException;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.dto.UserAttendanceDto;
import jp.co.sss.lms.entity.TCompanyAttendance;
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
	 * @author 馬場成樹  – Task.58
	 * @param model
	 * @return 勤怠一括登録画面
	 */
	@RequestMapping(path = "/bulkRegist", method = RequestMethod.GET)
	public String bulkRegist(Model model) {
		
		// 会場ID・会場名設定
		model.addAttribute("placeId",loginUserDto.getPlaceId());
		model.addAttribute("placeName", placeService.getPlaceName(loginUserDto.getPlaceId()));

		return "attendance/bulkRegist";
	}

	/**
	 * 勤怠一括登録画面 『検索』ボタン押下
	 * 
	 * @author 馬場成樹  – Task.58
	 * @param model
	 * @param attendanceForm
	 * @return 勤怠一括登録画面
	 */
	@RequestMapping(path = "/bulkRegist/search", method = {RequestMethod.GET, RequestMethod.POST})
	public String search(Model model, @ModelAttribute("attendanceForm") AttendanceForm attendanceForm) {

		System.out.println(attendanceForm);
		// 入力チェック処理
		Map<String, String> errors = studentAttendanceService.searchParamCheck(attendanceForm);

		model.addAttribute("placeId",loginUserDto.getPlaceId());
		model.addAttribute("placeName", placeService.getPlaceName(loginUserDto.getPlaceId()));

		// エラーの場合、エラーメッセージを返す
		if (!errors.isEmpty()) {
			model.addAttribute("errors", errors);
			System.out.println(attendanceForm);
			model.addAttribute("attendanceForm", attendanceForm);
			return "attendance/bulkRegist";
		}

		// ユーザー勤怠情報DTOリストの取得
		List<UserAttendanceDto> userAttendanceDtoList = placeService.getUserAttendanceDto(attendanceForm);

		// 日別勤怠情報フォームリストの設定
		Map<String, List<DailyAttendanceForm>> dailyAttendanceFormMap = studentAttendanceService.setDailyAttendanceForm(userAttendanceDtoList);

		model.addAttribute("dailyAttendanceFormMap", dailyAttendanceFormMap);
		model.addAttribute("attendanceForm", attendanceForm);

		return "attendance/bulkRegist";
	}

	/**
	 * 『確定』ボタン押下
	 * 
	 * @param model
	 * @return 勤怠一括登録画面
	 */
	@RequestMapping(path = "/bulkRegist/complete", method = RequestMethod.POST)
	public String complete(RedirectAttributes redirectAttributes,
			Integer mapIndex,
			@RequestParam Map<String, String> paramMap,
			@ModelAttribute("attendanceForm") AttendanceForm attendanceForm) {

		// パラメータ設定
		List<DailyAttendanceForm> dailyAttendanceFormList = studentAttendanceService.setParamMap(paramMap, mapIndex); 

		// 入力チェック
		Map<String, String> errors = studentAttendanceService.completeParamCheck(dailyAttendanceFormList, mapIndex);

		if (!errors.isEmpty()) {
			redirectAttributes.addFlashAttribute("errors", errors);
			redirectAttributes.addFlashAttribute("mapIndex", mapIndex);
			redirectAttributes.addFlashAttribute("attendanceForm", attendanceForm);
			return "redirect:/attendance/bulkRegist/search";
		}

		// 勤怠情報（企業入力）リストの設定
		List<TCompanyAttendance> tCompanyAttendanceList = studentAttendanceService.setCompanyAttendanceList(dailyAttendanceFormList);

		// 勤怠情報（企業入力）テーブルのデータ登録／更新
		String message = studentAttendanceService.updateCompanyAttendanceDB(tCompanyAttendanceList);

		redirectAttributes.addFlashAttribute("attendanceForm", attendanceForm);
		redirectAttributes.addFlashAttribute("mapIndex", mapIndex);
		redirectAttributes.addFlashAttribute("message", message);
		return "redirect:/attendance/bulkRegist/search";
	}
}