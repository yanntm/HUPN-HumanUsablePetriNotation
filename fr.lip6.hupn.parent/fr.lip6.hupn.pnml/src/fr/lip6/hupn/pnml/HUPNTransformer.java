package fr.lip6.hupn.pnml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import fr.lip6.hupn.hUPN.And;
import fr.lip6.hupn.hUPN.Or;
import fr.lip6.hupn.hUPN.BinaryCFunc;
import fr.lip6.hupn.hUPN.BinaryOperators;
import fr.lip6.hupn.hUPN.BinaryTokenExpression;
import fr.lip6.hupn.hUPN.BooleanExpression;
import fr.lip6.hupn.hUPN.CFunc;
import fr.lip6.hupn.hUPN.Comparison;
import fr.lip6.hupn.hUPN.ComparisonOperators;
import fr.lip6.hupn.hUPN.Sort;
import fr.lip6.hupn.hUPN.ElementRef;
import fr.lip6.hupn.hUPN.Enumeration;
import fr.lip6.hupn.hUPN.HUPNFactory;
import fr.lip6.hupn.hUPN.Multiplier;
import fr.lip6.hupn.hUPN.VarDef;
import fr.lip6.hupn.hUPN.VarRef;
import fr.lip6.hupn.hUPN.PetriNet;
import fr.lip6.hupn.hUPN.Pred;
import fr.lip6.hupn.hUPN.RefSort;
import fr.lip6.hupn.hUPN.SortElement;
import fr.lip6.hupn.hUPN.Succ;
import fr.lip6.hupn.hUPN.Token;
import fr.lip6.hupn.hUPN.TokenExpression;
import fr.lip6.move.pnml.symmetricnet.booleans.Bool;
import fr.lip6.move.pnml.symmetricnet.booleans.Equality;
import fr.lip6.move.pnml.symmetricnet.booleans.Inequality;
import fr.lip6.move.pnml.symmetricnet.booleans.Not;
import fr.lip6.move.pnml.symmetricnet.cyclicEnumerations.Predecessor;
import fr.lip6.move.pnml.symmetricnet.cyclicEnumerations.Successor;
import fr.lip6.move.pnml.symmetricnet.dots.Dot;
import fr.lip6.move.pnml.symmetricnet.dots.DotConstant;
import fr.lip6.move.pnml.symmetricnet.finiteEnumerations.FEConstant;
import fr.lip6.move.pnml.symmetricnet.finiteEnumerations.FiniteEnumeration;
import fr.lip6.move.pnml.symmetricnet.finiteIntRanges.FiniteIntRange;
import fr.lip6.move.pnml.symmetricnet.finiteIntRanges.FiniteIntRangeConstant;
import fr.lip6.move.pnml.symmetricnet.finiteIntRanges.GreaterThan;
import fr.lip6.move.pnml.symmetricnet.finiteIntRanges.GreaterThanOrEqual;
import fr.lip6.move.pnml.symmetricnet.finiteIntRanges.LessThan;
import fr.lip6.move.pnml.symmetricnet.finiteIntRanges.LessThanOrEqual;
import fr.lip6.move.pnml.symmetricnet.hlcorestructure.Arc;
import fr.lip6.move.pnml.symmetricnet.hlcorestructure.Condition;
import fr.lip6.move.pnml.symmetricnet.hlcorestructure.Declaration;
import fr.lip6.move.pnml.symmetricnet.hlcorestructure.HLMarking;
import fr.lip6.move.pnml.symmetricnet.hlcorestructure.Page;
import fr.lip6.move.pnml.symmetricnet.hlcorestructure.Place;
import fr.lip6.move.pnml.symmetricnet.hlcorestructure.PnObject;
import fr.lip6.move.pnml.symmetricnet.hlcorestructure.Transition;
import fr.lip6.move.pnml.symmetricnet.integers.NumberConstant;
import fr.lip6.move.pnml.symmetricnet.multisets.Add;
import fr.lip6.move.pnml.symmetricnet.multisets.All;
import fr.lip6.move.pnml.symmetricnet.multisets.NumberOf;
import fr.lip6.move.pnml.symmetricnet.multisets.Subtract;
import fr.lip6.move.pnml.symmetricnet.terms.NamedSort;
import fr.lip6.move.pnml.symmetricnet.terms.OperatorDecl;
import fr.lip6.move.pnml.symmetricnet.terms.ProductSort;
import fr.lip6.move.pnml.symmetricnet.terms.Term;
import fr.lip6.move.pnml.symmetricnet.terms.TermsDeclaration;
import fr.lip6.move.pnml.symmetricnet.terms.Tuple;
import fr.lip6.move.pnml.symmetricnet.terms.UserOperator;
import fr.lip6.move.pnml.symmetricnet.terms.UserSort;
import fr.lip6.move.pnml.symmetricnet.terms.Variable;
import fr.lip6.move.pnml.symmetricnet.terms.VariableDecl;

