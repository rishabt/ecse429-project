package ecsce429BlackBox;

import static org.junit.Assert.*;
import fm.impl.FeatureDiagramImpl;
import fm.impl.FeatureImpl;
import fm.impl.MandatoryFMLinkImpl;
import fm.impl.OptionalFMLinkImpl;
import grl.ElementLink;
import grl.Evaluation;
import grl.EvaluationStrategy;
import grl.GRLGraph;
import grl.GRLNode;
import grl.IntentionalElement;
import grl.LinkRef;
import grl.impl.DecompositionImpl;
import grl.impl.EvaluationImpl;
import grl.impl.IntentionalElementImpl;
import grl.impl.LinkRefImpl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import seg.jUCMNav.JUCMNavPlugin;
import seg.jUCMNav.editors.GrlEditor;
import seg.jUCMNav.editors.UCMNavMultiPageEditor;
import seg.jUCMNav.editparts.GrlGraphicalEditPartFactory;
import seg.jUCMNav.editparts.IntentionalElementEditPart;
import seg.jUCMNav.model.commands.create.CreateGrlGraphCommand;
import seg.jUCMNav.model.commands.delete.DeleteMapCommand;
import seg.jUCMNav.strategies.EvaluationStrategyManager;
import seg.jUCMNav.views.preferences.StrategyEvaluationPreferences;
import ucm.map.UCMmap;
import urn.URNspec;
import urncore.IURNConnection;
import urncore.IURNDiagram;
import urncore.IURNNode;


public class FeatureModelStrategyAlgorithmTest  
{
	
	private static String externalTestProjectPath="C:\\Users\\Bernie\\workspace\\TestSuite";
    private static String testProjectName="TestSuite";
    private static String testFileName="testcases.jucm";
	
	private static UCMNavMultiPageEditor editor;
    private static EList<IURNDiagram> diagrams;
    private static EList<EvaluationStrategy> strategies;
    private static EvaluationStrategyManager evalStrMan;
    private static HashMap<String, EvaluationStrategy> strategyMap;
    private static HashMap<String, FeatureDiagramImpl> diagramMap;
    private static HashMap<String,FeatureImpl> featureMap;
    private static HashMap<String, OptionalFMLinkImpl> optionalLinkMap;
    private static HashMap<String,MandatoryFMLinkImpl > mandatoryLinkMap;
    private static HashMap<String,IntentionalElementImpl > intentionalElementMap;
    private static IEditorPart ed;
    private static URNspec urn;

