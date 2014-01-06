package pt.uminho.haslab.echo.transform.alloy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.qvtd.pivot.qvtrelation.RelationalTransformation;

import pt.uminho.haslab.echo.EchoError;
import pt.uminho.haslab.echo.EchoOptionsSetup;
import pt.uminho.haslab.echo.EchoReporter;
import pt.uminho.haslab.echo.EchoSolution;
import pt.uminho.haslab.echo.ErrorTransform;
import pt.uminho.haslab.echo.ErrorUnsupported;
import pt.uminho.haslab.echo.consistency.atl.ATLTransformation;
import pt.uminho.haslab.echo.consistency.qvt.QVTTransformation;
import pt.uminho.haslab.echo.emf.EchoParser;
import pt.uminho.haslab.echo.emf.URIUtil;
import pt.uminho.haslab.echo.transform.EchoTranslator;
import edu.mit.csail.sdg.alloy4.ConstList;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorSyntax;
import edu.mit.csail.sdg.alloy4compiler.ast.Attr;
import edu.mit.csail.sdg.alloy4compiler.ast.CommandScope;
import edu.mit.csail.sdg.alloy4compiler.ast.Expr;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprBinary;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprCall;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprConstant;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprITE;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprLet;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprList;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprQt;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprUnary;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprVar;
import edu.mit.csail.sdg.alloy4compiler.ast.Func;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig.Field;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig.PrimSig;
import edu.mit.csail.sdg.alloy4compiler.ast.VisitQuery;
import edu.mit.csail.sdg.alloy4compiler.translator.A4Solution;


public class AlloyEchoTranslator extends EchoTranslator {

    public AlloyEchoTranslator() {}

    //private static AlloyEchoTranslator instance = new AlloyEchoTranslator();

    public static AlloyEchoTranslator getInstance() {
        return (AlloyEchoTranslator) EchoTranslator.getInstance();
    }

    @Override
    public void writeAllInstances(EchoSolution solution, String metaModelUri, String modelUri) throws EchoError {
        writeAllInstances(((AlloyTuple) solution.getContents()).getSolution(),metaModelUri,modelUri,
                ((AlloyTuple)solution.getContents()).getState(modelUri));
    }

    @Override
    public void writeInstance(EchoSolution solution, String modelUri) throws EchoError {
    	PrimSig statesig = ((AlloyTuple)solution.getContents()).getState(modelUri);
        writeInstance(((AlloyTuple)solution.getContents()).getSolution(),modelUri,statesig);
    }

    @Override
    public String getMetaModelFromModelPath(String path) {
        return getModelStateSig(path).parent.label;

    }

    /** maps metamodels to the respective state signatures (should be "abstract")*/
	private Map<String,Expr> metamodelstatesigs = new HashMap<String,Expr>();
	/** maps instances to the respective state signatures (should be "one")*/
	private Map<String,PrimSig> modelstatesigs = new HashMap<String,PrimSig>();
	/** maps metamodel URIs to the respective Alloy translator*/
	private Map<String,ECore2Alloy> metamodelalloys = new HashMap<String,ECore2Alloy>();
	/** maps instance URIs to the respective Alloy translator*/
	private Map<String,XMI2Alloy> modelalloys = new HashMap<String,XMI2Alloy>();
	/** maps qvt-r URIs to the respective Alloy translator*/
	private Map<String,Transformation2Alloy> qvtalloys = new HashMap<String,Transformation2Alloy>();
	/** the initial command scopes of the target instance 
	 * only these need be increased in enforce mode, null if not enforce mode */
	private ConstList<CommandScope> scopes;
	/** the scope increment for each Sig, if in operation-based distance 
	 * null is GED */
	private Map<PrimSig,Integer> scopesincrement = new HashMap<PrimSig,Integer>();

	private Map<String,String> modelmetamodel = new HashMap<String,String>();
	
	/** the abstract top level state sig */
    static final PrimSig STATE;
    static{
    	PrimSig s = null;
    	try {s = new PrimSig(AlloyUtil.STATESIGNAME,Attr.ABSTRACT);}
    	catch (Err a){}
    	STATE = s;
    }


	/** Translates ECore meta-models to the respective Alloy specs.
	 * @param metaModel the meta-model to translate
	 */
	public void translateMetaModel(EPackage metaModel) throws EchoError {
		createModelStateSigs(metaModel);
		ECore2Alloy mmtrans = new ECore2Alloy(metaModel,(PrimSig) metamodelstatesigs.get(URIUtil.resolveURI(metaModel.eResource())));
		metamodelalloys.put(URIUtil.resolveURI(metaModel.eResource()),mmtrans);
		try {
			mmtrans.translate();
		} catch (EchoError e) {
			metamodelalloys.remove(URIUtil.resolveURI(metaModel.eResource()));
			throw e;
		}
	}

