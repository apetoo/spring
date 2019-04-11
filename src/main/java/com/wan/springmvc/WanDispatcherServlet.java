package com.wan.springmvc;

import com.wan.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @author 万明宇
 * @date 2019/3/29 10:37
 */
public class WanDispatcherServlet extends HttpServlet {

    private Properties contextConfig = new Properties();

    private List<String> classNames = new ArrayList<>();

    private Map<String, Object> ioc = new HashMap<>();

    private List<HandlerMapping> handlerMapping = new ArrayList<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        super.doPost(req, resp);
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception,Detail:" + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {


        HandlerMapping handler = getHandler(req);
        if (null == handler) {
            resp.getWriter().write("404 Not Found!");
            return;
        }
        Map<String, Integer> paramIndexMapping = handler.paramIndexMapping;
        Class<?>[] paramTypes = handler.getParamTypes();

        Object[] paramValues = new Object[paramTypes.length];

        Map<String, String[]> parameterMap = req.getParameterMap();

        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String value = Arrays.toString(entry.getValue()).replaceAll("\\[|\\]", "")
                    .replaceAll("\\s", ",");
            String key = entry.getKey();
            Integer index = paramIndexMapping.get(key);
            paramValues[index] = convert(paramTypes[index], value);

        }

        if (handler.paramIndexMapping.containsKey(HttpServletRequest.class.getName())) {
            int reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = req;
        }

        if (handler.paramIndexMapping.containsKey(HttpServletResponse.class.getName())) {
            int respIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[respIndex] = resp;
        }
        Object controller = handler.controller;
        Method method = handler.method;
        Object returnValue = method.invoke(controller, paramValues);
        if (returnValue == null || returnValue instanceof Void) {
            return;
        }
        resp.getWriter().write(returnValue.toString());


    }

    private Object convert(Class<?> type, String value) throws Exception {
        String name = type.getName();
        System.out.println("name = " + name);
        Class<?> aClass = Class.forName(name);
        return aClass;
    }

    private HandlerMapping getHandler(HttpServletRequest request) {

        if (handlerMapping.isEmpty()) {
            return null;
        }

        String url = request.getRequestURI();
        String contextPath = request.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");

        for (HandlerMapping mapping : this.handlerMapping) {
            if (url.equals(mapping.getUrl())) {
                return mapping;
            }
        }

        return null;

    }

    @Override
    public void init(ServletConfig config) throws ServletException {


        //1、加载配置文件
        String contextConfigLocation = config.getInitParameter("contextConfigLocation");
        doLoadConfig(contextConfigLocation);

        //2、扫描相关的类
        String scanPackage = contextConfig.getProperty("scanPackage");
        doScanner(scanPackage);

        //3、初始化扫描到的类，并且将它们放入到ICO容器之中
        doInstance();

        //4、完成依赖注入
        doAutowired();

        //5、初始化HandlerMapping
        initHandlerMapping();

        System.out.println("GP Spring framework is init.");

    }

    private void initHandlerMapping() {

        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> aClass = entry.getValue().getClass();
            String baseUrl = "";
            if (aClass.isAnnotationPresent(WanRquestMapping.class)) {
                WanRquestMapping wanRquestMapping = aClass.getAnnotation(WanRquestMapping.class);
                baseUrl = "/" + wanRquestMapping.value();
            }
            for (Method method : aClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(WanRquestMapping.class)) {
                    WanRquestMapping annotation = method.getAnnotation(WanRquestMapping.class);
                    String url = baseUrl + "/" + annotation.value();
                    url = url.replaceAll("/+", "/");
//                    handlerMapping.put(url, method);

                    this.handlerMapping.add(new HandlerMapping(url, method, entry.getValue()));
                }
            }


        }
    }

    private void doAutowired() {

        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(WanAutowired.class)) {
                    continue;
                }
                WanAutowired wanAutowired = field.getAnnotation(WanAutowired.class);
                String beanName = wanAutowired.name().trim();
                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }

                field.setAccessible(true);

                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        for (String className : classNames) {
            Class<?> aClass = null;
            try {
                aClass = Class.forName(className);

                if (aClass.isAnnotationPresent(WanController.class)) {
                    Object newInstance = aClass.newInstance();
                    String simpleName = aClass.getSimpleName();
                    ioc.put(toLowerFirstCase(simpleName), newInstance);
                }

                if (aClass.isAnnotationPresent(WanService.class)) {
                    WanService wanService = aClass.getAnnotation(WanService.class);
                    String beanName = wanService.value();
                    Object newInstance = aClass.newInstance();
                    String simpleName = aClass.getSimpleName();
                    if ("".equals(beanName.trim())) {
                        beanName = toLowerFirstCase(simpleName);
                    }
                    ioc.put(beanName, newInstance);
                    for (Class<?> clazz : aClass.getInterfaces()) {
                        if (ioc.containsKey(clazz.getName())) {
                            continue;
                        } else {
                            ioc.put(clazz.getName(), newInstance);
                        }
                    }
                }
                System.out.println("ioc = " + ioc);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private String toLowerFirstCase(String value) {
        char[] chars = value.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doScanner(String scanPackage) {

        String s = scanPackage.replaceAll("\\.", "/");
        URL url = this.getClass().getClassLoader().getResource("/" + s);
        File classPath = new File(url.getFile());
        for (File file : classPath.listFiles()) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                String className = scanPackage + "." + file.getName().replaceAll(".class", "");
                System.out.println("className = " + className);
                //将className绝对路径存放到list中
                classNames.add(className);
            }

        }

    }

    private void doLoadConfig(String contextConfigLocation) {

        InputStream fis = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public class HandlerMapping {

        private String url;
        private Method method;
        private Object controller;
        private Class<?>[] paramTypes;

        /**
         * 形参列表
         */
        private Map<String, Integer> paramIndexMapping;

        public HandlerMapping(String url, Method method, Object controller) {
            this.url = url;
            this.method = method;
            this.controller = controller;
            this.paramTypes = method.getParameterTypes();
            paramIndexMapping = new HashMap<>();
            putParamIndexMapping(method);
        }

        private void putParamIndexMapping(Method method) {

            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            for (int i = 0; i < parameterAnnotations.length; i++) {
                Annotation[] parameterAnnotation = parameterAnnotations[i];
                for (int j = 0; j < parameterAnnotation.length; j++) {
                    Annotation annotation = parameterAnnotation[j];
                    if (annotation instanceof WanRequestParam) {
                        String paramName = ((WanRequestParam) annotation).value();
                        paramIndexMapping.put(paramName, i);
                    }

                }
            }

            //提取方法中的request和response参数
            Class<?>[] paramsTypes = method.getParameterTypes();
            for (int i = 0; i < paramsTypes.length; i++) {
                Class<?> type = paramsTypes[i];
                if (type == HttpServletRequest.class ||
                        type == HttpServletResponse.class) {
                    paramIndexMapping.put(type.getName(), i);
                }
            }
        }

        public String getUrl() {
            return url;
        }

        public Method getMethod() {
            return method;
        }

        public Object getController() {
            return controller;
        }

        public Class<?>[] getParamTypes() {
            return paramTypes;
        }
    }
}
