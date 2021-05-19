package fr.lip6.hupn.formatting2


import org.eclipse.xtext.formatting2.AbstractFormatter2
import org.eclipse.xtext.formatting2.IFormattableDocument
import fr.lip6.hupn.hUPN.Arc
import fr.lip6.hupn.hUPN.Place
import fr.lip6.hupn.hUPN.HUPNPackage
import fr.lip6.hupn.hUPN.PetriNet
import fr.lip6.hupn.hUPN.Transition
import fr.lip6.hupn.hUPN.TokenExpression
import fr.lip6.hupn.hUPN.Sort
import fr.lip6.hupn.hUPN.VarDef
import fr.lip6.hupn.hUPN.BinaryTokenExpression

class HUPNFormatter extends AbstractFormatter2 {
	
	
	def dispatch void format(Arc stat, extension IFormattableDocument document) { 		
		for (k : stat.regionFor.keywords(";")) {
			k.prepend[noSpace].append[newLine]
		}
		for (k : stat.regionFor.keywords("`")) {
			k.prepend[noSpace].append[noSpace]
		}
		stat.func.format;
	}		
	
	def dispatch void format(Sort stat, extension IFormattableDocument document) { 		
		for (k : stat.regionFor.keywords(";")) {
			k.prepend[noSpace].append[newLine]
		}
	}
	
	
	def dispatch void format(Place stat, extension IFormattableDocument document) {
		stat.regionFor.keyword("place").prepend[noSpace].append[oneSpace]
		stat.regionFor.feature(HUPNPackage.Literals.PN_OBJECT__NAME).prepend[oneSpace];
		stat.regionFor.keyword(";").prepend[noSpace].append[newLine]
		
		for (k : stat.regionFor.keywords("`")) {
			k.prepend[noSpace].append[noSpace]
		}
		stat.initial.format;
	} 		
	
	def dispatch void format(PetriNet type, extension IFormattableDocument document) { 
		interior(
			type.regionFor.keyword('{').append[newLine],
			type.regionFor.keyword('}').prepend[newLine].append[newLine],
			[indent]
		)
		
		for (v : type.sorts) {
			v.format;
		}
		for (a : type.places) {
			a.format;
		}
		for (a : type.vars) {
			a.format;
		}
		for (t: type.transitions) {
			t.format
		}		
	}	
	
	
	def dispatch void format(Transition trans, extension IFormattableDocument document) { 
		interior(
			trans.regionFor.keyword('{').append[newLine],
			trans.regionFor.keyword('}').append[newLine],
			[indent]
		)
		for (k : trans.regionFor.keywords(";")) {
			k.prepend[noSpace].append[newLine]
		}
		for (s : trans.preArcs) {
			s.format
		}
		for (s : trans.postArcs) {
			s.format
		}
		//trans.interior[indent];
	}	
	def dispatch void format(BinaryTokenExpression ref, extension IFormattableDocument document) {
		ref.left.format ;
		ref.right.format ;
		
	}
	
	def dispatch void format(TokenExpression ref, extension IFormattableDocument document) {		
		
		for (k : ref.regionFor.keywords("[")) {
			k.prepend[noSpace].append[noSpace]
		}
		for (k : ref.regionFor.keywords("]")) {
			k.prepend[noSpace].append[noSpace]
		}
		
		for (k : ref.regionFor.keywords("(")) {
			k.append[noSpace]
		}
		for (k : ref.regionFor.keywords(")")) {
			k.prepend[noSpace]
		}

		for (k : ref.regionFor.keywords("<")) {
			k.append[noSpace]
		}
		for (k : ref.regionFor.keywords(">")) {
			k.prepend[noSpace]
		}
		for (k : ref.regionFor.keywords("`")) {
			k.prepend[noSpace].append[noSpace]
		}

		for (k : ref.regionFor.keywords(",")) {
			k.prepend[noSpace].append[oneSpace; autowrap]
		}
		
		for (k : ref.regionFor.keywords(":")) {
			k.prepend[noSpace].append[noSpace]
		}
		
		for (k : ref.regionFor.keywords(".")) {
			k.prepend[noSpace].append[noSpace]
		}
		
		for (k : ref.regionFor.keywords(";")) {
			k.prepend[noSpace].append[newLine]
		}
		
	}
	def dispatch void format(VarDef ref, extension IFormattableDocument document) {
		for (k : ref.regionFor.keywords(";")) {
			k.prepend[noSpace].append[newLine]
		}
	}
	
	
}