public class HUPNTransformer {

	public PetriNet transform(fr.lip6.move.pnml.symmetricnet.hlcorestructure.PetriNet pn) {
		PetriNet net = HUPNFactory.eINSTANCE.createPetriNet();

		net.setName(normalizeName(pn.getName().getText()));

		Map<fr.lip6.move.pnml.symmetricnet.terms.Sort, Sort> sortMap = new HashMap<>();

		// Start by parsing all domain declarations
		for (Declaration ddecl : pn.getDeclaration()) {
			for (TermsDeclaration decl : ddecl.getStructure().getDeclaration()) {
				handleSortDeclaration(decl, net, sortMap);
			}
		}
		for (Page p : pn.getPages()) {
			for (Declaration ddecl : p.getDeclaration()) {
				for (TermsDeclaration decl : ddecl.getStructure().getDeclaration()) {
					handleSortDeclaration(decl, net, sortMap);
				}
			}
		}
		Map<VariableDecl, VarDef> varMap = new HashMap<>();
		// now introduce variables/formal parameters
		for (Declaration ddecl : pn.getDeclaration()) {
			for (TermsDeclaration decl : ddecl.getStructure().getDeclaration()) {
				handleVarDeclaration(decl, net, sortMap, varMap);
			}
		}
		for (Page p : pn.getPages()) {
			for (Declaration ddecl : p.getDeclaration()) {
				for (TermsDeclaration decl : ddecl.getStructure().getDeclaration()) {
					handleVarDeclaration(decl, net, sortMap, varMap);
				}
			}
		}

		// now add all the objects of the net
		for (Page p : pn.getPages()) {
			handlePage(p, net, sortMap, varMap);
		}

		return net;
	}

	private void handlePage(Page page, PetriNet res, Map<fr.lip6.move.pnml.symmetricnet.terms.Sort, Sort> sortMap,
			Map<VariableDecl, VarDef> varMap) {
		long time = System.currentTimeMillis();
		Map<Place, fr.lip6.hupn.hUPN.Place> placeMap = new HashMap<>();

		for (PnObject n : page.getObjects()) {
			if (n instanceof Place) {
				Place p = (Place) n;

				fr.lip6.hupn.hUPN.Place img = HUPNFactory.eINSTANCE.createPlace();
				img.setName(normalizeName(p.getName().getText()));
				fr.lip6.move.pnml.symmetricnet.terms.Sort psort = p.getType().getStructure();
				if (psort instanceof UserSort) {
					UserSort usort = (UserSort) psort;
					if (usort.getDeclaration() instanceof NamedSort) {
						NamedSort nsort = (NamedSort) usort.getDeclaration();
						psort = nsort.getSortdef();
					}
				}
				img.setSort(sortMap.get(psort));
				TokenExpression tok = interpretMarking(p.getHlinitialMarking(), psort, sortMap, varMap);
				img.setInitial(tok);

				res.getPlaces().add(img);
				placeMap.put(p, img);
			}
		}

		for (PnObject pnobj : page.getObjects()) {
			if (pnobj instanceof Transition) {
				Transition t = (Transition) pnobj;

				BooleanExpression guard = HUPNFactory.eINSTANCE.createTrue();
				Condition cond = t.getCondition();
				if (cond != null) {
					Term g = cond.getStructure();
					guard = convertToBoolean(g, sortMap, varMap);
				}

				fr.lip6.hupn.hUPN.Transition timg = HUPNFactory.eINSTANCE.createTransition();
				timg.setName(normalizeName(t.getName().getText()));

				timg.setGuard(guard);

				res.getTransitions().add(timg);

				for (Arc arc : t.getInArcs()) {
					Place pl = (Place) arc.getSource();
					fr.lip6.hupn.hUPN.Place pind = placeMap.get(pl);

					TokenExpression tok = interpretMarkingTerm(arc.getHlinscription().getStructure(),
							pl.getType().getStructure(), sortMap, varMap);

					fr.lip6.hupn.hUPN.Arc img = HUPNFactory.eINSTANCE.createArc();
					img.setPlace(pind);
					img.setFunc(tok);

					timg.getPreArcs().add(img);
				}
				for (Arc arc : t.getOutArcs()) {
					Place pl = (Place) arc.getTarget();
					fr.lip6.hupn.hUPN.Place pind = placeMap.get(pl);

					TokenExpression tok = interpretMarkingTerm(arc.getHlinscription().getStructure(),
							pl.getType().getStructure(), sortMap, varMap);

					fr.lip6.hupn.hUPN.Arc img = HUPNFactory.eINSTANCE.createArc();
					img.setPlace(pind);
					img.setFunc(tok);

					timg.getPostArcs().add(img);
				}
			}
		}
//		long flatp=0;
//		for (Integer ind : constPlaces) {
//			res.getPlaces().get(ind).setConstant(true);
//			flatp += res.getPlaces().get(ind).getInitial().length ;
//		}
//		if (!constPlaces.isEmpty()) {
//			getLog().info("Detected "+ constPlaces.size() + " constant HL places corresponding to " + flatp + " PT places.");
//		}
//		getLog().info("Imported "+ res.getPlaces().size() + " HL places and " + res.getTransitions().size()+ " HL transitions for a total of " 
//				+ res.getPlaceCount() + " PT places and " + totalt + " transition bindings in " + (System.currentTimeMillis() - time) + " ms.");
	}

