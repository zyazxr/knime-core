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
 *   Apr 27, 2017 (adewi): created
 */
package org.knime.core.node.port.database.metadata.dialog;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.node.port.database.metadata.filter.search.DefaultSearchFilter;
import org.knime.core.node.port.database.metadata.filter.search.SchemaSearchFilter;
import org.knime.core.node.port.database.metadata.filter.search.SearchFilter;
import org.knime.core.node.port.database.metadata.filter.search.TableSearchFilter;

/**
 * The panel for the DB metadata search filter.
 *
 * @author Andisa Dewi, KNIME.com, Berlin, Germany
 * @since 3.4
 */
public class DBSearchFilterPanel extends JPanel {

    private static final long serialVersionUID = -6298721143049346668L;

    private final static String[] FILTER_OPTION = {"All", "Schema", "Table", "View"};

    private final JTextField m_keyword = new JTextField(10);

    private final JComboBox<String> m_dropDownList = new JComboBox<String>(FILTER_OPTION);

    /**
     * The constructor for the DB search filter panel.
     *
     * @param panel the metadata tree browser
     */
    public DBSearchFilterPanel(final DBMetadataTreeBrowser panel) {
        JLabel label = new JLabel("Search: ");
        m_keyword.setToolTipText("Search using a keyword");

        m_dropDownList.setSelectedIndex(0);
        m_dropDownList.setToolTipText("Specify what to search");
        JButton searchButton = new JButton("Search");
        searchButton.setToolTipText("Apply search");

        JButton resetButton = new JButton("Reset");
        resetButton.setToolTipText("Reset search results");
        resetButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                doReset(panel);
            }
        });

        searchButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                doSearch(panel);
            }
        });

        m_keyword.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                doSearch(panel);
            }
        });

        JPanel m_searchBoxContainer = new JPanel();
        JPanel panelForm = new JPanel(new GridBagLayout());
        m_searchBoxContainer.add(panelForm);

        GridBagConstraints ct = new GridBagConstraints();
        ct.insets = new Insets(0, 2, 0, 2);

        ct.gridx = 0;
        ct.gridy = 0;
        panelForm.add(label, ct);

        ct.gridx = 1;
        ct.gridy = 0;
        m_keyword.setPreferredSize(m_dropDownList.getPreferredSize());
        panelForm.add(m_keyword, ct);
        ct.gridx = 2;
        ct.gridy = 0;

        panelForm.add(m_dropDownList, ct);
        ct.gridx = 3;
        ct.gridy = 0;

        panelForm.add(searchButton, ct);
        ct.gridx = 4;
        ct.gridy = 0;

        panelForm.add(resetButton);
        ct.gridx = 5;
        ct.gridy = 0;

        setLayout(new BorderLayout());
        add(m_searchBoxContainer, BorderLayout.WEST);

    }

    /**
     * Reset the tree browser and the search box.
     *
     * @param panel the tree browser
     */
    private void doReset(final DBMetadataTreeBrowser panel) {
        panel.updateTreeFilter(null);
        m_keyword.setText("");
    }

    /**
     * Execute the search filter. At the end the tree browser should be updated.
     *
     * @param panel the tree browser
     */
    private void doSearch(final DBMetadataTreeBrowser panel) {
        if (!m_keyword.getText().isEmpty()) {
            SearchFilter filter = null;
            Pattern regex = Pattern.compile("^.*(" + Pattern.quote(m_keyword.getText()) + ").*$",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            switch (m_dropDownList.getSelectedIndex()) {
                case 0:
                    filter = new DefaultSearchFilter(regex);
                    break;
                case 1:
                    filter = new SchemaSearchFilter(regex);
                    break;
                case 2:
                    filter = new TableSearchFilter(regex, 0);
                    break;
                case 3:
                    filter = new TableSearchFilter(regex, 1);
                    break;
                default:
                    break;
            }
            panel.updateTreeFilter(filter);
        } else {
            doReset(panel);
        }
    }
}
