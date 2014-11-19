package ecsce429BlackBox;

import java.util.HashMap;

public class TestData 
{
	public static String[] testCaseDiagrams ;
	public static String fileD="C:/Users/Bernie/Desktop/FeatureDiagrams.csv" ;
	public static String[] testCaseStrategy ;
	public static String fileS="/Users/Bernie/Desktop/EvaluationStrategies.csv";
	public static String[][] testCaseNodes ;
	public static String fileN="/Users/Bernie/Desktop/Nodes.csv";
	public static String[][] testCaseLinks ;
	public static String fileL="/Users/Bernie/Desktop/Links.csv";
	public static HashMap<Integer, HashMap<String, String[]>> expectedValuesForNodes;
	public static String fileEN="/Users/Bernie/Desktop/data.csv";
	public static HashMap<Integer, HashMap<String, Integer>> expectedValuesForLinks;
	public static String fileEL="C:/Users/Bernie/Desktop/Links_CV.csv";
	
	public static void loadDataForTest()
	{
		
	}
	
	
	public static void main(String[] args)
	{
		testCaseDiagrams = ReadCVS.readGraphNames(fileD, testCaseDiagrams);//format: every new line is a diagram name: line 
		testCaseStrategy =ReadCVS.readStrategyNames(fileS, testCaseStrategy);//format: every new line is a strategy name
		testCaseNodes= ReadCVS.readNodeNames(fileN, testCaseNodes);
		testCaseLinks=ReadCVS.readLinkNames(fileS, testCaseLinks);
		expectedValuesForNodes=ReadCVS.readNodesValues(fileEN, expectedValuesForNodes);
		expectedValuesForLinks = ReadCVS.readLinkValues(fileEL, expectedValuesForLinks);
		
	}
	
	
	public static void loadData()
	{
		testCaseDiagrams = ReadCVS.readGraphNames(fileD, testCaseDiagrams);//format: every new line is a diagram name: line 
		testCaseStrategy =ReadCVS.readStrategyNames(fileS, testCaseStrategy);//format: every new line is a strategy name
		testCaseNodes= ReadCVS.readNodeNames(fileN, testCaseNodes);
		testCaseLinks=ReadCVS.readLinkNames(fileS, testCaseLinks);
		expectedValuesForNodes=ReadCVS.readNodesValues(fileEN, expectedValuesForNodes);
		expectedValuesForLinks = ReadCVS.readLinkValues(fileEL, expectedValuesForLinks);
		
	}
	
	
}
