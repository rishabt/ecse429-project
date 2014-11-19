package ecsce429BlackBox;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
 
public class ReadCVS {
	public static String[][] readLinkNames(String file, String[][] links)
	  {
		links = new String[100][100];
		String csvFile = file;
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
	 
		try {
	 
			br = new BufferedReader(new FileReader(csvFile));
			int i=0;
			while ((line = br.readLine()) != null) {
	 
			        // use comma as separator
				String[] entry = line.split(cvsSplitBy);
				links[i]=new String[entry.length];
				links[i]=entry;
				i++;
			}
	 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return links;
	  }
	public static String[][] readNodeNames(String file, String[][] nodes)
	  {
		String csvFile = file;
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
	 
		try {
			nodes = new String[100][100];
			br = new BufferedReader(new FileReader(csvFile));
			int i=0;
			while ((line = br.readLine()) != null) {
	 
			        // use comma as separator
				String[] entry = line.split(cvsSplitBy);
				nodes[i]=new String[entry.length];
				nodes[i]=entry;
				i++;
			}
	 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return nodes;
	  }
	public static String[] readStrategyNames(String file, String[] strategies)
	  {
		  String csvFile = file;
			BufferedReader br = null;
			String line = "";
			String cvsSplitBy = ",";
		 ArrayList<String> list = new ArrayList<String>();
			try {
		 
				br = new BufferedReader(new FileReader(csvFile));
				int i =0;
				while ((line = br.readLine()) != null) {
					list.add(line);
					i++;
				}
				strategies = new String[list.size()];
				strategies = list.toArray(strategies);
				 
		 
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return strategies;
	  }
	public static String[] readGraphNames(String file, String[] graphs)
	  {
		  String csvFile = file;
			BufferedReader br = null;
			String line = "";
			String cvsSplitBy = ",";
		 ArrayList<String> list = new ArrayList<String>();
			try {
		 
				br = new BufferedReader(new FileReader(csvFile));
				int i =0;
				while ((line = br.readLine()) != null) {
					list.add(line);
					i++;
				}
				graphs = new String[list.size()];
				graphs = list.toArray(graphs);
		 
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (br != null) {
					try {
						br.close();
						
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return graphs;
	  }
	public static String[] readNodesNames(String file, String[] nodes)
	  {
		  String csvFile = file;
			BufferedReader br = null;
			String line = "";
			String cvsSplitBy = ",";
		 ArrayList<String> list = new ArrayList<String>();
			try {
		 
				br = new BufferedReader(new FileReader(csvFile));
				int i =0;
				while ((line = br.readLine()) != null) {
					list.add(line);
					i++;
				}
				nodes = new String[list.size()];
				nodes = list.toArray(nodes);
		 
		 
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return nodes;
	  }
	
	public static HashMap<Integer, HashMap<String, String[]>> readNodesValues(String file, HashMap<Integer,HashMap<String, String[]>> expected)
  {
	  String csvFile = file;
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		expected = new HashMap<Integer, HashMap<String,String[]>>();
		
		try {
	 
			br = new BufferedReader(new FileReader(csvFile));
			int i =0;
			while ((line = br.readLine()) != null) 
			{
				//feature1,sv,color,warning
				String[] entry =line.split(cvsSplitBy);
				HashMap<String, String[]> test = new HashMap<String, String[]>();
			    for(int j=0;j<entry.length;j+=4)
			    {
			    	String Name = entry[j];
					String Value = entry[j+1];
					String color = entry[j+2];
					String warning = entry[j+3];
					
					String [] exp = new String[]{Value,color,warning};
					test.put(Name, exp);
			    }
				expected.put(i, test);
				i++;
			}
	 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return expected;
  }
  public static HashMap<Integer, HashMap<String, Integer>> readLinkValues(String file, HashMap<Integer, HashMap<String, Integer>> expected)
  {
	  String csvFile = file;
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		expected = new HashMap<Integer, HashMap<String,Integer>>();
		try {
	 
			br = new BufferedReader(new FileReader(csvFile));
			int i =0;
			while ((line = br.readLine()) != null) 
			{
				String[] entry =line.split(cvsSplitBy);
				HashMap<String, Integer> test = new HashMap<String, Integer>();
			    for(int j=0;j<entry.length;j+=2)
			    {
			    	String Name = entry[j];
					String Value = entry[j+1];
					
					test.put(Name,Integer.parseInt(Value));
			    }
				expected.put(i, test);
				i++;
			}
	 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return expected;
  }
 
}