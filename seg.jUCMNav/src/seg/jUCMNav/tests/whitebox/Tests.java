package seg.jUCMNav.tests.commands;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import grl.ActorRef;
import grl.Contribution;
import grl.Decomposition;
import grl.Dependency;
import grl.EvaluationStrategy;
import grl.GRLGraph;
import grl.GRLNode;
import grl.IntentionalElementRef;
import grl.IntentionalElementType;

import java.io.ByteArrayInputStream;
import java.util.Iterator;

import junit.framework.TestCase;

import org.eclipse.emf.common.util.EList;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import seg.jUCMNav.editors.UCMNavMultiPageEditor;
import seg.jUCMNav.model.ModelCreationFactory;

import seg.jUCMNav.model.commands.create.AddDependencyElementLinkCommand;
import seg.jUCMNav.model.commands.create.AddIntentionalElementRefCommand;
import seg.jUCMNav.model.commands.create.AddStandardElementLinkCommand;
import seg.jUCMNav.model.commands.create.CreateElementLinkCommand;
import seg.jUCMNav.model.commands.create.CreateGrlGraphCommand;
import seg.jUCMNav.model.commands.delete.DeleteMapCommand;
import seg.jUCMNav.model.util.ParentFinder;
import seg.jUCMNav.strategies.MockEvaluationStrategyManager;
import seg.jUCMNav.strategies.QualitativeGRLStrategyAlgorithm;
import seg.jUCMNav.views.preferences.DeletePreferences;
import ucm.map.UCMmap;
import urn.URNspec;
import urncore.IURNDiagram;

public class Tests extends TestCase implements IApplication{

	
	
	
	private EList<EvaluationStrategy> strategies;
	private MockEvaluationStrategyManager esm;
	
	private UCMNavMultiPageEditor editor;
    private CommandStack cs;

    private URNspec urnspec;
    private GRLGraph graph;
    private IntentionalElementRef ref;
    
    private boolean testBindings;
    
