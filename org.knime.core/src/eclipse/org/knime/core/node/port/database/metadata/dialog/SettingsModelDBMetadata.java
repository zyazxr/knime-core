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
 *   Mar 20, 2017 (adewi): created
 */
package org.knime.core.node.port.database.metadata.dialog;

import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Settingsmodel for database metadata.
 *
 * @author Andisa Dewi, KNIME.com, Berlin, Germany
 * @since 3.4
 */
public class SettingsModelDBMetadata extends SettingsModel {

    private final static String SCHEMA = "schema";

    private final static String TABLE = "table";

    private final static String COLUMN = "column";

    private String m_schema;

    private String m_table;

    private String m_column;

    private String m_config;

    /**
     * Constructor for the settingsmodel
     *
     * @param configName the config name
     */
    public SettingsModelDBMetadata(final String configName) {
        m_schema = "";
        m_table = "";
        m_column = "";
        m_config = configName;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelDBMetadata createClone() {
        return new SettingsModelDBMetadata(m_config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "SMID_dbmetadata";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getConfigName() {
        return m_config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        final Config config;
        try {
            config = settings.getConfig(m_config);
            setValues(config.getString(SCHEMA, m_schema), config.getString(TABLE, m_table),
                config.getString(COLUMN, m_column));
        } catch (InvalidSettingsException ex) {
            throw new NotConfigurableException(ex.getMessage());
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveSettingsForModel(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        // nothing to validate?
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        Config config = settings.getConfig(m_config);
        setValues(config.getString(SCHEMA, m_schema), config.getString(TABLE, m_table),
            config.getString(COLUMN, m_column));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        Config config = settings.addConfig(m_config);
        config.addString(SCHEMA, m_schema);
        config.addString(TABLE, m_table);
        config.addString(COLUMN, m_column);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_config + "')";
    }

    /**
     * Set the schema and table values.
     *
     * @param schema the schema name
     * @param table the table name
     */
    public void setValues(final String schema, final String table) {
        boolean changed = false;
        changed = setSchema(schema) || changed;
        changed = setTable(table) || changed;
        if (changed) {
            notifyChangeListeners();
        }
    }

    /**
     * Set the schema, table and column values.
     *
     * @param schema the schema name
     * @param table the table name
     * @param column the column name
     */
    public void setValues(final String schema, final String table, final String column) {
        boolean changed = false;
        changed = setSchema(schema) || changed;
        changed = setTable(table) || changed;
        changed = setColumn(column) || changed;
        if (changed) {
            notifyChangeListeners();
        }
    }

    private boolean setSchema(final String newValue) {
        boolean sameValue;
        if (newValue == null) {
            sameValue = (m_schema == null);
        } else {
            sameValue = newValue.equals(m_schema);
        }
        m_schema = newValue;
        return !sameValue;
    }

    private boolean setTable(final String newValue) {
        boolean sameValue;
        if (newValue == null) {
            sameValue = (m_table == null);
        } else {
            sameValue = newValue.equals(m_table);
        }
        m_table = newValue;
        return !sameValue;
    }

    private boolean setColumn(final String newValue) {
        boolean sameValue;
        if (newValue == null) {
            sameValue = (m_column == null);
        } else {
            sameValue = newValue.equals(m_column);
        }
        m_column = newValue;
        return !sameValue;
    }

    /**
     * Get the schema name.
     *
     * @return schema the schema name
     */
    public String getSchema() {
        return m_schema;
    }

    /**
     * Get the table name.
     *
     * @return table the table name
     */
    public String getTable() {
        return m_table;
    }

    /**
     * Get the column name.
     *
     * @return column the column name
     */
    public String getColumn() {
        return m_column;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void prependChangeListener(final ChangeListener l) {
        super.prependChangeListener(l);
    }
}
