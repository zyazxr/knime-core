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
 *   Mar 6, 2017 (adewi): created
 */
package org.knime.core.node.port.database.metadata.dialog;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.node.port.database.metadata.DBMetadataProvider;
import org.knime.core.node.port.database.metadata.filter.selected.SelectedElementFilter;
import org.knime.core.node.port.database.metadata.filter.selected.SelectedTableFilter;
import org.knime.core.node.port.database.metadata.filter.selection.SelectionFilter;
import org.knime.core.node.port.database.metadata.model.DBObject;
import org.knime.core.node.port.database.metadata.model.DBSchemaObject;

/**
 * Panel for the DB table selector.
 *
 * @author Andisa Dewi, KNIME.com, Berlin, Germany
 * @since 3.4
 */
public class DBTableSelectorPanel extends JPanel {

    private static final long serialVersionUID = 1;

    private final JTextField m_schemaField;

    private final JTextField m_tableField;

    private final JButton m_showButton;

    private DBMetadataProvider m_dbMetadataProvider;

    private DBMetadataTreeBrowserDialog m_dialog = null;

    private boolean m_updateDialog = false;

    /**
     * Constructor for the table selector panel
     *
     * @param dbmetaProvider the metadata provider
     * @param borderLabel the label for this panel
     * @param selectionFilter the selection filter
     * @param selElementFilter the selected element filter
     */
    public DBTableSelectorPanel(final DBMetadataProvider dbmetaProvider, final String borderLabel,
        final SelectionFilter selectionFilter, final SelectedElementFilter selElementFilter) {
        if (borderLabel != null && !borderLabel.isEmpty()) {
            setBorder(BorderFactory.createTitledBorder(borderLabel));
        }

        JLabel schemaLabel = new JLabel("Schema: ");
        JLabel objectLabel = new JLabel("Table: ");

        m_schemaField = new JTextField(15);
        m_tableField = new JTextField(15);

        schemaLabel.setLabelFor(m_schemaField);
        objectLabel.setLabelFor(m_tableField);

        JPanel schemaPanel = new JPanel(new FlowLayout());
        schemaPanel.add(schemaLabel, BorderLayout.WEST);
        schemaPanel.add(m_schemaField, BorderLayout.CENTER);

        JPanel tablePanel = new JPanel(new FlowLayout());
        tablePanel.add(objectLabel, BorderLayout.WEST);
        tablePanel.add(m_tableField, BorderLayout.CENTER);

        m_showButton = new JButton("Show browser");
        m_showButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {

                Container container = getParent();
                while (container != null) {
                    if (container instanceof Frame) {
                        break;
                    }
                    container = container.getParent();
                }

                ((SelectedTableFilter)selElementFilter).setSchema(m_schemaField.getText());
                ((SelectedTableFilter)selElementFilter).setTable(m_tableField.getText());

                if (m_dialog == null || m_updateDialog || (m_dialog != null && m_dialog.isJobCancelled())) {
                    m_dialog = new DBMetadataTreeBrowserDialog((Frame)container, m_dbMetadataProvider, borderLabel,
                        selectionFilter, selElementFilter);
                    m_updateDialog = false;
                }
                m_dialog.open(selElementFilter);

                DBObject selObject = m_dialog.getSelectedObject();
                if (selObject != null) {
                    if (selObject instanceof DBSchemaObject) {
                        m_schemaField.setText(((DBSchemaObject)selObject).getSchemaName());
                    }
                    m_tableField.setText(selObject.getName());
                }
            }

        });

        add(schemaPanel, BorderLayout.WEST);
        add(tablePanel, BorderLayout.CENTER);
        add(m_showButton, BorderLayout.EAST);
    }

    private void changeButtonVisibility(final boolean enabled) {
        if (enabled) {
            m_showButton.setEnabled(true);
            m_showButton.setToolTipText("Open database metadata browser");
        } else {
            m_showButton.setEnabled(false);
            m_showButton.setToolTipText("Please provide a valid input connection");
        }
    }

    /**
     * Update the metadata provider
     *
     * @param dbMetadataProvider
     */
    public void updateMetadataProvider(final DBMetadataProvider dbMetadataProvider) {
        m_dbMetadataProvider = dbMetadataProvider;
        if (m_dbMetadataProvider == null) {
            changeButtonVisibility(false);
        } else {
            changeButtonVisibility(true);
            m_updateDialog = true;
        }

    }

    /**
     * Get the schema name from the text field.
     *
     * @return the schema name
     */
    public String getSchema() {
        return m_schemaField.getText();
    }

    /**
     * Get the table name from the text field.
     *
     * @return the table name
     */
    public String getTable() {
        return m_tableField.getText();
    }

    /**
     * Set the schema name in the text field.
     *
     * @param newSchema the name of the new schema
     */
    public void setSchema(final String newSchema) {
        m_schemaField.setText(newSchema);
    }

    /**
     * Set the table name in the text field.
     *
     * @param newTable the name of the new table
     */
    public void setTable(final String newTable) {
        m_tableField.setText(newTable);
    }
}
