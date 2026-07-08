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

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;

public class SwingUtil {
    public static JButton createLogButton() {
        JButton button = new JButton("Open log");
        button.addActionListener(ev -> {
            File file = new File("installer.log");
            try {
                if (file.exists())
                    Desktop.getDesktop().open(file);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        return button;
    }

    static void applyGlobalFont(String fontFamily) {
        UIDefaults defaults = UIManager.getDefaults();
        Enumeration<Object> keys = defaults.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = defaults.get(key);
            if (value instanceof FontUIResource) {
                FontUIResource font = (FontUIResource) value;
                defaults.put(key, new FontUIResource(fontFamily, font.getStyle(), font.getSize()));
            }
        }
    }
}
