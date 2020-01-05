package ar;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Controller implements Initializable {


    private List<String> attNames;
    private String[][] data;

    private int supportThresh;
    private double confThresh;

    private List<Integer> stableAttributes;
    private int decisionAttribute;

    private String decisionValueFrom;
    private String decisionValueTo;

    private List<Rule> certainRules = null;
    private List<Rule> possibleRules = null;

    private List<Rule> allRules = new ArrayList<>();

    private List<ActionRule> actionRules = new ArrayList<>();

    private PrintStream outFile;

    private boolean inputLoaded = false;


    @FXML
    TextField inputDataFile;
    @FXML
    TextField inputNameFile;
    @FXML
    TextField outputDataFile;

    @FXML
    ComboBox<String> delimiterBox;

    @FXML
    TextField supportTextField;
    @FXML
    TextField confTextField;

    @FXML
    ComboBox<String> decAttributeComboBox;
    @FXML
    TextField decValueFromTextField;
    @FXML
    TextField decValueToTextField;

    @FXML
    ListView<String> stableListView;

    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {

        // Add delimiter options.
        delimiterBox.getItems().setAll("comma", "tab", "space");
        delimiterBox.getSelectionModel().selectFirst();

    }

    // Expects data and names files to be selected to function
    public void loadInputs() {
        inputLoaded = false;
        System.out.println("User has pressed Load Inputs button...");

        String dataPath = inputDataFile.getText();
        String namePath = inputNameFile.getText();

        List<String> dataContent = null;
        List<String> nameContent = null;

        try {
            dataContent = Files.readAllLines(Paths.get(dataPath));
            nameContent = Files.readAllLines(Paths.get(namePath));
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid Input File Paths", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        // Parse Data file.

        try {
            String delimiter = delimiterBox.getValue();

            if (delimiter.equals("comma")) {
                delimiter = ",";
            } else if (delimiter.equals("tab")) {
                delimiter = "\t";
            } else {
                delimiter = " ";
            }

            int numAttributes = dataContent.get(0).split(delimiter).length;

            String[][] dataMatrix = new String[dataContent.size()][numAttributes];

            for (int i = 0; i < dataContent.size(); i++) {
                String[] currEntry = dataContent.get(i).split(delimiter);
                dataMatrix[i] = currEntry;
            }

            data = dataMatrix;
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Unable to read input data file", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        // Parse Name file.
        try {
            attNames = nameContent;
            if (attNames.size() != data[0].length) {
                throw new Exception();
            }
            stableListView.getItems().clear();
            // Update attribute list with names.
            stableListView.getItems().addAll(attNames);

            decAttributeComboBox.setItems(FXCollections.observableArrayList(attNames));
        } catch(Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Unable to read input names file. Each name must be on a newline.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        inputLoaded = true;
        System.out.println("Successfully Loaded Input Files!");
    }


    public void generateRules(ActionEvent actionEvent) {

        if (!inputLoaded) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Load input files first!", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        allRules.clear();
        actionRules.clear();

        // First, save current necessary input from forms.
        try {
            supportThresh = Integer.parseInt(supportTextField.getText());
            confThresh = Double.parseDouble(confTextField.getText()) * .01;
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Select min support and confidence!", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        decisionAttribute = decAttributeComboBox.getSelectionModel().getSelectedIndex();

        if (decisionAttribute == -1) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Select decision attribute!", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        stableAttributes = stableListView.getSelectionModel().getSelectedIndices();

        decisionValueFrom = decValueFromTextField.getText();
        decisionValueTo = decValueToTextField.getText();

        if (decisionValueFrom.isEmpty() || decisionValueTo.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Please specify both the decision value to and from.", ButtonType.OK);
            alert.showAndWait();
            return;
        }



        try {
            outFile = new PrintStream(new File(outputDataFile.getText()));
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid Output File Path", ButtonType.OK);
            alert.showAndWait();
            return;
        }


        printMessage("\n-- Computing LERS! --\n");
        computeLERS(data, decisionAttribute);
        printMessage("\n-- Computing Action Rules! --\n");
        computeActionRules();
        outFile.close();
        System.exit(0);
    }

    // Input: t - n x m table where n = # of entries and m = # of attributes
    //        d - index of decision attribute (between 0 and m - 1)
    private void computeLERS(String[][] t, int d) {

        // ******************************ASSEMBLE************************************************

        List<AttributeGroup> attributeGroups = new ArrayList<>();
        List<AttributeGroup> decisionAttributeGroups = new ArrayList<>();

        // Outer loop - attribute level
        for (int i = 0; i < t[0].length; i++) {

            HashMap<String, Set<Integer>> hm = new HashMap<>();
            Set<String> currVals = new TreeSet<>();

            // Inner loop - entry level
            for (int j = 0; j <t.length; j++) {

                String keyString = t[j][i];

                // Skip unspecified attributes
                if (keyString.equals("?"))
                    continue;

                Set<Integer> currVal = hm.get(keyString);

                if (currVal != null) {
                    // Add entry #.
                    currVal.add(j);
                } else {
                    // Create new value.
                    Set<Integer> newVal = new TreeSet<>();
                    newVal.add(j);
                    hm.put(keyString, newVal);
                }
            }

            // Create set of attribute values here?
            Set<String> keys = hm.keySet();
            for (String k : keys) {
                ArrayList<String> attVals = new ArrayList<>();

                for (int a = 1; a < t[0].length; a++) {
                    attVals.add(null);
                }

                // Add current attribute's hashmap to the list. Leave the rest blank. These will be filled in as
                // merging happens later on.
                attVals.add(i, k);

                AttributeGroup ag = new AttributeGroup(attVals, hm.get(k));

                // Separate the decision attribute sets from others.
                if (i == d) {
                    decisionAttributeGroups.add(ag);
                } else {
                    attributeGroups.add(ag);
                }
            }

        }


        // ******************************MAIN LOOP************************************************


        // Main loop. Check for subsets of decision attributes.

        int desiredSize = 2;
        certainRules = new ArrayList<>();
        possibleRules = new ArrayList<>();

        boolean setsRemain = true;

        while (setsRemain) {

            printMessage(String.format("\n-- Iteration %d --", desiredSize - 1));

            List<Rule> currCertainRules = new ArrayList<>();
            List<Rule> currPossibleRules = new ArrayList<>();

            List<AttributeGroup> currUnmarked = new ArrayList<>();

            for (AttributeGroup attGroup : attributeGroups) {

                // First, check to see if this attribute group contains a subset group in certain rules.
                // If so, skip this.
                boolean isSubsetOfMarked = false;
                for (Rule c : certainRules) {
                    if (c.attributeGroup.isSubsetOf(attGroup)) {
                        isSubsetOfMarked = true;
                        break;
                    }
                }

                if (isSubsetOfMarked)
                    continue;

                for (AttributeGroup decGroup : decisionAttributeGroups) {

                    // Find intersection of attGroup and decGroup.

                    attGroup.marked = null;
                    // This is a possible rule.
                    int numOverLap = 0;
                    for (Integer e : attGroup.entries) {
                        if (decGroup.entries.contains(e)) {
                            numOverLap++;
                        }
                    }

                    // Only create rules for fully or partially overlapping groups.
                    if (numOverLap == 0)
                        continue;

                    double currConf = (double) numOverLap / attGroup.entries.size();

                    // Must pass support and confidence thresholds!
                    if (numOverLap < supportThresh || currConf < confThresh)
                        continue;

                    Rule newRule = new Rule(
                            attGroup,
                            decGroup,
                            currConf,
                            numOverLap,
                            attNames);


                    if (numOverLap == attGroup.entries.size()) {
                        currCertainRules.add(newRule);
                    } else {
                        currPossibleRules.add(newRule);
                        currUnmarked.add(attGroup);
                    }

                }
            }


            // Rules have been created. From the current possible ones, attempt to combine them.
            List<AttributeGroup> newAttGroups = new ArrayList<>();
            for (int i = 0; i < currPossibleRules.size(); i++) {

                for (int j = i + 1; j < currPossibleRules.size(); j++) {
                    // Attempts to combine sets. If fails, returns null
                    AttributeGroup combinedGroup = AttributeGroup.combine(currUnmarked.get(i), currUnmarked.get(j), desiredSize);
                    if (combinedGroup != null) {
                        newAttGroups.add(combinedGroup);
                    }
                }
            }

            desiredSize++;

            // New AttributeGroups have all been created.
            // Remove duplicates!
            List<AttributeGroup> uniqueAttGroups = new ArrayList<>();
            for (AttributeGroup a : newAttGroups) {
                if (!uniqueAttGroups.contains(a))
                    uniqueAttGroups.add(a);
            }

            List<Rule> uniqueCertainRules = new ArrayList<>();
            for (Rule r : currCertainRules) {
                if (!uniqueCertainRules.contains(r))
                    uniqueCertainRules.add(r);
            }

            List<Rule> uniquePossibleRules = new ArrayList<>();
            for (Rule r : currPossibleRules) {
                if (!uniquePossibleRules.contains(r))
                    uniquePossibleRules.add(r);
            }


            attributeGroups = uniqueAttGroups;


            // Print Certain rules.
            printMessage("\n- CERTAIN RULES -");
            for(Rule c : currCertainRules) {
                printMessage(c.toString());
            }

            // Print Possible rules with support and confidence.
            printMessage("\n- POSSIBLE RULES -");
            for(Rule p : currPossibleRules) {
                printMessage(p.toString());
            }

            certainRules.addAll(uniqueCertainRules);
            possibleRules.addAll(uniquePossibleRules);

            setsRemain = !uniqueAttGroups.isEmpty();
        }

        printMessage("No more extraction can be done!");
        allRules.addAll(certainRules);
        allRules.addAll(possibleRules);

    }

    // From the LERS certain rules, construct Action Rules from the user input <decision attribute, to value, from value>
    private void computeActionRules() {
        // Look for elements with From
        // Look for elements with To

        for (int i = 0; i < allRules.size(); i++) {
            Rule fromRule = allRules.get(i);
            if (fromRule.decisionGroup.attVals.contains(decisionValueFrom)) {
                for (int j = i + 1; j < allRules.size(); j++) {
                    Rule toRule = allRules.get(j);
                    if (toRule.decisionGroup.attVals.contains(decisionValueTo)) {
                        // This could be an action rule!
                        // Check if stable attributes are the same. They must be.
                        // Identify flexible attributes that are different.

                        List<String[]> toFromPairs = new ArrayList<>();
                        int numFlexible = 0;
                        for (int k = 0; k < attNames.size(); k++) {
                            String fromVal = fromRule.attributeGroup.attVals.get(k);
                            String toVal = toRule.attributeGroup.attVals.get(k);

                            if (fromVal == null && toVal == null) {
                                continue;
                            }


                            if (stableAttributes.contains(k)) {

                                // Ensure they are the same.
                                if (fromVal == null) {
                                    toFromPairs.add(new String[] {Integer.toString(k), toVal, toVal});
                                } else if (toVal == null) {
                                    toFromPairs.add(new String[] {Integer.toString(k), fromVal, fromVal});
                                } else if (fromVal.equals(toVal)) {
                                    toFromPairs.add(new String[] {Integer.toString(k), fromVal, toVal});
                                } else {
                                    break;
                                }

                            } else {
                                // It's a flexible attribute.
                                if (fromVal != null && toVal != null) {
                                    toFromPairs.add(new String[]{Integer.toString(k), fromVal, toVal});
                                    numFlexible++;
                                }

                            }
                        }

                        if (toFromPairs.isEmpty() || numFlexible == 0)
                            continue;

                        AttributeGroup fromRuleDec = fromRule.decisionGroup;
                        AttributeGroup toRuleDec = toRule.decisionGroup;

                        // Encode decision transformation same way as others.
                        String[] decToFromPair = new String[] {
                                Integer.toString(decisionAttribute),
                                fromRuleDec.attVals.get(decisionAttribute),
                                toRuleDec.attVals.get(decisionAttribute)};

                        // Support = card(From attributes ^ From decision attributes)
                        // Conf = (support / card(From attributes)) * (card(To attributes ^ To decision attributes) / card(To attributes))

                        int support = fromRule.getSupport();
                        double confidence =
                                ((double) support / fromRule.attributeGroup.getCardinality()) *
                                ((double) toRule.getSupport() / toRule.attributeGroup.getCardinality());

                        if (support < supportThresh || confidence < confThresh)
                            continue;

                        ActionRule newAR = new ActionRule(toFromPairs, attNames, decToFromPair, support, confidence);
                        actionRules.add(newAR);
                    }
                }
            }
        }

        List<ActionRule> uniqueActionRules = new ArrayList<>();
        for(ActionRule a : actionRules) {
            if (!uniqueActionRules.contains(a)) {
                uniqueActionRules.add(a);
            }
        }

        actionRules = uniqueActionRules;

        // All rules have been added. Now print them and save to disk!
        printMessage("\n-- ACTION RULES --\n");

        for(ActionRule a : actionRules) {
            printMessage(a.toString());
        }

    }

    // Prints a message both to console and the specified output file
    private void printMessage(String message) {
        System.out.println(message);
        outFile.println(message);
    }

}
