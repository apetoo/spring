package com.wan.annotation;

import java.lang.annotation.*;

/**
 * @author 万明宇
 * @date 2019/3/31 16:12
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WanRequestParam {

    String value() default "";


}
