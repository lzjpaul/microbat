package microbat.instrumentation.filter;

import org.apache.bcel.generic.ArrayInstruction;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.InvokeInstruction;

public class EmptyInstrFilter implements IInstrFilter {
	private static final EmptyInstrFilter instance = new EmptyInstrFilter();
	
	EmptyInstrFilter() {
		
	}
	public static EmptyInstrFilter getInstance() {
		return instance;
	}

	@Override
	public boolean isValid(InvokeInstruction instruction, ConstantPoolGen constPool) {
		return true;
	}
	@Override
	public boolean isValid(FieldInstruction instruction) {
		return true;
	}
	@Override
	public boolean isValid(ArrayInstruction instruction) {
		return true;
	}
}
