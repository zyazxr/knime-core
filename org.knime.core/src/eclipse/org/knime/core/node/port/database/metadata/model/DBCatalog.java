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
 *   May 4, 2017 (adewi): created
 */
package org.knime.core.node.port.database.metadata.model;

import java.util.Collection;
import java.util.HashMap;

import schemacrawler.schema.Catalog;
import schemacrawler.schema.NamedObject;
import schemacrawler.schema.Schema;
import schemacrawler.schema.Table;

/**
 * This class is a wrapper for the Schemacrawler Catalog object. This is needed to store a schemas-tables mapping of the
 * database.
 *
 * @author Andisa Dewi, KNIME.com, Berlin, Germany
 * @since 3.4
 */
public class DBCatalog implements NamedObject {

    private static final long serialVersionUID = -7199412792869358654L;

    private final Catalog m_catalog;

    private final HashMap<Schema, Collection<Table>> m_tableMapping = new HashMap<Schema, Collection<Table>>();

    /**
     * The wrapper for Schemacrawler Catalog object.
     *
     * @param catalog the catalog from Schemacrawler
     */
    public DBCatalog(final Catalog catalog) {
        m_catalog = catalog;
        loadTableMapping();
    }

    /**
     * Do the schemas-tables mapping. This should be done at the beginning and only once for faster loading.
     */
    private void loadTableMapping() {
        Collection<Schema> schemas = m_catalog.getSchemas();
        for (Schema schema : schemas) {
            m_tableMapping.put(schema, m_catalog.getTables(schema));
        }
    }

    /**
     * @return all tables in this catalog
     *
     */
    public Collection<Table> getTables() {
        return m_catalog.getTables();
    }

    /**
     * @param schema the schema, whose tables should be returned
     * @return the tables contained in the input schema
     *
     */
    public Collection<Table> getTables(final Schema schema) {
        return m_tableMapping.get(schema);
    }

    /**
     * @return all schemas in this catalog
     *
     */
    public Collection<Schema> getSchemas() {
        return m_catalog.getSchemas();
    }

    /**
     * Get the name of the RDBMS vendor and product.
     *
     * @return the database name
     */
    public String getDatabaseName() {
        return m_catalog.getDatabaseInfo().getProductName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return m_catalog.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final NamedObject o) {
        return m_catalog.compareTo(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFullName() {
        return m_catalog.getFullName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLookupKey() {
        return m_catalog.getLookupKey();
    }

}
