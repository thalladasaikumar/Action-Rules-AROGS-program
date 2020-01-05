package ar;

import java.util.*;

// Represents a set of entries for a group of attributes.
public class AttributeGroup {

    public ArrayList<String> attVals;

    // Which entries from input file have these values?
    public Set<Integer> entries;
    // True if this AttributeGroup is a subset of one of the decision attribute's classes.
    public Set<String> marked = null;


    public AttributeGroup(ArrayList<String> attVals, Set<Integer> entries) {
        this.attVals = attVals;
        this.entries = entries;
    }

    // Combine two AttributeGroup
    public static AttributeGroup combine(AttributeGroup a, AttributeGroup b, int desiredSize) {


        int numAttributes = a.attVals.size();

        // Attempt to grow AttributeGroup to the desired size. (Assumes current groups are 1 less than that.)

        ArrayList<String> newAttVals = new ArrayList<>();

        int numSame = 0;
        int numDiff = 0;

        for (int i = 0; i < numAttributes; i++) {
            String aVal = a.attVals.get(i);
            String bVal = b.attVals.get(i);

            // Are they both not present?
            if (aVal == null && bVal == null) {
                newAttVals.add(null);
                continue;
            }

            if (aVal == null) {
                newAttVals.add(bVal);
                numDiff++;

            } else if (bVal == null) {
                newAttVals.add(aVal);
                numDiff++;

            } else {
                // aVal and bVal both exist. They MUST be the same.
                if (aVal.equals(bVal)) {
                    newAttVals.add(aVal);
                    numSame++;
                } else {
                    return null;
                }
            }
        }

        // sameSet size + diffSet size must equal desired size.
        int totalSize = numSame + numDiff;
        if (totalSize != desiredSize)
            return null;

        Set<Integer> newEntries = new TreeSet<>();
        for (Integer e : a.entries) {
            if (b.entries.contains(e)) {
                newEntries.add(e);
            }
        }

        return new AttributeGroup(newAttVals, newEntries);
    }

    // True if this object is a subset of the other one
    public boolean isSubsetOf(AttributeGroup other) {
        // This object is a subset of other if every attVal in this is found in other.
        for (int i = 0; i < this.attVals.size(); i++) {
            String currItem = this.attVals.get(i);
            if (currItem == null)
                continue;

            String otherItem = other.attVals.get(i);
            if (!currItem.equals(otherItem)) {
                return false;
            }
        }

        return true;
    }

    public int getCardinality() {
        return entries.size();
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object other) {

        if (!(other instanceof AttributeGroup)) {
            return false;
        }

        AttributeGroup a = (AttributeGroup) other;

        return this.attVals.equals(a.attVals);

    }
}
