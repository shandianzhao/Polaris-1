package com.nepxion.polaris.component.env.context;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepxion.polaris.component.common.constant.PolarisConstant;
import com.nepxion.polaris.component.env.constant.PolarisEnvConstant;
import com.nepxion.polaris.component.env.entity.PolarisEnv;

public class PolarisEnvProvider {
    private static final Logger LOG = LoggerFactory.getLogger(PolarisEnvProvider.class);

    private static String rootDomain;
    private static String zone;
    private static String env;
    private static String appId;

    static {
        initializeDomain();
        initializeZone();
        initializeEnv();
    }

    private static void initializeDomain() {
        try {
            rootDomain = initializeContext(PolarisEnvConstant.ROOT_DOMAIN_NAME);
        } catch (Exception e) {
            LOG.info("Initialize root domain failed, use root domain={} as default", PolarisEnvConstant.ROOT_DOMAIN_VALUE);
        }

        if (StringUtils.isBlank(rootDomain)) {
            // 通过三种方式之一未找到根域值，默认取预定义的静态变量值
            rootDomain = PolarisEnvConstant.ROOT_DOMAIN_VALUE;
        }

        // 设置到System Property，保证占位符生效。根域名值不可空缺的
        System.setProperty(PolarisEnvConstant.ROOT_DOMAIN_NAME, rootDomain);

        // 设置到System Property，提供给注册中心元数据用
        System.setProperty(PolarisConstant.POLARIS_ROOT_DOMIAN_NAME, rootDomain);
    }

    private static void initializeZone() {
        try {
            zone = initializeContext(PolarisEnvConstant.ZONE_NAME);
        } catch (Exception e) {
            LOG.info("Initialize zone failed, use no zone as default");
        }

        if (StringUtils.isBlank(zone)) {
            // 通过三种方式之一未找到区域，把空字符串设置到System Property，代替到占位符
            System.setProperty(PolarisEnvConstant.ZONE_NAME, StringUtils.EMPTY);
        } else {
            // 通过三种方式之一找到区域，进行分隔符拼接，并设置到System Property，保证占位符生效
            // 前缀方式，把分隔符放在zone前面；后缀方式，把分隔符放在zone后面
            if (PolarisEnvConstant.ZONE_SEPARATE_PREFIX) {
                System.setProperty(PolarisEnvConstant.ZONE_NAME, PolarisEnvConstant.ZONE_SEPARATE + zone);
            } else {
                System.setProperty(PolarisEnvConstant.ZONE_NAME, zone + PolarisEnvConstant.ZONE_SEPARATE);
            }

            // 设置到System Property，提供给注册中心元数据用
            System.setProperty(PolarisConstant.POLARIS_ZONE_NAME, zone);
        }
    }

    private static void initializeEnv() {
        try {
            env = initializeContext(PolarisEnvConstant.ENV_NAME);
        } catch (Exception e) {
            LOG.info("Initialize env failed, use env={} as default", PolarisEnv.DEV.getEnv());
        }

        if (StringUtils.isBlank(env)) {
            // 通过三种方式之一未找到环境值，默认取dev
            env = PolarisEnv.DEV.getEnv();
        }

        // 设置到System Property，提供给注册中心元数据用
        System.setProperty(PolarisConstant.POLARIS_ENV_NAME, env);
    }

    private static String initializeContext(String key) throws Exception {
        String value = System.getProperty(key);
        if (!StringUtils.isBlank(value)) {
            value = value.trim();
        } else {
            value = System.getenv(key.toUpperCase());
            if (!StringUtils.isBlank(value)) {
                value = value.trim();
            } else {
                Properties properties = new Properties();
                String path = getServerPropertiesPath();

                File file = new File(path);
                if (file.exists() && file.canRead()) {
                    FileInputStream inputStream = null;
                    InputStreamReader inputStreamReader = null;

                    try {
                        inputStream = new FileInputStream(file);
                        inputStreamReader = new InputStreamReader(inputStream);

                        properties.load(inputStreamReader);
                    } finally {
                        IOUtils.closeQuietly(inputStreamReader);
                        IOUtils.closeQuietly(inputStream);
                    }
                }

                value = properties.getProperty(key);
                if (!StringUtils.isBlank(value)) {
                    value = value.trim();
                }
            }
        }

        return value;
    }

    public static String getRootDomain() {
        return rootDomain;
    }

    public static String getZone() {
        return zone;
    }

    public static String getEnv() {
        return env;
    }

    public static String getAppId() {
        return appId;
    }

    public static void setAppId(String appId) {
        PolarisEnvProvider.appId = appId;
    }

    public static String getServerPropertiesPath() {
        return SystemUtils.IS_OS_WINDOWS ? PolarisEnvConstant.SERVER_PROPERTIES_PATH_WINDOWS : PolarisEnvConstant.SERVER_PROPERTIES_PATH_LINUX;
    }

    public static String getLogPath() {
        return SystemUtils.IS_OS_WINDOWS ? PolarisEnvConstant.LOG_PATH_WINDOWS : PolarisEnvConstant.LOG_PATH_LINUX;
    }
}