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
 *   Feb 15, 2017 (adewi): created
 */
package org.knime.base.node.io.database.metadata;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionPortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.metadata.DBMetadataProvider;
import org.knime.core.node.port.database.metadata.dialog.DialogComponentDBTableSelector;
import org.knime.core.node.port.database.metadata.dialog.SettingsModelDBMetadata;
import org.knime.core.node.port.database.metadata.filter.selected.SelectedTableFilter;
import org.knime.core.node.port.database.metadata.filter.selection.SelectionTableFilter;

/**
 * Node dialog for DB metadata node
 *
 * @author Andisa Dewi, KNIME.com, Berlin, Germany
 * @since 3.4
 */
public class DBMetadataDialogPane extends DefaultNodeSettingsPane {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DBMetadataDialogPane.class);

    private DatabaseConnectionSettings m_settings;

    private SettingsModelDBMetadata m_dbSettings = getMetadataModel();

    private DialogComponentDBTableSelector m_dialog;

    private DBMetadataProvider m_dbMetadataProvider = null;

    /**
     * Create a new node dialog for DB metadata node.
     */
    public DBMetadataDialogPane() {
        m_dialog = new DialogComponentDBTableSelector(m_dbSettings, m_dbMetadataProvider,
            new SelectionTableFilter(false), new SelectedTableFilter());
        addDialogComponent(m_dialog);
    }

    /** {@inheritDoc} */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        // TODO should we throw notConfigurableException?
        for (PortObjectSpec pos : specs) {
            if (pos == null) {
                m_settings = null;
            }
            if (pos instanceof DatabaseConnectionPortObjectSpec) {
                try {
                    m_settings =
                        ((DatabaseConnectionPortObjectSpec)pos).getConnectionSettings(getCredentialsProvider());

                    m_dbSettings.loadSettingsFrom(settings);
                } catch (InvalidSettingsException ex) {
                    LOGGER.warn("Could not load database connection from upstream node: " + ex.getMessage(), ex);
                }
            }
        }

        loadMetadataProvider();
    }

    /** {@inheritDoc} */
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

        m_dbSettings.saveSettingsTo(settings);
    }

    private DatabaseConnectionSettings getConnectionSettings() {
        return m_settings;
    }

    private void loadMetadataProvider() {
        final DatabaseConnectionSettings settings = getConnectionSettings();
        if (settings != null) {
            m_dbMetadataProvider = settings.getUtility().getDBMetadataProvider(getCredentialsProvider(), settings);
        } else {
            m_dbMetadataProvider = null;
        }
        m_dialog.updateMetadataProvider(m_dbMetadataProvider);

    }

    /**
     * @return the settingsmodel for DBMetadata
     */
    public static SettingsModelDBMetadata getMetadataModel() {
        return new SettingsModelDBMetadata("metadataBrowser");
    }

}