    /** Creates the metamodels abstract state signatures */
	private void createModelStateSigs(EPackage metamodel) throws EchoError {
		PrimSig s = null;
		try {
			//if (EchoOptionsSetup.getInstance().isOperationBased())
				s = new PrimSig(URIUtil.resolveURI(metamodel.eResource()),STATE);
			//else
				//s = new PrimSig(URIUtil.resolveURI(metamodel.eResource()),STATE,Attr.ABSTRACT);
			metamodelstatesigs.put(URIUtil.resolveURI(metamodel.eResource()), s);
		} catch (Err a) {throw new ErrorAlloy (a.getMessage()); }
	}
	
	/** Translates EObject models to the respective Alloy specs.
	 * @param model the model to translate
	 */
	public void translateModel(EObject model) throws EchoError {
		createInstanceStateSigs(model);
		String modeluri = URIUtil.resolveURI(model.eResource());
		String metamodeluri = EchoParser.getInstance().getMetamodelURI(model.eClass().getEPackage().getName());
		PrimSig state = modelstatesigs.get(URIUtil.resolveURI(model.eResource()));
		ECore2Alloy mmtrans = metamodelalloys.get(metamodeluri);	
		XMI2Alloy insttrans = new XMI2Alloy(model,mmtrans,state);
		modelmetamodel.put(modeluri, metamodeluri);
		modelalloys.put(modeluri,insttrans);
	}
	
    /** Creates the instances singleton state signatures */
	private void createInstanceStateSigs(EObject model) throws EchoError {
		String modeluri = model.eResource().getURI().toString();
		String metamodeluri = EchoParser.getInstance().getMetamodelURI(model.eClass().getEPackage().getName());
		try {
			String name = modeluri;
			PrimSig s = new PrimSig(name,(PrimSig) metamodelstatesigs.get(metamodeluri),Attr.ONE);
			modelstatesigs.put(modeluri, s);
		} catch (Err a) {throw new ErrorAlloy (a.getMessage()); }
	}
	
	/** Translates the QVT transformation to the respective Alloy specs
	 * @throws EchoError */
	public void translateQVT(RelationalTransformation qvt) throws EchoError {
		QVTTransformation q = new QVTTransformation(qvt);

		Transformation2Alloy qvtrans = new Transformation2Alloy(q);	
		String qvturi = URIUtil.resolveURI(qvt.eResource());
		qvtalloys.put(qvturi, qvtrans);
	}
	
	public boolean remQVT(String qvturi)  {
		qvtalloys.remove(qvturi);
		return true;
	}

    @Override
    public boolean hasMetaModel(String metaModelUri) {
        return getMetaModelStateSig(metaModelUri) != null;
    }

    @Override
    public boolean hasModel(String modelUri) {
        return (getModelStateSig(modelUri) != null) && modelalloys.containsKey(modelUri);
    }

    public void createScopesFromSizes(int overall, Map<Entry<String,String>,Integer> scopesmap, String uri) throws ErrorAlloy {
		Map<PrimSig,Integer> sc = new HashMap<PrimSig,Integer>();
		sc.put(Sig.STRING, overall);
		for (Entry<String,String> cla : scopesmap.keySet()) {
			if (cla.getKey().equals("") && cla.getValue().equals("String"))
				sc.put(PrimSig.STRING, scopesmap.get(cla));
			else {
				//EchoReporter.getInstance().debug(cla.getKey() + ", "+ metamodelalloys.keySet());
				ECore2Alloy e2a = metamodelalloys.get(cla.getKey());
				EClassifier eclass = e2a.epackage.getEClassifier(cla.getValue());
				PrimSig sig = e2a.getSigFromEClassifier(eclass);
				sc.put(sig, scopesmap.get(cla));
			}
		}
		scopes = AlloyUtil.createScope(new HashMap<PrimSig,Integer>(),sc);
	}
	
