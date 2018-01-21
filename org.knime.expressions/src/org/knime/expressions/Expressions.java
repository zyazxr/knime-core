/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 */
package org.knime.expressions;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.expressions.node.ExpressionCompletionProvider;
import org.knime.ext.sun.nodes.script.expression.Expression;

import groovy.lang.GroovyClassLoader;

/**
 * Class which provides methods to parse a script, compile it to a Java class
 * and to wrap it in such a way that it can be easily invoked with the given
 * data.
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 *
 */
public class Expressions {

	private final static String CLASS_NAME = "KNIME_GroovyClass";
	private final static String METHOD_NAME = FunctionScript.METHOD_NAME;
	private static int counter = 0;

	static ParsedScript parseScript(String script, DataColumnSpec[] columns, FlowVariable[] variables,
			final DataType returnType) {
		/*
		 * Escape characters used to mark column names and flow variables in the
		 * expression
		 */
		String escapeColumnStart = ExpressionCompletionProvider.getEscapeColumnStartSymbol();
		String escapeColumnEnd = ExpressionCompletionProvider.getEscapeColumnEndSymbol();
		String escapeFlowVariableStart = ExpressionCompletionProvider.getEscapeFlowVariableStartSymbol();
		String escapeFlowVariableEnd = ExpressionCompletionProvider.getEscapeFlowVariableEndSymbol();

		/*
		 * HashMap used to check if a column/flow variable found in the script actually
		 * exists. Furthermore it stores the DataType/Type so that we can easily access
		 * it with only the original name, i.e. without delimiters.
		 */
		HashMap<String, DataType> columnNameDataTypeMap = new HashMap<>();
		HashMap<String, Type> flowVariableTypeMap = new HashMap<>();

		/* Mapping the original names to the original indexes. */
		HashMap<String, Integer> originalColumnNameIndexMap = new HashMap<>();
		HashMap<String, Integer> originalFlowVariableNameIndexMap = new HashMap<>();

		for (int i = 0; i < columns.length; i++) {
			DataColumnSpec column = columns[i];
			columnNameDataTypeMap.put(column.getName(), column.getType());
			originalColumnNameIndexMap.put(column.getName(), i);
		}

		for (int i = 0; i < variables.length; i++) {
			FlowVariable variable = variables[i];
			flowVariableTypeMap.put(variable.getName(), variable.getType());
			originalFlowVariableNameIndexMap.put(variable.getName(), i);
		}

		/*
		 * List to store the found column and flow variable names.
		 */
		LinkedList<String> foundColumnList = new LinkedList<>();
		LinkedList<String> foundFlowVariableList = new LinkedList<>();

		/*
		 * booleans to determine if ROWID, ROWINDEX, or ROWCOUNT is used in the
		 * expression.
		 */
		boolean containsROWID = false;
		boolean containsROWINDEX = false;
		boolean containsROWCOUNT = false;

		/*
		 * Split expression into lines, so that we are able to parse line by line. This
		 * makes it easier concerning the handling of the end delimiter (e.g. if its not
		 * in the same line).
		 */
		String[] lines = StringUtils.split(script, "\n");

		/* Search for used column names using the escape characters. */
		for (int i = 0; i < lines.length; i++) {
			int startIndex = 0;
			String line = lines[i];

			/*
			 * Check if ROWID, ROWINDEX, or ROWCOUNT is used in the expression.
			 */
			containsROWID = containsROWID
					|| StringUtils.contains(line, ExpressionCompletionProvider.getEscapeExpressionStartSymbol()
							+ Expression.ROWID + ExpressionCompletionProvider.getEscapeExpressionEndSymbol());

			containsROWINDEX = containsROWINDEX
					|| StringUtils.contains(line, ExpressionCompletionProvider.getEscapeExpressionStartSymbol()
							+ Expression.ROWINDEX + ExpressionCompletionProvider.getEscapeExpressionEndSymbol());

			containsROWCOUNT = containsROWCOUNT
					|| StringUtils.contains(line, ExpressionCompletionProvider.getEscapeExpressionStartSymbol()
							+ Expression.ROWCOUNT + ExpressionCompletionProvider.getEscapeExpressionEndSymbol());

			/* Columns */
			/* Continue until we've read all start delimiters. */
			while ((startIndex = StringUtils.indexOf(line, escapeColumnStart, startIndex)) >= 0) {
				int endIndex = StringUtils.indexOf(line, escapeColumnEnd, startIndex + 1);

				if (endIndex < 0) {
					throw new IllegalArgumentException("No such column: " + StringUtils.substring(line, startIndex + 1)
							+ " (at line " + i + ") \n\n expression: \n" + script);
				}

				/* Found a column name. */
				String foundColumn = StringUtils.substring(line, startIndex, endIndex + escapeColumnEnd.length());
				String column = StringUtils.substring(foundColumn, escapeColumnStart.length(),
						foundColumn.length() - escapeColumnEnd.length());

				if (!columnNameDataTypeMap.containsKey(column)) {

					throw new IllegalArgumentException(
							"Column '" + column + "' in line " + i + " is not known. \n\nexpression:\n" + script);
				}

				foundColumnList.add(foundColumn);

				/*
				 * Update startIndex in case the end escape is the same as the start escape
				 */
				startIndex = endIndex + escapeColumnEnd.length();
			}

			startIndex = 0;

			/* Flow variables */
			/* Continue until we've read all start delimiters. */
			while ((startIndex = StringUtils.indexOf(line, escapeFlowVariableStart, startIndex)) >= 0) {
				int endIndex = StringUtils.indexOf(line, escapeFlowVariableEnd, startIndex + 1);

				if (endIndex < 0) {
					throw new IllegalArgumentException("No such column: " + StringUtils.substring(line, startIndex + 1)
							+ " (at line " + i + ") \n\n expression: \n" + script);
				}

				String foundVariable = StringUtils.substring(line, startIndex,
						endIndex + escapeFlowVariableEnd.length());
				String flowVariable = StringUtils.substring(foundVariable, escapeColumnStart.length(),
						foundVariable.length() - escapeFlowVariableEnd.length());

				if (!flowVariableTypeMap.containsKey(flowVariable)) {
					throw new IllegalArgumentException("Flow variable '" + flowVariable + "' in line " + (i + 1)
							+ " is not known. \n\nexpression:\n" + script);
				}

				foundFlowVariableList.add(foundVariable);

				/*
				 * Update startIndex in case the end escape is the same as the start escape
				 */
				startIndex = endIndex + escapeFlowVariableEnd.length();
			}
		}

		/* Parse the names of the found columns and flow variables. */
		String[] columnNames = foundColumnList.toArray(new String[foundColumnList.size()]);
		String[] flowVariableNames = foundFlowVariableList.toArray(new String[foundFlowVariableList.size()]);

		/* Mapping from original names to parsed names. */
		Map<String, String> originalToParsedColumnMap = parseNames(script, columnNames);
		Map<String, String> originalToParsedFlowVariableMap = parseNames(script, flowVariableNames);

		/* Mapping from the parsed name to its DataType/Type. Used to create fields. */
		HashMap<String, DataType> parsedColumnDataTypeMap = new HashMap<>();

		/*
		 * Create and fill arrays which are used (i.e. names with delimiter) to replace
		 * the found names with their parsed names.
		 */
		String[] parsedNames = new String[columnNames.length + flowVariableNames.length];
		String[] scriptNames = new String[parsedNames.length];

		HashMap<String, Integer> parsedNameFieldMap = new HashMap<>();

		for (int i = 0; i < parsedNames.length; i++) {
			if (i < columnNames.length) {
				/* Column names */
				scriptNames[i] = columnNames[i];

				/* Add the DataType of each parsed name to the map. */
				String originalName = StringUtils.substring(scriptNames[i], escapeColumnStart.length(),
						scriptNames[i].length() - escapeColumnEnd.length());

				parsedNames[i] = originalToParsedColumnMap.get(originalName);
				parsedColumnDataTypeMap.put(parsedNames[i], columnNameDataTypeMap.get(originalName));
				parsedNameFieldMap.put(parsedNames[i], i);
			} else {
				/* Flow variable names. */
				scriptNames[i] = flowVariableNames[i - columnNames.length];

				String originalName = StringUtils.substring(scriptNames[i], escapeFlowVariableStart.length(),
						scriptNames[i].length() - escapeFlowVariableEnd.length());

				parsedNames[i] = originalToParsedFlowVariableMap.get(originalName);
			}
		}

		/* Replace the names with their parsed names. */
		String replacedScript = StringUtils.replaceEach(script, scriptNames, parsedNames);

		/* Creates the fields together with their getters and setters. */
		String[] columnFields = createColumnFields(parsedColumnDataTypeMap);
		String variableFields = createVariableFields(variables, originalToParsedFlowVariableMap,
				originalFlowVariableNameIndexMap);

		/* Appends needed methods provided by ExpressionSetRegistry. */
		String methods = createMethods(script);

		/* Creates the parsed script, wrapped in a method wrapped in a class. */
		StringBuilder classBuilder = new StringBuilder();

		/* Start of class */
		classBuilder.append("class ");
		classBuilder.append(CLASS_NAME + (counter++));
		classBuilder.append("{\n");
		/* Fields */
		classBuilder.append(columnFields[0]);
		classBuilder.append("\n");
		classBuilder.append(variableFields);
		classBuilder.append("\n");
		/* Main method */
		classBuilder.append(ExpressionConverterUtils.extractJavaTypeString(returnType));
		classBuilder.append(" ");
		classBuilder.append(METHOD_NAME);
		classBuilder.append("() {\n");
		/* Parsed script */
		classBuilder.append(replacedScript);
		classBuilder.append("\n}\n");
		/* Setters */
		classBuilder.append(columnFields[1]);
		/* Getters */
		classBuilder.append(columnFields[2]);
		/* Methods */
		classBuilder.append(methods);
		/* End of class*/
		classBuilder.append("}");

		/* TODO: append found predefined expressions like AND() OR(),.... */
		/* TODO: ROWID etc replace name. */

		/* Types of the used fields. */
		Class<?>[] columnFieldTypes = getUsedColumnTypes(columnNameDataTypeMap, parsedNameFieldMap,
				originalToParsedColumnMap);

		/* Return type of the script. */
		Class<?> javaReturnType = ExpressionConverterUtils.getDestinationType(returnType);

		return new DefaultParsedScript(classBuilder.toString(), javaReturnType, parsedNameFieldMap,
				originalToParsedColumnMap, originalColumnNameIndexMap, columnFieldTypes, containsROWID,
				containsROWINDEX, containsROWCOUNT);
	}

