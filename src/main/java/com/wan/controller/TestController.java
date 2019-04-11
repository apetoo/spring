package com.wan.controller;

import com.wan.annotation.WanAutowired;
import com.wan.annotation.WanController;
import com.wan.annotation.WanRequestParam;
import com.wan.annotation.WanRquestMapping;
import com.wan.service.TestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author 万明宇
 * @date 2019/3/29 10:58
 */
@WanController
public class TestController {

    @WanAutowired
    private TestService testService;

    @WanRquestMapping("/test1")
    public void testwan1(HttpServletRequest request,HttpServletResponse response) throws IOException {
        response.getWriter().write("test01");
    }

    @WanRquestMapping("/test2")
    public void testwan2(HttpServletRequest request,HttpServletResponse response, @WanRequestParam("name") String name) throws IOException {
//        String name = request.getParameter("name");
        response.getWriter().write(testService.test01(name));
    }
}
