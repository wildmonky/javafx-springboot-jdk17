package org.lizhao.validator.utils;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RMICleanupUtils {

    private static Registry registry;
    private static List<Remote> exportedObjects = new ArrayList<>();

    public static void exportObject(Remote obj, int port) throws RemoteException {
        Remote stub = UnicastRemoteObject.exportObject(obj, port);
        exportedObjects.add(obj);
    }

    public static void cleanup() {
        // 取消导出所有对象
        for (Remote obj : exportedObjects) {
            try {
                UnicastRemoteObject.unexportObject(obj, true);
            } catch (NoSuchObjectException e) {
                // 忽略，对象可能已经取消导出
            }
        }
        exportedObjects.clear();

        // 关闭注册表
        if (registry != null) {
            try {
                UnicastRemoteObject.unexportObject(registry, true);
            } catch (NoSuchObjectException e) {
                // 忽略
            }
            registry = null;
        }

        // 强制终止 RMI 相关线程
        terminateRMIThreads();
    }

    /**
     * idea插件cool request问题，开了RMI，我未在程序中关闭，导致RMI Reaper未正常关闭
     */
    private static void terminateRMIThreads() {
//        Set<Thread> threads = Thread.getAllStackTraces().keySet();
//        for (Thread thread : threads) {
//            if (thread.getName().contains("RMI") ||
//                    thread.getName().contains("JMX") ||
//                    thread.getName().contains("Timer")) {
//                System.out.println("中断线程: " + thread.getName());
//                thread.interrupt();
//            }
//        }
//        for (Thread thread : threads) {
//            if (thread.getName().contains("RMI Reaper")) {
//                System.out.println("中断线程: " + thread.getName());
//                thread.interrupt();
//            }
//        }
    }
}
