package org.example;

import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


@Plugin(type = Command.class, menuPath = "Plugins>Segmentation>MyPlugin")
public class MyPlugin<T extends RealType<T>> implements Command {

    @Parameter
    private Dataset currentData;

    @Parameter
    private UIService uiService;

    @Parameter
    private LogService log;

    private final JFrame frame = new JFrame("MyPluginWindow");

    private final String[] convergenceChoices = {"avgFracPerimeter (round)", "avgFracPerimeter (square)", "avgFracPerimeter (star)", "avgFracPerimeter (fusiform)"};

    static class ObjectData {
        private final Map<String, Object> parameters;

        public ObjectData(double alpha, double beta, double gamma, double kappa,
                          double mu, double perimeterFactor, int iterations,
                          int iterationsVF, double convergenceLimit,
                          String convergenceMetric) {
            this.parameters = new HashMap<>();
            parameters.put("alpha", alpha);
            parameters.put("beta", beta);
            parameters.put("gamma", gamma);
            parameters.put("kappa", kappa);
            parameters.put("mu", mu);
            parameters.put("perimeterFactor", perimeterFactor);
            parameters.put("iterations", iterations);
            parameters.put("iterationsVF", iterationsVF);
            parameters.put("convergenceLimit", convergenceLimit);
            parameters.put("convergenceMetric", convergenceMetric);
        }

        public Object getParameter(String key) {
            return parameters.get(key);
        }

        public void setParameter(String key, Object value) {
            parameters.put(key, value);
            System.out.println("Key: " + key + "is now " + value.toString());
        }

        @Override
        public String toString() {
            String resultingString = "";
            resultingString += "alpha: " + parameters.get("alpha") + '\n';
            resultingString += "beta: " + parameters.get("beta") + '\n';
            resultingString += "gamma: " + parameters.get("gamma") + '\n';
            resultingString += "kappa: " + parameters.get("kappa") + '\n';
            resultingString += "mu: " + parameters.get("mu") + '\n';
            resultingString += "perimeterFactor: " + parameters.get("perimeterFactor") + '\n';
            resultingString += "iterations: " + parameters.get("iterations") + '\n';
            resultingString += "iterationsVF: " + parameters.get("iterationsVF") + '\n';
            resultingString += "convergenceLimit: " + parameters.get("convergenceLimit") + '\n';
            resultingString += "convergenceMetric: " + parameters.get("convergenceMetric") + '\n';
            return resultingString;
        }

    }

    ObjectData roundObject = new ObjectData(1.0,
            0.001,
            0.5,
            1.0,
            0.05,
            1.0,
            5000,
            20,
            0.02,
            "avgFracPerimeter (round)");

    ObjectData squareObject = new ObjectData(1.0,
            0.002,
            0.5,
            1.0,
            0.05,
            1.0,
            5001,
            21,
            0.02,
            "avgFracPerimeter (square)");

    ObjectData starObject = new ObjectData(1.0,
            0.003,
            0.5,
            1.0,
            0.05,
            1.0,
            5002,
            22,
            0.02,
            "avgFracPerimeter (star)");

    ObjectData fusiformObject = new ObjectData(1.0,
            0.004,
            0.5,
            1.0,
            0.05,
            1.0,
            5003,
            23,
            0.02,
            "avgFracPerimeter (fusiform)");

    // Parameters for the execution of active contours
    ObjectData currentParameters = roundObject;

    @Override
    public void run() {

        SwingUtilities.invokeLater(this::initializeUI);

        final Img<T> image = (Img<T>) currentData.getImgPlus();

    }

    private void getROIs() {
        RoiManager rm = RoiManager.getInstance();
        rm.setVisible(true);
        try {
            Roi[] rois = rm.getRoisAsArray();
            int i = 0;
            for (Roi roi : rois) {
                log.info("Roi " + i + ": ");
                log.info("\tname: " + roi.getName());
                log.info("\tPoints: ");
                int j = 0;
                for (Point p : roi) {
                    log.info("\t\tPoint " + j + ": " + p.toString());
                    j++;
                }
                i++;
            }
        } catch (NullPointerException exception) {
            JOptionPane.showMessageDialog(frame, "There are no ROIs in the Roi Manager.\nThis plug-in requires at least one.", "ROI Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            rm.setVisible(false);
        }
    }

    private final ButtonGroup shapesButtonGroup = new ButtonGroup();

    private void addComponent(Container container, Component component, int gridx, int gridy, int gridwidth, int gridheight, int anchor, int fill, int insetsTop, int insetsLeft) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.gridwidth = gridwidth;
        gbc.gridheight = gridheight;
        gbc.anchor = anchor;
        gbc.fill = fill;
        gbc.insets = new Insets(insetsTop, insetsLeft, 10, 10);
        container.add(component, gbc);
    }