	/**
	 * Appends needed methods provided by {@link ExpressionSetRegistry}
	 * 
	 * @param script
	 *            The script to be compiled.
	 * @return String containing already defined methods.
	 */
	private static String createMethods(String script) {
		List<ExpressionSet> expressionSets = ExpressionSetRegistry.getExpressionSets();

		StringBuilder methodBuilder = new StringBuilder();

		for (ExpressionSet set : expressionSets) {
			for (org.knime.expressions.Expression exp : set.getExpressionManipulators()) {
				if (StringUtils.contains(script, exp.getName() + "(")) {
					methodBuilder.append("\n\n");
					methodBuilder.append(exp.getScript());
				}

			}
		}

		return methodBuilder.toString();
	}

	/**
	 * Creates initialized fields with the values of the {@link FlowVariable}s.
	 * 
	 * @param variables
	 *            Available {@link FlowVariables}.
	 * @param originalToParsedNameMap
	 *            Mapping from original to parsed names.
	 * @param originalFlowVariableNameIndexMap
	 *            Mapping from original names to the indexes in the provided array.
	 * @return
	 */
	private static String createVariableFields(FlowVariable[] variables, Map<String, String> originalToParsedNameMap,
			Map<String, Integer> originalFlowVariableNameIndexMap) {
		StringBuilder fieldBuilder = new StringBuilder();

		for (String originalName : originalToParsedNameMap.keySet()) {
			FlowVariable variable = variables[originalFlowVariableNameIndexMap.get(originalName)];
			String field = originalToParsedNameMap.get(originalName);

			String javaType = ExpressionConverterUtils.extractJavaTypeString(variable.getType());
			Object value = ExpressionConverterUtils.extractFlowVariable(variable);

			String valueString = value.toString();

			if (variable.getType() == Type.STRING) {
				valueString = "\"" + StringUtils.replace(valueString, "\\", "\\\\") + "\"";
			}

			fieldBuilder.append(javaType + " " + field + " = " + valueString + ";\n");

		}

		return fieldBuilder.toString();
	}

