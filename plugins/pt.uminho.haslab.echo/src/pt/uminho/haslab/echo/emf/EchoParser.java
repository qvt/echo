package pt.uminho.haslab.echo.emf;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.ocl.examples.pivot.delegate.OCLDelegateDomain;
import org.eclipse.ocl.examples.pivot.delegate.OCLInvocationDelegateFactory;
import org.eclipse.ocl.examples.pivot.delegate.OCLSettingDelegateFactory;
import org.eclipse.ocl.examples.pivot.delegate.OCLValidationDelegateFactory;
import org.eclipse.ocl.examples.pivot.model.OCLstdlib;
import org.eclipse.ocl.examples.pivot.utilities.PivotUtil;
import org.eclipse.ocl.examples.xtext.base.utilities.BaseCSResource;
import org.eclipse.ocl.examples.xtext.base.utilities.CS2PivotResourceAdapter;
import org.eclipse.qvtd.pivot.qvtrelation.RelationModel;
import org.eclipse.qvtd.pivot.qvtrelation.RelationalTransformation;
import org.eclipse.qvtd.xtext.qvtrelation.QVTrelationStandaloneSetup;
import org.eclipse.xtext.resource.XtextResourceSet;

import pt.uminho.haslab.echo.EchoOptionsSetup;
import pt.uminho.haslab.echo.ErrorParser;
import pt.uminho.haslab.echo.ErrorTransform;

public class EchoParser {
	//TODO: there should be some helper classes for stuff like getRootClass, which are totally independent from kodkod or alloy
	private static final EchoParser instance = new EchoParser();
		
	public static EchoParser getInstance() {
		return instance;
	}

	/** the ECore resource set */
	private XtextResourceSet resourceSet = new XtextResourceSet();
	/** the loaded metamodels */
	private Map<String,EPackage> metamodels = new HashMap<String,EPackage>();
	/** the loaded instances (key is the XMI file uri) */
	private Map<String,EObject> models = new HashMap<String,EObject>();
	/** the loaded QVT-R transformation */
	private Map<String,RelationalTransformation> transformations = new HashMap<String, RelationalTransformation>();

	private Map<String,String> metamodelnames = new HashMap<String,String>();
	
	private EchoParser(){	
		if (EchoOptionsSetup.getInstance().isStandalone()) {

            // install the OCL standard library
            OCLstdlib.install();
            resourceSet.getResourceFactoryRegistry()
             .getExtensionToFactoryMap().put("xmi",
                             new XMIResourceFactoryImpl());
			 resourceSet.getResourceFactoryRegistry()
             .getExtensionToFactoryMap().put(
                     "ecore", new EcoreResourceFactoryImpl());
			OCLstdlib.install();		
			QVTrelationStandaloneSetup.doSetup();
		}
	}
	
	/**
	 * Loads the EObject its uri
	 * @throws ErrorParser 
	 */
	public EObject loadModel(String uri) throws ErrorParser {
		Resource load_resource = resourceSet.getResource(URI.createURI(uri), true);
		load_resource.unload();
		try {
			load_resource.load(resourceSet.getLoadOptions());
		} catch (IOException e) {
			//throw new ErrorParser(e.getMessage());
		}
		EObject res = load_resource.getContents().get(0);
		models.put(uri,res);

		return res;
	}
	
	public EObject remModel(String uri) {
		return models.remove(uri);
	}

	/**
	 * Loads the EPackages its uri
	 */
	public EPackage loadMetamodel(String uri) throws ErrorParser{
		Resource load_resource = null;
		if (!EchoOptionsSetup.getInstance().isStandalone())
			load_resource = resourceSet.getResource(URI.createURI(uri),true);
		else {
			File file = new File(uri);
			try {
				load_resource = resourceSet.getResource(URI.createFileURI(file.getCanonicalPath()),true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		load_resource.unload();
		try {
			load_resource.load(resourceSet.getLoadOptions());
		} catch (IOException e) {
			throw new ErrorParser(e.getMessage());
		}
		EPackage res = (EPackage) load_resource.getContents().get(0);

		if (res.getNsURI() == null) throw new ErrorParser("nsUri required");
		
		resourceSet.getPackageRegistry().put(res.getNsURI(),res);
		
		if (metamodelnames.get(res.getName()) != null && !metamodelnames.get(res.getName()).equals(uri)) {
			throw new ErrorParser("A metamodel with the same name is already tracked.");
		}
		metamodelnames.put(res.getName(), uri);
		metamodels.put(uri,res);			
		
		return res;
	}
	
	public EPackage remMetamodel(String uri) {
		EPackage res = metamodels.remove(uri);	
		return res;
	}
	
	public String getMetamodelURI(String name) {
		return metamodelnames.get(name);
	}
	
	/**
	 * Loads the QVT specification from the CLI argument
	 * @throws ErrorTransform 
	 */
	public RelationalTransformation loadQVT(String uri) throws ErrorParser, ErrorTransform {
		Resource pivotResource;
        try{
        	CS2PivotResourceAdapter adapter = null;
        	URI inputURI;
        	if (EchoOptionsSetup.getInstance().isStandalone())
        		inputURI = URI.createURI(uri);
        	else 
        		inputURI = URI.createPlatformResourceURI(uri,true);
            BaseCSResource xtextResource = (BaseCSResource) resourceSet.getResource(inputURI, true);
            
            xtextResource.unload();
    		try {
    			xtextResource.load(resourceSet.getLoadOptions());
    		} catch (IOException e) {
    			throw new ErrorParser(e.getMessage());
    		}
            
    		adapter = xtextResource.getCS2ASAdapter(null);
            //adapter = BaseCSResource.getCS2ASAdapter(xtextResource, null);
            pivotResource = adapter.getASResource(xtextResource);
            pivotResource.setURI(URI.createURI(uri));
            String message = PivotUtil.formatResourceDiagnostics(pivotResource.getErrors(), "Error parsing QVT.", "\n\t");
			if (message != null) throw new ErrorParser (message,"QVT Parser");
			
		} catch (Exception e) { throw new ErrorParser (e.getMessage(),"QVT Parser");}

        try{	
			RelationModel rm = (RelationModel) pivotResource.getContents().get(0);
			RelationalTransformation transformation = (RelationalTransformation) rm.eContents().get(0);
		
			transformations.put(uri, transformation);
			return transformation;
		} catch (Exception e) { throw new ErrorTransform(e.getMessage());}
		
	}

	public RelationalTransformation getTransformation(String uri){
		return transformations.get(uri);
	}

	public EObject getModelFromUri(String uri){
		return models.get(uri);
	}



	public String backUpTarget(String uri){
		StringBuilder sb = new StringBuilder(uri);
		sb.insert(sb.length()-4,".old");

		XMIResource resource = (XMIResource) resourceSet.createResource(URI.createURI(sb.toString()));
		resource.getContents().add(getModelFromUri(uri));

		Map<Object,Object> options = new HashMap<Object,Object>();
		options.put(XMIResource.OPTION_SCHEMA_LOCATION, true);
		try{
			resource.save(options);
		}catch (IOException e) {
			e.printStackTrace();
		}
		
		return sb.toString();
		
	}

}