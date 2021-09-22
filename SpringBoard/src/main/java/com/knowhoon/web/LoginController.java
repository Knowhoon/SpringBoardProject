package com.knowhoon.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.JsonNode;
import com.knowhoon.util.Util;
import com.knowhoon.web.log.LogDTO;
import com.knowhoon.web.log.LogService;
import com.knowhoon.web.login.KakaoService;
import com.knowhoon.web.login.LoginDTO;
import com.knowhoon.web.login.LoginService;

@Controller
public class LoginController {
	@Autowired
	private LoginService loginService;
	@Autowired
	private LogService logService;
	@Autowired
	private Util util;
	 @Autowired
	private KakaoService kakaoService;


	@GetMapping("/login")
	public String login() {
		
		return "login";
	}
	//로그인
	@PostMapping("/login")
	public String loginAction(LoginDTO loginDTO, HttpServletRequest request) {
		LoginDTO result = loginService.loginAction(loginDTO);
		System.out.println(request.getParameter("sm_id"));
		System.out.println(request.getParameter("sm_pw"));
		if(result != null) {
			request.getSession().setAttribute("sm_id", result.getSm_id());
			request.getSession().setAttribute("sm_name", result.getSm_name());
			request.getSession().setAttribute("sm_grade", result.getSm_grade());
			String target = "login";
			LogDTO log = new LogDTO(
					util.getIP(request), 
					target , 
					(String)request.getSession().getAttribute("sm_id"), 
					target + "성공");
			logService.insertLog(log);	
			return "redirect:/home";			
		} else {
			if(request.getSession().getAttribute("sm_id") != null) {
				request.getSession().removeAttribute("sm_id");				
			}
			if(request.getSession().getAttribute("sm_name") != null) {
				request.getSession().removeAttribute("sm_name");							
			}
			if(request.getSession().getAttribute("sm_grade") != null) {
				request.getSession().removeAttribute("sm_grade");							
			}
			return "redirect:/login?error=loginError";
		}
		
	}
	//카카오 로그인
	@RequestMapping("/kakaoLogin")
	public String kakaoLogin() {
		 StringBuffer loginUrl = new StringBuffer();
	        loginUrl.append("https://kauth.kakao.com/oauth/authorize?client_id=");
	        loginUrl.append("5f932911fbf9bd32c027dd5421cedb62"); 
	        loginUrl.append("&redirect_uri=");
	        loginUrl.append("http://localhost:8080/web/kakaoLoginReturn"); 
	        loginUrl.append("&response_type=code");
	        
	        return "redirect:"+loginUrl.toString();
	}
	//카카오 로그인 리턴
	@RequestMapping("/kakaoLoginReturn")
	public String kakao(@RequestParam String code, HttpSession session) {
		System.out.println(code);
		JsonNode accessToken;
        JsonNode jsonToken = kakaoService.getKakaoAccessToken(code);
        accessToken = jsonToken.get("access_token");
        //System.out.println("access_token : " + accessToken);
        JsonNode userInfo = KakaoService.getKakaoUserInfo(accessToken);
        
        // Get id
        String id = userInfo.path("id").asText();
        String name = null;
        //String email = null;
        String userImg = null;
        
        // 유저정보 카카오에서 가져오기 Get properties
        JsonNode properties = userInfo.path("properties");
        //JsonNode kakao_account = userInfo.path("kakao_account");
      
        name = properties.path("nickname").asText();
        //email = kakao_account.path("email").asText();
        userImg = properties.path("thumbnail_image").asText();
        
        //System.out.println("id : " + id);
        //System.out.println("name : " + name);
        //System.out.println("email : " + email);
        //System.out.println("userImg : " + userImg);
        session.setAttribute("sm_id", id);
        session.setAttribute("sm_name", name);
        session.setAttribute("userImg", userImg);
        return "redirect:/home";
	}
	//로그 아웃
	@GetMapping("/logout")
	public String logout(HttpServletRequest request) {
		String target = "logout";
		LogDTO log = new LogDTO(
				util.getIP(request), 
				target , 
				(String)request.getSession().getAttribute("sm_id"), 
				target + "성공");
		logService.insertLog(log);	
		if(request.getSession().getAttribute("sm_id") != null) {
			request.getSession().removeAttribute("sm_id");
		}
		if(request.getSession().getAttribute("sm_name") != null) {
			request.getSession().removeAttribute("sm_name");							
		}
		if(request.getSession().getAttribute("sm_grade") != null) {
			request.getSession().removeAttribute("sm_grade");							
		}
		if(request.getSession().getAttribute("userImg") != null) {
			request.getSession().removeAttribute("userImg");							
		}
		return "redirect:/home";
	}
	@GetMapping("/join")
	public String join() {		
		return "join";
	}
	//회원 가입
	@PostMapping("/join")
	public String joinAction(HttpServletRequest request, LoginDTO loginDTO) {
		int result = 0;
		result = loginService.joinAction(loginDTO);
		if(result == 1) {
			String target = "join";
			LogDTO log = new LogDTO(
					util.getIP(request), 
					target , 
					(String)request.getSession().getAttribute("sm_id"), 
					loginDTO.getSm_id()+" 회원가입 성공");
			logService.insertLog(log);	
			return "redirect:/home";			
		}else {
			return "redirect:/join?error=joinError";
		}
		
	}
	//아이디 중복 체크
	@PostMapping("/checkID")
	public @ResponseBody String checkID(HttpServletRequest request) {
		String check = "1";		
		check = loginService.checkID(request.getParameter("sm_id"));		
		return check;
	}
	//이메일 중복 체크
	@PostMapping("/checkEmail")
	public @ResponseBody String checkEmail(HttpServletRequest request) {
		String check = loginService.checkEmail(request.getParameter("sm_email"));		
		return check;
	}
	//이메일 인증번호 전송
	@PostMapping("/emailConfirm")
	public @ResponseBody String emailConfirm(HttpServletRequest request) {
		String authCode = util.getAuthCode(6);
		String email = request.getParameter("sm_email");
		String subject = "[본인인증] 회원가입 인증 이메일 입니다.";
		String content = 
		        "<h2>안녕하세요. 회원가입 인증 메일입니다. 방문해주셔서 감사합니다.</h2><br><br>"
		        +"인증 번호는 [ <b>" + authCode + "</b> ] 입니다." + 
		        "<br><br>" + 
		        "해당 인증번호를 인증번호 확인란에 기입하여 주세요.";
		util.sendEmail(authCode, email , subject, content);
		return authCode;	
	}
}
