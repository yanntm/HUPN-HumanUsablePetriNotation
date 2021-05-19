# HUPN-HumanUsablePetriNotation

A textual format to manipulate high-level colored Petri nets. Compatible with PNML.

The ISO standard for the Petri net Markup Language (PNML) defines an XML based exchange format for High-Level Petri nets.

While there exist examples of HLPN expressed in this format, such as the models used in the Model-Checking Competition (https://mcc.lip6.fr)
it is is quite difficult to find a tool that can comfortably edit high-level nets.

This tool consists in a simple textual format for HLPN, and offers import/export to and from PNML.
It is based on Xtext, so we have a nice feature rich editor (error correction, auto-completion...) and also an EMF compliant metamodel.

Development of properties expressed in a similar way is in progress, enabling easier use of the tools participating in the MCC.

## Installation

Use the following update site address in "Help->Install New Software" menu of a recent eclipse : https://yanntm.github.io/HUPN-HumanUsablePetriNotation/

You can then right-click a PNML file and ask to generate a HUPN representation.

## Example

### Example 1 : DrinkVending

Source model can be examined here : https://mcc.lip6.fr/pdf/DrinkVendingMachine-form.pdf

```
net DrinkVendingMachine_COL_02 {
	sort Quality is { Quality1 , Quality2 , Quality3 , Quality4 , Quality5 , Quality6 , Quality7 , Quality8 };
	sort Products is { Products1 , Products2 };
	sort Options is { Options1 , Options2 };
	place ready sort Quality;
	place wait sort Quality initial <all>;
	place theProducts sort Products initial <all>;
	place productSlots sort Products;
	place theOptions sort Options initial <all>;
	place optionSlots sort Options;
	var Products $p;
	var Options $o3;
	var Options $o2;
	var Options $o1;
	var Quality $x;
	transition elaborate3 [ $x > Quality6 && $x <= Quality8 ] {
		pre wait <$x>;
		pre theProducts <$p>;
		pre theOptions <$o1> + <$o2> + <$o3>;
		post productSlots <$p>;
		post optionSlots <$o1> + <$o2> + <$o3>;
		post ready <$x>;
	}
	transition elaborate2 [ $x > Quality4 && $x <= Quality6 ] {
		pre theOptions <$o1> + <$o2>;
		pre wait <$x>;
		pre theProducts <$p>;
		post productSlots <$p>;
		post ready <$x>;
		post optionSlots <$o1> + <$o2>;
	}
	transition elaborate1 [ $x > Quality2 && $x <= Quality4 ] {
		pre wait <$x>;
		pre theProducts <$p>;
		pre theOptions <$o1>;
		post ready <$x>;
		post productSlots <$p>;
		post optionSlots <$o1>;
	}
	transition serve [ true ] {
		pre ready <$x>;
		post wait <$x>;
	}
	transition elaborate0 [ $x <= Quality2 ] {
		pre theProducts <$p>;
		pre wait <$x>;
		post ready <$x>;
		post productSlots <$p>;
	}
	transition addProduct [ true ] {
		pre productSlots <$p>;
		post theProducts <$p>;
	}
	transition addOption [ true ] {
		pre optionSlots <$o1>;
		post theOptions <$o1>;
	}
}
```

### Example 2 : Peterson's mutual exclusion

Source model can be examined here : https://mcc.lip6.fr/pdf/Peterson-form.pdf

```
net Peterson_COL_4 {
	sort Process is { process0 , process1 , process2 , process3 , process4 };
	sort Tour is { tour0 , tour1 , tour2 , tour3 };
	sort Bool is { false , true };
	sort ProcTour is Process x Tour;
	sort TourProc is Tour x Process;
	sort ProcTourProc is Process x Tour x Process;
	sort ProcBool is Process x Bool;
	place Idle sort Process initial <all>;
	place WantSection sort ProcBool initial <process0, ^false> + <process1, ^false> + <process2, ^false> + <process3,
	^false> + <process4, ^false>;
	place AskForSection sort ProcTour;
	place Turn sort TourProc initial <tour0, process0> + <tour1, process0> + <tour2, process0> + <tour3, process0>;
	place TestTurn sort ProcTour;
	place BeginLoop sort ProcTourProc;
	place EndTurn sort ProcTour;
	place CS sort Process;
	place TestIdentity sort ProcTourProc;
	place TestAlone sort ProcTourProc;
	place IsEndLoop sort ProcTourProc;
	var Process $i;
	var Process $k;
	var Tour $j;
	transition Ask [ true ] {
		pre Idle <$i>;
		pre WantSection <$i, ^false>;
		post WantSection <$i, ^true>;
		post AskForSection <$i, tour0>;
	}
	transition UpdateTurn [ true ] {
		pre Turn <$j, $k>;
		pre AskForSection <$i, $j>;
		post Turn <$j, $i>;
		post TestTurn <$i, $j>;
	}
	transition TurnEqual [ true ] {
		pre Turn <$j, $i>;
		pre TestTurn <$i, $j>;
		post Turn <$j, $i>;
		post BeginLoop <$i, $j, process0>;
	}
	transition TurnDiff [ $i != $k ] {
		pre TestTurn <$i, $j>;
		pre Turn <$j, $k>;
		post EndTurn <$i, $j>;
		post Turn <$j, $k>;
	}
	transition AccessCS [ true ] {
		pre EndTurn <$i, tour3>;
		post CS <$i>;
	}
	transition ProgressTurn [ $j != tour3 ] {
		pre EndTurn <$i, $j>;
		post AskForSection <$i, $j ++>;
	}
	transition BecomeIdle [ true ] {
		pre WantSection <$i, ^true>;
		pre CS <$i>;
		post Idle <$i>;
		post WantSection <$i, ^false>;
	}
	transition ContinueLoop [ true ] {
		pre BeginLoop <$i, $j, $k>;
		post TestIdentity <$i, $j, $k>;
	}
	transition Identity [ true ] {
		pre TestIdentity <$i, $j, $i>;
		post IsEndLoop <$i, $j, $i>;
	}
	transition NoIdentity [ $i != $k ] {
		pre TestIdentity <$i, $j, $k>;
		post TestAlone <$i, $j, $k>;
	}
	transition Loop [ $k != process4 ] {
		pre IsEndLoop <$i, $j, $k>;
		post BeginLoop <$i, $j, $k ++>;
	}
	transition NotAlone [ true ] {
		pre TestAlone <$i, $j, $k>;
		pre WantSection <$k, ^true>;
		post WantSection <$k, ^true>;
		post TestTurn <$i, $j>;
	}
	transition Alone1 [ true ] {
		pre WantSection <$k, ^false>;
		pre TestAlone <$i, $j, $k>;
		post WantSection <$k, ^false>;
		post IsEndLoop <$i, $j, $k>;
	}
	transition EndLoop [ true ] {
		pre IsEndLoop <$i, $j, process4>;
		post EndTurn <$i, $j>;
	}
}

```




This software is released under the terms of GPL V3.

(C) Yann Thierry-Mieg, LIP6, Sorbonne Université & CNRS
