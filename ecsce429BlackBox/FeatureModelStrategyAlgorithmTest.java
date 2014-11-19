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
import grl.LinkRef;
import grl.impl.DecompositionImpl;
import grl.impl.EvaluationImpl;
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
	
	private static String externalTestProjectPath="C:\\Users\\Bernie\\workspace\\testjucm";
    private static String testProjectName="testjucm";
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
			
			//File location of external test project 
			File externalProject = new File(externalTestProjectPath);
	        File workspaceProject = new File(workspaceRoot.getRawLocation()+"\\"+testProjectName);//"C:\\Users\\Bernie\\junit-workspace\\testjucm");
	        
	        //copy contents of external test project into newly created one(work around for eclipse not finding projects just copied into workspace)
	        copyFolder(externalProject, workspaceProject);

	        //refresh project just to reset everything
	        testproject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
	        IFile testfile = testproject.getFile(testFileName);			

	        if (!testproject.isOpen())
	            testproject.open(null);  
	        
	        
	        //get page, descriptor and editor 
	        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	        IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(testfile.getName());
	        editor = (UCMNavMultiPageEditor) page.openEditor(new FileEditorInput(testfile), desc.getId());
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
			for(int i =0 ; i<features.length;i++)
			{
				FeatureImpl f = (FeatureImpl)features[i];
				String color = f.getFillColor();
				featureMap.put(f.getName(),f );
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

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

//	@Test
//	public void tryingTest() 
//	{
//		try 
//		{  
//			EvaluationStrategy str = strategyMap.get("name");
//		    evalStrMan.setStrategy(str);
//		    HashMap evals = evalStrMan.getEvaluations();
//		    FeatureDiagramImpl dia = diagramMap.get("FeatureDiagram96");
//		    EList nodes = dia.getNodes();
//		    
//		    Collection vals = evals.values();//EvaluationImpl
//			Collection keys = evals.keySet();//FeatureImpl
//			Object[] features =  keys.toArray();
//			Object[] evalsArray = vals.toArray();
//			
//			HashMap<String,FeatureImpl> featureMap= new HashMap<String, FeatureImpl>();
//			for(int i =0 ; i<features.length;i++)
//			{
//				FeatureImpl f = (FeatureImpl)features[i];
//				String color = f.getFillColor();
//				featureMap.put(f.getName(),f );
//			}
//
//			FeatureImpl F121 =featureMap.get("Feature76");
//			
//			evalStrMan.getEvaluation(F121);
//			
//			
//			//EList meta = editor.getModel().getUrndef().getUrnspec().getMetadata();
//	        
//			EList meta = F121.getMetadata();//this works
//			//_userSetEvaluationWarning
//			//_autoSelected
//			EvaluationImpl evalOfFeature97 = (EvaluationImpl) evals.get(F121);
//			int sv = evalOfFeature97.getEvaluation();
//			assertEquals(sv, 100);
//			
//
//		}
//		catch (Exception e) {
//			// TODO: handle exception
//			fail();
//		}
//	}

	@Test
	public void test1()
	{
		try
		{
			
			EvaluationStrategy str = strategyMap.get(TestData.testCaseStrategy[0]);
		    evalStrMan.setStrategy(str);

		    FeatureDiagramImpl dia = diagramMap.get(TestData.testCaseDiagrams[0]);
		    
//		    GrlGraphicalEditPartFactory fac = new GrlGraphicalEditPartFactory(dia);
//		    EditPart refresher = fac.createEditPart((EditPart) editor, editor.getModel());
//		    refresher.refresh();
		    
		    HashMap evals = evalStrMan.getEvaluations();
		    ArrayList<Boolean> resultsNodes = new ArrayList<Boolean>(); 
			for(int i =0; i < TestData.testCaseNodes[0].length;i++)
			{
				if(TestData.testCaseNodes[0][i]==null)
					break;
					
				FeatureImpl f =featureMap.get(TestData.testCaseNodes[0][i]);
				evalStrMan.getEvaluation(f);
				EvaluationImpl e = (EvaluationImpl) evals.get(f);
				int sv = e.getEvaluation();
				String color = f.getFillColor();
				EList meta = f.getMetadata();
				
				boolean r =Integer.parseInt(TestData.expectedValuesForNodes.get(0).get(f.getName())[0])==sv;
				boolean r2;
				if(color==null)
					r2=false;
				else
					r2 = color.equals(TestData.expectedValuesForNodes.get(0).get(f.getName())[1]);
				resultsNodes.add(r);
				resultsNodes.add(r2);
				boolean r3;
				
				if(meta.size()<3)
				{
					r3=TestData.expectedValuesForNodes.get(0).get(f.getName())[2].equals("none");
					resultsNodes.add(r3);
					//assertEquals("none", TestData.expectedValuesForNodes.get(0).get(f.getName())[2]);
					
				}
				else
				{
					String val= (String) meta.get(3);
					r3= TestData.expectedValuesForNodes.get(0).get(f.getName())[2].equals(val);
					resultsNodes.add(r3);
					//assertEquals(val, TestData.expectedValuesForNodes.get(0).get(f.getName())[2]);
					
				}
				//_userSetEvaluationWarning
				
			}
			ArrayList<Boolean> resultsLinks = new ArrayList<Boolean>(); 
			for(int i=0; i<TestData.testCaseLinks[0].length;i++)
			{
				if(TestData.testCaseLinks[0][i]==null)
					break;
				if(mandatoryLinkMap.containsKey(TestData.testCaseLinks[0][i]))
				{
					MandatoryFMLinkImpl l = mandatoryLinkMap.get(TestData.testCaseLinks[0][i]);
					//assertEquals(l.getQuantitativeContribution(),TestData.expectedValuesForNodes.get(0).get(l.getName()));
					boolean r= TestData.expectedValuesForNodes.get(0).get(l.getName()).equals(l.getQuantitativeContribution());
					resultsLinks.add(r);
				}
				if(optionalLinkMap.containsKey(TestData.testCaseLinks[0][i]))
				{
					OptionalFMLinkImpl l = optionalLinkMap.get(TestData.testCaseLinks[0][i]);
					boolean r = TestData.expectedValuesForNodes.get(0).get(l.getName()).equals(l.getQuantitativeContribution());
					resultsLinks.add(r);
					//assertEquals(l.getQuantitativeContribution(),TestData.expectedValuesForNodes.get(0).get(l.getName()));
				}
			}
		
			for (Boolean boolean1 : resultsLinks) {
				assertTrue(boolean1);
			}
			for (Boolean boolean1 : resultsNodes) {
				assertTrue(boolean1);
			}
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
}
