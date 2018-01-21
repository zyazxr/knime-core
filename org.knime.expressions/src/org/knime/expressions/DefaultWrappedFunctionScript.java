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

import java.util.function.Function;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Default wrapper that wraps a {@link FunctionScript} and executes the script
 * on the provided {@link DataRow} while returning the {@link DataCell} with the
 * result.
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 *
 */
public class DefaultWrappedFunctionScript implements Function<DataRow, DataCell> {

	private final FunctionScript m_functionScript;

	/**
	 * Creates an object.
	 * 
	 * @param functionScript
	 *            The function script which contains the script that shall be
	 *            executed.
	 */
	public DefaultWrappedFunctionScript(FunctionScript functionScript) {
		m_functionScript = functionScript;
	}

	/**
	 * Applys the provided script to the given {@link DataRow} and returns its
	 * result as a {@link DataCell} using the preferred converters.
	 * 
	 * @param inputRow
	 *            {@link DataRow} of the table for which the script shall be
	 *            executed.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public DataCell apply(DataRow inputRow) {
		Object[] input = new Object[m_functionScript.getNrArgs()];

		/* Creates the input for the script. May be empty if no column is being used. */
		for (String name : m_functionScript.getColumnNames()) {
			int col = m_functionScript.originalIdxOf(name);

			DataCell cell = inputRow.getCell(col);

			try {
				input[m_functionScript.argIdxOf(name)] = ExpressionConverterUtils
						.getDataCellToJavaConverter(cell.getType()).convertUnsafe(cell);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		/* Executes the script and converts the result to a DataCell. */
		Object result = m_functionScript.apply(input);

		JavaToDataCellConverter converter = ExpressionConverterUtils
				.getJavaToDataCellConverter(m_functionScript.getReturnType(), null);

		try {
			return converter.convert(result);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

}