	public void createScopesFromOps(List<String> uris) throws ErrorAlloy {
		Map<PrimSig,Integer> scopesmap = new HashMap<PrimSig,Integer>();
		Map<PrimSig,Integer> scopesexact = new HashMap<PrimSig, Integer>();
		
		for (String uri : uris) {
			XMI2Alloy x2a = modelalloys.get(uri);
			ECore2Alloy e2a = x2a.translator;
	
			scopesincrement = new HashMap<Sig.PrimSig, Integer>();
			for (String cl : e2a.getCreationCount().keySet()) {
				EClassifier eclass = e2a.epackage.getEClassifier(cl);
				PrimSig sig = e2a.getSigFromEClassifier(eclass);
				scopesincrement.put(sig,e2a.getCreationCount().get(cl));
			}
			
			for (PrimSig sig : scopesincrement.keySet()) {
				int count = x2a.getClassSigs(sig)==null?0:x2a.getClassSigs(sig).size();
				if (scopesmap.get(sig) == null) scopesmap.put(sig, count);
				else scopesmap.put(sig, scopesmap.get(sig) + count);
				PrimSig up = sig.parent;
				while (up != Sig.UNIV && up != null){
					if (scopesmap.get(up) == null) scopesmap.put(up, count);
					else scopesmap.put(up, scopesmap.get(up) + count);
					up = up.parent;
				}
			}
	
			scopesincrement.put(e2a.sig_metamodel,1);
			//scopesincrement.put(PrimSig.STRING,1);
			
			Integer s = scopesexact.get(e2a.sig_metamodel);
			s = (s==null)?1:s+1;
			scopesexact.put(e2a.sig_metamodel,s);
		}

		scopes = AlloyUtil.createScope(scopesmap,scopesexact);
	}	
	
	public void createScopesFromURI(List<String> uris) throws ErrorAlloy {
		Map<PrimSig,Integer> scopesmap = new HashMap<PrimSig,Integer>();
		Map<PrimSig,Integer> exact = new HashMap<PrimSig,Integer>();
		for (String uri : uris) {
			XMI2Alloy x2a = modelalloys.get(uri);
			ECore2Alloy e2a = x2a.translator;
			
			for (PrimSig sig : e2a.getAllSigs()) {
				//System.out.println("SigMap: "+x2a.getSigMap());
				int count = x2a.getClassSigs(sig)==null?0:x2a.getClassSigs(sig).size();
				if (scopesmap.get(sig) == null) scopesmap.put(sig, count);
				else scopesmap.put(sig, scopesmap.get(sig) + count);
				PrimSig up = sig.parent;
				while (up != Sig.UNIV && up != null){
					if (scopesmap.get(up) == null) scopesmap.put(up, count);
					else scopesmap.put(up, scopesmap.get(up) + count);
					up = up.parent;
				}
			}
		}
		scopes = AlloyUtil.createScope(scopesmap,exact);
		//System.out.println(this.scopes);
	}	
	
	ConstList<CommandScope> incrementScopes (List<CommandScope> scopes) throws ErrorSyntax  {
		List<CommandScope> list = new ArrayList<CommandScope>();
		
		//System.out.println("incs: "+scopesincrement);
		System.out.println("scps: "+scopes);
		if (!EchoOptionsSetup.getInstance().isOperationBased())
			for (CommandScope scope : scopes) 
				list.add(new CommandScope(scope.sig, scope.isExact, scope.startingScope+1));
		else
			for (CommandScope scope : scopes) {				
				Integer i = scopesincrement.get(scope.sig);
				if (i == null) i = 0;
				list.add(new CommandScope(scope.sig, scope.isExact, scope.startingScope+i));
				// need to manage inheritance
			}		
		return ConstList.make(list);
	}
	
	/** Writes an Alloy solution in the target instance file 
	 * @throws ErrorAlloy 
	 * @throws ErrorTransform */
	private void writeInstance(A4Solution sol,String trguri, PrimSig targetstate) throws EchoError {
		XMI2Alloy inst = modelalloys.get(trguri);
		List<PrimSig> instsigs = inst.getAllSigs();
		EObject rootobj = inst.eobject;
		PrimSig rootsig = inst.getSigFromEObject(rootobj);
		writeXMIAlloy(sol,trguri,rootsig,targetstate,inst.translator,instsigs);
	}

	
	private void writeAllInstances(A4Solution sol, String metamodeluri, String modeluri, PrimSig state) throws EchoError {
		ECore2Alloy e2a = metamodelalloys.get(metamodeluri);
		List<EClass> rootclasses = e2a.getRootClass();
		if (rootclasses.size() != 1) throw new ErrorUnsupported("Could not resolve root class: "+rootclasses);
		PrimSig sig = e2a.getSigFromEClassifier(rootclasses.get(0));
		writeXMIAlloy(sol,modeluri,sig,state,e2a,null);
	}
	
