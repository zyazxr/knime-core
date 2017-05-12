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
 *   Feb 21, 2017 (adewi): created
 */
package org.knime.core.node.port.database.metadata.impl;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.sql.SQLType;
import java.util.Collection;
import java.util.logging.Level;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.metadata.model.DBCatalog;
import org.knime.core.node.port.database.metadata.model.DBColumn;
import org.knime.core.node.port.database.metadata.model.DBColumnContainer;
import org.knime.core.node.port.database.metadata.model.DBMetadata;
import org.knime.core.node.port.database.metadata.model.DBSchema;
import org.knime.core.node.port.database.metadata.model.DBTable;
import org.knime.core.node.port.database.metadata.model.DBView;
import org.knime.core.node.workflow.CredentialsProvider;

import schemacrawler.schema.Column;
import schemacrawler.schema.NamedObject;
import schemacrawler.schema.Schema;
import schemacrawler.schema.Table;
import schemacrawler.schema.View;
import schemacrawler.schemacrawler.RegularExpressionInclusionRule;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaInfoLevel;
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder;
import schemacrawler.utility.SchemaCrawlerUtility;
import sf.util.Utility;

/**
 * The implementation of DBMetadata interface.
 *
 * @author Andisa Dewi, KNIME.com, Berlin, Germany
 * @since 3.4
 */
public class DBMetadataImpl implements DBMetadata {

    private final static NodeLogger LOGGER = NodeLogger.getLogger(DBMetadataImpl.class);

    private final DBCatalog m_catalog;

    private final DatabaseConnectionSettings m_settings;

    private final CredentialsProvider m_cp;

    private final Iterable<DBSchema> m_schemas;

    /**
     * Constructor for the DBMetadata. It fetches the new metadata information using SchemaCrawler.
     *
     * @param settings the database connection settings
     * @param cp the credentials provider
     *
     * @throws SQLException
     * @throws IOException
     * @throws InvalidSettingsException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     */
    public DBMetadataImpl(final DatabaseConnectionSettings settings, final CredentialsProvider cp) throws SQLException,
        InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidSettingsException, IOException {
        Utility.setApplicationLogLevel(Level.OFF);
        m_settings = settings;
        m_cp = cp;
        m_catalog = getCatalog(false, null);
        m_schemas = fetchSchemas();
    }

    /**
     * Get the catalog from Schemacrawler.
     *
     * @param includeColumns true if columns should be fetched as well, otherwise false
     * @param option if set to null, a default Schemacrawler option will be created, otherwise this will be used
     * @return the DBCatalog
     *
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws InvalidSettingsException
     * @throws SQLException
     * @throws IOException
     */
    @SuppressWarnings("resource")
    private DBCatalog getCatalog(final boolean includeColumns, final SchemaCrawlerOptions option)
        throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidSettingsException,
        SQLException, IOException {
        final Connection conn = m_settings.createConnection(m_cp);

        synchronized (m_settings.syncConnection(conn)) {
            try {
                return new DBCatalog(
                    SchemaCrawlerUtility.getCatalog(conn, option == null ? getOptions(includeColumns) : option));
            } catch (SchemaCrawlerException ex) {
                throw new SQLException("Cannot get catalog from SchemaCrawler. Reason: " + ex.getMessage());
            }
        }
    }

