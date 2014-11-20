To run test:

1. First import the ecse429BlackBox package into the jucmnav project. 


2. Then in the Testdata class update all the csv file paths(fileD,fileS,fileN,fileL,fileEn,fileEl) 
to the ****absolute paths***** pf the csvs' located in the ecse429BlackBox package.(lines 8,10,12,14,16,18 in TesData)


3. Then change the variable "externalTestProjectPath"(line 81) the FeatureModelStrategyAlgorithmTest class the the ****absolute path***** of the TestSuite project,
that is also included in the submission(this project contains the jucm file that is loaded for the test cases).