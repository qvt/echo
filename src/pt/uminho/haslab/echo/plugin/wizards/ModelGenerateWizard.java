package pt.uminho.haslab.echo.plugin.wizards;

import java.util.HashMap;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import pt.uminho.haslab.echo.EchoRunner;
import pt.uminho.haslab.echo.ErrorAlloy;
import pt.uminho.haslab.echo.ErrorTransform;
import pt.uminho.haslab.echo.plugin.EchoPlugin;
import pt.uminho.haslab.echo.plugin.views.AlloyModelView;

public class ModelGenerateWizard extends Wizard implements INewWizard {

	private ModelGenerateWizardPage page;
	
	private String metamodel;
	private Shell shell;
	
	
	public ModelGenerateWizard(String metamodel)
	{
		super();
		this.metamodel = metamodel;
	}
	
	@Override
	public void addPages()
	{
		page = new ModelGenerateWizardPage(metamodel);
		addPage(page);
	}
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {

		shell = workbench.getModalDialogShellProvider().getShell();
		/*Object firstElement = selection.getFirstElement();
		
		if(firstElement instanceof IFile)
		{	
			IFile res = (IFile) firstElement;
			String qvt = res.getRawLocation().toString();
			page = new RelationPage(qvt);
			addPage(page);
		//}*/
	}

	
	
	@Override
	public boolean performFinish() {
		EchoRunner er = EchoPlugin.getInstance().getEchoRunner();
		
		try {
			Map<Entry<String,String>,Integer> scopes = new HashMap<Entry<String,String>,Integer>();
			String[] args = page.getScopes().split(" ");
			if (args != null) {
				for (int i = 0; i < args.length ; i++) {
					scopes.put(new SimpleEntry<String,String>(er.parser.getModelsFromUri(metamodel).getName(),args[i]),Integer.parseInt(args[++i]));					
				}
			}			
			er.generate(metamodel, scopes);
			
			AlloyModelView amv = EchoPlugin.getInstance().getAlloyView();
			amv.refresh();
			amv.setPathToWrite(page.getPath());
			amv.setMetamodel(metamodel);
		} catch (ErrorAlloy | ErrorTransform /*| ErrorUnsupported | ErrorParser*/ e) {
			MessageDialog.openInformation(shell, "Error generating instance", e.getMessage());
		}
		return true;
	}

}