    @Before
    protected void setUp() throws Exception {
        super.setUp();

        testBindings = true;
        

        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IProject testproject = workspaceRoot.getProject("seg.JUCMNav"); //$NON-NLS-1$
        if (!testproject.exists())
            testproject.create(null);

        if (!testproject.isOpen())
            testproject.open(null);

        IFile testfile = testproject.getFile("test_file.jucm"); //$NON-NLS-1$

        // start with clean file
        if (testfile.exists())
            testfile.delete(true, false, null);

        testfile.create(new ByteArrayInputStream("".getBytes()), false, null); //$NON-NLS-1$

        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(testfile.getName());
        editor = (UCMNavMultiPageEditor) page.openEditor(new FileEditorInput(testfile), desc.getId());

        // generate a top level model element
        urnspec = editor.getModel();

        // cs = new CommandStack();
        cs = editor.getDelegatingCommandStack();

        // Delete the default UCM map, if present
        Command cmd;
        Object defaultMap = urnspec.getUrndef().getSpecDiagrams().get(0);
        if (defaultMap instanceof UCMmap) {
        	cmd = new DeleteMapCommand((UCMmap) defaultMap);
        	assertTrue("Can't execute DeleteMapCommand.", cmd.canExecute()); //$NON-NLS-1$
        	cs.execute(cmd);
        }

        // Create a new GRLGraph
        cmd = new CreateGrlGraphCommand(urnspec);
        graph = ((CreateGrlGraphCommand) cmd).getDiagram();
        assertTrue("Can't execute CreateGrlGraphCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);

        // Set the preferences for deleting the references to ALWAYS
        DeletePreferences.getPreferenceStore().setValue(DeletePreferences.PREF_DELDEFINITION, DeletePreferences.PREF_ALWAYS);
        DeletePreferences.getPreferenceStore().setValue(DeletePreferences.PREF_DELREFERENCE, DeletePreferences.PREF_ALWAYS);

    }

    @After
    protected void tearDown() throws Exception {
        super.tearDown();

        editor.doSave(null);

        // Verify the Actor References binding and executing undo/redo
        if (testBindings) {
            verifyBindings();
        }

        int i = cs.getCommands().length;

        if (cs.getCommands().length > 0) {
            assertTrue("Can't undo first command", cs.canUndo()); //$NON-NLS-1$
            cs.undo();
            editor.doSave(null);
            assertTrue("Can't redo first command", cs.canRedo()); //$NON-NLS-1$
            cs.redo();
            editor.doSave(null);
        }

        while (i-- > 0) {
            assertTrue("Can't undo a certain command", cs.canUndo()); //$NON-NLS-1$
            cs.undo();
        }

        editor.doSave(null);

        i = cs.getCommands().length;
        while (i-- > 0) {
            assertTrue("Can't redo a certain command", cs.canRedo()); //$NON-NLS-1$
            cs.redo();
        }

        if (testBindings) {
            verifyBindings();
        }

        editor.doSave(null);

        editor.closeEditor(false);

    }
    
    @Test
    public void testAddIntentionalElementRefCommand() { // reference to node
        ref = (IntentionalElementRef) ModelCreationFactory.getNewObject(urnspec, IntentionalElementRef.class, IntentionalElementType.SOFTGOAL);
       
        Command cmd;

        cmd = new AddIntentionalElementRefCommand(graph, ref);
        assertTrue("Can't execute AddIntentionalElementRefCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
    }
    
    @Test
    public void test1Node() { // figure 1 in document softgoal12, expected is softgoal12
        ref = (IntentionalElementRef) ModelCreationFactory.getNewObject(urnspec, IntentionalElementRef.class, IntentionalElementType.SOFTGOAL);
       
        Command cmd;

        cmd = new AddIntentionalElementRefCommand(graph, ref);
        assertTrue("Can't execute AddIntentionalElementRefCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        this.strategies = editor.getModel().getGrlspec().getStrategies();
        this.esm = MockEvaluationStrategyManager.getInstance();
        
        esm.setStrategy(this.strategies.get(0));
        QualitativeGRLStrategyAlgorithm a = new QualitativeGRLStrategyAlgorithm();
        a.init(esm.getEvaluationStrategy(), esm.getEvaluations());
        
        String nodeName = ref.getDef().getName();
        assertEquals(nodeName, a.nextNode().getName());		// --> returns the same node
        
        System.out.println("test1Node: Pass");
    }

	@Test
	public void test2NodesDependency() { // figure 2 in document  goal14 --) softgoal12, expected is goal 14
		
		testAddIntentionalElementRefCommand();
        IntentionalElementRef destination = (IntentionalElementRef) ModelCreationFactory.getNewObject(urnspec, IntentionalElementRef.class,
                IntentionalElementType.GOAL);
        Dependency dependency = (Dependency) ModelCreationFactory.getNewObject(urnspec, Dependency.class);
        // Create the second element
        Command cmd = new AddIntentionalElementRefCommand(graph, destination);
        assertTrue("Can't execute AddIntentionalElementRefCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);

        // Link the 2 elements
        cmd = new AddDependencyElementLinkCommand(urnspec, ref.getDef(), dependency);
        ((AddDependencyElementLinkCommand) cmd).setTarget(destination.getDef());
        assertTrue("Can't execute AddDependencyElementLinkCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        this.strategies = editor.getModel().getGrlspec().getStrategies();
        this.esm = MockEvaluationStrategyManager.getInstance();
        
        esm.setStrategy(this.strategies.get(0));
        QualitativeGRLStrategyAlgorithm a = new QualitativeGRLStrategyAlgorithm();
        a.init(esm.getEvaluationStrategy(), esm.getEvaluations());
        
        String dest = dependency.getDest().getName();
        String src = dependency.getSrc().getName();

        assertEquals(dest, ref.getDef().getName());
        assertTrue(a.hasNextNode());
        assertEquals(a.nextNode().getName(), src);	// --> gets the source node
        assertEquals(a.nextNode().getName(), dest);	// --> gets the target node
       
        System.out.println("test2NodesDependency: Pass");
	}

	@Test
	public void test3NodesDependency() { // Figure 3 in document softgoal12 (-- goal14 --) softgoal17, expected is goal14
		
		testAddIntentionalElementRefCommand();
        IntentionalElementRef destination = (IntentionalElementRef) ModelCreationFactory.getNewObject(urnspec, IntentionalElementRef.class,
                IntentionalElementType.GOAL);
        Dependency dependency = (Dependency) ModelCreationFactory.getNewObject(urnspec, Dependency.class);
        // Create the second element
        Command cmd = new AddIntentionalElementRefCommand(graph, destination);
        assertTrue("Can't execute AddIntentionalElementRefCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        // Link the 2 elements
        cmd = new AddDependencyElementLinkCommand(urnspec, ref.getDef(), dependency);
        ((AddDependencyElementLinkCommand) cmd).setTarget(destination.getDef());
        assertTrue("Can't execute AddDependencyElementLinkCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        IntentionalElementRef source2 = (IntentionalElementRef) ModelCreationFactory.getNewObject(urnspec, IntentionalElementRef.class,
                IntentionalElementType.SOFTGOAL);
        Dependency dependency2 = (Dependency) ModelCreationFactory.getNewObject(urnspec, Dependency.class);
        
        // Create the third element
        cmd = new AddIntentionalElementRefCommand(graph, source2);
        assertTrue("Can't execute AddIntentionalElementRefCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        // Link the 2 elements
        cmd = new AddDependencyElementLinkCommand(urnspec, source2.getDef(), dependency2);
        ((AddDependencyElementLinkCommand) cmd).setTarget(destination.getDef());
        assertTrue("Can't execute AddDependencyElementLinkCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        this.strategies = editor.getModel().getGrlspec().getStrategies();
        this.esm = MockEvaluationStrategyManager.getInstance();
        
        esm.setStrategy(this.strategies.get(0));
        QualitativeGRLStrategyAlgorithm a = new QualitativeGRLStrategyAlgorithm();
        a.init(esm.getEvaluationStrategy(), esm.getEvaluations());
        
        String dest2 = dependency.getDest().getName();
        String src = dependency.getSrc().getName();
        String dest = dependency2.getDest().getName();
        
        assertEquals(dest2, ref.getDef().getName());
        assertTrue(a.hasNextNode());
        assertEquals(a.nextNode().getName(), src);	// --> gets leaf which is the source for 2 links
        assertEquals(a.nextNode().getName(), dest);	// --> gets the first destination
        assertEquals(a.nextNode().getName(), dest2);	// --> gets the second destination
        
        System.out.println("test3NodesDependency: Pass");
	}

	@Test
	public void test3NodesDependencyOneNodeSeparate() {

		testAddIntentionalElementRefCommand();
        IntentionalElementRef destination = (IntentionalElementRef) ModelCreationFactory.getNewObject(urnspec, IntentionalElementRef.class,
                IntentionalElementType.GOAL);
        Dependency dependency = (Dependency) ModelCreationFactory.getNewObject(urnspec, Dependency.class);
        // Create the second element
        Command cmd = new AddIntentionalElementRefCommand(graph, destination);
        assertTrue("Can't execute AddIntentionalElementRefCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        // Link the 2 elements
        cmd = new AddDependencyElementLinkCommand(urnspec, ref.getDef(), dependency);
        ((AddDependencyElementLinkCommand) cmd).setTarget(destination.getDef());
        assertTrue("Can't execute AddDependencyElementLinkCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        IntentionalElementRef source2 = (IntentionalElementRef) ModelCreationFactory.getNewObject(urnspec, IntentionalElementRef.class,
                IntentionalElementType.SOFTGOAL);
        
        // Create the third element
        cmd = new AddIntentionalElementRefCommand(graph, source2);
        assertTrue("Can't execute AddIntentionalElementRefCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        this.strategies = editor.getModel().getGrlspec().getStrategies();
        this.esm = MockEvaluationStrategyManager.getInstance();
        
        esm.setStrategy(this.strategies.get(0));
        QualitativeGRLStrategyAlgorithm a = new QualitativeGRLStrategyAlgorithm();
        a.init(esm.getEvaluationStrategy(), esm.getEvaluations());
        
        String dest = dependency.getDest().getName();
        String src = dependency.getSrc().getName();
        String src2 = source2.getDef().getName();
        
        assertEquals(dest, ref.getDef().getName());
        assertTrue(a.hasNextNode());
        
	    assertEquals(a.nextNode().getName(), src);		// --> gets the first leaf node
	    assertEquals(a.nextNode().getName(), dest);		// --> gets the root corresponding to the leaf
	    assertEquals(a.nextNode().getName(), src2);		// --> gets the separate node
	
	    System.out.println("test3NodesDependencyOneNodeSeparate: Pass");
	}

    
	@Test
	public void test2NodesContribution() { // figure 4 in document softgoal12 --> goal14, expected is softgoal12

		testAddIntentionalElementRefCommand();
        IntentionalElementRef destination = (IntentionalElementRef) ModelCreationFactory.getNewObject(urnspec, IntentionalElementRef.class,
                IntentionalElementType.GOAL);
        Contribution contribution = (Contribution) ModelCreationFactory.getNewObject(urnspec, Contribution.class);
        // Create the second element
        Command cmd = new AddIntentionalElementRefCommand(graph, destination);
        assertTrue("Can't execute AddIntentionalElementRefCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);

        // Link the 2 elements
        cmd = new CreateElementLinkCommand(urnspec, ref.getDef(), contribution);
        ((CreateElementLinkCommand) cmd).setTarget(destination.getDef());
        assertTrue("Can't execute AddDependencyElementLinkCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        this.strategies = editor.getModel().getGrlspec().getStrategies();
        this.esm = MockEvaluationStrategyManager.getInstance();
        
        esm.setStrategy(this.strategies.get(0));
        QualitativeGRLStrategyAlgorithm a = new QualitativeGRLStrategyAlgorithm();
        a.init(esm.getEvaluationStrategy(), esm.getEvaluations());
        
        String dest = contribution.getDest().getName();
        String src = contribution.getSrc().getName();             

        assertEquals(src, ref.getDef().getName());
        assertTrue(a.hasNextNode());
        assertEquals(a.nextNode().getName(), src);  	// --> gets leaf
        assertEquals(a.nextNode().getName(), dest);  	// --> gets root
        
        System.out.println("test2NodesContribution: Pass");
	}
	
	@Test
	public void test3NodesContribution() { // figure 5 in document softgoal12 --> goal 14 <-- softgoal 17, expected is softgoal12

		testAddIntentionalElementRefCommand(); // creates SOFTGOAL: variable is called ref
        IntentionalElementRef destination = (IntentionalElementRef) ModelCreationFactory.getNewObject(urnspec, IntentionalElementRef.class,
                IntentionalElementType.GOAL);
        Contribution contribution = (Contribution) ModelCreationFactory.getNewObject(urnspec, Contribution.class);
        // Create the second element
        Command cmd = new AddIntentionalElementRefCommand(graph, destination);
        assertTrue("Can't execute AddIntentionalElementRefCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        // Link the 2 elements
        cmd = new CreateElementLinkCommand(urnspec, ref.getDef(), contribution);
        ((CreateElementLinkCommand) cmd).setTarget(destination.getDef());
        assertTrue("Can't execute AddDependencyElementLinkCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        IntentionalElementRef source2 = (IntentionalElementRef) ModelCreationFactory.getNewObject(urnspec, IntentionalElementRef.class,
                IntentionalElementType.SOFTGOAL);
        Contribution contribution2 = (Contribution) ModelCreationFactory.getNewObject(urnspec, Contribution.class);
        
        // Create the third element
        cmd = new AddIntentionalElementRefCommand(graph, source2);
        assertTrue("Can't execute AddIntentionalElementRefCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        // Link the 2 elements
        cmd = new CreateElementLinkCommand(urnspec, source2.getDef(), contribution2);
        ((CreateElementLinkCommand) cmd).setTarget(destination.getDef());
        assertTrue("Can't execute AddDependencyElementLinkCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        this.strategies = editor.getModel().getGrlspec().getStrategies();
        this.esm = MockEvaluationStrategyManager.getInstance();
        
        esm.setStrategy(this.strategies.get(0));
        QualitativeGRLStrategyAlgorithm a = new QualitativeGRLStrategyAlgorithm();
        a.init(esm.getEvaluationStrategy(), esm.getEvaluations());
        
        String dest = contribution.getDest().getName();
        String src = contribution.getSrc().getName();
        String src2 = contribution2.getSrc().getName();
        
        assertEquals(dest, destination.getDef().getName());

        assertTrue(a.hasNextNode());
        assertEquals(a.nextNode().getName(), src);		// --> gets the first leaf node
        assertEquals(a.nextNode().getName(), src2); 		// --> gets the second leaf node
        assertEquals(a.nextNode().getName(), dest);		// --> gets the root node
        
        System.out.println("test3NodesContribution: Pass");
	}
	
    public void test2NodesDecomposition() { // figure 6 in document goal14 --| softgoal12, expected is softgoal 12
    	testAddIntentionalElementRefCommand();
        IntentionalElementRef destination = (IntentionalElementRef) ModelCreationFactory.getNewObject(urnspec, IntentionalElementRef.class,
                IntentionalElementType.GOAL);
        Decomposition decomposition = (Decomposition) ModelCreationFactory.getNewObject(urnspec, Decomposition.class);

        // Create the second element
        Command cmd = new AddIntentionalElementRefCommand(graph, destination);
        assertTrue("Can't execute AddIntentionalElementRefCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);

        // Link the 2 elements

        cmd = new AddStandardElementLinkCommand(urnspec, ref.getDef(), decomposition);
        ((AddStandardElementLinkCommand) cmd).setTarget(destination.getDef());
        assertTrue("Can't execute AddStandardElementLinkCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        this.strategies = editor.getModel().getGrlspec().getStrategies();
        this.esm = MockEvaluationStrategyManager.getInstance();
      
        esm.setStrategy(this.strategies.get(0));
        QualitativeGRLStrategyAlgorithm a = new QualitativeGRLStrategyAlgorithm();
        a.init(esm.getEvaluationStrategy(), esm.getEvaluations());
        
        String src = decomposition.getSrc().getName();
        String dest = decomposition.getDest().getName();

        assertEquals(dest, destination.getDef().getName());
        assertTrue(a.hasNextNode());
        assertEquals(a.nextNode().getName(), src);		// --> gets the leaf
        assertEquals(a.nextNode().getName(), dest);		// --> gets the root
        
        System.out.println("test2NodesDecomposition: Pass");
    }
    
    @Test
	public void test3NodesDecomposition() { // figure 7 in document softgoal12 |-- goal14 --| softgoal 17, expected is softgoal12

		testAddIntentionalElementRefCommand(); // creates SOFTGOAL: variable is called ref
        IntentionalElementRef destination = (IntentionalElementRef) ModelCreationFactory.getNewObject(urnspec, IntentionalElementRef.class,
                IntentionalElementType.GOAL);
        Decomposition decomposition = (Decomposition) ModelCreationFactory.getNewObject(urnspec, Decomposition.class);
        // Create the second element
        Command cmd = new AddIntentionalElementRefCommand(graph, destination);
        assertTrue("Can't execute AddIntentionalElementRefCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        // Link the 2 elements
        cmd = new CreateElementLinkCommand(urnspec, ref.getDef(), decomposition);
        ((CreateElementLinkCommand) cmd).setTarget(destination.getDef());
        assertTrue("Can't execute AddDependencyElementLinkCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        IntentionalElementRef source2 = (IntentionalElementRef) ModelCreationFactory.getNewObject(urnspec, IntentionalElementRef.class,
                IntentionalElementType.SOFTGOAL);
        Decomposition decomposition2 = (Decomposition) ModelCreationFactory.getNewObject(urnspec, Decomposition.class);        
        
        // Create the third element
        cmd = new AddIntentionalElementRefCommand(graph, source2);
        assertTrue("Can't execute AddIntentionalElementRefCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        // Link the 2 elements
        cmd = new CreateElementLinkCommand(urnspec, source2.getDef(), decomposition2);
        ((CreateElementLinkCommand) cmd).setTarget(destination.getDef());
        assertTrue("Can't execute AddDependencyElementLinkCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        this.strategies = editor.getModel().getGrlspec().getStrategies();
        this.esm = MockEvaluationStrategyManager.getInstance();
        
        esm.setStrategy(this.strategies.get(0));
        QualitativeGRLStrategyAlgorithm a = new QualitativeGRLStrategyAlgorithm();
        a.init(esm.getEvaluationStrategy(), esm.getEvaluations());
        
        String dest = decomposition.getDest().getName();
        String src = decomposition.getSrc().getName();
        String src2 = decomposition2.getSrc().getName();
        
        assertEquals(dest, destination.getDef().getName());
        assertTrue(a.hasNextNode());
        
        assertEquals(a.nextNode().getName(), src);		// --> gets the first subtask
        assertEquals(a.nextNode().getName(), src2);		// --> gets the second subtask
        assertEquals(a.nextNode().getName(), dest);		// --> gets the main task
        
        System.out.println("test3NodesDecomposition: Pass");
	}	
	
    
	@Test
	public void test6NodesContribution() { // figure 5 in document softgoal12 --> goal 14 <-- softgoal 17, expected is softgoal12

		testAddIntentionalElementRefCommand(); // creates SOFTGOAL: variable is called ref
        IntentionalElementRef destination = (IntentionalElementRef) ModelCreationFactory.getNewObject(urnspec, IntentionalElementRef.class,
                IntentionalElementType.GOAL);
        Contribution contribution = (Contribution) ModelCreationFactory.getNewObject(urnspec, Contribution.class);
        // Create the second element
        Command cmd = new AddIntentionalElementRefCommand(graph, destination);
        assertTrue("Can't execute AddIntentionalElementRefCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        // Link the 2 elements
        cmd = new CreateElementLinkCommand(urnspec, ref.getDef(), contribution);
        ((CreateElementLinkCommand) cmd).setTarget(destination.getDef());
        assertTrue("Can't execute AddDependencyElementLinkCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        IntentionalElementRef source2 = (IntentionalElementRef) ModelCreationFactory.getNewObject(urnspec, IntentionalElementRef.class,
                IntentionalElementType.SOFTGOAL);
        Contribution contribution2 = (Contribution) ModelCreationFactory.getNewObject(urnspec, Contribution.class);
        
        // Create the third element
        cmd = new AddIntentionalElementRefCommand(graph, source2);
        assertTrue("Can't execute AddIntentionalElementRefCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        // Link the 2 elements
        cmd = new CreateElementLinkCommand(urnspec, source2.getDef(), contribution2);
        ((CreateElementLinkCommand) cmd).setTarget(destination.getDef());
        assertTrue("Can't execute AddDependencyElementLinkCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        IntentionalElementRef l1 = (IntentionalElementRef) ModelCreationFactory.getNewObject(urnspec, IntentionalElementRef.class,
                IntentionalElementType.SOFTGOAL);
        Contribution contributionl1 = (Contribution) ModelCreationFactory.getNewObject(urnspec, Contribution.class);
        
        cmd = new AddIntentionalElementRefCommand(graph, l1);
        assertTrue("Can't execute AddIntentionalElementRefCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        cmd = new CreateElementLinkCommand(urnspec, l1.getDef(), contributionl1);
        ((CreateElementLinkCommand) cmd).setTarget(ref.getDef());
        assertTrue("Can't execute AddDependencyElementLinkCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        IntentionalElementRef r1 = (IntentionalElementRef) ModelCreationFactory.getNewObject(urnspec, IntentionalElementRef.class,
                IntentionalElementType.SOFTGOAL);
        Contribution contributionr1 = (Contribution) ModelCreationFactory.getNewObject(urnspec, Contribution.class);
        
        cmd = new AddIntentionalElementRefCommand(graph, r1);
        assertTrue("Can't execute AddIntentionalElementRefCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        cmd = new CreateElementLinkCommand(urnspec, r1.getDef(), contributionr1);
        ((CreateElementLinkCommand) cmd).setTarget(source2.getDef());
        assertTrue("Can't execute AddDependencyElementLinkCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        IntentionalElementRef r2 = (IntentionalElementRef) ModelCreationFactory.getNewObject(urnspec, IntentionalElementRef.class,
                IntentionalElementType.SOFTGOAL);
        Contribution contributionr2 = (Contribution) ModelCreationFactory.getNewObject(urnspec, Contribution.class);
        
        cmd = new AddIntentionalElementRefCommand(graph, r2);
        assertTrue("Can't execute AddIntentionalElementRefCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        cmd = new CreateElementLinkCommand(urnspec, r2.getDef(), contributionr2);
        ((CreateElementLinkCommand) cmd).setTarget(source2.getDef());
        assertTrue("Can't execute AddDependencyElementLinkCommand.", cmd.canExecute()); //$NON-NLS-1$
        cs.execute(cmd);
        
        this.strategies = editor.getModel().getGrlspec().getStrategies();
        this.esm = MockEvaluationStrategyManager.getInstance();
        
        esm.setStrategy(this.strategies.get(0));
        QualitativeGRLStrategyAlgorithm a = new QualitativeGRLStrategyAlgorithm();
        a.init(esm.getEvaluationStrategy(), esm.getEvaluations());
        
        String dest = contribution.getDest().getName();
        String src = contribution.getSrc().getName();
        String src2 = contribution2.getSrc().getName();
        String lef1 = contributionl1.getSrc().getName();
        String ri1 = contributionr1.getSrc().getName();
        String ri2 = contributionr2.getSrc().getName();
        
        // Follows post order traversal 
        assertEquals(a.nextNode().getName(), lef1);		
        assertEquals(a.nextNode().getName(), src);		
        assertEquals(a.nextNode().getName(), ri1);
        assertEquals(a.nextNode().getName(), ri2);
        assertEquals(a.nextNode().getName(), src2);
        assertEquals(a.nextNode().getName(), dest);
        
        System.out.println("test6NodesContribution: Pass");
	}
	public void verifyBindings() {
        for (Iterator iter = urnspec.getUrndef().getSpecDiagrams().iterator(); iter.hasNext();) {
            IURNDiagram g = (IURNDiagram) iter.next();
            if (g instanceof GRLGraph) {
                GRLGraph graph = (GRLGraph) g;

                for (Iterator iter2 = graph.getContRefs().iterator(); iter2.hasNext();) {
                    ActorRef actor = (ActorRef) iter2.next();
                    assertEquals("ActorRef " + actor.toString() + " is not properly bound.", ParentFinder.getPossibleParent(actor), actor.getParent()); //$NON-NLS-1$ //$NON-NLS-2$

                }
                for (Iterator iter2 = graph.getNodes().iterator(); iter2.hasNext();) {
                    GRLNode gn = (GRLNode) iter2.next();
                    assertEquals("GRLNode " + gn.toString() + " is not properly bound.", ParentFinder.getPossibleParent(gn), gn.getContRef()); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }
    }

	@Override
	public Object start(IApplicationContext arg0) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

}
