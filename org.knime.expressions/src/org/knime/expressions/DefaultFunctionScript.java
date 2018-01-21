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

/*
 * ------------------------------------------------------------------------


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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * A default {@link FunctionScript} used to execute a script provided by its
 * class.
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 *
 */
public class DefaultFunctionScript implements FunctionScript {
	private final Class<?> m_scriptClass;
	private final ParsedScript m_info;
	private Object m_scriptObject;

	private String[] m_columnNames;

	public DefaultFunctionScript(Class<?> scriptClass, ParsedScript info) {
		m_scriptClass = scriptClass;
		m_info = info;
	}

	/**
	 * Executes the script for the given input.
	 * 
	 * @param input
	 *            Array providing the input arguments needed by the script.
	 * @return Result of executing the script with the given parameters.
	 */
	@Override
	public Object apply(Object[] input) {
		int expectedInputLength = getNrArgs();
		int inputLength = input == null ? 0 : input.length;

		if (expectedInputLength != inputLength) {
			throw new IllegalArgumentException("Number of input arguments (" + inputLength
					+ ") is not equal to the number of needed arguments (" + expectedInputLength + ")");
		}

		/* Creates object of the compiled script only once. */
		if (m_scriptObject == null) {
			try {
				m_scriptObject = m_scriptClass.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		Map<String, String> columnNameMap = m_info.getColumnNameMap();

		Class<?>[] columnFieldTypes = m_info.getFieldTypes();
		Map<String, Integer> parsedColumnInputMap = m_info.getFieldInputMap();

		/* Sets the value for each field provided by input. */
		for (String fieldName : columnNameMap.values()) {
			String setterName = "set" + fieldName;

			try {
				Method method = m_scriptClass.getMethod(setterName,
						columnFieldTypes[parsedColumnInputMap.get(fieldName)]);
				method.invoke(m_scriptObject, input[parsedColumnInputMap.get(fieldName)]);

			} catch (NoSuchMethodException | SecurityException e) {
				// TODO cant get method exception.
				e.printStackTrace();
			} catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
				// TODO couldnt invoke...
				e.printStackTrace();
			}
		}

		/*
		 * Executes the script by simply invoking the main method (METHOD_NAME) and
		 * returning the last computed statement.
		 */
		Object returnValue = null;

		try {
			Method method = m_scriptClass.getMethod(METHOD_NAME);

			returnValue = method.invoke(m_scriptObject);
		} catch (NoSuchMethodException | SecurityException e) {
			// TODO cant get method exception.
			e.printStackTrace();
		} catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
			// TODO couldnt invoke...
			e.printStackTrace();
		}

		return returnValue;
	}

	/**
	 * 
	 */
	@Override
	public int getNrArgs() {
		int nrArgs = 0;

		nrArgs += m_info.usesROWID() ? 1 : 0;
		nrArgs += m_info.usesROWINDEX() ? 1 : 0;
		nrArgs += m_info.usesROWCOUNT() ? 1 : 0;
		nrArgs += m_info.usesColumns() ? m_info.getFieldTypes().length : 0;

		return nrArgs;
	}

	/**
	 * 
	 */
	@Override
	public Class<?> type(int idx) {
		int nrColumns = m_info.usesColumns() ? 0 : m_info.getFieldTypes().length;

		if (idx >= nrColumns || idx < 0) {
			return null;
		}

		return m_info.getFieldTypes()[idx];
	}

	/**
	 * 
	 */
	@Override
	public int argIdxOf(String originalName) {
		Integer idx = m_info.getFieldInputMap().get(m_info.getColumnNameMap().get(originalName));

		return idx == null ? -1 : idx;
	}

	/**
	 * 
	 */
	@Override
	public Class<?> getReturnType() {
		return m_info.getReturnType();
	}

	/**
	 * 
	 */
	@Override
	public int originalIdxOf(String originalName) {
		Integer idx = m_info.getColumnTableMap().get(originalName);

		return idx == null ? -1 : idx;
	}

	/**
	 * 
	 */
	@Override
	public String[] getColumnNames() {
		if (m_columnNames == null) {
			m_columnNames = new String[m_info.getColumnNameMap().size()];

			int i = 0;
			for (String name : m_info.getColumnNameMap().keySet()) {
				m_columnNames[i++] = name;
			}
		}

		return m_columnNames;
	}
}