	private void handleVarDeclaration(TermsDeclaration decl, PetriNet res,
			Map<fr.lip6.move.pnml.symmetricnet.terms.Sort, Sort> sortMap, Map<VariableDecl, VarDef> varMap) {
		if (decl instanceof VariableDecl) {
			VariableDecl vdecl = (VariableDecl) decl;
			VarDef param = HUPNFactory.eINSTANCE.createVarDef();
			param.setName("$" + vdecl.getName());
			if (vdecl.getSort() instanceof UserSort) {
				UserSort usort = (UserSort) vdecl.getSort();

				fr.lip6.move.pnml.symmetricnet.terms.Sort truesort = ((NamedSort) usort.getDeclaration()).getSortdef();
				param.setType(sortMap.get(truesort));
			} else {
				param.setType(sortMap.get(vdecl.getSort()));
			}
			res.getVars().add(param);
			varMap.put(vdecl, param);
		} else {
			return;
		}
	}

	private void handleSortDeclaration(TermsDeclaration decl, PetriNet res,
			Map<fr.lip6.move.pnml.symmetricnet.terms.Sort, Sort> sortMap) {
		if (decl instanceof NamedSort) {
			fr.lip6.move.pnml.symmetricnet.terms.Sort sort = ((NamedSort) decl).getSortdef();
			if (sort instanceof Bool) {
				// Degenerate to a domain with two values
				Sort booldom = HUPNFactory.eINSTANCE.createSort();
				booldom.setName("Boolean");
				Enumeration def = HUPNFactory.eINSTANCE.createEnumeration();
				booldom.setDef(def);

				SortElement e = HUPNFactory.eINSTANCE.createSortElement();
				e.setName("true");
				def.getElements().add(e);

				e = HUPNFactory.eINSTANCE.createSortElement();
				e.setName("false");
				def.getElements().add(e);

				res.getSorts().add(booldom);
				sortMap.put(sort, booldom);
			} else if (sort instanceof FiniteEnumeration) {
				// Build a corresponding enumerated domain
				FiniteEnumeration fen = (FiniteEnumeration) sort;

				Sort dom = HUPNFactory.eINSTANCE.createSort();
				dom.setName(normalizeName(decl.getName()));
				Enumeration def = HUPNFactory.eINSTANCE.createEnumeration();
				dom.setDef(def);

				for (FEConstant elt : fen.getElements()) {
					SortElement e = HUPNFactory.eINSTANCE.createSortElement();
					e.setName(normalizeName(elt.getId()));
					def.getElements().add(e);
				}

				res.getSorts().add(dom);
				sortMap.put(sort, dom);
			} else if (sort instanceof Dot) {
				// Degenerate to a domain with single value
				Sort dom = HUPNFactory.eINSTANCE.createSort();
				dom.setName("Dot");
				Enumeration def = HUPNFactory.eINSTANCE.createEnumeration();
				dom.setDef(def);

				SortElement e = HUPNFactory.eINSTANCE.createSortElement();
				e.setName("o");
				def.getElements().add(e);

				res.getSorts().add(dom);
				sortMap.put(sort, dom);
			} else if (sort instanceof ProductSort) {
				ProductSort srcprod = (ProductSort) sort;
				fr.lip6.hupn.hUPN.ProductSort prod = HUPNFactory.eINSTANCE.createProductSort();
				for (fr.lip6.move.pnml.symmetricnet.terms.Sort sub : srcprod.getElementSort()) {
					if (sub instanceof UserSort) {
						NamedSort nsort = (NamedSort) ((UserSort) sub).getDeclaration();
						sub = nsort.getSortdef();
					}
					RefSort rs = HUPNFactory.eINSTANCE.createRefSort();
					rs.setSort(sortMap.get(sub));
					prod.getSorts().add(rs);
				}
				Sort dom = HUPNFactory.eINSTANCE.createSort();
				dom.setName(normalizeName(decl.getName()));
				dom.setDef(prod);

				res.getSorts().add(dom);
				sortMap.put(sort, dom);
			} else if (sort instanceof FiniteIntRange) {
				FiniteIntRange fir = (FiniteIntRange) sort;
				Sort dom = HUPNFactory.eINSTANCE.createSort();
				dom.setName(normalizeName(decl.getName()));
				Enumeration def = HUPNFactory.eINSTANCE.createEnumeration();
				dom.setDef(def);

				for (long i=fir.getStart() ; i <= fir.getEnd() ; i++) {
					SortElement e = HUPNFactory.eINSTANCE.createSortElement();
					e.setName(normalizeName("e" + i));
					def.getElements().add(e);
				}

				res.getSorts().add(dom);
				sortMap.put(sort, dom);
			}

		} else if (decl instanceof VariableDecl) {
			VariableDecl vdecl = (VariableDecl) decl;
			return;
		} else {
			// Partition
			throw new UnsupportedOperationException("Unknown type for declaration : " + decl.getClass().getName());
		}
	}