    private void initializeUI() {
        // This is the whole plugin window
        frame.setSize(300, 600);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setVisible(true);

        // This is the panel that contains the buttons with the object shapes
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setPreferredSize(new Dimension(300, 500));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Object Shape"));

        ImageIcon roundObjectIcon = new ImageIcon(getClass().getResource("/letter_a.png"));
        JButton roundObjectButton = new JButton(roundObjectIcon);
        roundObjectButton.setToolTipText("This is the tooltip for the Round Object button, this will display some useful info about this button.");
        JLabel roundObjectButtonLabel = new JLabel("Round Shape");
        roundObjectButton.setActionCommand("roundObjectSelected");

        ImageIcon squareObjectIcon = new ImageIcon(getClass().getResource("/letter_b.png"));
        JButton squareObjectButton = new JButton(squareObjectIcon);
        squareObjectButton.setToolTipText("This is the tooltip for the Square Object button, this will display some useful info about this button.");
        JLabel squareObjectButtonLabel = new JLabel("Square Shape");
        squareObjectButton.setActionCommand("squareObjectSelected");

        ImageIcon starObjectIcon = new ImageIcon(getClass().getResource("/letter_a.png"));
        JButton starObjectButton = new JButton(starObjectIcon);
        starObjectButton.setToolTipText("This is the tooltip for the Star Object button, this will display some useful info about this button.");
        JLabel starObjectButtonLabel = new JLabel("Star Shape");
        starObjectButton.setActionCommand("starObjectSelected");

        ImageIcon fusiformObjectIcon = new ImageIcon(getClass().getResource("/letter_b.png"));
        JButton fusiformObjectButton = new JButton(fusiformObjectIcon);
        fusiformObjectButton.setToolTipText("This is the tooltip for the Fusiform Object button, this will display some useful info about this button.");
        JLabel fusiformObjectButtonLabel = new JLabel("Fusiform Shape");
        fusiformObjectButton.setActionCommand("fusiformObjectSelected");

        // Add components to the panel
        //leftPanel.add(Box.createVerticalGlue());
        JButton[] shapeButtons = {roundObjectButton, squareObjectButton, starObjectButton, fusiformObjectButton};
        JLabel[] shapeLabels = {roundObjectButtonLabel, squareObjectButtonLabel, starObjectButtonLabel, fusiformObjectButtonLabel};


        for (JButton btn : shapeButtons) {
            shapesButtonGroup.add(btn);
        }

        for (int i = 0; i < shapeButtons.length; i++) {
            addComponent(leftPanel, shapeButtons[i], 0, i, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, 10, 10);
            addComponent(leftPanel, shapeLabels[i], 1, i, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, 10, 10);
        }

        ImageIcon advancedOptionsIcon = new ImageIcon(getClass().getResource("/letter_a.png"));
        JButton advancedOptionsButton = new JButton(advancedOptionsIcon);
        advancedOptionsButton.setToolTipText("This is the tooltip for the Advanced Options button, this will display some useful info about this button.");
        JLabel advancedOptionsButtonLabel = new JLabel("Advanced Options");
        advancedOptionsButton.setActionCommand("advancedOptionsSelected");

        addComponent(leftPanel, advancedOptionsButton, 0, shapeButtons.length, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, 10, 10);
        addComponent(leftPanel, advancedOptionsButtonLabel, 1, shapeButtons.length, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, 10, 10);


        // Create a panel with number inputs
        JPanel rightPanel = new JPanel();
        rightPanel.setPreferredSize(new Dimension(400, 500));
        rightPanel.setLayout(new GridBagLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Advanced Parameter Configuration"));

        String[] parameterNames = {"alpha", "beta", "gamma", "kappa", "mu", "perimeterFactor", "convergenceLimit"};
        String[] iterationsNames = {"iterations", "iterationsVF"};

        JLabel[] inputLabels = new JLabel[parameterNames.length + iterationsNames.length];
        JTextField[] inputFields = new JTextField[parameterNames.length + iterationsNames.length];

        for (int i = 0; i < parameterNames.length; i++) {
            String parameterName = parameterNames[i];
            inputLabels[i] = new JLabel(parameterName);
            String parameterValue = String.valueOf(currentParameters.getParameter(parameterNames[i]));
            inputFields[i] = new JTextField(parameterValue);
            int inputLabelsIndex = i;
            int parameterNamesIndex = i;
            inputFields[i].addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    try {
                        double value = Double.parseDouble(inputFields[inputLabelsIndex].getText());
                        currentParameters.setParameter(parameterName, value);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(null, "Please enter a number in the " + parameterNames[parameterNamesIndex] + " field.");
                        inputFields[inputLabelsIndex].setText("");
                    }
                }
            });
        }

        for (int i = 0; i < iterationsNames.length; i++) {
            String parameterName = iterationsNames[i];
            inputLabels[i + parameterNames.length] = new JLabel(parameterName);
            String parameterValue = String.valueOf(currentParameters.getParameter(iterationsNames[i]));
            inputFields[i + parameterNames.length] = new JTextField(parameterValue);
            int inputLabelsIndex = i + parameterNames.length;
            int iterationsNamesIndex = i;
            inputFields[i + parameterNames.length].addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    try {
                        int value = Integer.parseInt(inputFields[inputLabelsIndex].getText());
                        currentParameters.setParameter(parameterName, value);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(null, "Please enter an integer in the " + iterationsNames[iterationsNamesIndex] + " field.");
                        inputFields[inputLabelsIndex].setText("");
                    }
                }
            });
        }

        JLabel convergenceSelectorLabel = new JLabel("convergenceMetric");
        JComboBox<String> convergenceSelector = new JComboBox<>(convergenceChoices);
        convergenceSelector.setSelectedItem(String.valueOf(currentParameters.getParameter("convergenceMetric")));

        for (int i = 0; i < inputFields.length; i++) {
            addComponent(rightPanel, inputLabels[i], 0, i, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, 10, 10);
            addComponent(rightPanel, inputFields[i], 1, i, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, 10, 10);
        }

        addComponent(rightPanel, convergenceSelectorLabel, 0, inputFields.length, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, 10, 10);
        addComponent(rightPanel, convergenceSelector, 1, inputFields.length, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, 10, 10);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        JButton bottomButton = new JButton("Run Algorithm");
        bottomButton.setPreferredSize(new Dimension(700, 40));
        bottomButton.setMargin(new Insets(10, 50, 10, 50)); // Adjust the margins as needed
        bottomButton.addActionListener(e -> {
            log.info(currentParameters.toString());
            getROIs();
        });
        bottomPanel.add(bottomButton, BorderLayout.CENTER);

        frame.add(leftPanel, BorderLayout.WEST);
        frame.add(rightPanel, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        // Hide the number panel initially
        rightPanel.setVisible(false);


        // Button action listener
          ActionListener actionListener = e -> {
              String command = e.getActionCommand();
              shapesButtonGroup.clearSelection();
              Enumeration<AbstractButton> objectButtons = shapesButtonGroup.getElements();
              while (objectButtons.hasMoreElements()) {
                  AbstractButton objectButton = objectButtons.nextElement();
                  objectButton.setBackground(UIManager.getColor("Button.background"));
              }
              switch (command) {
                  case "roundObjectSelected":
                      roundObjectButton.setSelected(true);
                      roundObjectButton.setBackground(Color.YELLOW);
                      currentParameters = roundObject;
                      log.info("Round Object Selected");
                      break;
                  case "squareObjectSelected":
                      squareObjectButton.setSelected(true);
                      squareObjectButton.setBackground(Color.YELLOW);
                      currentParameters = squareObject;
                      log.info("Square Object Selected");
                      break;
                  case "starObjectSelected":
                      starObjectButton.setSelected(true);
                      starObjectButton.setBackground(Color.YELLOW);
                      currentParameters = starObject;
                      log.info("Star Object Selected");
                      break;
                  case "fusiformObjectSelected":
                      fusiformObjectButton.setSelected(true);
                      fusiformObjectButton.setBackground(Color.YELLOW);
                      currentParameters = fusiformObject;
                      log.info("Fusiform Object Selected");
                      break;
              }
//              String[] parameterNames = {"alpha", "beta", "gamma", "kappa", "mu", "perimeterFactor", "convergenceLimit"};
//              String[] iterations = {"iterations", "iterationsVF"};
              for (int i=0; i < parameterNames.length; i++) {
                  inputFields[i].setText(String.valueOf(currentParameters.getParameter(parameterNames[i])));
              }
              for (int i=0; i < iterationsNames.length; i++) {
                  inputFields[parameterNames.length + i].setText(String.valueOf(currentParameters.getParameter(iterationsNames[i])));
              }
              convergenceSelector.setSelectedItem(String.valueOf(currentParameters.getParameter("convergenceMetric")));
          };

        // Add action listener to buttons
        roundObjectButton.addActionListener(actionListener);
        squareObjectButton.addActionListener(actionListener);
        starObjectButton.addActionListener(actionListener);
        fusiformObjectButton.addActionListener(actionListener);
        advancedOptionsButton.addActionListener(e -> {
            log.info("Advanced Options Selected");
            log.info("Panel previous state: " + rightPanel.isVisible());
            rightPanel.setVisible(!rightPanel.isVisible());
            log.info("Panel current state: " + rightPanel.isVisible());
            if (rightPanel.isVisible()) {
                frame.setSize(700, 600);
            } else {
                frame.setSize(300, 600);
            }
            frame.revalidate();
            frame.repaint();
        });
    }



    public static void main(final String... args) throws Exception {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
    }

}