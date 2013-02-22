package pt.uminho.haslab.echo.transform;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.uminho.haslab.echo.ErrorAlloy;
import pt.uminho.haslab.echo.ErrorTransform;
import pt.uminho.haslab.echo.ErrorUnsupported;

import net.sourceforge.qvtparser.model.emof.Property;
import net.sourceforge.qvtparser.model.emof.impl.PackageImpl;
import net.sourceforge.qvtparser.model.essentialocl.BooleanLiteralExp;
import net.sourceforge.qvtparser.model.essentialocl.OclExpression;
import net.sourceforge.qvtparser.model.essentialocl.VariableExp;
import net.sourceforge.qvtparser.model.qvtbase.TypedModel;
import net.sourceforge.qvtparser.model.qvtrelation.RelationDomain;
import net.sourceforge.qvtparser.model.qvttemplate.ObjectTemplateExp;
import net.sourceforge.qvtparser.model.qvttemplate.PropertyTemplateItem;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4compiler.ast.Decl;
import edu.mit.csail.sdg.alloy4compiler.ast.Expr;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprConstant;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprHasName;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig;

public class OCL2Alloy {

	private RelationDomain domain;
	private Map<String,List<Sig>> modelsigs = new HashMap<String,List<Sig>>();
	private TypedModel target;
	private List<Decl> vardecls;

	
	
	public OCL2Alloy(RelationDomain domain, Map<String, List<Sig>> modelsigs,
			TypedModel target, List<Decl> vardecls) {
		this.domain = domain;
		this.modelsigs = modelsigs;
		this.target = target;
		this.vardecls = vardecls;
	}
	
	public Expr oclExprToAlloy (VariableExp expr) throws ErrorTransform{
		String varname = expr.getName();
		Decl decl = null;
		for (Decl d : vardecls){
			if (d.get().label.equals(varname))
				decl = d;}
		if (decl == null) throw new ErrorTransform ("Variable not declared.","OCL2Alloy",expr);
		ExprHasName var = decl.get();
		return var;	
	}
	
	public Expr oclExprToAlloy (BooleanLiteralExp expr){
		if (expr.getBooleanSymbol()) return ExprConstant.TRUE;
		else return ExprConstant.FALSE;
	}
	
	public Expr oclExprToAlloy (ObjectTemplateExp temp) throws Exception{
		Expr result = Sig.NONE.no();
		for (Object part1: ((ObjectTemplateExp) temp).getPart()) { // should be PropertyTemplateItem
			
			// calculates OCL expression
			PropertyTemplateItem part = (PropertyTemplateItem) part1;
			OclExpression value = part.getValue();
			Expr ocl = this.oclExprToAlloy(value);
			// retrieves the Alloy field
			Property prop = part.getReferredProperty();
			String mdl = ((PackageImpl) domain.getTypedModel().getUsedPackage().get(0)).getName();
			List<Sig> sigs = modelsigs.get(mdl);
			Expr localfield = null;
			try {
				localfield = AlloyUtil.localStateAttribute(prop, domain.getTypedModel(), sigs, target.equals(domain.getTypedModel()));
			}
			catch (Err e) { throw new ErrorAlloy(e.getMessage(),"OCL2Alloy",prop); }
			// retrieves the Alloy root variable
			String varname = ((ObjectTemplateExp) temp).getBindsTo().getName();
			Decl decl = null;
			for (Decl d : vardecls)
				if (d.get().label.equals(varname))
					decl = d;
			if (decl == null) throw new ErrorTransform ("Variable not declared.","OCL2Alloy",((ObjectTemplateExp) temp).getBindsTo());
			ExprHasName var = decl.get();
			
			// merges the whole thing
			Expr item;
			if (ocl.equals(ExprConstant.TRUE)) item = var.in(localfield);
			else if (ocl.equals(ExprConstant.FALSE)) item = var.not().in(localfield);
			else if (value instanceof ObjectTemplateExp) {
				varname = ((ObjectTemplateExp) value).getBindsTo().getName();
				decl = null;
				for (Decl d : vardecls)
					if (d.get().label.equals(varname))
						decl = d;
				if (decl == null) throw new ErrorTransform ("Variable not declared.","OCL2Alloy",((ObjectTemplateExp) value).getBindsTo());
				ExprHasName var1 = decl.get();
				item = var1.in(var.join(localfield));
				item = AlloyUtil.cleanAnd(item,ocl);
			}
			else {
				item = ocl.in(var.join(localfield));
			}
			
			result = AlloyUtil.cleanAnd(result,item);
		}
		return result;
	}
	
	public Expr oclExprToAlloy (OclExpression expr) throws Exception {
		if (expr instanceof ObjectTemplateExp) return oclExprToAlloy((ObjectTemplateExp) expr);
		else if (expr instanceof BooleanLiteralExp) return oclExprToAlloy((BooleanLiteralExp) expr);
		else if (expr instanceof VariableExp) return oclExprToAlloy((VariableExp) expr);
		else throw new ErrorUnsupported ("OCL expression not supported.","OCL2Alloy",expr);
	}
}
