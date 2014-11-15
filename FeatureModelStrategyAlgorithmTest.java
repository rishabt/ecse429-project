package ecsce429BlackBox;

import static org.junit.Assert.*;
import grl.EvaluationStrategy;
import grl.GRLGraph;
import grl.GRLNode;
import grl.LinkRef;

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
import org.eclipse.ui.IEditorDescriptor;
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
import ucm.map.UCMmap;
import urn.URNspec;
import urncore.IURNConnection;
import urncore.IURNDiagram;
import urncore.IURNNode;


public class FeatureModelStrategyAlgorithmTest extends TestCase  
{
	private static UCMNavMultiPageEditor editor;
    private static EList<IURNDiagram> diagrams;
    private static EList<EvaluationStrategy> strategies;
	
	private CommandStack cs;

    private URNspec urnspec;
    private GRLGraph graph;
    boolean testBindings;
	
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception 
	{
		try 
		{
			//getting workspace
			IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
			//getting project, if it exists 
			IProject testproject = workspaceRoot.getProject("testjucm"); //$NON-NLS-1$
			
			//if it does not exists create it 
			if (!testproject.exists())
	            testproject.create(null);
			
			//File location of external test project 
			File externalProject = new File("C:\\Users\\Bernie\\workspace\\testjucm");
	        File workspaceProject = new File("C:\\Users\\Bernie\\junit-workspace\\testjucm");
	        
	        //copy contents of external test project into newly created one(work around for eclipse not finding projects just copied into workspace)
	        copyFolder(externalProject, workspaceProject);

	        //refresh project just to reset everything
	        testproject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
	        IFile testfile = testproject.getFile("FRM.jucm");			

	        if (!testproject.isOpen())
	            testproject.open(null);  
	        
	        //get page, descriptor and editor 
	        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	        IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(testfile.getName());
	        editor = (UCMNavMultiPageEditor) page.openEditor(new FileEditorInput(testfile), desc.getId());
	        diagrams = editor.getModel().getUrndef().getSpecDiagrams();
	       strategies = editor.getModel().getGrlspec().getStrategies();
	      
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
//			//getting workspace
//			IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
//			//getting project, if it exists 
//			IProject testproject = workspaceRoot.getProject("testjucm"); //$NON-NLS-1$
//			
//			//if it does not exists create it 
//			if (!testproject.exists())
//	            testproject.create(null);
//			
//			//File location of external test project 
//			File externalProject = new File("C:\\Users\\Bernie\\workspace\\testjucm");
//	        File workspaceProject = new File("C:\\Users\\Bernie\\junit-workspace\\testjucm");
//	        
//	        //copy contents of external test project into newly created one(work around for eclipse not finding projects just copied into workspace)
//	        copyFolder(externalProject, workspaceProject);
//
//	        //refresh project just to reset everything
//	        testproject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
//	        IFile testfile = testproject.getFile("FRM.jucm");			
//
//	        if (!testproject.isOpen())
//	            testproject.open(null);  
//	        
//	        //get page, descriptor and editor 
//	        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
//	        IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(testfile.getName());
//	        editor = (UCMNavMultiPageEditor) page.openEditor(new FileEditorInput(testfile), desc.getId());
//
//	       EList<EvaluationStrategy> strategies = editor.getModel().getGrlspec().getStrategies();
//	       EList<IURNDiagram> diagrams = editor.getModel().getUrndef().getSpecDiagrams();
	
			
			EvaluationStrategyManager.getInstance().setStrategy(strategies.get(0));
	       EvaluationStrategyManager.getInstance().calculateEvaluation();
	       
	       for (IURNDiagram gram : diagrams) 
	       {
	    	 EList<GRLNode> nodes =  gram.getNodes();
	    	 //EList<IURNNode> nodes =  gram.getNodes();
	    	 //EList<IURNConnection> cons = gram.getConnections();
	    	 EList<LinkRef>cons = gram.getConnections();
	       }
	       //GRLNode nodes = (GRLNode) dias.getNodes();
	       //LinkRef links = (LinkRef) dias.getConnections();
	        // cs = new CommandStack();
	       cs = editor.getDelegatingCommandStack();
	        
			
			
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
