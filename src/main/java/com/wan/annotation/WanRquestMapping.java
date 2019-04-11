package com.wan.annotation;

import java.lang.annotation.*;

/**
 * @author 万明宇
 * @date 2019/3/29 11:00
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WanRquestMapping {

    String value() default "";
}
