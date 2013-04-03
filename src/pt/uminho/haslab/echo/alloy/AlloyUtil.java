package pt.uminho.haslab.echo.alloy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.ocl.examples.pivot.Package;
import org.eclipse.ocl.examples.pivot.Property;
import org.eclipse.qvtd.pivot.qvtbase.TypedModel;
import org.eclipse.qvtd.pivot.qvtrelation.Relation;

import pt.uminho.haslab.echo.ErrorAlloy;
import pt.uminho.haslab.echo.ErrorTransform;
import pt.uminho.haslab.echo.transform.ECore2Alloy;
import pt.uminho.haslab.echo.transform.EMF2Alloy;
import pt.uminho.haslab.echo.transform.OCL2Alloy;


import edu.mit.csail.sdg.alloy4.ConstList;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorSyntax;
import edu.mit.csail.sdg.alloy4compiler.ast.Attr;
import edu.mit.csail.sdg.alloy4compiler.ast.CommandScope;
import edu.mit.csail.sdg.alloy4compiler.ast.Decl;
import edu.mit.csail.sdg.alloy4compiler.ast.Expr;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprConstant;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprVar;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig.Field;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig.PrimSig;

public class AlloyUtil {

	
	
	public static String targetName(String target) {
		return target+"_new_";
	}
	
	
	
	// composes an expression with the respective state variable
	public static Expr localStateAttribute(Property prop, Expr statesig, EMF2Alloy translator) throws ErrorAlloy, ErrorTransform{
		Expr exp = OCL2Alloy.propertyToField(prop,translator);
		return exp.join(statesig);
	}
	
	public static Expr localStateSig(Sig sig, Expr var) throws ErrorTransform, ErrorAlloy{
		Expr exp = null;
		
		for (Field field : sig.getFields()) {
			if (field.label.endsWith("_") && field.label.substring(0, field.label.length()-1).equals(sig.label) ){	
				exp = field;
			}
		}
		if (exp == null) throw new ErrorTransform ("State field not found.","AlloyUtil",sig);
		
		return exp.join(var);
	}
	

	
	// methods used to append prefixes to expressions
	public static String pckPrefix (String mdl, String str) {
		return (mdl + "_" + str);
	}

	public static String stateFieldName (EPackage pck, EClass cls) {
		return pck.getName() +"_"+ cls.getName() +"_";
	}
	
	public static String relationFieldName (Relation rel, TypedModel dir) {
		return rel.getName() +"_"+dir.getName()+"_";
	}
	
	
	// ignores first parameter if "no none" or "true"
	public static Expr cleanAnd (Expr e, Expr f) {
		if (e.isSame(Sig.NONE.no()) || e.isSame(ExprConstant.TRUE)) return f;
		else if (f.isSame(Sig.NONE.no()) || f.isSame(ExprConstant.TRUE)) return e;
		else return e.and(f);
	}
	
	public static ConstList<CommandScope> createScope (List<PrimSig> instsigs, List<PrimSig> modelsigs) throws ErrorAlloy {
		Map<String,CommandScope> scopes = new HashMap<String,CommandScope>();
		
		for (PrimSig sig : instsigs) {
			incrementScope(scopes,sig.parent);
			PrimSig up = sig.parent.parent;
			while (up != Sig.UNIV && up != null){
				incrementScope(scopes,up);
				up = up.parent;
			}
		}		
		for (Sig sig : modelsigs){
			if (scopes.get(sig.label)==null)
				try { scopes.put(sig.label,new CommandScope(sig, false, 0));}
				catch (Err e) { throw new ErrorAlloy(e.getMessage(),"AlloyUtil");}
		}
		return ConstList.make(scopes.values());
	}
	
	private static void incrementScope (Map<String,CommandScope> scopes, Sig sig) throws ErrorAlloy  {
		String type = sig.toString();
		CommandScope scope = scopes.get(type);
		if (scope == null)
			try { scope = new CommandScope(sig, false, 1);}
			catch (Err e) { throw new ErrorAlloy(e.getMessage(),"AlloyUtil");}
		else 
			try { scope = new CommandScope(sig, false, scope.startingScope+1);}
			catch (Err e) { throw new ErrorAlloy(e.getMessage(),"AlloyUtil");}
		scopes.put(type, scope);
	
	}
	
	public static ConstList<CommandScope> incrementScopes (List<CommandScope> scopes) throws ErrorSyntax  {
		List<CommandScope> list = new ArrayList<CommandScope>();
		
		for (CommandScope scope : scopes)
			list.add(new CommandScope(scope.sig, false, scope.startingScope+1));

		return ConstList.make(list);
	}
	
	public static List<Decl> ordDecls (List<Decl> decls){
		List<Decl> res = new ArrayList<Decl>();
		int last = decls.size()+1;
		while (last > decls.size() && decls.size() != 1) {
			last = decls.size();
			for (int i = 0; i<decls.size(); i++) {
				boolean safe = true;
				for (int j = 0; j<decls.size(); j++)
					if (decls.get(i).expr.hasVar((ExprVar)(decls.get(j)).get())) safe = false;
				if (safe) res.add(decls.get(i));
			}
			decls.removeAll(res);
		}

		if (decls.size() > 1) {
			String error = "Could not order: \n";
			for (Decl d : decls)
				error = error.concat(d.get()+ " : "+ d.expr+"\n");
			throw new Error(error);
				
		}
		if (decls.size()==1){
			res.add(decls.get(0));
			decls.remove(0);
		}
		
		return res;
	}
	/**
	 * returns true is able to determine true;
	 * false otherwise
	 * @param exp
	 * @return
	 */
	public static boolean isTrue (Expr exp) {
		if (exp.isSame(Sig.NONE.no())) return true;
		if (exp.isSame(ExprConstant.TRUE)) return true;
		return false;
	}
	
}