	private void writeXMIAlloy(A4Solution sol, String uri, PrimSig rootatom, PrimSig state, ECore2Alloy trad,List<PrimSig> instsigs) throws EchoError {
		Alloy2XMI a2x = new Alloy2XMI(sol,rootatom,trad,state,instsigs);
		
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(
		    "*", new  XMIResourceFactoryImpl());

		Resource resource = resourceSet.createResource(URI.createURI(uri));
		resource.getContents().add(a2x.getModel());

		/*
		* Save the resource using OPTION_SCHEMA_LOCATION save option toproduce 
		* xsi:schemaLocation attribute in the document
		*/
		Map<Object,Object> options = new HashMap<Object,Object>();
		options.put(XMIResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);
		try{
		    resource.save(options);
	    }catch (Exception e) {
	    	throw new ErrorTransform(e.getMessage());
	    }
		
	}
	
	Expr getModelFact(String uri){
		if (modelalloys.get(uri) == null) return null;
		Expr fact = modelalloys.get(uri).getModelConstraint();
		EchoReporter.getInstance().debug("Model fact: "+fact);
		return fact;
	}

	Func getQVTFact(String uri) {
		//EchoReporter.getInstance().debug(uri + " over "+qvtalloys.keySet());
		if (qvtalloys.get(uri) == null) return null;
		return qvtalloys.get(uri).getTransformationConstraint();
	}
	
	
	public boolean hasQVT(String uri)
	{
		return qvtalloys.get(uri) != null;
	}
	
	ConstList<CommandScope> getScopes(){
		return scopes;
	}

	ConstList<CommandScope> getScopes(int strings) throws ErrorAlloy{
		List<CommandScope> aux = new ArrayList<CommandScope>();
		if (scopes != null)
			aux = new ArrayList<CommandScope>(scopes);
		try {
			aux.add(new CommandScope(PrimSig.STRING, true, strings));
		} catch (ErrorSyntax e) {
			throw new ErrorAlloy(e.getMessage());
		}
		scopes = ConstList.make(aux);
		return scopes;
	}



	public List<PrimSig> getEnumSigs(String metamodeluri){
		ECore2Alloy e2a = metamodelalloys.get(metamodeluri);
		List<PrimSig> aux = new ArrayList<PrimSig>(e2a.getEnumSigs());
		return aux;
	}	

	Expr getMetaModelStateSig(String metamodeluri){
       		return metamodelstatesigs.get(metamodeluri);
	}
	
	PrimSig getModelStateSig (String modeluri){
		return modelstatesigs.get(modeluri);
	}

	PrimSig getClassifierFromSig(EClassifier c) {
		if (c.getName().equals("EString")) return Sig.STRING;
		else if (c.getName().equals("EBoolean")) return Sig.NONE;
		else {
			ECore2Alloy e2a = metamodelalloys.get(c.getEPackage().eResource().getURI().path());
			return e2a.getSigFromEClassifier((EClass) c);
		}
	}

	PrimSig getSigFromClass(String metamodeluri, EClass eclass) {
		ECore2Alloy e2a = metamodelalloys.get(metamodeluri);
		return e2a.getSigFromEClassifier(eclass);
	}
	
	Field getStateFieldFromClass(String metamodeluri, EClass eclass) {
		ECore2Alloy e2a = metamodelalloys.get(metamodeluri);
		return e2a.getStateFieldFromClass(eclass);
	}
	
	Field getFieldFromFeature(String metamodeluri, EStructuralFeature f) {
		ECore2Alloy e2a = metamodelalloys.get(metamodeluri);
		return e2a.getFieldFromSFeature(f);
	}
	
	public List<PrimSig> getMetamodelSigs(String metamodeluri) throws ErrorAlloy{
		ECore2Alloy e2a = metamodelalloys.get(metamodeluri);
		
		List<PrimSig> aux = new ArrayList<PrimSig>(e2a.getAllSigs());
		
		return aux;
	}	
	
	public EStructuralFeature getESFeatureFromName(String pck, String cla, String fie) {
		ECore2Alloy e2a = metamodelalloys.get(pck);
		if (e2a == null) return null;
		EClass eclass = (EClass) e2a.epackage.getEClassifier(cla);
		return eclass.getEStructuralFeature(fie);
	}

	public EClassifier getEClassifierFromName(String pck, String cla) {
		ECore2Alloy e2a = metamodelalloys.get(pck);
		if (e2a == null) return null;
		return e2a.epackage.getEClassifier(cla);
	}

