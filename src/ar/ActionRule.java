package ar;

import java.util.Arrays;
import java.util.List;

// Represents an action rule. Transformations can be derived from the dectoFromPair field
public class ActionRule {

    private List<String[]> toFromPairs;
    private List<String> attNames;
    private String[] decToFromPair;

    private int support;
    private double confidence;

    public ActionRule(List<String[]> toFromPairs, List<String> attNames, String[] decToFromPair, int support, double confidence) {
        this.toFromPairs = toFromPairs;
        this.attNames = attNames;
        this.decToFromPair = decToFromPair;

        this.support = support;
        this.confidence = confidence;
    }


    @Override
    public String toString() {
        // (a: 1 -> 2 ^ c = 5) -> (d: 7 -> 9)
        // (a1, c5) -> d6 -- Support:2 -- Confidence 50%

        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < this.toFromPairs.size(); i++) {
            String[] currTerm = this.toFromPairs.get(i);
            // First index is index of attribute.
            int attIndex = Integer.parseInt(currTerm[0]);
            // Second index is FROM value.
            String fromVal = currTerm[1];
            // Third index is TO value.
            String toVal = currTerm[2];

            if (fromVal.equals(toVal)) {
                // This was stable? TODO: Verify this logic is correct.
                // format is x = 9,
                sb.append(attNames.get(attIndex));
                sb.append(" = ");
                sb.append(fromVal);
            } else {
                // Values not equal. Means flexible
                // format is x: 1 -> 8
                sb.append(attNames.get(attIndex));
                sb.append(": ");
                sb.append(fromVal);
                sb.append(" -> ");
                sb.append(toVal);
            }

            sb.append(", ");

        }

        // Remove final comma and space
        sb.delete(sb.length() - 2, sb.length() - 1);

        sb.append(")");

        sb.append(" -> ");

        // Add decision attName - attVal

        sb.append("(");
        sb.append(attNames.get(Integer.parseInt(this.decToFromPair[0])));
        sb.append(": ");
        sb.append(this.decToFromPair[1]);
        sb.append(" -> ");
        sb.append(this.decToFromPair[2]);
        sb.append(")");

        sb.append(String.format(" *** SUPPORT: %d *** CONFIDENCE: %.2f percent", this.support, this.confidence * 100));

        return sb.toString();
    }

    @Override
    public int hashCode() {
        int val = toFromPairs.get(0)[0].hashCode();
        return val;
    }

    @Override
    public boolean equals(Object other) {

        if (!(other instanceof ActionRule)) {
            return false;
        }

        ActionRule r = (ActionRule) other;

        if (this.toFromPairs.size() != r.toFromPairs.size()) {
            return false;
        }

        for (int i = 0; i < this.toFromPairs.size(); i++) {
            if (!Arrays.equals(this.toFromPairs.get(i), r.toFromPairs.get(i))) {
                return false;
            }
        }
        return true;

    }
}
