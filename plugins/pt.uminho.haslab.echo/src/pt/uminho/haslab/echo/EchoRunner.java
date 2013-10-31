package pt.uminho.haslab.echo;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.qvtd.pivot.qvtrelation.RelationalTransformation;


import pt.uminho.haslab.echo.alloy.GraphPainter;
import pt.uminho.haslab.echo.transform.EchoTranslator;
import edu.mit.csail.sdg.alloy4viz.VizState;

public class EchoRunner {

	private EngineRunner runner;
	private static EchoRunner instance = new EchoRunner();
	
	private EchoRunner() {}
 	
	public static EchoRunner getInstance() {
		return instance;
	}

	/**
	 * Translates a meta-model into Alloy
	 * @param metaModel the EPackage representing the meta-model to translate
	 * @throws ErrorUnsupported
	 * @throws pt.uminho.haslab.echo.alloy.ErrorAlloy
	 * @throws ErrorTransform
	 * @throws ErrorParser
	 */
	public void addMetaModel(EPackage metaModel) throws ErrorUnsupported, ErrorInternalEngine, ErrorTransform, ErrorParser {
		EchoTranslator.getInstance().translateMetaModel(metaModel);
	}

	/**
	 * Removes a meta-model from the system
	 * @param metaModelUri the URI of the meta-model to remove
	 */
	public void remMetaModel(String metaModelUri) {
		EchoTranslator.getInstance().remMetaModel(metaModelUri);
	}

	/**
	 * Tests if a meta-model exists in the system
	 * @param metaModelUri the URI of the meta-model
	 */
	public boolean hasMetaModel(String metaModelUri) {
		return EchoTranslator.getInstance().hasMetaModel(metaModelUri);
	}

	/**
	 * Translates a model into Alloy
	 * @param model the EObject representing the model to translate
	 * @throws ErrorUnsupported
	 * @throws ErrorInternalEngine
	 * @throws ErrorTransform
	 * @throws ErrorParser
	 */
	public void addModel(EObject model) throws ErrorUnsupported, ErrorInternalEngine, ErrorTransform, ErrorParser {
		EchoTranslator.getInstance().translateModel(model);
	}

	/**
	 * Removes a model from the system
	 * @param modeluri the URI of the model to remove
	 */
	public void remModel(String modeluri) {
		EchoTranslator.getInstance().remModel(modeluri);
	}

	/**
	 * Tests if a model exists in the system
	 * @param modelUri the URI of the model
	 */
	public boolean hasModel(String modelUri) {
		return EchoTranslator.getInstance().hasModel(modelUri);
	}

	/**
	 * Translates a QVT-R transformation into Alloy 
	 * @param qvt the RelationalTransformation representing the QVT-R transformation to translate
	 * @throws ErrorUnsupported
	 * @throws ErrorInternalEngine
	 * @throws ErrorTransform
	 * @throws ErrorParser
	 */
	public void addQVT(RelationalTransformation qvt) throws ErrorUnsupported, ErrorInternalEngine, ErrorTransform, ErrorParser {
		EchoTranslator.getInstance().translateQVT(qvt);
	}

	public boolean hasQVT(String qvtUri) {
		return EchoTranslator.getInstance().getQVTFact(qvtUri) != null;
	}
	
	public boolean remQVT(String qvtUri) {
		return EchoTranslator.getInstance().remQVT(qvtUri);
	}
	
	
	public String getMetaModelFromModelPath(String path)
    {
        return EchoTranslator.getInstance().getMetaModelFromModelPath(path);
    }
	
	public void addATL(EObject atl, EObject mdl1, EObject mdl2) throws ErrorUnsupported, ErrorInternalEngine, ErrorTransform, ErrorParser {
		EchoTranslator.getInstance().translateATL(atl,mdl1,mdl2);
	}


	
	/**
	 * Tests if a list of models conform to their meta-models
	 * @param modeluris the URIs of the models to test conformity
	 * @return true if all models conform to the meta-models
	 * @throws ErrorInternalEngine
	 */
	public boolean conforms(List<String> modeluris) throws ErrorInternalEngine {
		runner = EngineFactory.instance().createRunner();
		runner.conforms(modeluris);
		return runner.getSolution().satisfiable();
	}
	
	public boolean show(List<String> modeluris) throws ErrorInternalEngine {
		runner = EngineFactory.instance().createRunner();
		runner.show(modeluris);
		return runner.getSolution().satisfiable();
	}

	/**
	 * Repairs a model not conforming to its meta-model
	 * @param targeturi the URI of the model to repair
	 * @return true if the model was successfully repaired
	 * @throws ErrorInternalEngine
	 */
	public void repair(String targeturi) throws ErrorInternalEngine {
		runner = EngineFactory.instance().createRunner();
		runner.repair(targeturi);
	}

