package com.microsoft.speech.xiaomi.controller;

import com.microsoft.speech.xiaomi.dto.ResponseCode;
import com.microsoft.speech.xiaomi.service.CheckEndpointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@RestController
public class HealthController {
	@Autowired
	private CheckEndpointService checkEndpointService;

	@RequestMapping(value = "/", method = {RequestMethod.GET,RequestMethod.POST})
	public String index() {
		String info = "";
		try {
			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
			String name = request.getParameter("name");
			if(name == null) name = "default-name-fortest";

			info = String.format("param-name=%s; queryStr=%s;method=%s", name, request.getQueryString(),request.getMethod());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "This is Xiaomi Demp App ---- . " + ResponseCode.AppName + "; " + info;
	}


	@GetMapping("/hs")
	public String health() {
		return "Health is OK!";
	}

	@RequestMapping(value = "/test", method = RequestMethod.GET)
	public @ResponseBody String test(){
		int candSize = checkEndpointService.getCandidateSize();
		return "It's OK, candSize=" + candSize;
	}
}
