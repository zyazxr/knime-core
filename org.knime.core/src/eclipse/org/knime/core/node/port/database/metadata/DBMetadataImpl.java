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
package org.knime.core.node.port.database.metadata;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import schemacrawler.schema.Catalog;
import schemacrawler.schema.Column;
import schemacrawler.schema.NamedObject;
import schemacrawler.schema.Schema;
import schemacrawler.schema.Table;
import schemacrawler.schema.View;
import schemacrawler.schemacrawler.IncludeAll;
import schemacrawler.schemacrawler.RegularExpressionInclusionRule;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder;
import schemacrawler.utility.SchemaCrawlerUtility;

/**
 *
 * @author adewi
 * @since 3.4
 */
public class DBMetadataImpl implements DBMetadata {

    SchemaCrawlerOptions m_option;

    Catalog m_catalog;

    /**
     * @param conn
     */
    public DBMetadataImpl(final Connection conn) {
        initializeSchemaCrawler();

        try {
            m_catalog = SchemaCrawlerUtility.getCatalog(conn, m_option);
            conn.close();
        } catch (SchemaCrawlerException ex) {
            //TODO
        } catch (SQLException ex) {
            // TODO
        }
    }

    private void initializeSchemaCrawler() {
        m_option = new SchemaCrawlerOptions();

        // by default include everything
        m_option.setSchemaInfoLevel(SchemaInfoLevelBuilder.maximum());
        m_option.setRoutineInclusionRule(new IncludeAll());
        m_option.setSequenceInclusionRule(new IncludeAll());
        m_option.setSynonymInclusionRule(new IncludeAll());
        m_option.setSchemaInclusionRule(new RegularExpressionInclusionRule((String)null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDatabaseName() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<DBSchema> getSchemas() {
        return new SchemaCrawlerCollection<DBSchema, Schema>(m_catalog, new ElementCreator<DBSchema, Schema>() {

            @Override
            public Collection<Schema> getElements(final NamedObject source) {
                return ((Catalog)source).getSchemas();
            }

            @Override
            public DBSchema wrapElement(final Schema schema) {
                return new DBSchema() {

                    @Override
                    public String getName() {
                        return schema.getFullName();
                    }

                    @Override
                    public Iterable<DBColumnContainer> getTables() {
                        return new SchemaCrawlerCollection<DBColumnContainer, Table>(m_catalog,
                            new ElementCreator<DBColumnContainer, Table>() {

                                @Override
                                public Collection<Table> getElements(final NamedObject source) {
                                    return ((Catalog)source).getTables(schema);
                                }

                                @Override
                                public DBColumnContainer wrapElement(final Table table) {
                                    if (table instanceof View) {
                                        return new DBView() {

                                            @Override
                                            public Iterable<DBColumn> getColumns() {
                                                return new SchemaCrawlerCollection<DBColumn, Column>(table,
                                                    new ElementCreator<DBColumn, Column>() {

                                                        @Override
                                                        public Collection<Column>
                                                            getElements(final NamedObject source) {
                                                            return ((Table)source).getColumns();
                                                        }

                                                        @Override
                                                        public DBColumn wrapElement(final Column column) {
                                                            return new DBColumn() {

                                                                @Override
                                                                public String getName() {
                                                                    return column.getName();
                                                                }

                                                                @Override
                                                                public String getColumnType() {
                                                                    return column.getColumnDataType().getName();
                                                                }

                                                            };
                                                        }

                                                    });
                                            }

                                            @Override
                                            public String getName() {
                                                return table.getName();
                                            }

                                            @Override
                                            public String getSchemaName() {
                                                return schema.getName();
                                            }

                                        };
                                    } else {
                                        return new DBTable() {

                                            @Override
                                            public Iterable<DBColumn> getColumns() {
                                                return new SchemaCrawlerCollection<DBColumn, Column>(table,
                                                    new ElementCreator<DBColumn, Column>() {

                                                        @Override
                                                        public Collection<Column>
                                                            getElements(final NamedObject source) {
                                                            return ((Table)source).getColumns();
                                                        }

                                                        @Override
                                                        public DBColumn wrapElement(final Column column) {
                                                            return new DBColumn() {

                                                                @Override
                                                                public String getName() {
                                                                    return column.getName();
                                                                }

                                                                @Override
                                                                public String getColumnType() {
                                                                    return column.getColumnDataType().getName();
                                                                }

                                                            };
                                                        }

                                                    });
                                            }


                                            @Override
                                            public String getName() {
                                                return table.getName();
                                            }


                                            @Override
                                            public String getSchemaName() {
                                                return schema.getName();
                                            }

                                        };
                                    }

                                }
                            });
                    }

                };
            }

        });

    }

}
