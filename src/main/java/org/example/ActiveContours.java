package org.example;

import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static org.example.CamelCaseConverter.toCapitalized;


@Plugin(type = Command.class, menuPath = "Plugins>Segmentation>Active Contours")
public class ActiveContours<T extends RealType<T>> implements Command {

    // DEBUG MODES
    // 1 = ERRORS
    // 2 = WARNINGS
    // 3 = INFO
    private final int DEBUG = 0;

    @Parameter
    private Dataset currentData;

    private Img<T> image;

    @Parameter
    private UIService uiService;

    @Parameter
    private LogService log;

    private final JFrame frame = new JFrame("Active Contours");

    private final String[] convergenceChoices = {"avgFracPerimeter"};

    String idlParametersTemporalFilename = "C:/Users/gonza/Desktop/idl_params.dat";
    String idlRoisTemporalFilename = "C:/Users/gonza/Desktop/idl_rois.dat";
    String idlImageTemporalFilename = "C:/Users/gonza/Desktop/idl_tmp_img.dat";
    String idlAdjustedRoisTemporalFilename = "C:/Users/gonza/Desktop/idl_adjusted_rois.dat";

//    String idlParametersTemporalFilename = "C:/Users/Gonzalo/Desktop/tmp/idl_params.dat";
//    String idlRoisTemporalFilename = "C:/Users/Gonzalo/Desktop/tmp/idl_rois.dat";
//    String idlImageTemporalFilename = "C:/Users/Gonzalo/Desktop/tmp/idl_tmp_img.dat";
//    String idlAdjustedRoisTemporalFilename = "C:/Users/Gonzalo/Desktop/tmp/idl_adjusted_rois.dat";

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

    ObjectData roundObject = new ObjectData(0.5, 0.7, 0.6, 0.8, 0.5, 1.0, 100, 20, 0.02, "avgFracPerimeter");

    ObjectData squareObject = new ObjectData(0.5, 0.6, 0.5, 0.7, 0.5, 1.0, 100, 20, 0.02, "avgFracPerimeter");

    ObjectData starObject = new ObjectData(0.6, 1.0, 0.5, 2.5, 2.5, 1.0, 350, 20, 0.02, "avgFracPerimeter");

    ObjectData fusiformObject = new ObjectData(0.5, 0.6, 0.5, 0.7, 0.5, 1.0, 100, 20, 0.02, "avgFracPerimeter");

    // Parameters for the execution of active contours
    ObjectData currentParameters = roundObject;

    @Override
    public void run() {

        SwingUtilities.invokeLater(this::initializeUI);

        image = (Img<T>) currentData.getImgPlus();

    }

    public void callToIDL() {
        System.out.println("Entered Java script");

        String idl_executable = "C:/Program Files/ITT/IDL71/bin/bin.x86_64/idl.exe";
        String idl_vm = "C:/Program Files/ITT/IDL71/bin/bin.x86_64/idlrt.exe";
        String idl_script = "C:/Users/gonza/Desktop/activecontours.sav";
        // String idl_script = "C:/Users/Gonzalo/Desktop/activecontours.sav";

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(idl_vm, "-vm=" + idl_script);
            Process process = processBuilder.start();
            System.out.println("Trying to execute the script.");

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("IDL script executed successfully.");
            } else {
                System.out.println("Error executing IDL script. Exit code: " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            System.out.println("Error executing IDL script: " + e);
        }
    }

