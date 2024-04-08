package cl.scian;

import javax.swing.*;

public class MenuBar extends JMenuBar{

    public MenuBar() {
        super();

        JMenu helpMenu = new JMenu("Help");

        JMenuItem quickstart = new JMenuItem("Quickstart Guide");

        quickstart.addActionListener(e -> JOptionPane.showMessageDialog(null, "This is the Quickstart Guide"));

        JMenuItem userManual = new JMenuItem("User Manual");

        userManual.addActionListener(e -> JOptionPane.showMessageDialog(null, "This is the User Manual"));

        helpMenu.add(quickstart);
        helpMenu.add(userManual);

        this.add(helpMenu);

    }

}