	private TokenExpression interpretMarking(HLMarking hlinitialMarking, fr.lip6.move.pnml.symmetricnet.terms.Sort psort,
			Map<fr.lip6.move.pnml.symmetricnet.terms.Sort, Sort> sortMap, Map<VariableDecl, VarDef> varMap) {
		if (hlinitialMarking == null) {
			return null;
		}
		return interpretMarkingTerm(hlinitialMarking.getStructure(), psort, sortMap, varMap);
	}

	private CFunc interpretToken(Term term, fr.lip6.move.pnml.symmetricnet.terms.Sort psort, Map<fr.lip6.move.pnml.symmetricnet.terms.Sort, Sort> sortMap,
			Map<VariableDecl, VarDef> varMap) {
		if (term instanceof All) {
			return HUPNFactory.eINSTANCE.createAllRef();
		} else if (term instanceof UserOperator) {
			// Probably designating a constant of the type
			UserOperator uo = (UserOperator) term;
			int index = getConstantIndex(uo);

			Token tok = HUPNFactory.eINSTANCE.createToken();
			ElementRef elt = HUPNFactory.eINSTANCE.createElementRef();
			elt.setElement(((Enumeration) sortMap.get(psort).getDef()).getElements().get(index));
			tok.getTuple().add(elt);
			return elt;
		} else if (term instanceof Variable) {
			Variable var = (Variable) term;
			VarRef pref = HUPNFactory.eINSTANCE.createVarRef();
			pref.setVar(varMap.get(var.getVariableDecl()));
			return pref;
		} else if (term instanceof Add) {
			Add add = (Add) term;
			CFunc cur = interpretToken(add.getSubterm().get(0), psort, sortMap, varMap);

			for (Term t : add.getSubterm().subList(1, add.getSubterm().size())) {
				CFunc toadd = interpretToken(t, psort, sortMap, varMap);
				BinaryCFunc bte = HUPNFactory.eINSTANCE.createBinaryCFunc();
				bte.setOp(BinaryOperators.ADD);
				bte.setLeft(cur);
				bte.setRight(toadd);
				cur = bte;
			}
			return cur;
		} else if (term instanceof Subtract) {
			Subtract add = (Subtract) term;
			assert (((Subtract) term).getSubterm().size() == 2);
			CFunc l = interpretToken(add.getSubterm().get(0), psort, sortMap, varMap);
			CFunc r = interpretToken(add.getSubterm().get(1), psort, sortMap, varMap);

			BinaryCFunc bte = HUPNFactory.eINSTANCE.createBinaryCFunc();
			bte.setOp(BinaryOperators.MINUS);
			bte.setLeft(l);
			bte.setRight(r);

			return bte;
		}

		return convertToInt(term, sortMap, varMap);

	}

