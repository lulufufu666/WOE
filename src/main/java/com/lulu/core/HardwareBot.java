package com.lulu.core;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;

/**
 * 纯硬件级键鼠驱动 (突破游戏底层拦截)
 * @author 噜噜
 */
public class HardwareBot {

    public interface User32 extends Library {
        User32 INSTANCE = Native.load("user32", User32.class);
        short GetAsyncKeyState(int vKey);
        void mouse_event(int dwFlags, int dx, int dy, int dwData, int dwExtraInfo);
        boolean SetCursorPos(int X, int Y);
        void keybd_event(byte vKey, int i, int i1, int i2);

        // 🌟 新增：底层接口，用于通过窗口句柄获取进程 PID
        int GetWindowThreadProcessId(HWND hWnd, IntByReference pref);
    }

    private static final int MOUSEEVENTF_LEFTDOWN = 0x0002;
    private static final int MOUSEEVENTF_LEFTUP = 0x0004;


    // 🌟 新增：自动获取游戏启动的绝对路径 (无感黑科技)
    public static String getGameExecutablePath(HWND hwnd) {
        try {
            if (hwnd == null) return null;
            IntByReference pid = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);
            int processId = pid.getValue();
            if (processId == 0) return null;

            return ProcessHandle.of(processId)
                    .map(ProcessHandle::info) // 👈 这里修正为 map
                    .flatMap(ProcessHandle.Info::command)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    // 🌟 新增：系统级强杀游戏进程，保证彻底释放内存
    public static void killGameProcess(String windowTitle) {
        try {
            HWND hwnd = com.sun.jna.platform.win32.User32.INSTANCE.FindWindow(null, windowTitle);
            if (hwnd != null) {
                IntByReference pid = new IntByReference();
                User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);
                int processId = pid.getValue();
                if (processId > 0) {
                    Runtime.getRuntime().exec("taskkill /F /PID " + processId);
                }
            }
        } catch (Exception ignored) {}
    }

    public static boolean isKeyPressed(int vKey) {
        try {
            short state = User32.INSTANCE.GetAsyncKeyState(vKey);
            return (state & 0x8000) != 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static void moveMouse(int x, int y) {
        User32.INSTANCE.SetCursorPos(x, y);
    }

    public static void clickLeftMouse() {
        User32.INSTANCE.mouse_event(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0);
        try {
            Thread.sleep((long) (20 + Math.random() * 20));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        User32.INSTANCE.mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0);
    }

    public static void clickAt(int x, int y) {
        moveMouse(x, y);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        clickLeftMouse();
    }

    public static void pressKey(int vKey) {
        User32.INSTANCE.keybd_event((byte)vKey, 0, 0, 0);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        User32.INSTANCE.keybd_event((byte)vKey, 0, 2, 0);
    }

    public static void dragTo(int startX, int startY, int endX, int endY) {
        User32.INSTANCE.SetCursorPos(startX, startY);
        User32.INSTANCE.mouse_event(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0);

        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        int steps = 20;
        for (int i = 1; i <= steps; i++) {
            int midX = startX + (endX - startX) * i / steps;
            int midY = startY + (endY - startY) * i / steps;
            User32.INSTANCE.SetCursorPos(midX, midY);
            try { Thread.sleep(20); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        User32.INSTANCE.mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0);
    }

    public static void mouseWheel(int delta) {
        User32.INSTANCE.mouse_event(0x0800, 0, 0, delta, 0);
    }
}