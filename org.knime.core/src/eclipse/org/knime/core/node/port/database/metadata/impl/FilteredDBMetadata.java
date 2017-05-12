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
 *   Apr 20, 2017 (adewi): created
 */
package org.knime.core.node.port.database.metadata.impl;

import org.knime.core.node.port.database.metadata.filter.search.SearchFilter;
import org.knime.core.node.port.database.metadata.model.DBColumnContainer;
import org.knime.core.node.port.database.metadata.model.DBMetadata;
import org.knime.core.node.port.database.metadata.model.DBSchema;

/**
 * The class for the filtered DBMetadata.
 *
 * @author Andisa Dewi, KNIME.com, Berlin, Germany
 * @since 3.4
 */
public class FilteredDBMetadata implements DBMetadata {

    private final DBMetadata m_metadata;

    private final SearchFilter m_searchFilter;

    private final Iterable<DBSchema> m_schemas;

    /**
     * Constructor for the filtered DBMetadata.
     *
     * @param metadata the metadata to be filtered
     * @param searchFilter the filter to be applied on the metadata
     */
    public FilteredDBMetadata(final DBMetadata metadata, final SearchFilter searchFilter) {
        m_metadata = metadata;
        m_searchFilter = searchFilter;
        m_schemas = fetchSchemas();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDatabaseName() {
        return m_metadata.getDatabaseName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<DBSchema> getSchemas() {
        return m_schemas;
    }

    /**
     * Fetch the schemas from the database. This should only be called once, and the result will be stored in an array.
     *
     * @return the Iterable<DBSchema>
     */
    private Iterable<DBSchema> fetchSchemas() {
        Iterable<DBSchema> schemas = m_metadata.getSchemas();
        return new DBObjectFilterIterator<DBSchema>(schemas, m_searchFilter, new ElementWrapper<DBSchema, DBSchema>() {

            @Override
            public DBSchema wrapElement(final DBSchema schemaObject) {
                return new DBSchema() {

                    @Override
                    public String toString() {
                        return getName();
                    }

                    @Override
                    public String getName() {
                        return schemaObject.getName();
                    }

                    @Override
                    public Iterable<DBColumnContainer> getColumnContainers() {
                        return new DBObjectFilterIterator<DBColumnContainer>(schemaObject.getColumnContainers(),
                            m_searchFilter, new ElementWrapper<DBColumnContainer, DBColumnContainer>() {

                                @Override
                                public DBColumnContainer wrapElement(final DBColumnContainer colContainerObject) {
                                    return colContainerObject;
                                }
                            });
                    }

                };
            }
        });
    }

}