	private TokenExpression interpretMarkingTerm(Term term, fr.lip6.move.pnml.symmetricnet.terms.Sort sort,
			Map<fr.lip6.move.pnml.symmetricnet.terms.Sort, Sort> sortMap, Map<VariableDecl, VarDef> varMap) {

		if (term instanceof Add) {
			Add add = (Add) term;
			TokenExpression cur = interpretMarkingTerm(add.getSubterm().get(0), sort, sortMap, varMap);

			for (Term t : add.getSubterm().subList(1, add.getSubterm().size())) {
				TokenExpression toadd = interpretMarkingTerm(t, sort, sortMap, varMap);
				BinaryTokenExpression bte = HUPNFactory.eINSTANCE.createBinaryTokenExpression();
				bte.setOp(BinaryOperators.ADD);
				bte.setLeft(cur);
				bte.setRight(toadd);
				cur = bte;
			}
			return cur;

		} else if (term instanceof Subtract) {
			Subtract add = (Subtract) term;
			assert (((Subtract) term).getSubterm().size() == 2);
			TokenExpression l = interpretMarkingTerm(add.getSubterm().get(0), sort, sortMap, varMap);
			TokenExpression r = interpretMarkingTerm(add.getSubterm().get(1), sort, sortMap, varMap);

			BinaryTokenExpression bte = HUPNFactory.eINSTANCE.createBinaryTokenExpression();
			bte.setOp(BinaryOperators.MINUS);
			bte.setLeft(l);
			bte.setRight(r);

			return bte;
		} else if (term instanceof NumberOf) {
			NumberOf no = (NumberOf) term;
			int card = getCardinality(no);
			int tokenindex = 1;
			if (no.getSubterm().size() == 1) {
				// this could happen if the pnml input is malformed and numberOf has no
				// cardinality.
				tokenindex = 0;
			}
			if (sort instanceof UserSort) {
				sort = ((NamedSort) ((UserSort) sort).getDeclaration()).getSortdef();
			}
			Token token = (Token) interpretMarkingTerm(no.getSubterm().get(tokenindex), sort, sortMap, varMap);
			if (card != 1) {
				Multiplier m = HUPNFactory.eINSTANCE.createMultiplier();
				m.setMult(card);
				token.setMult(m);
			}
			return token;
		} else if (term instanceof Tuple) {
			Tuple tuple = (Tuple) term;

			Token token = HUPNFactory.eINSTANCE.createToken();

			ProductSort prodsort ;
			if (sort instanceof UserSort) {
				UserSort usort = (UserSort) sort;
				prodsort = (ProductSort) ((NamedSort) usort.getDeclaration()).getSortdef();
			} else {
				prodsort = (ProductSort) sort;
			}
			
			Iterator<fr.lip6.move.pnml.symmetricnet.terms.Sort> it = prodsort.getElementSort().iterator();
			for (Term subterm : tuple.getSubterm()) {
				token.getTuple().add(interpretToken(subterm,
						((NamedSort) ((UserSort) it.next()).getDeclaration()).getSortdef(), sortMap, varMap));
			}
			return token;
		} else {
			Token token = HUPNFactory.eINSTANCE.createToken();
			token.getTuple().add(interpretToken(term, sort, sortMap, varMap));
			return token;
		}

//		if (term instanceof All) {
//			Token token = HUPNFactory.eINSTANCE.createToken();
//			token.getTuple().add(HUPNFactory.eINSTANCE.createAllRef());
//			token.setMult(1);
//			return interpretToken(term, psort, sortMap);			
//		} else if (term instanceof NumberOf) {
//			NumberOf no = (NumberOf) term;
//			int card = getCardinality(no);
//			int tokenindex = 1;
//			if (no.getSubterm().size() == 1) {
//				// this could happen if the pnml input is malformed and numberOf has no cardinality.
//				tokenindex = 0;
//			}
//			Token token = interpretToken(no.getSubterm().get(tokenindex),psort, sortMap);
//			token.setMult(card);
//			return token;
//		} else if (term instanceof UserOperator) {
//			return interpretToken(term, psort, sortMap);
//			
//		} else if (term instanceof Tuple) {
//			Tuple tuple = (Tuple) term;
//			
//			
//			// hopefully, only constants in the tuple
//			int tot = 1;
//			
//			List<Integer> targets = new ArrayList<>();
//			targets.add(0);
//			
//			for (int i = tuple.getSubterm().size() -1 ; i >= 0 ; i--) {
//				Term elem = tuple.getSubterm().get(i);
//				Sort elemSort = null; 					
//				if (psort instanceof UserSort) {
//					UserSort usort = (UserSort) psort;
//					if (usort.getDeclaration() instanceof NamedSort) {
//						NamedSort nsort = (NamedSort) usort.getDeclaration();
//						if (nsort.getSortdef() instanceof ProductSort) {
//							ProductSort prod = (ProductSort) nsort.getSortdef();
//							elemSort = prod.getElementSort().get(i);
//						}
//					}
//				}
//				if (elemSort == null) {
//					throw new UnsupportedOperationException();
//				}
//				
//				if (elem instanceof UserOperator) {
//					targets = interpretSubTerm(tot, targets, elem);
//				} else if (elem instanceof Add) {
//					Add add = (Add) elem;
//					List<Integer> newtargets = new ArrayList<>();
//					addTargets(add, targets, tot, newtargets);
//					targets = newtargets;
//				} else if (elem instanceof All) {
//					List<Integer> newtargets = new ArrayList<>();
//					for (int target : targets) {
//						for (int ii = 0, iie = computeSortCardinality(elemSort) ; ii < iie ; ii++) {
//							newtargets.add(target + tot * ii);
//						}
//					}
//					targets = newtargets;
//				} else if (elem instanceof FiniteIntRangeConstant) {
//					FiniteIntRangeConstant firc = (FiniteIntRangeConstant) elem;
//					int pos = firc.getValue() - Math.toIntExact(firc.getRange().getStart());
//					for (int ii=0; ii < targets.size() ; ii++) {
//						targets.set(ii, targets.get(ii) + tot * pos);
//					}					
//				} else {
//					throw new UnsupportedOperationException();
//				}
//				tot *= computeSortCardinality(elemSort);
//			}
//			for (Integer target : targets) {
//				toret[target] = 1;
//			}

	}

