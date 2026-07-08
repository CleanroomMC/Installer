/*
 * Installer
 * Copyright (c) 2016-2018.
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.minecraftforge.installer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Locale;
import java.util.concurrent.Callable;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public final class Win11MicaEffect {
    private static final Color TRANSPARENT = new Color(220, 220, 220, 0);
    private static final Color WINDOW_BACKGROUND = new Color(220, 220, 220, 1);

    private Win11MicaEffect() {}

    public static void prepare(Component component) {
        if (isUnsupportedPlatform())
            return;

        if (component instanceof JPanel || component instanceof JOptionPane || component instanceof JRadioButton) {
            JComponent jComponent = (JComponent) component;
            jComponent.setOpaque(false);
            jComponent.setBackground(TRANSPARENT);
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                prepare(child);
            }
        }
    }

    public static void install(JDialog dialog) throws Exception {
        if (isUnsupportedPlatform())
            return;

        dialog.getRootPane().setOpaque(false);
        dialog.getLayeredPane().setOpaque(false);
        if (dialog.getContentPane() instanceof JComponent contentPane) {
            contentPane.setOpaque(false);
            contentPane.setBackground(TRANSPARENT);
        }
        prepare(dialog.getRootPane());

        dialog.setBackground(WINDOW_BACKGROUND);

        Callable<Void> apply = () -> {
            try {
                applyTo(dialog);
            } catch (Throwable t) {
                if (t instanceof Exception e) throw e;
                else throw new RuntimeException(t);
            }
            dialog.invalidate();
            dialog.validate();
            dialog.repaint();
            dialog.getRootPane().repaint();
            return null;
        };

        if (dialog.isShowing()) {
            apply.call();
            return;
        }

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                dialog.removeWindowListener(this);
                try {
                    apply.call();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    private static void applyTo(Dialog dialog) throws Throwable {
        if (!dialog.isDisplayable())
            return;

        var linker = Linker.nativeLinker();
        try (Arena arena = Arena.ofConfined()) {
            SymbolLookup user32 = SymbolLookup.libraryLookup("user32", arena);
            SymbolLookup dwmapi = SymbolLookup.libraryLookup("dwmapi", arena);

            MemorySegment hwnd = findDialogWindow(dialog, arena, user32);
            if (MemorySegment.NULL.equals(hwnd))
                return;

            MethodHandle setWindowPos = linker.downcallHandle(
                user32.find("SetWindowPos").orElseThrow(),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT
                )
            );
            MethodHandle dwmSetWindowAttribute = linker.downcallHandle(
                dwmapi.find("DwmSetWindowAttribute").orElseThrow(),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_INT
                )
            );
            MethodHandle dwmExtendFrameIntoClientArea = linker.downcallHandle(
                dwmapi.find("DwmExtendFrameIntoClientArea").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            );

            promoteToTaskbarWindow(hwnd, user32, setWindowPos);

            MemorySegment cornerPreference = arena.allocate(ValueLayout.JAVA_INT);
            cornerPreference.set(ValueLayout.JAVA_INT, 0, 2);
            int DWMWA_WINDOW_CORNER_PREFERENCE = 33;
            int _ = (int) dwmSetWindowAttribute.invokeExact(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, cornerPreference, Integer.BYTES);

            MemorySegment backdropType = arena.allocate(ValueLayout.JAVA_INT);
            int backdropTypePreference = Integer.getInteger("forgeinstaller.backdroptype", 2);
            backdropType.set(ValueLayout.JAVA_INT, 0, backdropTypePreference);
            int DWMWA_SYSTEMBACKDROP_TYPE = 38;
            int hresult = (int) dwmSetWindowAttribute.invokeExact(hwnd, DWMWA_SYSTEMBACKDROP_TYPE, backdropType, Integer.BYTES);
            if (hresult != 0)
                throw new RuntimeException("DwmSetWindowAttribute failed with HRESULT 0x" + Integer.toHexString(hresult));

            MemorySegment margins = arena.allocate(4L * Integer.BYTES, Integer.BYTES);
            margins.set(ValueLayout.JAVA_INT, 0L, -1);
            margins.set(ValueLayout.JAVA_INT, Integer.BYTES, -1);
            margins.set(ValueLayout.JAVA_INT, 2L * Integer.BYTES, -1);
            margins.set(ValueLayout.JAVA_INT, 3L * Integer.BYTES, -1);
            int _2 = (int) dwmExtendFrameIntoClientArea.invokeExact(hwnd, margins);

            refreshWindowFrame(hwnd, setWindowPos);
        }
    }

    private static MemorySegment findDialogWindow(Dialog dialog, Arena arena, SymbolLookup user32) throws Throwable {
        var linker = Linker.nativeLinker();
        MethodHandle findWindowW = linker.downcallHandle(
                user32.find("FindWindowW").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );

        String title = dialog.getTitle();
        if (title != null && !title.isBlank()) {
            MemorySegment windowTitle = arena.allocateFrom(ValueLayout.JAVA_CHAR, (title + '\0').toCharArray());
            MemorySegment hwnd = (MemorySegment) findWindowW.invokeExact(MemorySegment.NULL, windowTitle);
            if (!MemorySegment.NULL.equals(hwnd))
                return hwnd;
        }

        MethodHandle getForegroundWindow = linker.downcallHandle(
                user32.find("GetForegroundWindow").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.ADDRESS)
        );

        return (MemorySegment) getForegroundWindow.invokeExact();
    }

    private static void promoteToTaskbarWindow(MemorySegment hwnd, SymbolLookup user32, MethodHandle setWindowPos) throws Throwable {
        var linker = Linker.nativeLinker();
        MethodHandle getWindowLongW = linker.downcallHandle(
                user32.find("GetWindowLongW").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
        );
        MethodHandle setWindowLongW = linker.downcallHandle(
                user32.find("SetWindowLongW").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)
        );

        int WS_EX_TOOLWINDOW = 0x00000080;
        int WS_EX_APPWINDOW = 0x00040000;
        int GWL_EXSTYLE = -20;
        int exStyle = (int) getWindowLongW.invokeExact(hwnd, GWL_EXSTYLE);
        int newExStyle = (exStyle | WS_EX_APPWINDOW) & ~WS_EX_TOOLWINDOW;
        if (newExStyle == exStyle)
            return;

        int _ = (int) setWindowLongW.invokeExact(hwnd, GWL_EXSTYLE, newExStyle);
        refreshWindowFrame(hwnd, setWindowPos);
    }

    private static void refreshWindowFrame(MemorySegment hwnd, MethodHandle setWindowPos) throws Throwable {
        int flags = 0x0001 | 0x0002 | 0x0004 | 0x0010 | 0x0020;
        int _ = (int) setWindowPos.invokeExact(hwnd, MemorySegment.NULL, 0, 0, 0, 0, flags);
    }

    public static boolean isSupported() {
        return !isUnsupportedPlatform();
    }

    private static boolean isUnsupportedPlatform() {
        final class LazyInit {
            private LazyInit() {}
            private static final boolean IS_UNSUPPORTED;
            static {
                boolean useMica = true;
                boolean forceDisableMica = false;
                boolean isWin11 = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("windows 11");
                if (isWin11) {
                    forceDisableMica = !Boolean.parseBoolean(System.getProperty("forgeinstaller.usewin11mica", "true"));
                    if (forceDisableMica)
                        useMica = false;
                } else {
                    useMica = false;
                }
                IS_UNSUPPORTED = !useMica;
            }
        }
        return LazyInit.IS_UNSUPPORTED;
    }
}
