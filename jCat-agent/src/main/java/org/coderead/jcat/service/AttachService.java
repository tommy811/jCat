package org.coderead.jcat.service;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import com.sun.tools.attach.*;
import lombok.Getter;
import org.coderead.jcat.Agent;
import org.coderead.jcat.common.Assert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 利用agentmain 装载至目标jvm进程
 *
 * @author 鲁班大叔
 * @date 2024
 */
public class AttachService {
    // 获取jvm 列表
    Map<String, JvmItem> attachMap = new HashMap<>();
    static final Logger logger = Logger.getLogger(AttachService.class.getName());
    static final String pid;

    static {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        pid = runtimeMXBean.getName().split("@")[0];
    }

    public AttachService() {
        DefaultHttpServer httpServer = DefaultHttpServer.getInstance();
    }

    public Collection<JvmItem> jvmList() {
        List<VirtualMachineDescriptor> list = VirtualMachine.list();
        Map<String, JvmItem> items = list.stream()
                .filter(v -> !v.id().equals(pid)) //排除自身
                .map(v ->
                        attachMap.containsKey(v.id()) ? attachMap.get(v.id()) : new JvmItem(v.id(), v.displayName())
                ).collect(Collectors.toMap(s -> s.id, v -> v));
        checkAndCleanJvm(items);

        return items.values();
    }




    // 判断目标虚拟机是否在线
    private void checkAndCleanJvm(Map<String, JvmItem> currentJvmMaps) {
        Iterator<Map.Entry<String, JvmItem>> iterator = attachMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, JvmItem> next = iterator.next();
            if (!currentJvmMaps.containsKey(next.getKey())) {
                logger.info("目标jvm离线：" + next.getValue());
                iterator.remove();
            }
        }
    }

    /**
     * configs 不允许换行，使用(,)逗号分割
     *
     * @return
     */
    public JvmItem attach(String id,String  agentPath,  String configs) {
        Assert.hasText(id, "参数id不能为空");
        // 参数配置
        Properties configsPro = new Properties();
        if (configs != null) {
            try {
                configsPro.load(new ByteArrayInputStream(
                        configs.replaceAll(",", "\n").getBytes()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (!configsPro.contains("port")) {
            configsPro.put("port", getInitPort());
        }
        configs = configsPro.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(","));
        float currentJvmVersion = 0f, targetJvmVersion = 0f;
        VirtualMachine vm = null;
        VirtualMachineDescriptor virtualMachineDescriptor;
        String httpPort;
        String warningMessage = null;

        File file = new File(agentPath);
        Assert.isTrue(file.exists(),"找不到方件:"+file);
        agentPath=file.toString();
        Assert.isTrue(agentPath.endsWith(".jar") || agentPath.endsWith(".lib"),"找不到agent.path"); ;
        virtualMachineDescriptor = VirtualMachine.list().stream().filter(v -> v.id().equals(id)).findFirst().get();
        // 1.attach
        try {
            vm = VirtualMachine.attach(virtualMachineDescriptor);
            Properties targetVmProperties = vm.getSystemProperties();
            // 验证jvm版本信息
            currentJvmVersion = getJavaVersion(System.getProperties());
            targetJvmVersion = getJavaVersion(targetVmProperties);
            if (targetJvmVersion != currentJvmVersion) {
                warningMessage = String.format("与目标JVM版本不一至，可能引发agent装载错误，当前JVM%s,目标JVM%s", currentJvmVersion, targetJvmVersion);
                logger.warning(warningMessage);
            }
        } catch (AttachNotSupportedException e) {
            throw new IllegalStateException("目标虚拟机不支持Attach", e);
        } catch (IOException e) {
            throw new IllegalStateException("连接(attach)目标虚拟机失败", e);
        }
        // 2.loadAgent
        try {
            vm.loadAgent(agentPath, configs);
        } catch (AgentLoadException e) {
            if ("0".equals(e.getMessage())) {
                // https://stackoverflow.com/a/54454418
                warningMessage = String.format("与目标JVM版本不一至,当前JVM%s 目标JVM%s", currentJvmVersion, targetJvmVersion);
                logger.log(Level.WARNING, warningMessage, e);
            } else {
                throw new IllegalStateException("agent装载失败", e);
            }
        } catch (AgentInitializationException e) {
            throw new IllegalStateException("agent初始化失败", e);
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("Non-numeric value found")) {
                warningMessage = String.format("与目标JVM版本不一至,当前JVM%s 目标JVM%s", currentJvmVersion, targetJvmVersion);
                logger.log(Level.WARNING, warningMessage, e);
            } else {
                throw new IllegalStateException("读取目标虚拟机信息失败", e);
            }
        }
        // 3.验证端口是否顺利打开
        try {
            httpPort = vm.getSystemProperties().getProperty("jcat.agent.httpPort");
            Assert.hasText(httpPort, "agent打开端口失败：未能获取到目标通信端口");
            vm.detach();// 分离端口
        } catch (IOException e) {
            throw new IllegalStateException("读取目标虚拟机信息失败", e);
        }

        // 4.封装返回结果
        JvmItem jvmItem = new JvmItem(vm.id(), virtualMachineDescriptor.displayName());
        jvmItem.targetPort = Integer.parseInt(httpPort);
        jvmItem.attachTime = System.currentTimeMillis();
        jvmItem.jvmVersion = targetJvmVersion;
        jvmItem.warningMessage = warningMessage;
        attachMap.put(jvmItem.id, jvmItem);
        return jvmItem;
    }

    private float getJavaVersion(Properties systemProperties) {
        return Float.parseFloat(systemProperties.getProperty("java.specification.version"));
    }

    private int getInitPort() {
        return attachMap.values().stream().mapToInt(s -> s.targetPort).max().orElse(3426);
    }

    @Getter
    public static class JvmItem implements Serializable {
        String id;
        String name;
        long attachTime; // 负载时间
        int targetPort;// 目标虚拟机通信端口
        String targetIp = "127.0.0.1";
        String warningMessage;
        float jvmVersion;

        public JvmItem(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return "JvmItem{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", attachTime=" + attachTime +
                    ", targetPort=" + targetPort +
                    ", targetIp='" + targetIp + '\'' +
                    '}';
        }
    }
}