    boolean testBindings;
	
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception 
	{

		try 
		{
			TestData.loadData();
			IWorkbench wb = PlatformUI.getWorkbench();
			IPreferenceStore pref = JUCMNavPlugin.getDefault().getPreferenceStore();
			String algo = pref.getString(StrategyEvaluationPreferences.PREF_ALGORITHM);
			pref.setValue(StrategyEvaluationPreferences.PREF_ALGORITHM, StrategyEvaluationPreferences.FEATURE_MODEL_ALGORITHM+"");
			pref.setValue(StrategyEvaluationPreferences.PREF_TOLERANCE, 0);
			pref.setValue(StrategyEvaluationPreferences.PREF_AUTOSELECTMANDATORYFEATURES, true);

			IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
			//getting project, if it exists 
			IProject testproject = workspaceRoot.getProject(testProjectName); //$NON-NLS-1$
			
			//if it does not exists create it 
			if (!testproject.exists())
	            testproject.create(null);
			
			if (!testproject.isOpen())
	            testproject.open(null);  
			
			IFile testfile = testproject.getFile(testFileName);
			testfile.create(new ByteArrayInputStream("".getBytes()), false, null);
			//File location of external test project 
			File externalProject = new File(externalTestProjectPath);
	        File workspaceProject = new File(workspaceRoot.getRawLocation()+"\\"+testProjectName);//"C:\\Users\\Bernie\\junit-workspace\\testjucm");
	        
	        //copy contents of external test project into newly created one(work around for eclipse not finding projects just copied into workspace)
	        copyFolder(externalProject, workspaceProject);

	        //refresh project just to reset everything
	        testproject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
	        
	        //get page, descriptor and editor 
	        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	        IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(testfile.getName());
	        ed = page.openEditor(new FileEditorInput(testfile), desc.getId());
	        editor = (UCMNavMultiPageEditor) ed;
	       
	        
	        urn = editor.getModel().getGrlspec().getUrnspec();
	        
	        
	        diagrams = editor.getModel().getUrndef().getSpecDiagrams();
	        strategies = editor.getModel().getGrlspec().getStrategies();
	        evalStrMan = EvaluationStrategyManager.getInstance(editor,true);
	        strategyMap = new HashMap<String, EvaluationStrategy>();
	        for (EvaluationStrategy str : strategies) 
	        {
				strategyMap.put(str.getName(), str);
			}
	        
	        diagramMap = new HashMap<String, FeatureDiagramImpl>();
	        optionalLinkMap = new HashMap<String, OptionalFMLinkImpl>();
	        mandatoryLinkMap = new HashMap<String, MandatoryFMLinkImpl>();
	        
	        //just calculating to get all nodes elegantly for verification 
	        EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[0]);
	        evalStrMan.setStrategy(str);
	        HashMap evals = evalStrMan.getEvaluations();
	        Collection vals = evals.values();//EvaluationImpl
			Collection keys = evals.keySet();//FeatureImpl
			
			Object[] features =  keys.toArray();
			Object[] evalsArray = vals.toArray();
			
			featureMap= new HashMap<String, FeatureImpl>();
			intentionalElementMap= new HashMap<String, IntentionalElementImpl>();
			for(int i =0 ; i<features.length;i++)
			{
				if(features[i] instanceof FeatureImpl )
				{
					FeatureImpl f = (FeatureImpl)features[i];
					featureMap.put(f.getName(),f );
				}
				else if(features[i] instanceof IntentionalElementImpl)
				{
					IntentionalElementImpl element = (IntentionalElementImpl)features[i];
					intentionalElementMap.put(element.getName(), element);
				}
			}
			
	        
	        for (IURNDiagram gram : diagrams) 
	        {
	        	FeatureDiagramImpl dia =(FeatureDiagramImpl)gram;
	        	
				diagramMap.put(dia.getName(), dia);
				EList linkRefs = dia.getConnections();
				
				for (Object linkerR : linkRefs) 
				{
					LinkRefImpl linkerRef = (LinkRefImpl)linkerR;
					ElementLink linker = linkerRef.getLink();
					if(linker instanceof DecompositionImpl)
					{
						
					}	
					if(linker instanceof OptionalFMLinkImpl)
					{
						OptionalFMLinkImpl l = (OptionalFMLinkImpl)linker;
						optionalLinkMap.put(l.getName(), l);
					}
					if(linker instanceof MandatoryFMLinkImpl)
					{
						MandatoryFMLinkImpl m = (MandatoryFMLinkImpl) linker;
						mandatoryLinkMap.put(m.getName(), m);
					}
				}
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

	@Test
	public void test1()
	{
		try
		{
			
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[0]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[0]);

		    HashMap evals = evalStrMan.getEvaluations();
		    HashMap<String, boolean[]> NodeTestResults = new HashMap<String, boolean[]>();
		    //sv,color,warning
		    HashMap<String, Boolean> LinkTestResults = new HashMap<String, Boolean>();
		    
			for(int i =0; i < TestData.testCaseNodes[0].length;i++)
			{
				if(TestData.testCaseNodes[0][i]==null)
					break;
					
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[0][i]);
				
				
				boolean ig =evalStrMan.isIgnored(f);
				EvaluationImpl e = (EvaluationImpl) evals.get(f);
				int sv = e.getEvaluation();
				String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
			    String color = getColorFromRGB(colorRGB);
				EList meta = f.getMetadata();
				
				boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(0).get(f.getName())[0])==sv;
				boolean colorResult;
				if(color==null)
					colorResult=false;
				else
					colorResult = color.equals(TestData.expectedValuesForNodes.get(0).get(f.getName())[1]);
				
				boolean warningResult;
				
				if(meta.size()<3)
				{
					warningResult=TestData.expectedValuesForNodes.get(0).get(f.getName())[2].equals("none");
					//assertEquals("none", TestData.expectedValuesForNodes.get(0).get(f.getName())[2]);
					
				}
				else
				{
					String val= (String) meta.get(3);
					warningResult= TestData.expectedValuesForNodes.get(0).get(f.getName())[2].equals(val);
					//assertEquals(val, TestData.expectedValuesForNodes.get(0).get(f.getName())[2]);
					
				}
				NodeTestResults.put(f.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				//_userSetEvaluationWarning
				
			}
			 
			for(int i=0; i<TestData.testCaseLinks[0].length;i++)
			{
				boolean contributionValueResult;
				if(TestData.testCaseLinks[0][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[0][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[0][i]);
					//assertEquals(l.getQuantitativeContribution(),TestData.expectedValuesForNodes.get(0).get(l.getName()));
					contributionValueResult= TestData.expectedValuesForLinks.get(0).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[0][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[0][i]);
					contributionValueResult = TestData.expectedValuesForLinks.get(0).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
					//assertEquals(l.getQuantitativeContribution(),TestData.expectedValuesForNodes.get(0).get(l.getName()));
				}
			}
		
			boolean pass = true;
			System.out.println("Results for test # "+"1");
			for (String node : NodeTestResults.keySet()) 
			{
				System.out.println("Node:"+node);
				boolean[] results= NodeTestResults.get(node);
				if(!results[0]||!results[1]||!results[2])
					pass =false;
				System.out.println("Results:"+Boolean.toString(results[0])+","+Boolean.toString(results[1])+","+Boolean.toString(results[2]));
			}
			for (String link : LinkTestResults.keySet()) 
			{
				System.out.println("Link:"+link);
				boolean result= LinkTestResults.get(link);
				if(!result)
					pass = false;
				System.out.println("Result:"+Boolean.toString(result));			
			}
			assertTrue(pass);
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
			
		}
	}
	
	@Test
	public void test2()
	{
		try
		{
			int testCaseIndex=1;
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[testCaseIndex]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[testCaseIndex]);

		    HashMap evals = evalStrMan.getEvaluations();
		    HashMap<String, boolean[]> NodeTestResults = new HashMap<String, boolean[]>();
		    //sv,color,warning
		    HashMap<String, Boolean> LinkTestResults = new HashMap<String, Boolean>();
		    
			for(int i =0; i < TestData.testCaseNodes[testCaseIndex].length;i++)
			{
				if(TestData.testCaseNodes[testCaseIndex][i]==null)
					break;
					
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[testCaseIndex][i]);
				
				boolean ig =evalStrMan.isIgnored(f);
				EvaluationImpl e = (EvaluationImpl) evals.get(f);
				int sv = e.getEvaluation();
				String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
			    String color = getColorFromRGB(colorRGB);
				EList meta = f.getMetadata();
				
				boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[0])==sv;
				boolean colorResult;
				if(color==null)
					colorResult=false;
				else
					// dont change array index here
					colorResult = color.equals(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[1]);
				
				boolean warningResult;
				
				if(meta.size()<3)
				{
					warningResult=TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals("none");	
				}
				else
				{
					String val= (String) meta.get(3);
					warningResult= TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals(val);	
				}
				NodeTestResults.put(f.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				//_userSetEvaluationWarning
				
			}
			 
			for(int i=0; i<TestData.testCaseLinks[testCaseIndex].length;i++)
			{
				boolean contributionValueResult;
				if(TestData.testCaseLinks[testCaseIndex][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult= TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult = TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
			}
		
			boolean pass = true;
			System.out.println("Results for test # "+testCaseIndex+1);
			for (String node : NodeTestResults.keySet()) 
			{
				System.out.println("Node:"+node);
				boolean[] results= NodeTestResults.get(node);
				if(!results[0]||!results[1]||!results[2])
					pass =false;
				System.out.println("Results:"+Boolean.toString(results[0])+","+Boolean.toString(results[1])+","+Boolean.toString(results[2]));
			}
			for (String link : LinkTestResults.keySet()) 
			{
				System.out.println("Link:"+link);
				boolean result= LinkTestResults.get(link);
				if(!result)
					pass = false;
				System.out.println("Result:"+Boolean.toString(result));			
			}
			assertTrue(pass);
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
			
		}
	}
	
	@Test
	public void test3()
	{
		try
		{
			int testCaseIndex=2;
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[testCaseIndex]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[testCaseIndex]);

		    HashMap evals = evalStrMan.getEvaluations();
		    HashMap<String, boolean[]> NodeTestResults = new HashMap<String, boolean[]>();
		    //sv,color,warning
		    HashMap<String, Boolean> LinkTestResults = new HashMap<String, Boolean>();
		    
			for(int i =0; i < TestData.testCaseNodes[testCaseIndex].length;i++)
			{
				if(TestData.testCaseNodes[testCaseIndex][i]==null)
					break;
					
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[testCaseIndex][i]);
				
				boolean ig =evalStrMan.isIgnored(f);
				EvaluationImpl e = (EvaluationImpl) evals.get(f);
				int sv = e.getEvaluation();
				String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
			    String color = getColorFromRGB(colorRGB);
				EList meta = f.getMetadata();
				
				boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[0])==sv;
				boolean colorResult;
				if(color==null)
					colorResult=false;
				else
					// dont change array index here
					colorResult = color.equals(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[1]);
				
				boolean warningResult;
				
				if(meta.size()>=4)
				{
					String val= (String) meta.get(3);
					warningResult= TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals(val);		
				}
				else
				{

					warningResult=TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals("none");
				}
				NodeTestResults.put(f.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				//_userSetEvaluationWarning
				
			}
			 
			for(int i=0; i<TestData.testCaseLinks[testCaseIndex].length;i++)
			{
				boolean contributionValueResult;
				if(TestData.testCaseLinks[testCaseIndex][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult= TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult = TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
			}
		
			boolean pass = true;
			int testCaseNumber = testCaseIndex+1;
			System.out.println("Results for test # "+testCaseNumber);
			for (String node : NodeTestResults.keySet()) 
			{
				System.out.println("Node:"+node);
				boolean[] results= NodeTestResults.get(node);
				if(!results[0]||!results[1]||!results[2])
					pass =false;
				System.out.println("Results:"+Boolean.toString(results[0])+","+Boolean.toString(results[1])+","+Boolean.toString(results[2]));
			}
			for (String link : LinkTestResults.keySet()) 
			{
				System.out.println("Link:"+link);
				boolean result= LinkTestResults.get(link);
				if(!result)
					pass = false;
				System.out.println("Result:"+Boolean.toString(result));			
			}
			assertTrue(pass);
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
			
		}
	}

	@Test
	public void test4()
	{
		try
		{
			int testCaseIndex=3;
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[testCaseIndex]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[testCaseIndex]);

		    HashMap evals = evalStrMan.getEvaluations();
		    HashMap<String, boolean[]> NodeTestResults = new HashMap<String, boolean[]>();
		    //sv,color,warning
		    HashMap<String, Boolean> LinkTestResults = new HashMap<String, Boolean>();
		    
			for(int i =0; i < TestData.testCaseNodes[testCaseIndex].length;i++)
			{
				if(TestData.testCaseNodes[testCaseIndex][i]==null)
					break;
					
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[testCaseIndex][i]);
				
				boolean ig =evalStrMan.isIgnored(f);
				EvaluationImpl e = (EvaluationImpl) evals.get(f);
				int sv = e.getEvaluation();
				String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
			    String color = getColorFromRGB(colorRGB);
				EList meta = f.getMetadata();
				
				boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[0])==sv;
				boolean colorResult;
				if(color==null)
					colorResult=false;
				else
					// dont change array index here
					colorResult = color.equals(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[1]);
				
				boolean warningResult;
				
				if(meta.size()>=4)
				{
					String val= (String) meta.get(3);
					warningResult= TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals(val);		
				}
				else
				{

					warningResult=TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals("none");
				}
				NodeTestResults.put(f.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				//_userSetEvaluationWarning
				
			}
			 
			for(int i=0; i<TestData.testCaseLinks[testCaseIndex].length;i++)
			{
				boolean contributionValueResult;
				if(TestData.testCaseLinks[testCaseIndex][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult= TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult = TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
			}
		
			boolean pass = true;
			int testCaseNumber = testCaseIndex+1;
			System.out.println("Results for test # "+testCaseNumber);
			for (String node : NodeTestResults.keySet()) 
			{
				System.out.println("Node:"+node);
				boolean[] results= NodeTestResults.get(node);
				if(!results[0]||!results[1]||!results[2])
					pass =false;
				System.out.println("Results:"+Boolean.toString(results[0])+","+Boolean.toString(results[1])+","+Boolean.toString(results[2]));
			}
			for (String link : LinkTestResults.keySet()) 
			{
				System.out.println("Link:"+link);
				boolean result= LinkTestResults.get(link);
				if(!result)
					pass = false;
				System.out.println("Result:"+Boolean.toString(result));			
			}
			assertTrue(pass);
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
			
		}
	}
	
	@Test
	public void test5()
	{
		try
		{
			int testCaseIndex=4;
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[testCaseIndex]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[testCaseIndex]);

		    HashMap evals = evalStrMan.getEvaluations();
		    HashMap<String, boolean[]> NodeTestResults = new HashMap<String, boolean[]>();
		    //sv,color,warning
		    HashMap<String, Boolean> LinkTestResults = new HashMap<String, Boolean>();
		    
			for(int i =0; i < TestData.testCaseNodes[testCaseIndex].length;i++)
			{
				if(TestData.testCaseNodes[testCaseIndex][i]==null)
					break;
					
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[testCaseIndex][i]);
				
				boolean ig =evalStrMan.isIgnored(f);
				EvaluationImpl e = (EvaluationImpl) evals.get(f);
				int sv = e.getEvaluation();
				String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
			    String color = getColorFromRGB(colorRGB);
				EList meta = f.getMetadata();
				
				boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[0])==sv;
				boolean colorResult;
				if(color==null)
					colorResult=false;
				else
					// dont change array index here
					colorResult = color.equals(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[1]);
				
				boolean warningResult;
				
				if(meta.size()>=4)
				{
					String val= (String) meta.get(3);
					warningResult= TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals(val);		
				}
				else
				{

					warningResult=TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals("none");
				}
				NodeTestResults.put(f.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				//_userSetEvaluationWarning
				
			}
			 
			for(int i=0; i<TestData.testCaseLinks[testCaseIndex].length;i++)
			{
				boolean contributionValueResult;
				if(TestData.testCaseLinks[testCaseIndex][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult= TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult = TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
			}
		
			boolean pass = true;
			int testCaseNumber = testCaseIndex+1;
			System.out.println("Results for test # "+testCaseNumber);
			for (String node : NodeTestResults.keySet()) 
			{
				System.out.println("Node:"+node);
				boolean[] results= NodeTestResults.get(node);
				if(!results[0]||!results[1]||!results[2])
					pass =false;
				System.out.println("Results:"+Boolean.toString(results[0])+","+Boolean.toString(results[1])+","+Boolean.toString(results[2]));
			}
			for (String link : LinkTestResults.keySet()) 
			{
				System.out.println("Link:"+link);
				boolean result= LinkTestResults.get(link);
				if(!result)
					pass = false;
				System.out.println("Result:"+Boolean.toString(result));			
			}
			assertTrue(pass);
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
			
		}
	}

	@Test
	public void test6()
	{
		try
		{
			int testCaseIndex=5;
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[testCaseIndex]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[testCaseIndex]);

		    HashMap evals = evalStrMan.getEvaluations();
		    HashMap<String, boolean[]> NodeTestResults = new HashMap<String, boolean[]>();
		    //sv,color,warning
		    HashMap<String, Boolean> LinkTestResults = new HashMap<String, Boolean>();
		    
			for(int i =0; i < TestData.testCaseNodes[testCaseIndex].length;i++)
			{
				if(TestData.testCaseNodes[testCaseIndex][i]==null)
					break;
					
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[testCaseIndex][i]);
				
				boolean ig =evalStrMan.isIgnored(f);
				EvaluationImpl e = (EvaluationImpl) evals.get(f);
				int sv = e.getEvaluation();
				String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
			    String color = getColorFromRGB(colorRGB);
				EList meta = f.getMetadata();
				
				boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[0])==sv;
				boolean colorResult;
				if(color==null)
					colorResult=false;
				else
					// dont change array index here
					colorResult = color.equals(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[1]);
				
				boolean warningResult;
				
				if(meta.size()>=4)
				{
					String val= (String) meta.get(3);
					warningResult= TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals(val);		
				}
				else
				{

					warningResult=TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals("none");
				}
				NodeTestResults.put(f.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				//_userSetEvaluationWarning
				
			}
			 
			for(int i=0; i<TestData.testCaseLinks[testCaseIndex].length;i++)
			{
				boolean contributionValueResult;
				if(TestData.testCaseLinks[testCaseIndex][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult= TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult = TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
			}
		
			boolean pass = true;
			int testCaseNumber = testCaseIndex+1;
			System.out.println("Results for test # "+testCaseNumber);
			for (String node : NodeTestResults.keySet()) 
			{
				System.out.println("Node:"+node);
				boolean[] results= NodeTestResults.get(node);
				if(!results[0]||!results[1]||!results[2])
					pass =false;
				System.out.println("Results:"+Boolean.toString(results[0])+","+Boolean.toString(results[1])+","+Boolean.toString(results[2]));
			}
			for (String link : LinkTestResults.keySet()) 
			{
				System.out.println("Link:"+link);
				boolean result= LinkTestResults.get(link);
				if(!result)
					pass = false;
				System.out.println("Result:"+Boolean.toString(result));			
			}
			assertTrue(pass);
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
			
		}
	}

	@Test
	public void test7()
	{
		try
		{
			int testCaseIndex=6;
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[testCaseIndex]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[testCaseIndex]);

		    HashMap evals = evalStrMan.getEvaluations();
		    HashMap<String, boolean[]> NodeTestResults = new HashMap<String, boolean[]>();
		    //sv,color,warning
		    HashMap<String, Boolean> LinkTestResults = new HashMap<String, Boolean>();
		    
			for(int i =0; i < TestData.testCaseNodes[testCaseIndex].length;i++)
			{
				if(TestData.testCaseNodes[testCaseIndex][i]==null)
					break;
					
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[testCaseIndex][i]);
				
				boolean ig =evalStrMan.isIgnored(f);
				EvaluationImpl e = (EvaluationImpl) evals.get(f);
				int sv = e.getEvaluation();
				String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
			    String color = getColorFromRGB(colorRGB);
				EList meta = f.getMetadata();
				
				boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[0])==sv;
				boolean colorResult;
				if(color==null)
					colorResult=false;
				else
					// dont change array index here
					colorResult = color.equals(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[1]);
				
				boolean warningResult;
				
				if(meta.size()>=4)
				{
					String val= (String) meta.get(3);
					warningResult= TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals(val);		
				}
				else
				{

					warningResult=TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals("none");
				}
				NodeTestResults.put(f.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				//_userSetEvaluationWarning
				
			}
			 
			for(int i=0; i<TestData.testCaseLinks[testCaseIndex].length;i++)
			{
				boolean contributionValueResult;
				if(TestData.testCaseLinks[testCaseIndex][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult= TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult = TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
			}
		
			boolean pass = true;
			int testCaseNumber = testCaseIndex+1;
			System.out.println("Results for test # "+testCaseNumber);
			for (String node : NodeTestResults.keySet()) 
			{
				System.out.println("Node:"+node);
				boolean[] results= NodeTestResults.get(node);
				if(!results[0]||!results[1]||!results[2])
					pass =false;
				System.out.println("Results:"+Boolean.toString(results[0])+","+Boolean.toString(results[1])+","+Boolean.toString(results[2]));
			}
			for (String link : LinkTestResults.keySet()) 
			{
				System.out.println("Link:"+link);
				boolean result= LinkTestResults.get(link);
				if(!result)
					pass = false;
				System.out.println("Result:"+Boolean.toString(result));			
			}
			assertTrue(pass);
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
			
		}
	}
	
	@Test
	public void test8()
	{
		try
		{
			int testCaseIndex=7;
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[testCaseIndex]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[testCaseIndex]);

		    HashMap evals = evalStrMan.getEvaluations();
		    HashMap<String, boolean[]> NodeTestResults = new HashMap<String, boolean[]>();
		    //sv,color,warning
		    HashMap<String, Boolean> LinkTestResults = new HashMap<String, Boolean>();
		    
			for(int i =0; i < TestData.testCaseNodes[testCaseIndex].length;i++)
			{
				if(TestData.testCaseNodes[testCaseIndex][i]==null)
					break;
					
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[testCaseIndex][i]);
				
				boolean ig =evalStrMan.isIgnored(f);
				EvaluationImpl e = (EvaluationImpl) evals.get(f);
				int sv = e.getEvaluation();
				String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
			    String color = getColorFromRGB(colorRGB);
				EList meta = f.getMetadata();
				
				boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[0])==sv;
				boolean colorResult;
				if(color==null)
					colorResult=false;
				else
					// dont change array index here
					colorResult = color.equals(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[1]);
				
				boolean warningResult;
				
				if(meta.size()>=4)
				{
					String val= (String) meta.get(3);
					warningResult= TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals(val);		
				}
				else
				{

					warningResult=TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals("none");
				}
				NodeTestResults.put(f.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				//_userSetEvaluationWarning
				
			}
			 
			for(int i=0; i<TestData.testCaseLinks[testCaseIndex].length;i++)
			{
				boolean contributionValueResult;
				if(TestData.testCaseLinks[testCaseIndex][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult= TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult = TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
			}
		
			boolean pass = true;
			int testCaseNumber = testCaseIndex+1;
			System.out.println("Results for test # "+testCaseNumber);
			for (String node : NodeTestResults.keySet()) 
			{
				System.out.println("Node:"+node);
				boolean[] results= NodeTestResults.get(node);
				if(!results[0]||!results[1]||!results[2])
					pass =false;
				System.out.println("Results:"+Boolean.toString(results[0])+","+Boolean.toString(results[1])+","+Boolean.toString(results[2]));
			}
			for (String link : LinkTestResults.keySet()) 
			{
				System.out.println("Link:"+link);
				boolean result= LinkTestResults.get(link);
				if(!result)
					pass = false;
				System.out.println("Result:"+Boolean.toString(result));			
			}
			assertTrue(pass);
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
			
		}
	}
	
	@Test
	public void test9()
	{
		try
		{
			int testCaseIndex=8;
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[testCaseIndex]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[testCaseIndex]);

		    HashMap evals = evalStrMan.getEvaluations();
		    HashMap<String, boolean[]> NodeTestResults = new HashMap<String, boolean[]>();
		    //sv,color,warning
		    HashMap<String, Boolean> LinkTestResults = new HashMap<String, Boolean>();
		    
			for(int i =0; i < TestData.testCaseNodes[testCaseIndex].length;i++)
			{
				if(TestData.testCaseNodes[testCaseIndex][i]==null)
					break;
					
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[testCaseIndex][i]);
				
				boolean ig =evalStrMan.isIgnored(f);
				EvaluationImpl e = (EvaluationImpl) evals.get(f);
				int sv = e.getEvaluation();
				String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
			    String color = getColorFromRGB(colorRGB);
				EList meta = f.getMetadata();
				
				boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[0])==sv;
				boolean colorResult;
				if(color==null)
					colorResult=false;
				else
					// dont change array index here
					colorResult = color.equals(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[1]);
				
				boolean warningResult;
				
				if(meta.size()>=4)
				{
					String val= (String) meta.get(3);
					warningResult= TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals(val);		
				}
				else
				{

					warningResult=TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals("none");
				}
				NodeTestResults.put(f.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				//_userSetEvaluationWarning
				
			}
			 
			for(int i=0; i<TestData.testCaseLinks[testCaseIndex].length;i++)
			{
				boolean contributionValueResult;
				if(TestData.testCaseLinks[testCaseIndex][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult= TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult = TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
			}
		
			boolean pass = true;
			int testCaseNumber = testCaseIndex+1;
			System.out.println("Results for test # "+testCaseNumber);
			for (String node : NodeTestResults.keySet()) 
			{
				System.out.println("Node:"+node);
				boolean[] results= NodeTestResults.get(node);
				if(!results[0]||!results[1]||!results[2])
					pass =false;
				System.out.println("Results:"+Boolean.toString(results[0])+","+Boolean.toString(results[1])+","+Boolean.toString(results[2]));
			}
			for (String link : LinkTestResults.keySet()) 
			{
				System.out.println("Link:"+link);
				boolean result= LinkTestResults.get(link);
				if(!result)
					pass = false;
				System.out.println("Result:"+Boolean.toString(result));			
			}
			assertTrue(pass);
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
			
		}
	}
	
	@Test
	public void test10()
	{
		try
		{
			int testCaseIndex=9;
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[testCaseIndex]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[testCaseIndex]);

		    HashMap evals = evalStrMan.getEvaluations();
		    HashMap<String, boolean[]> NodeTestResults = new HashMap<String, boolean[]>();
		    //sv,color,warning
		    HashMap<String, Boolean> LinkTestResults = new HashMap<String, Boolean>();
		    
			for(int i =0; i < TestData.testCaseNodes[testCaseIndex].length;i++)
			{
				if(TestData.testCaseNodes[testCaseIndex][i]==null)
					break;
					
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[testCaseIndex][i]);
				
				boolean ig =evalStrMan.isIgnored(f);
				EvaluationImpl e = (EvaluationImpl) evals.get(f);
				int sv = e.getEvaluation();
				String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
			    String color = getColorFromRGB(colorRGB);
				EList meta = f.getMetadata();
				
				boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[0])==sv;
				boolean colorResult;
				if(color==null)
					colorResult=false;
				else
					// dont change array index here
					colorResult = color.equals(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[1]);
				
				boolean warningResult;
				
				if(meta.size()>=4)
				{
					String val= (String) meta.get(3);
					warningResult= TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals(val);		
				}
				else
				{

					warningResult=TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals("none");
				}
				NodeTestResults.put(f.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				//_userSetEvaluationWarning
				
			}
			 
			for(int i=0; i<TestData.testCaseLinks[testCaseIndex].length;i++)
			{
				boolean contributionValueResult;
				if(TestData.testCaseLinks[testCaseIndex][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult= TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult = TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
			}
		
			boolean pass = true;
			int testCaseNumber = testCaseIndex+1;
			System.out.println("Results for test # "+testCaseNumber);
			for (String node : NodeTestResults.keySet()) 
			{
				System.out.println("Node:"+node);
				boolean[] results= NodeTestResults.get(node);
				if(!results[0]||!results[1]||!results[2])
					pass =false;
				System.out.println("Results:"+Boolean.toString(results[0])+","+Boolean.toString(results[1])+","+Boolean.toString(results[2]));
			}
			for (String link : LinkTestResults.keySet()) 
			{
				System.out.println("Link:"+link);
				boolean result= LinkTestResults.get(link);
				if(!result)
					pass = false;
				System.out.println("Result:"+Boolean.toString(result));			
			}
			assertTrue(pass);
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
			
		}
	}
	
	@Test
	public void test11()
	{
		try
		{
			int testCaseIndex=10;
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[testCaseIndex]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[testCaseIndex]);

		    HashMap evals = evalStrMan.getEvaluations();
		    HashMap<String, boolean[]> NodeTestResults = new HashMap<String, boolean[]>();
		    //sv,color,warning
		    HashMap<String, Boolean> LinkTestResults = new HashMap<String, Boolean>();
		    
			for(int i =0; i < TestData.testCaseNodes[testCaseIndex].length;i++)
			{
				if(TestData.testCaseNodes[testCaseIndex][i]==null)
					break;
					
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[testCaseIndex][i]);
				
				boolean ig =evalStrMan.isIgnored(f);
				EvaluationImpl e = (EvaluationImpl) evals.get(f);
				int sv = e.getEvaluation();
				String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
			    String color = getColorFromRGB(colorRGB);
				EList meta = f.getMetadata();
				
				boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[0])==sv;
				boolean colorResult;
				if(color==null)
					colorResult=false;
				else
					// dont change array index here
					colorResult = color.equals(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[1]);
				
				boolean warningResult;
				
				if(meta.size()>=4)
				{
					String val= (String) meta.get(3);
					warningResult= TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals(val);		
				}
				else
				{

					warningResult=TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals("none");
				}
				NodeTestResults.put(f.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				//_userSetEvaluationWarning
				
			}
			 
			for(int i=0; i<TestData.testCaseLinks[testCaseIndex].length;i++)
			{
				boolean contributionValueResult;
				if(TestData.testCaseLinks[testCaseIndex][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult= TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult = TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
			}
		
			boolean pass = true;
			int testCaseNumber = testCaseIndex+1;
			System.out.println("Results for test # "+testCaseNumber);
			for (String node : NodeTestResults.keySet()) 
			{
				System.out.println("Node:"+node);
				boolean[] results= NodeTestResults.get(node);
				if(!results[0]||!results[1]||!results[2])
					pass =false;
				System.out.println("Results:"+Boolean.toString(results[0])+","+Boolean.toString(results[1])+","+Boolean.toString(results[2]));
			}
			for (String link : LinkTestResults.keySet()) 
			{
				System.out.println("Link:"+link);
				boolean result= LinkTestResults.get(link);
				if(!result)
					pass = false;
				System.out.println("Result:"+Boolean.toString(result));			
			}
			assertTrue(pass);
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
			
		}
	}

	@Test
	public void test12()
	{
		try
		{
			int testCaseIndex=11;
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[testCaseIndex]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[testCaseIndex]);

		    HashMap evals = evalStrMan.getEvaluations();
		    HashMap<String, boolean[]> NodeTestResults = new HashMap<String, boolean[]>();
		    //sv,color,warning
		    HashMap<String, Boolean> LinkTestResults = new HashMap<String, Boolean>();
		    
			for(int i =0; i < TestData.testCaseNodes[testCaseIndex].length;i++)
			{
				if(TestData.testCaseNodes[testCaseIndex][i]==null)
					break;
					
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[testCaseIndex][i]);
				
				boolean ig =evalStrMan.isIgnored(f);
				EvaluationImpl e = (EvaluationImpl) evals.get(f);
				int sv = e.getEvaluation();
				String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
			    String color = getColorFromRGB(colorRGB);
				EList meta = f.getMetadata();
				
				boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[0])==sv;
				boolean colorResult;
				if(color==null)
					colorResult=false;
				else
					// dont change array index here
					colorResult = color.equals(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[1]);
				
				boolean warningResult;
				
				if(meta.size()>=4)
				{
					String val= (String) meta.get(3);
					warningResult= TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals(val);		
				}
				else
				{

					warningResult=TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals("none");
				}
				NodeTestResults.put(f.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				//_userSetEvaluationWarning
				
			}
			 
			for(int i=0; i<TestData.testCaseLinks[testCaseIndex].length;i++)
			{
				boolean contributionValueResult;
				if(TestData.testCaseLinks[testCaseIndex][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult= TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult = TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
			}
		
			boolean pass = true;
			int testCaseNumber = testCaseIndex+1;
			System.out.println("Results for test # "+testCaseNumber);
			for (String node : NodeTestResults.keySet()) 
			{
				System.out.println("Node:"+node);
				boolean[] results= NodeTestResults.get(node);
				if(!results[0]||!results[1]||!results[2])
					pass =false;
				System.out.println("Results:"+Boolean.toString(results[0])+","+Boolean.toString(results[1])+","+Boolean.toString(results[2]));
			}
			for (String link : LinkTestResults.keySet()) 
			{
				System.out.println("Link:"+link);
				boolean result= LinkTestResults.get(link);
				if(!result)
					pass = false;
				System.out.println("Result:"+Boolean.toString(result));			
			}
			assertTrue(pass);
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
			
		}
	}

	@Test
	public void test13()
	{
		try
		{
			int testCaseIndex=12;
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[testCaseIndex]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[testCaseIndex]);

		    HashMap evals = evalStrMan.getEvaluations();
		    HashMap<String, boolean[]> NodeTestResults = new HashMap<String, boolean[]>();
		    //sv,color,warning
		    HashMap<String, Boolean> LinkTestResults = new HashMap<String, Boolean>();
		    
			for(int i =0; i < TestData.testCaseNodes[testCaseIndex].length;i++)
			{
				if(TestData.testCaseNodes[testCaseIndex][i]==null)
					break;
					
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[testCaseIndex][i]);
				
				boolean ig =evalStrMan.isIgnored(f);
				EvaluationImpl e = (EvaluationImpl) evals.get(f);
				int sv = e.getEvaluation();
				String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
			    String color = getColorFromRGB(colorRGB);
				EList meta = f.getMetadata();
				
				boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[0])==sv;
				boolean colorResult;
				if(color==null)
					colorResult=false;
				else
					// dont change array index here
					colorResult = color.equals(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[1]);
				
				boolean warningResult;
				
				if(meta.size()>=4)
				{
					String val= (String) meta.get(3);
					warningResult= TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals(val);		
				}
				else
				{

					warningResult=TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals("none");
				}
				NodeTestResults.put(f.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				//_userSetEvaluationWarning
				
			}
			 
			for(int i=0; i<TestData.testCaseLinks[testCaseIndex].length;i++)
			{
				boolean contributionValueResult;
				if(TestData.testCaseLinks[testCaseIndex][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult= TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult = TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
			}
		
			boolean pass = true;
			int testCaseNumber = testCaseIndex+1;
			System.out.println("Results for test # "+testCaseNumber);
			for (String node : NodeTestResults.keySet()) 
			{
				System.out.println("Node:"+node);
				boolean[] results= NodeTestResults.get(node);
				if(!results[0]||!results[1]||!results[2])
					pass =false;
				System.out.println("Results:"+Boolean.toString(results[0])+","+Boolean.toString(results[1])+","+Boolean.toString(results[2]));
			}
			for (String link : LinkTestResults.keySet()) 
			{
				System.out.println("Link:"+link);
				boolean result= LinkTestResults.get(link);
				if(!result)
					pass = false;
				System.out.println("Result:"+Boolean.toString(result));			
			}
			assertTrue(pass);
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
			
		}
	}

	@Test
	public void test14()
	{
		try
		{
			int testCaseIndex=13;
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[testCaseIndex]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[testCaseIndex]);

		    HashMap evals = evalStrMan.getEvaluations();
		    HashMap<String, boolean[]> NodeTestResults = new HashMap<String, boolean[]>();
		    //sv,color,warning
		    HashMap<String, Boolean> LinkTestResults = new HashMap<String, Boolean>();
		    
			for(int i =0; i < TestData.testCaseNodes[testCaseIndex].length;i++)
			{
				if(TestData.testCaseNodes[testCaseIndex][i]==null)
					break;
					
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[testCaseIndex][i]);
				
				boolean ig =evalStrMan.isIgnored(f);
				EvaluationImpl e = (EvaluationImpl) evals.get(f);
				int sv = e.getEvaluation();
				String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
			    String color = getColorFromRGB(colorRGB);
				EList meta = f.getMetadata();
				
				boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[0])==sv;
				boolean colorResult;
				if(color==null)
					colorResult=false;
				else
					// dont change array index here
					colorResult = color.equals(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[1]);
				
				boolean warningResult;
				
				if(meta.size()>=4)
				{
					String val= (String) meta.get(3);
					warningResult= TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals(val);		
				}
				else
				{

					warningResult=TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals("none");
				}
				NodeTestResults.put(f.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				//_userSetEvaluationWarning
				
			}
			 
			for(int i=0; i<TestData.testCaseLinks[testCaseIndex].length;i++)
			{
				boolean contributionValueResult;
				if(TestData.testCaseLinks[testCaseIndex][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult= TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult = TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
			}
		
			boolean pass = true;
			int testCaseNumber = testCaseIndex+1;
			System.out.println("Results for test # "+testCaseNumber);
			for (String node : NodeTestResults.keySet()) 
			{
				System.out.println("Node:"+node);
				boolean[] results= NodeTestResults.get(node);
				if(!results[0]||!results[1]||!results[2])
					pass =false;
				System.out.println("Results:"+Boolean.toString(results[0])+","+Boolean.toString(results[1])+","+Boolean.toString(results[2]));
			}
			for (String link : LinkTestResults.keySet()) 
			{
				System.out.println("Link:"+link);
				boolean result= LinkTestResults.get(link);
				if(!result)
					pass = false;
				System.out.println("Result:"+Boolean.toString(result));			
			}
			assertTrue(pass);
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
			
		}
	}

	@Test
	public void test15()
	{
		try
		{
			int testCaseIndex=14;
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[testCaseIndex]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[testCaseIndex]);

		    HashMap evals = evalStrMan.getEvaluations();
		    HashMap<String, boolean[]> NodeTestResults = new HashMap<String, boolean[]>();
		    //sv,color,warning
		    HashMap<String, Boolean> LinkTestResults = new HashMap<String, Boolean>();
		    
			for(int i =0; i < TestData.testCaseNodes[testCaseIndex].length;i++)
			{
				if(TestData.testCaseNodes[testCaseIndex][i]==null)
					break;
					
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[testCaseIndex][i]);
				
				boolean ig =evalStrMan.isIgnored(f);
				EvaluationImpl e = (EvaluationImpl) evals.get(f);
				int sv = e.getEvaluation();
				String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
			    String color = getColorFromRGB(colorRGB);
				EList meta = f.getMetadata();
				
				boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[0])==sv;
				boolean colorResult;
				if(color==null)
					colorResult=false;
				else
					// dont change array index here
					colorResult = color.equals(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[1]);
				
				boolean warningResult;
				
				if(meta.size()>=4)
				{
					String val= (String) meta.get(3);
					warningResult= TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals(val);		
				}
				else
				{

					warningResult=TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals("none");
				}
				NodeTestResults.put(f.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				//_userSetEvaluationWarning
				
			}
			 
			for(int i=0; i<TestData.testCaseLinks[testCaseIndex].length;i++)
			{
				boolean contributionValueResult;
				if(TestData.testCaseLinks[testCaseIndex][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult= TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult = TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
			}
		
			boolean pass = true;
			int testCaseNumber = testCaseIndex+1;
			System.out.println("Results for test # "+testCaseNumber);
			for (String node : NodeTestResults.keySet()) 
			{
				System.out.println("Node:"+node);
				boolean[] results= NodeTestResults.get(node);
				if(!results[0]||!results[1]||!results[2])
					pass =false;
				System.out.println("Results:"+Boolean.toString(results[0])+","+Boolean.toString(results[1])+","+Boolean.toString(results[2]));
			}
			for (String link : LinkTestResults.keySet()) 
			{
				System.out.println("Link:"+link);
				boolean result= LinkTestResults.get(link);
				if(!result)
					pass = false;
				System.out.println("Result:"+Boolean.toString(result));			
			}
			assertTrue(pass);
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
			
		}
	}
	
	@Test
	public void test16()
	{
		try
		{
			int testCaseIndex=15;
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[testCaseIndex]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[testCaseIndex]);

		    HashMap evals = evalStrMan.getEvaluations();
		    HashMap<String, boolean[]> NodeTestResults = new HashMap<String, boolean[]>();
		    //sv,color,warning
		    HashMap<String, Boolean> LinkTestResults = new HashMap<String, Boolean>();
		    
			for(int i =0; i < TestData.testCaseNodes[testCaseIndex].length;i++)
			{
				if(TestData.testCaseNodes[testCaseIndex][i]==null)
					break;
					
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[testCaseIndex][i]);
				
				boolean ig =evalStrMan.isIgnored(f);
				EvaluationImpl e = (EvaluationImpl) evals.get(f);
				int sv = e.getEvaluation();
				String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
			    String color = getColorFromRGB(colorRGB);
				EList meta = f.getMetadata();
				
				boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[0])==sv;
				boolean colorResult;
				if(color==null)
					colorResult=false;
				else
					// dont change array index here
					colorResult = color.equals(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[1]);
				
				boolean warningResult;
				
				if(meta.size()>=4)
				{
					String val= (String) meta.get(3);
					warningResult= TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals(val);		
				}
				else
				{

					warningResult=TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals("none");
				}
				NodeTestResults.put(f.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				//_userSetEvaluationWarning
				
			}
			 
			for(int i=0; i<TestData.testCaseLinks[testCaseIndex].length;i++)
			{
				boolean contributionValueResult;
				if(TestData.testCaseLinks[testCaseIndex][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult= TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult = TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
			}
		
			boolean pass = true;
			int testCaseNumber = testCaseIndex+1;
			System.out.println("Results for test # "+testCaseNumber);
			for (String node : NodeTestResults.keySet()) 
			{
				System.out.println("Node:"+node);
				boolean[] results= NodeTestResults.get(node);
				if(!results[0]||!results[1]||!results[2])
					pass =false;
				System.out.println("Results:"+Boolean.toString(results[0])+","+Boolean.toString(results[1])+","+Boolean.toString(results[2]));
			}
			for (String link : LinkTestResults.keySet()) 
			{
				System.out.println("Link:"+link);
				boolean result= LinkTestResults.get(link);
				if(!result)
					pass = false;
				System.out.println("Result:"+Boolean.toString(result));			
			}
			assertTrue(pass);
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
			
		}
	}

	@Test
	public void test17()
	{
		try
		{
			int testCaseIndex=16;
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[testCaseIndex]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[testCaseIndex]);

		    HashMap evals = evalStrMan.getEvaluations();
		    HashMap<String, boolean[]> NodeTestResults = new HashMap<String, boolean[]>();
		    //sv,color,warning
		    HashMap<String, Boolean> LinkTestResults = new HashMap<String, Boolean>();
		    
			for(int i =0; i < TestData.testCaseNodes[testCaseIndex].length;i++)
			{
				if(TestData.testCaseNodes[testCaseIndex][i]==null)
					break;
					
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[testCaseIndex][i]);
				
				boolean ig =evalStrMan.isIgnored(f);
				EvaluationImpl e = (EvaluationImpl) evals.get(f);
				int sv = e.getEvaluation();
				String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
			    String color = getColorFromRGB(colorRGB);
				EList meta = f.getMetadata();
				
				boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[0])==sv;
				boolean colorResult;
				if(color==null)
					colorResult=false;
				else
					// dont change array index here
					colorResult = color.equals(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[1]);
				
				boolean warningResult;
				
				if(meta.size()>=4)
				{
					String val= (String) meta.get(3);
					warningResult= TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals(val);		
				}
				else
				{

					warningResult=TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals("none");
				}
				NodeTestResults.put(f.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				//_userSetEvaluationWarning
				
			}
			 
			for(int i=0; i<TestData.testCaseLinks[testCaseIndex].length;i++)
			{
				boolean contributionValueResult;
				if(TestData.testCaseLinks[testCaseIndex][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult= TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult = TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
			}
		
			boolean pass = true;
			int testCaseNumber = testCaseIndex+1;
			System.out.println("Results for test # "+testCaseNumber);
			for (String node : NodeTestResults.keySet()) 
			{
				System.out.println("Node:"+node);
				boolean[] results= NodeTestResults.get(node);
				if(!results[0]||!results[1]||!results[2])
					pass =false;
				System.out.println("Results:"+Boolean.toString(results[0])+","+Boolean.toString(results[1])+","+Boolean.toString(results[2]));
			}
			for (String link : LinkTestResults.keySet()) 
			{
				System.out.println("Link:"+link);
				boolean result= LinkTestResults.get(link);
				if(!result)
					pass = false;
				System.out.println("Result:"+Boolean.toString(result));			
			}
			assertTrue(pass);
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
			
		}
	}
	
	@Test
	public void test18()
	{
		try
		{
			int testCaseIndex=17;
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[testCaseIndex]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[testCaseIndex]);

		    HashMap evals = evalStrMan.getEvaluations();
		    HashMap<String, boolean[]> NodeTestResults = new HashMap<String, boolean[]>();
		    //sv,color,warning
		    HashMap<String, Boolean> LinkTestResults = new HashMap<String, Boolean>();
		    
			for(int i =0; i < TestData.testCaseNodes[testCaseIndex].length;i++)
			{
				if(TestData.testCaseNodes[testCaseIndex][i]==null)
					break;
					
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[testCaseIndex][i]);
				
				boolean ig =evalStrMan.isIgnored(f);
				EvaluationImpl e = (EvaluationImpl) evals.get(f);
				int sv = e.getEvaluation();
				String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
			    String color = getColorFromRGB(colorRGB);
				EList meta = f.getMetadata();
				
				boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[0])==sv;
				boolean colorResult;
				if(color==null)
					colorResult=false;
				else
					// dont change array index here
					colorResult = color.equals(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[1]);
				
				boolean warningResult;
				
				if(meta.size()>=4)
				{
					String val= (String) meta.get(3);
					warningResult= TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals(val);		
				}
				else
				{

					warningResult=TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals("none");
				}
				NodeTestResults.put(f.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				//_userSetEvaluationWarning
				
			}
			 
			for(int i=0; i<TestData.testCaseLinks[testCaseIndex].length;i++)
			{
				boolean contributionValueResult;
				if(TestData.testCaseLinks[testCaseIndex][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult= TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult = TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
			}
		
			boolean pass = true;
			int testCaseNumber = testCaseIndex+1;
			System.out.println("Results for test # "+testCaseNumber);
			for (String node : NodeTestResults.keySet()) 
			{
				System.out.println("Node:"+node);
				boolean[] results= NodeTestResults.get(node);
				if(!results[0]||!results[1]||!results[2])
					pass =false;
				System.out.println("Results:"+Boolean.toString(results[0])+","+Boolean.toString(results[1])+","+Boolean.toString(results[2]));
			}
			for (String link : LinkTestResults.keySet()) 
			{
				System.out.println("Link:"+link);
				boolean result= LinkTestResults.get(link);
				if(!result)
					pass = false;
				System.out.println("Result:"+Boolean.toString(result));			
			}
			assertTrue(pass);
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
			
		}
	}
	
	@Test
	public void test19()
	{
		try
		{
			int testCaseIndex=18;
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[testCaseIndex]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[testCaseIndex]);

		    HashMap evals = evalStrMan.getEvaluations();
		    HashMap<String, boolean[]> NodeTestResults = new HashMap<String, boolean[]>();
		    //sv,color,warning
		    HashMap<String, Boolean> LinkTestResults = new HashMap<String, Boolean>();
		    
			for(int i =0; i < TestData.testCaseNodes[testCaseIndex].length;i++)
			{
				if(TestData.testCaseNodes[testCaseIndex][i]==null)
					break;
					
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[testCaseIndex][i]);
				
				boolean ig =evalStrMan.isIgnored(f);
				EvaluationImpl e = (EvaluationImpl) evals.get(f);
				int sv = e.getEvaluation();
				String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
			    String color = getColorFromRGB(colorRGB);
				EList meta = f.getMetadata();
				
				boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[0])==sv;
				boolean colorResult;
				if(color==null)
					colorResult=false;
				else
					// dont change array index here
					colorResult = color.equals(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[1]);
				
				boolean warningResult;
				
				if(meta.size()>=4)
				{
					String val= (String) meta.get(3);
					warningResult= TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals(val);		
				}
				else
				{

					warningResult=TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals("none");
				}
				NodeTestResults.put(f.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				//_userSetEvaluationWarning
				
			}
			 
			for(int i=0; i<TestData.testCaseLinks[testCaseIndex].length;i++)
			{
				boolean contributionValueResult;
				if(TestData.testCaseLinks[testCaseIndex][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult= TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult = TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
			}
		
			boolean pass = true;
			int testCaseNumber = testCaseIndex+1;
			System.out.println("Results for test # "+testCaseNumber);
			for (String node : NodeTestResults.keySet()) 
			{
				System.out.println("Node:"+node);
				boolean[] results= NodeTestResults.get(node);
				if(!results[0]||!results[1]||!results[2])
					pass =false;
				System.out.println("Results:"+Boolean.toString(results[0])+","+Boolean.toString(results[1])+","+Boolean.toString(results[2]));
			}
			for (String link : LinkTestResults.keySet()) 
			{
				System.out.println("Link:"+link);
				boolean result= LinkTestResults.get(link);
				if(!result)
					pass = false;
				System.out.println("Result:"+Boolean.toString(result));			
			}
			assertTrue(pass);
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
			
		}
	}
	
	@Test
	public void test20()
	{
		try
		{
			int testCaseIndex=19;
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[testCaseIndex]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[testCaseIndex]);

		    HashMap evals = evalStrMan.getEvaluations();
		    HashMap<String, boolean[]> NodeTestResults = new HashMap<String, boolean[]>();
		    //sv,color,warning
		    HashMap<String, Boolean> LinkTestResults = new HashMap<String, Boolean>();
		    
			for(int i =0; i < TestData.testCaseNodes[testCaseIndex].length;i++)
			{
				if(TestData.testCaseNodes[testCaseIndex][i]==null)
					break;
					
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[testCaseIndex][i]);
				
				boolean ig =evalStrMan.isIgnored(f);
				EvaluationImpl e = (EvaluationImpl) evals.get(f);
				int sv = e.getEvaluation();
				String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
			    String color = getColorFromRGB(colorRGB);
				EList meta = f.getMetadata();
				
				boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[0])==sv;
				boolean colorResult;
				if(color==null)
					colorResult=false;
				else
					// dont change array index here
					colorResult = color.equals(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[1]);
				
				boolean warningResult;
				
				if(meta.size()>=4)
				{
					String val= (String) meta.get(3);
					warningResult= TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals(val);		
				}
				else
				{

					warningResult=TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals("none");
				}
				NodeTestResults.put(f.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				//_userSetEvaluationWarning
				
			}
			 
			for(int i=0; i<TestData.testCaseLinks[testCaseIndex].length;i++)
			{
				boolean contributionValueResult;
				if(TestData.testCaseLinks[testCaseIndex][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult= TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult = TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
			}
		
			boolean pass = true;
			int testCaseNumber = testCaseIndex+1;
			System.out.println("Results for test # "+testCaseNumber);
			for (String node : NodeTestResults.keySet()) 
			{
				System.out.println("Node:"+node);
				boolean[] results= NodeTestResults.get(node);
				if(!results[0]||!results[1]||!results[2])
					pass =false;
				System.out.println("Results:"+Boolean.toString(results[0])+","+Boolean.toString(results[1])+","+Boolean.toString(results[2]));
			}
			for (String link : LinkTestResults.keySet()) 
			{
				System.out.println("Link:"+link);
				boolean result= LinkTestResults.get(link);
				if(!result)
					pass = false;
				System.out.println("Result:"+Boolean.toString(result));			
			}
			assertTrue(pass);
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
			
		}
	}
	
	@Test
	public void test21()
	{
		try
		{
			int testCaseIndex=20;
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[testCaseIndex]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[testCaseIndex]);

		    HashMap evals = evalStrMan.getEvaluations();
		    HashMap<String, boolean[]> NodeTestResults = new HashMap<String, boolean[]>();
		    //sv,color,warning
		    HashMap<String, Boolean> LinkTestResults = new HashMap<String, Boolean>();
		    
			for(int i =0; i < TestData.testCaseNodes[testCaseIndex].length;i++)
			{
				if(TestData.testCaseNodes[testCaseIndex][i]==null)
					break;
				 	
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[testCaseIndex][i]);
				EList meta;
				String color;
				int sv;
				if(f==null)
				{
					IntentionalElementImpl  ele= intentionalElementMap.get(TestData.testCaseNodes[testCaseIndex][i]);
					boolean ig =evalStrMan.isIgnored(ele);
					EvaluationImpl e = (EvaluationImpl) evals.get(ele);
					sv = e.getEvaluation();
					String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
				    color = getColorFromRGB(colorRGB);
					meta = ele.getMetadata();
					boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(testCaseIndex).get(ele.getName())[0])==sv;
					boolean colorResult;
					if(color==null)
						colorResult=false;
					else
						// dont change array index here
						colorResult = color.equals(TestData.expectedValuesForNodes.get(testCaseIndex).get(ele.getName())[1]);
					
					boolean warningResult;
					
					if(meta.size()>=4)
					{
						String val= (String) meta.get(3);
						warningResult= TestData.expectedValuesForNodes.get(testCaseIndex).get(ele.getName())[2].equals(val);		
					}
					else
					{

						warningResult=TestData.expectedValuesForNodes.get(testCaseIndex).get(ele.getName())[2].equals("none");
					}
					NodeTestResults.put(ele.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				}
				else
				{
					boolean ig =evalStrMan.isIgnored(f);
					EvaluationImpl e = (EvaluationImpl) evals.get(f);
					sv = e.getEvaluation();
					String colorRGB =IntentionalElementEditPart.determineColor(urn,f, e, ig , 7);
				    color = getColorFromRGB(colorRGB);
					meta = f.getMetadata();
					boolean satisfactionValueResult =Integer.parseInt(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[0])==sv;
					boolean colorResult;
					if(color==null)
						colorResult=false;
					else
						// dont change array index here
						colorResult = color.equals(TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[1]);
					
					boolean warningResult;
					
					if(meta.size()>=4)
					{
						String val= (String) meta.get(3);
						warningResult= TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals(val);		
					}
					else
					{

						warningResult=TestData.expectedValuesForNodes.get(testCaseIndex).get(f.getName())[2].equals("none");
					}
					NodeTestResults.put(f.getName(),new boolean[]{satisfactionValueResult,colorResult,warningResult});
				}
				
				//_userSetEvaluationWarning
				
			}
			 
			for(int i=0; i<TestData.testCaseLinks[testCaseIndex].length;i++)
			{
				boolean contributionValueResult;
				if(TestData.testCaseLinks[testCaseIndex][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult= TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[testCaseIndex][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[testCaseIndex][i]);
					contributionValueResult = TestData.expectedValuesForLinks.get(testCaseIndex).get(l.getName()).equals(l.getQuantitativeContribution());
					LinkTestResults.put(l.getName(), contributionValueResult);
				}
			}
		
			boolean pass = true;
			int testCaseNumber = testCaseIndex+1;
			System.out.println("Results for test # "+testCaseNumber);
			for (String node : NodeTestResults.keySet()) 
			{
				System.out.println("Node:"+node);
				boolean[] results= NodeTestResults.get(node);
				if(!results[0]||!results[1]||!results[2])
					pass =false;
				System.out.println("Results:"+Boolean.toString(results[0])+","+Boolean.toString(results[1])+","+Boolean.toString(results[2]));
			}
			for (String link : LinkTestResults.keySet()) 
			{
				System.out.println("Link:"+link);
				boolean result= LinkTestResults.get(link);
				if(!result)
					pass = false;
				System.out.println("Result:"+Boolean.toString(result));			
			}
			assertTrue(pass);
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
			
		}
	}
	//credit to  mkyong and www.mkyong.com
	public static void copyFolder(File src, File dest)
	    	throws IOException{
	 
	    	if(src.isDirectory()){
	 
	    		//if directory not exists, create it
	    		if(!dest.exists()){
	    		   dest.mkdir();
	    		   System.out.println("Directory copied from " 
	                              + src + "  to " + dest);
	    		}
	 
	    		//list all the directory contents
	    		String files[] = src.list();
	 
	    		for (String file : files) {
	    		   //construct the src and dest file structure
	    		   File srcFile = new File(src, file);
	    		   File destFile = new File(dest, file);
	    		   //recursive copy
	    		   copyFolder(srcFile,destFile);
	    		}
	 
	    	}else{
	    		//if file, then copy it
	    		//Use bytes stream to support all file types
	    		InputStream in = new FileInputStream(src);
	    	        OutputStream out = new FileOutputStream(dest); 
	 
	    	        byte[] buffer = new byte[1024];
	 
	    	        int length;
	    	        //copy the file content in bytes 
	    	        while ((length = in.read(buffer)) > 0){
	    	    	   out.write(buffer, 0, length);
	    	        }
	 
	    	        in.close();
	    	        out.close();
	    	        System.out.println("File copied from " + src + " to " + dest);
	    	}
	    }
	public static String getColorFromRGB(String rgb)
	{
		String[] all = rgb.split(",");
		String r = all[0];
		String g = all[1];
		String b = all[2];
		if(r.equals("169")&&g.equals("169")&&b.equals("169"))
		{
			return"grey";
		}
		if(r.equals("0")&&g.equals("255")&&b.equals("255"))
		{
			return "yellow";
		}
		if(r.equals("255")&&b.equals("96"))
		{
			return "yellow";
		}
		if(g.equals("255")&&b.equals("96"))
		{
			return "green";
		}
		return null;
	}
}