	private BooleanExpression convertToBoolean(Term g, Map<fr.lip6.move.pnml.symmetricnet.terms.Sort, Sort> sortMap,
			Map<VariableDecl, VarDef> varMap) {

		if (g instanceof fr.lip6.move.pnml.symmetricnet.booleans.And) {
			fr.lip6.move.pnml.symmetricnet.booleans.And and = (fr.lip6.move.pnml.symmetricnet.booleans.And) g;
			if (and.getSubterm().size() == 2) {
				BooleanExpression l = convertToBoolean(and.getSubterm().get(0), sortMap, varMap);
				BooleanExpression r = convertToBoolean(and.getSubterm().get(1), sortMap, varMap);
				And andt = HUPNFactory.eINSTANCE.createAnd();
				andt.setLeft(l);
				andt.setRight(r);
				return andt;
			} else if (and.getSubterm().size() == 1) {
				getLog().warning("AND operator with single subterm is malformed PNML.");
				return convertToBoolean(and.getSubterm().get(0), sortMap, varMap);
			} else {
				List<BooleanExpression> children = and.getSubterm().stream()
						.map(st -> convertToBoolean(st, sortMap, varMap)).collect(Collectors.toList());

				BooleanExpression cur = children.get(0);
				for (BooleanExpression sub : children.subList(1, children.size())) {
					And andt = HUPNFactory.eINSTANCE.createAnd();
					andt.setLeft(cur);
					andt.setRight(sub);
					cur = andt;
				}

				return cur;
			}
		} else if (g instanceof fr.lip6.move.pnml.symmetricnet.booleans.Or) {
			fr.lip6.move.pnml.symmetricnet.booleans.Or and = (fr.lip6.move.pnml.symmetricnet.booleans.Or) g;
			if (and.getSubterm().size() == 2) {
				BooleanExpression l = convertToBoolean(and.getSubterm().get(0), sortMap, varMap);
				BooleanExpression r = convertToBoolean(and.getSubterm().get(1), sortMap, varMap);
				Or andt = HUPNFactory.eINSTANCE.createOr();
				andt.setLeft(l);
				andt.setRight(r);
				return andt;
			} else if (and.getSubterm().size() == 1) {
				getLog().warning("AND operator with single subterm is malformed PNML.");
				return convertToBoolean(and.getSubterm().get(0), sortMap, varMap);
			} else {
				List<BooleanExpression> children = and.getSubterm().stream()
						.map(st -> convertToBoolean(st, sortMap, varMap)).collect(Collectors.toList());

				BooleanExpression cur = children.get(0);
				for (BooleanExpression sub : children.subList(1, children.size())) {
					Or andt = HUPNFactory.eINSTANCE.createOr();
					andt.setLeft(cur);
					andt.setRight(sub);
					cur = andt;
				}

				return cur;
			}
		} else if (g instanceof Not) {
			Not not = (Not) g;
			fr.lip6.hupn.hUPN.Not tnot = HUPNFactory.eINSTANCE.createNot();
			tnot.setValue(convertToBoolean(not.getSubterm().get(0), sortMap, varMap));
			return tnot;
		} else if (g instanceof Equality) {
			Equality equ = (Equality) g;

			return buildComparison(ComparisonOperators.EQ, equ.getSubterm().get(0), equ.getSubterm().get(1), sortMap,
					varMap);
		} else if (g instanceof Inequality) {
			Inequality equ = (Inequality) g;
			return buildComparison(ComparisonOperators.NE, equ.getSubterm().get(0), equ.getSubterm().get(1), sortMap,
					varMap);
		} else if (g instanceof LessThanOrEqual) {
			LessThanOrEqual equ = (LessThanOrEqual) g;
			return buildComparison(ComparisonOperators.LE, equ.getSubterm().get(0), equ.getSubterm().get(1), sortMap,
					varMap);
		} else if (g instanceof LessThan) {
			LessThan equ = (LessThan) g;
			return buildComparison(ComparisonOperators.LT, equ.getSubterm().get(0), equ.getSubterm().get(1), sortMap,
					varMap);
		} else if (g instanceof GreaterThanOrEqual) {
			GreaterThanOrEqual equ = (GreaterThanOrEqual) g;
			return buildComparison(ComparisonOperators.GE, equ.getSubterm().get(0), equ.getSubterm().get(1), sortMap,
					varMap);
		} else if (g instanceof GreaterThan) {
			GreaterThan equ = (GreaterThan) g;
			return buildComparison(ComparisonOperators.GT, equ.getSubterm().get(0), equ.getSubterm().get(1), sortMap,
					varMap);
		} else {
			getLog().warning("Unknown boolean operator encountered " + g.getClass().getName());
		}
		return HUPNFactory.eINSTANCE.createTrue();
	}

