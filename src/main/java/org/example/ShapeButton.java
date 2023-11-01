package org.example;

import javax.swing.*;
import java.util.Objects;

public class ShapeButton extends JButton {
    public ShapeButton(String iconPath){
        super(new ImageIcon(Objects.requireNonNull(ShapeButton.class.getResource(iconPath))));
    }
}