	/**
	 * Gets the Java types used by the fields in the script. The types are in the
	 * same order as the fields occur.
	 * 
	 * @param columnTypeMap
	 *            Mapping from original column names to their types.
	 * @param columnIndexMap
	 *            Mapping from parsed column names to their field index in the
	 *            script.
	 * @param columnNameMap
	 *            Mapping from original column names to parsed column names.
	 * @return Array containing the field's {@link Class}es.
	 */
	private static Class<?>[] getUsedColumnTypes(Map<String, DataType> columnTypeMap,
			Map<String, Integer> columnIndexMap, Map<String, String> columnNameMap) {
		Class<?>[] types = new Class<?>[columnNameMap.size()];

		for (String originalName : columnNameMap.keySet()) {
			String parsedName = columnNameMap.get(originalName);

			types[columnIndexMap.get(parsedName)] = ExpressionConverterUtils
					.getDestinationType(columnTypeMap.get(originalName));
		}

		return types;
	}

	// /**
	// * Gets the Java types used by the fields in the script. The types are in the
	// * same order as the fields occur.
	// *
	// * @param variableTypeMap
	// * Mapping from original flow variable names to their types.
	// * @param variableIndexMap
	// * Mapping from original flow variable names to their field index in
	// * the script.
	// * @param variableNameMap
	// * Mapping from original flow variable names to parsed flow variable
	// * names.
	// * @return Array containing the field's {@link Class}es.
	// */
	// private static Class<?>[] getFlowVariableTypes(Map<String, Type>
	// variableTypeMap,
	// Map<String, Integer> variableIndexMap, Map<String, String> variableNameMap) {
	// Class<?>[] types = new Class<?>[variableIndexMap.size()];
	//
	// for (String originalName : variableTypeMap.keySet()) {
	// String parsedName = variableNameMap.get(originalName);
	//
	// types[variableIndexMap.get(parsedName)] = ExpressionConverterUtils
	// .getDestinationType(variableTypeMap.get(originalName));
	// }
	//
	// return types;
	// }