    /**
     * Create a new Schemacrawler option.
     *
     * @param includeColumns true if columns should be fetched as well, otherwise false
     * @return the Schemacrawler option
     */
    private SchemaCrawlerOptions getOptions(final boolean includeColumns) {
        SchemaCrawlerOptions option = new SchemaCrawlerOptions();
        SchemaInfoLevel level = SchemaInfoLevelBuilder.minimum();
        level.setRetrieveRoutines(false);
        if (includeColumns) {
            level.setRetrieveTableColumns(true);
            level.setRetrieveHiddenTableColumns(true);
            level.setRetrieveColumnDataTypes(true);
        }
        option.setSchemaInfoLevel(level);
        return option;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDatabaseName() {
        return m_catalog.getDatabaseName();
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
        return new SchemaCrawlerCollection<DBSchema, Schema>(m_catalog, new ElementCreator<DBSchema, Schema>() {

            @Override
            public Collection<Schema> getElements(final NamedObject source) {
                return ((DBCatalog)source).getSchemas();
            }

            @Override
            public DBSchema wrapElement(final Schema schema) {
                return new DBSchema() {

                    private Iterable<DBColumnContainer> m_containers = fetchColumnContainers();

                    @Override
                    public String toString() {
                        return getName();
                    }

                    @Override
                    public String getName() {
                        return schema.getFullName();
                    }

                    @Override
                    public Iterable<DBColumnContainer> getColumnContainers() {
                        return m_containers;
                    }

                    private Iterable<DBColumnContainer> fetchColumnContainers() {
                        m_containers = new SchemaCrawlerCollection<DBColumnContainer, Table>(m_catalog,
                            new ElementCreator<DBColumnContainer, Table>() {

                                @Override
                                public Collection<Table> getElements(final NamedObject source) {
                                    return ((DBCatalog)source).getTables(schema);
                                }

                                @Override
                                public DBColumnContainer wrapElement(final Table table) {
                                    if (table instanceof View) {
                                        return new DBViewImpl(table);

                                    } else {
                                        return new DBTableImpl(table);
                                    }

                                }
                            });
                        return m_containers;
                    }

                };
            }

        });

    }

    private class DBViewImpl extends DBMetadataImpl.DBColumnContainerImpl implements DBView {

        /**
         * Wrap the Schemacrawler View into DBView.
         *
         * @param table the view to be wrapped
         */
        public DBViewImpl(final Table table) {
            super(table);
        }

    }

    private class DBTableImpl extends DBMetadataImpl.DBColumnContainerImpl implements DBTable {

        /**
         * Wrap the Schemacrawler Table into DBTable
         *
         * @param table the table to be wrapped
         */
        public DBTableImpl(final Table table) {
            super(table);
        }

    }

    private class DBColumnContainerImpl implements DBColumnContainer {

        private Iterable<DBColumn> m_columns = null;

        private final Table m_table;

        /**
         * The implementation of the DBColumnContainer interface.
         *
         * @param table the column container to be wrapped
         */
        public DBColumnContainerImpl(final Table table) {
            m_table = table;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return getName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getSchemaName() {
            return m_table.getSchema().toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return m_table.getName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Iterable<DBColumn> getColumns() {
            return m_columns;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Iterable<DBColumn> getNewColumnsIfEmpty() {
            if (m_columns == null) {
                try {
                    SchemaCrawlerOptions option = DBMetadataImpl.this.getOptions(true);
                    option.setTableNamePattern(this.getName());
                    option.setSchemaInclusionRule(new RegularExpressionInclusionRule(this.getSchemaName()));
                    DBCatalog catalog = DBMetadataImpl.this.getCatalog(true, option);

                    for (Table newTable : catalog.getTables()) {
                        m_columns = new SchemaCrawlerCollection<DBColumn, Column>(newTable,
                            new ElementCreator<DBColumn, Column>() {

                                @Override
                                public Collection<Column> getElements(final NamedObject source) {
                                    return ((Table)source).getColumns();
                                }

                                @Override
                                public DBColumn wrapElement(final Column column) {
                                    return new DBColumn() {

                                        @Override
                                        public String toString() {
                                            return getName();
                                        }

                                        @Override
                                        public String getName() {
                                            return column.getName();
                                        }

                                        @Override
                                        public String getColumnTypeName() {
                                            return column.getColumnDataType().getName();
                                        }

                                        @Override
                                        public SQLType getColumnType() {
                                            return JDBCType.valueOf(
                                                column.getColumnDataType().getJavaSqlType().getJavaSqlTypeName());
                                        }

                                        @Override
                                        public String getColumnContainerName() {
                                            return newTable.getName();
                                        }

                                        @Override
                                        public String getFullColumnContainerName() {
                                            return newTable.getFullName();
                                        }
                                    };
                                }

                            });
                    }
                } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException
                        | InvalidSettingsException | SQLException | IOException ex) {
                    LOGGER.info("Error fetching table columns. Reason: " + ex.getMessage(), ex);
                    m_columns = null;
                }
            }
            return m_columns;
        }
    }

}
