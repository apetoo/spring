package com.wan.service.impl;

import com.wan.annotation.WanService;
import com.wan.service.TestService;

/**
 * @author 万明宇
 * @date 2019/3/31 11:00
 */
@WanService
public class TestServiceImpl implements TestService {
    @Override
    public String test01(String name) {
        return name+"service";
    }
}
