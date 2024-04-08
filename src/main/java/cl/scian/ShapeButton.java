package cl.scian;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class ShapeButton extends JButton {
    public ShapeButton(String iconPath){
        super();
        this.setPreferredSize(new Dimension(64, 64));
        ImageIcon icon = new ImageIcon(Objects.requireNonNull(ShapeButton.class.getResource(iconPath)));
        Image image = icon.getImage();
        Image scaledImage = image.getScaledInstance(48, 48, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);
        this.setIcon(scaledIcon);
    }
}