//	protected void configureFormatting(FormattingConfig c) 
//	{
//
//		IGrammarAccess ga = getGrammarAccess() ; 
//		
//		
//		c.setAutoLinewrap(120);
//		
//		// find common keywords an specify formatting for them
//		for (Pair<Keyword, Keyword> pair : ga.findKeywordPairs("(", ")")) 
//		{
//			c.setNoSpace().after(pair.getFirst());
//			c.setNoSpace().before(pair.getSecond());
//		}
//		for (Pair<Keyword, Keyword> pair : ga.findKeywordPairs("[", "]")) 
//		{
//			c.setNoSpace().after(pair.getFirst());
//			c.setNoSpace().before(pair.getSecond());
//		}
//		
//		for (Keyword comma : ga.findKeywords(",")) 
//		{
//			c.setNoSpace().before(comma);
//		}
//		for (Keyword comma : ga.findKeywords(".")) 
//		{
//			c.setNoSpace().before(comma);
//			c.setNoSpace().after(comma);			
//		}
//		
//		
//		// brackets content treatment
//		for(Keyword kw : ga.findKeywords(";"))
//		{
//			c.setLinewrap(1).after(kw) ;
//		}
//		
//		
//		List<Pair<Keyword, Keyword>> bracketsList = ga.findKeywordPairs("{", "}") ; 
//		
//		//c.setLinewrap(1).after(GrammarUtil.findRuleForName(ga.getGrammar(), "ML_COMMENT"));
//		//c.setLinewrap(1).before(GrammarUtil.findRuleForName(ga.getGrammar(), "ML_COMMENT"));
//		
//		
//		for (TerminalRule tr : GrammarUtil.allTerminalRules(ga.getGrammar())) {
//			if (isCommentRule(tr)) {
//				c.setIndentation(tr.getAlternatives(), tr.getAlternatives());
//
//			}
//		}
//		for(Keyword kw : ga.findKeywords("int", "list", "array"))
//		{
//			c.setLinewrap(1).before(kw) ;
//		}
//
//		
//		for(Keyword kw : ga.findKeywords("transition"))
//		{
//			c.setLinewrap(1).before(kw) ;
//		}
//		
//		c.setLinewrap(1).after(GrammarUtil.findRuleForName(ga.getGrammar(), "COMMENT"));
//		
//		// brackets treatment
//		for(Pair<Keyword, Keyword> pair : bracketsList)
//		{
//			// a space before the first '{'
//			c.setSpace(" ").before(pair.getFirst());
//			// Indentation between
//			c.setIndentation(pair.getFirst(), pair.getSecond());
//			
//			// newline after '{'
//			c.setLinewrap(1).after(pair.getFirst()) ;
//			
//			// newline before and after '}'
//			c.setLinewrap(1).before(pair.getSecond()) ;
//			c.setLinewrap(1).after(pair.getSecond())  ;
//			
//		}
//		
//		for(Keyword kw : ga.findKeywords("else"))
//		{
//			c.setLinewrap(0).before(kw) ;
//		} 
//		c.setLinewrap(0, 1, 2).before(GrammarUtil.findRuleForName(ga.getGrammar(), "SL_COMMENT")) ; 
//		c.setLinewrap(0, 1, 1).after (GrammarUtil.findRuleForName(ga.getGrammar(), "ML_COMMENT")) ;
//	}
//
//	
//	private boolean isCommentRule(TerminalRule term) {
//		
//		return "ML_COMMENT".equals(term.getName()) 
//			|| "SL_COMMENT".equals(term.getName())
//			|| "COMMENT".equals(term.getName());
//	}