	private BooleanExpression buildComparison(ComparisonOperators op, Term ll, Term rr,
			Map<fr.lip6.move.pnml.symmetricnet.terms.Sort, Sort> sortMap, Map<VariableDecl, VarDef> varMap) {
		Comparison cmp = HUPNFactory.eINSTANCE.createComparison();
		CFunc l = convertToInt(ll, sortMap, varMap);
		CFunc r = convertToInt(rr, sortMap, varMap);
		cmp.setLeft(l);
		cmp.setRight(r);
		cmp.setOperator(op);
		return cmp;
	}

	private CFunc convertToInt(Term g, Map<fr.lip6.move.pnml.symmetricnet.terms.Sort, Sort> sortMap,
			Map<VariableDecl, VarDef> varMap) {
		if (g instanceof fr.lip6.move.pnml.symmetricnet.terms.Variable) {
			fr.lip6.move.pnml.symmetricnet.terms.Variable var = (fr.lip6.move.pnml.symmetricnet.terms.Variable) g;
			VarRef ref = HUPNFactory.eINSTANCE.createVarRef();
			ref.setVar(varMap.get(var.getVariableDecl()));
			return ref;
//		} else if (g instanceof IntegerOperator) {
//			IntegerOperator io = (IntegerOperator) g;
//			
//			return Expression.op(toOp(io),
//						convertToInt(io.getSubterm().get(0), varMap), 						 
//						convertToInt(io.getSubterm().get(1), varMap));
//		} else if (g instanceof NumberConstant) {
//			NumberConstant nc = (NumberConstant) g;
//			return Expression.constant(Math.toIntExact(nc.getValue()));
		} else if (g instanceof UserOperator) {
			UserOperator uo = (UserOperator) g;
			ElementRef cte = HUPNFactory.eINSTANCE.createElementRef();
			cte.setElement(getImage(uo, sortMap));
			return cte;
		} else if (g instanceof Predecessor) {
			Predecessor uo = (Predecessor) g;
			Pred pred = HUPNFactory.eINSTANCE.createPred();
			pred.setVar(varMap.get(((Variable) uo.getSubterm().get(0)).getVariableDecl()));
			return pred;
		} else if (g instanceof Successor) {
			Successor uo = (Successor) g;
			Succ succ = HUPNFactory.eINSTANCE.createSucc();
			succ.setVar(varMap.get(((Variable) uo.getSubterm().get(0)).getVariableDecl()));
			return succ;
		} else if (g instanceof FiniteIntRangeConstant) {
			FiniteIntRangeConstant firc = (FiniteIntRangeConstant) g;
			ElementRef cte = HUPNFactory.eINSTANCE.createElementRef();
			int index = Math.toIntExact(firc.getValue() - firc.getRange().getStart());
			cte.setElement(((Enumeration) sortMap.get(firc.getSort())).getElements().get(index));
			return cte;
		} else if (g instanceof DotConstant) {
			DotConstant dc = (DotConstant) g;			
			ElementRef cte = HUPNFactory.eINSTANCE.createElementRef();
			// find the DotConstant
			for (Entry<fr.lip6.move.pnml.symmetricnet.terms.Sort, Sort> e : sortMap.entrySet()) {
				if (e.getKey() instanceof Dot) {
					cte.setElement(((Enumeration) e.getValue().getDef()).getElements().get(0));					
					return cte;
				}
			}
		} else {
			getLog().warning("Unknown arithmetic term or operator :" + g.getClass().getName());
		}
		return null;
	}