	Func getMetamodelDeltaExpr(String metamodeluri) throws ErrorAlloy {
		return metamodelalloys.get(metamodeluri).getDeltaSetFunc();
	}

	Func getMetamodelDeltaRelFunc(String metamodeluri) throws ErrorAlloy {
		return metamodelalloys.get(metamodeluri).getDeltaRelFunc();
	}

	public List<PrimSig> getInstanceSigs(String uri) {
		return modelalloys.get(uri).getAllSigs();
	}
	
	public String getModelMetamodel(String modeluri) {
		return modelmetamodel.get(modeluri);
	}
	
	Expr getConformsInstance(String uri) throws ErrorAlloy {
		Func f = modelalloys.get(uri).translator.getConforms();
		//EchoReporter.getInstance().debug("Model fact: "+f.getBody());
		return f.call(modelstatesigs.get(uri));
	}

	Expr getConformsInstance(String uri, PrimSig sig) throws ErrorAlloy {
		Func f = modelalloys.get(uri).translator.getConforms();
		return f.call(sig);
	}
	
	Expr getGenerateInstance(String metamodeluri, PrimSig sig) throws ErrorAlloy {
		Func f = metamodelalloys.get(metamodeluri).getGenerate();
		return f.call(sig);
	}

	//Unused!
	Expr getConformsAllInstances(String metamodeluri) throws ErrorAlloy {
		Func f = metamodelalloys.get(metamodeluri).getConforms();
		return f.call(metamodelstatesigs.get(metamodeluri));
	}
	
	public void remMetaModel(String metaModelUri) {
		metamodelalloys.remove(metaModelUri);
	}

	public void remModel(String modelUri) {
		modelalloys.remove(modelUri);
		modelstatesigs.remove(modelUri);
	}

	public List<EClass> getRootClass(String metamodeluri) {
		//EchoReporter.getInstance().debug("here "+metamodeluri +" at "+metamodelalloys.keySet());
		return metamodelalloys.get(metamodeluri).getRootClass();
	}

	/**
	 * returns true is able to determine determinism;
	 * false otherwise
	 * @param exp
	 * @return true if able to determine determinism, false otherwise
	 * @throws ErrorUnsupported 
	 */
	Boolean isFunctional(Expr exp) throws EchoError {
		IsFunctionalQuery q = new IsFunctionalQuery();
		try {
			return q.visitThis(exp);
		} catch (Err e1) { throw new ErrorUnsupported(e1.getMessage()); }
	}
	
	private final class IsFunctionalQuery extends VisitQuery<Boolean> {

		IsFunctionalQuery() {}
		@Override public final Boolean visit(ExprQt x) { return false; }

        @Override public final Boolean visit(ExprBinary x) throws Err {
			switch (x.op) {
				case JOIN : 
					//System.out.println("DEBUG FUNC JOIN: " + x.right + " is "+visitThis(x.right)+", "+x.left + " is "+visitThis(x.left));
					return (visitThis(x.right) && visitThis(x.left));
				default : return false;
			}
		}

        @Override public final Boolean visit(ExprCall x) { return false; }

        @Override public final Boolean visit(ExprList x) { return false; }

        @Override public final Boolean visit(ExprConstant x) { return false; }

        @Override public final Boolean visit(ExprITE x) { return false; }

        @Override public final Boolean visit(ExprLet x) { return false; }

        @Override public final Boolean visit(ExprUnary x) { return false; }

        @Override public final Boolean visit(ExprVar x) { return true; }

        @Override public final Boolean visit(Sig x) {
        	return x.attributes.contains(Attr.ONE);
        }

        @Override public final Boolean visit(Sig.Field x) {
        	String metamodeluri = AlloyUtil.getMetamodelURIfromExpr(x);
        	ECore2Alloy e2a = metamodelalloys.get(metamodeluri);
        	if (e2a == null) return false;
        	EStructuralFeature sf = e2a.getSFeatureFromField(x);
        	if (sf == null) return false;
        
        	if (sf instanceof EAttribute && !sf.getEType().getName().equals("EBoolean")) return true;
        	if (sf.getLowerBound() == 1 && sf.getUpperBound() == 1) return true;
        	return false;
       }
    }

	public void translateATL(EObject atl, EObject mdl1, EObject mdl2) throws EchoError {
		ATLTransformation a = new ATLTransformation(atl,mdl1,mdl2);

		Transformation2Alloy qvtrans = new Transformation2Alloy(a);	
		String qvturi = URIUtil.resolveURI(atl.eResource());
		qvtalloys.put(qvturi, qvtrans);
	}
}