	/**
	 * Generates a model conforming to the given meta-model
	 * @param metamodeluri the URI of the meta-model
	 * @param scope the exact scopes of the model to generate
	 * @return true if able to generate conforming model
	 * @throws ErrorInternalEngine
	 * @throws ErrorTransform 
	 * @throws ErrorUnsupported 
	 */
	public void generate(String metamodeluri, Map<Entry<String,String>,Integer> scope) throws ErrorInternalEngine, ErrorUnsupported {
		runner =  EngineFactory.instance().createRunner();
		runner.generate(metamodeluri,scope);
	}

	/**
	 * Checks if models are consistent according to a QVT-R transformation
	 * @param qvturi the URI of the QVT-R transformation
	 * @param modeluris the URIs of the models (should be in the order of the QVT-R transformation arguments)
	 * @return true if consistent
	 * @throws ErrorInternalEngine
	 */
	public boolean check(String qvturi, List<String> modeluris) throws ErrorInternalEngine {
		runner =  EngineFactory.instance().createRunner();
		runner.check(qvturi, modeluris);
		return runner.getSolution().satisfiable();
	}

	/**
	 * Starts enforcement run according to a QVT-R transformation
	 * @param qvturi the URI of the QVT-R transformation
	 * @param modeluris the URIs of the models (should be in the order of the QVT-R transformation arguments)
	 * @param targeturi the URI of the target model
	 * @return true if able to generate model
	 * @throws ErrorInternalEngine
	 */
	public boolean enforce( String qvturi, List<String> modeluris, String targeturi) throws ErrorInternalEngine {
		runner = EngineFactory.instance().createRunner();
		return runner.enforce(qvturi, modeluris, targeturi);
	}

	/**
	 * Generates a model conforming to the given meta-model and consistent with existing models through a QVT-R transformation
	 * @param qvtUri the URI of the QVT-R transformation
	 * @param metaModelUri the URI of the meta-model
	 * @param modelUris the URIs of the models (should be in the order of the QVT-R transformation arguments)
	 * @param targetUri the URI of the new model
	 * @throws ErrorInternalEngine
	 * @throws ErrorTransform 
	 * @throws ErrorUnsupported 
	 */
	public void generateQvt(String qvtUri, String metaModelUri, List<String> modelUris, String targetUri) throws ErrorInternalEngine, ErrorUnsupported {
		runner =  EngineFactory.instance().createRunner();
		runner.generateQvt(qvtUri, modelUris, targetUri, metaModelUri);
	}


	/**
	 * Shows the next Alloy instance, if any
	 * @return true if able to generate another instance
	 * @throws ErrorInternalEngine
	 */
	public void next() throws ErrorInternalEngine {
		runner.nextInstance();
	}

	/**
	 * Retrieves the current Alloy instance
	 * @return the Alloy instance, if satisfiable
	 */
	public EchoSolution getAInstance() {
		if (runner != null && runner.getSolution()!= null && runner.getSolution().satisfiable()) return runner.getSolution();
		else return null;
	}

	/**
	 * Applies a generated Alloy theme for a given instance
	 * @param vizstate the state of the visualizer
	 */
	public void generateTheme (VizState vizstate) {
		new GraphPainter(vizstate).generateTheme();
	}

	/**
	 * Writes a new instance from the current Alloy solution into XMI
	 * @param metamodeluri the URI of the meta-model of the new model
	 * @param modeluri the URI of the new model
	 * @throws ErrorTransform 
	 * @throws ErrorInternalEngine
	 * @throws ErrorUnsupported 
	 */
	public void writeAllInstances (String metamodeluri, String modeluri) throws ErrorInternalEngine, ErrorTransform, ErrorUnsupported {
		EchoTranslator.getInstance().writeAllInstances(runner.getSolution(), metamodeluri, modeluri);
	}

	/**
	 * Writes an existing instance from the current Alloy solution into XMI
	 * @param modeluri the URI of the existing model
	 * @throws ErrorTransform 
	 * @throws ErrorInternalEngine
	 */
	public void writeInstance (String modeluri) throws ErrorInternalEngine, ErrorTransform {
		EchoTranslator.getInstance().writeInstance(runner.getSolution(), modeluri);
	}
	
	public enum Task {
		ECHO_RUN("echorun"),
		PROCESS_RESOURCES("processresources"),
		CONFORMS_TASK("conformstask"),
		REPAIR_TASK( "repairtask"),
		CHECK_TASK("checktask"),
		ENFORCE_TASK("enforcetask"),
		GENERATE_TASK("generatetask"),
		ITERATION("iteration");

		private Task(String label) { this.label = label; }

		private final String label;
		
		public String toString() {
			return label;
		}
	}


}