package fr.lip6.move.gal.pnml.togal.popup.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import fr.lip6.hupn.pnml.PNMLtoHUPNTransformer;
import fr.lip6.hupn.serialization.SerializationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class ImportFromPNMLToGAL implements IObjectActionDelegate {


	private List<IFile> files = new ArrayList<IFile>();

	/**
	 * Constructor for Action1.
	 */
	public ImportFromPNMLToGAL() {
		super();
	}

	private Shell shell;
	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		setShell(targetPart.getSite().getShell());
	}
	public void setShell(Shell shell) {
		this.shell = shell;
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		// ConsoleAdder.startConsole();
		String filenames = "" ;
		for (IFile file : files) {

			try {
				PNMLtoHUPNTransformer trans = new PNMLtoHUPNTransformer();
				fr.lip6.hupn.hUPN.PetriNet spec = trans.importFromPNML(file.getLocationURI());
				SerializationUtil.systemToFile(spec,file.getLocationURI().getPath().toString() +".hupn");
			} catch (Exception e) {
				MessageDialog.openInformation(
						shell,
						"PNMLToHUPN",
						"ImportToHUPN failed." + 			e.getMessage());
				getLog().warning("ImportToHUPN failed." + 			e.getMessage());
				e.printStackTrace();
			}


			getLog().info("ImportToHUPN was executed on file : " + file.getName());
			filenames += file.getName() + "; ";
		}
		MessageDialog.openInformation(
				shell,
				"ImportToHUPN",
				"ImportToHUPN was executed on files : " + filenames + " You might need to click 'File->Refresh' on the folder containing your PNML file to see them.");
	}



	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		files .clear();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ts = (IStructuredSelection) selection;
			for (Object s : ts.toList()) {
				if (s instanceof IFile) {
					IFile file = (IFile) s;
					if (file.getFileExtension().equals("pnml")) {
						this.files.add(file);
					}
				}
			}
		}
	}

	
	private static Logger getLog() {
		return Logger.getLogger("fr.lip6.move.gal");
	}

}
