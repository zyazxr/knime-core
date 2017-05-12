/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 10, 2017 (adewi): created
 */
package org.knime.core.node.port.database.metadata.filter.selected;

import org.knime.core.node.port.database.metadata.model.DBObject;
import org.knime.core.node.port.database.metadata.model.DBTable;

/**
 * Implementation of the SelectedElementFilter for selecting Table. In this case, the selected table name and its
 * corresponding schema name are stored.
 *
 * @author Andisa Dewi, KNIME.com, Berlin, Germany
 * @since 3.4
 */
public class SelectedTableFilter implements SelectedElementFilter {

    private String m_schemaName;

    private String m_tableName;

    /**
     * Constructor for the selected table filter.
     *
     */
    public SelectedTableFilter() {
        m_schemaName = "";
        m_tableName = "";
    }

    /**
     * Set the schema name.
     *
     * @param schema the schema name
     */
    public void setSchema(final String schema) {
        m_schemaName = schema;
    }

    /**
     * Get the schema name.
     *
     * @return the schema name
     */
    public String getSchema() {
        return m_schemaName;
    }

    /**
     * Set the selected table name.
     *
     * @param table the table name
     */
    public void setTable(final String table) {
        m_tableName = table;
    }

    /**
     * Get the selected table name.
     *
     * @return the table name
     */
    public String getTable() {
        return m_tableName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean filter(final DBObject object) {
        if (m_tableName.isEmpty()) {
            return false;
        }
        if (object instanceof DBTable) {
            DBTable table = (DBTable)object;
            return m_schemaName.equals(table.getSchemaName()) && m_tableName.equals(table.getName());
        }
        return false;
    }

}
