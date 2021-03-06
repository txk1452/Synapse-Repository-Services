package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.table.query.model.FunctionReturnType;
import org.sagebionetworks.table.query.model.SetFunctionSpecification;

public class SetFunctionSpecificationTest {

	@Test
	public void testCountStar() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("count(*)").setFunctionSpecification();
		assertEquals("COUNT(*)", element.toString());
		assertEquals(FunctionReturnType.LONG, element.getFunctionReturnType());
	}
	
	@Test
	public void testCountDistict() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("count(distinct foo)").setFunctionSpecification();
		assertEquals("COUNT(DISTINCT foo)", element.toString());
		assertEquals(FunctionReturnType.LONG, element.getFunctionReturnType());
	}
	
	
	@Test
	public void testMax() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("max(foo)").setFunctionSpecification();
		assertEquals("MAX(foo)", element.toString());
		assertEquals(FunctionReturnType.MATCHES_PARAMETER, element.getFunctionReturnType());
	}
	
	@Test
	public void testMin() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("min(all foo)").setFunctionSpecification();
		assertEquals("MIN(ALL foo)", element.toString());
		assertEquals(FunctionReturnType.MATCHES_PARAMETER, element.getFunctionReturnType());
	}
	
	@Test
	public void testSum() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("sum(foo)").setFunctionSpecification();
		assertEquals("SUM(foo)", element.toString());
		assertEquals(FunctionReturnType.MATCHES_PARAMETER, element.getFunctionReturnType());
	}
	
	@Test
	public void testBitAnd() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("bit_and(foo)").setFunctionSpecification();
		assertEquals("BIT_AND(foo)", element.toString());
		assertEquals(FunctionReturnType.LONG, element.getFunctionReturnType());
	}
	
	@Test
	public void testBitOr() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("bit_or(foo)").setFunctionSpecification();
		assertEquals("BIT_OR(foo)", element.toString());
		assertEquals(FunctionReturnType.LONG, element.getFunctionReturnType());
	}
	
	@Test
	public void testBitXOr() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("bit_xor(foo)").setFunctionSpecification();
		assertEquals("BIT_XOR(foo)", element.toString());
		assertEquals(FunctionReturnType.LONG, element.getFunctionReturnType());
	}
	
	@Test
	public void testSTD() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("std(foo)").setFunctionSpecification();
		assertEquals("STD(foo)", element.toString());
		assertEquals(FunctionReturnType.DOUBLE, element.getFunctionReturnType());
	}
	
	@Test
	public void testSTDDEV() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("stddev(foo)").setFunctionSpecification();
		assertEquals("STDDEV(foo)", element.toString());
		assertEquals(FunctionReturnType.DOUBLE, element.getFunctionReturnType());
	}
	
	@Test
	public void testSTDDEV_POP() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("stddev_pop(foo)").setFunctionSpecification();
		assertEquals("STDDEV_POP(foo)", element.toString());
		assertEquals(FunctionReturnType.DOUBLE, element.getFunctionReturnType());
	}
	
	@Test
	public void testSTDDEV_SAMP() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("STDDEV_SAMP(foo)").setFunctionSpecification();
		assertEquals("STDDEV_SAMP(foo)", element.toString());
		assertEquals(FunctionReturnType.DOUBLE, element.getFunctionReturnType());
	}
	
	@Test
	public void testVAR_POP() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("VAR_POP(foo)").setFunctionSpecification();
		assertEquals("VAR_POP(foo)", element.toString());
		assertEquals(FunctionReturnType.DOUBLE, element.getFunctionReturnType());
	}
	
	@Test
	public void testVAR_SAMP() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("VAR_SAMP(foo)").setFunctionSpecification();
		assertEquals("VAR_SAMP(foo)", element.toString());
		assertEquals(FunctionReturnType.DOUBLE, element.getFunctionReturnType());
	}
	
	@Test
	public void testVARIANCE() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("VARIANCE(foo)").setFunctionSpecification();
		assertEquals("VARIANCE(foo)", element.toString());
		assertEquals(FunctionReturnType.DOUBLE, element.getFunctionReturnType());
	}
	
	@Test
	public void testIsAggregate() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("COUNT(one)").generalSetFunction();
		assertTrue(element.isElementAggregate());
	}

}
