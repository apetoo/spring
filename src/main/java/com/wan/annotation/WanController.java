package com.wan.annotation;

import java.lang.annotation.*;

/**
 * @author 万明宇
 * @date 2019/3/29 10:59
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WanController {
}
