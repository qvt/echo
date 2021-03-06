package pt.uminho.haslab.echo.transform.alloy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.uminho.haslab.echo.EchoError;
import pt.uminho.haslab.echo.EchoReporter;
import pt.uminho.haslab.echo.EchoRunner.Task;
import pt.uminho.haslab.mde.transformation.EDependency;
import pt.uminho.haslab.mde.transformation.EModelParameter;
import pt.uminho.haslab.mde.transformation.ERelation;
import pt.uminho.haslab.mde.transformation.ETransformation;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4compiler.ast.Decl;
import edu.mit.csail.sdg.alloy4compiler.ast.Expr;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprHasName;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprVar;
import edu.mit.csail.sdg.alloy4compiler.ast.Func;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig;

class EAlloyTransformation {

	/** the Alloy expression rising from this QVT Transformation*/
	private Func func;
	
	/** the transformation being translated */
	public final ETransformation transformation;
	
	/** the Alloy functions defining sub-relation consistency */
	private Map<String,Func> subrelationcall_defs = new HashMap<String,Func>();

	/** the Alloy functions defining sub-relation calls */
	private Map<String,Func> subrelationcall_funcs = new HashMap<String,Func>();
	
	/** the Alloy functions defining top-relation calls */
	private Map<String,Func> toprelationcall_funcs = new HashMap<String,Func>();
	
	/** 
	 * Constructs a new QVT Transformation to Alloy translator.
	 * A {@code QVTRelation2Alloy} is called for every top QVT Relation and direction.
	 * @param transformation the QVT Transformation being translated
	 * @throws EchoError
	 */
	EAlloyTransformation (ETransformation transformation, Map<String,List<EDependency>> dependencies) throws EchoError {
		EchoReporter.getInstance().debug(""+dependencies);
		
		EchoReporter.getInstance().start(Task.TRANSLATE_TRANSFORMATION, transformation.getName());
		Expr fact = Sig.NONE.no();
		this.transformation = transformation;
		List<Decl> model_params_decls = new ArrayList<Decl>();
		List<ExprHasName> model_params_vars = new ArrayList<ExprHasName>();

		for (EModelParameter mdl : transformation.getModels()) {
			Decl d;
			String metamodelID = mdl.getMetamodel().ID;
			try {
				d = AlloyEchoTranslator.getInstance().getMetamodel(metamodelID).sig_metamodel.oneOf(mdl.getName());
			} catch (Err a) { 
				throw new ErrorAlloy(
						ErrorAlloy.FAIL_CREATE_VAR,
						"Failed to create transformation model variable: "+mdl.getName(),
						a,Task.TRANSLATE_TRANSFORMATION); 
			}
			model_params_decls.add(d);
			model_params_vars.add(d.get());
		}

		for (ERelation rel : transformation.getRelations())
			if (rel.isTop()) {
				for (EDependency dep : dependencies.get(rel.getName()))
					new Relation2Alloy(this,dep,rel);
			}
		
		for (Func f : toprelationcall_funcs.values())
			fact = fact.and(f.call(model_params_vars.toArray(new ExprVar[model_params_vars.size()])));

		for (Func f : subrelationcall_defs.values())
			fact = fact.and(f.call(model_params_vars.toArray(new ExprVar[model_params_vars.size()])));

		try {
			func = new Func(null, transformation.getName(), model_params_decls, null, fact);		
		} catch (Err a) { 
			throw new ErrorAlloy(
					ErrorAlloy.FAIL_CREATE_FUNC,
					"Failed to create transformation function: "+transformation.getName(),
					a,Task.TRANSLATE_TRANSFORMATION); 
		}
		EchoReporter.getInstance().result(Task.TRANSLATE_TRANSFORMATION, "", true);

	}
	
	/**
	 * Returns the Alloy function corresponding to this QVT Transformation
	 * Function parameters are the model variables
	 * @return this.fact
	 */	
	Func getTransformationConstraint() {
		return func;
	}

	/** 
	 * Adds a new sub-relation definition
	 * Function parameters are the model variables
	 * called by containing relations
	 * @param f the function definition
	 */
	void addSubRelationDef(Func f) {
		subrelationcall_defs.put(f.label, f);
	}
	
	/** 
	 * Adds a new sub-relation call function
	 * Function parameters are the model variables and the domain variables
	 * called by containing relations
	 * @param x the function definition
	 */
	void addSubRelationCall(Func x) {
		subrelationcall_funcs.put(x.label, x);
	}
	
	/** 
	 * Adds a new top-relation call function
	 * Function parameters are the model variables
	 * called by containing relations
	 * @param x the function definition
	 */
	void addTopRelationCall(Func x) {
		toprelationcall_funcs.put(x.label, x);
	}
	
	/** 
	 * Returns the function call of a sub-relation
	 * @param n the relation being called
	 * @param dep
	 * @return the respective Alloy function
	 */
	Func callRelation(ERelation n, EDependency dep) {
		return subrelationcall_funcs.get(AlloyUtil.relationFieldName(n,dep.target));
	}

}