	/**
	 * Creates the fields together with their getters and setters, which are used to
	 * create the class containing the script.
	 * 
	 * @param columnDataTypeMap
	 *            Map containing the {@link DataType} for each column that will be
	 *            represented as a field.
	 * @return Array containing the field declarations at position 0, the setters at
	 *         position 1, and the getters at position 2.
	 */
	private static String[] createColumnFields(Map<String, DataType> columnDataTypeMap) {
		StringBuilder fieldBuilder = new StringBuilder();
		StringBuilder getterBuilder = new StringBuilder();
		StringBuilder setterBuilder = new StringBuilder();

		for (String field : columnDataTypeMap.keySet()) {
			String javaType = "";

			javaType = ExpressionConverterUtils.extractJavaTypeString(columnDataTypeMap.get(field));

			fieldBuilder.append(javaType + " " + field + ";\n");

			String setter = "\n\n void set" + field + "(" + javaType + " arg) {" + field + " = arg;}";
			setterBuilder.append(setter);

			String getter = "\n\n" + javaType + " get" + field + "(){ return " + field + ";}";
			getterBuilder.append(getter);
		}

		return new String[] { fieldBuilder.toString(), setterBuilder.toString(), getterBuilder.toString() };
	}

	// this is actually just an intermediate step which might be useful later... (or
	// not)
	static FunctionScript compile(ParsedScript info) {

		GroovyClassLoader loader = new GroovyClassLoader();
		Class<?> parsedClass = loader.parseClass(info.getScript());
		try {
			loader.close();
		} catch (IOException e) {
			// Should not happen as we parse the class from a String and not a File.
			e.printStackTrace();
		}

		// ExpressionConverterUtils.getDataCellToJavaConverter(null).
		return new DefaultFunctionScript(parsedClass, info);
	}

	static Function<DataRow, DataCell> wrap(FunctionScript script) {
		return new DefaultWrappedFunctionScript(script);
	}

	/**
	 * Parses the used column and flow variable names in such a way that special
	 * characters are replaced. Additionally, column names will get the prefix "c_"
	 * whereas flow variables will get the prefix "f_". This ensures that flow
	 * variables and column variables with the same name can be distinguished.
	 * Furthermore the script will be checked if the parsed name is already in use
	 * and may alternate it depending on the given result. This method should be
	 * called separately for column names and flow variable names.
	 * 
	 * @param script
	 *            The original script that is being parsed.
	 * @param names
	 *            The used names together with their delimiters.
	 * 
	 * @return A mapping from the original column/flow variable names to their
	 *         parsed names.
	 */
	private static Map<String, String> parseNames(final String script, final String[] names) {
		HashMap<String, String> nameMap = new HashMap<>(names.length);

		if (names.length == 0) {
			return nameMap;
		}

		HashSet<String> usedNames = new HashSet<>(names.length);
		Random random = new Random();

		/* Check if the given names are flow variables or column variables. */
		boolean isFlowVariable = names[0].startsWith(ExpressionCompletionProvider.getEscapeFlowVariableStartSymbol());

		for (String name : names) {
			/* Strip the found name down to the actual column name. */
			if (isFlowVariable) {
				name = name.substring(ExpressionCompletionProvider.getEscapeFlowVariableStartSymbol().length(),
						name.length() - ExpressionCompletionProvider.getEscapeFlowVariableEndSymbol().length());
			} else {
				name = name.substring(ExpressionCompletionProvider.getEscapeColumnStartSymbol().length(),
						name.length() - ExpressionCompletionProvider.getEscapeColumnEndSymbol().length());
			}

			/* Remove all characters that aren't letters or numbers or '_'. */
			String parsedName = name.replaceAll("[^A-Za-z0-9_]", "");

			/*
			 * Add the specific prefix (also ensures that a variable doesn't start with a
			 * number).
			 */
			if (isFlowVariable) {
				parsedName = "f_" + parsedName;
			} else {
				parsedName = "c_" + parsedName;
			}

			/*
			 * If the name is already contained in the script (i.e. a variable with the same
			 * name exist), append a random number until we have a free variable name.
			 */
			while (script.contains(parsedName) || usedNames.contains(parsedName)) {
				parsedName += random.nextInt(100);
			}

			usedNames.add(parsedName);
			nameMap.put(name, parsedName);
		}

		return nameMap;
	}
}
