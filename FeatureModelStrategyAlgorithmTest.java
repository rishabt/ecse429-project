package ecsce429BlackBox;

import static org.junit.Assert.*;
import fm.impl.FeatureImpl;
import grl.Evaluation;
import grl.EvaluationStrategy;
import grl.GRLGraph;
import grl.GRLNode;
import grl.LinkRef;
import grl.impl.EvaluationImpl;

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

import seg.jUCMNav.editors.UCMNavMultiPageEditor;
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
    private static String testFileName="FRM.jucm";
	
	private static UCMNavMultiPageEditor editor;
    private static EList<IURNDiagram> diagrams;
    private static EList<EvaluationStrategy> strategies;
    private static EvaluationStrategyManager evalStrMan;
    private static HashMap<String, EvaluationStrategy> strategyMap;
    
    private static String[] test1NodeNames={"name","name2"};

	private CommandStack cs;

    private URNspec urnspec;
    private GRLGraph graph;
    boolean testBindings;
	
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception 
	{

		try 
		{
			IWorkbench wb = PlatformUI.getWorkbench();
			
			IPreferenceStore a = wb.getPreferenceStore();
			String b = a.getString("PREF_ALGORITHM");//i have no idea what going on here and how to set this 
			
			//getting workspace
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
	        diagrams = editor.getModel().getUrndef().getSpecDiagrams();
	        strategies = editor.getModel().getGrlspec().getStrategies();
	        evalStrMan = EvaluationStrategyManager.getInstance();
	      
	        strategyMap = new HashMap<String, EvaluationStrategy>();
	        for (EvaluationStrategy str : strategies) 
	        {
				strategyMap.put(str.getName(), str);
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

	@Test
	public void test1() 
	{
		try 
		{  
			EvaluationStrategy str = strategyMap.get("name");
		    evalStrMan.setStrategy(str);   
		    HashMap evals = evalStrMan.getEvaluations();
			Collection vals = evals.values();//EvaluationImpl
			Collection keys = evals.keySet();//FeatureImpl
			Object[] features =  keys.toArray();
			Object[] evalsArray = vals.toArray();
			
			HashMap<String,FeatureImpl> featureMap= new HashMap<String, FeatureImpl>();
			for(int i =0 ; i<features.length;i++)
			{
				FeatureImpl f = (FeatureImpl)features[i];
				featureMap.put(f.getName(),f );
			}

			EvaluationImpl evalOfFeature97 = (EvaluationImpl) evals.get(featureMap.get("Feature97"));
			int sv = evalOfFeature97.getEvaluation();
			assertEquals(sv, 100);
			

		}
		catch (Exception e) {
			// TODO: handle exception
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
