package pt.uminho.haslab.echo.transform.kodkod;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.qvtd.pivot.qvtrelation.RelationalTransformation;

import pt.uminho.haslab.echo.*;
import pt.uminho.haslab.echo.emf.EchoParser;
import pt.uminho.haslab.echo.emf.URIUtil;
import pt.uminho.haslab.echo.transform.EchoTranslator;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: tmg
 * Date: 10/24/13
 * Time: 12:41 PM
 */
public class KodkodEchoTranslator extends EchoTranslator {
    public KodkodEchoTranslator(){}

    /** maps meta-models Uris into translators*/
    private Map<String,Ecore2Kodkod> metaModels = new HashMap<>();
    /** maps models Uris into translators*/
    private Map<String,XMI2Kodkod> models = new HashMap<>();
    /** maps models Uris into meta-models Uris*/
    private Map<String,String> model2metaModel = new HashMap<>();

    @Override
    public void writeAllInstances(EchoSolution solution, String metaModelUri, String modelUri) throws ErrorTransform, ErrorUnsupported, ErrorInternalEngine {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void writeInstance(EchoSolution solution, String modelUri) throws ErrorInternalEngine, ErrorTransform {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getMetaModelFromModelPath(String path) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void translateMetaModel(EPackage metaModel) throws ErrorUnsupported, ErrorInternalEngine, ErrorTransform, ErrorParser {
        //TODO: Register meta-models already parsed.

        Ecore2Kodkod e2k = new Ecore2Kodkod(metaModel);
        metaModels.put(URIUtil.resolveURI(metaModel.eResource()), e2k);
        try {
            e2k.translate();
        } catch (EchoError e) {
            metaModels.remove(URIUtil.resolveURI(metaModel.eResource()));
            throw e;
        }
    }

    @Override
    public void remMetaModel(String metaModelUri) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void translateModel(EObject model) throws EchoError {
        String modelUri = URIUtil.resolveURI(model.eResource());
        String metaModelURI = EchoParser.getInstance().getMetamodelURI(model.eClass().getEPackage().getName());
        Ecore2Kodkod e2k = metaModels.get(metaModelURI);
        XMI2Kodkod x2k = new XMI2Kodkod(model,e2k);
        models.put(modelUri,x2k);
        model2metaModel.put(modelUri, metaModelURI);
    }

    @Override
    public void remModel(String modelUri) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void translateQVT(RelationalTransformation qvt) throws ErrorTransform, ErrorInternalEngine, ErrorUnsupported, ErrorParser {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void translateATL(EObject atl, EObject mdl1, EObject mdl2) throws ErrorTransform, ErrorInternalEngine, ErrorUnsupported, ErrorParser {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean remQVT(String qvtUri) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasMetaModel(String metaModelUri) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasModel(String modelUri) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

	@Override
	public boolean hasQVT(String qvtUri) {
		// TODO Auto-generated method stub
		return false;
	}
}