	public static int getCardinality(NumberOf no) {
		Term num = no.getSubterm().get(0);

		if (num instanceof NumberConstant) {
			NumberConstant nc = (NumberConstant) num;
			return nc.getValue().intValue();
		} else {
			getLog().warning(
					"Expected a number constant in first son of NumberOf expression; inferring cardinality 1.");
		}

		return 1;
	}

	public static SortElement getImage(UserOperator uo, Map<fr.lip6.move.pnml.symmetricnet.terms.Sort, Sort> sortMap) {
		OperatorDecl decl = uo.getDeclaration();
		if (decl instanceof FEConstant) {
			FEConstant fec = (FEConstant) decl;
			int index = fec.getSort().getElements().indexOf(fec);
			return ((Enumeration) sortMap.get(((FEConstant) decl).getSort()).getDef()).getElements().get(index);
		} else {
			getLog().warning("Expected an enumeration constant as child of UserOperator, encountered "
					+ decl.getClass().getName());
		}
		return null;
	}

	public static int getConstantIndex(UserOperator uo) {
		OperatorDecl decl = uo.getDeclaration();
		if (decl instanceof FEConstant) {
			FEConstant fec = (FEConstant) decl;
			int index = fec.getSort().getElements().indexOf(fec);
			return index;
		} else {
			getLog().warning("Expected an enumeration constant as child of UserOperator, encountered "
					+ decl.getClass().getName());
		}
		return 0;
	}

	public static String normalizeName(String text) {
		String res = text.replace(' ', '_');
		res = res.replace('-', '_');
		res = res.replace('/', '_');
		res = res.replace('*', 'x');
		res = res.replace('=', '_');

		return res;
	}

	private static Logger getLog() {
		return Logger.getLogger("fr.lip6.move.gal");
	}
}
