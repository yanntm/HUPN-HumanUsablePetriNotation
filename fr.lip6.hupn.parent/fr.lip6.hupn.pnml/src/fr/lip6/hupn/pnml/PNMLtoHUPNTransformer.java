package fr.lip6.hupn.pnml;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

import fr.lip6.hupn.hUPN.HUPNFactory;
import fr.lip6.hupn.hUPN.PetriNet;
import fr.lip6.move.pnml.framework.general.PnmlImport;
import fr.lip6.move.pnml.framework.hlapi.HLAPIRootClass;
import fr.lip6.move.pnml.framework.utils.ModelRepository;
import fr.lip6.move.pnml.framework.utils.exception.InvalidIDException;
import fr.lip6.move.pnml.framework.utils.exception.VoidRepositoryException;
import fr.lip6.move.pnml.symmetricnet.hlcorestructure.hlapi.PetriNetDocHLAPI;

public class PNMLtoHUPNTransformer {

	
	public PetriNet importFromPNML (URI uri) throws IOException {
		long time = System.currentTimeMillis();
		PetriNet net = HUPNFactory.eINSTANCE.createPetriNet();
		
		final PnmlImport pim = new PnmlImport();
		try {
			ModelRepository.getInstance().createDocumentWorkspace(uri.getPath());
		} catch (final InvalidIDException e1) {
			e1.printStackTrace();
		}

		pim.setFallUse(true);
		HLAPIRootClass imported;
		try {
			imported = (HLAPIRootClass) pim.importFile(uri.getPath());
		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException(e);
		}
		getLog().info("Load time of PNML (colored model parsed with PNMLFW) : " + (System.currentTimeMillis() - time) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$

		final PetriNetDocHLAPI root = (PetriNetDocHLAPI) imported;

		assert(root.getNets().size()==1);

		// still wip
	//	ParameterBindingHelper.analyze(root.getNets().get(0));
		
		HUPNTransformer trans = new HUPNTransformer();
		try {
			net = trans.transform(root.getNets().get(0));
		} catch (ArithmeticException e) {
			throw new IOException("Annotations (e.g. markings) use too many bits cannot handle transformation accurately.");
		}		

		try {
			ModelRepository.getInstance().destroyCurrentWorkspace();
		} catch (VoidRepositoryException e) {
			e.printStackTrace();
			throw new IOException(e);
		}

		
		return net;
	}
	
	private static Logger getLog() {
		return Logger.getLogger("fr.lip6.move.gal");		
	}
}
