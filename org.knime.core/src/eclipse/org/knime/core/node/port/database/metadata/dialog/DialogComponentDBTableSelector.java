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

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.metadata.DBMetadataProvider;
import org.knime.core.node.port.database.metadata.filter.selected.SelectedElementFilter;
import org.knime.core.node.port.database.metadata.filter.selection.SelectionFilter;

/**
 * Dialog component for database table selector.
 *
 * @author Andisa Dewi, KNIME.com, Berlin, Germany
 * @since 3.4
 */
public class DialogComponentDBTableSelector extends DialogComponent {

    private final DBTableSelectorPanel m_panel;

    /**
     * Constructor for the table selector dialog component.
     *
     * @param model the settingsmodel for the DB metadata
     * @param dbmetadata the metadata provider
     * @param selFilter the selection filter
     * @param selElementFilter the selected element filter
     */
    public DialogComponentDBTableSelector(final SettingsModelDBMetadata model, final DBMetadataProvider dbmetadata,
        final SelectionFilter selFilter, final SelectedElementFilter selElementFilter) {
        super(model);

        final JPanel existingPanel = getComponentPanel();
        m_panel = new DBTableSelectorPanel(dbmetadata, "Database Metadata Browser", selFilter, selElementFilter);
        existingPanel.add(m_panel);

        model.prependChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });

        updateComponent();
    }

    /**
     * Update the schema and table name in the settingsmodel.
     */
    protected void updateModel() {
        String newSchema = m_panel.getSchema();
        String newTable = m_panel.getTable();

        SettingsModelDBMetadata model = (SettingsModelDBMetadata)getModel();
        model.setValues(newSchema, newTable);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        final SettingsModelDBMetadata model = (SettingsModelDBMetadata)getModel();

        m_panel.setSchema(model.getSchema());
        m_panel.setTable(model.getTable());

        // update enable status
        setEnabledComponents(model.isEnabled());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        updateModel();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // nothing to do

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_panel.setEnabled(enabled);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_panel.setToolTipText(text);

    }

    /**
     * Update metadata provider
     *
     * @param dbMetadata the metadata provider to be updated
     */
    public void updateMetadataProvider(final DBMetadataProvider dbMetadata) {
        m_panel.updateMetadataProvider(dbMetadata);
    }

}