    public void newfile(String filename) {
        try {
            FileWriter writer = new FileWriter(filename);
            writer.write("");
            writer.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
    public void writeToFile(String filename, String text) {
        try {
            FileWriter writer = new FileWriter(filename, true);
            writer.write(text);
            writer.write(System.lineSeparator());
            writer.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public String convertArrayListToString(ArrayList<Double> list) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Double d : list) {
                sb.append(d).append(" ");
            }

            sb.deleteCharAt(sb.length() - 1);

            return sb.toString();
        } catch (StringIndexOutOfBoundsException e) {
            return "";
        }
    }

    private void getROIs() {
        try {
            RoiManager rm = RoiManager.getInstance();
            rm.setVisible(true);
            newfile(idlRoisTemporalFilename);
            Roi[] rois = rm.getRoisAsArray();
            writeToFile(idlRoisTemporalFilename, String.valueOf(rois.length));
            ArrayList<Double> roisSize = new ArrayList<>();
            for (Roi roi : rois) {
                int j = 0;
                for (Point ignored : roi) {
                    j++;
                }
                roisSize.add((double) j);
            }
            writeToFile(idlRoisTemporalFilename, convertArrayListToString(roisSize));
            int i = 0;
            for (Roi roi : rois) {
                if (DEBUG >= 3) {
                    log.info("Roi " + i + ": ");
                    log.info("\tname: " + roi.getName());
                    log.info("\tPoints: ");
                }
                int j = 0;
                ArrayList<Double> xCoords = new ArrayList<>();
                ArrayList<Double> yCoords = new ArrayList<>();
                for (Point p : roi) {
                    if (DEBUG >= 3) log.info("\t\tPoint " + j + ": " + p.toString());
                    xCoords.add(p.getX());
                    yCoords.add(p.getY());
                    j++;
                }
                writeToFile(idlRoisTemporalFilename, convertArrayListToString(xCoords));
                writeToFile(idlRoisTemporalFilename, convertArrayListToString(yCoords));
                i++;
            }
        } catch (NullPointerException exception) {
            JOptionPane.showMessageDialog(frame, "There are no ROIs in the Roi Manager.\nThis plug-in requires at least one.", "ROI Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setROIs() {
        try (BufferedReader br = new BufferedReader(new FileReader(idlAdjustedRoisTemporalFilename))) {
            String line;
            RoiManager roiManager = RoiManager.getInstance();
            if (roiManager == null) {
                roiManager = new RoiManager();
            }

            roiManager.runCommand("reset");

            ArrayList<String[]> xCoordinates = new ArrayList<>();
            ArrayList<String[]> yCoordinates = new ArrayList<>();

            while ((line = br.readLine()) != null) {
                String[] x = line.split(" ");
                xCoordinates.add(x);
                if ((line = br.readLine()) != null) {
                    String[] y = line.split(" ");
                    yCoordinates.add(y);
                } else {
                    break;
                }
            }

            for (int i = 0; i < xCoordinates.size(); i++){
                float[] x = new float[xCoordinates.get(i).length];
                float[] y = new float[yCoordinates.get(i).length];
                for (int j = 0; j < xCoordinates.get(i).length; j++) {
                    x[j] = Float.parseFloat(xCoordinates.get(i)[j]);
                    y[j] = Float.parseFloat(yCoordinates.get(i)[j]);
                }
                roiManager.addRoi(new ij.gui.PolygonRoi(x, y, xCoordinates.get(i).length, Roi.POLYGON));
            }

            roiManager.runCommand("Show All");

        } catch (IOException e) {
            e.printStackTrace();
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

        int PANEL_MARGIN = 8;

        int LEFT_PANEL_WIDTH = 300;
        int LEFT_PANEL_HEIGHT = 550;

        int RIGHT_PANEL_WIDTH = 400;
        int RIGHT_PANEL_HEIGHT = LEFT_PANEL_HEIGHT;

        int BASE_BOTTOM_PANEL_WIDTH = LEFT_PANEL_WIDTH;
        int BASE_BOTTOM_PANEL_HEIGHT = 30;
        int EXTENDED_BOTTOM_PANEL_WIDTH = LEFT_PANEL_WIDTH + RIGHT_PANEL_WIDTH;
        int EXTENDED_BOTTOM_PANEL_HEIGHT = BASE_BOTTOM_PANEL_HEIGHT;

        int BASE_FRAME_WIDTH = LEFT_PANEL_WIDTH;
        int BASE_FRAME_HEIGHT = LEFT_PANEL_HEIGHT + BASE_BOTTOM_PANEL_HEIGHT;
        int EXTENDED_FRAME_WIDTH = LEFT_PANEL_WIDTH + RIGHT_PANEL_WIDTH;
        int EXTENDED_FRAME_HEIGHT = BASE_FRAME_HEIGHT;


        MenuBar menuBar = new MenuBar();
        frame.setJMenuBar(menuBar);
        // This is the whole plugin window
        frame.setSize(BASE_FRAME_WIDTH, BASE_FRAME_HEIGHT);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setVisible(true);

        // This is the panel that contains the buttons with the object shapes
        JPanel outerLeftPanel = new JPanel(new BorderLayout());
        outerLeftPanel.setBorder(new EmptyBorder(PANEL_MARGIN, PANEL_MARGIN, PANEL_MARGIN/2, PANEL_MARGIN));
        outerLeftPanel.setPreferredSize(new Dimension(LEFT_PANEL_WIDTH, LEFT_PANEL_HEIGHT));
        JPanel innerLeftPanel = new JPanel(new GridBagLayout());
        outerLeftPanel.setPreferredSize(new Dimension(LEFT_PANEL_WIDTH - 2*PANEL_MARGIN, LEFT_PANEL_HEIGHT - 2*PANEL_MARGIN));
        innerLeftPanel.setBorder(BorderFactory.createTitledBorder("Object Shape"));

        ShapeButton roundObjectButton = new ShapeButton("/round.png");
        roundObjectButton.setToolTipText("This is the tooltip for the Round Object button, this will display some useful info about this button.");
        JLabel roundObjectButtonLabel = new JLabel("Round Shape");
        roundObjectButton.setActionCommand("roundObjectSelected");

        ShapeButton squareObjectButton = new ShapeButton("/square.png");
        squareObjectButton.setToolTipText("This is the tooltip for the Square Object button, this will display some useful info about this button.");
        JLabel squareObjectButtonLabel = new JLabel("Square Shape");
        squareObjectButton.setActionCommand("squareObjectSelected");

        ShapeButton starObjectButton = new ShapeButton("/star.png");
        starObjectButton.setToolTipText("This is the tooltip for the Star Object button, this will display some useful info about this button.");
        JLabel starObjectButtonLabel = new JLabel("Star Shape");
        starObjectButton.setActionCommand("starObjectSelected");

        ShapeButton fusiformObjectButton = new ShapeButton("/fusiform.png");
        fusiformObjectButton.setToolTipText("This is the tooltip for the Fusiform Object button, this will display some useful info about this button.");
        JLabel fusiformObjectButtonLabel = new JLabel("Fusiform Shape");
        fusiformObjectButton.setActionCommand("fusiformObjectSelected");

        // Add components to the panel
        JButton[] shapeButtons = {roundObjectButton, squareObjectButton, starObjectButton, fusiformObjectButton};
        JLabel[] shapeLabels = {roundObjectButtonLabel, squareObjectButtonLabel, starObjectButtonLabel, fusiformObjectButtonLabel};


        for (JButton btn : shapeButtons) {
            shapesButtonGroup.add(btn);
        }
        roundObjectButton.setSelected(true);
        roundObjectButton.setBackground(Color.YELLOW);

        for (int i = 0; i < shapeButtons.length; i++) {
            addComponent(innerLeftPanel, shapeButtons[i], 0, i, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, 10, 10);
            addComponent(innerLeftPanel, shapeLabels[i], 1, i, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, 10, 10);
        }

        ShapeButton advancedOptionsButton = new ShapeButton("/blank.png");
        advancedOptionsButton.setToolTipText("This is the tooltip for the Advanced Options button, this will display some useful info about this button.");
        JLabel advancedOptionsButtonLabel = new JLabel("Advanced Options");
        advancedOptionsButton.setActionCommand("advancedOptionsSelected");

        addComponent(innerLeftPanel, advancedOptionsButton, 0, shapeButtons.length, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, 10, 10);
        addComponent(innerLeftPanel, advancedOptionsButtonLabel, 1, shapeButtons.length, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, 10, 10);


        // This is the panel that contains the number inputs
        JPanel outerRightPanel = new JPanel(new BorderLayout());
        outerRightPanel.setBorder(new EmptyBorder(PANEL_MARGIN, PANEL_MARGIN/2, PANEL_MARGIN/2, PANEL_MARGIN));
        outerRightPanel.setPreferredSize(new Dimension(RIGHT_PANEL_WIDTH, RIGHT_PANEL_HEIGHT));
        JPanel innerRightPanel = new JPanel(new GridBagLayout());
        innerRightPanel.setPreferredSize(new Dimension(RIGHT_PANEL_WIDTH - 2*PANEL_MARGIN, RIGHT_PANEL_WIDTH - 2*PANEL_MARGIN));
        innerRightPanel.setBorder(BorderFactory.createTitledBorder("Advanced Parameter Configuration"));

        String[] parameterNames = {"alpha", "beta", "gamma", "kappa", "mu", "perimeterFactor", "convergenceLimit"};
        String[] iterationsNames = {"iterations", "iterationsVF"};

        JLabel[] inputLabels = new JLabel[parameterNames.length + iterationsNames.length];
        JTextField[] inputFields = new JTextField[parameterNames.length + iterationsNames.length];

        for (int i = 0; i < parameterNames.length; i++) {
            String parameterName = parameterNames[i];
            inputLabels[i] = new JLabel(toCapitalized(parameterName));
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
            inputLabels[i + parameterNames.length] = new JLabel(toCapitalized(parameterName));
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

        JLabel convergenceSelectorLabel = new JLabel("Convergence Metric");
        JComboBox<String> convergenceSelector = new JComboBox<>(convergenceChoices);
        convergenceSelector.setSelectedItem(String.valueOf(currentParameters.getParameter("convergenceMetric")));

        for (int i = 0; i < inputFields.length; i++) {
            addComponent(innerRightPanel, inputLabels[i], 0, i, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, 10, 10);
            addComponent(innerRightPanel, inputFields[i], 1, i, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, 10, 10);
        }

        addComponent(innerRightPanel, convergenceSelectorLabel, 0, inputFields.length, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, 10, 10);
        addComponent(innerRightPanel, convergenceSelector, 1, inputFields.length, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, 10, 10);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.setBorder(new EmptyBorder(PANEL_MARGIN/2, PANEL_MARGIN, PANEL_MARGIN, PANEL_MARGIN));
        JButton bottomButton = new JButton("Run Algorithm");
        bottomButton.setPreferredSize(new Dimension(BASE_BOTTOM_PANEL_WIDTH, BASE_BOTTOM_PANEL_HEIGHT));
        bottomButton.addActionListener(e -> {

            if (DEBUG >= 3) log.info(currentParameters.toString());

            // Create a non-modal dialog
            JDialog loadingDialog = new JDialog(frame, "Loading...", false);
            loadingDialog.setModalityType(Dialog.ModalityType.MODELESS);
            loadingDialog.setSize(200, 100);

            // Create a label with the loading message
            JLabel loadingLabel = new JLabel("Loading results...");
            loadingLabel.setHorizontalAlignment(JLabel.CENTER);
            loadingLabel.setVerticalAlignment(JLabel.CENTER);
            loadingDialog.add(loadingLabel, BorderLayout.CENTER);

            JPanel loadingPanel = new JPanel();
            loadingPanel.add(loadingLabel);
            loadingDialog.getContentPane().add(loadingPanel);

            // Set the location of the dialog relative to the frame
            loadingDialog.setLocationRelativeTo(frame);

            // Show the dialog
            loadingDialog.setVisible(true);

            newfile(idlParametersTemporalFilename);
            writeToFile(idlParametersTemporalFilename, currentParameters.getParameter("alpha").toString());
            writeToFile(idlParametersTemporalFilename, currentParameters.getParameter("beta").toString());
            writeToFile(idlParametersTemporalFilename, currentParameters.getParameter("gamma").toString());
            writeToFile(idlParametersTemporalFilename, currentParameters.getParameter("kappa").toString());
            writeToFile(idlParametersTemporalFilename, currentParameters.getParameter("mu").toString());
            writeToFile(idlParametersTemporalFilename, currentParameters.getParameter("iterations").toString());
            writeToFile(idlParametersTemporalFilename, currentParameters.getParameter("iterationsVF").toString());

            newfile(idlImageTemporalFilename);

            int width = (int) image.dimension(0);
            int height = (int) image.dimension(1);

            writeToFile(idlImageTemporalFilename, String.valueOf(width));
            writeToFile(idlImageTemporalFilename, String.valueOf(height));

            for (int y = 0; y < height; y++) {
                ArrayList<Double> row = new ArrayList<>();
                for (int x = 0; x < width; x++) {
                    RandomAccess<? extends RealType<?>> randomAccess = image.randomAccess();
                    randomAccess.setPosition(x, 0);
                    randomAccess.setPosition(y, 1);
                    double value = randomAccess.get().getRealDouble();
                    row.add(value);
                }
                writeToFile(idlImageTemporalFilename, convertArrayListToString(row));
            }

            getROIs();

            callToIDL();

            setROIs();

            loadingDialog.dispose();

        });
        bottomPanel.add(bottomButton, BorderLayout.CENTER);

        outerLeftPanel.add(innerLeftPanel, BorderLayout.CENTER);
        frame.add(outerLeftPanel, BorderLayout.WEST);
        outerRightPanel.add(innerRightPanel, BorderLayout.CENTER);
        frame.add(outerRightPanel, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        // Hide the number panel initially
        outerRightPanel.setVisible(false);


        // Button action listener
          ActionListener actionListener = e -> {
              String command = e.getActionCommand();
              shapesButtonGroup.clearSelection();
              Enumeration<AbstractButton> objectButtons = shapesButtonGroup.getElements();
              while (objectButtons.hasMoreElements()) {
                  AbstractButton objectButton = objectButtons.nextElement();
                  objectButton.setSelected(false);
                  objectButton.setBackground(Color.WHITE);
              }
              switch (command) {
                  case "roundObjectSelected":
                      roundObjectButton.setSelected(true);
                      roundObjectButton.setBackground(Color.YELLOW);
                      currentParameters = roundObject;
                      if (DEBUG >= 3) log.info("Round Object Selected");
                      break;
                  case "squareObjectSelected":
                      squareObjectButton.setSelected(true);
                      squareObjectButton.setBackground(Color.YELLOW);
                      currentParameters = squareObject;
                      if (DEBUG >= 3) log.info("Square Object Selected");
                      break;
                  case "starObjectSelected":
                      starObjectButton.setSelected(true);
                      starObjectButton.setBackground(Color.YELLOW);
                      currentParameters = starObject;
                      if (DEBUG >= 3) log.info("Star Object Selected");
                      break;
                  case "fusiformObjectSelected":
                      fusiformObjectButton.setSelected(true);
                      fusiformObjectButton.setBackground(Color.YELLOW);
                      currentParameters = fusiformObject;
                      if (DEBUG >= 3) log.info("Fusiform Object Selected");
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
            if (DEBUG >= 3) {
                log.info("Advanced Options Selected");
                log.info("Panel previous state: " + outerRightPanel.isVisible());
            }
            outerRightPanel.setVisible(!outerRightPanel.isVisible());
            if (DEBUG >= 3) log.info("Panel current state: " + outerRightPanel.isVisible());
            if (outerRightPanel.isVisible()) {
                outerLeftPanel.setBorder(new EmptyBorder(PANEL_MARGIN, PANEL_MARGIN, PANEL_MARGIN/2, PANEL_MARGIN/2));
                frame.setSize(EXTENDED_FRAME_WIDTH, EXTENDED_FRAME_HEIGHT);
            } else {
                outerLeftPanel.setBorder(new EmptyBorder(PANEL_MARGIN, PANEL_MARGIN, PANEL_MARGIN/2, PANEL_MARGIN));
                frame.setSize(BASE_FRAME_WIDTH, BASE_FRAME_HEIGHT);
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