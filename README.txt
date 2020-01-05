Group 12 - Action Rules Extraction Project

Ayanjyoti Thakuria
Sai Kumar Thallada
Narendra Kumar Vankayala
Mohit Varma
Sathyaram Venkatesan


Input Data:

- Some sample input data and names files are in /sample_input. These are the car and mammogram datasets, along with an example from class.

- The data file format is expected to have one data entry per line with delimited attributes. This application supports
 comma, tab, and space delimiters.

- Note that the names input file is expected to have one attribute name per line.


Compiling and Executing Program:

- The java files are in src/ar. You can compile them if you'd like.
- Run with java Main
- Already generated .class files can be found in out/production/ActionRulesProject.


Using Application:

1. Input paths to data file and text files in the appropriate fields. Select the delimiter for the data file. Click 'Load Input Files'.
2. Enter the minimum support and confidence values. Note: These are used for both the LERS and Action Rules as filters.
3. Select decision attribute (this dropdown list is populated after loading input files) and choose the from/to values.
4. Select any stable attributes. You can hold CTRL to select more than one.
5. Enter output file path. This is where rules will be saved to.
6. Click 'Compute Action Rules' and the program will extract the action rules.


