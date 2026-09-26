package com.lulu.tool;

import com.lulu.config.Config;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinDef.RECT;

public class MouseTracker {
    public static void main(String[] args) throws InterruptedException {
        System.out.println(">>> 连续探测器已启动！");
        System.out.println(">>> 规则：连续按两次 F 键记录一组 ROI (左上角 -> 右下角)");
        System.out.println(">>> 按 ESC 键退出程序。");

        int[] x = new int[2], y = new int[2];
        int count = 0;
        boolean lastFState = false;

        while (true) {
            // 0x46: F键, 0x1B: ESC键
            short fState = User32.INSTANCE.GetAsyncKeyState(0x46);
            short escState = User32.INSTANCE.GetAsyncKeyState(0x1B);

            if ((escState & 0x8000) != 0) {
                System.out.println(">>> 程序已退出。");
                break;
            }

            boolean isPressed = (fState != 0);

            if (isPressed && !lastFState) {
                HWND hwnd = User32.INSTANCE.FindWindow(null, Config.Global.APP_TITLE);
                if (hwnd != null) {
                    RECT rect = new RECT();
                    User32.INSTANCE.GetWindowRect(hwnd, rect);
                    POINT mouse = new POINT();
                    User32.INSTANCE.GetCursorPos(mouse);

                    x[count] = mouse.x - rect.left;
                    y[count] = mouse.y - rect.top;
                    System.out.println("记录点 " + (count + 1) + ": [" + x[count] + ", " + y[count] + "]");
                    count++;

                    // 满两点，计算并重置
                    if (count == 2) {
                        System.out.println("\n--- ROI 结果 ---");
                        System.out.printf("ROI_X = %d; ROI_Y = %d; ROI_W = %d; ROI_H = %d;\n",
                                x[0], y[0], x[1] - x[0], y[1] - y[0]);
                        System.out.println("----------------\n请继续记录下一组，或按 ESC 退出。\n");
                        count = 0; // 重置计数器，开始下一轮记录
                    }
                }
            }
            lastFState = isPressed;
            Thread.sleep(50);
        }
    }
}