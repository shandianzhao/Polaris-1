package com.nepxion.polaris.component.env.processor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.nepxion.polaris.component.common.constant.PolarisConstant;

public abstract class PolarisEnvProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(PolarisEnvProcessor.class);

    public void process(Environment environment) throws Exception {
        String name = getName();

        processCommonProperties(environment, name);
        processEnvProperties(environment, name);
    }

    public void processCommonProperties(Environment environment, String name) throws Exception {
        String path = PolarisConstant.META_INF_PATH + name + "-" + PolarisConstant.COMMON + "." + PolarisConstant.PROPERTIES_FORMAT;

        processProperties(environment, path);
    }

    public void processEnvProperties(Environment environment, String name) throws Exception {
        String env = getEnv();
        String path = PolarisConstant.META_INF_PATH + name + "-" + env + "." + PolarisConstant.PROPERTIES_FORMAT;

        processProperties(environment, path);
    }

    private void processProperties(Environment environment, String path) throws Exception {
        Properties properties = new Properties();

        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;

        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource(path);
        try {
            inputStream = resource.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream);

            properties.load(inputStreamReader);
        } finally {
            IOUtils.closeQuietly(inputStreamReader);
            IOUtils.closeQuietly(inputStream);
        }

        String zone = PolarisEnvProvider.getZone();

        for (String key : properties.stringPropertyNames()) {
            // 如果已经设置，则尊重已经设置的值
            if (environment.getProperty(key) == null && System.getProperty(key) == null) {
                String value = properties.getProperty(key);
                value = processDomain(value, zone);

                LOG.info("* Env parameter : {} = {}", key, value);

                System.setProperty(key, value);
            } else {
                LOG.info("* Env parameter : {} has been set", key);
            }
        }
    }

    // 根据server.properties里配置的env和zone，动态解析和创建多活或者多云的域名
    // 域名表达式，样例：nacos-fat{-%zone%}.nepxion.com，该域名格式为组件-环境-区域.根域，也可以用其它符号代替"-"
    // 区域，zone表示用来区别多活或者多云的域名后缀或者前缀
    @SuppressWarnings("deprecation")
    private String processDomain(String domainExpression, String zone) {
        String zoneExpression = "%" + PolarisConstant.ZONE + "%";

        String domain = null;
        // 不符合域名表达式的配置项，不做处理直接返回
        if (StringUtils.contains(domainExpression, zoneExpression) && StringUtils.contains(domainExpression, "{") && StringUtils.contains(domainExpression, "}") && StringUtils.indexOf(domainExpression, "}") - StringUtils.indexOf(domainExpression, "{") >= zoneExpression.length()) {
            if (StringUtils.isNotBlank(zone)) {
                // 兼容低版本的commons-langs
                domain = StringUtils.replaceAll(domainExpression, zoneExpression, zone);
                domain = StringUtils.replace(domain, "{", StringUtils.EMPTY);
                domain = StringUtils.replace(domain, "}", StringUtils.EMPTY);
            } else {
                // 兼容低版本的commons-langs
                domain = StringUtils.replaceAll(domainExpression, "\\{\\S+\\}", StringUtils.EMPTY);
            }
        } else {
            domain = domainExpression;
        }

        return domain;
    }

    public String getEnv() {
        return PolarisEnvProvider.getEnv();
    }

    public String getZone() {
        return PolarisEnvProvider.getZone();
    }

    public String getProjectName(Environment environment) {
        try {
            String appId = "";

            return appId;
        } catch (Exception e) {
            String applicationName = environment.getProperty(PolarisConstant.SPRING_APPLICATION_NAME);

            return applicationName;
        }
    }

    public String getServerPropertiesPath() {
        return PolarisEnvProvider.getServerPropertiesPath();
    }

    public String getLogPath() {
        return PolarisEnvProvider.getLogPath();
    }

    public abstract String getName();
}